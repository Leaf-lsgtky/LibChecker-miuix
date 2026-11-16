package com.absinthe.libchecker.domain.home.presentation

import android.content.pm.PackageInfo
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.annotation.STATUS_INIT_END
import com.absinthe.libchecker.annotation.STATUS_NOT_START
import com.absinthe.libchecker.annotation.STATUS_START_INIT
import com.absinthe.libchecker.annotation.STATUS_START_REQUEST_CHANGE
import com.absinthe.libchecker.annotation.STATUS_START_REQUEST_CHANGE_END
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.list.export.ExportAppListToUriUseCase
import com.absinthe.libchecker.domain.app.list.export.ExportAppListUseCase
import com.absinthe.libchecker.domain.app.list.model.AppListItemViewState
import com.absinthe.libchecker.domain.app.list.usecase.BuildAppListItemViewStatesUseCase
import com.absinthe.libchecker.domain.app.list.usecase.BuildAppListUpdatePlanUseCase
import com.absinthe.libchecker.domain.app.list.usecase.GetAppListContentUseCase
import com.absinthe.libchecker.domain.app.list.usecase.GetAppListPackageStatesUseCase
import com.absinthe.libchecker.domain.app.list.usecase.InitializeAppListUseCase
import com.absinthe.libchecker.domain.app.list.usecase.ObserveAppListLoadingUseCase
import com.absinthe.libchecker.domain.app.maintenance.ClearApkCacheUseCase
import com.absinthe.libchecker.domain.app.model.PackageChangeState
import com.absinthe.libchecker.domain.app.repository.AppListRepository
import com.absinthe.libchecker.domain.app.repository.AppListSettingsRepository
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.app.repository.PackageListLoadException
import com.absinthe.libchecker.domain.app.search.HandleAppListSearchCommandUseCase
import com.absinthe.libchecker.domain.app.sync.AppListChangeRequestQueue
import com.absinthe.libchecker.domain.app.sync.SyncAppListChangesUseCase
import com.absinthe.libchecker.services.IWorkerService
import com.absinthe.libchecker.utils.harmony.HarmonyOsUtil
import jonathanfinerty.once.Once
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

class HomeViewModel(
  private val installedAppRepository: InstalledAppRepository,
  private val appListRepository: AppListRepository,
  private val initializeAppListUseCase: InitializeAppListUseCase,
  private val syncAppListChangesUseCase: SyncAppListChangesUseCase,
  private val exportAppListToUriUseCase: ExportAppListToUriUseCase,
  private val getAppListContentUseCase: GetAppListContentUseCase,
  private val buildAppListItemViewStatesUseCase: BuildAppListItemViewStatesUseCase,
  private val buildAppListUpdatePlanUseCase: BuildAppListUpdatePlanUseCase,
  private val handleAppListSearchCommandUseCase: HandleAppListSearchCommandUseCase,
  private val appListSettingsRepository: AppListSettingsRepository,
  private val clearApkCacheUseCase: ClearApkCacheUseCase,
  observeAppListLoadingUseCase: ObserveAppListLoadingUseCase
) : ViewModel() {

  val dbItemsFlow: Flow<List<LCItem>> = appListRepository.items
  val displayItemsFlow: Flow<List<LCItem>> =
    appListRepository.items.distinctUntilChanged(::areAppListItemsEquivalent)
  val packageChanges = installedAppRepository.packageChanges
  val appListDisplayOptionsChanges = appListSettingsRepository.displayOptionsChanges

  private val _effect: MutableSharedFlow<Effect> = MutableSharedFlow()
  val effect = _effect.asSharedFlow()

  private val _isRequestChangeRunning = MutableStateFlow(false)
  private val isRequestChangeRunning = _isRequestChangeRunning.asStateFlow()
  val toolbarLoading: Flow<Boolean> = observeAppListLoadingUseCase(isRequestChangeRunning, dbItemsFlow)

  private val _appListUiState = MutableStateFlow(AppListUiState())
  val appListUiState: StateFlow<AppListUiState> = _appListUiState.asStateFlow()
  private val _appListSearchUiState = MutableStateFlow(AppListSearchUiState())
  val appListSearchUiState: StateFlow<AppListSearchUiState> = _appListSearchUiState.asStateFlow()

  var appListStatus: Int = STATUS_NOT_START

  private var toolbarSearchMenuState = ToolbarSearchMenuState()
  private var pendingPackagesPermissionCheck = false
  private var pendingReturnTopAfterRequestChange = false
  private var hasUserScrolledAppList = false
  private var appListSearchKeyword: String = ""
  private var workerBinder: IWorkerService? = null
  private var pendingFeatureInitializationRequest = false
  private var hasStartedAppListObservers = false
  private var hasCommittedInitialContent = false
  private var isFirstRequestChange = true
  private var currentIs64Bit = true
  private var onBeforeFirstContent: (suspend (List<LCItem>, Map<String, PackageInfo>) -> Unit)? = null
  private var refreshAppListJob: Job? = null
  private var remainingViewStatesJob: Job? = null
  private var appListSearchJob: Job? = null
  private var appListSearchGeneration = 0
  private var appListRefreshGeneration = 0

  private val appListChangeRequestQueue = AppListChangeRequestQueue()

  fun reloadApps() {
    if (appListStatus != STATUS_NOT_START || (initJob?.isActive == false && requestChangeJob?.isActive == false)) {
      Timber.d("reloadApps: ignore, appListStatus: $appListStatus")
      return
    }
    Once.clearDone(OnceTag.FIRST_LAUNCH)
    initItems()
    setEffect {
      Effect.ReloadApps()
    }
  }

  private fun refreshList() {
    pendingReturnTopAfterRequestChange = true
    scheduleAppListRefresh(
      highlightRefresh = false,
      shouldReturnTop = false,
      forceImmediate = true
    )
    setEffect {
      Effect.RefreshList()
    }
  }

  fun packageChanged(packageChangeState: PackageChangeState) {
    requestChange(packageChangeState)
    setEffect {
      Effect.PackageChanged(packageChangeState)
    }
  }

  fun connectWorkerBinder(binder: IWorkerService) {
    workerBinder = binder
    if (pendingFeatureInitializationRequest) {
      requestFeatureInitialization()
    }
  }

  fun disconnectWorkerBinder() {
    workerBinder = null
  }

  fun requestFeatureInitialization() {
    val activeBinder = workerBinder ?: run {
      pendingFeatureInitializationRequest = true
      return
    }
    if (appListStatus == STATUS_START_INIT || appListStatus == STATUS_START_REQUEST_CHANGE) {
      pendingFeatureInitializationRequest = true
      return
    }
    pendingFeatureInitializationRequest = false
    runCatching {
      activeBinder.initFeatures()
    }.onFailure {
      Timber.w(it, "requestFeatureInitialization failed")
    }
  }

  private fun updateInitProgress(progress: Int) {
    _appListUiState.update { it.copy(initProgress = progress) }
    setEffect {
      Effect.UpdateInitProgress(progress)
    }
  }

  private fun updateAppListStatus(status: Int) {
    setEffect {
      Effect.UpdateAppListStatus(status)
    }
    appListStatus = status
  }

  private fun setEffect(builder: () -> Effect) {
    val newEffect = builder()
    viewModelScope.launch {
      _effect.emit(newEffect)
    }
  }

  suspend fun buildAppListUpdate(
    isCurrentProcess64Bit: Boolean,
    currentItems: List<LCItem>,
    highlightRefresh: Boolean,
    initialItemViewStateCount: Int? = null
  ): AppListUpdate {
    val content = getAppListContentUseCase(
      if (initialItemViewStateCount != null) {
        GetAppListContentUseCase.Request(
          keyword = "",
          isCurrentProcess64Bit = isCurrentProcess64Bit,
          initialItemViewStateCount = initialItemViewStateCount
        )
      } else {
        GetAppListContentUseCase.Request(
          keyword = "",
          isCurrentProcess64Bit = isCurrentProcess64Bit
        )
      }
    )
    return when (content) {
      GetAppListContentUseCase.Result.OnlySelf -> AppListUpdate.OnlySelf

      is GetAppListContentUseCase.Result.Content -> {
        AppListUpdate.Content(
          buildAppListUpdatePlanUseCase(
            BuildAppListUpdatePlanUseCase.Request(
              currentItems = currentItems,
              content = content,
              pendingReturnTopAfterRequestChange = pendingReturnTopAfterRequestChange,
              highlightRefresh = highlightRefresh,
              hasUserScrolledList = hasUserScrolledAppList
            )
          )
        )
      }
    }
  }

  suspend fun isOnlySelfAppInDatabase(): Boolean {
    return getAppListContentUseCase.isOnlySelfAppInDatabase()
  }

  suspend fun buildAppListItemViewStates(
    items: List<LCItem>,
    packageStateSnapshot: GetAppListPackageStatesUseCase.PackageStateSnapshot? = null
  ): Map<String, AppListItemViewState> {
    return buildAppListItemViewStatesUseCase(
      BuildAppListItemViewStatesUseCase.Request(
        items = items,
        options = appListSettingsRepository.displayOptions,
        packageStateSnapshot = packageStateSnapshot
      )
    )
  }

  fun onAppListUserScrolled() {
    hasUserScrolledAppList = true
  }

  fun onAppListViewCreated() {
    pendingReturnTopAfterRequestChange = false
    hasUserScrolledAppList = false
  }

  fun onAppListUpdatePlanApplied(plan: BuildAppListUpdatePlanUseCase.Plan) {
    if (plan.shouldClearPendingReturnTopAfterRequestChange) {
      pendingReturnTopAfterRequestChange = false
    }
  }

  fun setPackagesPermissionCheckPending(pending: Boolean) {
    pendingPackagesPermissionCheck = pending
  }

  fun shouldCheckPackagesPermissionOnResume(): Boolean {
    return pendingPackagesPermissionCheck
  }

  fun onPackagesPermissionResult(isGranted: Boolean): Boolean {
    if (isGranted) {
      pendingPackagesPermissionCheck = false
    }
    return isGranted
  }

  fun saveToolbarSearchMenuState(isExpanded: Boolean, query: String) {
    toolbarSearchMenuState = ToolbarSearchMenuState(
      isExpanded = isExpanded,
      query = if (isExpanded) query else ""
    )
  }

  fun getToolbarSearchMenuState(): ToolbarSearchMenuState {
    return toolbarSearchMenuState
  }

  fun getAppListAdvancedMenuState(): AppListAdvancedMenuState {
    return AppListAdvancedMenuState(
      displayOptions = appListSettingsRepository.displayOptions,
      itemDisplayOptions = appListSettingsRepository.itemDisplayOptions,
      colorfulRuleIcon = appListSettingsRepository.colorfulRuleIcon
    )
  }

  fun setAppListDisplayOptions(options: Int): Int {
    appListSettingsRepository.setDisplayOptions(options)
    return options
  }

  fun setAppListItemDisplayOptions(options: Int): Int {
    appListSettingsRepository.setItemDisplayOptions(options)
    return options
  }

  fun onAppListAdvancedMenuDismissed(
    displayOptionsDiff: Int,
    itemDisplayOptionsDiff: Int
  ): AppListAdvancedMenuDismissPlan {
    if (displayOptionsDiff > 0) {
      viewModelScope.launch {
        appListSettingsRepository.notifyDisplayOptionsChanged(displayOptionsDiff)
      }
    }
    return AppListAdvancedMenuDismissPlan(
      shouldRefreshItems = displayOptionsDiff > 0 || itemDisplayOptionsDiff > 0
    )
  }

  fun ensureAppListStarted(
    isCurrentProcess64Bit: Boolean,
    onBeforeFirstContent: suspend (List<LCItem>, Map<String, PackageInfo>) -> Unit
  ) {
    currentIs64Bit = isCurrentProcess64Bit
    this.onBeforeFirstContent = onBeforeFirstContent
    if (hasStartedAppListObservers) {
      return
    }
    hasStartedAppListObservers = true
    startAppListObservers()

    val isFirstLaunch = !Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_LAUNCH)
    val needsHarmonyInit = HarmonyOsUtil.isHarmonyOs() &&
      !Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.HARMONY_FIRST_INIT)
    val needsReloadInit = !isFirstLaunch &&
      !Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.SHOULD_RELOAD_APP_LIST)
    if (isFirstLaunch || needsHarmonyInit || needsReloadInit) {
      if (needsHarmonyInit) {
        Once.markDone(OnceTag.HARMONY_FIRST_INIT)
      }
      if (needsReloadInit) {
        Once.markDone(OnceTag.SHOULD_RELOAD_APP_LIST)
      }
      initItems()
    }
  }

  private fun startAppListObservers() {
    displayItemsFlow.onEach { dbItems ->
      if (dbItems.isEmpty()) {
        if (appListStatus != STATUS_START_INIT && initJob?.isActive != true) {
          initItems()
        }
      } else if (
        appListStatus != STATUS_START_INIT &&
        appListStatus != STATUS_START_REQUEST_CHANGE
      ) {
        scheduleAppListRefresh(highlightRefresh = false, shouldReturnTop = false)
        if (isFirstRequestChange) {
          isFirstRequestChange = false
          requestChange()
        }
      }
    }.launchIn(viewModelScope)

    appListDisplayOptionsChanges.onEach { diff ->
      if (appListStatus != STATUS_START_INIT) {
        scheduleAppListRefresh(
          highlightRefresh = false,
          shouldReturnTop = (diff and APP_LIST_FILTER_AND_SORT_OPTIONS) != 0
        )
        if (appListSearchKeyword.isNotEmpty()) {
          refreshAppListSearch(appListSearchKeyword)
        }
      }
    }.launchIn(viewModelScope)
  }

  fun refreshAppListAfterSearch() {
    refreshAppListSearch(appListSearchKeyword)
  }

  private fun refreshAppListSearch(keyword: String) {
    val generation = ++appListSearchGeneration
    appListSearchJob?.cancel()
    if (keyword.isEmpty()) {
      _appListSearchUiState.value = AppListSearchUiState()
      return
    }
    _appListSearchUiState.update { current ->
      current.copy(isLoading = current.query != keyword && current.items.isEmpty(), query = keyword)
    }
    appListSearchJob = viewModelScope.launch(Dispatchers.IO) {
      val result = getAppListContentUseCase(
        GetAppListContentUseCase.Request(
          keyword = keyword,
          isCurrentProcess64Bit = currentIs64Bit,
          initialItemViewStateCount = Int.MAX_VALUE
        )
      )
      if (!isActive || generation != appListSearchGeneration) return@launch
      when (result) {
        GetAppListContentUseCase.Result.OnlySelf -> {
          _appListSearchUiState.value = AppListSearchUiState(
            isLoading = false,
            query = keyword
          )
        }

        is GetAppListContentUseCase.Result.Content -> {
          _appListSearchUiState.value = AppListSearchUiState(
            isLoading = false,
            query = keyword,
            items = result.items,
            iconPackageInfos = result.renderState.iconPackageInfos,
            viewStates = result.renderState.itemViewStates,
            chips = result.renderState.itemChips
          )
        }
      }
    }
  }

  private fun scheduleAppListRefresh(
    highlightRefresh: Boolean,
    shouldReturnTop: Boolean,
    forceImmediate: Boolean = false
  ) {
    val generation = ++appListRefreshGeneration
    val hasReadyContent = _appListUiState.value.mode == AppListScreenMode.Content ||
      _appListUiState.value.mode == AppListScreenMode.Empty
    val shouldDebounce = !forceImmediate && (highlightRefresh || hasReadyContent)
    remainingViewStatesJob?.cancel()
    refreshAppListJob?.cancel()
    refreshAppListJob = viewModelScope.launch(Dispatchers.IO) {
      if (shouldDebounce) {
        delay(APP_LIST_REFRESH_DEBOUNCE_MS)
      }
      if (!isActive || generation != appListRefreshGeneration || appListStatus == STATUS_START_INIT) {
        return@launch
      }

      val currentState = _appListUiState.value
      val update = buildAppListUpdate(
        isCurrentProcess64Bit = currentIs64Bit,
        currentItems = currentState.items,
        highlightRefresh = highlightRefresh,
        initialItemViewStateCount = if (hasReadyContent) Int.MAX_VALUE else null
      )
      if (!isActive || generation != appListRefreshGeneration) {
        return@launch
      }

      when (update) {
        AppListUpdate.OnlySelf -> {
          if (appListStatus == STATUS_NOT_START) {
            Once.clearDone(OnceTag.FIRST_LAUNCH)
            _appListUiState.update { it.copy(mode = AppListScreenMode.Reject) }
          }
        }

        is AppListUpdate.Content -> {
          val plan = update.plan
          val content = plan.content
          onAppListUpdatePlanApplied(plan)

          if (content.items.isNotEmpty()) {
            val prewarm = onBeforeFirstContent
            if (prewarm != null) {
              val timeoutMs = if (hasCommittedInitialContent) {
                UPDATE_CONTENT_ICON_PREWARM_TIMEOUT_MS
              } else {
                FIRST_CONTENT_ICON_PREWARM_TIMEOUT_MS
              }
              withTimeoutOrNull(timeoutMs) {
                prewarm(content.items, content.renderState.iconPackageInfos)
              }
            }
            hasCommittedInitialContent = true
          }

          if (!isActive || generation != appListRefreshGeneration) {
            return@launch
          }

          val nextMode = if (content.items.isEmpty()) {
            AppListScreenMode.Empty
          } else {
            AppListScreenMode.Content
          }
          val shouldBumpScrollTop = shouldReturnTop || plan.shouldReturnTopAfterRequestChange

          _appListUiState.update { current ->
            val unchangedContent = current.mode == nextMode &&
              current.items == content.items &&
              current.viewStates == content.renderState.itemViewStates &&
              current.chips == content.renderState.itemChips &&
              current.highlightText == content.renderState.highlightText &&
              !shouldBumpScrollTop
            if (unchangedContent) {
              current
            } else {
              current.copy(
                mode = nextMode,
                items = content.items,
                iconPackageInfos = content.renderState.iconPackageInfos,
                viewStates = content.renderState.itemViewStates,
                chips = content.renderState.itemChips,
                highlightText = content.renderState.highlightText,
                scrollToTopSignal = if (shouldBumpScrollTop) {
                  current.scrollToTopSignal + 1
                } else {
                  current.scrollToTopSignal
                }
              )
            }
          }
          if (appListSearchKeyword.isNotEmpty()) {
            refreshAppListSearch(appListSearchKeyword)
          }

          val initialCount = content.renderState.itemViewStates.size
          if (content.items.size > initialCount) {
            val remaining = content.items.drop(initialCount)
            remainingViewStatesJob = viewModelScope.launch(Dispatchers.IO) {
              val more = buildAppListItemViewStates(remaining, content.packageStateSnapshot)
              if (!isActive || generation != appListRefreshGeneration) {
                return@launch
              }
              _appListUiState.update { current ->
                if (current.items != content.items) {
                  current
                } else {
                  current.copy(viewStates = current.viewStates + more)
                }
              }
            }
          }
        }
      }
    }
  }

  private var initJob: Job? = null

  fun initItems() {
    if (initJob?.isActive == true) {
      return
    }
    remainingViewStatesJob?.cancel()
    refreshAppListJob?.cancel()
    initJob = viewModelScope.launch(Dispatchers.IO) {
      try {
        initItemsImpl()
      } finally {
        initJob = null
      }
    }
  }

  private suspend fun initItemsImpl() {
    updateAppListStatus(STATUS_START_INIT)
    _appListUiState.update { it.copy(mode = AppListScreenMode.Loading, initProgress = 0) }
    try {
      if (!initializeAppListUseCase(::updateInitProgress)) return
    } catch (e: PackageListLoadException) {
      Timber.w(e)
      updateAppListStatus(STATUS_NOT_START)
      _appListUiState.update { current ->
        current.copy(
          mode = if (current.items.isEmpty()) AppListScreenMode.Empty else AppListScreenMode.Content
        )
      }
      setEffect { Effect.PackageListLoadFailed }
      return
    }
    updateAppListStatus(STATUS_INIT_END)

    val isFirstLaunch = !Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_LAUNCH)
    if (isFirstLaunch) {
      if (isOnlySelfAppInDatabase()) {
        updateAppListStatus(STATUS_NOT_START)
        _appListUiState.update { it.copy(mode = AppListScreenMode.Reject) }
        return
      }
      Once.markDone(OnceTag.FIRST_LAUNCH)
      Once.markDone(OnceTag.SHOULD_RELOAD_APP_LIST)
    }

    updateAppListStatus(STATUS_NOT_START)
    isFirstRequestChange = false
    scheduleAppListRefresh(
      highlightRefresh = false,
      shouldReturnTop = false,
      forceImmediate = true
    )
    requestFeatureInitialization()
  }

  private var requestChangeJob: Job? = null

  fun requestChange(
    packageChangeState: PackageChangeState? = null,
    showLoadingFeedback: Boolean = false
  ) {
    viewModelScope.launch {
      if (appListStatus == STATUS_START_INIT) {
        Timber.d("Request change canceled: STATUS_START_INIT")
        return@launch
      }
      val changeRequest = appListChangeRequestQueue.start(packageChangeState)
      requestChangeJob?.cancel()
      requestChangeJob = requestChangeImpl(changeRequest, showLoadingFeedback)
    }
  }

  private fun requestChangeImpl(
    changeRequest: AppListChangeRequestQueue.ChangeRequest,
    showLoadingFeedback: Boolean
  ) = viewModelScope.launch(Dispatchers.IO) {
    val loadingStartedAt = System.currentTimeMillis()
    if (showLoadingFeedback) {
      _isRequestChangeRunning.value = true
    }
    val dbItems = appListRepository.getItems()
    if (dbItems.isEmpty()) {
      if (appListChangeRequestQueue.isCurrent(changeRequest)) {
        _isRequestChangeRunning.value = false
        updateAppListStatus(STATUS_NOT_START)
        requestChangeJob = null
      }
      return@launch
    }

    _isRequestChangeRunning.value = true
    updateAppListStatus(STATUS_START_REQUEST_CHANGE)

    try {
      val syncRequest = appListChangeRequestQueue.buildSyncRequest(changeRequest)

      val syncResult = syncAppListChangesUseCase(syncRequest, dbItems)
      if (syncResult == SyncAppListChangesUseCase.Result.Canceled) {
        return@launch
      }

      appListChangeRequestQueue.consumeSyncedRequest(changeRequest, syncRequest)

      if (syncResult == SyncAppListChangesUseCase.Result.Changed) {
        refreshList()
      }

      updateAppListStatus(STATUS_START_REQUEST_CHANGE_END)
    } catch (e: PackageListLoadException) {
      Timber.w(e)
      setEffect { Effect.PackageListLoadFailed }
    } finally {
      if (appListChangeRequestQueue.isCurrent(changeRequest)) {
        if (showLoadingFeedback) {
          delayManualRefreshLoadingFeedback(loadingStartedAt)
        }
        if (!appListChangeRequestQueue.isCurrent(changeRequest)) {
          return@launch
        }
        updateAppListStatus(STATUS_NOT_START)
        _isRequestChangeRunning.value = false
        requestChangeJob = null
        requestFeatureInitialization()
      }
    }
  }

  private suspend fun delayManualRefreshLoadingFeedback(startedAt: Long) {
    val remaining = MANUAL_REFRESH_LOADING_MIN_DURATION_MS - (System.currentTimeMillis() - startedAt)
    if (remaining > 0L) {
      delay(remaining)
    }
  }

  private var clearApkCacheJob: Job? = null

  fun clearApkCache() {
    clearApkCacheJob?.cancel()
    clearApkCacheJob = viewModelScope.launch {
      clearApkCacheUseCase()
    }
  }

  fun onAppListSearchQueryChanged(query: String): AppListSearchQueryChange {
    if (appListSearchKeyword == query) {
      return AppListSearchQueryChange(
        shouldRefreshItems = false,
        action = AppListSearchCommandAction.None
      )
    }
    appListSearchKeyword = query
    return AppListSearchQueryChange(
      shouldRefreshItems = true,
      action = handleAppListSearchQuery(query)
    )
  }

  fun getAppListSearchQuery(): String {
    return appListSearchKeyword
  }

  private fun handleAppListSearchQuery(query: String): AppListSearchCommandAction {
    return when (val result = handleAppListSearchCommandUseCase(query)) {
      HandleAppListSearchCommandUseCase.Result.None -> AppListSearchCommandAction.None

      HandleAppListSearchCommandUseCase.Result.EasterEgg -> AppListSearchCommandAction.EasterEgg

      HandleAppListSearchCommandUseCase.Result.DebugModeEnabled -> AppListSearchCommandAction.DebugModeEnabled

      HandleAppListSearchCommandUseCase.Result.UserModeEnabled -> AppListSearchCommandAction.UserModeEnabled

      is HandleAppListSearchCommandUseCase.Result.DumpAppsInfo -> {
        AppListSearchCommandAction.DumpAppsInfo(
          fileName = result.fileName,
          format = result.format
        )
      }
    }
  }

  sealed class Effect {
    data object PackageListLoadFailed : Effect()
    data class ReloadApps(val obj: Any? = null) : Effect()
    data class UpdateInitProgress(val progress: Int) : Effect()
    data class UpdateAppListStatus(val status: Int) : Effect()
    data class PackageChanged(val packageChangeState: PackageChangeState) : Effect()
    data class RefreshList(val obj: Any? = null) : Effect()
  }

  data class AppListAdvancedMenuState(
    val displayOptions: Int,
    val itemDisplayOptions: Int,
    val colorfulRuleIcon: Boolean
  )

  data class AppListAdvancedMenuDismissPlan(
    val shouldRefreshItems: Boolean
  )

  data class ToolbarSearchMenuState(
    val isExpanded: Boolean = false,
    val query: String = ""
  )

  data class AppListSearchQueryChange(
    val shouldRefreshItems: Boolean,
    val action: AppListSearchCommandAction
  )

  sealed interface AppListUpdate {
    data object OnlySelf : AppListUpdate
    data class Content(val plan: BuildAppListUpdatePlanUseCase.Plan) : AppListUpdate
  }

  sealed interface AppListSearchCommandAction {
    data object None : AppListSearchCommandAction
    data object EasterEgg : AppListSearchCommandAction
    data object DebugModeEnabled : AppListSearchCommandAction
    data object UserModeEnabled : AppListSearchCommandAction
    data class DumpAppsInfo(
      val fileName: String,
      val format: ExportAppListUseCase.Format
    ) : AppListSearchCommandAction
  }

  enum class AppListScreenMode {
    Loading,
    Reject,
    Empty,
    Content
  }

  data class AppListUiState(
    val mode: AppListScreenMode = AppListScreenMode.Loading,
    val items: List<LCItem> = emptyList(),
    val iconPackageInfos: Map<String, PackageInfo> = emptyMap(),
    val viewStates: Map<String, AppListItemViewState> = emptyMap(),
    val chips: Map<String, List<String>> = emptyMap(),
    val highlightText: String = "",
    val initProgress: Int = 0,
    val scrollToTopSignal: Int = 0
  )

  data class AppListSearchUiState(
    val isLoading: Boolean = false,
    val query: String = "",
    val items: List<LCItem> = emptyList(),
    val iconPackageInfos: Map<String, PackageInfo> = emptyMap(),
    val viewStates: Map<String, AppListItemViewState> = emptyMap(),
    val chips: Map<String, List<String>> = emptyMap()
  )

  fun dumpAppsInfo(
    uri: Uri,
    format: ExportAppListUseCase.Format
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      runCatching {
        exportAppListToUriUseCase(uri, format)
      }.onFailure {
        Timber.e(it)
      }
    }
  }

  fun clearMenuState() {
    toolbarSearchMenuState = ToolbarSearchMenuState()
  }
}

private fun areAppListItemsEquivalent(old: List<LCItem>, new: List<LCItem>): Boolean {
  return old.size == new.size && old.zip(new).all { (oldItem, newItem) ->
    oldItem.packageName == newItem.packageName &&
      oldItem.label == newItem.label &&
      oldItem.versionName == newItem.versionName &&
      oldItem.versionCode == newItem.versionCode &&
      oldItem.lastUpdatedTime == newItem.lastUpdatedTime &&
      oldItem.isSystem == newItem.isSystem &&
      oldItem.abi == newItem.abi &&
      oldItem.targetApi == newItem.targetApi &&
      oldItem.variant == newItem.variant
  }
}

private const val MANUAL_REFRESH_LOADING_MIN_DURATION_MS = 450L
private const val APP_LIST_REFRESH_DEBOUNCE_MS = 120L
private const val FIRST_CONTENT_ICON_PREWARM_TIMEOUT_MS = 250L
private const val UPDATE_CONTENT_ICON_PREWARM_TIMEOUT_MS = 120L
private const val APP_LIST_FILTER_AND_SORT_OPTIONS =
  AdvancedOptions.SHOW_SYSTEM_APPS or
    AdvancedOptions.SHOW_SYSTEM_FRAMEWORK_APPS or
    AdvancedOptions.SHOW_OVERLAYS or
    AdvancedOptions.SHOW_64_BIT_APPS or
    AdvancedOptions.SHOW_32_BIT_APPS or
    AdvancedOptions.SORT_BY_NAME or
    AdvancedOptions.SORT_BY_UPDATE_TIME or
    AdvancedOptions.SORT_BY_TARGET_API
