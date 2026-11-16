@file:Suppress("ktlint:standard:function-naming")

package com.absinthe.libchecker.domain.statistics.chart.ui

import androidx.compose.runtime.Composable
import com.absinthe.libchecker.domain.statistics.chart.ui.compose.ChartPage
import com.absinthe.libchecker.ui.base.BaseComposeActivity

class ChartActivity : BaseComposeActivity() {
  @Composable
  override fun Content() {
    ChartPage(onBack = { onBackPressedDispatcher.onBackPressed() })
  }
}
