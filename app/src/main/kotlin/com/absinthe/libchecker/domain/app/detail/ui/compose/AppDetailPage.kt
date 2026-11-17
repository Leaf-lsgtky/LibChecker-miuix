@file:Suppress("ktlint:standard:function-naming")

package com.absinthe.libchecker.domain.app.detail.ui.compose

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ALL
import com.absinthe.libchecker.compat.IntentCompat
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.detail.model.DetailExtraBean
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_DETAIL_BEAN
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.packageinfo.GetAppDetailPackageUseCase
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.model.VersionedFeature
import com.absinthe.libchecker.domain.home.presentation.RecentVisitsViewModel
import com.absinthe.libchecker.domain.home.recent.RecentVisit
import com.absinthe.libchecker.domain.statistics.reference.ui.EXTRA_REF_NAME
import com.absinthe.libchecker.domain.statistics.reference.ui.EXTRA_REF_TYPE
import com.absinthe.libchecker.ui.compose.component.AppDetailSkeleton
import com.absinthe.libchecker.utils.Toasty
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/** Page-scoped state works both in the Miuix stack and the external intent activity. */
@Composable
fun AppDetailPage(intent: Intent, onBack: () -> Unit) {
  val context = LocalContext.current
  val viewModel: DetailViewModel = koinViewModel()
  val recentVisits: RecentVisitsViewModel = koinViewModel()
  val currentOnBack by rememberUpdatedState(onBack)
  val packageName = remember(intent) {
    intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: intent.data?.let { uri ->
      if (uri.scheme == "market" && uri.host == "details") uri.getQueryParameter("id") else null
    }
  }
  val extra = remember(intent) { IntentCompat.getParcelableExtra<DetailExtraBean>(intent, EXTRA_DETAIL_BEAN) }
  val referenceName = remember(intent) { intent.getStringExtra(EXTRA_REF_NAME) }
  val referenceType = remember(intent) { intent.getIntExtra(EXTRA_REF_TYPE, ALL) }
  // A re-entered page keeps the loaded package in the view model, so it must not load it again.
  var packageInfo by remember(intent) { mutableStateOf<PackageInfo?>(viewModel.loadedPackageInfo(packageName)) }
  var recordVisit by rememberSaveable(intent) { mutableStateOf(true) }
  LaunchedEffect(intent) {
    if (packageName == null) {
      currentOnBack()
      return@LaunchedEffect
    }
    launch(start = CoroutineStart.UNDISPATCHED) {
      viewModel.packageLoadResults.collect { loaded ->
        if (loaded.packageName != packageName) return@collect
        when (val result = loaded.result) {
          is GetAppDetailPackageUseCase.Result.Available -> {
            viewModel.reset()
            viewModel.initPackageInfo(result.packageInfo)
            viewModel.initAbiInfo(result.packageInfo, apkAnalyticsMode = false)
            viewModel.emitFeature(VersionedFeature(Features.Ext.APPLICATION_PROP))
            viewModel.setFeatureLoading(true)
            viewModel.initFeatures(result.packageInfo, extra?.features ?: -1)
            packageInfo = result.packageInfo
            if (recordVisit) {
              recordVisit = false
              recentVisits.record(RecentVisit(packageName))
            }
          }

          GetAppDetailPackageUseCase.Result.Archived -> {
            recentVisits.remove(RecentVisit(packageName))
            Toasty.showLong(context, R.string.toast_archived_app_detail_unavailable)
            currentOnBack()
          }

          GetAppDetailPackageUseCase.Result.NotFound -> {
            recentVisits.remove(RecentVisit(packageName))
            currentOnBack()
          }
        }
      }
    }
    if (viewModel.loadedPackageInfo(packageName) == null) {
      viewModel.loadAppDetailPackage(packageName)
    }
  }
  DisposableEffect(context, packageName) {
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        if (intent.data?.schemeSpecificPart != packageName) return
        if (intent.action == Intent.ACTION_PACKAGE_REMOVED) {
          currentOnBack()
        } else if (packageName != null) {
          packageInfo = null
          viewModel.loadAppDetailPackage(packageName)
        }
      }
    }
    context.registerReceiver(
      receiver,
      IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_REPLACED)
        addAction(Intent.ACTION_PACKAGE_REMOVED)
        addDataScheme("package")
      }
    )
    onDispose { context.unregisterReceiver(receiver) }
  }
  val insightViewModel: com.absinthe.libchecker.domain.app.detail.presentation.LibraryInsightViewModel = koinViewModel()
  Crossfade(
    targetState = packageInfo,
    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
    label = "AppDetailLoadTransition"
  ) { resolved ->
    if (resolved == null) {
      AppDetailSkeleton(onBack = onBack)
    } else {
      key(resolved.packageName) {
        AppDetailScreen(
          viewModel = viewModel,
          packageInfo = resolved,
          isHarmonyVariant = extra?.variant == Constants.VARIANT_HAP,
          insightViewModel = insightViewModel,
          referenceName = referenceName,
          referenceType = referenceType,
          onBack = onBack
        )
      }
    }
  }
}
