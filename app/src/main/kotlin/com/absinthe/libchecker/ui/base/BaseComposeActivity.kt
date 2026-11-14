package com.absinthe.libchecker.ui.base

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.ui.compose.theme.LcTheme
import com.absinthe.libchecker.utils.OsUtils
import java.util.Locale
import timber.log.Timber

/**
 * Base activity for Compose screens.
 *
 * Keeps the same locale, night-mode overlay and edge-to-edge behaviour as [BaseActivity] but hosts
 * a Compose tree instead of a ViewBinding layout.
 */
abstract class BaseComposeActivity : AppCompatActivity() {

  private var appliedLocale: Locale? = null

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
    ThemeTransitionController.animateEnterIfNeeded(this)
    setContent {
      LcTheme {
        Content()
      }
    }
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
