package com.absinthe.libchecker.domain.app.list.ui.compose

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Process
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.Coil
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.detail.ui.compose.OverlayDetailSheet
import com.absinthe.libchecker.domain.home.presentation.HomeViewModel
import com.absinthe.libchecker.ui.compose.component.AdaptiveGlassOverflowMenu
import com.absinthe.libchecker.ui.compose.component.AdaptiveSearchOverlay
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopBarIconButton
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopBarMenuAction
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopSearchButton
import com.absinthe.libchecker.ui.compose.component.AppListSkeleton
import com.absinthe.libchecker.ui.compose.component.LcEmptyState
import com.absinthe.libchecker.ui.compose.component.LocalHomeBottomPadding
import com.absinthe.libchecker.ui.compose.component.MiuixPageHost
import com.absinthe.libchecker.ui.compose.component.TopBarSearchConfig
import com.absinthe.libchecker.ui.compose.component.TopBarSearchState
import com.absinthe.libchecker.ui.compose.component.adaptiveVerticalScroll
import com.absinthe.libchecker.ui.compose.component.rememberMiuixRefreshTexts
import com.absinthe.libchecker.ui.compose.component.rememberTopBarSearchState
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.showToast
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Blocklist
import top.yukonga.miuix.kmp.icon.glass.Info
import top.yukonga.miuix.kmp.icon.glass.Timer
import top.yukonga.miuix.kmp.icon.glass.Tune
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.absinthe.libchecker.ui.compose.component.FastScrollLazyColumn as LazyColumn

/**
 * App list destination.
 *
 * Filtering, sorting and package lookups still come from [HomeViewModel] and its use cases, so the
 * Compose list and the remaining view-system screens share one behaviour model.
 */
@Composable
fun AppListScreen(
  homeViewModel: HomeViewModel,
  detailViewModel: com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel,
  reselectSignal: Int = 0,
  isCurrentPage: Boolean = true,
  recentVisitsViewModel: com.absinthe.libchecker.domain.home.presentation.RecentVisitsViewModel? = null,
  onOpenRecentApp: ((String) -> Unit)? = null,
  onOpenRecentLibrary: ((com.absinthe.libchecker.domain.home.recent.RecentVisit) -> Unit)? = null,
  onOpenDetail: (LCItem) -> Unit,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState(),
  searchState: TopBarSearchState = rememberTopBarSearchState(
    initialQuery = homeViewModel.getAppListSearchQuery(),
    initialExpanded = homeViewModel.getAppListSearchQuery().isNotEmpty()
  ),
  showSearchButton: Boolean = true
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  val iconSizePx = remember(density) { with(density) { 48.dp.roundToPx() } }

  val uiState by homeViewModel.appListUiState.collectAsStateWithLifecycle()
  val searchUiState by homeViewModel.appListSearchUiState.collectAsStateWithLifecycle()
  var overlayItem by remember { mutableStateOf<LCItem?>(null) }
  var displayOptions by remember { mutableIntStateOf(homeViewModel.getAppListAdvancedMenuState().displayOptions) }
  val initialSearchQuery = remember { homeViewModel.getAppListSearchQuery() }
  val query = searchState.query
  var lastDispatchedQuery by remember { mutableStateOf(initialSearchQuery) }
  var isRefreshing by remember { mutableStateOf(false) }
  var pendingDump by remember { mutableStateOf<HomeViewModel.AppListSearchCommandAction.DumpAppsInfo?>(null) }

  val scrollBehavior = MiuixScrollBehavior()
  val pullToRefreshState = rememberPullToRefreshState()
  val title = rememberAppTitle(context)
  val toolbarLoading by homeViewModel.toolbarLoading.collectAsStateWithLifecycle(initialValue = false)

  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (homeViewModel.onPackagesPermissionResult(granted)) {
      homeViewModel.initItems()
    }
  }
  val dumpLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.CreateDocument("*/*")
  ) { uri ->
    val action = pendingDump
    pendingDump = null
    if (uri != null && action != null) {
      homeViewModel.dumpAppsInfo(uri, action.format)
    }
  }

  var hasActivated by rememberSaveable { mutableStateOf(isCurrentPage) }
  LaunchedEffect(isCurrentPage) {
    if (isCurrentPage) {
      hasActivated = true
    }
  }

  LaunchedEffect(hasActivated) {
    if (!hasActivated) return@LaunchedEffect
    val appContext = context.applicationContext
    homeViewModel.onAppListViewCreated()
    homeViewModel.ensureAppListStarted(
      isCurrentProcess64Bit = Process.is64Bit(),
      onBeforeFirstContent = { items, iconPackageInfos ->
        prewarmVisibleAppIcons(
          context = appContext,
          items = items,
          iconPackageInfos = iconPackageInfos,
          iconSizePx = iconSizePx
        )
      }
    )
  }

  LaunchedEffect(uiState.mode) {
    homeViewModel.setPackagesPermissionCheckPending(
      uiState.mode == HomeViewModel.AppListScreenMode.Reject
    )
  }

  LaunchedEffect(reselectSignal) {
    if (reselectSignal > 0) {
      listState.animateScrollToItem(0)
    }
  }

  var appliedScrollToTopSignal by rememberSaveable { mutableIntStateOf(uiState.scrollToTopSignal) }
  if (appliedScrollToTopSignal != uiState.scrollToTopSignal) {
    // Override key-based anchoring in the same measure as the new sort/filter result.
    // Scrolling in a later effect first shows the old key at its new index, then jumps back.
    listState.requestScrollToItem(0)
    appliedScrollToTopSignal = uiState.scrollToTopSignal
  }

  LaunchedEffect(listState) {
    listState.interactionSource.interactions.collect { interaction ->
      if (interaction is DragInteraction.Start) {
        homeViewModel.onAppListUserScrolled()
      }
    }
  }

  var showRecentVisits by remember { mutableStateOf(false) }

  if (recentVisitsViewModel != null) {
    com.absinthe.libchecker.domain.home.ui.compose.RecentVisitsSheet(
      show = showRecentVisits,
      viewModel = recentVisitsViewModel,
      onOpenApp = { targetPackage ->
        showRecentVisits = false
        onOpenRecentApp?.invoke(targetPackage)
      },
      onOpenLibrary = { visit ->
        showRecentVisits = false
        onOpenRecentLibrary?.invoke(visit)
      },
      onDismiss = { showRecentVisits = false }
    )
  }

  OverlayDetailSheet(
    show = overlayItem != null,
    viewModel = detailViewModel,
    item = overlayItem,
    onDismiss = { overlayItem = null }
  )

  LaunchedEffect(Unit) {
    homeViewModel.effect.collect { effect ->
      if (effect is HomeViewModel.Effect.PackageListLoadFailed) {
        isRefreshing = false
        context.showToast(R.string.package_list_load_failed)
      }
    }
  }

  LaunchedEffect(toolbarLoading) {
    if (!toolbarLoading) {
      isRefreshing = false
    }
  }

  LaunchedEffect(query) {
    val shouldSyncRestoredQuery = query.isNotEmpty() && searchUiState.query != query
    if (query == lastDispatchedQuery && !shouldSyncRestoredQuery) return@LaunchedEffect
    if (query != lastDispatchedQuery) {
      delay(120)
    }
    val change = homeViewModel.onAppListSearchQueryChanged(query)
    lastDispatchedQuery = query
    if (change.shouldRefreshItems || shouldSyncRestoredQuery) {
      homeViewModel.refreshAppListAfterSearch()
    }
    when (val action = change.action) {
      HomeViewModel.AppListSearchCommandAction.None -> Unit
      HomeViewModel.AppListSearchCommandAction.EasterEgg -> context.showToast("🥚")
      HomeViewModel.AppListSearchCommandAction.DebugModeEnabled -> context.showToast("DEBUG MODE")
      HomeViewModel.AppListSearchCommandAction.UserModeEnabled -> context.showToast("USER MODE")
      is HomeViewModel.AppListSearchCommandAction.DumpAppsInfo -> {
        pendingDump = action
        runCatching { dumpLauncher.launch(action.fileName) }
      }
    }
  }

  fun updateDisplayOptions(newOptions: Int) {
    val diff = displayOptions xor newOptions
    if (diff == 0) return
    displayOptions = newOptions
    homeViewModel.setAppListDisplayOptions(newOptions)
    homeViewModel.onAppListAdvancedMenuDismissed(displayOptionsDiff = diff, itemDisplayOptionsDiff = 0)
  }

  var showFilterMenu by remember { mutableStateOf(false) }
  val filterMenuAnchor = rememberGlassPopupAnchor()
  val appListMenuState = rememberAppListMenuState(
    displayOptions = displayOptions,
    onDisplayOptionsChange = ::updateDisplayOptions,
    onDismissMenu = { showFilterMenu = false }
  )
  val homeBottomPadding = LocalHomeBottomPadding.current

  MiuixPageHost(
    modifier = modifier.fillMaxSize(),
    enableMenuBackdrop = true,
    overlay = { pageState ->
      AdaptiveGlassOverflowMenu(
        show = showFilterMenu,
        onDismissRequest = { showFilterMenu = false },
        anchor = filterMenuAnchor,
        backdrop = pageState.menuBackdrop,
        entries = appListMenuState.entries,
        collapseOnSelection = false
      ) {
        Icon(
          imageVector = MiuixIcons.Glass.Tune,
          contentDescription = stringResource(R.string.menu_filter)
        )
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
            title = title,
            scrollBehavior = scrollBehavior,
            isContentScrolled = listState.canScrollBackward,
            search = TopBarSearchConfig(
              state = searchState,
              label = stringResource(R.string.search_hint)
            ),
            actions = {
              if (showSearchButton) {
                AdaptiveTopSearchButton(onClick = searchState::open)
              }
              if (recentVisitsViewModel != null) {
                AdaptiveTopBarIconButton(onClick = { showRecentVisits = true }) {
                  Icon(
                    imageVector = MiuixIcons.Glass.Timer,
                    contentDescription = stringResource(R.string.recent_apps)
                  )
                }
              }
              AdaptiveTopBarMenuAction(
                entries = appListMenuState.entries,
                menuAnchor = filterMenuAnchor,
                show = showFilterMenu,
                onShowChange = { showFilterMenu = it },
                isContentScrolled = listState.canScrollBackward,
                collapseOnSelection = false
              ) {
                Icon(
                  imageVector = MiuixIcons.Glass.Tune,
                  contentDescription = stringResource(R.string.menu_filter)
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
          Crossfade(
            targetState = uiState.mode,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            label = "AppListModeTransition"
          ) { mode ->
            when (mode) {
              HomeViewModel.AppListScreenMode.Reject -> Box(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(padding)
              ) {
                AppListPermissionState(
                  onRequest = { runCatching { permissionLauncher.launch(Constants.GET_INSTALLED_APPS) } }
                )
              }

              HomeViewModel.AppListScreenMode.Loading -> WideContentBox { sidePadding ->
                AppListSkeleton(
                  contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 6.dp,
                    start = sidePadding,
                    end = sidePadding
                  ),
                  homeBottomPadding = homeBottomPadding
                )
              }

              HomeViewModel.AppListScreenMode.Empty -> LcEmptyState(
                title = stringResource(R.string.empty_list),
                icon = MiuixIcons.Glass.Info,
                modifier = Modifier.padding(padding)
              )

              HomeViewModel.AppListScreenMode.Content -> PullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = {
                  isRefreshing = true
                  homeViewModel.requestChange(showLoadingFeedback = true)
                },
                pullToRefreshState = pullToRefreshState,
                topAppBarScrollBehavior = scrollBehavior,
                contentPadding = PaddingValues(top = padding.calculateTopPadding() + 6.dp),
                refreshTexts = rememberMiuixRefreshTexts(),
                modifier = Modifier.fillMaxSize()
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
                    items(items = uiState.items, key = { it.packageName }) { item ->
                      AppListRow(
                        item = item,
                        viewState = uiState.viewStates[item.packageName],
                        packageInfo = uiState.iconPackageInfos[item.packageName],
                        chips = uiState.chips[item.packageName].orEmpty(),
                        highlight = "",
                        onClick = {
                          if (item.abi.toInt() == Constants.OVERLAY) {
                            overlayItem = item
                          } else {
                            onOpenDetail(item)
                          }
                        }
                      )
                    }
                    item(key = "app_list_bottom_spacer") {
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
        LaunchedEffect(searchUiState.query) {
          if (searchUiState.query.isNotEmpty()) {
            searchListState.scrollToItem(0)
          }
        }
        val isSearchPending = searchUiState.isLoading ||
          (activeQuery.isNotEmpty() && searchUiState.query != activeQuery)
        when {
          isSearchPending && searchUiState.items.isEmpty() -> WideContentBox { sidePadding ->
            AppListSkeleton(
              contentPadding = PaddingValues(
                top = 6.dp,
                start = sidePadding,
                end = sidePadding
              ),
              homeBottomPadding = homeBottomPadding
            )
          }

          searchUiState.items.isEmpty() -> LcEmptyState(
            title = stringResource(R.string.empty_list),
            icon = MiuixIcons.Glass.Info,
            message = activeQuery
          )

          else -> WideContentBox { sidePadding ->
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
              items(items = searchUiState.items, key = { it.packageName }) { item ->
                AppListRow(
                  item = item,
                  viewState = searchUiState.viewStates[item.packageName],
                  packageInfo = searchUiState.iconPackageInfos[item.packageName],
                  chips = searchUiState.chips[item.packageName].orEmpty(),
                  highlight = searchUiState.query.ifEmpty { activeQuery },
                  onClick = {
                    if (item.abi.toInt() == Constants.OVERLAY) {
                      overlayItem = item
                    } else {
                      onOpenDetail(item)
                    }
                  }
                )
              }
              item(key = "app_list_search_bottom_spacer") {
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

  appListMenuState.Sheets()
}

@Composable
private fun AppListPermissionState(onRequest: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .clickable(onClick = onRequest)
      .padding(horizontal = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = MiuixIcons.Blocklist,
      contentDescription = null,
      tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
      modifier = Modifier.size(96.dp)
    )
    Spacer(Modifier.height(20.dp))
    Text(
      text = stringResource(R.string.get_app_list_denied_tip),
      style = MiuixTheme.textStyles.title4,
      color = MiuixTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center
    )
  }
}

@Composable
private fun rememberAppTitle(context: Context): String {
  return remember(context) {
    runCatching {
      LCAppUtils.buildAppTitle(context).toString()
    }.getOrDefault(context.getString(R.string.app_name))
  }
}

private suspend fun prewarmVisibleAppIcons(
  context: Context,
  items: List<LCItem>,
  iconPackageInfos: Map<String, PackageInfo>,
  iconSizePx: Int
) {
  val packages = items.asSequence()
    .mapNotNull { iconPackageInfos[it.packageName] }
    .take(PREWARM_APP_ICON_COUNT)
    .toList()
  if (packages.isEmpty()) return
  val imageLoader = Coil.imageLoader(context)
  coroutineScope {
    packages.map { packageInfo ->
      async {
        runCatching {
          imageLoader.execute(
            buildAppIconImageRequest(
              context = context,
              packageInfo = packageInfo,
              iconSizePx = iconSizePx
            )
          )
        }
      }
    }.awaitAll()
  }
}

private const val PREWARM_APP_ICON_COUNT = 12
