package com.koala.koalagenda

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Ce dont le widget a besoin, copié en local à chaque changement (le widget ne lit pas Firebase). */
data class WidgetData(
    val moi: Who?,
    val roulements: Map<Who, Roulement>,
    val jours: Map<String, Jour>,
    val evenements: List<Evenement>,
    val couleurs: Map<Who, Long>,
    val enfants: Enfants,
)

object WidgetStore {
    private const val PREFS = "widget"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private fun jsonVersAny(v: Any?): Any? = when (v) {
        is JSONObject -> v.keys().asSequence().associateWith { jsonVersAny(v.opt(it)) }
        is JSONArray -> (0 until v.length()).map { jsonVersAny(v.opt(it)) }
        JSONObject.NULL -> null
        else -> v
    }

    @Suppress("UNCHECKED_CAST")
    private fun map(v: Any?) = (jsonVersAny(v) as? Map<String, Any?>) ?: emptyMap()

    fun sauver(ctx: Context, s: AgendaState, moi: Who?) {
        val today = LocalDate.now()
        val o = JSONObject()
        o.put("moi", moi?.id ?: "")
        o.put("roulements", JSONObject().apply { s.roulements.forEach { (w, r) -> put(w.id, JSONObject(r.toMap())) } })
        o.put("couleurs", JSONObject().apply { s.couleurs.forEach { (w, c) -> put(w.id, c) } })
        o.put("enfants", JSONObject(s.enfants.toMap()))
        val debut = lundi(today).minusDays(1)
        val fin = today.plusDays(21)
        o.put("jours", JSONArray().apply {
            s.jours.values.filter { it.date.isAfter(debut) && it.date.isBefore(fin) }.forEach { put(JSONObject(it.toMap())) }
        })
        o.put("evenements", JSONArray().apply {
            s.evenements
                .filter { (moi == null || it.visiblePar(moi)) && !it.date.isBefore(lundi(today)) }
                .sortedWith(compareBy({ it.date }, { it.heure }))
                .take(20)
                .forEach { put(JSONObject(it.toMap()).put("id", it.id)) }
        })
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("data", o.toString()).apply()
        // Regroupe les mises à jour rapprochées
        job?.cancel()
        job = scope.launch {
            delay(800)
            runCatching { AgendaWidget().updateAll(ctx) }
        }
    }

    fun lire(ctx: Context): WidgetData? = runCatching {
        val t = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("data", null) ?: return null
        val o = JSONObject(t)
        val r = o.getJSONObject("roulements")
        val c = o.getJSONObject("couleurs")
        val j = o.getJSONArray("jours")
        val e = o.getJSONArray("evenements")
        WidgetData(
            moi = Who.of(o.optString("moi")),
            roulements = Who.entries.associateWith { w ->
                if (r.has(w.id)) Roulement.fromMap(map(r.get(w.id))) else Roulement.parDefaut(w)
            },
            jours = (0 until j.length()).mapNotNull { Jour.fromMap(map(j.get(it))) }.associateBy { it.id },
            evenements = (0 until e.length()).mapNotNull { i ->
                val m = map(e.get(i)); Evenement.fromMap((m["id"] as? String).orEmpty(), m)
            },
            couleurs = Who.entries.associateWith { w -> if (c.has(w.id)) c.getLong(w.id) else COULEURS_DEFAUT.getValue(w) },
            enfants = Enfants.fromMap(map(o.opt("enfants"))),
        )
    }.getOrNull()
}

class AgendaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetStore.lire(context)
        provideContent { Contenu(data) }
    }
}

class AgendaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AgendaWidget()
}

private fun cp(c: Color) = ColorProvider(c)

@androidx.compose.runtime.Composable
private fun Contenu(data: WidgetData?) {
    val today = LocalDate.now()
    Column(
        GlanceModifier.fillMaxSize().background(Navy).cornerRadius(20.dp).padding(10.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        if (data == null) {
            Text("Ouvre Koalagenda une fois", style = TextStyle(color = cp(Muted), fontSize = 13.sp))
            return@Column
        }
        val lun = lundi(today)
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Koal", style = TextStyle(color = cp(Blue), fontSize = 14.sp, fontWeight = FontWeight.Bold))
            Text("agenda", style = TextStyle(color = cp(Yellow), fontSize = 14.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.defaultWeight())
            Text(
                "Semaine du ${lun.dayOfMonth} ${MOIS[lun.monthValue - 1].take(4)}.",
                style = TextStyle(color = cp(Muted), fontSize = 11.sp),
            )
        }
        Spacer(GlanceModifier.height(6.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            for (i in 0..6) {
                val d = lun.plusDays(i.toLong())
                val fond = if (data.enfants.presents(d)) FondEnfants else Navy2
                Column(
                    GlanceModifier.defaultWeight().padding(horizontal = 1.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        GlanceModifier.fillMaxWidth().background(fond).cornerRadius(8.dp).padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            JOURS_COURTS[i],
                            style = TextStyle(color = cp(if (i >= 5) Yellow else Muted), fontSize = 10.sp, textAlign = TextAlign.Center),
                        )
                        Box(
                            GlanceModifier.size(22.dp).cornerRadius(11.dp).background(if (d == today) Yellow else fond),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${d.dayOfMonth}",
                                style = TextStyle(
                                    color = cp(if (d == today) Navy else if (ferie(d) != null) CouleurFerie else Color.White), fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                                ),
                            )
                        }
                        Spacer(GlanceModifier.height(3.dp))
                        Row(horizontalAlignment = Alignment.CenterHorizontally) {
                            Who.entries.forEachIndexed { k, w ->
                                if (k > 0) Spacer(GlanceModifier.width(2.dp))
                                val v = jourVu(data.roulements.getValue(w), data.jours[Jour.idDe(w, d)], d)
                                val coul = when {
                                    v.etat.off -> Color(data.couleurs[w] ?: COULEURS_DEFAUT.getValue(w))
                                    v.remplacement.isNotBlank() -> BleuRemplacement
                                    else -> fond
                                }
                                Box(GlanceModifier.size(12.dp).cornerRadius(3.dp).background(coul)) {}
                            }
                        }
                    }
                }
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        val prochains = data.evenements.filter { !it.date.isBefore(today) }.take(3)
        if (prochains.isEmpty()) {
            Text("Aucun rdv à venir", style = TextStyle(color = cp(Muted), fontSize = 12.sp))
        }
        prochains.forEach { e ->
            val coul = Who.of(e.pour)?.let { Color(data.couleurs[it] ?: COULEURS_DEFAUT.getValue(it)) } ?: Yellow
            Row(GlanceModifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(GlanceModifier.width(4.dp).height(14.dp).cornerRadius(2.dp).background(coul)) {}
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    buildString {
                        append(JOURS_LONGS[e.date.dayOfWeek.value - 1].take(3).replaceFirstChar { it.uppercase() })
                        append(" ").append(e.date.dayOfMonth)
                        if (!e.journee) append(" · ").append(e.heure.replace(":", "h"))
                        append(" · ").append(e.titre.ifBlank { "Rdv" })
                    },
                    style = TextStyle(color = cp(Color.White), fontSize = 12.sp),
                    maxLines = 1,
                )
            }
        }
    }
}
