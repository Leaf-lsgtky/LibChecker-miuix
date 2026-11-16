@file:Suppress("ktlint:standard:function-naming")

package com.absinthe.libchecker.domain.statistics.reference.ui

import androidx.compose.runtime.Composable
import com.absinthe.libchecker.domain.statistics.reference.ui.compose.LibReferencePage
import com.absinthe.libchecker.ui.base.BaseComposeActivity

const val EXTRA_REF_NAME = "REF_NAME"
const val EXTRA_REF_LABEL = "REF_LABEL"
const val EXTRA_REF_TYPE = "REF_TYPE"
const val EXTRA_REF_LIST = "REF_LIST"

class LibReferenceActivity : BaseComposeActivity() {
  @Composable
  override fun Content() {
    LibReferencePage(intent = navigationIntent, onBack = { onBackPressedDispatcher.onBackPressed() })
  }
}
