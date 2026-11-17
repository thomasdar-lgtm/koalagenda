package com.koala.koalagenda

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
    val visibles = state.evenements.filter { it.visiblePar(moi) }
    val parJour = visibles.groupBy { it.date }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            // En-tête
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Logo(20)
                Text(
                    BuildConfig.VERSION_NAME, color = Muted.copy(alpha = 0.6f), fontSize = 10.sp,
                    modifier = Modifier.padding(start = 6.dp, top = 8.dp),
                )
                Spacer(Modifier.weight(1f))
                if (state.pending) Box(Modifier.padding(end = 10.dp).size(7.dp).clip(CircleShape).background(Yellow))
                Avatar(moi, 30)
                Box(
                    Modifier.padding(start = 6.dp).size(40.dp).clip(CircleShape).clickable { onReglages() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Settings, "Réglages", tint = Muted) }
            }
            UpdateBanner(upd)
            state.error?.let {
                Text("⚠ $it", color = Red, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                // Mois
                Row(
                    Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        MOIS[mois.monthValue - 1].replaceFirstChar { it.uppercase() },
                        color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp,
                    )
                    Text(
                        " ${mois.year}", color = Muted.copy(alpha = 0.55f), fontWeight = FontWeight.Bold, fontSize = 28.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (mois != YearMonth.from(today)) {
                        Text(
                            "Auj.", color = Navy, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            modifier = Modifier.padding(end = 6.dp).clip(RoundedCornerShape(50)).background(Yellow)
                                .clickable { onMois(YearMonth.from(today)) }.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                    RondBouton({ onMois(mois.minusMonths(1)) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mois précédent", tint = Color.White)
                    }
                    Spacer(Modifier.width(6.dp))
                    RondBouton({ onMois(mois.plusMonths(1)) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mois suivant", tint = Color.White)
                    }
                }

                // Calendrier (glisser à gauche / à droite pour changer de mois)
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 6.dp).pointerInput(mois) {
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
                        label = "mois",
                    ) { m -> Grille(state, m, today, parJour, onDay) }
                }

                // Rdv du mois
                val duMois = visibles.filter { YearMonth.from(it.date) == mois }
                if (duMois.isNotEmpty()) {
                    Column(Modifier.padding(horizontal = 12.dp)) {
                        Section(MOIS[mois.monthValue - 1])
                        duMois.forEach { e -> LigneEvenement(e, showDate = true) { onEvent(e) } }
                    }
                }
                Spacer(Modifier.height(96.dp))
            }
        }
        FloatingActionButton(
            onClick = onNewEvent,
            containerColor = Yellow, contentColor = Navy, shape = CircleShape,
            modifier = Modifier.align(Alignment.BottomEnd).safeDrawingPadding().padding(20.dp),
        ) { Icon(Icons.Filled.Add, "Nouveau rdv") }
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
    val semaines = (ChronoUnit.DAYS.between(debut, mois.atEndOfMonth()) / 7 + 1).toInt()
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            JOURS_COURTS.forEachIndexed { i, l ->
                Text(
                    l, color = if (i >= 5) Yellow.copy(alpha = 0.7f) else Muted.copy(alpha = 0.7f),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        repeat(semaines) { s ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { j ->
                    val d = debut.plusDays((s * 7 + j).toLong())
                    Case(
                        state, d, d.month == mois.month, d == today, parJour[d].orEmpty(),
                        Modifier.weight(1f).padding(1.5.dp),
                    ) { onDay(d) }
                }
            }
        }
    }
}

/** Ce qui concerne une personne un jour donné. Deux parts identiques = case non séparée. */
private data class Part(val who: Who, val vu: JourVu, val solo: List<Evenement>) {
    val signature get() = listOf(vu.etat, vu.heureSup, vu.remplacement.isNotBlank(), solo.isNotEmpty())
    val badges: List<String> get() = buildList {
        when (vu.etat) {
            Etat.CONGE -> add("CP")
            Etat.RECUP -> add("RC")
            Etat.FRAC -> add("FR")
            else -> {}
        }
        if (vu.heureSup) add("+1")
        if (vu.remplacement.isNotBlank()) add("⇄")
    }
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
    val t = Part(Who.THOMAS, state.vu(Who.THOMAS, d), evts.filter { it.pour == Who.THOMAS.id })
    val c = Part(Who.CINDY, state.vu(Who.CINDY, d), evts.filter { it.pour == Who.CINDY.id })
    val communs = evts.filter { it.pour != Who.THOMAS.id && it.pour != Who.CINDY.id }
    val separee = t.signature != c.signature
    val ct = whoColor(Who.THOMAS)
    val cc = whoColor(Who.CINDY)
    val ensembleOff = !separee && t.vu.etat.off
    val forme = RoundedCornerShape(10.dp)

    Box(
        modifier.height(88.dp).alpha(if (dansLeMois) 1f else 0.3f).clip(forme)
            .then(
                when {
                    ensembleOff -> Modifier.background(Brush.linearGradient(listOf(ct, cc)))
                    else -> Modifier.background(Navy2)
                }
            )
            .clickable { onClick() },
    ) {
        if (separee) Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight().background(if (t.vu.etat.off) ct else Color.Transparent))
            Box(Modifier.width(1.dp).fillMaxHeight().background(Navy))
            Box(Modifier.weight(1f).fillMaxHeight().background(if (c.vu.etat.off) cc else Color.Transparent))
        }
        Column(Modifier.fillMaxSize().padding(3.dp)) {
            // Numéro du jour
            val surCouleur = ensembleOff || (separee && t.vu.etat.off)
            Box(
                Modifier.size(20.dp).clip(CircleShape).background(if (aujourdhui) Yellow else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${d.dayOfMonth}", fontSize = 12.sp, fontWeight = FontWeight.Black,
                    color = when { aujourdhui -> Navy; surCouleur -> Navy; else -> Color.White },
                )
            }
            Spacer(Modifier.height(2.dp))
            if (separee) {
                Row(Modifier.fillMaxWidth()) {
                    Marques(t, Modifier.weight(1f))
                    Spacer(Modifier.width(1.dp))
                    Marques(c, Modifier.weight(1f))
                }
            } else {
                Marques(t, Modifier.fillMaxWidth(), initiales = false, neutre = true)
            }
            Spacer(Modifier.weight(1f))
            communs.take(2).forEach { e -> Puce(e.titre.ifBlank { "Rdv" }) }
            if (communs.size > 2) Text(
                "+${communs.size - 2}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            )
        }
    }
}

/** Badges (congé, heure sup, remplacement) et initiales des rdv personnels. */
@Composable
private fun Marques(p: Part, modifier: Modifier, initiales: Boolean = true, neutre: Boolean = false) = Column(
    modifier, horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(2.dp),
) {
    val couleur = whoColor(p.who)
    val surCouleur = p.vu.etat.off
    p.badges.forEach { b ->
        Text(
            b, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1,
            color = if (surCouleur) Navy else if (neutre) Color.White else couleur,
        )
    }
    if (initiales && p.solo.isNotEmpty()) {
        Box(
            Modifier.size(15.dp).clip(CircleShape).background(Navy),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (p.solo.size > 1) "${p.who.initiale}${p.solo.size}" else p.who.initiale,
                color = couleur, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1,
            )
        }
    }
}

@Composable
private fun Puce(texte: String) = Text(
    texte, color = Navy, fontSize = 9.sp, fontWeight = FontWeight.Bold,
    maxLines = 1, overflow = TextOverflow.Clip,
    modifier = Modifier.fillMaxWidth().padding(top = 2.dp).clip(RoundedCornerShape(4.dp))
        .background(Yellow).padding(horizontal = 3.dp, vertical = 1.dp),
)

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
