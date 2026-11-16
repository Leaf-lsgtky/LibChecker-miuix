@file:Suppress("ktlint:standard:function-naming")

package com.absinthe.libchecker.ui.compose.navigation

import android.content.Intent
import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import com.absinthe.libchecker.domain.about.ui.AboutActivity
import com.absinthe.libchecker.domain.about.ui.LicensesActivity
import com.absinthe.libchecker.domain.about.ui.compose.AboutScreen
import com.absinthe.libchecker.domain.about.ui.compose.LicensesScreen
import com.absinthe.libchecker.domain.app.detail.ui.AppDetailActivity
import com.absinthe.libchecker.domain.app.detail.ui.compose.AppDetailPage
import com.absinthe.libchecker.domain.settings.ui.AppearanceActivity
import com.absinthe.libchecker.domain.settings.ui.compose.AppearanceScreen
import com.absinthe.libchecker.domain.snapshot.album.ui.AlbumActivity
import com.absinthe.libchecker.domain.snapshot.album.ui.compose.AlbumPage
import com.absinthe.libchecker.domain.snapshot.comparison.ui.ComparisonActivity
import com.absinthe.libchecker.domain.snapshot.comparison.ui.compose.ComparisonPage
import com.absinthe.libchecker.domain.snapshot.detail.ui.SnapshotDetailActivity
import com.absinthe.libchecker.domain.snapshot.detail.ui.compose.SnapshotDetailPage
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackActivity
import com.absinthe.libchecker.domain.snapshot.track.ui.compose.TrackScreen
import com.absinthe.libchecker.domain.statistics.chart.ui.ChartActivity
import com.absinthe.libchecker.domain.statistics.chart.ui.compose.ChartPage
import com.absinthe.libchecker.domain.statistics.reference.ui.LibReferenceActivity
import com.absinthe.libchecker.domain.statistics.reference.ui.compose.LibReferencePage
import com.absinthe.libchecker.ui.base.BaseComposeActivity
import java.util.UUID
import kotlinx.parcelize.Parcelize
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.navBackStackOf
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Parcelize
internal data class PageKey(val id: String, val intent: Intent? = null) : NavKey, Parcelable

// Android Intent extras (including existing Parcelable launch arguments) are saved by the
// platform. Stable entry IDs preserve each page's ViewModels and rememberSaveable state.
private val BackStackSaver = listSaver<NavBackStack, PageKey>(
  save = { stack -> stack.map { it as PageKey } },
  restore = { keys -> keys.map<PageKey, NavKey> { it }.toMutableStateList() }
)

private val internalPages = setOf(
  AppDetailActivity::class.java.name,
  AppearanceActivity::class.java.name,
  AboutActivity::class.java.name,
  LicensesActivity::class.java.name,
  AlbumActivity::class.java.name,
  ComparisonActivity::class.java.name,
  TrackActivity::class.java.name,
  SnapshotDetailActivity::class.java.name,
  LibReferenceActivity::class.java.name,
  ChartActivity::class.java.name
)

@Composable
fun LcNavHost(activity: BaseComposeActivity, rootIntent: Intent, root: @Composable () -> Unit) {
  val backStack = rememberSaveable(saver = BackStackSaver) { navBackStackOf(PageKey("root")) }
  val previousRootIntent = remember { arrayOf(rootIntent) }
  DisposableEffect(rootIntent) {
    if (previousRootIntent[0] !== rootIntent) {
      previousRootIntent[0] = rootIntent
      while (backStack.size > 1) backStack.removeLastOrNull()
    }
    onDispose { }
  }
  val focusManager = LocalFocusManager.current
  DisposableEffect(backStack.lastOrNull()) {
    focusManager.clearFocus()
    onDispose { }
  }
  DisposableEffect(activity, backStack) {
    val navigate: (Intent) -> Boolean = { intent ->
      val changesTask = intent.flags and (
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        ) != 0
      if (!changesTask && intent.component?.className.orEmpty() in internalPages) {
        backStack.add(PageKey(UUID.randomUUID().toString(), Intent(intent)))
        true
      } else {
        false
      }
    }
    activity.navigateWithinApp = navigate
    onDispose { activity.navigateWithinApp = null }
  }
  NavDisplay(
    backStack = backStack,
    modifier = Modifier.fillMaxSize(),
    effects = NavDisplayEffects(
      cornerClipRadius = rememberNavSystemCornerRadius(),
      cornerClipMode = NavCornerClipMode.All,
      backdropColor = MiuixTheme.colorScheme.surface
    ),
    onBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
  ) {
    entry<PageKey>(contentKey = { it.id }) { page ->
      Surface(modifier = Modifier.fillMaxSize(), color = MiuixTheme.colorScheme.surface) {
        val intent = page.intent
        if (intent == null) {
          root()
        } else {
          val onBack: () -> Unit = {
            if (backStack.lastOrNull() == page && backStack.size > 1) backStack.removeLastOrNull()
          }
          when (intent.component?.className.orEmpty()) {
            AppDetailActivity::class.java.name -> AppDetailPage(intent, onBack)
            AppearanceActivity::class.java.name -> AppearanceScreen(koinViewModel(), onBack)
            AboutActivity::class.java.name -> AboutScreen(
              onBack = onBack,
              onOpenLicenses = { activity.startActivity(Intent(activity, LicensesActivity::class.java)) }
            )
            LicensesActivity::class.java.name -> LicensesScreen(onBack = onBack)
            AlbumActivity::class.java.name -> AlbumPage(intent, onBack)
            ComparisonActivity::class.java.name -> ComparisonPage(intent, onBack)
            TrackActivity::class.java.name -> TrackScreen(viewModel = koinViewModel(), onBack = onBack)
            SnapshotDetailActivity::class.java.name -> SnapshotDetailPage(intent, onBack)
            LibReferenceActivity::class.java.name -> LibReferencePage(intent, onBack)
            ChartActivity::class.java.name -> ChartPage(onBack)
          }
        }
      }
    }
  }
}
