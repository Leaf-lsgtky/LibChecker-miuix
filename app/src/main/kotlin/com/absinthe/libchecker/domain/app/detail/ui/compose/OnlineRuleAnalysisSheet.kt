package com.absinthe.libchecker.domain.app.detail.ui.compose

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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.statistics.AppStatisticAnalysisState
import com.absinthe.libchecker.domain.statistics.chart.ui.resolve
import com.absinthe.libchecker.domain.statistics.chart.ui.resolveDrawable
import com.absinthe.libchecker.ui.compose.component.LcEmptyState
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/** Online rule analysis sheet: detect which online rule definitions match this app. */
@Composable
fun OnlineRuleAnalysisSheet(
  show: Boolean,
  viewModel: DetailViewModel,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val state by viewModel.appStatisticAnalysisState.collectAsStateWithLifecycle()

  LaunchedEffect(show) {
    if (show) {
      viewModel.analyzeOnlineStatistics()
    }
  }

  WindowBottomSheet(
    show = show,
    title = stringResource(R.string.chart_statistics_online_analysis),
    onDismissRequest = onDismiss
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp)
    ) {
      when (val current = state) {
        AppStatisticAnalysisState.Idle,
        is AppStatisticAnalysisState.Loading -> Column {
          LcLoadingState(
            modifier = Modifier.height(120.dp),
            message = stringResource(R.string.loading)
          )
          if (current is AppStatisticAnalysisState.Loading) {
            LinearProgressIndicator(
              progress = current.progress / 100f,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }

        AppStatisticAnalysisState.Empty -> LcEmptyState(
          title = stringResource(R.string.chart_statistics_no_available)
        )

        AppStatisticAnalysisState.Error -> Text(
          text = stringResource(R.string.chart_statistics_online_error),
          fontSize = 13.sp,
          color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )

        is AppStatisticAnalysisState.Results -> LazyColumn(
          modifier = Modifier.heightIn(max = 480.dp)
        ) {
          items(current.analyses, key = { it.definition.id }) { analysis ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              var icon by remember(analysis.definition.id) {
                mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
              }
              LaunchedEffect(analysis.definition.id) {
                icon = runCatching {
                  analysis.definition.icon.resolveDrawable(context)
                    ?.toBitmap(48, 48)
                    ?.asImageBitmap()
                }.getOrNull()
              }
              if (icon != null) {
                androidx.compose.foundation.Image(
                  bitmap = icon!!,
                  contentDescription = null,
                  modifier = Modifier
                    .padding(end = 10.dp)
                    .size(24.dp)
                )
              }
              Column(Modifier.weight(1f)) {
                Text(
                  text = analysis.definition.title.resolve(context),
                  fontSize = 14.sp,
                  fontWeight = FontWeight(550),
                  color = MiuixTheme.colorScheme.onSurface
                )
                if (analysis.matchedFacetIds.isNotEmpty()) {
                  Spacer(Modifier.height(2.dp))
                  Text(
                    text = analysis.matchedFacetIds.joinToString(),
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                  )
                }
              }
              Text(
                text = stringResource(
                  if (analysis.matched) {
                    R.string.chart_statistics_added
                  } else {
                    R.string.chart_item_not_support
                  }
                ),
                fontSize = 11.sp,
                color = if (analysis.matched) {
                  MiuixTheme.colorScheme.onSurface
                } else {
                  MiuixTheme.colorScheme.onSurfaceVariantSummary
                }
              )
            }
          }
        }
      }
    }
  }
}
