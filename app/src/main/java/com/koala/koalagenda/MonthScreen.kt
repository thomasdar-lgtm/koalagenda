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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
val CouleurFerie = Color(0xFFFF9F43)

/** Texte sans marge interne : centré pour de vrai dans les petites cases. */
val Serre = androidx.compose.ui.text.TextStyle(
    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
        androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
        androidx.compose.ui.text.style.LineHeightStyle.Trim.Both,
    ),
)

/** Saisie sur plusieurs jours (Ordo ou congés), sur un ou plusieurs mois. */
data class Selection(val ordo: Boolean, val etat: Etat, val avant: Set<LocalDate>, val jours: Set<LocalDate>)

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
    selection: Selection?,
    onSelection: (Selection?) -> Unit,
    onValider: (Selection) -> Unit,
) {
    var menuPlus by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = selection != null) { onSelection(null) }
    val clicJour: (LocalDate) -> Unit = { d ->
        if (selection != null) onSelection(selection.copy(jours = if (d in selection.jours) selection.jours - d else selection.jours + d))
        else onDay(d)
    }
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
                Box {
                    Box(
                        Modifier.padding(start = 8.dp).size(36.dp).clip(CircleShape).background(Yellow).clickable { menuPlus = true },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Add, "Ajouter", tint = Navy) }
                    androidx.compose.material3.DropdownMenu(expanded = menuPlus, onDismissRequest = { menuPlus = false }) {
                        androidx.compose.material3.DropdownMenuItem(text = { Text("Rdv") }, onClick = { menuPlus = false; onNewEvent() })
                        androidx.compose.material3.DropdownMenuItem(text = { Text("Ordo") }, onClick = {
                            menuPlus = false
                            val a = joursOrdo(state.jours, moi)
                            onSelection(Selection(true, Etat.TRAVAIL, a, a))
                        })
                        androidx.compose.material3.DropdownMenuItem(text = { Text("Congés") }, onClick = {
                            menuPlus = false
                            val a = joursEtat(state.jours, moi, Etat.CONGE)
                            onSelection(Selection(false, Etat.CONGE, a, a))
                        })
                    }
                }
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
                ) { m -> Grille(state, m, today, parJour, selection?.jours, clicJour) }
            }
            if (selection != null) BarreSelection(state, moi, selection, onSelection, onValider)
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
    choisis: Set<LocalDate>?,
    onDay: (LocalDate) -> Unit,
) {
    val debut = lundi(mois.atDay(1))
    Column(Modifier.fillMaxSize()) {
        repeat(6) { s ->
            Row(Modifier.fillMaxWidth().weight(1f)) {
                repeat(7) { j ->
                    val d = debut.plusDays((s * 7 + j).toLong())
                    Case(
                        state, d, d.month == mois.month, d == today, parJour[d].orEmpty(), choisis?.contains(d),
                        Modifier.weight(1f).fillMaxHeight().padding(1.5.dp),
                    ) { onDay(d) }
                }
            }
        }
    }
}

val BleuRemplacement = Color(0xFF3D8BFF)

@Composable
private fun Case(
    state: AgendaState,
    d: LocalDate,
    dansLeMois: Boolean,
    aujourdhui: Boolean,
    evts: List<Evenement>,
    choisi: Boolean?,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val fond = if (state.enfants.presents(d)) FondEnfants else Navy2
    BoxWithConstraints(
        modifier.alpha(if (dansLeMois || choisi == true) 1f else 0.3f).clip(RoundedCornerShape(10.dp)).background(fond)
            .then(if (choisi == true) Modifier.border(2.5.dp, Yellow, RoundedCornerShape(10.dp)) else Modifier)
            .clickable { onClick() }.padding(3.dp),
    ) {
        val gap = 3.dp
        val slot = minOf((maxWidth - gap) / 2, 26.dp)
        val barre = 17.dp
        val place = maxHeight - 22.dp - gap - slot - gap
        val lignes = (place / (barre + 2.dp)).toInt().coerceIn(0, 3)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(22.dp).clip(CircleShape).background(if (aujourdhui) Yellow else Color.Transparent)
                    .then(if (ferie(d) != null && !aujourdhui) Modifier.border(1.5.dp, CouleurFerie, CircleShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${d.dayOfMonth}", fontSize = 12.sp, fontWeight = FontWeight.Black, style = Serre,
                    textAlign = TextAlign.Center,
                    color = when { aujourdhui -> Navy; ferie(d) != null -> CouleurFerie; else -> Color.White },
                )
            }
            Spacer(Modifier.height(gap))
            // Ligne des repos : Thomas à gauche, Cindy à droite
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Who.entries.forEach { w -> Emplacement(state.vu(w, d), whoColor(w), slot) }
            }
            // Rdv et évènements, une ligne chacun
            if (lignes > 0 && evts.isNotEmpty()) {
                Spacer(Modifier.height(gap))
                val montres = if (evts.size > lignes) evts.take(lignes - 1) else evts
                montres.forEach { e -> Barre(e.titre.ifBlank { "Rdv" }, pourColor(e.pour), barre) }
                if (evts.size > lignes) Barre("+${evts.size - montres.size}", Muted, barre)
            }
        }
    }
}

/** Repos/congé (carré plein), sinon remplacement (petit carré bleu) et heure sup. */
@Composable
private fun Emplacement(v: JourVu, couleur: Color, taille: androidx.compose.ui.unit.Dp) = Box(
    Modifier.size(taille), contentAlignment = Alignment.Center,
) {
    val code = when (v.etat) {
        Etat.CONGE -> "CP"; Etat.RECUP -> "RC"; Etat.FRAC -> "FR"; else -> ""
    }
    when {
        v.etat.off -> Box(
            Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(couleur),
            contentAlignment = Alignment.Center,
        ) { if (code.isNotEmpty()) Text(code, color = Navy, fontSize = 10.sp, fontWeight = FontWeight.Black, style = Serre) }

        v.remplacement.isNotBlank() -> Box(
            Modifier.size(taille * 0.62f).clip(RoundedCornerShape(4.dp)).background(BleuRemplacement),
            contentAlignment = Alignment.Center,
        ) { if (v.heureSup) Text("+1", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, style = Serre) }

        v.heureSup -> Text("+1", color = couleur, fontSize = 11.sp, fontWeight = FontWeight.Black, style = Serre)
    }
}

@Composable
private fun Barre(texte: String, couleur: Color, hauteur: androidx.compose.ui.unit.Dp) = Box(
    Modifier.fillMaxWidth().padding(bottom = 2.dp).height(hauteur).clip(RoundedCornerShape(4.dp)).background(couleur)
        .padding(horizontal = 3.dp),
    contentAlignment = Alignment.CenterStart,
) {
    Text(texte, color = Navy, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Clip, style = Serre)
}

@Composable
private fun BarreSelection(
    state: AgendaState,
    moi: Who,
    sel: Selection,
    onSelection: (Selection?) -> Unit,
    onValider: (Selection) -> Unit,
) = Column(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).background(Navy3)
        .padding(horizontal = 16.dp, vertical = 12.dp),
) {
    if (!sel.ordo) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Etat.CONGE, Etat.RECUP, Etat.FRAC).forEach { e ->
                Choix(e.label, sel.etat == e, whoColor(moi)) {
                    val a = joursEtat(state.jours, moi, e)
                    onSelection(Selection(false, e, a, a))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${if (sel.ordo) "Ordo" else sel.etat.label} · ${sel.jours.size} j",
            color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp, modifier = Modifier.weight(1f),
        )
        androidx.compose.material3.TextButton(onClick = { onSelection(null) }) { Text("Annuler", color = Muted) }
        androidx.compose.material3.Button(onClick = { onValider(sel) }) { Text("Valider", fontWeight = FontWeight.Bold) }
    }
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
