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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotTimeNodeListData
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.list.ui.compose.SnapshotAutoRemoveCard
import com.absinthe.libchecker.domain.snapshot.list.ui.compose.SnapshotTimeNodeSheet
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotContributionData
import com.absinthe.libchecker.ui.compose.component.AdaptiveBackButton
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.MiuixPageHost
import com.absinthe.libchecker.ui.compose.component.adaptiveVerticalScroll
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import com.absinthe.libchecker.ui.compose.util.rememberActiveValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.Delete
import top.yukonga.miuix.kmp.icon.glass.HorizontalSplit
import top.yukonga.miuix.kmp.icon.glass.Layers
import top.yukonga.miuix.kmp.icon.glass.UploadCloud
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import com.absinthe.libchecker.ui.compose.component.FastScrollLazyColumn as LazyColumn

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
  var timeNodeData by remember { mutableStateOf<SnapshotTimeNodeListData?>(null) }
  var contributionData by remember { mutableStateOf<SnapshotContributionData?>(null) }
  var autoRemoveThreshold by remember {
    mutableIntStateOf(viewModel.getSnapshotAutoRemoveThreshold())
  }
  var pendingDeleteTimestamp by remember { mutableStateOf<Long?>(null) }
  var loadingStamps by remember { mutableStateOf(false) }

  suspend fun reloadManagementData() {
    loadingStamps = true
    val stamps = withContext(Dispatchers.IO) { viewModel.getTimeStamps() }
    timeNodeData = withContext(Dispatchers.IO) {
      runCatching { viewModel.buildSnapshotTimeNodeListData(stamps) }.getOrNull()
    }
    contributionData = withContext(Dispatchers.IO) {
      runCatching { viewModel.computeSnapshotContributions(stamps) }.getOrNull()
    }
    autoRemoveThreshold = viewModel.getSnapshotAutoRemoveThreshold()
    loadingStamps = false
  }

  LaunchedEffect(showManagement) {
    if (showManagement) {
      reloadManagementData()
    }
  }

  val items = listOf(
    AlbumEntry(
      icon = MiuixIcons.Glass.HorizontalSplit,
      tint = Color(0xFFE53935),
      title = stringResource(R.string.album_item_comparison_title),
      subtitle = stringResource(R.string.album_item_comparison_subtitle),
      onClick = onOpenComparison
    ),
    AlbumEntry(
      icon = MiuixIcons.Glass.Layers,
      tint = Color(0xFF1E88E5),
      title = stringResource(R.string.album_item_management_title),
      subtitle = stringResource(R.string.album_item_management_subtitle),
      onClick = { showManagement = true }
    ),
    AlbumEntry(
      icon = MiuixIcons.Glass.UploadCloud,
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

  MiuixPageHost(modifier = modifier.fillMaxSize()) {
    val pageState = this
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        AdaptiveTopAppBar(
          title = stringResource(R.string.title_album),
          scrollBehavior = scrollBehavior,
          isContentScrolled = listState.canScrollBackward,
          navigationIcon = {
            AdaptiveBackButton(onClick = onBack)
          }
        )
      }
    ) { padding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .then(pageState.topBarBackdropModifier)
      ) {
        WideContentBox { sidePadding ->
          LazyColumn(
            state = listState,
            modifier = Modifier
              .fillMaxSize()
              .horizontalCutoutPadding()
              .adaptiveVerticalScroll(scrollBehavior),
            contentPadding = PaddingValues(
              top = padding.calculateTopPadding() + 6.dp,
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
    }
  }

  SnapshotTimeNodeSheet(
    show = showManagement,
    title = stringResource(R.string.dialog_title_select_to_delete),
    items = timeNodeData?.items.orEmpty(),
    iconSources = timeNodeData?.packageIconSources.orEmpty(),
    contributionData = contributionData,
    currentTimestamp = null,
    onSelect = { timestamp -> pendingDeleteTimestamp = timestamp },
    onDismiss = { showManagement = false },
    isLoading = loadingStamps,
    showSelectedState = false,
    showDaySnapshotAction = false,
    trailingIcon = MiuixIcons.Glass.Delete,
    trailingIconTint = MiuixTheme.colorScheme.error,
    topCard = {
      SnapshotAutoRemoveCard(
        autoRemoveThreshold = autoRemoveThreshold,
        onDisable = {
          viewModel.disableSnapshotAutoRemoveThreshold()
          autoRemoveThreshold = 0
        },
        onConfirmThreshold = { threshold ->
          autoRemoveThreshold = threshold
          scope.launch {
            withContext(Dispatchers.IO) {
              runCatching { viewModel.enableSnapshotAutoRemoveAndRetainLatest(threshold) }
            }
            reloadManagementData()
          }
        }
      )
    }
  )

  val activePendingDelete = rememberActiveValue(pendingDeleteTimestamp)
  WindowDialog(
    show = pendingDeleteTimestamp != null,
    title = stringResource(R.string.dialog_title_confirm_to_delete),
    summary = activePendingDelete.value?.let { ts ->
      val formatted = timeNodeData?.items?.find { it.timestamp == ts }?.timestampText
        ?: viewModel.getFormatDateString(ts)
      stringResource(R.string.dialog_message_confirm_to_delete, formatted)
    } ?: "",
    onDismissRequest = { pendingDeleteTimestamp = null },
    onDismissFinished = { if (pendingDeleteTimestamp == null) activePendingDelete.value = null }
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      TextButton(
        text = stringResource(android.R.string.cancel),
        onClick = { pendingDeleteTimestamp = null },
        modifier = Modifier.weight(1f)
      )
      TextButton(
        text = stringResource(R.string.dialog_action_delete),
        onClick = {
          val timestamp = activePendingDelete.value ?: return@TextButton
          pendingDeleteTimestamp = null
          scope.launch {
            withContext(Dispatchers.IO) { viewModel.deleteSnapshotTimeStamp(timestamp) }
            reloadManagementData()
          }
        },
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.textButtonColorsPrimary()
      )
    }
  }
}

private data class AlbumEntry(
  @androidx.annotation.DrawableRes val iconRes: Int? = null,
  val icon: ImageVector? = null,
  val tint: Color,
  val title: String,
  val subtitle: String,
  val onClick: () -> Unit
)

@Composable
private fun AlbumRow(entry: AlbumEntry) {
  val colorScheme = MiuixTheme.colorScheme
  val isDark = colorScheme.surface.luminance() < 0.5f
  val containerColor = entry.tint.copy(alpha = if (isDark) 0.24f else 0.15f)
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
          .padding(end = 16.dp)
          .size(44.dp)
          .squircleSurface(containerColor, 12.dp),
        contentAlignment = Alignment.Center
      ) {
        if (entry.icon != null) {
          Icon(
            imageVector = entry.icon,
            contentDescription = null,
            tint = entry.tint,
            modifier = Modifier.size(24.dp)
          )
        } else if (entry.iconRes != null) {
          Icon(
            painter = painterResource(entry.iconRes),
            contentDescription = null,
            tint = entry.tint,
            modifier = Modifier.size(24.dp)
          )
        }
      }
      Column(Modifier.weight(1f)) {
        Text(
          text = entry.title,
          fontWeight = FontWeight(500),
          color = colorScheme.onSurface,
          maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = entry.subtitle,
          style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
          color = colorScheme.onSurfaceVariantSummary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}
