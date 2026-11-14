package com.absinthe.libchecker.domain.app.list.ui.compose

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Process
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import top.yukonga.miuix.kmp.squircle.squircleBackground
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.STATUS_INIT_END
import com.absinthe.libchecker.annotation.STATUS_NOT_START
import com.absinthe.libchecker.annotation.STATUS_START_INIT
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.detail.ui.compose.OverlayDetailSheet
import com.absinthe.libchecker.domain.app.list.model.AppListItemViewState
import com.absinthe.libchecker.domain.home.presentation.HomeViewModel
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.LcEmptyState
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.showToast
import jonathanfinerty.once.Once
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import androidx.compose.ui.res.painterResource
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Blocklist
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

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
  recentVisitsViewModel: com.absinthe.libchecker.domain.home.presentation.RecentVisitsViewModel? = null,
  onOpenRecentApp: ((String) -> Unit)? = null,
  onOpenRecentLibrary: ((com.absinthe.libchecker.domain.home.recent.RecentVisit) -> Unit)? = null,
  onOpenDetail: (LCItem) -> Unit,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState()
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current

  val items by homeViewModel.displayItemsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
  var viewStates by remember { mutableStateOf<Map<String, AppListItemViewState>>(emptyMap()) }
  var iconPackageInfos by remember { mutableStateOf<Map<String, PackageInfo>>(emptyMap()) }
  var chips by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
  var highlight by remember { mutableStateOf("") }
  var status by remember { mutableIntStateOf(STATUS_NOT_START) }
  var overlayItem by remember { mutableStateOf<LCItem?>(null) }
  var loading by remember { mutableStateOf(true) }
  var initProgress by remember { mutableIntStateOf(0) }
  var reject by remember { mutableStateOf(false) }
  var displayOptions by remember { mutableIntStateOf(homeViewModel.getAppListAdvancedMenuState().displayOptions) }
  var searchExpanded by rememberSaveable { mutableStateOf(false) }
  var query by rememberSaveable { mutableStateOf("") }
  var refreshKey by remember { mutableIntStateOf(0) }
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
      reject = false
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

  BackHandler(enabled = searchExpanded) {
    searchExpanded = false
    focusManager.clearFocus()
  }

  LaunchedEffect(Unit) {
    homeViewModel.onAppListViewCreated()
    if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_LAUNCH)) {
      homeViewModel.initItems()
    }
  }

  LaunchedEffect(reselectSignal) {
    if (reselectSignal > 0) {
      listState.animateScrollToItem(0)
    }
  }

  var showRecentVisits by remember { mutableStateOf(false) }

  if (showRecentVisits && recentVisitsViewModel != null) {
    com.absinthe.libchecker.domain.home.ui.compose.RecentVisitsSheet(
      show = true,
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

  LaunchedEffect(Unit) {
    homeViewModel.effect.collect { effect ->
      when (effect) {
        HomeViewModel.Effect.PackageListLoadFailed -> {
          loading = false
          isRefreshing = false
          context.showToast(R.string.package_list_load_failed)
        }

        is HomeViewModel.Effect.ReloadApps -> {
          Once.clearDone(OnceTag.FIRST_LAUNCH)
          homeViewModel.initItems()
        }

        is HomeViewModel.Effect.UpdateInitProgress -> initProgress = effect.progress

        is HomeViewModel.Effect.PackageChanged -> homeViewModel.requestChange(effect.packageChangeState)

        is HomeViewModel.Effect.UpdateAppListStatus -> {
          status = effect.status
          when (effect.status) {
            STATUS_START_INIT -> {
              loading = true
              reject = false
            }

            STATUS_INIT_END -> loading = false
            else -> Unit
          }
        }

        is HomeViewModel.Effect.RefreshList -> refreshKey++
      }
    }
  }

  LaunchedEffect(toolbarLoading) {
    if (!toolbarLoading) {
      isRefreshing = false
    }
  }

  LaunchedEffect(items, refreshKey) {
    if (items.isEmpty() && status == STATUS_NOT_START) return@LaunchedEffect
    val update = withContext(Dispatchers.IO) {
      homeViewModel.buildAppListUpdate(
        isCurrentProcess64Bit = Process.is64Bit(),
        currentItems = items,
        highlightRefresh = true
      )
    }
    when (update) {
      HomeViewModel.AppListUpdate.OnlySelf -> {
        if (homeViewModel.appListStatus == STATUS_NOT_START) {
          Once.clearDone(OnceTag.FIRST_LAUNCH)
          reject = true
          loading = false
        }
      }

      is HomeViewModel.AppListUpdate.Content -> {
        val content = update.plan.content
        reject = false
        iconPackageInfos = content.renderState.iconPackageInfos
        chips = content.renderState.itemChips
        highlight = content.renderState.highlightText
        viewStates = content.renderState.itemViewStates
        loading = false
        val remaining = content.items.drop(content.renderState.itemViewStates.size)
        if (remaining.isNotEmpty()) {
          val more = withContext(Dispatchers.IO) {
            homeViewModel.buildAppListItemViewStates(remaining, content.packageStateSnapshot)
          }
          viewStates = viewStates + more
        }
      }
    }
  }

  LaunchedEffect(homeViewModel.appListDisplayOptionsChanges) {
    homeViewModel.appListDisplayOptionsChanges.collect { refreshKey++ }
  }

  LaunchedEffect(query) {
    delay(160)
    val change = homeViewModel.onAppListSearchQueryChanged(query)
    if (change.shouldRefreshItems) {
      highlight = query
      refreshKey++
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

  Column(modifier = modifier.fillMaxSize()) {
    AdaptiveTopAppBar(
      title = title,
      scrollBehavior = scrollBehavior,
      actions = {
        if (recentVisitsViewModel != null) {
          IconButton(onClick = { showRecentVisits = true }) {
            Icon(
              painter = painterResource(R.drawable.ic_time),
              contentDescription = stringResource(R.string.recent_apps)
            )
          }
        }
        AppListMenu(
          displayOptions = displayOptions,
          onDisplayOptionsChange = ::updateDisplayOptions
        ) {
          Icon(
            imageVector = MiuixIcons.Tune,
            contentDescription = stringResource(R.string.menu_filter)
          )
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
          AnimatedVisibility(
            visible = items.isEmpty() && loading,
            enter = fadeIn(),
            exit = fadeOut()
          ) {
            LinearProgressIndicator(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
              progress = if (initProgress in 1..99) initProgress / 100f else 0f,
              height = 3.dp
            )
          }
        }
      }
    )

    Box(
      Modifier
        .weight(1f)
        .imePadding()
    ) {
      when {
        reject -> AppListPermissionState(
          onRequest = { runCatching { permissionLauncher.launch(Constants.GET_INSTALLED_APPS) } }
        )

        items.isEmpty() && loading -> LcLoadingState(message = stringResource(R.string.loading))

        items.isEmpty() -> LcEmptyState(
          title = stringResource(R.string.empty_list),
          message = if (query.isNotEmpty()) stringResource(R.string.empty_list) else null
        )

        else -> PullToRefresh(
          isRefreshing = isRefreshing,
          onRefresh = {
            isRefreshing = true
            homeViewModel.requestChange(showLoadingFeedback = true)
          },
          pullToRefreshState = pullToRefreshState,
          topAppBarScrollBehavior = scrollBehavior,
          modifier = Modifier.fillMaxSize()
        ) {
          WideContentBox { sidePadding ->
            FastScrollRail(
              items = items,
              listState = listState,
              modifier = Modifier
                .align(Alignment.CenterEnd)
                .zIndex(1f)
                .padding(end = 2.dp, top = 60.dp, bottom = 60.dp)
            )
            LazyColumn(
              state = listState,
              modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
              contentPadding = PaddingValues(
                top = 6.dp,
                start = sidePadding,
                end = sidePadding
              )
            ) {
              items(items = items, key = { it.packageName }) { item ->
                AppListRow(
                  item = item,
                  viewState = viewStates[item.packageName],
                  packageInfo = iconPackageInfos[item.packageName],
                  chips = chips[item.packageName].orEmpty(),
                  highlight = highlight,
                  onClick = {
                if (item.abi.toInt() == com.absinthe.libchecker.constant.Constants.OVERLAY) {
                  overlayItem = item
                } else {
                  onOpenDetail(item)
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

@Composable
private fun FastScrollRail(
  items: List<LCItem>,
  listState: LazyListState,
  modifier: Modifier = Modifier
) {
  val letters = remember(items) {
    items.mapIndexedNotNull { index, item ->
      item.label.firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' }?.let { it to index }
    }.distinctBy { it.first }
  }
  if (letters.size < 6) {
    return
  }
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current
  var showBubble by remember { mutableStateOf<Char?>(null) }

  fun letterAt(y: Float, height: Int): ByteLetter? {
    if (height <= 0) return null
    val index = ((y / height) * letters.size).toInt().coerceIn(0, letters.lastIndex)
    return letters[index]
  }

  Box(
    modifier = modifier
      .pointerInput(letters) {
        detectVerticalDragGestures(
          onDragStart = { offset ->
            letterAt(offset.y, size.height)?.let { letter ->
              showBubble = letter.first
              scope.launch { listState.scrollToItem(letter.second) }
            }
          },
          onDragEnd = { showBubble = null },
          onDragCancel = { showBubble = null }
        ) { change, _ ->
          change.consume()
          letterAt(change.position.y, size.height)?.let { letter ->
            showBubble = letter.first
            scope.launch { listState.scrollToItem(letter.second) }
          }
        }
      }
      .pointerInput(letters) {
        detectTapGestures { offset ->
          letterAt(offset.y, size.height)?.let { letter ->
            scope.launch { listState.scrollToItem(letter.second) }
          }
        }
      },
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      letters.forEach { letter ->
        Text(
          text = letter.first.toString(),
          fontSize = 9.sp,
          fontWeight = FontWeight(600),
          color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
      }
    }
    showBubble?.let { letter ->
      Box(
        modifier = Modifier
          .padding(end = 24.dp)
          .squircleBackground(MiuixTheme.colorScheme.surfaceContainerHigh, 12.dp)
          .padding(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Text(
          text = letter.toString(),
          fontSize = 16.sp,
          fontWeight = FontWeight(600),
          color = MiuixTheme.colorScheme.onSurface
        )
      }
    }
  }
}

private typealias ByteLetter = Pair<Char, Int>