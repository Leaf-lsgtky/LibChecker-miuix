package com.absinthe.libchecker.ui.base

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.backup.RoomBackup
import com.absinthe.libchecker.domain.snapshot.backup.ui.SnapshotRoomBackupOwner
import com.absinthe.libchecker.ui.compose.AppAppearanceStore
import com.absinthe.libchecker.ui.compose.navigation.LcNavHost
import com.absinthe.libchecker.ui.compose.theme.LcTheme
import com.absinthe.libchecker.utils.OsUtils
import java.util.Locale
import timber.log.Timber
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Base activity for Compose screens.
 *
 * Keeps the same locale, night-mode overlay and edge-to-edge behaviour as [BaseActivity] but hosts
 * a Compose tree instead of a ViewBinding layout.
 */
abstract class BaseComposeActivity : AppCompatActivity(), SnapshotRoomBackupOwner {

  private var appliedLocale: Locale? = null
  // Register the legacy backup launcher before STARTED, including when Album is a nav entry.
  final override val snapshotRoomBackup = RoomBackup(this)
  protected var navigationIntent by mutableStateOf(Intent())
    private set
  internal var navigateWithinApp: ((Intent) -> Boolean)? = null

  override fun startActivity(intent: Intent, options: Bundle?) {
    if (navigateWithinApp?.invoke(intent) != true) {
      super.startActivity(intent, options)
    }
  }

  override fun attachBaseContext(newBase: Context) {
    val locale = GlobalValues.locale
    appliedLocale = locale
    Locale.setDefault(locale)
    val configuration = Configuration(newBase.resources.configuration).apply {
      setLocale(locale)
      setLayoutDirection(locale)
    }
    super.attachBaseContext(newBase.createConfigurationContext(configuration))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    onApplyUserThemeResource(theme, false)
    if (shouldApplyTranslucentSystemBars()) {
      onApplyTranslucentSystemBars()
    }
    super.onCreate(savedInstanceState.discardIfContainsUnreadableParcelable(javaClass.classLoader))
    navigationIntent = intent
    ThemeTransitionController.animateEnterIfNeeded(this)
    setContent {
      LcTheme {
        val appearance = AppAppearanceStore.current
        val systemDark = isSystemInDarkTheme()
        val isDark = when (appearance.darkMode) {
          Constants.DARK_MODE_ON -> true
          Constants.DARK_MODE_OFF -> false
          else -> systemDark
        }
        DisposableEffect(isDark) {
          if (shouldApplyTranslucentSystemBars()) {
            enableEdgeToEdge(
              statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark },
              navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark }
            )
            if (OsUtils.atLeastQ()) {
              window.isNavigationBarContrastEnforced = false
            }
          }
          onDispose {}
        }
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MiuixTheme.colorScheme.surface
        ) {
          LcNavHost(this@BaseComposeActivity, navigationIntent) { Content() }
        }
      }
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    onApplyUserThemeResource(theme, false)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    navigationIntent = intent
  }

  override fun onResume() {
    super.onResume()
    if (OsUtils.atLeastT() && appliedLocale != GlobalValues.locale) {
      recreate()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    ThemeTransitionController.onActivityDestroyed(this)
  }

  @Composable
  abstract fun Content()

  open fun shouldApplyTranslucentSystemBars(): Boolean = true

  open fun onApplyTranslucentSystemBars() {
    enableEdgeToEdge()
    if (OsUtils.atLeastQ()) {
      window.isNavigationBarContrastEnforced = false
    }
  }

  open fun onApplyUserThemeResource(theme: Resources.Theme, isDecorView: Boolean) {
    theme.applyStyle(
      resolveUserThemeOverlay(GlobalValues.isAmoledTheme, resources.configuration.uiMode),
      true
    )
  }

  protected fun logBindingIssue(throwable: Throwable) {
    Timber.w(throwable)
  }
}
