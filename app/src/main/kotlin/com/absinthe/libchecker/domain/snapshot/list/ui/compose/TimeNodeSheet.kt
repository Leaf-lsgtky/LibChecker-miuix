package com.absinthe.libchecker.domain.snapshot.list.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
  var autoRemoveThreshold by remember(show) {
    mutableIntStateOf(viewModel.getSnapshotAutoRemoveThreshold())
  }

  LaunchedEffect(show, timeStamps, currentTimestamp) {
    if (!show) return@LaunchedEffect
    items = runCatching {
      viewModel.buildSnapshotTimeNodeListData(timeStamps).items
    }.getOrDefault(emptyList())
    autoRemoveThreshold = viewModel.getSnapshotAutoRemoveThreshold()
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
}

private const val DEFAULT_THRESHOLD = 3
