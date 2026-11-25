package com.koala.koalagenda

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject

/**
 * Rappels locaux (AlarmManager). La liste programmée est gardée dans les préférences
 * pour pouvoir l'annuler et la reprogrammer après un redémarrage du téléphone.
 */
object Rappels {
    private const val CANAL = "rappels"
    private const val PREFS = "rappels"
    private const val HORIZON = 120L * 24 * 3600 * 1000

    fun canal(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CANAL, "Rappels des rdv", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    private data class Item(val id: String, val at: Long, val titre: String, val texte: String)

    private fun intent(ctx: Context, it: Item) = Intent(ctx, RappelReceiver::class.java)
        .setData(Uri.parse("koalagenda://rappel/${it.id}"))
        .putExtra("titre", it.titre)
        .putExtra("texte", it.texte)
        .putExtra("id", it.id)

    private fun pending(ctx: Context, it: Item) = PendingIntent.getBroadcast(
        ctx, 0, intent(ctx, it), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun lire(ctx: Context): List<Item> = runCatching {
        val a = JSONArray(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("items", "[]"))
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            Item(o.getString("id"), o.getLong("at"), o.optString("titre"), o.optString("texte"))
        }
    }.getOrDefault(emptyList())

    private fun ecrire(ctx: Context, l: List<Item>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("id", it.id).put("at", it.at).put("titre", it.titre).put("texte", it.texte)) }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("items", a.toString()).apply()
    }

    private fun programmer(ctx: Context, am: AlarmManager, it: Item) {
        val pi = pending(ctx, it)
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        runCatching {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it.at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it.at, pi)
        }
    }

    fun planifier(ctx: Context, evenements: List<Evenement>, anniversaires: List<Anniversaire>, moi: Who) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val now = System.currentTimeMillis()
        val nouveaux = evenements
            .filter { it.visiblePar(moi) && it.concerne(moi) }
            .mapNotNull { e ->
                val at = e.rappelA() ?: return@mapNotNull null
                if (at <= now || at > now + HORIZON) return@mapNotNull null
                val quand = buildString {
                    append(fmtJour(e.date))
                    if (!e.journee) append(" à ${e.heure.replace(":", "h")}")
                }
                Item("${e.id}_${e.majLe}", at, e.titre.ifBlank { "Rdv" }, quand)
            } + anniversaires.mapNotNull { a ->
                // Le jour même à 8h
                val jour = a.prochain(java.time.LocalDate.now())
                val at = jour.atTime(8, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                if (at <= now || at > now + HORIZON) return@mapNotNull null
                val age = a.age(jour.year)
                Item(
                    "anniv_${a.id}_${jour}_${a.nom.hashCode()}", at, "🎂 Anniversaire de ${a.nom}",
                    if (age != null) "$age ans aujourd'hui" else "C'est aujourd'hui !",
                )
            }
        val anciens = lire(ctx)
        val garder = nouveaux.map { it.id }.toSet()
        anciens.filter { it.id !in garder }.forEach { am.cancel(pending(ctx, it)) }
        val deja = anciens.map { it.id }.toSet()
        nouveaux.filter { it.id !in deja }.forEach { programmer(ctx, am, it) }
        ecrire(ctx, nouveaux)
    }

    /** Après un redémarrage ou une mise à jour : les alarmes ont été effacées par Android. */
    fun replanifier(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val now = System.currentTimeMillis()
        val l = lire(ctx).filter { it.at > now }
        l.forEach { programmer(ctx, am, it) }
        ecrire(ctx, l)
    }

    fun afficher(ctx: Context, id: String, titre: String, texte: String) {
        canal(ctx)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val ouvrir = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CANAL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(titre)
            .setContentText(texte)
            .setAutoCancel(true)
            .setContentIntent(ouvrir)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(id.hashCode(), n) }
        // L'item est consommé
        ecrire(ctx, lire(ctx).filter { it.id != id })
    }
}

class RappelReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Rappels.afficher(
            ctx,
            intent.getStringExtra("id").orEmpty(),
            intent.getStringExtra("titre").orEmpty(),
            intent.getStringExtra("texte").orEmpty(),
        )
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Rappels.replanifier(ctx)
    }
}
