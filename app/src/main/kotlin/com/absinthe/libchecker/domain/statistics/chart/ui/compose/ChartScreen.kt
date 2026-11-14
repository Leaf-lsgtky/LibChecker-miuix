package com.absinthe.libchecker.domain.statistics.chart.ui.compose

import android.widget.FrameLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.chart.presentation.ChartViewModel
import com.absinthe.libchecker.domain.statistics.chart.ui.resolve
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Statistics chart page: statistic selector plus the hosted MPAndroidChart view. */
@Composable
fun ChartScreen(
  viewModel: ChartViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scrollBehavior = MiuixScrollBehavior()
  val colorScheme = MiuixTheme.colorScheme
  val selectorPlan by viewModel.statisticSelectorPlan.collectAsStateWithLifecycle()
  val loadingProgress by viewModel.loadingProgress.collectAsStateWithLifecycle()

  val controller = remember(viewModel) {
    ChartHostController(
      context = context,
      scope = viewModel.viewModelScope,
      viewModel = viewModel,
      onProgress = { viewModel.setLoadingProgress(it) }
    )
  }

  LaunchedEffect(viewModel) {
    viewModel.chartRenderRequests.collect(controller::render)
  }

  Column(modifier = modifier.fillMaxSize()) {
    AdaptiveTopAppBar(
      title = stringResource(R.string.tab_chart),
      scrollBehavior = scrollBehavior,
      navigationIcon = {
        IconButton(onClick = onBack) {
          Icon(MiuixIcons.Back, contentDescription = null)
        }
      }
    )

    LazyRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp),
      contentPadding = PaddingValues(horizontal = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(items = selectorPlan.visibleStatistics, key = { it.id }) { statistic ->
        val selected = statistic.id == selectorPlan.selectedStatistic?.id
        Box(
          modifier = Modifier
            .squircleBackground(
              if (selected) colorScheme.primary else colorScheme.surfaceContainer,
              14.dp
            )
            .clickable { viewModel.selectStatistic(statistic.id) }
            .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
          Text(
            text = statistic.title.resolve(context),
            fontSize = 13.sp,
            fontWeight = FontWeight(550),
            color = if (selected) colorScheme.onPrimary else colorScheme.onSurface,
            maxLines = 1
          )
        }
      }
    }

    if (loadingProgress in 0..99) {
      LinearProgressIndicator(
        progress = loadingProgress / 100f,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp)
      )
    }

    Box(Modifier.weight(1f)) {
      AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
          FrameLayout(ctx).also { controller.attach(it) }
        }
      )
      if (loadingProgress in 0..99 && loadingProgress == 0) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          LcLoadingState()
        }
      }
    }
  }
}
