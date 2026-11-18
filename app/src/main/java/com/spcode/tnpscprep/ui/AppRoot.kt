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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Today
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.foundation.gestures.draggable
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.spcode.tnpscprep.ui.care.CareScreen
import com.spcode.tnpscprep.ui.diary.DiaryViewModel
import com.spcode.tnpscprep.ui.diary.JournalEditorScreen
import com.spcode.tnpscprep.ui.diary.JournalScreen
import com.spcode.tnpscprep.ui.settings.ProfileScreen
import com.spcode.tnpscprep.ui.study.StudyScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import com.spcode.tnpscprep.ui.affairs.AffairsViewModel
import com.spcode.tnpscprep.ui.affairs.HeadlinesViewModel
import com.spcode.tnpscprep.ui.auth.AuthViewModel
import com.spcode.tnpscprep.ui.design.FloatingNavBar
import com.spcode.tnpscprep.ui.design.LocalBottomBarInset
import com.spcode.tnpscprep.ui.design.NavItem
import com.spcode.tnpscprep.ui.design.rememberReduceMotion
import com.spcode.tnpscprep.ui.syllabus.SyllabusViewModel
import com.spcode.tnpscprep.ui.timer.TimerScreen
import com.spcode.tnpscprep.ui.timer.TimerViewModel
import com.spcode.tnpscprep.ui.today.TodayScreen
import com.spcode.tnpscprep.ui.today.TodayViewModel

/** Each tab is a NESTED graph, so a tab remembers its own sub-screen and scroll position when she switches away and back. */
private object Graph {
    const val TODAY = "g_today"
    const val STUDY = "g_study"
    const val CARE = "g_care"
    const val JOURNAL = "g_journal"
}

private object Dest {
    const val TODAY = "today"
    const val STUDY = "study"
    const val CARE = "care"
    const val JOURNAL = "journal"
    const val JOURNAL_EDITOR = "journal/editor?id={id}&date={date}"
    const val FOCUS = "focus"
    const val PROFILE = "profile"
    const val WRAPUP = "wrapup"
}

private class Tab(val graph: String, val labelRes: Int, val icon: ImageVector)

/** Bottom bar tabs, left to right. The Focus button sits between the 2nd and 3rd. */
private val TABS = listOf(
    Tab(Graph.TODAY, R.string.nav_today, Icons.Rounded.Today),
    Tab(Graph.STUDY, R.string.nav_study, Icons.AutoMirrored.Rounded.MenuBook),
    Tab(Graph.CARE, R.string.nav_care, Icons.Rounded.Favorite),
    Tab(Graph.JOURNAL, R.string.nav_journal, Icons.Rounded.EditNote),
)

private fun NavBackStackEntry?.tabIndex(): Int =
    if (this == null) -1 else TABS.indexOfFirst { t -> destination.hierarchy.any { it.route == t.graph } }

/** Full-screen pages hide the bar: the timer, the profile and the diary editor. */
private fun isFull(route: String?) = NavPolicy.isFullScreen(route)

/** Slide direction follows the tab order: moving right slides content in from the right. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.direction(): Int =
    if (targetState.tabIndex() >= initialState.tabIndex()) 1 else -1

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
 * Navigation shell: bottom bar (Today | Study | Focus | Care | Journal), a nested graph per tab, and full-screen pages (Focus, Profile,
 * diary editor) that hide the bar. The ViewModels are created by the caller at Activity scope (not inside the NavHost), so the timer
 * isn't torn down when she switches tabs. No top bars: each screen draws its own heading.
 */
@Composable
fun AppRoot(
    todayVm: TodayViewModel, syllabusVm: SyllabusViewModel, affairsVm: AffairsViewModel, headlinesVm: HeadlinesViewModel,
    authVm: AuthViewModel, timerVm: TimerViewModel, diaryVm: DiaryViewModel, twoPane: Boolean,
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val selectedTab = entry.tabIndex().let { if (isFull(route)) -1 else it }
    val showBar = !isFull(route)
    val reduceMotion = rememberReduceMotion()

    // --- timer concerns that must hold on EVERY screen ---
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
        if (completions != seen) {
            seen = completions; haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            com.spcode.tnpscprep.notify.Praise.notify(activity ?: return@LaunchedEffect, com.spcode.tnpscprep.domain.PraiseEvent.FOCUS_DONE)
        }
    }

    fun goTab(graph: String) = nav.navigate(graph) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    fun open(r: String) = nav.navigate(r) { launchSingleTop = true }

    val heat by todayVm.heat.collectAsStateWithLifecycle()
    val studyByDate = remember(heat) { heat.weeks.flatten().filterNotNull().associate { it.date to it.minutes } }

    val items = TABS.map { NavItem(stringResource(it.labelRes), it.icon) }
    val focusItem = NavItem(stringResource(R.string.nav_focus), Icons.Rounded.Timer)

    // The status-bar height, remembered at its largest: on some phones the reported inset briefly drops to 0 while the keyboard opens or the
    // navigation buttons hide, which pushed the editor's top row under the clock. The bar itself never shrinks, so the padding must not either.
    val density = LocalDensity.current
    val statusNow = WindowInsets.statusBars.getTop(density)
    var statusMax by remember { mutableIntStateOf(0) }
    if (statusNow > statusMax) statusMax = statusNow
    val stableStatusTop = with(density) { statusMax.toDp() }
    var barPx by remember { mutableIntStateOf(0) }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // The inset = the bar's MEASURED height (incl. system-bar padding) + air while the bar shows; on full-screen pages just the system bar.
    var scrolledAway by remember { mutableStateOf(false) }
    val inset = if (showBar && !scrolledAway) (if (barPx > 0) with(LocalDensity.current) { barPx.toDp() } + 16.dp else 108.dp) else 16.dp + navBottom

    // The bar slides away while she scrolls down and returns on a scroll up, at the end of a list, on a tap at the bottom edge, or on a tab change.
    LaunchedEffect(route) { scrolledAway = false }
    val barScroll = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                when {
                    consumed.y < -6f -> scrolledAway = true       // finger moves up = content scrolls down
                    consumed.y > 6f || available.y < -6f -> scrolledAway = false // scroll up, or hit the end of the list
                }
                return Offset.Zero
            }
        }
    }
    // Swipe left/right anywhere on a tab page moves to the next/previous tab. A standard horizontal drag on the root: a child that scrolls
    // sideways (the heatmap) gets the gesture first, vertical scrolling and taps are unaffected. A short drag or a quick flick is enough.
    val swipeThreshold = with(LocalDensity.current) { 56.dp.toPx() }
    val flickMin = with(LocalDensity.current) { 24.dp.toPx() }
    val dragged = remember { floatArrayOf(0f) }
    val swipeState = androidx.compose.foundation.gestures.rememberDraggableState { dragged[0] += it }
    Box(
        Modifier.fillMaxSize().nestedScroll(barScroll).draggable(
            state = swipeState, orientation = androidx.compose.foundation.gestures.Orientation.Horizontal, enabled = selectedTab >= 0,
            onDragStarted = { dragged[0] = 0f },
            onDragStopped = { v ->
                val d = dragged[0]
                val go = kotlin.math.abs(d) > swipeThreshold || (kotlin.math.abs(d) > flickMin && kotlin.math.abs(v) > 800f)
                if (go) { val next = selectedTab + if (d < 0) 1 else -1; if (next in TABS.indices) goTab(TABS[next].graph) }
            },
        ),
    ) {
        CompositionLocalProvider(LocalBottomBarInset provides inset) {
            NavHost(
                nav,
                startDestination = Graph.TODAY,
                modifier = Modifier.fillMaxSize().padding(top = stableStatusTop),
                enterTransition = {
                    if (reduceMotion) EnterTransition.None
                    else if (isFull(targetState.destination.route)) fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 16 }
                    else tabEnter()
                },
                exitTransition = {
                    if (reduceMotion) ExitTransition.None
                    else if (isFull(targetState.destination.route)) fadeOut(tween(160)) else tabExit()
                },
                popEnterTransition = {
                    if (reduceMotion) EnterTransition.None
                    else if (isFull(initialState.destination.route)) fadeIn(tween(220)) else tabEnter()
                },
                popExitTransition = {
                    if (reduceMotion) ExitTransition.None
                    else if (isFull(initialState.destination.route)) fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 16 } else tabExit()
                },
            ) {
                navigation(startDestination = Dest.TODAY, route = Graph.TODAY) {
                    composable(Dest.TODAY) {
                        TodayScreen(
                            todayVm,
                            onOpenProfile = { open(Dest.PROFILE) },
                            onOpenFocus = { open(Dest.FOCUS) },
                            onOpenWrapup = { open(Dest.WRAPUP) },
                            onOpenStudy = { goTab(Graph.STUDY) },
                            onOpenDiary = { start ->
                                when {
                                    start.startsWith("entry:") -> open("journal/editor?id=" + start.removePrefix("entry:"))
                                    start.startsWith("new:") -> open("journal/editor?date=" + start.removePrefix("new:"))
                                    else -> goTab(Graph.JOURNAL)
                                }
                            },
                        )
                    }
                }
                navigation(startDestination = Dest.STUDY, route = Graph.STUDY) {
                    composable(Dest.STUDY) { StudyScreen(syllabusVm, affairsVm, headlinesVm, todayVm, twoPane) }
                }
                navigation(startDestination = Dest.CARE, route = Graph.CARE) {
                    composable(Dest.CARE) { CareScreen() }
                }
                navigation(startDestination = Dest.JOURNAL, route = Graph.JOURNAL) {
                    composable(Dest.JOURNAL) {
                        JournalScreen(diaryVm, todayVm, studyByDate, onOpen = { id -> open("journal/editor?id=$id") }, onNew = { open("journal/editor") }, onNewOn = { d -> open("journal/editor?date=$d") }, onWrapup = { open(Dest.WRAPUP) })
                    }
                    composable(
                        Dest.JOURNAL_EDITOR,
                        arguments = listOf(
                            navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                            navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                        ),
                    ) { e ->
                        JournalEditorScreen(
                            diaryVm,
                            entryId = e.arguments?.getLong("id")?.takeIf { it >= 0 },
                            dateArg = e.arguments?.getString("date"),
                            onDone = { removed -> if (removed != null) diaryVm.showUndo(removed); nav.popBackStack() },
                            onWrapup = { nav.popBackStack(); open(Dest.WRAPUP) },
                        )
                    }
                }
                composable(Dest.WRAPUP) { com.spcode.tnpscprep.ui.wrapup.WrapupScreen(todayVm, onClose = { nav.popBackStack() }) }
                composable(Dest.FOCUS) { TimerScreen(timerVm, onClose = { nav.popBackStack() }) }
                composable(Dest.PROFILE) { ProfileScreen(headlinesVm, authVm, studyByDate, onBack = { nav.popBackStack() }) }
            }
        }
        if (showBar && scrolledAway) Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().height(28.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { scrolledAway = false },
        )
        AnimatedVisibility(
            visible = showBar && !scrolledAway,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = if (reduceMotion) EnterTransition.None else fadeIn(tween(200)) + slideInVertically(tween(240)) { it / 2 },
            exit = if (reduceMotion) ExitTransition.None else fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 2 },
        ) {
            Box {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.92f), MaterialTheme.colorScheme.background))),
                )
                FloatingNavBar(
                    items = items,
                    selected = selectedTab,
                    onSelect = { goTab(TABS[it].graph) },
                    modifier = Modifier.align(Alignment.BottomCenter).onSizeChanged { barPx = it.height },
                    center = focusItem,
                    onCenter = { open(Dest.FOCUS) },
                )
            }
        }
    }
}
