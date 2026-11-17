package com.koala.koalagenda

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import java.time.LocalDate

@Composable
fun DayScreen(
    repo: Repo,
    state: AgendaState,
    moi: Who,
    date: LocalDate,
    onBack: () -> Unit,
    onEvent: (Evenement) -> Unit,
    onNewEvent: () -> Unit,
    onPeriode: () -> Unit,
) {
    // Copies locales, enregistrées en quittant l'écran
    var jt by remember(date) { mutableStateOf(state.jour(Who.THOMAS, date) ?: Jour(Who.THOMAS, date)) }
    var jc by remember(date) { mutableStateOf(state.jour(Who.CINDY, date) ?: Jour(Who.CINDY, date)) }
    val init = remember(date) { listOf(jt, jc) }

    fun quitter() {
        sauver(repo, jt, jc, init)
        onBack()
    }
    BackHandler { quitter() }

    // Moi en premier
    val ordre = if (moi == Who.THOMAS) listOf(Who.THOMAS, Who.CINDY) else listOf(Who.CINDY, Who.THOMAS)

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        TopBar(fmtJour(date) + " " + date.year, onBack = { quitter() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            ordre.forEach { w ->
                val j = if (w == Who.THOMAS) jt else jc
                CarteJour(state.roulement(w), j) { n -> if (w == Who.THOMAS) jt = n else jc = n }
                Spacer(Modifier.height(8.dp))
            }

            val evts = state.evenements.filter { it.date == date && it.visiblePar(moi) }
            if (evts.isNotEmpty()) Section("Rdv")
            evts.forEach { e -> LigneEvenement(e, showDate = false) { sauver(repo, jt, jc, init); onEvent(e) } }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { sauver(repo, jt, jc, init); onNewEvent() }, modifier = Modifier.weight(1f)) {
                    Text("+ Rdv", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { sauver(repo, jt, jc, init); onPeriode() }, modifier = Modifier.weight(1f)) {
                    Text("Période", color = Yellow)
                }
            }
            Spacer(Modifier.height(18.dp))
            Button(onClick = { quitter() }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Valider", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun sauver(repo: Repo, jt: Jour, jc: Jour, init: List<Jour>) {
    listOf(jt, jc).zip(init).forEach { (n, a) -> if (n != a) repo.saveJour(n) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CarteJour(r: Roulement, j: Jour, onChange: (Jour) -> Unit) {
    val v = jourVu(r, j, j.date)
    val couleur = whoColor(j.who)
    var ouvert by remember(j.who, j.date) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Navy2)) {
        Row(
            Modifier.fillMaxWidth().clickable { ouvert = !ouvert }.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(couleur))
            Spacer(Modifier.width(10.dp))
            Text(j.who.label, color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp, modifier = Modifier.weight(1f))
            val resume = buildString {
                append(if (v.etat == Etat.TRAVAIL) (if (v.heureSup) "4h – 12h21" else "5h – 12h21") else v.etat.label)
                if (v.remplacement.isNotBlank()) append(" · ").append(v.remplacement)
            }
            Text(resume, color = if (v.etat.off) couleur else Muted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Icon(
                if (ouvert) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null,
                tint = Muted, modifier = Modifier.padding(start = 6.dp),
            )
        }
        if (ouvert) Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Choix("Auto", j.etat == null, Muted) { onChange(j.copy(etat = null)) }
                Etat.entries.forEach { e -> Choix(e.label, j.etat == e, couleur) { onChange(j.copy(etat = e)) } }
            }
            if (v.etat == Etat.TRAVAIL) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Heure sup", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Switch(checked = j.heureSup, onCheckedChange = { onChange(j.copy(heureSup = it)) })
                }
                Row(
                    Modifier.fillMaxWidth().clickable {
                        onChange(j.copy(remplacement = if (j.remplacement.isBlank()) "Ordo" else ""))
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Ordo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Checkbox(
                        checked = j.remplacement.isNotBlank(),
                        onCheckedChange = { onChange(j.copy(remplacement = if (it) "Ordo" else "")) },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                j.note, { onChange(j.copy(note = it)) },
                label = { Text("Note") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
