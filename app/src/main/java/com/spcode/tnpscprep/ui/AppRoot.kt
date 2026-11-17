package com.spcode.tnpscprep.ui

import android.app.Activity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Today
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.ui.affairs.AffairsTab
import com.spcode.tnpscprep.ui.affairs.AffairsViewModel
import com.spcode.tnpscprep.ui.affairs.HeadlinesViewModel
import com.spcode.tnpscprep.ui.auth.AuthViewModel
import com.spcode.tnpscprep.ui.design.FloatingNavBar
import com.spcode.tnpscprep.ui.design.LocalBottomBarInset
import com.spcode.tnpscprep.ui.design.NavItem
import com.spcode.tnpscprep.ui.design.rememberReduceMotion
import com.spcode.tnpscprep.ui.settings.SettingsScreen
import com.spcode.tnpscprep.ui.syllabus.SyllabusScreen
import com.spcode.tnpscprep.ui.syllabus.SyllabusViewModel
import com.spcode.tnpscprep.ui.timer.TimerScreen
import com.spcode.tnpscprep.ui.timer.TimerViewModel
import com.spcode.tnpscprep.ui.today.TodayScreen
import com.spcode.tnpscprep.ui.today.TodayViewModel

private object Routes {
    const val TODAY = "today"
    const val SYLLABUS = "syllabus"
    const val AFFAIRS = "affairs"
    const val TIMER = "timer"
    const val SETTINGS = "settings"
}

private class Tab(val route: String, val labelRes: Int, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.TODAY, R.string.nav_today, Icons.Rounded.Today),
    Tab(Routes.SYLLABUS, R.string.nav_syllabus, Icons.AutoMirrored.Rounded.MenuBook),
    Tab(Routes.AFFAIRS, R.string.nav_news, Icons.Rounded.Newspaper),
    Tab(Routes.TIMER, R.string.nav_timer, Icons.Rounded.Timer),
    Tab(Routes.SETTINGS, R.string.nav_settings, Icons.Rounded.Settings),
)

private fun indexOf(route: String?) = TABS.indexOfFirst { it.route == route }

/** Slide direction follows the tab order: moving right slides content in from the right. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.direction(): Int =
    if (indexOf(targetState.destination.route) >= indexOf(initialState.destination.route)) 1 else -1

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabEnter(): EnterTransition {
    val dir = direction()
    return fadeIn(tween(260, delayMillis = 70)) +
        slideInHorizontally(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { dir * it / 9 }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabExit(): ExitTransition {
    val dir = direction()
    return fadeOut(tween(140)) + slideOutHorizontally(tween(240)) { -dir * it / 9 }
}

/**
 * Floating bottom bar + animated tab transitions. No top bars: each screen draws its own big heading.
 * Both ViewModels are created by the caller at Activity scope (not inside the NavHost), so the timer isn't torn down when she switches tabs.
 */
@Composable
fun AppRoot(
    todayVm: TodayViewModel, syllabusVm: SyllabusViewModel, affairsVm: AffairsViewModel, headlinesVm: HeadlinesViewModel,
    authVm: AuthViewModel, timerVm: TimerViewModel, twoPane: Boolean,
) {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route ?: Routes.TODAY
    val reduceMotion = rememberReduceMotion()

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

    val items = TABS.map { NavItem(stringResource(it.labelRes), it.icon) }

    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBottomBarInset provides 108.dp) {
            NavHost(
                nav,
                startDestination = Routes.TODAY,
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                enterTransition = { if (reduceMotion) EnterTransition.None else tabEnter() },
                exitTransition = { if (reduceMotion) ExitTransition.None else tabExit() },
                popEnterTransition = { if (reduceMotion) EnterTransition.None else tabEnter() },
                popExitTransition = { if (reduceMotion) ExitTransition.None else tabExit() },
            ) {
                composable(Routes.TODAY) { TodayScreen(todayVm) }
                composable(Routes.SYLLABUS) { SyllabusScreen(syllabusVm, twoPane) }
                composable(Routes.AFFAIRS) { AffairsTab(affairsVm, headlinesVm) }
                composable(Routes.TIMER) { TimerScreen(timerVm) }
                composable(Routes.SETTINGS) { SettingsScreen(headlinesVm, authVm) }
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(150.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.92f), MaterialTheme.colorScheme.background))),
        )
        FloatingNavBar(
            items = items,
            selected = indexOf(route).coerceAtLeast(0),
            onSelect = { go(TABS[it].route) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
