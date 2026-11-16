@file:Suppress("ktlint:standard:function-naming")

package com.absinthe.libchecker.domain.snapshot.detail.ui

import androidx.compose.runtime.Composable
import com.absinthe.libchecker.domain.snapshot.detail.ui.compose.SnapshotDetailPage
import com.absinthe.libchecker.ui.base.BaseComposeActivity

const val EXTRA_ICON = "EXTRA_ICON"

class SnapshotDetailActivity : BaseComposeActivity() {
  @Composable
  override fun Content() {
    SnapshotDetailPage(intent = navigationIntent, onBack = { onBackPressedDispatcher.onBackPressed() })
  }
}
