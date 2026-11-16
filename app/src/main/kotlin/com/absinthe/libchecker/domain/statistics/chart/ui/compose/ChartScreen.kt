package com.absinthe.libchecker.domain.statistics.chart.ui.compose

import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.list.ui.compose.AppIcon
import com.absinthe.libchecker.domain.statistics.chart.model.ClassifyDialogState
import com.absinthe.libchecker.domain.statistics.chart.presentation.ChartViewModel
import com.absinthe.libchecker.domain.statistics.chart.ui.resolve
import com.absinthe.libchecker.domain.statistics.chart.ui.resolveDrawable
import com.absinthe.libchecker.domain.statistics.chart.ui.toScaledAspectBitmap
import com.absinthe.libchecker.ui.compose.component.AdaptiveBackButton
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.LcEmptyState
import com.absinthe.libchecker.ui.compose.component.LcMiuixLoading
import com.absinthe.libchecker.ui.compose.component.MiuixPageHost
import com.absinthe.libchecker.ui.compose.theme.lcCondensedFontFamily
import com.absinthe.libchecker.ui.compose.theme.withCustomFont
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import com.absinthe.libchecker.ui.compose.util.rememberActiveValue
import com.absinthe.libchecker.ui.compose.util.sheetBottomSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetHeightTransition
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.Add
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import com.absinthe.libchecker.ui.compose.component.FastScrollLazyColumn as LazyColumn

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
  val latestStatisticTitle by rememberUpdatedState(currentStatisticTitle)
  val scope = rememberCoroutineScope()
  var classifyJob by remember { mutableStateOf<Job?>(null) }

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
        if (classifyState != null || classifyJob?.isActive == true) {
          return@ChartHostController
        }
        runCatching {
          localView.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
        }
        classifyJob = scope.launch {
          classifyState = runCatching {
            viewModel.buildClassifyDialogState(
              source = source,
              x = x,
              title = latestStatisticTitle ?: source.getLabelByXValue(context, x),
              subtitle = source.getLabelByXValue(context, x)
                .takeIf { latestStatisticTitle != null }
            )
          }.getOrNull()
        }
      }
    )
  }

  LaunchedEffect(viewModel) {
    viewModel.chartRenderRequests.collect(controller::render)
  }

  MiuixPageHost(modifier = modifier.fillMaxSize()) {
    val pageState = this
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        AdaptiveTopAppBar(
          title = stringResource(R.string.tab_chart),
          scrollBehavior = scrollBehavior,
          isContentScrolled = false,
          pinned = true,
          navigationIcon = {
            AdaptiveBackButton(onClick = onBack)
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
      }
    ) { padding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .then(pageState.topBarBackdropModifier)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .horizontalCutoutPadding()
        ) {
          val activeSubtitle = rememberActiveValue(subtitle)
          AnimatedVisibility(
            visible = subtitle != null,
            enter = expandVertically(tween(260, easing = FastOutSlowInEasing)) +
              fadeIn(tween(220, easing = FastOutSlowInEasing)),
            exit = shrinkVertically(tween(220, easing = FastOutSlowInEasing)) +
              fadeOut(tween(160, easing = FastOutSlowInEasing))
          ) {
            activeSubtitle.value?.let { text ->
              Text(
                text = text,
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp)
                  .withCustomFont(lcCondensedFontFamily()),
                color = colorScheme.onSurfaceVariantSummary,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    copyToClipboardWithToast(
                      context,
                      com.absinthe.libchecker.api.ApiManager.ANDROID_VERSION_DISTRIBUTION_URL
                    )
                  }
                  .padding(horizontal = 16.dp, vertical = 4.dp)
              )
            }
          }

          val activeLegend = rememberActiveValue(legend.takeIf { it.isNotEmpty() })
          AnimatedVisibility(
            visible = legend.isNotEmpty(),
            enter = expandVertically(tween(280, easing = FastOutSlowInEasing)) +
              fadeIn(tween(220, easing = FastOutSlowInEasing)),
            exit = shrinkVertically(tween(240, easing = FastOutSlowInEasing)) +
              fadeOut(tween(160, easing = FastOutSlowInEasing))
          ) {
            activeLegend.value?.let { entries ->
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp)
                  .padding(top = 6.dp, bottom = 10.dp)
                  .squircleClip(CardDefaults.CornerRadius)
                  .animateContentSize(tween(260, easing = FastOutSlowInEasing)),
                insideMargin = PaddingValues(0.dp)
              ) {
                entries.forEachIndexed { index, entry ->
                  ChartLegendRow(
                    entry = entry,
                    isFirst = index == 0,
                    isLast = index == entries.lastIndex,
                    onClick = {
                      if (entry.count == 0 || classifyState != null || classifyJob?.isActive == true) {
                        return@ChartLegendRow
                      }
                      val source = controller.currentSource
                      if (source != null) {
                        classifyJob = scope.launch {
                          classifyState = runCatching {
                            viewModel.buildClassifyDialogState(
                              source = source,
                              x = entry.x,
                              title = latestStatisticTitle ?: entry.label,
                              subtitle = entry.label.takeIf { latestStatisticTitle != null }
                            )
                          }.getOrNull()
                        }
                      }
                    }
                  )
                }
              }
            }
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
                onClick = { viewModel.selectStatistic(statistic.id) },
                modifier = Modifier.animateItem()
              )
            }
            item(key = "add-statistic") {
              Box(
                modifier = Modifier
                  .animateItem()
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(colorScheme.surfaceContainer)
                  .clickable { showEditor = true },
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = MiuixIcons.Glass.Add,
                  contentDescription = stringResource(R.string.chart_statistics_add),
                  tint = colorScheme.onSurfaceVariantSummary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }

          AnimatedVisibility(
            visible = loadingProgress in 0..99,
            enter = expandVertically(tween(200)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
          ) {
            LinearProgressIndicator(
              progress = loadingProgress.coerceIn(0, 100) / 100f,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp)
            )
          }

          Card(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
              .padding(horizontal = 12.dp)
              .padding(bottom = 12.dp),
            insideMargin = PaddingValues(8.dp)
          ) {
            Box(Modifier.fillMaxSize()) {
              AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                  FrameLayout(ctx).also { controller.attach(it) }
                }
              )
              androidx.compose.animation.AnimatedVisibility(
                visible = empty && loadingProgress !in 0..99,
                enter = fadeIn(tween(durationMillis = 240, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(durationMillis = 200, easing = FastOutSlowInEasing))
              ) {
                Box(
                  modifier = Modifier.fillMaxSize(),
                  contentAlignment = Alignment.Center
                ) {
                  LcEmptyState(title = stringResource(R.string.chart_no_data))
                }
              }
              androidx.compose.animation.AnimatedVisibility(
                visible = loadingProgress == 0,
                enter = fadeIn(tween(durationMillis = 220, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(durationMillis = 220, easing = FastOutSlowInEasing))
              ) {
                Box(
                  modifier = Modifier.fillMaxSize(),
                  contentAlignment = Alignment.Center
                ) {
                  LcMiuixLoading()
                }
              }
            }
          }
        }
      }
    }
  }

  StatisticEditorSheet(
    show = showEditor,
    viewModel = viewModel,
    onDismiss = { showEditor = false }
  )

  val activeClassifyState = rememberActiveValue(classifyState)
  WindowBottomSheet(
    show = classifyState != null,
    title = activeClassifyState.value?.title ?: "",
    onDismissRequest = {
      classifyState = null
      controller.clearHighlight()
    },
    onDismissFinished = {
      if (classifyState == null) {
        activeClassifyState.value = null
        controller.clearHighlight()
      }
    }
  ) {
    activeClassifyState.value?.let { state ->
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .sheetHeightTransition()
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
        LazyColumn(
          modifier = Modifier.weight(1f, fill = false),
          contentPadding = PaddingValues(bottom = sheetBottomSafePadding(16.dp)),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(state.items, key = { it.packageName }) { item ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              onClick = {
                (context as? androidx.fragment.app.FragmentActivity)?.launchDetailPage(item)
                classifyState = null
              },
              showIndication = true,
              colors = CardDefaults.defaultColors(
                color = colorScheme.onSurface.copy(alpha = 0.04f)
              ),
              insideMargin = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                val packageInfo by produceState<android.content.pm.PackageInfo?>(
                  initialValue = null,
                  key1 = item.packageName
                ) {
                  value = withContext(Dispatchers.IO) {
                    runCatching {
                      com.absinthe.libchecker.compat.PackageManagerCompat.getPackageInfo(
                        item.packageName,
                        0
                      )
                    }.getOrNull()
                  }
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
}

@Composable
private fun ChartLegendRow(
  entry: ChartLegendEntry,
  isFirst: Boolean,
  isLast: Boolean,
  onClick: () -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  val rowContext = LocalContext.current
  val iconSizePx = with(LocalDensity.current) { 24.dp.roundToPx() }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(
        start = 16.dp,
        end = 16.dp,
        top = if (isFirst) 12.dp else 8.dp,
        bottom = if (isLast) 12.dp else 8.dp
      ),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .padding(end = 12.dp)
        .size(40.dp)
        .squircleBackground(colorScheme.surfaceContainerHigh, 20.dp),
      contentAlignment = Alignment.Center
    ) {
      var legendIcon by remember(entry, colorScheme) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
      }
      LaunchedEffect(entry, colorScheme) {
        legendIcon = entry.statisticIcon?.let { spec ->
          runCatching { spec.resolveDrawable(rowContext) }.getOrNull()?.let { drawable ->
            runCatching { drawable.toScaledAspectBitmap(iconSizePx) }.getOrNull()
          }
        }
      }
      val loadedIcon = legendIcon
      if (loadedIcon != null) {
        Image(
          bitmap = loadedIcon,
          contentDescription = null,
          contentScale = ContentScale.Fit,
          modifier = Modifier.size(24.dp)
        )
      } else {
        AsyncImage(
          model = entry.iconRes,
          contentDescription = null,
          contentScale = ContentScale.Fit,
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
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val colorScheme = MiuixTheme.colorScheme
  val iconSizePx = with(LocalDensity.current) { 24.dp.roundToPx() }
  var icon by remember(statistic.id, colorScheme) {
    mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
  }
  LaunchedEffect(statistic.id, colorScheme) {
    icon = runCatching {
      statistic.icon.resolveDrawable(context)?.toScaledAspectBitmap(iconSizePx)
    }.getOrNull()
  }
  val chipBackground by animateColorAsState(
    targetValue = if (selected) colorScheme.surfaceContainerHigh else colorScheme.surfaceContainer,
    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
    label = "StatisticChipBg"
  )
  val endPadding by animateDpAsState(
    targetValue = if (selected) 12.dp else 8.dp,
    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
    label = "StatisticChipEndPadding"
  )
  Row(
    modifier = modifier
      .squircleBackground(chipBackground, 20.dp)
      .squircleClip(20.dp)
      .clickable(onClick = onClick)
      .padding(start = 8.dp, end = endPadding, top = 8.dp, bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (icon != null) {
      Image(
        bitmap = icon!!,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(24.dp)
      )
    }
    AnimatedVisibility(
      visible = selected,
      enter = expandHorizontally(tween(240, easing = FastOutSlowInEasing)) +
        fadeIn(tween(200, easing = FastOutSlowInEasing)),
      exit = shrinkHorizontally(tween(200, easing = FastOutSlowInEasing)) +
        fadeOut(tween(150, easing = FastOutSlowInEasing))
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
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
}

private fun copyToClipboardWithToast(context: android.content.Context, text: String) {
  val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
  clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("", text))
  com.absinthe.libchecker.compat.VersionCompat.showCopiedOnClipboardToast(context)
}
