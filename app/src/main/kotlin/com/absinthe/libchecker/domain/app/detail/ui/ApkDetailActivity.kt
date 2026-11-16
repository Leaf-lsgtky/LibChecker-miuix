package com.absinthe.libchecker.domain.app.detail.ui

import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Bundle
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.compat.IntentCompat
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel.ApkAnalysisPackageResult
import com.absinthe.libchecker.domain.app.detail.ui.compose.AppDetailScreen
import com.absinthe.libchecker.domain.app.model.VersionedFeature
import com.absinthe.libchecker.domain.app.packageinfo.PrepareApkAnalysisPackageUseCase
import com.absinthe.libchecker.domain.snapshot.detail.ui.launchSnapshotDetail
import com.absinthe.libchecker.ui.base.BaseComposeActivity
import com.absinthe.libchecker.ui.compose.component.AppDetailSkeleton
import com.absinthe.libchecker.utils.extensions.requireAvailableCacheDir
import com.absinthe.libchecker.utils.showToast
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

/** Detail page for a standalone APK file opened through share/open-with. */
class ApkDetailActivity : BaseComposeActivity() {

  private val viewModel: DetailViewModel by viewModel()
  private var packageInfoState by mutableStateOf<PackageInfo?>(null)
  private var canCompareWithInstalled by mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    lifecycleScope.launch {
      viewModel.apkAnalysisPackageResults.collect(::handleApkAnalysisPackageResult)
    }
    resolveIntent(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    packageInfoState = null
    resolveIntent(intent)
  }

  private fun resolveIntent(intent: Intent?) {
    when {
      intent == null -> finish()

      intent.action == Intent.ACTION_SEND -> {
        val stream = IntentCompat.getParcelableExtra<Uri>(intent, Intent.EXTRA_STREAM)
        if (stream != null) {
          initPackage(stream)
        } else {
          // Remote APK preview needs the legacy preview renderer; unsupported for now.
          showToast(R.string.toast_cant_open_app)
          finish()
        }
      }

      intent.data?.scheme == "content" -> {
        intent.data?.let(::initPackage) ?: finish()
      }

      else -> finish()
    }
  }

  private fun initPackage(uri: Uri) {
    viewModel.loadApkAnalysisPackage(requireAvailableCacheDir(), uri)
  }

  private fun handleApkAnalysisPackageResult(loadResult: ApkAnalysisPackageResult) {
    when (val result = loadResult.result) {
      is PrepareApkAnalysisPackageUseCase.Result.Available -> {
        viewModel.reset()
        viewModel.initPackageInfo(result.packageInfo)
        viewModel.initAbiInfo(result.packageInfo, apkAnalyticsMode = true)
        viewModel.emitFeature(VersionedFeature(Features.Ext.APPLICATION_PROP))
        viewModel.setFeatureLoading(true)
        viewModel.initFeatures(result.packageInfo, -1)
        packageInfoState = result.packageInfo
      }

      is PrepareApkAnalysisPackageUseCase.Result.InvalidPackage -> {
        Timber.w("Invalid apk package: ${result.file}")
        showToast(R.string.toast_use_another_file_manager)
        finish()
      }

      PrepareApkAnalysisPackageUseCase.Result.Unreadable -> {
        showToast(R.string.toast_use_another_file_manager)
        finish()
      }

      PrepareApkAnalysisPackageUseCase.Result.NotEnoughStorage -> {
        showToast(R.string.toast_not_enough_storage_space)
        finish()
      }
    }
  }

  @Composable
  override fun Content() {
    val packageInfo = packageInfoState
    Crossfade(
      targetState = packageInfo,
      animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
      label = "ApkDetailLoadTransition"
    ) { resolvedPackage ->
      if (resolvedPackage == null) {
        AppDetailSkeleton(onBack = { onBackPressedDispatcher.onBackPressed() })
      } else {
        LaunchedEffect(resolvedPackage.packageName) {
          canCompareWithInstalled = runCatching {
            viewModel.isInstalledAppComparisonAvailable(resolvedPackage.packageName)
          }.getOrDefault(false)
        }
        AppDetailScreen(
          viewModel = viewModel,
          packageInfo = resolvedPackage,
          onCompareWithInstalled = if (canCompareWithInstalled) {
            {
              lifecycleScope.launch {
                val base = viewModel.loadInstalledAppComparisonPackage(resolvedPackage.packageName)
                if (base == null) {
                  showToast(R.string.toast_cant_open_app)
                  return@launch
                }
                val diff = viewModel.buildPackageComparisonSnapshotItem(base, resolvedPackage)
                if (diff != null) {
                  launchSnapshotDetail(this@ApkDetailActivity, diff)
                }
              }
            }
          } else {
            null
          },
          onBack = { onBackPressedDispatcher.onBackPressed() }
        )
      }
    }
  }
}
