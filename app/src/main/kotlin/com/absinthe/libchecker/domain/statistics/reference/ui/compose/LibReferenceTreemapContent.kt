package com.absinthe.libchecker.domain.statistics.reference.ui.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import com.absinthe.libchecker.domain.statistics.reference.ui.view.LibReferenceTreemapView

/** Hosts the treemap view used by the statistics page. */
@Composable
fun LibReferenceTreemapContent(
  references: List<LibReference>,
  colorfulRuleIcon: Boolean,
  onReferenceClick: (LibReference) -> Unit,
  modifier: Modifier = Modifier
) {
  AndroidView(
    modifier = modifier.fillMaxSize(),
    factory = { context ->
      LibReferenceTreemapView(context).apply {
        this.onReferenceClick = onReferenceClick
      }
    },
    update = { view ->
      view.colorfulRuleIcon = colorfulRuleIcon
      view.submitReferences(references)
    }
  )
}
