package com.absinthe.libchecker.domain.snapshot.list.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotTimeNodeItem
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * Time node sheet: switch the snapshot the list is showing and manage the automatic removal rule.
 *
 * The contribution wall of the original sheet is not ported; the list still exposes every node with
 * its timestamp text and app count so switching stays possible.
 */
@Composable
fun TimeNodeSheet(
  show: Boolean,
  viewModel: SnapshotViewModel,
  timeStamps: List<TimeStampItem>,
  currentTimestamp: Long?,
  onSelect: (Long) -> Unit,
  onDismiss: () -> Unit
) {
  val scope = androidx.compose.runtime.rememberCoroutineScope()
  var items by remember(timeStamps, currentTimestamp) {
    mutableStateOf<List<SnapshotTimeNodeItem>>(emptyList())
  }
  var contributionData by remember(show) {
    mutableStateOf<com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotContributionData?>(null)
  }
  var dayDetail by remember(show) {
    mutableStateOf<com.absinthe.libchecker.domain.snapshot.timenode.model.DayContribution?>(null)
  }
  var autoRemoveThreshold by remember(show) {
    mutableIntStateOf(viewModel.getSnapshotAutoRemoveThreshold())
  }

  LaunchedEffect(show, timeStamps, currentTimestamp) {
    if (!show) return@LaunchedEffect
    items = runCatching {
      viewModel.buildSnapshotTimeNodeListData(timeStamps).items
    }.getOrDefault(emptyList())
    autoRemoveThreshold = viewModel.getSnapshotAutoRemoveThreshold()
    contributionData = runCatching {
      viewModel.computeSnapshotContributions(timeStamps)
    }.getOrNull()
  }

  WindowBottomSheet(
    show = show,
    title = stringResource(R.string.snapshot_time_node_title),
    onDismissRequest = onDismiss
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.snapshot_auto_remove_title),
            fontSize = 14.sp,
            fontWeight = FontWeight(550),
            color = MiuixTheme.colorScheme.onSurface
          )
          Text(
            text = stringResource(
              R.string.snapshot_auto_remove_summary,
              autoRemoveThreshold.coerceAtLeast(1)
            ),
            fontSize = 11.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
          )
        }
        Switch(
          checked = autoRemoveThreshold > 0,
          onCheckedChange = { enabled ->
            if (enabled) {
              val threshold = autoRemoveThreshold.takeIf { it > 0 } ?: DEFAULT_THRESHOLD
              scope.launch {
                runCatching { viewModel.enableSnapshotAutoRemoveAndRetainLatest(threshold) }
              }
              autoRemoveThreshold = threshold
            } else {
              viewModel.disableSnapshotAutoRemoveThreshold()
              autoRemoveThreshold = 0
            }
          }
        )
      }

      contributionData?.let { data ->
        ContributionGrid(
          data = data,
          onTileClick = { day -> dayDetail = day }
        )
        Spacer(Modifier.height(8.dp))
      }
      if (items.isEmpty()) {
        Text(
          text = stringResource(R.string.empty_list),
          fontSize = 13.sp,
          color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
      } else {
        LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
          items(items, key = { it.timestamp }) { node ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .squircleBackground(
                  if (node.isCurrent) {
                    MiuixTheme.colorScheme.surfaceContainerHigh
                  } else {
                    MiuixTheme.colorScheme.surface
                  },
                  12.dp
                )
                .clickable { onSelect(node.timestamp) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(Modifier.weight(1f)) {
                Text(
                  text = node.timestampText,
                  fontSize = 14.sp,
                  fontWeight = FontWeight(550),
                  color = MiuixTheme.colorScheme.onSurface
                )
                if (node.description.isNotBlank()) {
                  Spacer(Modifier.height(2.dp))
                  Text(
                    text = node.description,
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                  )
                }
              }
              if (node.appCount > 0) {
                Text(
                  text = node.appCount.toString(),
                  fontSize = 12.sp,
                  color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
              }
              if (node.isCurrent) {
                Spacer(Modifier.height(0.dp))
                Text(
                  text = stringResource(R.string.snapshot_time_node_current),
                  modifier = Modifier.padding(start = 8.dp),
                  fontSize = 11.sp,
                  color = MiuixTheme.colorScheme.primary
                )
              }
            }
          }
        }
      }
    }
  }

  dayDetail?.let { day ->
    WindowDialog(
      show = true,
      title = day.date.toString(),
      summary = if (day.updatedApps.isEmpty()) {
        stringResource(R.string.snapshot_contribution_no_updates)
      } else {
        day.updatedApps.joinToString("\n") { it.label }
      },
      onDismissRequest = { dayDetail = null }
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
      ) {
        day.snapshotTimestamp?.let { timestamp ->
          TextButton(
            text = stringResource(R.string.snapshot_contribution_view_snapshot),
            onClick = {
              dayDetail = null
              onSelect(timestamp)
            }
          )
        }
        TextButton(
          text = stringResource(android.R.string.ok),
          onClick = { dayDetail = null }
        )
      }
    }
  }
}

private const val DEFAULT_THRESHOLD = 3

@Composable
private fun ContributionGrid(
  data: com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotContributionData,
  onTileClick: (com.absinthe.libchecker.domain.snapshot.timenode.model.DayContribution) -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  val weeks = remember(data) {
    val firstMonday = data.startDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val days = generateSequence(firstMonday) { it.plusDays(1) }
      .takeWhile { !it.isAfter(data.endDate.plusDays(1)) }
      .toList()
    days.chunked(7)
  }

  Column(modifier = Modifier.padding(vertical = 4.dp)) {
    Text(
      text = stringResource(R.string.snapshot_contribution_total, data.totalUpdates),
      fontSize = 11.sp,
      color = colorScheme.onSurfaceVariantSummary
    )
    Spacer(Modifier.height(2.dp))
    Text(
      text = stringResource(R.string.snapshot_contribution_hint),
      fontSize = 10.sp,
      color = colorScheme.onSurfaceVariantSummary
    )
    Spacer(Modifier.height(6.dp))
    Row(
      modifier = Modifier.horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        weeks.forEach { week ->
          Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            week.forEach { date ->
              val day = data.days[date]
              val color = when {
                day == null -> colorScheme.surfaceContainerHigh
                day.snapshotColor != null -> Color(day.snapshotColor)
                day.updateCount > 0 -> Color(
                  com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotPalette
                    .getColor(day.date.dayOfYear)
                ).copy(alpha = 0.45f)

                else -> colorScheme.surfaceContainerHigh
              }
              Box(
                modifier = Modifier
                  .size(11.dp)
                  .squircleBackground(color, 3.dp)
                  .clickable(enabled = day != null) {
                    day?.let(onTileClick)
                  }
              )
            }
          }
        }
      }
    }
  }
