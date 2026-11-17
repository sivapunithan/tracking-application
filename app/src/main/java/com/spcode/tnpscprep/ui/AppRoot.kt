package com.spcode.tnpscprep.ui

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.ui.affairs.AffairsTab
import com.spcode.tnpscprep.ui.affairs.HeadlinesViewModel
import com.spcode.tnpscprep.ui.affairs.AffairsViewModel
import com.spcode.tnpscprep.ui.settings.SettingsScreen
import com.spcode.tnpscprep.ui.syllabus.SyllabusScreen
import com.spcode.tnpscprep.ui.syllabus.SyllabusViewModel
import com.spcode.tnpscprep.ui.timer.TimerScreen
import com.spcode.tnpscprep.ui.today.TodayScreen
import com.spcode.tnpscprep.ui.today.TodayViewModel
import com.spcode.tnpscprep.ui.timer.TimerViewModel

private object Routes {
    const val TODAY = "today"
    const val SYLLABUS = "syllabus"
    const val AFFAIRS = "affairs"
    const val TIMER = "timer"
    const val SETTINGS = "settings"
}

private val TABS = listOf(
    Routes.TODAY to R.string.tab_today,
    Routes.SYLLABUS to R.string.syllabus_title,
    Routes.AFFAIRS to R.string.tab_affairs,
    Routes.TIMER to R.string.timer_title,
    Routes.SETTINGS to R.string.settings_title,
)

/**
 * Two top-level destinations => tabs + NavHost. Both ViewModels are created by the caller at
 * Activity scope (not inside the NavHost), so the timer isn't torn down when she switches tabs.
 */
@Composable
fun AppRoot(
    todayVm: TodayViewModel, syllabusVm: SyllabusViewModel, affairsVm: AffairsViewModel, headlinesVm: HeadlinesViewModel,
    timerVm: TimerViewModel, twoPane: Boolean,
) {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route ?: Routes.TODAY

    // --- timer concerns that must hold on EVERY tab ---
    val view = LocalView.current
    val activity = LocalContext.current as? Activity
    val running by timerVm.running.collectAsStateWithLifecycle()
    DisposableEffect(running) {            // keep the screen on during a session
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        // Rotation / language switch also stop+recreate the Activity: that must not pause the timer.
        if (activity?.isChangingConfigurations != true) timerVm.onAppStopped()
    }
    val completions by timerVm.completions.collectAsStateWithLifecycle()
    var seen by rememberSaveable { mutableIntStateOf(completions) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(completions) {
        if (completions != seen) { seen = completions; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    }

    fun go(r: String) = nav.navigate(r) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        PrimaryScrollableTabRow(selectedTabIndex = TABS.indexOfFirst { it.first == route }.coerceAtLeast(0), edgePadding = 8.dp) {
            TABS.forEach { (r, label) ->
                Tab(selected = route == r, onClick = { go(r) }, text = { Text(stringResource(label)) })
            }
        }
        NavHost(nav, startDestination = Routes.TODAY, modifier = Modifier.weight(1f)) {
            composable(Routes.TODAY) { TodayScreen(todayVm) }
            composable(Routes.SYLLABUS) { SyllabusScreen(syllabusVm, twoPane) }
            composable(Routes.AFFAIRS) { AffairsTab(affairsVm, headlinesVm) }
            composable(Routes.TIMER) { TimerScreen(timerVm) }
            composable(Routes.SETTINGS) { SettingsScreen(headlinesVm) }
        }
    }
}
