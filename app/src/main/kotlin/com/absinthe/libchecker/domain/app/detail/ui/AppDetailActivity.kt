@file:Suppress("ktlint:standard:function-naming")

package com.absinthe.libchecker.domain.app.detail.ui

import androidx.compose.runtime.Composable
import com.absinthe.libchecker.domain.app.detail.ui.compose.AppDetailPage
import com.absinthe.libchecker.ui.base.BaseComposeActivity

class AppDetailActivity : BaseComposeActivity() {
  @Composable
  override fun Content() {
    AppDetailPage(intent = navigationIntent, onBack = { onBackPressedDispatcher.onBackPressed() })
  }
}
