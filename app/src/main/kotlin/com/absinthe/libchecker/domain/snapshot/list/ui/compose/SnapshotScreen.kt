package com.absinthe.libchecker.domain.snapshot.list.ui.compose

import android.content.Context
import android.content.Intent
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.options.SnapshotOptions
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.ui.AppDetailActivity
import com.absinthe.libchecker.domain.snapshot.album.ui.AlbumActivity
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailDiffTextStyle
import com.absinthe.libchecker.domain.snapshot.detail.ui.launchSnapshotDetail
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotNoDiffMode
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.shouldShowSnapshotNoDiffPopup
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.toSnapshotNoDiffRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.toSnapshotNoDiffTitleIconRenderState
import com.absinthe.libchecker.domain.snapshot.detail.usecase.BuildSnapshotTitleDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.display.SnapshotAbiDisplayData
import com.absinthe.libchecker.domain.snapshot.display.SnapshotAbiDisplayItem
import com.absinthe.libchecker.domain.snapshot.display.SnapshotUpdateTimeText
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemCardPresentation
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemDisplayData
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemPackageStateLabel
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotListRenderState
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotSystemPropDisplayData
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.list.ui.SnapshotShootServiceController
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotItemDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.services.OnShootListener
import com.absinthe.libchecker.ui.compose.component.AdaptiveGlassOverflowMenu
import com.absinthe.libchecker.ui.compose.component.AdaptiveSearchOverlay
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopBarIconButton
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopBarMenuAction
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopSearchButton
import com.absinthe.libchecker.ui.compose.component.LcEmptyState
import com.absinthe.libchecker.ui.compose.component.LcTag
import com.absinthe.libchecker.ui.compose.component.LocalHomeBottomPadding
import com.absinthe.libchecker.ui.compose.component.MiuixPageHost
import com.absinthe.libchecker.ui.compose.component.SnapshotDiffCardSkeleton
import com.absinthe.libchecker.ui.compose.component.SnapshotInitialSkeleton
import com.absinthe.libchecker.ui.compose.component.TopBarSearchConfig
import com.absinthe.libchecker.ui.compose.component.TopBarSearchState
import com.absinthe.libchecker.ui.compose.component.adaptiveVerticalScroll
import com.absinthe.libchecker.ui.compose.component.rememberMiuixRefreshTexts
import com.absinthe.libchecker.ui.compose.component.rememberSkeletonBrush
import com.absinthe.libchecker.ui.compose.component.rememberTopBarSearchState
import com.absinthe.libchecker.ui.compose.theme.lcBlackFontFamily
import com.absinthe.libchecker.ui.compose.theme.lcCondensedFontFamily
import com.absinthe.libchecker.ui.compose.theme.withCustomFont
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import com.absinthe.libchecker.ui.compose.util.rememberActiveValue
import com.absinthe.libchecker.ui.compose.util.sheetBottomSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetHeightTransition
import com.absinthe.libchecker.ui.compose.util.toAnnotatedString
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.ChevronForward
import top.yukonga.miuix.kmp.icon.glass.MoreCircle
import top.yukonga.miuix.kmp.icon.glass.Save
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog
import com.absinthe.libchecker.ui.compose.component.FastScrollLazyColumn as LazyColumn

/**
 * Snapshot destination: pick a time node, review the diff of that snapshot and capture a new one.
 *
 * Comparison, filtering and capture still run in [SnapshotViewModel] and the shoot service.
 */
@Composable
fun SnapshotScreen(
  viewModel: SnapshotViewModel,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState(),
  isCurrentPage: Boolean = true,
  searchState: TopBarSearchState = rememberTopBarSearchState(),
  showSearchButton: Boolean = true
) {
  val context = LocalContext.current
  var hasActivated by rememberSaveable { mutableStateOf(isCurrentPage) }
  LaunchedEffect(isCurrentPage) {
    if (isCurrentPage) {
      hasActivated = true
    }
  }
  val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
    contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
  ) { }
  LaunchedEffect(hasActivated) {
    if (!hasActivated) return@LaunchedEffect
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
  val scrollBehavior = MiuixScrollBehavior()

  val buildDisplayData = remember {
    GlobalContext.get().get<BuildSnapshotItemDisplayDataUseCase>()
  }
  val buildTitleDisplayData = remember {
    GlobalContext.get().get<BuildSnapshotTitleDisplayDataUseCase>()
  }
  var timeStamps by remember { mutableStateOf<List<TimeStampItem>>(emptyList()) }
  var selectedTimestamp by remember { mutableLongStateOf(0L) }
  var items by remember { mutableStateOf<List<SnapshotDiffItem>>(emptyList()) }
  var renderState by remember { mutableStateOf(SnapshotListRenderState()) }
  var searchItems by remember { mutableStateOf<List<SnapshotDiffItem>>(emptyList()) }
  var searchRenderState by remember { mutableStateOf(SnapshotListRenderState()) }
  var snapshotCount by remember { mutableIntStateOf(0) }
  var appCount by remember { mutableIntStateOf(0) }
  var loading by remember { mutableStateOf(true) }
  val query = searchState.query
  var showDeleteDialog by remember { mutableStateOf(false) }
  var showMoreMenu by remember { mutableStateOf(false) }
  var snapshotOptions by remember { mutableIntStateOf(viewModel.getSnapshotOptions()) }
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

  DisposableEffect(hasActivated) {
    if (!hasActivated) return@DisposableEffect onDispose { }
    shootController.startAndBind(context)
    onDispose { shootController.release(context) }
  }

  LaunchedEffect(hasActivated) {
    if (!hasActivated) return@LaunchedEffect
    viewModel.effect.collect { effect ->
      when (effect) {
        is SnapshotViewModel.Effect.DashboardCountChange -> {
          snapshotCount = effect.snapshotCount
          appCount = effect.appCount
        }

        is SnapshotViewModel.Effect.TimeStampChange -> {
          if (effect.timestamp != 0L) {
            selectedTimestamp = effect.timestamp
          }
        }

        SnapshotViewModel.Effect.PackageListLoadFailed -> {
          loading = false
        }
      }
    }
  }

  LaunchedEffect(hasActivated) {
    if (!hasActivated) return@LaunchedEffect
    reloadTimeStamps(selectLatest = true)
  }

  LaunchedEffect(hasActivated) {
    if (!hasActivated) return@LaunchedEffect
    viewModel.snapshotDiffItemsUpdates.collect {
      val plan = viewModel.buildSnapshotListUpdatePlan(
        currentItems = items,
        highlightRefresh = false
      )
      items = plan.items
      renderState = plan.renderState
      loading = false
    }
  }

  var settledSearchQuery by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(query, items, hasActivated) {
    if (!hasActivated) return@LaunchedEffect
    if (query.isEmpty()) {
      searchItems = emptyList()
      searchRenderState = SnapshotListRenderState()
      settledSearchQuery = query
    } else {
      val plan = viewModel.buildSnapshotListUpdatePlan(
        currentItems = searchItems,
        highlightRefresh = true,
        searchKeyword = query
      )
      searchItems = plan.items
      searchRenderState = plan.renderState
      settledSearchQuery = query
    }
  }

  val formattedDates = remember(timeStamps) {
    timeStamps.associate { it.timestamp to viewModel.getFormatDateString(it.timestamp) }
  }

  LaunchedEffect(selectedTimestamp) {
    systemProps = if (selectedTimestamp == 0L) {
      emptyList()
    } else {
      runCatching<List<SnapshotSystemPropDisplayData>> {
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
      if (timestamp != selectedTimestamp) {
        selectedTimestamp = timestamp
        items = emptyList()
        loading = true
        viewModel.refreshSnapshotTimestamp(timestamp, shouldClearDiff = true)
      }
    },
    onDismiss = { showTimeNode = false }
  )

  fun toggleSnapshotOption(option: Int) {
    val checked = snapshotOptions and option == 0
    snapshotOptions = viewModel.setSnapshotOption(option, checked)
    scope.launch {
      val plan = viewModel.buildSnapshotListUpdatePlan(
        currentItems = items,
        highlightRefresh = true
      )
      items = plan.items
      renderState = plan.renderState
    }
  }

  val snapshotMenuSpecs = listOf(
    SnapshotOptions.SHOW_UPDATE_TIME to stringResource(R.string.snapshot_menu_show_update_time),
    SnapshotOptions.HIDE_NO_COMPONENT_CHANGES to stringResource(R.string.snapshot_menu_hide_no_component_changes),
    SnapshotOptions.DIFF_HIGHLIGHT to stringResource(R.string.snapshot_menu_diff_highlight),
    SnapshotOptions.DIFF_EMPHASIS to stringResource(R.string.snapshot_menu_diff_emphasis),
    SnapshotOptions.USE_IEC_UNITS to stringResource(R.string.snapshot_menu_use_iec_units)
  )

  WindowDialog(
    show = showKeepPreviousDialog,
    title = stringResource(R.string.dialog_title_keep_previous_snapshot),
    summary = stringResource(R.string.dialog_message_keep_previous_snapshot),
    onDismissRequest = { showKeepPreviousDialog = false }
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      TextButton(
        text = stringResource(R.string.btn_drop),
        onClick = {
          showKeepPreviousDialog = false
          isCapturing = shootController.computeSnapshot(dropPrevious = true)
        },
        modifier = Modifier.weight(1f)
      )
      TextButton(
        text = stringResource(R.string.btn_keep),
        onClick = {
          showKeepPreviousDialog = false
          isCapturing = shootController.computeSnapshot(dropPrevious = false)
        },
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.textButtonColorsPrimary()
      )
    }
  }
  val moreMenuAnchor = rememberGlassPopupAnchor()
  val homeBottomPadding = LocalHomeBottomPadding.current
  val moreMenuEntries = if (selectedTimestamp != 0L) {
    listOf(
      DropdownEntry(
        items = listOf(
          DropdownItem(
            text = stringResource(R.string.snapshot_time_node_title),
            onClick = {
              showMoreMenu = false
              showTimeNode = true
            }
          ),
          DropdownItem(
            text = stringResource(R.string.advanced_menu),
            children = snapshotMenuSpecs.map { (option, label) ->
              DropdownItem(
                text = label,
                selected = snapshotOptions and option != 0,
                onClick = { toggleSnapshotOption(option) }
              )
            }
          ),
          DropdownItem(
            text = stringResource(R.string.dialog_action_delete),
            onClick = {
              showMoreMenu = false
              showDeleteDialog = true
            }
          )
        )
      )
    )
  } else {
    emptyList()
  }
  val isScrolled = if (timeStamps.isEmpty()) {
    false
  } else {
    listState.canScrollBackward
  }

  MiuixPageHost(
    modifier = modifier.fillMaxSize(),
    enableMenuBackdrop = true,
    overlay = { pageState ->
      if (selectedTimestamp != 0L) {
        AdaptiveGlassOverflowMenu(
          show = showMoreMenu,
          onDismissRequest = { showMoreMenu = false },
          anchor = moreMenuAnchor,
          backdrop = pageState.menuBackdrop,
          entries = moreMenuEntries,
          collapseOnSelection = false
        ) {
          Icon(
            imageVector = MiuixIcons.Glass.MoreCircle,
            contentDescription = null
          )
        }
      }
    }
  ) {
    val pageState = this
    Box(modifier = Modifier.fillMaxSize()) {
      Scaffold(
        modifier = Modifier
          .fillMaxSize()
          .then(pageState.menuBackdropModifier),
        containerColor = MiuixTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
          AdaptiveTopAppBar(
            title = stringResource(R.string.title_snapshot),
            scrollBehavior = scrollBehavior,
            isContentScrolled = isScrolled,
            search = TopBarSearchConfig(
              state = searchState,
              label = stringResource(R.string.search_hint)
            ),
            actions = {
              if (showSearchButton) {
                AdaptiveTopSearchButton(onClick = searchState::open)
              }
              AdaptiveTopBarIconButton(
                enabled = !isCapturing,
                onClick = {
                  if (timeStamps.isNotEmpty()) {
                    showKeepPreviousDialog = true
                  } else {
                    isCapturing = shootController.computeSnapshot(dropPrevious = false)
                  }
                }
              ) {
                Icon(
                  imageVector = MiuixIcons.Glass.Save,
                  contentDescription = stringResource(R.string.channel_shoot)
                )
              }
              if (selectedTimestamp != 0L) {
                AdaptiveTopBarMenuAction(
                  entries = moreMenuEntries,
                  menuAnchor = moreMenuAnchor,
                  show = showMoreMenu,
                  onShowChange = { showMoreMenu = it },
                  isContentScrolled = isScrolled,
                  collapseOnSelection = false
                ) {
                  Icon(
                    imageVector = MiuixIcons.Glass.MoreCircle,
                    contentDescription = null
                  )
                }
              }
            },
            bottomContent = {
              if (comparingProgress in 1..99) {
                LinearProgressIndicator(
                  modifier = Modifier.fillMaxWidth(),
                  progress = comparingProgress / 100f,
                  height = 2.dp
                )
              }
            }
          )
        }
      ) { padding ->
        Box(
          Modifier
            .fillMaxSize()
            .then(pageState.topBarBackdropModifier)
            .imePadding()
        ) {
          val screenMode = when {
            timeStamps.isEmpty() && loading -> SnapshotScreenMode.InitialLoading
            timeStamps.isEmpty() -> SnapshotScreenMode.Empty
            else -> SnapshotScreenMode.Content
          }
          Crossfade(
            targetState = screenMode,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            label = "SnapshotScreenModeTransition"
          ) { mode ->
            when (mode) {
              SnapshotScreenMode.InitialLoading -> WideContentBox { sidePadding ->
                SnapshotInitialSkeleton(
                  contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 6.dp,
                    start = sidePadding,
                    end = sidePadding
                  ),
                  homeBottomPadding = homeBottomPadding
                )
              }

              SnapshotScreenMode.Empty -> Box(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(padding)
              ) {
                LcEmptyState(
                  title = stringResource(R.string.snapshot_no_snapshot),
                  actionLabel = stringResource(R.string.channel_shoot),
                  onAction = {
                    if (!isCapturing) {
                      isCapturing = shootController.computeSnapshot(dropPrevious = false)
                    }
                  }
                )
              }

              SnapshotScreenMode.Content -> PullToRefresh(
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
                contentPadding = PaddingValues(top = padding.calculateTopPadding() + 6.dp),
                refreshTexts = rememberMiuixRefreshTexts(),
                modifier = Modifier.fillMaxSize()
              ) {
                WideContentBox { sidePadding ->
                  val snapshotSkeletonBrush = rememberSkeletonBrush()
                  val itemPlacementSpec = remember {
                    spring<IntOffset>(
                      dampingRatio = Spring.DampingRatioNoBouncy,
                      stiffness = Spring.StiffnessMediumLow,
                      visibilityThreshold = IntOffset.VisibilityThreshold
                    )
                  }
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
                    item(key = "dashboard") {
                      SnapshotDashboardCard(
                        timestampText = formattedDates[selectedTimestamp].orEmpty(),
                        snapshotCount = snapshotCount,
                        appCount = appCount,
                        systemProps = systemProps,
                        onTimestampClick = { showTimeNode = true },
                        onCardClick = {
                          context.startActivity(
                            android.content.Intent(context, AlbumActivity::class.java)
                          )
                        }
                      )
                    }
                    when {
                      loading && items.isEmpty() -> items(
                        count = 5,
                        key = { "snapshot_diff_skeleton_$it" }
                      ) { index ->
                        SnapshotDiffCardSkeleton(
                          index = index,
                          brush = snapshotSkeletonBrush,
                          modifier = Modifier.animateItem(
                            fadeInSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                            placementSpec = itemPlacementSpec,
                            fadeOutSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                          )
                        )
                      }

                      items.isEmpty() -> item(key = "empty_diffs") {
                        Box(
                          modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .animateItem(
                              fadeInSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                              placementSpec = itemPlacementSpec,
                              fadeOutSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                            )
                        ) {
                          LcEmptyState(title = stringResource(R.string.snapshot_empty_list_title))
                        }
                      }

                      else -> items(items = items, key = { it.packageName }) { item ->
                        SnapshotDiffRow(
                          item = item,
                          renderState = renderState,
                          buildDisplayData = buildDisplayData,
                          onClick = {
                            if (item.shouldShowSnapshotNoDiffPopup()) {
                              noDiffItem = item
                            } else {
                              scope.launch {
                                context.launchSnapshotDetail(lifecycleOwner, item)
                              }
                            }
                          },
                          modifier = Modifier.animateItem(
                            fadeInSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                            placementSpec = itemPlacementSpec,
                            fadeOutSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                          )
                        )
                      }
                    }
                    item {
                      Spacer(
                        if (homeBottomPadding > 0.dp) {
                          Modifier.height(24.dp + homeBottomPadding)
                        } else {
                          Modifier
                            .height(24.dp)
                            .navigationBarsPadding()
                        }
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      AdaptiveSearchOverlay(
        config = TopBarSearchConfig(
          state = searchState,
          label = stringResource(R.string.search_hint)
        )
      ) { activeQuery ->
        val searchListState = rememberLazyListState()
        val isSearchPending = (loading && items.isEmpty()) ||
          (activeQuery.isNotEmpty() && settledSearchQuery != activeQuery)
        LaunchedEffect(activeQuery) {
          searchListState.scrollToItem(0)
        }
        if (isSearchPending && searchItems.isEmpty()) {
          val brush = rememberSkeletonBrush()
          WideContentBox { sidePadding ->
            LazyColumn(
              userScrollEnabled = false,
              contentPadding = PaddingValues(
                top = 6.dp,
                start = sidePadding,
                end = sidePadding,
                bottom = homeBottomPadding
              ),
              modifier = Modifier
                .fillMaxSize()
                .horizontalCutoutPadding()
            ) {
              items(count = 5, key = { "snapshot_search_skeleton_$it" }) { index ->
                SnapshotDiffCardSkeleton(index = index, brush = brush)
              }
            }
          }
        } else if (searchItems.isEmpty()) {
          LcEmptyState(title = stringResource(R.string.snapshot_empty_list_title))
        } else {
          WideContentBox { sidePadding ->
            LazyColumn(
              state = searchListState,
              modifier = Modifier
                .fillMaxSize()
                .horizontalCutoutPadding()
                .adaptiveVerticalScroll(),
              contentPadding = PaddingValues(
                top = 6.dp,
                start = sidePadding,
                end = sidePadding
              )
            ) {
              items(items = searchItems, key = { it.packageName }) { item ->
                SnapshotDiffRow(
                  item = item,
                  renderState = searchRenderState,
                  buildDisplayData = buildDisplayData,
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
                  if (homeBottomPadding > 0.dp) {
                    Modifier.height(24.dp + homeBottomPadding)
                  } else {
                    Modifier
                      .height(24.dp)
                      .navigationBarsPadding()
                  }
                )
              }
            }
          }
        }
      }
    }
  }

  WindowDialog(
    show = showDeleteDialog,
    title = stringResource(R.string.dialog_title_confirm_to_delete),
    summary = stringResource(
      R.string.dialog_message_confirm_to_delete,
      formattedDates[selectedTimestamp].orEmpty()
    ),
    onDismissRequest = { showDeleteDialog = false }
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
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
  val activeNoDiffItem = rememberActiveValue(noDiffItem)
  SnapshotNoDiffSheet(
    show = noDiffItem != null,
    item = activeNoDiffItem.value,
    iconSource = activeNoDiffItem.value?.let { renderState.packageIconSources[it.packageName] },
    snapshotOptions = snapshotOptions,
    buildTitleDisplayData = buildTitleDisplayData,
    onOpenAppDetail = { packageName ->
      noDiffItem = null
      runCatching {
        context.startActivity(
          Intent(context, AppDetailActivity::class.java)
            .putExtra(EXTRA_PACKAGE_NAME, packageName)
        )
      }
    },
    onDismiss = { noDiffItem = null },
    onDismissFinished = { if (noDiffItem == null) activeNoDiffItem.value = null }
  )
}

@Composable
private fun SnapshotNoDiffSheet(
  show: Boolean,
  item: SnapshotDiffItem?,
  iconSource: SnapshotPackageIconSource?,
  snapshotOptions: Int,
  buildTitleDisplayData: BuildSnapshotTitleDisplayDataUseCase,
  onOpenAppDetail: (String) -> Unit,
  onDismiss: () -> Unit,
  onDismissFinished: () -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  val primaryArgb = colorScheme.primary.toArgb()
  val onSurfaceArgb = colorScheme.onSurface.toArgb()
  val noDiffState = remember(item, snapshotOptions, primaryArgb, onSurfaceArgb) {
    item?.toSnapshotNoDiffRenderState(
      title = buildTitleDisplayData(
        BuildSnapshotTitleDisplayDataUseCase.Request(
          item = item,
          formatSplitPackageName = true,
          diffTextStyle = SnapshotDetailDiffTextStyle(
            highlightColor = if (snapshotOptions and SnapshotOptions.DIFF_HIGHLIGHT > 0) primaryArgb else null,
            emphasizeDiffs = snapshotOptions and SnapshotOptions.DIFF_EMPHASIS > 0,
            arrowColor = onSurfaceArgb,
            metricDeltaColor = onSurfaceArgb
          )
        )
      )
    )
  }
  val iconState = remember(iconSource) {
    iconSource.toSnapshotNoDiffTitleIconRenderState()
  }

  WindowBottomSheet(
    show = show,
    title = stringResource(R.string.detail),
    onDismissRequest = onDismiss,
    onDismissFinished = onDismissFinished
  ) {
    if (item == null || noDiffState == null) return@WindowBottomSheet
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .sheetHeightTransition()
        .padding(bottom = sheetBottomSafePadding(24.dp)),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
          color = colorScheme.onSurface.copy(alpha = 0.04f)
        ),
        onClick = if (iconState.opensDetailOnClick) {
          { onOpenAppDetail(item.packageName) }
        } else {
          null
        },
        showIndication = iconState.opensDetailOnClick,
        insideMargin = PaddingValues(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          when (val source = iconState.iconSource) {
            is SnapshotPackageIconSource.InstalledPackage -> com.absinthe.libchecker.domain.app.list.ui.compose.AppIcon(
              packageInfo = source.packageInfo,
              label = noDiffState.title.appName.toString(),
              modifier = Modifier
                .padding(end = 12.dp)
                .size(48.dp)
            )

            else -> Image(
              painter = painterResource(R.drawable.ic_icon_blueprint),
              contentDescription = null,
              modifier = Modifier
                .padding(end = 12.dp)
                .size(48.dp)
                .squircleClip(12.dp)
            )
          }
          Column(Modifier.weight(1f)) {
            Text(
              text = noDiffState.title.appName.toAnnotatedString(),
              style = MiuixTheme.textStyles.title4.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
              ),
              color = colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
              text = noDiffState.title.packageName.toAnnotatedString(),
              style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
              color = colorScheme.onSurfaceVariantSummary
            )
            Spacer(Modifier.height(4.dp))
            Text(
              text = noDiffState.title.versionInfo.toAnnotatedString(),
              style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
              color = colorScheme.onSurfaceVariantSummary
            )
            noDiffState.title.packageSize?.let { sizeData ->
              Text(
                text = sizeData.text.toAnnotatedString(),
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                color = colorScheme.onSurfaceVariantSummary
              )
            }
            Text(
              text = noDiffState.title.apis.toAnnotatedString(),
              style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
              color = colorScheme.onSurfaceVariantSummary
            )
          }
        }
      }

      val (illustrationRes, messageRes) = when (noDiffState.mode) {
        SnapshotNoDiffMode.New -> R.drawable.ic_yes to R.string.snapshot_detail_new_install_title
        SnapshotNoDiffMode.Deleted -> R.drawable.ic_no to R.string.snapshot_detail_deleted_title
        SnapshotNoDiffMode.NothingChanged -> R.drawable.ic_notebook to R.string.snapshot_empty_list_title
      }
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Image(
          painter = painterResource(illustrationRes),
          contentDescription = null,
          modifier = Modifier.size(120.dp)
        )
        Text(
          text = stringResource(messageRes),
          style = MiuixTheme.textStyles.title4.copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
          ),
          color = colorScheme.onSurface
        )
      }
    }
  }
}

@Composable
private fun SnapshotDashboardCard(
  timestampText: String,
  snapshotCount: Int,
  appCount: Int,
  systemProps: List<SnapshotSystemPropDisplayData>,
  onTimestampClick: () -> Unit,
  onCardClick: () -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .padding(bottom = 12.dp),
    onClick = onCardClick,
    showIndication = true,
    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.snapshot_current_timestamp),
          style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
          color = colorScheme.onSurfaceVariantSummary
        )
        Spacer(Modifier.height(2.dp))
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onTimestampClick)
            .padding(vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = timestampText.ifBlank { "..." },
            style = MiuixTheme.textStyles.body1.copy(
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            ).withCustomFont(lcBlackFontFamily()),
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(Modifier.width(4.dp))
          Icon(
            imageVector = MiuixIcons.Glass.ChevronForward,
            contentDescription = stringResource(R.string.snapshot_time_node_title),
            tint = colorScheme.onSurface,
            modifier = Modifier
              .size(16.dp)
              .graphicsLayer { rotationZ = 90f }
          )
        }
        Spacer(Modifier.height(6.dp))
        Text(
          text = stringResource(R.string.snapshot_apps_count),
          style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
          color = colorScheme.onSurfaceVariantSummary
        )
        Spacer(Modifier.height(2.dp))
        val blackFont = lcBlackFontFamily()
        Text(
          text = if (snapshotCount > 0 || appCount > 0) "$snapshotCount / $appCount" else "...",
          style = MiuixTheme.textStyles.body1.copy(
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          ).withCustomFont(blackFont),
          color = colorScheme.onSurface
        )
        if (systemProps.isNotEmpty()) {
          Spacer(Modifier.height(6.dp))
          systemProps.forEach { prop ->
            val propText = if (blackFont != null) {
              androidx.compose.ui.text.buildAnnotatedString {
                append("${prop.label}: ")
                pushStyle(
                  androidx.compose.ui.text.SpanStyle(
                    fontFamily = blackFont,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                  )
                )
                append(prop.displayValue)
                pop()
              }
            } else {
              androidx.compose.ui.text.AnnotatedString("${prop.label}: ${prop.displayValue}")
            }
            Text(
              text = propText,
              style = MiuixTheme.textStyles.footnote2.copy(fontSize = 11.sp),
              color = colorScheme.onSurfaceVariantSummary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
      Column(
        modifier = Modifier.padding(start = 12.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        SnapshotLegendItem(
          labelRes = R.string.snapshot_indicator_added,
          iconRes = R.drawable.ic_add,
          colorRes = R.color.material_green_300
        )
        SnapshotLegendItem(
          labelRes = R.string.snapshot_indicator_removed,
          iconRes = R.drawable.ic_remove,
          colorRes = R.color.material_red_300
        )
        SnapshotLegendItem(
          labelRes = R.string.snapshot_indicator_changed,
          iconRes = R.drawable.ic_changed,
          colorRes = R.color.material_yellow_300
        )
        SnapshotLegendItem(
          labelRes = R.string.snapshot_indicator_moved,
          iconRes = R.drawable.ic_move,
          colorRes = R.color.material_blue_300
        )
      }
    }
  }
}

@Composable
private fun SnapshotLegendItem(
  labelRes: Int,
  iconRes: Int,
  colorRes: Int
) {
  val colorScheme = MiuixTheme.colorScheme
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Text(
      text = stringResource(labelRes),
      style = MiuixTheme.textStyles.footnote2.copy(fontSize = 11.sp),
      color = colorScheme.onSurfaceVariantSummary
    )
    Icon(
      painter = painterResource(iconRes),
      contentDescription = null,
      tint = colorScheme.onSurface,
      modifier = Modifier.size(14.dp)
    )
    Box(
      modifier = Modifier
        .size(width = 20.dp, height = 5.dp)
        .background(colorResource(colorRes), RoundedCornerShape(99.dp))
    )
  }
}

private enum class SnapshotScreenMode {
  InitialLoading,
  Empty,
  Content
}

@Composable
private fun SnapshotDiffRow(
  item: SnapshotDiffItem,
  renderState: SnapshotListRenderState,
  buildDisplayData: BuildSnapshotItemDisplayDataUseCase,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colorScheme = MiuixTheme.colorScheme
  val highlightColorInt = if (renderState.displayOptions.highlightDiffs) {
    colorScheme.primary.toArgb()
  } else {
    null
  }
  val displayData: SnapshotItemDisplayData = remember(item, renderState, highlightColorInt) {
    buildDisplayData(
      BuildSnapshotItemDisplayDataUseCase.Request(
        item = item,
        cardPresentation = if (item.newInstalled || item.deleted) {
          SnapshotItemCardPresentation.Rounded
        } else {
          SnapshotItemCardPresentation.Normal
        },
        iconSource = renderState.packageIconSources[item.packageName],
        showUpdateTime = renderState.displayOptions.showUpdateTime,
        isApexPackage = item.packageName in renderState.apexPackageNames,
        animateStateIndicator = false,
        tintChangedAbiBadge = renderState.displayOptions.tintAbiLabels,
        highlightDiffColor = highlightColorInt,
        emphasizeDiffs = renderState.displayOptions.emphasizeDiffs,
        highlightText = renderState.highlightText
      )
    )
  }
  val isNewOrDeleted = displayData.isNewInstalled || displayData.isDeleted
  val cardBorderModifier = if (isNewOrDeleted) {
    val borderColorRes = if (displayData.isNewInstalled) {
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
        .alpha(if (displayData.isDeleted) 0.7f else 1f),
      verticalAlignment = Alignment.CenterVertically
    ) {
      val iconSource = displayData.iconSource
      if (iconSource is SnapshotPackageIconSource.InstalledPackage) {
        com.absinthe.libchecker.domain.app.list.ui.compose.AppIcon(
          packageInfo = iconSource.packageInfo,
          label = displayData.appName.text.toString(),
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
            text = displayData.appName.text.toAnnotatedString(),
            style = MiuixTheme.textStyles.body1.copy(
              fontSize = 15.sp,
              fontWeight = FontWeight.Medium
            ),
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
          )
          if (displayData.appName.showTrackIcon) {
            Spacer(Modifier.width(4.dp))
            Icon(
              painter = painterResource(R.drawable.ic_track),
              contentDescription = null,
              tint = colorScheme.primary,
              modifier = Modifier.size(14.dp)
            )
          }
          displayData.appName.packageStateLabel?.let { stateLabel ->
            Spacer(Modifier.width(6.dp))
            when (stateLabel) {
              SnapshotItemPackageStateLabel.New -> LcTag(
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

              SnapshotItemPackageStateLabel.Deleted -> LcTag(
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
        }
        Text(
          text = displayData.packageName,
          style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
          color = colorScheme.onSurfaceVariantSummary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        val condensedFont = lcCondensedFontFamily()
        if (displayData.versionInfo.isNotBlank()) {
          Text(
            text = displayData.versionInfo.toAnnotatedString(),
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp)
              .withCustomFont(condensedFont),
            color = colorScheme.onSurfaceVariantSummary
          )
        }
        displayData.packageSize?.let { pkgSize ->
          Text(
            text = pkgSize.text.toAnnotatedString(),
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp)
              .withCustomFont(condensedFont),
            color = colorScheme.onSurfaceVariantSummary
          )
        }
        if (displayData.apiText.isNotBlank()) {
          Text(
            text = displayData.apiText.toAnnotatedString(),
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp)
              .withCustomFont(condensedFont),
            color = colorScheme.onSurfaceVariantSummary
          )
        }
        displayData.updateTimeDisplayData?.let { updateTime ->
          val timeLabel = when (val text = updateTime.text) {
            SnapshotUpdateTimeText.Preinstalled ->
              stringResource(R.string.snapshot_preinstalled_app)

            is SnapshotUpdateTimeText.LastUpdated ->
              stringResource(R.string.format_last_updated, text.timeText)
          }
          Text(
            text = timeLabel,
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 11.sp)
              .withCustomFont(condensedFont),
            color = colorScheme.onSurfaceVariantSummary
          )
        }
        SnapshotAbiRow(
          abiDisplayData = displayData.abi.abiDisplayData,
          showChangedAbi = displayData.abi.showChangedAbi,
          tintChangedAbiBadge = displayData.abi.tintChangedAbiBadge
        )
      }

      if (!isNewOrDeleted) {
        val si = displayData.stateIndicator
        val segments = listOf(
          R.color.material_green_300 to si.added,
          R.color.material_red_300 to si.removed,
          R.color.material_yellow_300 to si.changed,
          R.color.material_blue_300 to si.moved
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
        }
      }
    }
  }
}

@Composable
private fun SnapshotAbiRow(
  abiDisplayData: SnapshotAbiDisplayData,
  showChangedAbi: Boolean,
  tintChangedAbiBadge: Boolean
) {
  val colorScheme = MiuixTheme.colorScheme
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    SnapshotAbiSegment(
      item = abiDisplayData.old,
      tintColor = if (showChangedAbi && tintChangedAbiBadge) {
        colorScheme.error
      } else {
        colorScheme.onSurfaceVariantSummary
      }
    )
    val newAbi = abiDisplayData.new
    if (showChangedAbi && newAbi != null) {
      Text(
        text = "→",
        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
        color = colorScheme.onSurfaceVariantSummary
      )
      SnapshotAbiSegment(
        item = newAbi,
        tintColor = if (tintChangedAbiBadge) {
          colorScheme.primary
        } else {
          colorScheme.onSurfaceVariantSummary
        }
      )
    }
  }
}

@Composable
private fun SnapshotAbiSegment(
  item: SnapshotAbiDisplayItem,
  tintColor: androidx.compose.ui.graphics.Color
) {
  val colorScheme = MiuixTheme.colorScheme
  item.badgeRes?.let { badgeRes ->
    Icon(
      painter = painterResource(badgeRes),
      contentDescription = null,
      tint = tintColor,
      modifier = Modifier.size(width = 24.dp, height = 16.dp)
    )
  }
  if (item.isMultiArch) {
    Icon(
      painter = painterResource(R.drawable.ic_multi_arch),
      contentDescription = stringResource(R.string.multiArch),
      tint = tintColor,
      modifier = Modifier.size(16.dp)
    )
  }
  Text(
    text = item.text,
    style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp)
      .withCustomFont(lcCondensedFontFamily()),
    color = colorScheme.onSurfaceVariantSummary,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis
  )
}
