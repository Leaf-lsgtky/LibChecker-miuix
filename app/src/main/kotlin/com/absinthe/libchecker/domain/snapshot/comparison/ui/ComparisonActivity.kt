@file:Suppress("ktlint:standard:function-naming")

package com.absinthe.libchecker.domain.snapshot.comparison.ui

import androidx.compose.runtime.Composable
import com.absinthe.libchecker.domain.snapshot.comparison.ui.compose.ComparisonPage
import com.absinthe.libchecker.ui.base.BaseComposeActivity

class ComparisonActivity : BaseComposeActivity() {
  @Composable
  override fun Content() {
    ComparisonPage(intent = navigationIntent, onBack = { onBackPressedDispatcher.onBackPressed() })
  }
}
