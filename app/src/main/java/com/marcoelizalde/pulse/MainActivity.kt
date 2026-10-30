package com.marcoelizalde.pulse

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.marcoelizalde.pulse.net.UrlConnectionTransport
import com.marcoelizalde.pulse.traffic.dashboard.DashboardRoute
import com.marcoelizalde.pulse.traffic.dashboard.DashboardViewModel
import com.marcoelizalde.pulse.traffic.dashboard.FileRefreshIntervalStore
import com.marcoelizalde.pulse.traffic.datafast.DataFastAnalyticsSource
import com.marcoelizalde.pulse.traffic.demo.DemoAnalyticsSource
import com.marcoelizalde.pulse.traffic.domain.LoadTrafficDashboard
import com.marcoelizalde.pulse.traffic.logos.LogoCache
import com.marcoelizalde.pulse.traffic.logos.LogoDownloader
import com.marcoelizalde.pulse.traffic.logos.ProductLogoSource
import com.marcoelizalde.pulse.traffic.logos.SiteIconFinder
import com.marcoelizalde.pulse.ui.theme.PulseTheme
import java.io.File
import java.time.Clock

class MainActivity : ComponentActivity() {
    private val dashboardViewModel: DashboardViewModel by viewModels {
        viewModelFactory {
            initializer { createDashboardViewModel(File(cacheDir, "logos"), File(filesDir, "refresh_interval")) }
        }
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

/**
 * Composition root: real DataFast data when a token is configured, demo data otherwise.
 * Product sites are fetched with a separate transport that never carries the DataFast token.
 */
private fun createDashboardViewModel(logoCacheDir: File, refreshIntervalFile: File): DashboardViewModel {
    val token = BuildConfig.DATAFAST_TOKEN
    val clock = Clock.systemDefaultZone()
    // In files, not the cache, so the choice survives the system clearing caches.
    val refreshIntervals = FileRefreshIntervalStore(refreshIntervalFile)
    if (token.isBlank()) {
        return DashboardViewModel(
            LoadTrafficDashboard(DemoAnalyticsSource(clock), clock)::invoke,
            isDemo = true,
            refreshIntervals = refreshIntervals,
        )
    }
    val dataFast = DataFastAnalyticsSource(token, UrlConnectionTransport())
    val publicWeb = UrlConnectionTransport(followRedirects = true)
    val logos = ProductLogoSource(
        dataFastLogoUrl = dataFast::logoUrl,
        siteIconUrl = SiteIconFinder(publicWeb)::iconUrl,
        download = LogoDownloader(publicWeb)::download,
        cache = LogoCache(logoCacheDir, clock),
    )
    return DashboardViewModel(
        LoadTrafficDashboard(dataFast, clock)::invoke,
        isDemo = false,
        loadLogo = logos::logo,
        refreshIntervals = refreshIntervals,
    )
}
