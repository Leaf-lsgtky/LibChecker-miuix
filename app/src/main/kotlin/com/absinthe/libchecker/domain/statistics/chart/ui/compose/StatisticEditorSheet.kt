package com.absinthe.libchecker.domain.statistics.chart.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import com.absinthe.libchecker.domain.statistics.chart.presentation.ChartViewModel
import com.absinthe.libchecker.domain.statistics.chart.ui.resolve
import com.absinthe.libchecker.domain.statistics.chart.ui.resolveDrawable
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
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp)
    ) {
      LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
        item(key = "added-title") {
          SectionTitle(stringResource(R.string.chart_statistics_added))
        }
        items(state.selectedStatistics, key = { "selected-${it.id}" }) { statistic ->
          StatisticEditorRow(
            statistic = statistic,
            actionLabel = "−",
            onAction = { viewModel.removeStatistic(statistic.id) }
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
              fontSize = 12.sp,
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
              fontSize = 12.sp,
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
    fontSize = 13.sp,
    fontWeight = FontWeight(600),
    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
  )
}

@Composable
private fun StatisticEditorRow(
  statistic: StatisticDefinition,
  actionLabel: String,
  onAction: () -> Unit
) {
  val context = LocalContext.current
  val colorScheme = MiuixTheme.colorScheme
  var icon by remember(statistic.id) { mutableStateOf<ImageBitmap?>(null) }
  LaunchedEffect(statistic.id) {
    icon = runCatching {
      statistic.icon.resolveDrawable(context)?.toBitmap(48, 48)?.asImageBitmap()
    }.getOrNull()
  }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (icon != null) {
      androidx.compose.foundation.Image(
        bitmap = icon!!,
        contentDescription = null,
        modifier = Modifier
          .padding(end = 12.dp)
          .size(24.dp)
      )
    }
    Text(
      text = statistic.title.resolve(context),
      modifier = Modifier.weight(1f),
      fontSize = 14.sp,
      color = colorScheme.onSurface
    )
    Box(
      modifier = Modifier
        .squircleBackground(colorScheme.surfaceContainerHigh, 12.dp)
        .clickable(onClick = onAction)
        .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
      Text(
        text = actionLabel,
        fontSize = 14.sp,
        fontWeight = FontWeight(600),
        color = colorScheme.onSurface
      )
    }
  }
}
