package com.spcode.tnpscprep

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spcode.tnpscprep.ui.AppRoot
import com.spcode.tnpscprep.ui.affairs.AffairsViewModel
import com.spcode.tnpscprep.ui.auth.AuthGate
import com.spcode.tnpscprep.ui.auth.AuthViewModel
import com.spcode.tnpscprep.ui.affairs.HeadlinesViewModel
import com.spcode.tnpscprep.ui.syllabus.SyllabusViewModel
import com.spcode.tnpscprep.ui.theme.ThemeMode
import com.spcode.tnpscprep.ui.theme.TnpscTheme
import com.spcode.tnpscprep.ui.timer.TimerViewModel
import com.spcode.tnpscprep.ui.today.TodayViewModel

// AppCompatActivity (not ComponentActivity) so AppCompatDelegate.setApplicationLocales works on API 26+.
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : AppCompatActivity() {
    override fun onStop() {
        super.onStop()
        com.spcode.tnpscprep.widget.WidgetUpdater.refreshAll(applicationContext) // widgets show what she just did
    }

    /** The phone's own navigation buttons (square / circle / triangle) are hidden; a swipe up from the bottom edge shows them for a moment. */
    private fun hidePhoneNavButtons() {
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hidePhoneNavButtons()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // must run before super.onCreate: swaps the splash theme for the normal one
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // targetSdk 36 is edge-to-edge anyway; AppRoot pads for system bars itself.
        val c = (application as TnpscApp).container
        setContent {
            // Compact = phones in portrait (<600dp). Medium/Expanded = tablets, big phones in landscape.
            val widthClass = calculateWindowSizeClass(this).widthSizeClass
            val themeMode by c.settingsStore.themeMode.collectAsStateWithLifecycle()
            TnpscTheme(ThemeMode.fromName(themeMode)) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val authVm = viewModel<AuthViewModel>(
                        factory = viewModelFactory { initializer { AuthViewModel(c.settingsStore, eraseData = c::resetEverything) } },
                    )
                    AuthGate(authVm) {
                        val syllabusVm = viewModel<SyllabusViewModel>(
                            factory = viewModelFactory { initializer { SyllabusViewModel(c.syllabusRepository, c.settingsStore) } },
                        )
                        val timerVm = viewModel<TimerViewModel>(
                            factory = viewModelFactory {
                                initializer { TimerViewModel(c.sessionRepository, c.settingsStore, c.syllabusRepository) }
                            },
                        )
                        val todayVm = viewModel<TodayViewModel>(
                            factory = viewModelFactory {
                                initializer { TodayViewModel(c.syllabusRepository, c.affairRepository, c.newsRepository, c.sessionRepository, c.activityRepository, c.settingsStore) }
                            },
                        )
                        val affairsVm = viewModel<AffairsViewModel>(
                            factory = viewModelFactory { initializer { AffairsViewModel(c.affairRepository) } },
                        )
                        val headlinesVm = viewModel<HeadlinesViewModel>(
                            factory = viewModelFactory {
                                initializer { HeadlinesViewModel(c.newsRepository, c.affairRepository, c.settingsStore) }
                            },
                        )
                        val diaryVm = viewModel<com.spcode.tnpscprep.ui.diary.DiaryViewModel>(
                            factory = viewModelFactory { initializer { com.spcode.tnpscprep.ui.diary.DiaryViewModel(c.diaryRepository, c.photoStore) } },
                        )
                        AppRoot(todayVm, syllabusVm, affairsVm, headlinesVm, authVm, timerVm, diaryVm, twoPane = widthClass != WindowWidthSizeClass.Compact)
                    }
                }
            }
        }
    }
}
