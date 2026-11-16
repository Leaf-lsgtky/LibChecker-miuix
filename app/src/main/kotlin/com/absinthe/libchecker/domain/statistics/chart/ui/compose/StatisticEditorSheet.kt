package com.absinthe.libchecker.domain.statistics.chart.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticSource
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.basic.Icon
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import com.absinthe.libchecker.domain.statistics.chart.presentation.ChartViewModel
import com.absinthe.libchecker.domain.statistics.chart.ui.resolve
import com.absinthe.libchecker.domain.statistics.chart.ui.resolveDrawable
import com.absinthe.libchecker.domain.statistics.chart.ui.toScaledAspectBitmap
import com.absinthe.libchecker.ui.compose.util.sheetBottomSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetContentSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetHeightTransition
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/** Add, remove and reorder the statistics shown on the chart page. */
@Composable
fun StatisticEditorSheet(
  show: Boolean,
  viewModel: ChartViewModel,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val state by viewModel.statisticCatalogEditorState.collectAsStateWithLifecycle()
  var draggingId by remember { mutableStateOf<String?>(null) }
  var dragOffset by remember { mutableStateOf(0f) }
  val rowHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { 52.dp.toPx() }

  LaunchedEffect(show) {
    if (show) {
      viewModel.openStatisticCatalogEditor()
    }
  }

  WindowBottomSheet(
    show = show,
    title = stringResource(R.string.chart_statistics_edit),
    onDismissRequest = onDismiss
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .sheetHeightTransition()
    ) {
      LazyColumn(
        modifier = Modifier.weight(1f, fill = false),
        contentPadding = PaddingValues(bottom = sheetBottomSafePadding(16.dp)),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        item(key = "added-title") {
          SectionTitle(stringResource(R.string.chart_statistics_added))
        }
        itemsIndexed(state.selectedStatistics, key = { _, it -> "selected-${it.id}" }) { index, statistic ->
          StatisticEditorRow(
            statistic = statistic,
            draggable = true,
            actionLabel = "−",
            actionEnabled = state.selectedStatistics.size > 1,
            onAction = { viewModel.removeStatistic(statistic.id) },
            modifier = Modifier
              .zIndex(if (draggingId == statistic.id) 1f else 0f)
              .graphicsLayer {
                translationY = if (draggingId == statistic.id) dragOffset else 0f
              }
              .pointerInput(statistic.id, state.selectedStatistics.size) {
                detectDragGesturesAfterLongPress(
                  onDragStart = {
                    draggingId = statistic.id
                    dragOffset = 0f
                  },
                  onDrag = { change, dragAmount ->
                    change.consume()
                    dragOffset += dragAmount.y
                  },
                  onDragEnd = {
                    val from = state.selectedStatistics.indexOfFirst { it.id == statistic.id }
                    val shift = (dragOffset / rowHeightPx).roundToInt()
                    val to = (from + shift).coerceIn(0, state.selectedStatistics.lastIndex)
                    if (from >= 0 && to != from) {
                      viewModel.moveStatistic(from, to)
                    }
                    draggingId = null
                    dragOffset = 0f
                  },
                  onDragCancel = {
                    draggingId = null
                    dragOffset = 0f
                  }
                )
              }
          )
        }
        if (state.isRefreshing || state.refreshFailed) {
          item(key = "status") {
            Text(
              text = stringResource(
                if (state.isRefreshing) {
                  R.string.chart_statistics_loading_online
                } else {
                  R.string.chart_statistics_online_error
                }
              ),
              style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
              color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
              modifier = Modifier.padding(vertical = 8.dp)
            )
          }
        }
        item(key = "available-title") {
          SectionTitle(stringResource(R.string.chart_statistics_available))
        }
        val addable = state.addableStatistics
        if (addable.isEmpty()) {
          item(key = "available-empty") {
            Text(
              text = stringResource(R.string.chart_statistics_no_available),
              style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
              color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
              modifier = Modifier.padding(vertical = 8.dp)
            )
          }
        } else {
          items(addable, key = { "addable-${it.id}" }) { statistic ->
            StatisticEditorRow(
              statistic = statistic,
              actionLabel = "+",
              onAction = { viewModel.addStatistic(statistic) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SectionTitle(text: String) {
  Text(
    text = text,
    style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
  )
}

@Composable
private fun StatisticEditorRow(
  statistic: StatisticDefinition,
  actionLabel: String,
  onAction: () -> Unit,
  actionEnabled: Boolean = true,
  draggable: Boolean = false,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val colorScheme = MiuixTheme.colorScheme
  val iconSizePx = with(LocalDensity.current) { 24.dp.roundToPx() }
  var icon by remember(statistic.id) { mutableStateOf<ImageBitmap?>(null) }
  LaunchedEffect(statistic.id, colorScheme) {
    icon = runCatching {
      statistic.icon.resolveDrawable(context)?.toScaledAspectBitmap(iconSizePx)
    }.getOrNull()
  }
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.defaultColors(
      color = colorScheme.onSurface.copy(alpha = 0.04f)
    ),
    insideMargin = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (draggable) {
        Icon(
          painter = painterResource(R.drawable.ic_drag_indicator),
          contentDescription = stringResource(R.string.chart_statistics_drag),
          tint = colorScheme.onSurfaceVariantSummary,
          modifier = Modifier
            .padding(end = 8.dp)
            .size(18.dp)
        )
      }
      if (icon != null) {
        androidx.compose.foundation.Image(
          bitmap = icon!!,
          contentDescription = null,
          modifier = Modifier
            .padding(end = 12.dp)
            .size(24.dp)
        )
      }
      Column(Modifier.weight(1f)) {
        Text(
          text = statistic.title.resolve(context),
          style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp),
          color = colorScheme.onSurface
        )
        Text(
          text = stringResource(
            if (statistic.source == StatisticSource.BUILTIN) {
              R.string.chart_statistics_source_builtin
            } else {
              R.string.chart_statistics_source_online
            }
          ),
          style = MiuixTheme.textStyles.footnote2.copy(fontSize = 11.sp),
          color = colorScheme.onSurfaceVariantSummary
        )
      }
      Box(
        modifier = Modifier
          .squircleBackground(colorScheme.onSurface.copy(alpha = 0.06f), 12.dp)
          .clickable(enabled = actionEnabled, onClick = onAction)
          .padding(horizontal = 12.dp, vertical = 6.dp)
      ) {
        Text(
          text = actionLabel,
          style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp),
          color = if (actionEnabled) {
            colorScheme.onSurface
          } else {
            colorScheme.onSurfaceVariantSummary
          }
        )
      }
    }
  }
}
