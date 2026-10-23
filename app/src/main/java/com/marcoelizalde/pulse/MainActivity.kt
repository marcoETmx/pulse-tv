package com.marcoelizalde.pulse

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.marcoelizalde.pulse.traffic.dashboard.DashboardRoute
import com.marcoelizalde.pulse.traffic.dashboard.DashboardViewModel
import com.marcoelizalde.pulse.traffic.datafast.DataFastAnalyticsSource
import com.marcoelizalde.pulse.traffic.datafast.UrlConnectionTransport
import com.marcoelizalde.pulse.traffic.demo.DemoAnalyticsSource
import com.marcoelizalde.pulse.traffic.domain.LoadTrafficDashboard
import com.marcoelizalde.pulse.ui.theme.PulseTheme
import java.time.Clock

class MainActivity : ComponentActivity() {
    private val dashboardViewModel: DashboardViewModel by viewModels {
        viewModelFactory { initializer { createDashboardViewModel() } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A wall dashboard: keep the TV from dimming or starting the screensaver.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            PulseTheme {
                DashboardRoute(dashboardViewModel)
            }
        }
    }
}

/** Composition root: real DataFast data when a token is configured, demo data otherwise. */
private fun createDashboardViewModel(): DashboardViewModel {
    val token = BuildConfig.DATAFAST_TOKEN
    val clock = Clock.systemDefaultZone()
    val source = if (token.isBlank()) {
        DemoAnalyticsSource(clock)
    } else {
        DataFastAnalyticsSource(token, UrlConnectionTransport())
    }
    return DashboardViewModel(LoadTrafficDashboard(source, clock)::invoke, isDemo = token.isBlank())
}
