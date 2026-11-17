package com.koala.koalagenda

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

val FondEnfants = Color(0xFF123B45)

@Composable
fun MonthScreen(
    state: AgendaState,
    moi: Who,
    upd: UpdateState,
    mois: YearMonth,
    onMois: (YearMonth) -> Unit,
    onDay: (LocalDate) -> Unit,
    onEvent: (Evenement) -> Unit,
    onNewEvent: () -> Unit,
    onReglages: () -> Unit,
) {
    val today = LocalDate.now()
    val parJour = state.evenements.filter { it.visiblePar(moi) }.groupBy { it.date }
    val estMoisCourant = mois == YearMonth.from(today)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            // En-tête
            Row(
                Modifier.fillMaxWidth().height(48.dp).padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Logo(20)
                Text(
                    BuildConfig.VERSION_NAME, color = Muted.copy(alpha = 0.6f), fontSize = 10.sp,
                    modifier = Modifier.padding(start = 6.dp, top = 8.dp),
                )
                Spacer(Modifier.weight(1f))
                Box(Modifier.padding(end = 10.dp).size(7.dp).clip(CircleShape).background(if (state.pending) Yellow else Color.Transparent))
                Avatar(moi, 30)
                Box(
                    Modifier.padding(start = 8.dp).size(36.dp).clip(CircleShape).background(Yellow).clickable { onNewEvent() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Add, "Nouveau rdv", tint = Navy) }
                Box(
                    Modifier.padding(start = 6.dp).size(40.dp).clip(CircleShape).clickable { onReglages() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Settings, "Réglages", tint = Muted) }
            }
            UpdateBanner(upd)
            state.error?.let {
                Text("⚠ $it", color = Red, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(horizontal = 16.dp))
            }

            // Mois (hauteur fixe : rien ne bouge en changeant de mois)
            Row(
                Modifier.fillMaxWidth().height(52.dp).padding(start = 18.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    MOIS[mois.monthValue - 1].replaceFirstChar { it.uppercase() },
                    color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp, maxLines = 1,
                )
                Text(
                    " ${mois.year}", color = Muted.copy(alpha = 0.55f), fontWeight = FontWeight.Bold, fontSize = 26.sp,
                    maxLines = 1, modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.padding(end = 6.dp).alpha(if (estMoisCourant) 0f else 1f).clip(RoundedCornerShape(50))
                        .background(Yellow)
                        .clickable(enabled = !estMoisCourant) { onMois(YearMonth.from(today)) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) { Text("Auj.", color = Navy, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                RondBouton({ onMois(mois.minusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mois précédent", tint = Color.White)
                }
                Spacer(Modifier.width(6.dp))
                RondBouton({ onMois(mois.plusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mois suivant", tint = Color.White)
                }
            }

            // Jours de la semaine
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)) {
                JOURS_COURTS.forEachIndexed { i, l ->
                    Text(
                        l, color = if (i >= 5) Yellow.copy(alpha = 0.7f) else Muted.copy(alpha = 0.7f),
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Calendrier : 6 semaines, remplit l'écran ; glisser pour changer de mois
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 6.dp).pointerInput(mois) {
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = {
                            if (total > 120f) onMois(mois.minusMonths(1))
                            else if (total < -120f) onMois(mois.plusMonths(1))
                        },
                    ) { _, d -> total += d }
                },
            ) {
                AnimatedContent(
                    targetState = mois,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally { it * dir } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it * dir } + fadeOut())
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "mois",
                ) { m -> Grille(state, m, today, parJour, onDay) }
            }
        }
    }
}

@Composable
private fun RondBouton(onClick: () -> Unit, content: @Composable () -> Unit) = Box(
    Modifier.size(38.dp).clip(CircleShape).background(Navy3).clickable { onClick() },
    contentAlignment = Alignment.Center,
) { content() }

@Composable
private fun Grille(
    state: AgendaState,
    mois: YearMonth,
    today: LocalDate,
    parJour: Map<LocalDate, List<Evenement>>,
    onDay: (LocalDate) -> Unit,
) {
    val debut = lundi(mois.atDay(1))
    Column(Modifier.fillMaxSize()) {
        repeat(6) { s ->
            Row(Modifier.fillMaxWidth().weight(1f)) {
                repeat(7) { j ->
                    val d = debut.plusDays((s * 7 + j).toLong())
                    Case(
                        state, d, d.month == mois.month, d == today, parJour[d].orEmpty(),
                        Modifier.weight(1f).fillMaxHeight().padding(1.5.dp),
                    ) { onDay(d) }
                }
            }
        }
    }
}

/** Une case carrée dans un jour : repos/congé (pleine) ou évènement (contour). */
private data class Tuile(val couleur: Color, val texte: String, val pleine: Boolean)

@Composable
private fun tuiles(state: AgendaState, d: LocalDate, evts: List<Evenement>): List<Tuile> {
    val l = mutableListOf<Tuile>()
    Who.entries.forEach { w ->
        val c = whoColor(w)
        val v = state.vu(w, d)
        when (v.etat) {
            Etat.REPOS -> l += Tuile(c, "", true)
            Etat.CONGE -> l += Tuile(c, "CP", true)
            Etat.RECUP -> l += Tuile(c, "RC", true)
            Etat.FRAC -> l += Tuile(c, "FR", true)
            Etat.TRAVAIL -> {}
        }
        if (v.heureSup) l += Tuile(c, "+1", false)
        if (v.remplacement.isNotBlank()) l += Tuile(c, "⇄", false)
        evts.filter { it.pour == w.id }.forEach { l += Tuile(c, w.initiale, false) }
    }
    evts.filter { Who.of(it.pour) == null }.forEach { e ->
        l += Tuile(Yellow, e.titre.trim().take(1).uppercase().ifBlank { "•" }, false)
    }
    return l
}

@Composable
private fun Case(
    state: AgendaState,
    d: LocalDate,
    dansLeMois: Boolean,
    aujourdhui: Boolean,
    evts: List<Evenement>,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val items = tuiles(state, d, evts)
    val fond = if (state.enfants.presents(d)) FondEnfants else Navy2
    BoxWithConstraints(
        modifier.alpha(if (dansLeMois) 1f else 0.3f).clip(RoundedCornerShape(10.dp)).background(fond)
            .clickable { onClick() }.padding(3.dp),
    ) {
        val gap = 3.dp
        val tuile = (maxWidth - gap) / 2
        val lignes = ((maxHeight - 18.dp - gap) / (tuile + gap)).toInt().coerceAtLeast(1)
        val max = lignes * 2
        val montrees = if (items.size > max) items.take(max - 1) + Tuile(Muted, "+${items.size - max + 1}", false) else items
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            Box(
                Modifier.size(18.dp).clip(CircleShape).background(if (aujourdhui) Yellow else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${d.dayOfMonth}", fontSize = 11.sp, fontWeight = FontWeight.Black,
                    color = if (aujourdhui) Navy else Color.White,
                )
            }
            montrees.chunked(2).forEach { ligne ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    ligne.forEach { t -> Carre(t, tuile) }
                }
            }
        }
    }
}

@Composable
private fun Carre(t: Tuile, taille: androidx.compose.ui.unit.Dp) = Box(
    Modifier.size(taille).clip(RoundedCornerShape(5.dp))
        .then(
            if (t.pleine) Modifier.background(t.couleur)
            else Modifier.background(Navy).border(1.5.dp, t.couleur, RoundedCornerShape(5.dp))
        ),
    contentAlignment = Alignment.Center,
) {
    if (t.texte.isNotEmpty()) Text(
        t.texte, color = if (t.pleine) Navy else t.couleur,
        fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1,
    )
}

@Composable
fun LigneEvenement(e: Evenement, showDate: Boolean, onClick: () -> Unit) = Row(
    Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp)).background(Navy2)
        .clickable { onClick() }.padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    if (showDate) {
        Column(Modifier.width(38.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${e.date.dayOfMonth}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text(JOURS_LONGS[e.date.dayOfWeek.value - 1].take(3), color = Muted, fontSize = 11.sp)
        }
        Spacer(Modifier.width(10.dp))
    }
    Box(Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(pourColor(e.pour)))
    Spacer(Modifier.width(10.dp))
    Column(Modifier.weight(1f)) {
        Text(
            e.titre.ifBlank { "Rdv" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            buildString {
                append(if (e.journee) "Journée" else e.heure.replace(":", "h"))
                append(" · ").append(pourLabel(e.pour))
                if (e.prive) append(" · 🔒")
                if (e.rappel >= 0) append(" · 🔔")
            },
            color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
