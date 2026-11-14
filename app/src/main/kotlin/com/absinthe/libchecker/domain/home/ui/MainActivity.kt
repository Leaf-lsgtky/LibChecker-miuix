package com.absinthe.libchecker.domain.home.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import android.view.View
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.app.list.ui.compose.AppListScreen
import com.absinthe.libchecker.domain.home.presentation.HomeViewModel
import com.absinthe.libchecker.domain.home.presentation.RecentVisitsViewModel
import com.absinthe.libchecker.domain.home.ui.compose.HomeScaffold
import com.absinthe.libchecker.domain.home.ui.compose.HomeTab
import com.absinthe.libchecker.domain.home.ui.view.RecentVisitItem
import com.absinthe.libchecker.domain.rules.CloudRulesRepository
import com.absinthe.libchecker.domain.settings.presentation.SettingsViewModel
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.list.ui.compose.SnapshotScreen
import com.absinthe.libchecker.domain.statistics.reference.presentation.LibReferenceViewModel
import com.absinthe.libchecker.domain.statistics.reference.ui.compose.ReferenceScreen
import com.absinthe.libchecker.domain.settings.ui.compose.SettingsScreen
import com.absinthe.libchecker.services.IWorkerService
import com.absinthe.libchecker.services.WorkerService
import com.absinthe.libchecker.ui.base.BaseComposeActivity
import com.absinthe.libchecker.ui.compose.HomeAppearanceState
import com.absinthe.libchecker.ui.compose.LocalHomeAppearance
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.utils.extensions.launchLibReferencePage
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MainActivity : BaseComposeActivity() {

  private val appViewModel: HomeViewModel by viewModel()
  private val recentVisitsViewModel: RecentVisitsViewModel by viewModel()
  private val settingsViewModel: SettingsViewModel by viewModel()
  private val referenceViewModel: LibReferenceViewModel by viewModel()
  private val detailViewModel: com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel by
    viewModel()
  private val snapshotViewModel: SnapshotViewModel by viewModel()
  private val cloudRulesRepository: CloudRulesRepository by inject()

  private var pendingTab by mutableStateOf<HomeTab?>(null)

  private val workerServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
      if (service?.pingBinder() == true) {
        appViewModel.connectWorkerBinder(IWorkerService.Stub.asInterface(service))
      }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
      appViewModel.disconnectWorkerBinder()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (intent.getBooleanExtra(Constants.PP_FROM_CLOUD_RULES_UPDATE, false)) {
      Timber.w("Reinitializing updated rule database")
      cloudRulesRepository.reinitializeRules()
    }

    bindService(
      Intent(this, WorkerService::class.java).apply { setPackage(packageName) },
      workerServiceConnection,
      BIND_AUTO_CREATE
    )
    appViewModel.clearApkCache()
    handleIntent(intent)

    appViewModel.packageChanges.onEach {
      Timber.d("MainActivity received package change: $it")
      appViewModel.packageChanged(it)
    }.launchIn(lifecycleScope)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
  }

  override fun onResume() {
    super.onResume()
    recentVisitsViewModel.refresh()
    if (appViewModel.shouldCheckPackagesPermissionOnResume()) {
      val granted = ContextCompat.checkSelfPermission(
        this,
        Constants.GET_INSTALLED_APPS
      ) == PackageManager.PERMISSION_GRANTED
      if (granted) {
        appViewModel.onPackagesPermissionResult(isGranted = true)
        appViewModel.initItems()
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    unbindService(workerServiceConnection)
  }

  private fun handleIntent(intent: Intent) {
    val tab = when (intent.action) {
      Constants.ACTION_APP_LIST -> HomeTab.APP_LIST
      Constants.ACTION_STATISTICS -> HomeTab.STATISTICS
      Constants.ACTION_SNAPSHOT -> HomeTab.SNAPSHOT
      Intent.ACTION_APPLICATION_PREFERENCES -> HomeTab.SETTINGS
      else -> null
    }
    if (tab != null) {
      pendingTab = tab
    }
  }

  @Composable
  override fun Content() {
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.APP_LIST) }
    val pending = pendingTab
    LaunchedEffect(pending) {
      if (pending != null) {
        selectedTab = pending
        pendingTab = null
      }
    }
    val appearance = remember { HomeAppearanceState() }
    val appListState = rememberLazyListState()
    val settingsState = rememberLazyListState()
    val referenceState = rememberLazyListState()
    val snapshotState = rememberLazyListState()
    val appearanceState = rememberLazyListState()
    var reselectSignal by androidx.compose.runtime.mutableIntStateOf(0)

    CompositionLocalProvider(LocalHomeAppearance provides appearance) {
      HomeScaffold(
        selectedTab = selectedTab,
        onTabSelected = { tab ->
          if (tab == selectedTab) {
            appViewModel.onAppListUserScrolled()
          }
          selectedTab = tab
        }
      ) { tab ->
        when (tab) {
          HomeTab.APP_LIST -> AppListScreen(
            homeViewModel = appViewModel,
            detailViewModel = detailViewModel,
            onOpenDetail = { item -> launchDetailPage(item) },
            listState = appListState
          )

          HomeTab.STATISTICS -> ReferenceScreen(
            viewModel = referenceViewModel,
            onOpenReference = { reference ->
              launchLibReferencePage(
                refName = reference.libName,
                refLabel = reference.rule?.label ?: reference.resolvedLabel,
                refType = reference.type,
                refList = reference.referredList.toTypedArray()
              )
            },
            onOpenChart = {
              startActivity(
                android.content.Intent(
                  this@MainActivity,
                  com.absinthe.libchecker.domain.statistics.chart.ui.ChartActivity::class.java
                )
              )
            },
            listState = referenceState
          )

          HomeTab.SNAPSHOT -> SnapshotScreen(
            viewModel = snapshotViewModel,
            listState = snapshotState
          )

          HomeTab.SETTINGS -> SettingsScreen(
            settingsViewModel = settingsViewModel,
            homeViewModel = appViewModel,
            mainListState = settingsState,
            appearanceListState = appearanceState
          )
        }
      }
    }
  }

}
