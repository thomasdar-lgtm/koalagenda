package com.koala.koalagenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

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
    onRoulements: () -> Unit,
    onCompteurs: () -> Unit,
    onChangeMoi: () -> Unit,
    onSignOut: () -> Unit,
) {
    val today = LocalDate.now()
    var menu by remember { mutableStateOf(false) }
    val visibles = state.evenements.filter { it.visiblePar(moi) }
    val parJour = visibles.groupBy { it.date }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Logo(22)
                Spacer(Modifier.weight(1f))
                if (state.pending) Text("synchro…", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp))
                Box {
                    Avatar(moi, 36) { menu = true }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Roulements") }, onClick = { menu = false; onRoulements() })
                        DropdownMenuItem(text = { Text("Compteurs (congés, récups…)") }, onClick = { menu = false; onCompteurs() })
                        DropdownMenuItem(text = { Text("Je suis ${moi.label} · changer") }, onClick = { menu = false; onChangeMoi() })
                        DropdownMenuItem(text = { Text("Se déconnecter") }, onClick = { menu = false; onSignOut() })
                        DropdownMenuItem(text = { Text("Version ${BuildConfig.VERSION_NAME}", color = Muted) }, onClick = { menu = false })
                    }
                }
            }
            UpdateBanner(upd)
            state.error?.let {
                Text(
                    "Erreur de synchro : $it", color = Red, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onMois(mois.minusMonths(1)) }) {
                        Icon(Icons.Filled.KeyboardArrowLeft, "Mois précédent", tint = Color.White)
                    }
                    Text(
                        "${MOIS[mois.monthValue - 1].replaceFirstChar { it.uppercase() }} ${mois.year}",
                        color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (mois != YearMonth.from(today)) {
                        TextButton(onClick = { onMois(YearMonth.from(today)) }) { Text("Aujourd'hui", color = Yellow) }
                    }
                    IconButton(onClick = { onMois(mois.plusMonths(1)) }) {
                        Icon(Icons.Filled.KeyboardArrowRight, "Mois suivant", tint = Color.White)
                    }
                }
                Grille(state, mois, today, parJour, onDay)
                Legende()

                Section("Rdv et évènements du mois")
                val duMois = visibles.filter { YearMonth.from(it.date) == mois }
                if (duMois.isEmpty()) {
                    Text("Rien de prévu ce mois-ci", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 6.dp))
                }
                duMois.forEach { e -> LigneEvenement(e, showDate = true) { onEvent(e) } }
                Spacer(Modifier.height(96.dp))
            }
        }
        ExtendedFloatingActionButton(
            onClick = onNewEvent,
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("Rdv", fontWeight = FontWeight.Bold) },
            containerColor = Yellow, contentColor = Navy,
            modifier = Modifier.align(Alignment.BottomEnd).safeDrawingPadding().padding(20.dp),
        )
    }
}

@Composable
private fun Grille(
    state: AgendaState,
    mois: YearMonth,
    today: LocalDate,
    parJour: Map<LocalDate, List<Evenement>>,
    onDay: (LocalDate) -> Unit,
) {
    val premier = mois.atDay(1)
    val debut = lundi(premier)
    val fin = mois.atEndOfMonth()
    val semaines = ((java.time.temporal.ChronoUnit.DAYS.between(debut, fin) / 7) + 1).toInt()
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            JOURS_COURTS.forEach {
                Text(
                    it, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        repeat(semaines) { s ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { j ->
                    val d = debut.plusDays((s * 7 + j).toLong())
                    Case(state, d, d.month == mois.month, d == today, parJour[d].orEmpty(), Modifier.weight(1f)) { onDay(d) }
                }
            }
        }
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
    val t = state.vu(Who.THOMAS, d)
    val c = state.vu(Who.CINDY, d)
    val ensemble = t.etat.off && c.etat.off
    val alpha = if (dansLeMois) 1f else 0.35f
    val bord = when {
        aujourdhui -> Color.White
        ensemble -> Yellow
        else -> Color.Transparent
    }
    Column(
        modifier.padding(2.dp).clip(RoundedCornerShape(8.dp))
            .background(if (ensemble) Yellow.copy(alpha = 0.12f * alpha) else Navy2.copy(alpha = alpha))
            .border(if (aujourdhui || ensemble) 1.5.dp else 0.dp, bord.copy(alpha = alpha), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "${d.dayOfMonth}", color = Color.White.copy(alpha = alpha),
            fontWeight = if (aujourdhui) FontWeight.Black else FontWeight.Bold, fontSize = 13.sp,
        )
        Spacer(Modifier.height(2.dp))
        Row(Modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Moitie(Who.THOMAS, t, alpha, Modifier.weight(1f))
            Moitie(Who.CINDY, c, alpha, Modifier.weight(1f))
        }
        Row(
            Modifier.fillMaxWidth().height(8.dp).padding(top = 3.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            evts.take(4).forEach { e ->
                Box(Modifier.padding(horizontal = 1.dp).size(5.dp).clip(CircleShape).background(pourColor(e.pour).copy(alpha = alpha)))
            }
        }
    }
}

@Composable
private fun Moitie(w: Who, v: JourVu, alpha: Float, modifier: Modifier) {
    val bg = etatColor(v.etat)
    val fg = if (v.etat == Etat.TRAVAIL) Muted else Navy
    val marque = when {
        v.heureSup && v.remplacement.isNotBlank() -> "+R"
        v.heureSup -> "+1"
        v.remplacement.isNotBlank() -> "R"
        else -> w.initiale
    }
    Box(
        modifier.height(22.dp).clip(RoundedCornerShape(4.dp)).background(bg.copy(alpha = alpha))
            .then(if (v.heureSup || v.remplacement.isNotBlank()) Modifier.border(1.dp, whoColor(w).copy(alpha = alpha), RoundedCornerShape(4.dp)) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(marque, color = fg.copy(alpha = alpha), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legende() = FlowRow(
    Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
) {
    Text("Gauche : Thomas · droite : Cindy", color = Muted, fontSize = 11.sp)
    Etat.entries.forEach { e ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(etatColor(e)))
            Spacer(Modifier.width(4.dp))
            Text(e.label, color = Muted, fontSize = 11.sp)
        }
    }
    Text("+1 heure sup · R remplacement · cadre jaune = repos ensemble", color = Muted, fontSize = 11.sp)
}

@Composable
fun LigneEvenement(e: Evenement, showDate: Boolean, onClick: () -> Unit) = Row(
    Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(Navy2)
        .clickable { onClick() }.padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Box(Modifier.width(4.dp).height(34.dp).clip(RoundedCornerShape(2.dp)).background(pourColor(e.pour)))
    Spacer(Modifier.width(10.dp))
    Column(Modifier.weight(1f)) {
        Text(
            e.titre.ifBlank { "Rdv" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            buildString {
                if (showDate) append(fmtJour(e.date)).append(" · ")
                append(if (e.journee) "Journée" else e.heure.replace(":", "h"))
                append(" · ").append(pourLabel(e.pour))
                if (e.prive) append(" · privé")
                if (e.rappel >= 0) append(" · 🔔")
            },
            color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
