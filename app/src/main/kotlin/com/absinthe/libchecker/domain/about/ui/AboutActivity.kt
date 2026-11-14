package com.absinthe.libchecker.domain.about.ui

import androidx.compose.runtime.Composable
import com.absinthe.libchecker.domain.about.ui.compose.AboutScreen
import com.absinthe.libchecker.ui.base.BaseComposeActivity

/** Compose about page; the licence list itself is still provided by AboutLibraries. */
class AboutActivity : BaseComposeActivity() {

  @Composable
  override fun Content() {
    AboutScreen(
      onBack = { onBackPressedDispatcher.onBackPressed() },
      onOpenLicenses = { AboutPageBuilder.start(this) }
    )
  }
}
