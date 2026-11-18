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
    data object Reglages : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        Theme.charger(this)
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
        Theme.charger(this)
        val repo = Repo.get(this)
        repo.listen()
        setContent {
            KoalagendaTheme {
                val clair = Clair
                LaunchedEffect(clair) {
                    val style = if (clair) SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    this@MainActivity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
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
    var selection by remember { mutableStateOf<Selection?>(null) }
    val state by repo.state.collectAsState()
    val upd = remember { UpdateState() }
    LaunchedEffect(Unit) { upd.info = runCatching { Updater.check() }.getOrNull() }

    fun nouveau(date: LocalDate) = Evenement(date = date, auteur = m.id, pour = "deux", rappel = 60, heure = "")

    androidx.compose.runtime.CompositionLocalProvider(LocalCouleurs provides state.couleurs) {
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
                onReglages = { screen = Screen.Reglages },
                selection = selection,
                onSelection = { selection = it },
                onValider = { sel ->
                    val now = System.currentTimeMillis()
                    if (sel.echange) {
                        val (a, b) = sel.jours.sorted().let { it[0] to it[1] }
                        val l = echangerRepos(state.roulement(m), state.jours, m, a, b, now)
                        if (l == null) toast(ctx, "Choisis un jour de repos et un jour travaillé")
                        else { repo.saveJours(l); toast(ctx, "Repos échangé"); selection = null }
                    } else {
                        val l = if (sel.ordo) appliquerOrdo(state.roulement(m), state.jours, m, sel.avant, sel.jours, now)
                        else appliquerEtat(state.jours, m, sel.etat, sel.avant, sel.jours, now)
                        repo.saveJours(l)
                        toast(ctx, "${l.size} jour(s) modifié(s)")
                        selection = null
                    }
                },
            )

            Screen.Reglages -> ReglagesScreen(
                m, upd,
                onRoulements = { screen = Screen.Roulements },
                onCompteurs = { screen = Screen.Compteurs },
                onChangeMoi = { repo.moi = null; moi = null; screen = Screen.Month },
                onSignOut = { repo.signOut(); signedIn = false; screen = Screen.Month },
                onCouleur = { w, c -> repo.saveCouleur(w, c) },
                enfants = state.enfants,
                onEnfants = { repo.saveEnfants(it) },
                onBack = { screen = Screen.Month },
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
                onSave = { w, r -> repo.saveRoulement(w, r); toast(ctx, "Roulement de ${w.label} enregistré"); screen = Screen.Reglages },
                onBack = { screen = Screen.Reglages },
            )

            Screen.Compteurs -> CompteursScreen(state, onBack = { screen = Screen.Reglages })
        }
    }
}
}
