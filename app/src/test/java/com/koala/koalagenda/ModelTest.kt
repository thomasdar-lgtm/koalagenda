package com.koala.koalagenda

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ModelTest {
    private fun d(s: String) = LocalDate.parse(s)
    private val thomas = Roulement.parDefaut(Who.THOMAS)
    private val cindy = Roulement.parDefaut(Who.CINDY)

    private fun reposDeLaSemaine(r: Roulement, lundi: String): List<Int> {
        val l = d(lundi)
        return (0..6).map { l.plusDays(it.toLong()) }.filter { r.etatAuto(it) == Etat.REPOS }.map { it.dayOfWeek.value }
    }

    @Test fun roulementThomas() {
        assertEquals(listOf(6, 7), reposDeLaSemaine(thomas, "2026-11-16")) // samedi
        assertEquals(listOf(5, 7), reposDeLaSemaine(thomas, "2026-11-23")) // vendredi
        assertEquals(listOf(4, 7), reposDeLaSemaine(thomas, "2026-11-30")) // jeudi
        assertEquals(listOf(1, 7), reposDeLaSemaine(thomas, "2026-12-07")) // lundi
        assertEquals(listOf(6, 7), reposDeLaSemaine(thomas, "2026-12-14")) // on reprend
        assertEquals(listOf(1, 7), reposDeLaSemaine(thomas, "2026-11-09")) // semaine d'avant
        assertEquals(listOf(4, 7), reposDeLaSemaine(thomas, "2026-11-02"))
    }

    @Test fun roulementCindy() {
        assertEquals(listOf(1, 7), reposDeLaSemaine(cindy, "2026-11-16"))
        assertEquals(listOf(2, 7), reposDeLaSemaine(cindy, "2026-11-23"))
        assertEquals(listOf(6, 7), reposDeLaSemaine(cindy, "2026-11-30"))
        assertEquals(listOf(5, 7), reposDeLaSemaine(cindy, "2026-12-07"))
        assertEquals(listOf(1, 7), reposDeLaSemaine(cindy, "2026-12-14"))
    }

    @Test fun semaineActuelle() {
        val r = thomas.avecSemaineActuelle(d("2026-11-19"), 2)
        assertEquals(listOf(4, 7), reposDeLaSemaine(r, "2026-11-16"))
        assertEquals(listOf(1, 7), reposDeLaSemaine(r, "2026-11-23"))
    }

    @Test fun allerRetourMap() {
        assertEquals(thomas, Roulement.fromMap(thomas.toMap()))
        val j = Jour(Who.CINDY, d("2026-12-01"), Etat.RECUP, true, "ordo", "x", 5)
        assertEquals(j, Jour.fromMap(j.toMap()))
        val e = Evenement("a", "Dentiste", d("2026-12-02"), "14:30", "thomas", true, "thomas", 60, "", 1)
        assertEquals(e, Evenement.fromMap("a", e.toMap()))
    }

    @Test fun pasDeTableauDansUnTableau() {
        // Firestore plante si une liste contient une liste
        listOf(thomas, cindy, Roulement(cycle = listOf(emptySet(), setOf(1, 4)))).forEach { r ->
            r.toMap().values.filterIsInstance<List<*>>().forEach { l -> assertTrue(l.none { it is List<*> }) }
            assertEquals(r, Roulement.fromMap(r.toMap()))
        }
    }

    @Test fun modificationDuJour() {
        val j = Jour(Who.THOMAS, d("2026-11-21"), Etat.TRAVAIL, heureSup = true, remplacement = "ordo")
        val v = jourVu(thomas, j, j.date)
        assertEquals(Etat.TRAVAIL, v.etat)
        assertTrue(v.heureSup)
        assertEquals("ordo", v.remplacement)
        assertEquals(Etat.REPOS, jourVu(thomas, null, d("2026-11-21")).etat)
    }

    @Test fun congesSurPeriode() {
        val l = poserPeriode(thomas, emptyMap(), Who.THOMAS, d("2026-12-21"), d("2026-12-27"), Etat.CONGE, 1)
        // semaine du 21/12 = 5 semaines après le 16/11 → indice 1 → vendredi
        assertEquals(listOf(21, 22, 23, 24, 26), l.map { it.date.dayOfMonth })
        val c = compter(l, Who.THOMAS, 2026)
        assertEquals(5, c.conges)
        val efface = poserPeriode(thomas, l.associateBy { it.id }, Who.THOMAS, d("2026-12-21"), d("2026-12-27"), null, 2)
        assertEquals(5, efface.size)
    }

    @Test fun enfantsUneSemaineSurDeux() {
        val e = Enfants()
        assertTrue(e.presents(d("2026-11-16")) && e.presents(d("2026-11-22")))
        assertTrue(!e.presents(d("2026-11-23")) && !e.presents(d("2026-11-29")))
        assertTrue(e.presents(d("2026-11-30")) && e.presents(d("2026-11-09").minusWeeks(1)))
        assertTrue(!e.presents(d("2026-11-09")))
        val inv = e.avec(d("2026-11-18"), false)
        assertTrue(!inv.presents(d("2026-11-18")) && inv.presents(d("2026-11-25")))
        assertTrue(!Enfants(actif = false).presents(d("2026-11-16")))
        assertEquals(e, Enfants.fromMap(e.toMap()))
    }

    @Test fun rappel() {
        val e = Evenement(date = d("2026-12-02"), heure = "14:30", rappel = 60)
        val attendu = d("2026-12-02").atTime(13, 30).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals(attendu, e.rappelA(ZoneOffset.UTC))
        assertEquals(null, e.copy(rappel = -1).rappelA(ZoneOffset.UTC))
    }
}
