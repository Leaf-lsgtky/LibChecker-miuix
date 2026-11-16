package com.absinthe.libchecker.domain.app.detail.ui.compose

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import com.absinthe.libchecker.utils.extensions.openUrlInBrowser
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.window.WindowDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.statistics.AppStatisticAnalysisState
import com.absinthe.libchecker.domain.app.detail.statistics.AppStatisticRuleAnalysis
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationKind
import com.absinthe.libchecker.domain.statistics.chart.ui.resolve
import com.absinthe.libchecker.domain.statistics.chart.ui.resolveDrawable
import com.absinthe.libchecker.domain.statistics.chart.ui.toScaledAspectBitmap
import com.absinthe.libchecker.ui.compose.component.LcEmptyState
import com.absinthe.libchecker.ui.compose.component.SheetCardListSkeleton
import com.absinthe.libchecker.ui.compose.util.rememberActiveValue
import com.absinthe.libchecker.ui.compose.util.sheetBottomSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetContentSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetHeightTransition
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

private fun resolveRuleSubtitle(
  context: android.content.Context,
  analysis: AppStatisticRuleAnalysis
): String? {
  val calculation = analysis.definition.calculation
  return when (calculation.kind) {
    StatisticCalculationKind.PREDICATE ->
      calculation.predicate
        ?.let { if (analysis.matched) it.matchedTitle else it.unmatchedTitle }
        ?.resolve(context)

    StatisticCalculationKind.FACETS -> {
      val facets = calculation.facets
      val facetTitles = facets?.items.orEmpty()
        .filter { it.id in analysis.matchedFacetIds }
        .map { it.summaryTitleOrTitle(context) }
        .filter { it.isNotBlank() }
      if (facetTitles.isNotEmpty()) {
        facetTitles.joinToString(separator = " · ")
      } else {
        facets?.let { if (analysis.matched) it.matchedTitle else it.unmatchedTitle }
          ?.resolve(context)
      }
    }

    StatisticCalculationKind.NATIVE -> null
  }?.takeIf { it.isNotBlank() }
}

/** Online rule analysis sheet: detect which online rule definitions match this app. */
@Composable
fun OnlineRuleAnalysisSheet(
  show: Boolean,
  viewModel: DetailViewModel,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val state by viewModel.appStatisticAnalysisState.collectAsStateWithLifecycle()
  var detailAnalysis by remember {
    mutableStateOf<AppStatisticRuleAnalysis?>(null)
  }

  LaunchedEffect(show) {
    if (show) {
      viewModel.analyzeOnlineStatistics()
    }
  }

  val activeDetailAnalysis = rememberActiveValue(detailAnalysis)
  WindowDialog(
    show = detailAnalysis != null,
    title = activeDetailAnalysis.value?.definition?.title?.resolve(context) ?: "",
    summary = activeDetailAnalysis.value?.definition?.details?.description?.resolve(context) ?: "",
    onDismissRequest = { detailAnalysis = null },
    onDismissFinished = { if (detailAnalysis == null) activeDetailAnalysis.value = null }
  ) {
    val details = activeDetailAnalysis.value?.definition?.details
    if (details == null) {
      TextButton(
        text = stringResource(android.R.string.ok),
        onClick = { detailAnalysis = null },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.textButtonColorsPrimary()
      )
    } else {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        TextButton(
          text = stringResource(R.string.lib_detail_app_props_tip),
          onClick = {
            context.openUrlInBrowser(details.referenceUrl)
            detailAnalysis = null
          },
          modifier = Modifier.weight(1f)
        )
        TextButton(
          text = stringResource(android.R.string.ok),
          onClick = { detailAnalysis = null },
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.textButtonColorsPrimary()
        )
      }
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
        .sheetHeightTransition()
    ) {
      val currentState = state
      var lastResults by remember {
        mutableStateOf<AppStatisticAnalysisState.Results?>(null)
      }
      if (currentState is AppStatisticAnalysisState.Results) {
        lastResults = currentState
      }
      val mode = when (currentState) {
        AppStatisticAnalysisState.Idle,
        is AppStatisticAnalysisState.Loading -> OnlineRuleAnalysisMode.Loading

        AppStatisticAnalysisState.Empty -> OnlineRuleAnalysisMode.Empty
        AppStatisticAnalysisState.Error -> OnlineRuleAnalysisMode.Error
        is AppStatisticAnalysisState.Results -> OnlineRuleAnalysisMode.Results
      }
      Crossfade(
        targetState = mode,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f, fill = false),
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "OnlineRuleAnalysisTransition"
      ) { stateMode ->
        when (stateMode) {
          OnlineRuleAnalysisMode.Loading -> Column(
            modifier = Modifier.padding(bottom = sheetBottomSafePadding(16.dp)),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            SheetCardListSkeleton(
              rowCount = 4,
              showIcon = true,
              twoLine = true
            )
          }

          OnlineRuleAnalysisMode.Empty -> Box(
            modifier = Modifier.padding(bottom = sheetBottomSafePadding(16.dp))
          ) {
            LcEmptyState(
              title = stringResource(R.string.chart_statistics_no_available)
            )
          }

          OnlineRuleAnalysisMode.Error -> Text(
            text = stringResource(R.string.chart_statistics_online_error),
            style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(bottom = sheetBottomSafePadding(16.dp))
          )

          OnlineRuleAnalysisMode.Results -> {
            val current = lastResults ?: return@Crossfade
            LazyColumn(
              contentPadding = PaddingValues(bottom = sheetBottomSafePadding(16.dp)),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              items(current.analyses, key = { it.definition.id }) { analysis ->
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                  ),
                  onClick = if (analysis.definition.details != null) {
                    { detailAnalysis = analysis }
                  } else {
                    null
                  },
                  showIndication = analysis.definition.details != null,
                  insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    var icon by remember(analysis.definition.id) {
                      mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
                    }
                    LaunchedEffect(analysis.definition.id) {
                      icon = runCatching {
                        analysis.definition.icon.resolveDrawable(context)
                          ?.toScaledAspectBitmap(72)
                      }.getOrNull()
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
                        text = analysis.definition.title.resolve(context),
                        style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp),
                        color = MiuixTheme.colorScheme.onSurface
                      )
                      val subtitleText = resolveRuleSubtitle(context, analysis)
                      if (!subtitleText.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                          text = subtitleText,
                          style = MiuixTheme.textStyles.footnote2.copy(fontSize = 11.sp),
                          color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                      }
                    }
                    Text(
                      text = stringResource(
                        if (analysis.matched) {
                          R.string.app_detail_online_rules_matched
                        } else {
                          R.string.app_detail_online_rules_unmatched
                        }
                      ),
                      style = MiuixTheme.textStyles.footnote2.copy(fontSize = 11.sp),
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
    }
  }
}

private enum class OnlineRuleAnalysisMode {
  Loading,
  Empty,
  Error,
  Results
}

private fun com.absinthe.libchecker.domain.statistics.chart.model.StatisticFacetSpec
  .summaryTitleOrTitle(context: android.content.Context): String {
  return (shortTitle ?: title).resolve(context)
}
