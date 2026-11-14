package com.absinthe.libchecker.domain.app.detail.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.compat.IntentCompat
import com.absinthe.libchecker.domain.app.detail.model.DetailExtraBean
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_DETAIL_BEAN
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.packageinfo.GetAppDetailPackageUseCase
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel.PackageLoadResult
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.detail.ui.compose.AppDetailScreen
import com.absinthe.libchecker.domain.app.model.VersionedFeature
import com.absinthe.libchecker.domain.home.presentation.RecentVisitsViewModel
import com.absinthe.libchecker.domain.home.recent.RecentVisit
import com.absinthe.libchecker.ui.base.BaseComposeActivity
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import com.absinthe.libchecker.utils.Toasty
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class AppDetailActivity : BaseComposeActivity() {

  private val viewModel: DetailViewModel by viewModel()
  private val recentVisitsViewModel: RecentVisitsViewModel by viewModel()

  private var pkgName: String? = null
  private var recordVisit = true
  private var extraBean: DetailExtraBean? = null
  private var packageInfoState by mutableStateOf<PackageInfo?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    recordVisit = savedInstanceState == null
    lifecycleScope.launch {
      viewModel.packageLoadResults.collect(::handlePackageLoadResult)
    }
    initPackage(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    recordVisit = true
    packageInfoState = null
    initPackage(intent)
  }

  override fun onStart() {
    super.onStart()
    registerPackageBroadcast()
  }

  override fun onStop() {
    super.onStop()
    unregisterReceiver(requestPackageReceiver)
  }

  private fun initPackage(intent: Intent) {
    pkgName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: let {
      intent.data?.let { uri ->
        uri.getQueryParameter("id").takeIf { uri.scheme == "market" && uri.host == "details" }
      }
    }
    extraBean = IntentCompat.getParcelableExtra<DetailExtraBean>(intent, EXTRA_DETAIL_BEAN)
    Timber.d("packageName: $pkgName, extraBean: $extraBean")
    val packageName = pkgName ?: return
    viewModel.loadAppDetailPackage(packageName)
  }

  private fun handlePackageLoadResult(loadResult: PackageLoadResult) {
    if (loadResult.packageName != pkgName) {
      return
    }
    when (val result = loadResult.result) {
      is GetAppDetailPackageUseCase.Result.Available -> {
        viewModel.reset()
        viewModel.initPackageInfo(result.packageInfo)
        viewModel.initAbiInfo(result.packageInfo, apkAnalyticsMode = false)
        viewModel.emitFeature(VersionedFeature(Features.Ext.APPLICATION_PROP))
        viewModel.setFeatureLoading(true)
        viewModel.initFeatures(result.packageInfo, extraBean?.features ?: -1)
        packageInfoState = result.packageInfo
        if (recordVisit) {
          recordVisit = false
          recentVisitsViewModel.record(RecentVisit(loadResult.packageName))
        }
      }

      GetAppDetailPackageUseCase.Result.Archived -> {
        recentVisitsViewModel.remove(RecentVisit(loadResult.packageName))
        Timber.w("isArchivedPackage: ${loadResult.packageName}")
        Toasty.showLong(this, R.string.toast_archived_app_detail_unavailable)
        finish()
      }

      GetAppDetailPackageUseCase.Result.NotFound -> {
        recentVisitsViewModel.remove(RecentVisit(loadResult.packageName))
        Timber.d("getPackageInfo: ${loadResult.packageName} failed")
        finish()
      }
    }
  }

  private val requestPackageReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      if (intent.data?.schemeSpecificPart.orEmpty() == pkgName) {
        if (intent.action == Intent.ACTION_PACKAGE_REMOVED) {
          finish()
        } else {
          packageInfoState = null
          pkgName?.let(viewModel::loadAppDetailPackage)
        }
      }
    }
  }

  private fun registerPackageBroadcast() {
    val intentFilter = IntentFilter().apply {
      addAction(Intent.ACTION_PACKAGE_REPLACED)
      addAction(Intent.ACTION_PACKAGE_REMOVED)
      addDataScheme("package")
    }
    registerReceiver(requestPackageReceiver, intentFilter)
  }

  @Composable
  override fun Content() {
    val packageInfo = packageInfoState
    if (packageInfo == null) {
      Box(Modifier.fillMaxSize()) {
        LcLoadingState()
      }
    } else {
      key(packageInfo.packageName) {
        AppDetailScreen(
          viewModel = viewModel,
          packageInfo = packageInfo,
          onBack = { onBackPressedDispatcher.onBackPressed() }
        )
      }
    }
  }
}
