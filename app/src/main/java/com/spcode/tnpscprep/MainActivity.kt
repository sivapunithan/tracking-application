package com.spcode.tnpscprep

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
    override fun onCreate(savedInstanceState: Bundle?) {
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
                            factory = viewModelFactory { initializer { SyllabusViewModel(c.syllabusRepository) } },
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
                        AppRoot(todayVm, syllabusVm, affairsVm, headlinesVm, authVm, timerVm, twoPane = widthClass != WindowWidthSizeClass.Compact)
                    }
                }
            }
        }
    }
}
