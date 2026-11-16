package com.absinthe.libchecker.domain.settings.ui

import androidx.compose.runtime.Composable
import com.absinthe.libchecker.domain.settings.presentation.SettingsViewModel
import com.absinthe.libchecker.domain.settings.ui.compose.AppearanceScreen
import com.absinthe.libchecker.ui.base.BaseComposeActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Dedicated activity for appearance preferences so transitions and dark-mode updates stay smooth. */
class AppearanceActivity : BaseComposeActivity() {

  private val settingsViewModel: SettingsViewModel by viewModel()

  @Composable
  override fun Content() {
    AppearanceScreen(
      settingsViewModel = settingsViewModel,
      onBack = { onBackPressedDispatcher.onBackPressed() }
    )
  }
}
