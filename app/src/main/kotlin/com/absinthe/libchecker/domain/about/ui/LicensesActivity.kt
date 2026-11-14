package com.absinthe.libchecker.domain.about.ui

import androidx.compose.runtime.Composable
import com.absinthe.libchecker.domain.about.ui.compose.LicensesScreen
import com.absinthe.libchecker.ui.base.BaseComposeActivity

/** Open source licences list. */
class LicensesActivity : BaseComposeActivity() {

  @Composable
  override fun Content() {
    LicensesScreen(onBack = { onBackPressedDispatcher.onBackPressed() })
  }
}
