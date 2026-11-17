package com.koala.koalagenda

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { c ->
    addOnSuccessListener { c.resume(it) }
    addOnFailureListener { c.resumeWithException(it) }
}

data class AgendaState(
    val loaded: Boolean = false,
    val roulements: Map<Who, Roulement> = Who.entries.associateWith { Roulement.parDefaut(it) },
    val jours: Map<String, Jour> = emptyMap(),
    val evenements: List<Evenement> = emptyList(),
    val couleurs: Map<Who, Long> = COULEURS_DEFAUT,
    val enfants: Enfants = Enfants(),
    val pending: Boolean = false,
    val error: String? = null,
) {
    fun roulement(w: Who) = roulements[w] ?: Roulement.parDefaut(w)
    fun jour(w: Who, d: java.time.LocalDate) = jours[Jour.idDe(w, d)]
    fun vu(w: Who, d: java.time.LocalDate) = jourVu(roulement(w), jour(w, d), d)
}

/**
 * Données dans Firestore (projet koalagenda), avec cache local illimité :
 * l'appli marche hors ligne, les modifications partent dès que le réseau revient.
 */
class Repo private constructor(private val ctx: Context) {
    private val app: FirebaseApp = FirebaseApp.getApps(ctx).firstOrNull() ?: FirebaseApp.initializeApp(
        ctx,
        FirebaseOptions.Builder()
            .setApiKey("AIzaSyA9jxwhZLXdqrK34hd0fda7XOC0pPi6qH4")
            .setApplicationId("1:390953071877:web:4b3d8aba69b926b2fbf36c")
            .setProjectId("koalagenda")
            .setStorageBucket("koalagenda.firebasestorage.app")
            .setGcmSenderId("390953071877")
            .build(),
    )
    private val auth = FirebaseAuth.getInstance(app)
    private val db = FirebaseFirestore.getInstance(app).apply {
        runCatching {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED).build()
                ).build()
        }
    }
    private val roulements get() = db.collection("roulements")
    private val jours get() = db.collection("jours")
    private val evenements get() = db.collection("evenements")
    private val prefs = ctx.getSharedPreferences("koalagenda", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(AgendaState())
    val state: StateFlow<AgendaState> = _state
    private val regs = mutableListOf<ListenerRegistration>()
    private val pend = mutableMapOf<String, Boolean>()
    private val charge = mutableSetOf<String>()

    val signedIn get() = auth.currentUser != null

    var moi: Who?
        get() = Who.of(prefs.getString("moi", null))
        set(v) { prefs.edit().putString("moi", v?.id).apply() }

    suspend fun signIn(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email.trim(), pass).awaitTask()
        listen()
    }

    fun signOut() {
        regs.forEach { it.remove() }; regs.clear()
        pend.clear(); charge.clear()
        auth.signOut()
        _state.value = AgendaState()
    }

    private fun maj(nom: String, pending: Boolean, err: String?, f: (AgendaState) -> AgendaState) {
        pend[nom] = pending
        charge += nom
        val s = f(_state.value)
        _state.value = s.copy(
            loaded = charge.size >= 5,
            pending = pend.values.any { it },
            error = err,
        )
    }

    fun listen() {
        if (regs.isNotEmpty() || !signedIn) return
        // INCLUDE : être prévenu aussi quand le serveur confirme un envoi
        regs += roulements.addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
            if (err != null) { maj("r", false, err.message) { it }; return@addSnapshotListener }
            if (snap == null) return@addSnapshotListener
            val m = snap.documents.mapNotNull { doc -> Who.of(doc.id)?.let { it to Roulement.fromMap(doc.data ?: emptyMap()) } }.toMap()
            // Premier lancement : on enregistre les roulements de départ (seulement d'après le serveur)
            if (!snap.metadata.isFromCache) Who.entries.filter { it !in m }.forEach { w ->
                roulements.document(w.id).set(Roulement.parDefaut(w).toMap())
            }
            maj("r", snap.metadata.hasPendingWrites(), null) { s ->
                s.copy(roulements = Who.entries.associateWith { m[it] ?: Roulement.parDefaut(it) })
            }
        }
        regs += jours.addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
            if (err != null) { maj("j", false, err.message) { it }; return@addSnapshotListener }
            if (snap == null) return@addSnapshotListener
            val m = snap.documents.mapNotNull { Jour.fromMap(it.data ?: emptyMap()) }.associateBy { it.id }
            maj("j", snap.metadata.hasPendingWrites(), null) { it.copy(jours = m) }
        }
        regs += evenements.addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
            if (err != null) { maj("e", false, err.message) { it }; return@addSnapshotListener }
            if (snap == null) return@addSnapshotListener
            val l = snap.documents.mapNotNull { Evenement.fromMap(it.id, it.data ?: emptyMap()) }
            maj("e", snap.metadata.hasPendingWrites(), null) { it.copy(evenements = trierEvenements(l)) }
            moi?.let { Rappels.planifier(ctx, l, it) }
        }
        listenCouleurs()
        listenEnfants()
    }

    private fun listenCouleurs() {
        regs += db.collection("reglages").document("couleurs").addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
            if (err != null) { maj("c", false, err.message) { it }; return@addSnapshotListener }
            val m = Who.entries.associateWith { w ->
                (snap?.get(w.id) as? Number)?.toLong() ?: COULEURS_DEFAUT.getValue(w)
            }
            maj("c", snap?.metadata?.hasPendingWrites() == true, null) { it.copy(couleurs = m) }
        }
    }

    private fun listenEnfants() {
        regs += db.collection("reglages").document("enfants").addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
            if (err != null) { maj("k", false, err.message) { it }; return@addSnapshotListener }
            val e = Enfants.fromMap(if (snap?.exists() == true) snap.data else null)
            maj("k", snap?.metadata?.hasPendingWrites() == true, null) { it.copy(enfants = e) }
        }
    }

    fun saveEnfants(e: Enfants) {
        db.collection("reglages").document("enfants").set(e.toMap())
    }

    fun saveCouleur(w: Who, c: Long) {
        db.collection("reglages").document("couleurs")
            .set(mapOf(w.id to c), com.google.firebase.firestore.SetOptions.merge())
    }

    fun replanifierRappels() {
        val m = moi ?: return
        Rappels.planifier(ctx, _state.value.evenements, m)
    }

    fun saveRoulement(w: Who, r: Roulement) {
        roulements.document(w.id).set(r.toMap())
    }

    fun saveJour(j: Jour) {
        if (j.vide) jours.document(j.id).delete()
        else jours.document(j.id).set(j.copy(majLe = System.currentTimeMillis()).toMap())
    }

    fun saveJours(l: List<Jour>) {
        if (l.isEmpty()) return
        l.chunked(400).forEach { part ->
            val b = db.batch()
            part.forEach { j ->
                if (j.vide) b.delete(jours.document(j.id)) else b.set(jours.document(j.id), j.toMap())
            }
            b.commit()
        }
    }

    fun saveEvenement(e: Evenement) {
        evenements.document(e.id).set(e.copy(majLe = System.currentTimeMillis()).toMap())
    }

    fun deleteEvenement(e: Evenement) {
        evenements.document(e.id).delete()
    }

    companion object {
        @Volatile private var inst: Repo? = null
        fun get(ctx: Context) = inst ?: synchronized(this) { inst ?: Repo(ctx.applicationContext).also { inst = it } }
    }
}
