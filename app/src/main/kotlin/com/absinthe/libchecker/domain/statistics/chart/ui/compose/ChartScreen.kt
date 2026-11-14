package com.absinthe.libchecker.domain.statistics.chart.ui.compose

import android.widget.FrameLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.chart.model.ClassifyDialogState
import com.absinthe.libchecker.domain.statistics.chart.presentation.ChartViewModel
import com.absinthe.libchecker.domain.statistics.chart.ui.resolve
import com.absinthe.libchecker.domain.statistics.chart.ui.resolveDrawable
import com.absinthe.libchecker.domain.app.list.ui.compose.AppIcon
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.LcEmptyState
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/** Statistics chart page: count legend, statistic selector and the hosted chart view. */
@Composable
fun ChartScreen(
  viewModel: ChartViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val localView = androidx.compose.ui.platform.LocalView.current
  val scrollBehavior = MiuixScrollBehavior()
  val colorScheme = MiuixTheme.colorScheme
  val selectorPlan by viewModel.statisticSelectorPlan.collectAsStateWithLifecycle()
  val loadingProgress by viewModel.loadingProgress.collectAsStateWithLifecycle()
  val detailedAbi by viewModel.detailAbiSwitch.collectAsStateWithLifecycle()
  val abiSwitchVisible by viewModel.detailAbiSwitchVisibility.collectAsStateWithLifecycle()
  var legend by remember { mutableStateOf<List<ChartLegendEntry>>(emptyList()) }
  var classifyState by remember { mutableStateOf<ClassifyDialogState?>(null) }
  var showEditor by remember { mutableStateOf(false) }
  var empty by remember { mutableStateOf(false) }
  var subtitle by remember { mutableStateOf<String?>(null) }
  val selectedDefinitions by viewModel.selectedStatisticDefinitions.collectAsStateWithLifecycle()
  val currentStatisticTitle = selectedDefinitions.firstOrNull()?.title
    ?.let { spec -> runCatching { spec.resolve(context) }.getOrNull() }
  val scope = rememberCoroutineScope()

  val controller = remember(viewModel) {
    ChartHostController(
      context = context,
      scope = viewModel.viewModelScope,
      viewModel = viewModel,
      onProgress = { viewModel.setLoadingProgress(it) },
      onLegend = { legend = it },
      onEmpty = { empty = it },
      onSubtitle = { subtitle = it },
      onSliceSelected = { source, x ->
        runCatching {
          localView.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
        }
        scope.launch {
          classifyState = runCatching {
            viewModel.buildClassifyDialogState(
              source = source,
              x = x,
              title = currentStatisticTitle ?: source.getLabelByXValue(context, x),
              subtitle = source.getLabelByXValue(context, x)
                .takeIf { currentStatisticTitle != null }
            )
          }.getOrNull()
        }
      }
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
          Icon(MiuixIcons.Back, contentDescription = stringResource(R.string.action_back))
        }
      },
      actions = {
        if (abiSwitchVisible) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = stringResource(
                if (detailedAbi) R.string.chart_abi_detailed else R.string.chart_abi_concise
              ),
              style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
              color = colorScheme.onSurfaceVariantSummary
            )
            Spacer(Modifier.size(6.dp))
            Switch(
              checked = detailedAbi,
              onCheckedChange = { viewModel.setDetailAbiSwitch(it) }
            )
            Spacer(Modifier.size(12.dp))
          }
        }
      }
    )

    subtitle?.let { text ->
      Text(
        text = text,
        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
        color = colorScheme.onSurfaceVariantSummary,
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            copyToClipboardWithToast(context, com.absinthe.libchecker.api.ApiManager.ANDROID_VERSION_DISTRIBUTION_URL)
          }
          .padding(horizontal = 16.dp, vertical = 4.dp)
      )
    }

    if (legend.isNotEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp)
      ) {
        legend.forEach { entry ->
          ChartLegendRow(
            entry = entry,
            onClick = {
              if (entry.count == 0) return@ChartLegendRow
              val source = controller.currentSource
              if (source != null) {
                scope.launch {
                  classifyState = runCatching {
                    viewModel.buildClassifyDialogState(
                      source = source,
                      x = entry.x,
                      title = currentStatisticTitle ?: entry.label,
                      subtitle = entry.label.takeIf { currentStatisticTitle != null }
                    )
                  }.getOrNull()
                }
              }
            }
          )
        }
      }
      Spacer(Modifier.height(4.dp))
    }

    LazyRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp),
      contentPadding = PaddingValues(horizontal = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(items = selectorPlan.visibleStatistics, key = { it.id }) { statistic ->
        StatisticChip(
          statistic = statistic,
          selected = statistic.id == selectorPlan.selectedStatistic?.id,
          onClick = { viewModel.selectStatistic(statistic.id) }
        )
      }
      item(key = "add-statistic") {
        Box(
          modifier = Modifier
            .squircleBackground(colorScheme.surfaceContainer, 20.dp)
            .clickable { showEditor = true }
            .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
          Icon(
            imageVector = MiuixIcons.Add,
            contentDescription = stringResource(R.string.chart_statistics_add),
            tint = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(20.dp)
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
      if (empty && loadingProgress !in 0..99) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          LcEmptyState(title = stringResource(R.string.chart_no_data))
        }
      } else if (loadingProgress in 0..99 && loadingProgress == 0) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          LcLoadingState()
        }
      }
    }
  }

  StatisticEditorSheet(
    show = showEditor,
    viewModel = viewModel,
    onDismiss = { showEditor = false }
  )

  classifyState?.let { state ->
    WindowBottomSheet(
      show = true,
      title = state.title,
      onDismissRequest = { classifyState = null }
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .padding(bottom = 16.dp)
      ) {
        state.androidVersion?.let { version ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            version.iconRes?.let { iconRes ->
              AsyncImage(
                model = iconRes,
                contentDescription = null,
                modifier = Modifier
                  .padding(end = 6.dp)
                  .size(18.dp)
              )
            }
            Text(
              text = version.text,
              style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
              color = colorScheme.onSurfaceVariantSummary
            )
          }
          Spacer(Modifier.height(4.dp))
        }
        state.subtitle?.let { subtitle ->
          Text(
            text = subtitle,
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
            color = colorScheme.onSurfaceVariantSummary
          )
          Spacer(Modifier.height(8.dp))
        }
        LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
          items(state.items, key = { it.packageName }) { item ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  (context as? androidx.fragment.app.FragmentActivity)?.launchDetailPage(item)
                  classifyState = null
                }
                .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              val packageInfo = remember(item.packageName) {
                runCatching {
                  com.absinthe.libchecker.compat.PackageManagerCompat.getPackageInfo(
                    item.packageName,
                    0
                  )
                }.getOrNull()
              }
              AppIcon(
                packageInfo = packageInfo,
                label = item.label,
                modifier = Modifier
                  .padding(end = 12.dp)
                  .size(40.dp)
              )
              Column(Modifier.weight(1f)) {
                Text(
                  text = item.label,
                  style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp),
                  color = colorScheme.onSurface
                )
                Text(
                  text = item.packageName,
                  style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                  color = colorScheme.onSurfaceVariantSummary
                )
              }
              state.itemChips[item.packageName]?.firstOrNull()?.let { chip ->
                Box(
                  modifier = Modifier
                    .squircleBackground(colorScheme.tertiaryContainer, 6.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = chip,
                    style = MiuixTheme.textStyles.footnote2.copy(fontSize = 10.sp),
                    color = colorScheme.onTertiaryContainer
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

@Composable
private fun ChartLegendRow(entry: ChartLegendEntry, onClick: () -> Unit) {
  val colorScheme = MiuixTheme.colorScheme
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .padding(end = 12.dp)
        .size(40.dp)
        .squircleBackground(colorScheme.surfaceContainerHigh, 20.dp),
      contentAlignment = Alignment.Center
    ) {
      val rowContext = androidx.compose.ui.platform.LocalContext.current
      var legendIcon by remember(entry) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
      }
      LaunchedEffect(entry) {
        legendIcon = entry.statisticIcon?.let { spec ->
          runCatching { spec.resolveDrawable(rowContext) }.getOrNull()?.let { drawable ->
            runCatching { drawable.toBitmap(72, 72).asImageBitmap() }.getOrNull()
          }
        }
      }
      val loadedIcon = legendIcon
      if (loadedIcon != null) {
        androidx.compose.foundation.Image(
          bitmap = loadedIcon,
          contentDescription = null,
          modifier = Modifier.size(24.dp)
        )
      } else {
        AsyncImage(
          model = entry.iconRes,
          contentDescription = null,
          colorFilter = if (entry.isGrayIcon) {
            androidx.compose.ui.graphics.ColorFilter.colorMatrix(
              androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }
            )
          } else {
            null
          },
          modifier = Modifier.size(24.dp)
        )
      }
    }
    Text(
      text = entry.label,
      modifier = Modifier.weight(1f),
      style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp),
      color = colorScheme.onSurface
    )
    Text(
      text = entry.count.toString(),
      style = MiuixTheme.textStyles.title1.copy(fontSize = 28.sp),
      color = colorScheme.onSurface
    )
  }
}

@Composable
private fun StatisticChip(
  statistic: com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition,
  selected: Boolean,
  onClick: () -> Unit
) {
  val context = LocalContext.current
  val localView = androidx.compose.ui.platform.LocalView.current
  val colorScheme = MiuixTheme.colorScheme
  var icon by remember(statistic.id) {
    mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
  }
  LaunchedEffect(statistic.id) {
    icon = runCatching {
      statistic.icon.resolveDrawable(context)
        ?.toBitmap(48, 48)
        ?.asImageBitmap()
    }.getOrNull()
  }
  Row(
    modifier = Modifier
      .squircleBackground(
        if (selected) colorScheme.surfaceContainerHigh else colorScheme.surfaceContainer,
        20.dp
      )
      .clickable(onClick = onClick)
      .padding(horizontal = if (selected) 12.dp else 8.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (icon != null) {
      androidx.compose.foundation.Image(
        bitmap = icon!!,
        contentDescription = null,
        modifier = Modifier.size(24.dp)
      )
    }
    if (selected) {
      Spacer(Modifier.size(6.dp))
      Text(
        text = statistic.title.resolve(context),
        style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
        color = colorScheme.onSurface,
        maxLines = 1
      )
    }
  }
}

private fun copyToClipboardWithToast(context: android.content.Context, text: String) {
  val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
  clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("", text))
  com.absinthe.libchecker.compat.VersionCompat.showCopiedOnClipboardToast(context)
}