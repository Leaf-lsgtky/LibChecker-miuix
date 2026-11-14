package com.absinthe.libchecker.domain.snapshot.album.ui.compose

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Backup
import top.yukonga.miuix.kmp.icon.extended.ConvertFile
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog

/** Snapshot album: entry points for comparison, management, backup and tracking. */
@Composable
fun AlbumScreen(
  viewModel: SnapshotViewModel,
  onBack: () -> Unit,
  onOpenComparison: () -> Unit,
  onOpenTrack: () -> Unit,
  onOpenBackup: () -> Unit,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState()
) {
  val scrollBehavior = MiuixScrollBehavior()
  val scope = rememberCoroutineScope()
  var showManagement by remember { mutableStateOf(false) }
  var timeStamps by remember { mutableStateOf<List<TimeStampItem>>(emptyList()) }
  var formattedDates by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
  var pendingDelete by remember { mutableStateOf<TimeStampItem?>(null) }
  var loadingStamps by remember { mutableStateOf(false) }

  fun refreshTimeStamps() {
    scope.launch {
      loadingStamps = true
      val stamps = withContext(Dispatchers.IO) { viewModel.getTimeStamps() }
      timeStamps = stamps
      formattedDates = stamps.associate { it.timestamp to viewModel.getFormatDateString(it.timestamp) }
      loadingStamps = false
    }
  }

  LaunchedEffect(showManagement) {
    if (showManagement) {
      refreshTimeStamps()
    }
  }

  val items = listOf(
    AlbumEntry(
      iconRes = R.drawable.ic_compare,
      tint = Color(0xFFE53935),
      title = stringResource(R.string.album_item_comparison_title),
      subtitle = stringResource(R.string.album_item_comparison_subtitle),
      onClick = onOpenComparison
    ),
    AlbumEntry(
      iconRes = R.drawable.ic_manage,
      tint = Color(0xFF1E88E5),
      title = stringResource(R.string.album_item_management_title),
      subtitle = stringResource(R.string.album_item_management_subtitle),
      onClick = { showManagement = true }
    ),
    AlbumEntry(
      iconRes = R.drawable.ic_backup,
      tint = Color(0xFF43A047),
      title = stringResource(R.string.album_item_backup_restore_title),
      subtitle = stringResource(R.string.album_item_backup_restore_subtitle),
      onClick = onOpenBackup
    ),
    AlbumEntry(
      iconRes = R.drawable.ic_track,
      tint = Color(0xFFFB8C00),
      title = stringResource(R.string.album_item_track_title),
      subtitle = stringResource(R.string.album_item_track_subtitle),
      onClick = onOpenTrack
    )
  )

  Column(modifier = modifier.fillMaxSize()) {
    AdaptiveTopAppBar(
      title = stringResource(R.string.title_album),
      scrollBehavior = scrollBehavior,
      navigationIcon = {
        IconButton(onClick = onBack) {
          Icon(MiuixIcons.Back, contentDescription = stringResource(R.string.action_back))
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
        items(items = items, key = { it.title }) { entry ->
          AlbumRow(entry = entry)
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

  WindowBottomSheet(
    show = showManagement,
    title = stringResource(R.string.dialog_title_select_to_delete),
    onDismissRequest = { showManagement = false }
  ) {
    Column(Modifier.fillMaxWidth()) {
      when {
        loadingStamps -> LcLoadingState(
          modifier = Modifier.height(180.dp),
          message = stringResource(R.string.loading)
        )

        timeStamps.isEmpty() -> Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = stringResource(R.string.snapshot_no_snapshot),
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
          )
        }

        else -> LazyColumn {
          items(items = timeStamps, key = { it.timestamp }) { stamp ->
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp),
              onClick = { pendingDelete = stamp },
              showIndication = true,
              insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = formattedDates[stamp.timestamp] ?: stamp.timestamp.toString(),
                  modifier = Modifier.weight(1f),
                  fontWeight = FontWeight(500),
                  color = MiuixTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Icon(
                  imageVector = MiuixIcons.Delete,
                  contentDescription = null,
                  tint = MiuixTheme.colorScheme.error,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }
      }
    }
  }

  pendingDelete?.let { stamp ->
    WindowDialog(
      show = true,
      title = stringResource(R.string.dialog_title_confirm_to_delete),
      summary = stringResource(
        R.string.dialog_message_confirm_to_delete,
        formattedDates[stamp.timestamp].orEmpty()
      ),
      onDismissRequest = { pendingDelete = null }
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        TextButton(
          text = stringResource(android.R.string.cancel),
          onClick = { pendingDelete = null },
          modifier = Modifier.weight(1f)
        )
        TextButton(
          text = stringResource(R.string.dialog_action_delete),
          onClick = {
            val timestamp = stamp.timestamp
            pendingDelete = null
            scope.launch {
              withContext(Dispatchers.IO) { viewModel.deleteSnapshotTimeStamp(timestamp) }
              refreshTimeStamps()
            }
          },
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.textButtonColorsPrimary()
        )
      }
    }
  }
}

private data class AlbumEntry(
  @androidx.annotation.DrawableRes val iconRes: Int,
  val tint: Color,
  val title: String,
  val subtitle: String,
  val onClick: () -> Unit
)

@Composable
private fun AlbumRow(entry: AlbumEntry) {
  val colorScheme = MiuixTheme.colorScheme
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .padding(bottom = 12.dp),
    onClick = entry.onClick,
    showIndication = true,
    insideMargin = PaddingValues(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .padding(end = 14.dp)
          .size(44.dp)
          .squircleBackground(entry.tint.copy(alpha = 0.15f), 12.dp),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          painter = painterResource(entry.iconRes),
          contentDescription = null,
          tint = entry.tint,
          modifier = Modifier.size(24.dp)
        )
      }
      Column(Modifier.weight(1f)) {
        Text(
          text = entry.title,
          fontWeight = FontWeight(550),
          color = colorScheme.onSurface,
          maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = entry.subtitle,
          fontSize = 12.sp,
          fontWeight = FontWeight(500),
          color = colorScheme.onSurfaceVariantSummary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}
