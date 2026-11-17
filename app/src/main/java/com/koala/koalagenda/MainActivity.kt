package com.koala.koalagenda

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.YearMonth

sealed interface Screen {
    data object Month : Screen
    data class Day(val date: LocalDate) : Screen
    data class Event(val e: Evenement, val isNew: Boolean, val from: Screen) : Screen
    data class Periode(val debut: LocalDate, val from: Screen) : Screen
    data object Roulements : Screen
    data object Compteurs : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        val crash = java.io.File(filesDir, "crash.txt")
        val ancien = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching {
                crash.writeText("Koalagenda ${BuildConfig.VERSION_NAME} · Android ${Build.VERSION.RELEASE} · ${Build.MODEL}\n\n" + e.stackTraceToString())
            }
            ancien?.uncaughtException(t, e)
        }
        if (crash.exists()) {
            val texte = runCatching { crash.readText() }.getOrDefault("")
            setContent {
                KoalagendaTheme {
                    Surface(Modifier.fillMaxSize(), color = Navy) {
                        CrashScreen(texte) { crash.delete(); recreate() }
                    }
                }
            }
            return
        }
        Rappels.canal(this)
        val repo = Repo.get(this)
        repo.listen()
        setContent {
            KoalagendaTheme {
                Surface(Modifier.fillMaxSize(), color = Navy) { App(repo) }
            }
        }
    }
}

@Composable
fun App(repo: Repo) {
    val ctx = LocalContext.current
    var signedIn by remember { mutableStateOf(repo.signedIn) }
    var moi by remember { mutableStateOf(repo.moi) }
    if (!signedIn) {
        LoginScreen(repo) { signedIn = true }
        return
    }
    val m = moi
    if (m == null) {
        WhoScreen { w -> repo.moi = w; moi = w; repo.replanifierRappels() }
        return
    }

    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    var screen by remember { mutableStateOf<Screen>(Screen.Month) }
    var mois by remember { mutableStateOf(YearMonth.now()) }
    val state by repo.state.collectAsState()
    val upd = remember { UpdateState() }
    LaunchedEffect(Unit) { upd.info = runCatching { Updater.check() }.getOrNull() }

    fun nouveau(date: LocalDate) = Evenement(date = date, auteur = m.id, pour = "deux", rappel = 60, heure = "")

    Box(Modifier.fillMaxSize()) {
        when (val s = screen) {
            Screen.Month -> MonthScreen(
                state, m, upd, mois, { mois = it },
                onDay = { screen = Screen.Day(it) },
                onEvent = { screen = Screen.Event(it, false, Screen.Month) },
                onNewEvent = {
                    val today = LocalDate.now()
                    val d = if (YearMonth.from(today) == mois) today else mois.atDay(1)
                    screen = Screen.Event(nouveau(d), true, Screen.Month)
                },
                onRoulements = { screen = Screen.Roulements },
                onCompteurs = { screen = Screen.Compteurs },
                onChangeMoi = { repo.moi = null; moi = null },
                onSignOut = { repo.signOut(); signedIn = false },
            )

            is Screen.Day -> DayScreen(
                repo, state, m, s.date,
                onBack = { screen = Screen.Month },
                onEvent = { screen = Screen.Event(it, false, s) },
                onNewEvent = { screen = Screen.Event(nouveau(s.date), true, s) },
                onPeriode = { screen = Screen.Periode(s.date, s) },
            )

            is Screen.Event -> EventScreen(
                s.e, s.isNew, m,
                onSave = {
                    repo.saveEvenement(it)
                    if (it.rappel >= 0 && (it.rappelA() ?: 0L) <= System.currentTimeMillis() && it.date >= LocalDate.now())
                        toast(ctx, "Rdv enregistré (l'heure du rappel est déjà passée)")
                    mois = YearMonth.from(it.date)
                    screen = if (s.from is Screen.Day) Screen.Day(it.date) else Screen.Month
                },
                onDelete = { repo.deleteEvenement(it); screen = s.from },
                onBack = { screen = s.from },
            )

            is Screen.Periode -> PeriodeScreen(
                state, m, s.debut,
                onApply = {
                    repo.saveJours(it)
                    toast(ctx, "${it.size} jour(s) modifié(s)")
                    screen = s.from
                },
                onBack = { screen = s.from },
            )

            Screen.Roulements -> RoulementScreen(
                state, m,
                onSave = { w, r -> repo.saveRoulement(w, r); toast(ctx, "Roulement de ${w.label} enregistré"); screen = Screen.Month },
                onBack = { screen = Screen.Month },
            )

            Screen.Compteurs -> CompteursScreen(state, onBack = { screen = Screen.Month })
        }
    }
}
