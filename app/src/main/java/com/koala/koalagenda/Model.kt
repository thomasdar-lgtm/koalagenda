package com.koala.koalagenda

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

enum class Who(val id: String, val label: String) {
    THOMAS("thomas", "Thomas"),
    CINDY("cindy", "Cindy");

    val initiale get() = label.first().toString()

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id }
    }
}

enum class Etat(val id: String, val label: String) {
    TRAVAIL("travail", "Travail"),
    REPOS("repos", "Repos"),
    CONGE("conge", "Congé"),
    RECUP("recup", "Récup"),
    FRAC("frac", "Frac");

    val off get() = this != TRAVAIL

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id }
    }
}

val JOURS_COURTS = listOf("L", "M", "M", "J", "V", "S", "D")
val JOURS_LONGS = listOf("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche")
val MOIS = listOf(
    "janvier", "février", "mars", "avril", "mai", "juin",
    "juillet", "août", "septembre", "octobre", "novembre", "décembre",
)

fun lundi(d: LocalDate): LocalDate = d.minusDays((d.dayOfWeek.value - 1).toLong())

fun fmtJour(d: LocalDate) =
    "${JOURS_LONGS[d.dayOfWeek.value - 1].replaceFirstChar { it.uppercase() }} ${d.dayOfMonth} ${MOIS[d.monthValue - 1]}"

fun fmtCourt(d: LocalDate) = "%02d/%02d/%d".format(d.dayOfMonth, d.monthValue, d.year)

private fun anyToInt(v: Any?): Int? = when (v) {
    is Number -> v.toInt()
    is String -> v.toIntOrNull()
    else -> null
}

private fun anyToLong(v: Any?): Long = when (v) {
    is Number -> v.toLong()
    is String -> v.toLongOrNull() ?: 0L
    else -> 0L
}

private fun anyToDate(v: Any?): LocalDate? = (v as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

private fun anyToIntSet(v: Any?): Set<Int> =
    (v as? List<*>)?.mapNotNull { anyToInt(it) }?.filter { it in 1..7 }?.toSet() ?: emptySet()

/**
 * Roulement d'une personne : jours de repos fixes + un cycle de semaines,
 * chaque semaine du cycle ayant ses jours de repos tournants.
 * Jours : 1 = lundi … 7 = dimanche. [debut] = lundi de la semaine 1 du cycle.
 */
data class Roulement(
    val fixes: Set<Int> = setOf(7),
    val cycle: List<Set<Int>> = emptyList(),
    val debut: LocalDate = LocalDate.of(2026, 11, 16),
    val congesParAn: Int = 25,
) {
    /** Indice (0…n-1) de la semaine du cycle qui contient [d], -1 si pas de cycle. */
    fun semaine(d: LocalDate): Int {
        if (cycle.isEmpty()) return -1
        val semaines = Math.floorDiv(ChronoUnit.DAYS.between(debut, lundi(d)), 7L)
        return Math.floorMod(semaines, cycle.size.toLong()).toInt()
    }

    fun etatAuto(d: LocalDate): Etat {
        val j = d.dayOfWeek.value
        if (j in fixes) return Etat.REPOS
        val i = semaine(d)
        if (i >= 0 && j in cycle[i]) return Etat.REPOS
        return Etat.TRAVAIL
    }

    /** Nouveau roulement où [d] tombe dans la semaine [index] du cycle. */
    fun avecSemaineActuelle(d: LocalDate, index: Int): Roulement =
        copy(debut = lundi(d).minusWeeks(index.toLong()))

    fun toMap(): Map<String, Any> = mapOf(
        "fixes" to fixes.sorted(),
        // Firestore refuse les tableaux dans des tableaux : chaque semaine devient "6" ou "1,4"
        "cycle" to cycle.map { it.sorted().joinToString(",") },
        "debut" to debut.toString(),
        "congesParAn" to congesParAn,
    )

    companion object {
        fun fromMap(m: Map<String, Any?>): Roulement = Roulement(
            fixes = anyToIntSet(m["fixes"]),
            cycle = (m["cycle"] as? List<*>)?.map { w ->
                if (w is String) w.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..7 }.toSet()
                else anyToIntSet(w)
            } ?: emptyList(),
            debut = anyToDate(m["debut"]) ?: LocalDate.of(2026, 11, 16),
            congesParAn = anyToInt(m["congesParAn"]) ?: 25,
        )

        /** Roulements donnés au lancement du projet (semaine du 16/11/2026 = semaine 1). */
        fun parDefaut(w: Who): Roulement = when (w) {
            Who.THOMAS -> Roulement(setOf(7), listOf(setOf(6), setOf(5), setOf(4), setOf(1)))
            Who.CINDY -> Roulement(setOf(7), listOf(setOf(1), setOf(2), setOf(6), setOf(5)))
        }
    }
}

/** Modification d'un jour précis. [etat] null = on suit le roulement. */
data class Jour(
    val who: Who,
    val date: LocalDate,
    val etat: Etat? = null,
    val heureSup: Boolean = false,
    val remplacement: String = "",
    val note: String = "",
    val majLe: Long = 0,
) {
    val id get() = idDe(who, date)
    val vide get() = etat == null && !heureSup && remplacement.isBlank() && note.isBlank()

    fun toMap(): Map<String, Any> = mapOf(
        "who" to who.id,
        "date" to date.toString(),
        "etat" to (etat?.id ?: ""),
        "heureSup" to heureSup,
        "remplacement" to remplacement.trim(),
        "note" to note.trim(),
        "majLe" to majLe,
    )

    companion object {
        fun idDe(w: Who, d: LocalDate) = "${w.id}_$d"

        fun fromMap(m: Map<String, Any?>): Jour? {
            val w = Who.of(m["who"] as? String) ?: return null
            val d = anyToDate(m["date"]) ?: return null
            return Jour(
                w, d,
                etat = Etat.of(m["etat"] as? String),
                heureSup = m["heureSup"] == true,
                remplacement = (m["remplacement"] as? String).orEmpty(),
                note = (m["note"] as? String).orEmpty(),
                majLe = anyToLong(m["majLe"]),
            )
        }
    }
}

/** Ce qu'affiche le calendrier pour une personne un jour donné. */
data class JourVu(
    val etat: Etat,
    val auto: Boolean,
    val heureSup: Boolean,
    val remplacement: String,
    val note: String,
)

fun jourVu(r: Roulement, j: Jour?, d: LocalDate): JourVu {
    val auto = r.etatAuto(d)
    val etat = j?.etat ?: auto
    return JourVu(
        etat = etat,
        auto = j?.etat == null,
        heureSup = (j?.heureSup ?: false) && etat == Etat.TRAVAIL,
        remplacement = if (etat == Etat.TRAVAIL) j?.remplacement.orEmpty() else "",
        note = j?.note.orEmpty(),
    )
}

/**
 * Pose [etat] sur la période [du]…[au] pour [w].
 * Congé, récup et frac ne se posent que sur les jours travaillés (les repos restent des repos).
 * [etat] null = efface l'état posé (retour au roulement) sur toute la période.
 */
fun poserPeriode(
    r: Roulement,
    jours: Map<String, Jour>,
    w: Who,
    du: LocalDate,
    au: LocalDate,
    etat: Etat?,
    maintenant: Long,
): List<Jour> {
    val (a, b) = if (au.isBefore(du)) au to du else du to au
    val out = mutableListOf<Jour>()
    var d = a
    while (!d.isAfter(b)) {
        val ancien = jours[Jour.idDe(w, d)] ?: Jour(w, d)
        val modifiable = when (etat) {
            null, Etat.TRAVAIL, Etat.REPOS -> true
            else -> r.etatAuto(d) == Etat.TRAVAIL || (ancien.etat != null && ancien.etat != Etat.REPOS)
        }
        if (modifiable && ancien.etat != etat) out += ancien.copy(etat = etat, majLe = maintenant)
        d = d.plusDays(1)
    }
    return out
}

data class Compteur(
    val conges: Int = 0,
    val recups: Int = 0,
    val fracs: Int = 0,
    val heuresSup: Int = 0,
    val remplacements: List<Pair<LocalDate, String>> = emptyList(),
)

fun compter(jours: Collection<Jour>, w: Who, annee: Int): Compteur {
    val miens = jours.filter { it.who == w && it.date.year == annee }
    return Compteur(
        conges = miens.count { it.etat == Etat.CONGE },
        recups = miens.count { it.etat == Etat.RECUP },
        fracs = miens.count { it.etat == Etat.FRAC },
        heuresSup = miens.count { it.heureSup && (it.etat == null || it.etat == Etat.TRAVAIL) },
        remplacements = miens
            .filter { it.remplacement.isNotBlank() && (it.etat == null || it.etat == Etat.TRAVAIL) }
            .sortedBy { it.date }
            .map { it.date to it.remplacement },
    )
}

/** Rappel : minutes avant l'évènement (-1 = aucun). */
val RAPPELS = listOf(-1 to "Aucun", 0 to "À l'heure", 15 to "15 min", 60 to "1 h", 120 to "2 h", 1440 to "La veille")

data class Evenement(
    val id: String = UUID.randomUUID().toString(),
    val titre: String = "",
    val date: LocalDate = LocalDate.now(),
    val heure: String = "",
    val pour: String = "deux",
    val prive: Boolean = false,
    val auteur: String = "",
    val rappel: Int = 60,
    val note: String = "",
    val majLe: Long = 0,
) {
    val journee get() = heure.isBlank()

    fun visiblePar(moi: Who) = !prive || auteur == moi.id
    fun concerne(moi: Who) = pour == "deux" || pour == moi.id

    fun heureLocale(): LocalTime? = runCatching { LocalTime.parse(heure) }.getOrNull()

    /** Moment du rappel (ms), null si aucun. Journée entière : référence à 9h. */
    fun rappelA(zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (rappel < 0) return null
        val t = heureLocale() ?: LocalTime.of(9, 0)
        return LocalDateTime.of(date, t).minusMinutes(rappel.toLong()).atZone(zone).toInstant().toEpochMilli()
    }

    fun toMap(): Map<String, Any> = mapOf(
        "titre" to titre.trim(),
        "date" to date.toString(),
        "heure" to heure,
        "pour" to pour,
        "prive" to prive,
        "auteur" to auteur,
        "rappel" to rappel,
        "note" to note.trim(),
        "majLe" to majLe,
    )

    companion object {
        fun fromMap(id: String, m: Map<String, Any?>): Evenement? {
            val d = anyToDate(m["date"]) ?: return null
            return Evenement(
                id = id,
                titre = (m["titre"] as? String).orEmpty(),
                date = d,
                heure = (m["heure"] as? String).orEmpty(),
                pour = (m["pour"] as? String) ?: "deux",
                prive = m["prive"] == true,
                auteur = (m["auteur"] as? String).orEmpty(),
                rappel = anyToInt(m["rappel"]) ?: -1,
                note = (m["note"] as? String).orEmpty(),
                majLe = anyToLong(m["majLe"]),
            )
        }
    }
}

/** Trie : journées entières d'abord, puis par heure. */
fun trierEvenements(l: List<Evenement>) = l.sortedWith(compareBy({ it.date }, { if (it.journee) "" else it.heure }, { it.titre }))
