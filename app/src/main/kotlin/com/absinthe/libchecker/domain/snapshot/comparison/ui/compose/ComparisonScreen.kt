package com.absinthe.libchecker.domain.snapshot.comparison.ui.compose

import android.content.pm.PackageInfo
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.absinthe.libchecker.R
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.app.list.ui.compose.AppIcon
import com.absinthe.libchecker.domain.snapshot.comparison.model.ComparisonDashboardLabels
import com.absinthe.libchecker.domain.snapshot.comparison.model.ComparisonDashboardSideState
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonPlan
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonSide
import com.absinthe.libchecker.domain.snapshot.comparison.presentation.SnapshotComparisonViewModel
import com.absinthe.libchecker.domain.snapshot.comparison.presentation.SnapshotComparisonViewModel.CompareAction
import com.absinthe.libchecker.domain.snapshot.comparison.presentation.SnapshotComparisonViewModel.Effect
import com.absinthe.libchecker.domain.snapshot.detail.ui.launchSnapshotDetail
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotTimeNodeListData
import com.absinthe.libchecker.domain.snapshot.list.ui.compose.SnapshotTimeNodeSheet
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotContributionData
import com.absinthe.libchecker.ui.compose.component.AdaptiveBackButton
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopBarIconButton
import com.absinthe.libchecker.ui.compose.component.LcEmptyState
import com.absinthe.libchecker.ui.compose.component.LcMiuixLoading
import com.absinthe.libchecker.ui.compose.component.LcTag
import com.absinthe.libchecker.ui.compose.component.MiuixPageHost
import com.absinthe.libchecker.ui.compose.component.adaptiveVerticalScroll
import com.absinthe.libchecker.ui.compose.theme.lcBlackFontFamily
import com.absinthe.libchecker.ui.compose.theme.lcCondensedFontFamily
import com.absinthe.libchecker.ui.compose.theme.withCustomFont
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import com.absinthe.libchecker.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.absinthe.libchecker.ui.compose.component.FastScrollLazyColumn as LazyColumn

/** Snapshot comparison screen built with Miuix Compose. */
@Composable
fun ComparisonScreen(
  viewModel: SnapshotComparisonViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState()
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val scope = rememberCoroutineScope()
  val colorScheme = MiuixTheme.colorScheme
  val scrollBehavior = MiuixScrollBehavior()
  val iconSizePx = with(LocalDensity.current) { 44.dp.roundToPx() }
  val itemPlacementSpec = remember {
    spring(
      dampingRatio = 0.86f,
      stiffness = Spring.StiffnessMediumLow,
      visibilityThreshold = IntOffset.VisibilityThreshold
    )
  }

  val timestampTitle = stringResource(R.string.snapshot_current_timestamp)
  val chooseTimestampText = stringResource(R.string.album_click_to_choose)
  val appsCountTitle = stringResource(R.string.comparison_snapshot_apps_count)
  val dashboardLabels = remember(timestampTitle, chooseTimestampText, appsCountTitle) {
    ComparisonDashboardLabels(
      timestampTitle = timestampTitle,
      chooseTimestampText = chooseTimestampText,
      appsCountTitle = appsCountTitle,
      defaultAppsCountText = "0"
    )
  }

  var dashboardState by remember(dashboardLabels) {
    mutableStateOf(viewModel.buildDashboardState(dashboardLabels))
  }
  var timeStamps by remember { mutableStateOf<List<TimeStampItem>>(emptyList()) }
  var timeNodeData by remember { mutableStateOf<SnapshotTimeNodeListData?>(null) }
  var contributionData by remember { mutableStateOf<SnapshotContributionData?>(null) }
  var diffItems by remember { mutableStateOf<List<SnapshotDiffItem>>(emptyList()) }
  var isComparing by remember { mutableStateOf(false) }
  var choosingSide by remember { mutableStateOf<SnapshotComparisonSide?>(null) }
  var pendingArchiveSide by remember { mutableStateOf<SnapshotComparisonSide?>(null) }

  val archiveLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    val side = pendingArchiveSide ?: choosingSide
    pendingArchiveSide = null
    if (uri != null && side != null) {
      viewModel.selectArchive(side, uri)
      dashboardState = viewModel.buildDashboardState(
        labels = dashboardLabels,
        previousState = dashboardState
      )
    }
  }

  LaunchedEffect(Unit) {
    dashboardState = viewModel.buildDashboardState(
      labels = dashboardLabels,
      previousState = dashboardState
    )
    launch {
      timeStamps = withContext(Dispatchers.IO) { viewModel.getTimeStamps() }
    }
    launch {
      viewModel.snapshotDiffItemsFlow.collect { items ->
        diffItems = items
        isComparing = false
      }
    }
    launch {
      viewModel.effect.collect { effect ->
        if (effect is Effect.DashboardCountChange) {
          val side = SnapshotComparisonSide.fromIsLeft(effect.isLeft)
          dashboardState = dashboardState.withAppsCountText(
            side = side,
            appsCountText = effect.snapshotCount.toString(),
            labels = dashboardLabels
          )
        }
      }
    }
  }

  LaunchedEffect(dashboardState.left.dashboardCountTimestamp) {
    dashboardState.left.dashboardCountTimestamp?.let { ts ->
      viewModel.getDashboardCount(ts, isLeft = true)
    }
  }

  LaunchedEffect(dashboardState.right.dashboardCountTimestamp) {
    dashboardState.right.dashboardCountTimestamp?.let { ts ->
      viewModel.getDashboardCount(ts, isLeft = false)
    }
  }

  LaunchedEffect(choosingSide) {
    val side = choosingSide ?: return@LaunchedEffect
    val latestStamps = withContext(Dispatchers.IO) { viewModel.getTimeStamps() }
    timeStamps = latestStamps
    val selectedTimestamp = dashboardState.getSide(side).dashboardCountTimestamp
    timeNodeData = runCatching {
      viewModel.buildSnapshotTimeNodeListData(latestStamps, selectedTimestamp)
    }.getOrNull()
    contributionData = runCatching {
      viewModel.computeSnapshotContributions(latestStamps)
    }.getOrNull()
  }

  fun compare() {
    if (!viewModel.canCompare()) {
      Toast.makeText(
        context,
        R.string.album_item_comparison_invalid_compare,
        Toast.LENGTH_SHORT
      ).show()
      return
    }
    isComparing = true
    scope.launch {
      try {
        val action = viewModel.buildCompareAction(
          cacheDir = context.externalCacheDir ?: context.requireAvailableCacheDirCompat(),
          iconSize = iconSizePx
        )
        if (action.hasNotEnoughStorageSpace) {
          context.showToast(R.string.toast_not_enough_storage_space)
        }
        when (action) {
          is CompareAction.Invalid -> {
            isComparing = false
            Toast.makeText(
              context,
              R.string.album_item_comparison_invalid_compare,
              Toast.LENGTH_SHORT
            ).show()
          }

          is CompareAction.Ready -> when (val plan = action.plan) {
            is SnapshotComparisonPlan.TimestampRange -> {
              viewModel.compareDiff(plan.previousTimestamp, plan.currentTimestamp)
            }

            is SnapshotComparisonPlan.SnapshotLists -> {
              viewModel.compareDiffWithSnapshotList(plan.lists.left, plan.lists.right)
              isComparing = false
            }

            is SnapshotComparisonPlan.ArchivePair -> {
              viewModel.compareDiffWithSnapshotList(
                listOf(plan.left.snapshotItem),
                listOf(plan.right.snapshotItem)
              )
              isComparing = false
            }
          }
        }
      } catch (_: Throwable) {
        isComparing = false
      }
    }
  }

  MiuixPageHost(modifier = modifier.fillMaxSize()) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        AdaptiveTopAppBar(
          title = stringResource(R.string.album_item_comparison_title),
          scrollBehavior = scrollBehavior,
          navigationIcon = {
            AdaptiveBackButton(onClick = onBack)
          },
          actions = {
            AdaptiveTopBarIconButton(onClick = ::compare) {
              Icon(
                painter = painterResource(R.drawable.ic_compare),
                contentDescription = stringResource(R.string.comparison_start),
                tint = colorScheme.onSurface
              )
            }
          }
        )
      }
    ) { contentPadding ->
      WideContentBox(
        modifier = Modifier
          .fillMaxSize()
          .then(topBarBackdropModifier)
          .horizontalCutoutPadding()
      ) {
        LazyColumn(
          state = listState,
          contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp
          ),
          modifier = Modifier
            .fillMaxSize()
            .adaptiveVerticalScroll(scrollBehavior)
        ) {
          item(key = "comparison_dashboard") {
            ComparisonDashboardCard(
              left = dashboardState.left,
              right = dashboardState.right,
              labels = dashboardLabels,
              onLeftClick = { choosingSide = SnapshotComparisonSide.LEFT },
              onRightClick = { choosingSide = SnapshotComparisonSide.RIGHT }
            )
          }

          if (isComparing) {
            item(key = "comparison_loading") {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .fillParentMaxHeight(0.65f),
                contentAlignment = Alignment.Center
              ) {
                LcMiuixLoading()
              }
            }
          } else if (diffItems.isEmpty()) {
            item(key = "comparison_empty") {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .fillParentMaxHeight(0.65f),
                contentAlignment = Alignment.Center
              ) {
                LcEmptyState(
                  drawableRes = R.drawable.ic_empty_list,
                  title = stringResource(R.string.snapshot_empty_list_title)
                )
              }
            }
          } else {
            items(diffItems, key = { it.packageName }) { item ->
              DiffRow(
                item = item,
                onClick = {
                  scope.launch {
                    context.launchSnapshotDetail(lifecycleOwner, item)
                  }
                },
                modifier = Modifier.animateItem(
                  fadeInSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                  placementSpec = itemPlacementSpec,
                  fadeOutSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                )
              )
            }
          }

          item(key = "comparison_footer") {
            Spacer(Modifier.navigationBarsPadding())
          }
        }
      }
    }
  }

  SnapshotTimeNodeSheet(
    show = choosingSide != null,
    title = stringResource(R.string.snapshot_time_node_title),
    items = timeNodeData?.items.orEmpty(),
    iconSources = timeNodeData?.packageIconSources.orEmpty(),
    contributionData = contributionData,
    currentTimestamp = choosingSide?.let { dashboardState.getSide(it).dashboardCountTimestamp },
    onSelect = { timestamp ->
      val side = choosingSide
      if (side != null) {
        viewModel.selectSnapshot(side, timestamp)
        dashboardState = viewModel.buildDashboardState(
          labels = dashboardLabels,
          previousState = dashboardState
        )
      }
      choosingSide = null
    },
    onDismiss = { choosingSide = null },
    topCard = {
      ChooseLocalArchiveCard(
        onClick = {
          pendingArchiveSide = choosingSide
          choosingSide = null
          archiveLauncher.launch(arrayOf("*/*"))
        }
      )
    }
  )
}

@Composable
private fun ComparisonDashboardCard(
  left: ComparisonDashboardSideState,
  right: ComparisonDashboardSideState,
  labels: ComparisonDashboardLabels,
  onLeftClick: () -> Unit,
  onRightClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .padding(bottom = 12.dp),
    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      ComparisonDashboardHalf(
        sideState = left,
        labels = labels,
        horizontalAlignment = Alignment.Start,
        textAlign = TextAlign.Start,
        onClick = onLeftClick,
        modifier = Modifier.weight(1f)
      )
      ComparisonDashboardHalf(
        sideState = right,
        labels = labels,
        horizontalAlignment = Alignment.End,
        textAlign = TextAlign.End,
        onClick = onRightClick,
        modifier = Modifier.weight(1f)
      )
    }
  }
}

@Composable
private fun ComparisonDashboardHalf(
  sideState: ComparisonDashboardSideState,
  labels: ComparisonDashboardLabels,
  horizontalAlignment: Alignment.Horizontal,
  textAlign: TextAlign,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colorScheme = MiuixTheme.colorScheme
  val blackFont = lcBlackFontFamily()
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 2.dp, vertical = 2.dp)
      .semantics(mergeDescendants = true) {
        contentDescription = sideState.contentDescription
      },
    horizontalAlignment = horizontalAlignment
  ) {
    Text(
      text = labels.timestampTitle,
      style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
      color = colorScheme.onSurfaceVariantSummary,
      textAlign = textAlign,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(2.dp))
    Text(
      text = sideState.timestampText,
      style = MiuixTheme.textStyles.body2.copy(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
      ).withCustomFont(blackFont),
      color = colorScheme.onSurface,
      textAlign = textAlign,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(6.dp))
    Text(
      text = labels.appsCountTitle,
      style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
      color = colorScheme.onSurfaceVariantSummary,
      textAlign = textAlign,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(2.dp))
    Text(
      text = sideState.appsCountText,
      style = MiuixTheme.textStyles.body2.copy(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
      ).withCustomFont(blackFont),
      color = colorScheme.onSurface,
      textAlign = textAlign,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
private fun ChooseLocalArchiveCard(onClick: () -> Unit) {
  val colorScheme = MiuixTheme.colorScheme
  Card(
    modifier = Modifier.fillMaxWidth(),
    onClick = onClick,
    showIndication = true,
    colors = CardDefaults.defaultColors(
      color = colorScheme.onSurface.copy(alpha = 0.04f)
    ),
    insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        painter = painterResource(R.drawable.ic_archive),
        contentDescription = null,
        tint = colorScheme.onSurface,
        modifier = Modifier.size(22.dp)
      )
      Spacer(Modifier.width(10.dp))
      Text(
        text = stringResource(R.string.album_item_comparison_choose_local_apk),
        style = MiuixTheme.textStyles.body2.copy(
          fontSize = 15.sp,
          fontWeight = FontWeight.Medium
        ).withCustomFont(lcCondensedFontFamily()),
        color = colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
      )
      Spacer(Modifier.width(8.dp))
      Icon(
        imageVector = MiuixIcons.Basic.ArrowRight,
        contentDescription = null,
        tint = colorScheme.onSurfaceVariantActions,
        modifier = Modifier.size(width = 10.dp, height = 16.dp)
      )
    }
  }
}

@Composable
private fun DiffRow(
  item: SnapshotDiffItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colorScheme = MiuixTheme.colorScheme
  val label = item.labelDiff.new ?: item.labelDiff.old
  val packageInfo by produceState<PackageInfo?>(initialValue = null, key1 = item.packageName) {
    value = withContext(Dispatchers.IO) {
      runCatching {
        PackageManagerCompat.getPackageInfo(item.packageName, 0)
      }.getOrNull()
    }
  }
  val isNewOrDeleted = item.newInstalled || item.deleted
  val cardBorderModifier = if (isNewOrDeleted) {
    val borderColorRes = if (item.newInstalled) {
      R.color.material_green_300
    } else {
      R.color.material_red_300
    }
    Modifier.border(1.5.dp, colorResource(borderColorRes), RoundedCornerShape(16.dp))
  } else {
    Modifier
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .padding(bottom = 12.dp)
      .then(cardBorderModifier),
    onClick = onClick,
    showIndication = true,
    insideMargin = PaddingValues(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min)
        .alpha(if (item.deleted) 0.7f else 1f),
      verticalAlignment = Alignment.CenterVertically
    ) {
      val info = packageInfo
      if (info != null) {
        AppIcon(
          packageInfo = info,
          label = label,
          modifier = Modifier
            .padding(end = 12.dp)
            .size(44.dp)
        )
      } else {
        Image(
          painter = painterResource(R.drawable.ic_icon_blueprint),
          contentDescription = null,
          modifier = Modifier
            .padding(end = 12.dp)
            .size(44.dp)
            .squircleClip(10.dp)
        )
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = label,
            style = MiuixTheme.textStyles.body1.copy(
              fontSize = 15.sp,
              fontWeight = FontWeight.Medium
            ),
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
          )
          if (item.newInstalled) {
            Spacer(Modifier.width(6.dp))
            LcTag(
              text = stringResource(R.string.snapshot_indicator_added),
              containerColor = colorResource(R.color.snapshot_status_added).copy(alpha = 0.16f),
              contentColor = colorResource(R.color.snapshot_status_added),
              horizontalPadding = 6,
              verticalPadding = 1,
              cornerRadius = 5,
              textStyle = MiuixTheme.textStyles.footnote2.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
              )
            )
          } else if (item.deleted) {
            Spacer(Modifier.width(6.dp))
            LcTag(
              text = stringResource(R.string.snapshot_indicator_removed),
              containerColor = colorResource(R.color.snapshot_status_removed).copy(alpha = 0.16f),
              contentColor = colorResource(R.color.snapshot_status_removed),
              horizontalPadding = 6,
              verticalPadding = 1,
              cornerRadius = 5,
              textStyle = MiuixTheme.textStyles.footnote2.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
              )
            )
          }
        }
        Text(
          text = item.packageName,
          style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
          color = colorScheme.onSurfaceVariantSummary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        val versionDiffText = buildString {
          append(item.versionNameDiff.old)
          item.versionNameDiff.new?.takeIf { it != item.versionNameDiff.old }?.let { newVer ->
            append(" → ")
            append(newVer)
          }
        }
        if (versionDiffText.isNotBlank()) {
          Text(
            text = versionDiffText,
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp)
              .withCustomFont(lcCondensedFontFamily()),
            color = colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      if (!isNewOrDeleted) {
        val segments = listOf(
          R.color.material_green_300 to item.added,
          R.color.material_red_300 to item.removed,
          R.color.material_yellow_300 to item.changed,
          R.color.material_blue_300 to item.moved
        ).filter { it.second > 0 }
        if (segments.isNotEmpty()) {
          Column(
            modifier = Modifier
              .padding(start = 10.dp, top = 2.dp, bottom = 2.dp)
              .width(5.dp)
              .fillMaxHeight()
              .clip(RoundedCornerShape(99.dp))
          ) {
            segments.forEach { (colorRes, count) ->
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(count.toFloat())
                  .background(colorResource(colorRes))
              )
            }
          }
        } else if (!item.isNothingChanged()) {
          Spacer(Modifier.width(8.dp))
          LcTag(
            text = stringResource(R.string.snapshot_indicator_changed),
            containerColor = colorResource(R.color.snapshot_status_changed).copy(alpha = 0.16f),
            contentColor = colorResource(R.color.snapshot_status_changed),
            horizontalPadding = 6,
            verticalPadding = 2,
            cornerRadius = 6,
            textStyle = MiuixTheme.textStyles.footnote2.copy(
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium
            )
          )
        }
      }
    }
  }
}

private fun android.content.Context.requireAvailableCacheDirCompat(): java.io.File {
  return cacheDir ?: java.io.File(filesDir, "cache").apply { mkdirs() }
}
