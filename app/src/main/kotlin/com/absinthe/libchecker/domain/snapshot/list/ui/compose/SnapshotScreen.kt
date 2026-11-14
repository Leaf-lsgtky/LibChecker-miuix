package com.absinthe.libchecker.domain.snapshot.list.ui.compose

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.detail.ui.launchSnapshotDetail
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.shouldShowSnapshotNoDiffPopup
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.list.ui.SnapshotShootServiceController
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.services.OnShootListener
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.LcEmptyState
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.MoreCircle
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * Snapshot destination: pick a time node, review the diff of that snapshot and capture a new one.
 *
 * Comparison, filtering and capture still run in [SnapshotViewModel] and the shoot service.
 */
@Composable
fun SnapshotScreen(
  viewModel: SnapshotViewModel,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState()
) {
  val context = LocalContext.current
  val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
    contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
  ) { }
  LaunchedEffect(Unit) {
    if (
      android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
      androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.POST_NOTIFICATIONS
      ) != android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
      notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
  }
  val lifecycleOwner = LocalLifecycleOwner.current
  val scope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val scrollBehavior = MiuixScrollBehavior()

  var timeStamps by remember { mutableStateOf<List<TimeStampItem>>(emptyList()) }
  var selectedTimestamp by remember { mutableLongStateOf(0L) }
  var items by remember { mutableStateOf<List<SnapshotDiffItem>>(emptyList()) }
  var iconSources by remember { mutableStateOf<Map<String, SnapshotPackageIconSource>>(emptyMap()) }
  var highlight by remember { mutableStateOf("") }
  var loading by remember { mutableStateOf(true) }
  var query by rememberSaveable { mutableStateOf("") }
  var searchExpanded by rememberSaveable { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  var showOptionsMenu by remember { mutableStateOf(false) }
  var showTimeNode by remember { mutableStateOf(false) }
  var systemProps by remember {
    mutableStateOf<List<com.absinthe.libchecker.domain.snapshot.list.model.SnapshotSystemPropDisplayData>>(emptyList())
  }
  var noDiffItem by remember {
    mutableStateOf<com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem?>(null)
  }
  var showKeepPreviousDialog by remember { mutableStateOf(false) }
  var isCapturing by remember { mutableStateOf(false) }
  var isRefreshing by remember { mutableStateOf(false) }
  val pullToRefreshState = rememberPullToRefreshState()
  val comparingProgress by viewModel.comparingProgress.collectAsStateWithLifecycle(0)

  suspend fun reloadTimeStamps(selectLatest: Boolean) {
    val stamps = viewModel.getTimeStamps()
    timeStamps = stamps
    if (stamps.isEmpty()) {
      loading = false
      return
    }
    val target = if (selectLatest || selectedTimestamp == 0L) {
      stamps.maxOf { it.timestamp }
    } else {
      selectedTimestamp
    }
    selectedTimestamp = target
    viewModel.refreshSnapshotTimestamp(target, shouldClearDiff = true)
  }

  val shootController = remember {
    SnapshotShootServiceController(
      object : OnShootListener.Stub() {
        override fun onShootFinished(timestamp: Long) {
          isCapturing = false
          scope.launch { reloadTimeStamps(selectLatest = true) }
        }

        override fun onProgressUpdated(progress: Int) = Unit

        override fun onShootFailed() {
          isCapturing = false
        }
      }
    )
  }

  DisposableEffect(Unit) {
    shootController.startAndBind(context)
    onDispose { shootController.release(context) }
  }

  LaunchedEffect(Unit) {
    reloadTimeStamps(selectLatest = true)
  }

  LaunchedEffect(Unit) {
    viewModel.snapshotDiffItemsUpdates.collect {
      val plan = viewModel.buildSnapshotListUpdatePlan(
        currentItems = items,
        highlightRefresh = false
      )
      items = plan.items
      iconSources = plan.renderState.packageIconSources
      highlight = plan.renderState.highlightText
      loading = false
    }
  }

  LaunchedEffect(query) {
    viewModel.updateSnapshotSearchKeyword(query)
    val plan = viewModel.buildSnapshotListUpdatePlan(
      currentItems = items,
      highlightRefresh = true
    )
    items = plan.items
    highlight = query
  }

  val formattedDates = remember(timeStamps) {
    timeStamps.associate { it.timestamp to viewModel.getFormatDateString(it.timestamp) }
  }

  LaunchedEffect(selectedTimestamp) {
    systemProps = if (selectedTimestamp == 0L) {
      emptyList()
    } else {
      runCatching<List<com.absinthe.libchecker.domain.snapshot.list.model.SnapshotSystemPropDisplayData>> {
        viewModel.getSystemPropDisplayData(selectedTimestamp)
      }.getOrDefault(emptyList())
    }
  }

  TimeNodeSheet(
    show = showTimeNode,
    viewModel = viewModel,
    timeStamps = timeStamps,
    currentTimestamp = selectedTimestamp.takeIf { it != 0L },
    onSelect = { timestamp ->
      showTimeNode = false
      selectedTimestamp = timestamp
      scope.launch {
        val plan = viewModel.buildSnapshotListUpdatePlan(
          currentItems = items,
          highlightRefresh = true
        )
        items = plan.items
        iconSources = plan.renderState.packageIconSources
        highlight = plan.renderState.highlightText
      }
    },
    onDismiss = { showTimeNode = false }
  )

  if (showOptionsMenu) {
    SnapshotMenuSheet(
      show = true,
      viewModel = viewModel,
      onDismiss = {
        showOptionsMenu = false
        scope.launch {
          val plan = viewModel.buildSnapshotListUpdatePlan(
            currentItems = items,
            highlightRefresh = true
          )
          items = plan.items
          iconSources = plan.renderState.packageIconSources
          highlight = plan.renderState.highlightText
        }
      }
    )
  }

  if (showKeepPreviousDialog) {
    WindowDialog(
      show = true,
      title = stringResource(R.string.dialog_title_keep_previous_snapshot),
      summary = stringResource(R.string.dialog_message_keep_previous_snapshot),
      onDismissRequest = { showKeepPreviousDialog = false }
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
      ) {
        TextButton(
          text = stringResource(R.string.btn_drop),
          onClick = {
            showKeepPreviousDialog = false
            isCapturing = shootController.computeSnapshot(dropPrevious = true)
          }
        )
        TextButton(
          text = stringResource(R.string.btn_keep),
          onClick = {
            showKeepPreviousDialog = false
            isCapturing = shootController.computeSnapshot(dropPrevious = false)
          }
        )
      }
    }
  }
  val tabs = remember(timeStamps, formattedDates) {
    timeStamps.map { stamp ->
      formattedDates[stamp.timestamp]?.substringAfter(", ", missingDelimiterValue = "")
        ?: stamp.timestamp.toString()
    }
  }

  Column(modifier = modifier.fillMaxSize()) {
    AdaptiveTopAppBar(
      title = stringResource(R.string.title_snapshot),
      scrollBehavior = scrollBehavior,
      actions = {
        IconButton(
          onClick = {
            context.startActivity(android.content.Intent(context, com.absinthe.libchecker.domain.snapshot.album.ui.AlbumActivity::class.java))
          }
        ) {
          Icon(
            imageVector = MiuixIcons.Album,
            contentDescription = stringResource(R.string.title_album)
          )
        }
        IconButton(
          onClick = {
            if (!isCapturing) {
              if (timeStamps.isNotEmpty()) {
                showKeepPreviousDialog = true
              } else {
                isCapturing = shootController.computeSnapshot(dropPrevious = false)
              }
            }
          }
        ) {
          Icon(
            painter = painterResource(R.drawable.ic_outline_save),
            contentDescription = stringResource(R.string.channel_shoot),
            tint = if (isCapturing) {
              MiuixTheme.colorScheme.disabledOnSurface
            } else {
              MiuixTheme.colorScheme.onSurface
            }
          )
        }
        if (selectedTimestamp != 0L) {
          OverlayIconDropdownMenu(
            entries = listOf(
              DropdownEntry(
                items = listOf(
                  DropdownItem(
                    text = stringResource(R.string.snapshot_time_node_title),
                    onClick = { showTimeNode = true }
                  ),
                  DropdownItem(
                    text = stringResource(R.string.advanced_menu),
                    onClick = { showOptionsMenu = true }
                  ),
                  DropdownItem(
                    text = stringResource(R.string.dialog_action_delete),
                    onClick = { showDeleteDialog = true }
                  )
                )
              )
            )
          ) {
            Icon(
              imageVector = MiuixIcons.MoreCircle,
              contentDescription = null
            )
          }
        }
      },
      bottomContent = {
        Column {
          InputField(
            query = query,
            onQueryChange = { query = it },
            onSearch = { focusManager.clearFocus() },
            expanded = searchExpanded,
            onExpandedChange = { searchExpanded = it },
            label = stringResource(R.string.search_hint),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp)
              .padding(bottom = 8.dp)
          )
          if (comparingProgress in 1..99) {
            LinearProgressIndicator(
              modifier = Modifier.fillMaxWidth(),
              progress = comparingProgress / 100f,
              height = 2.dp
            )
          }
        }
      }
    )

    if (tabs.size > 1) {
      TabRow(
        tabs = tabs,
        selectedTabIndex = timeStamps.indexOfFirst { it.timestamp == selectedTimestamp }
          .coerceAtLeast(0),
        onTabSelected = { index ->
          val target = timeStamps.getOrNull(index) ?: return@TabRow
          if (target.timestamp != selectedTimestamp) {
            selectedTimestamp = target.timestamp
            loading = true
            viewModel.refreshSnapshotTimestamp(target.timestamp, shouldClearDiff = true)
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp)
          .padding(bottom = 8.dp)
      )
    }

    Box(
      Modifier
        .weight(1f)
        .imePadding()
    ) {
      when {
        loading && items.isEmpty() -> LcLoadingState(message = stringResource(R.string.loading))

        timeStamps.isEmpty() -> LcEmptyState(
          title = stringResource(R.string.snapshot_no_snapshot),
          actionLabel = stringResource(R.string.channel_shoot),
          onAction = {
            if (!isCapturing) {
              isCapturing = shootController.computeSnapshot(dropPrevious = false)
            }
          }
        )

        items.isEmpty() -> LcEmptyState(title = stringResource(R.string.snapshot_empty_list_title))

        else -> PullToRefresh(
          isRefreshing = isRefreshing,
          onRefresh = {
            isRefreshing = true
            scope.launch {
              reloadTimeStamps(selectLatest = true)
              isRefreshing = false
            }
          },
          pullToRefreshState = pullToRefreshState,
          topAppBarScrollBehavior = scrollBehavior,
          modifier = Modifier.fillMaxSize()
        ) {
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
            if (systemProps.isNotEmpty()) {
              item(key = "system_props") {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .squircleBackground(MiuixTheme.colorScheme.surfaceContainerHigh, 12.dp)
                    .padding(12.dp)
                ) {
                  systemProps.forEach { prop ->
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = prop.label,
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                      )
                      Text(
                        text = prop.displayValue,
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurface
                      )
                    }
                  }
                }
              }
            }
            items(items = items, key = { it.packageName }) { item ->
              SnapshotDiffRow(
                item = item,
                iconSource = iconSources[item.packageName],
                highlight = highlight,
                onClick = {
                  if (item.shouldShowSnapshotNoDiffPopup()) {
                    noDiffItem = item
                  } else {
                    scope.launch {
                      context.launchSnapshotDetail(lifecycleOwner, item)
                    }
                  }
                }
              )
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
  }

  if (showDeleteDialog) {
    WindowDialog(
      show = true,
      title = stringResource(R.string.dialog_title_confirm_to_delete),
      summary = stringResource(
        R.string.dialog_message_confirm_to_delete,
        formattedDates[selectedTimestamp].orEmpty()
      ),
      onDismissRequest = { showDeleteDialog = false }
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        TextButton(
          text = stringResource(android.R.string.cancel),
          onClick = { showDeleteDialog = false },
          modifier = Modifier.weight(1f)
        )
        TextButton(
          text = stringResource(R.string.dialog_action_delete),
          onClick = {
            val timestamp = selectedTimestamp
            showDeleteDialog = false
            scope.launch {
              viewModel.deleteSnapshotTimeStamp(timestamp)
              selectedTimestamp = 0L
              reloadTimeStamps(selectLatest = true)
            }
          },
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.textButtonColorsPrimary()
        )
      }
    }
  }
  noDiffItem?.let { item ->
    val mode = when {
      item.newInstalled -> R.string.snapshot_indicator_added
      item.deleted -> R.string.snapshot_indicator_removed
      else -> R.string.snapshot_indicator_changed
    }
    WindowDialog(
      show = true,
      title = item.labelDiff.new ?: item.packageName,
      summary = stringResource(mode),
      onDismissRequest = { noDiffItem = null }
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.End
      ) {
        TextButton(
          text = stringResource(android.R.string.ok),
          onClick = { noDiffItem = null }
        )
      }
    }
  }
}

@Composable
private fun SnapshotDiffRow(
  item: SnapshotDiffItem,
  iconSource: SnapshotPackageIconSource?,
  highlight: String,
  onClick: () -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  val label = item.labelDiff.new ?: item.labelDiff.old
  val version = item.versionNameDiff.new ?: item.versionNameDiff.old
  val packageInfo = (iconSource as? SnapshotPackageIconSource.InstalledPackage)?.packageInfo

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .padding(bottom = 12.dp),
    onClick = onClick,
    showIndication = true,
    insideMargin = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      com.absinthe.libchecker.domain.app.list.ui.compose.AppIcon(
        packageInfo = packageInfo,
        label = label,
        modifier = Modifier
          .padding(end = 12.dp)
          .size(44.dp)
      )
      Column(Modifier.weight(1f)) {
        Text(
          text = label,
          fontWeight = FontWeight(550),
          color = colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = item.packageName,
          fontSize = 12.sp,
          fontWeight = FontWeight(500),
          color = colorScheme.onSurfaceVariantSummary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(1.dp))
        Text(
          text = version,
          fontSize = 12.sp,
          fontWeight = FontWeight(500),
          color = colorScheme.onSurfaceVariantSummary,
          maxLines = 1
        )
      }
      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        if (item.newInstalled) {
          SnapshotStateTag(
            text = stringResource(R.string.snapshot_indicator_added),
            container = colorScheme.tertiaryContainer,
            content = colorScheme.onTertiaryContainer
          )
        }
        if (item.deleted) {
          SnapshotStateTag(
            text = stringResource(R.string.snapshot_indicator_removed),
            container = colorScheme.errorContainer,
            content = colorScheme.onErrorContainer
          )
        }
        if (!item.newInstalled && !item.deleted && (item.changed > 0 || item.moved > 0)) {
          SnapshotStateTag(
            text = stringResource(R.string.snapshot_indicator_changed),
            container = colorScheme.secondaryContainer,
            content = colorScheme.onSecondaryContainer
          )
        }
      }
    }
  }
}

@Composable
private fun SnapshotStateTag(
  text: String,
  container: androidx.compose.ui.graphics.Color,
  content: androidx.compose.ui.graphics.Color
) {
  com.absinthe.libchecker.ui.compose.component.LcTag(
    text = text,
    containerColor = container,
    contentColor = content
  )
}