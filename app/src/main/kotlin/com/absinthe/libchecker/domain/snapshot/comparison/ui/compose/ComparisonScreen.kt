package com.absinthe.libchecker.domain.snapshot.comparison.ui.compose

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonSide
import com.absinthe.libchecker.domain.snapshot.comparison.presentation.SnapshotComparisonViewModel
import com.absinthe.libchecker.domain.snapshot.comparison.presentation.SnapshotComparisonViewModel.CompareAction
import com.absinthe.libchecker.domain.snapshot.comparison.presentation.SnapshotComparisonViewModel.Effect
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonPlan
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.app.list.ui.compose.AppIcon
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.LcEmptyState
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import com.absinthe.libchecker.ui.compose.util.sheetContentSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetHeightTransition
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import com.absinthe.libchecker.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/** Snapshot comparison page: pick both sides, compare and inspect the resulting diff list. */
@Composable
fun ComparisonScreen(
  viewModel: SnapshotComparisonViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState()
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val scrollBehavior = MiuixScrollBehavior()
  val colorScheme = MiuixTheme.colorScheme

  var timeStamps by remember { mutableStateOf<List<TimeStampItem>>(emptyList()) }
  var formattedDates by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
  var leftTimestamp by rememberSaveable { mutableLongStateOf(-1L) }
  var rightTimestamp by rememberSaveable { mutableLongStateOf(-1L) }
  var leftCount by remember { mutableStateOf<Int?>(null) }
  var rightCount by remember { mutableStateOf<Int?>(null) }
  var choosingSide by remember { mutableStateOf<SnapshotComparisonSide?>(null) }
  var items by remember { mutableStateOf<List<SnapshotDiffItem>>(emptyList()) }
  var loading by remember { mutableStateOf(false) }

  val archiveLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    val side = choosingSide
    if (uri != null && side != null) {
      viewModel.selectArchive(side, uri)
      choosingSide = null
    }
  }

  LaunchedEffect(Unit) {
    val stamps = withContext(Dispatchers.IO) { viewModel.getTimeStamps() }
    timeStamps = stamps
    formattedDates = stamps.associate { it.timestamp to viewModel.getFormatDateString(it.timestamp) }
    launch {
      viewModel.snapshotDiffItemsFlow.collect { items = it }
    }
    launch {
      viewModel.effect.collect { effect ->
        if (effect is Effect.DashboardCountChange) {
          if (effect.isLeft) leftCount = effect.snapshotCount else rightCount = effect.snapshotCount
        }
      }
    }
  }

  LaunchedEffect(leftTimestamp) {
    if (leftTimestamp >= 0) {
      viewModel.getDashboardCount(leftTimestamp, isLeft = true)
    }
  }
  LaunchedEffect(rightTimestamp) {
    if (rightTimestamp >= 0) {
      viewModel.getDashboardCount(rightTimestamp, isLeft = false)
    }
  }

  fun compare() {
    if (!viewModel.canCompare()) {
      context.showToast(R.string.album_item_comparison_invalid_compare)
      return
    }
    scope.launch {
      loading = true
      val action = withContext(Dispatchers.IO) {
        viewModel.buildCompareAction(
          cacheDir = context.requireAvailableCacheDirCompat(),
          iconSize = (48 * context.resources.displayMetrics.density).toInt()
        )
      }
      if (action.hasNotEnoughStorageSpace) {
        context.showToast(R.string.toast_not_enough_storage_space)
      }
      when (action) {
        is CompareAction.Invalid -> context.showToast(R.string.album_item_comparison_invalid_compare)

        is CompareAction.Ready -> when (val plan = action.plan) {
          is SnapshotComparisonPlan.TimestampRange ->
            viewModel.compareDiff(plan.previousTimestamp, plan.currentTimestamp)

          is SnapshotComparisonPlan.SnapshotLists ->
            viewModel.compareDiffWithSnapshotList(plan.lists.left, plan.lists.right)

          is SnapshotComparisonPlan.ArchivePair ->
            viewModel.compareDiffWithSnapshotList(
              listOf(plan.left.snapshotItem),
              listOf(plan.right.snapshotItem)
            )
        }
      }
      loading = false
    }
  }

  Column(modifier = modifier.fillMaxSize()) {
    AdaptiveTopAppBar(
      title = stringResource(R.string.album_item_comparison_title),
      scrollBehavior = scrollBehavior,
      navigationIcon = {
        IconButton(onClick = onBack) {
          Icon(MiuixIcons.Back, contentDescription = stringResource(R.string.action_back))
        }
      },
      actions = {
        IconButton(onClick = ::compare) {
          Icon(
            imageVector = MiuixIcons.Basic.Check,
            contentDescription = stringResource(R.string.comparison_start)
          )
        }
      }
    )
    WideContentBox { sidePadding ->
      LazyColumn(
        state = listState,
        modifier = Modifier
          .fillMaxSize()
          .horizontalCutoutPadding()
          .scrollEndHaptic()
          .overScrollVertical()
          .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(
          top = 6.dp,
          start = sidePadding,
          end = sidePadding
        )
      ) {
        item(key = "sides") {
          Column(Modifier.padding(horizontal = 12.dp)) {
            ComparisonSideCard(
              title = stringResource(R.string.comparison_side_left),
              timestamp = leftTimestamp,
              count = leftCount,
              formattedDates = formattedDates,
              onPickTime = { choosingSide = SnapshotComparisonSide.LEFT },
              onPickArchive = {
                choosingSide = SnapshotComparisonSide.LEFT
                archiveLauncher.launch(arrayOf("*/*"))
              }
            )
            Spacer(Modifier.height(12.dp))
            ComparisonSideCard(
              title = stringResource(R.string.comparison_side_right),
              timestamp = rightTimestamp,
              count = rightCount,
              formattedDates = formattedDates,
              onPickTime = { choosingSide = SnapshotComparisonSide.RIGHT },
              onPickArchive = {
                choosingSide = SnapshotComparisonSide.RIGHT
                archiveLauncher.launch(arrayOf("*/*"))
              }
            )
          }
        }
        item(key = "status") {
          if (loading) {
            Box(
              Modifier
                .fillMaxWidth()
                .height(200.dp)
            ) {
              LcLoadingState(message = stringResource(R.string.loading))
            }
          }
        }
        if (items.isEmpty()) {
          item(key = "empty") {
            Box(
              Modifier
                .fillMaxWidth()
                .height(220.dp)
            ) {
              LcEmptyState(title = stringResource(R.string.snapshot_empty_list_title))
            }
          }
        } else {
          items(items = items, key = { it.packageName }) { item ->
            DiffRow(item = item)
          }
        }
        item {
          Spacer(
            Modifier
              .height(24.dp)
              .navigationBarsPadding()
          )
        }
      }
    }
  }

  if (choosingSide != null) {
    WindowBottomSheet(
      show = true,
      title = stringResource(R.string.dialog_title_select_to_delete),
      onDismissRequest = { choosingSide = null }
    ) {
      Column(
        Modifier
          .fillMaxWidth()
          .sheetContentSafePadding()
          .sheetHeightTransition()
      ) {
        timeStamps.forEach { stamp ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp)
              .padding(bottom = 8.dp),
            onClick = {
              val side = choosingSide ?: return@Card
              if (side == SnapshotComparisonSide.LEFT) {
                leftTimestamp = stamp.timestamp
              } else {
                rightTimestamp = stamp.timestamp
              }
              viewModel.selectSnapshot(side, stamp.timestamp)
              choosingSide = null
            },
            showIndication = true,
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
          ) {
            Text(
              text = formattedDates[stamp.timestamp] ?: stamp.timestamp.toString(),
              fontWeight = FontWeight(500),
              color = MiuixTheme.colorScheme.onSurface
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ComparisonSideCard(
  title: String,
  timestamp: Long,
  count: Int?,
  formattedDates: Map<Long, String>,
  onPickTime: () -> Unit,
  onPickArchive: () -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  Card(
    modifier = Modifier.fillMaxWidth(),
    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = title,
        modifier = Modifier.weight(1f),
        fontWeight = FontWeight(600),
        color = colorScheme.onSurface
      )
      Text(
        text = count?.toString() ?: "--",
        fontSize = 12.sp,
        color = colorScheme.onSurfaceVariantSummary
      )
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Box(
        modifier = Modifier
          .squircleBackground(colorScheme.surfaceContainerHigh, 10.dp)
          .clickable(onClick = onPickTime)
          .padding(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Text(
          text = formattedDates[timestamp] ?: stringResource(R.string.album_click_to_choose),
          fontSize = 12.sp,
          color = colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Box(
        modifier = Modifier
          .squircleBackground(colorScheme.surfaceContainerHigh, 10.dp)
          .clickable(onClick = onPickArchive)
          .padding(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Text(
          text = stringResource(R.string.comparison_side_archive),
          fontSize = 12.sp,
          color = colorScheme.onSurface,
          maxLines = 1
        )
      }
    }
  }
}

@Composable
private fun DiffRow(item: SnapshotDiffItem) {
  val colorScheme = MiuixTheme.colorScheme
  val label = item.labelDiff.new ?: item.labelDiff.old
  val stateText = when {
    item.newInstalled -> stringResource(R.string.snapshot_indicator_added)
    item.deleted -> stringResource(R.string.snapshot_indicator_removed)
    item.changed > 0 || item.added > 0 || item.removed > 0 || item.moved > 0 ->
      stringResource(R.string.snapshot_indicator_changed)
    else -> stringResource(R.string.snapshot_indicator_changed)
  }
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .padding(bottom = 8.dp),
    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      val packageInfo = remember(item.packageName) {
        runCatching {
          com.absinthe.libchecker.compat.PackageManagerCompat.getPackageInfo(item.packageName, 0)
        }.getOrNull()
      }
      AppIcon(
        packageInfo = packageInfo,
        label = label,
        modifier = Modifier
          .padding(end = 12.dp)
          .size(40.dp)
      )
      Column(Modifier.weight(1f)) {
        Text(
          text = label,
          fontWeight = FontWeight(500),
          color = colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = item.packageName,
          fontSize = 12.sp,
          color = colorScheme.onSurfaceVariantSummary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Box(
        modifier = Modifier
          .squircleBackground(colorScheme.tertiaryContainer, 8.dp)
          .padding(horizontal = 8.dp, vertical = 3.dp)
      ) {
        Text(
          text = stateText,
          fontSize = 11.sp,
          fontWeight = FontWeight(550),
          color = colorScheme.onTertiaryContainer
        )
      }
    }
  }
}

private fun android.content.Context.requireAvailableCacheDirCompat(): java.io.File {
  return cacheDir ?: java.io.File(filesDir, "cache").apply { mkdirs() }
}