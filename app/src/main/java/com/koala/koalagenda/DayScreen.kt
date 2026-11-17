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
import androidx.compose.material3.Button
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
                Spacer(Modifier.height(12.dp))
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
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Navy2),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .background(if (v.etat.off) couleur else Navy3)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                j.who.label, color = if (v.etat.off) Navy else Color.White,
                fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.weight(1f),
            )
            Text(
                if (v.etat == Etat.TRAVAIL) (if (v.heureSup) "4h – 12h21" else "5h – 12h21") else v.etat.label,
                color = if (v.etat.off) Navy else couleur, fontWeight = FontWeight.Black, fontSize = 15.sp,
            )
        }
        Column(Modifier.padding(14.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Choix("Auto", j.etat == null, Muted) { onChange(j.copy(etat = null)) }
                Etat.entries.forEach { e -> Choix(e.label, j.etat == e, couleur) { onChange(j.copy(etat = e)) } }
            }
            if (v.etat == Etat.TRAVAIL) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Heure sup", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Switch(checked = j.heureSup, onCheckedChange = { onChange(j.copy(heureSup = it)) })
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    j.remplacement, { onChange(j.copy(remplacement = it)) },
                    label = { Text("Remplacement") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                j.note, { onChange(j.copy(note = it)) },
                label = { Text("Note") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
