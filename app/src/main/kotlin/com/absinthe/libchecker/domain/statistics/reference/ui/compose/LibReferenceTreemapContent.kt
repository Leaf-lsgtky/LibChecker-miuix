package com.absinthe.libchecker.domain.statistics.reference.ui.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
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
  aggregateSmallReferences: Boolean = true,
  onReferenceClick: (LibReference) -> Unit,
  modifier: Modifier = Modifier
) {
  AndroidView(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 12.dp)
      .padding(top = 6.dp, bottom = 6.dp),
    factory = { context ->
      LibReferenceTreemapView(context).apply {
        this.onReferenceClick = onReferenceClick
      }
    },
    update = { view ->
      view.colorfulRuleIcon = colorfulRuleIcon
      view.submitReferences(
        references,
        animate = true,
        aggregateSmallReferences = aggregateSmallReferences
      )
    }
  )
}