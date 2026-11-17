package com.spcode.tnpscprep.ui.care

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spcode.tnpscprep.ui.today.FoodViewModel
import com.spcode.tnpscprep.ui.today.SectionTitle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.TnpscApp
import com.spcode.tnpscprep.ui.design.LocalBottomBarInset
import com.spcode.tnpscprep.ui.design.ScreenTitle
import com.spcode.tnpscprep.ui.today.HealthSheet
import com.spcode.tnpscprep.ui.today.HealthViewModel
import com.spcode.tnpscprep.ui.today.StepsCard
import com.spcode.tnpscprep.ui.today.hasStepSensor

/**
 * The Care tab: her private health hub. Three groups on one page: Food history, Steps, Daily log
 * (logs and estimates only: no diagnosis, no scores).
 */
@Composable
fun CareScreen() {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as TnpscApp
    val hvm = viewModel<HealthViewModel>(
        factory = viewModelFactory { initializer { HealthViewModel(app.container.healthRepository, app.container.settingsStore) } },
    )
    val stepSensor = remember { hasStepSensor(app) }
    val fvm = viewModel<FoodViewModel>(
        factory = viewModelFactory { initializer { FoodViewModel(app.container.foodRepository, app.container.settingsStore) } },
    )
    val food by fvm.days.collectAsStateWithLifecycle()
    val today = remember { com.spcode.tnpscprep.domain.FoodCheckin.foodDay(System.currentTimeMillis(), java.time.ZoneId.systemDefault()) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp + LocalBottomBarInset.current),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenTitle(stringResource(R.string.care_title))
        // Three groups: Food (history of the Today check-in), Steps, Daily log (water, body measurements, cycle: all private estimates)
        SectionTitle(stringResource(R.string.care_food))
        FoodHistoryCard(food, today)
        if (stepSensor) {
            StepsCard()
        }
        SectionTitle(stringResource(R.string.care_log))
        HealthSheet(hvm, dismiss = null)
    }
}
