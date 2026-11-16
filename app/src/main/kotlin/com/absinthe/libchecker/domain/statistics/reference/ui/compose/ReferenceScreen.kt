package com.absinthe.libchecker.domain.statistics.reference.ui.compose

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceLoadingState
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceSearchLabels
import com.absinthe.libchecker.domain.statistics.reference.model.resolveReferenceIcon
import com.absinthe.libchecker.domain.statistics.reference.presentation.LibReferenceViewModel
import com.absinthe.libchecker.ui.compose.component.AdaptiveGlassOverflowMenu
import com.absinthe.libchecker.ui.compose.component.AdaptiveSearchOverlay
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopBarIconButton
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopBarMenuAction
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopSearchButton
import com.absinthe.libchecker.ui.compose.component.CoverFlowReferenceLoading
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
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.Menu
import top.yukonga.miuix.kmp.menu.OverlayIconCascadingDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.absinthe.libchecker.ui.compose.component.FastScrollLazyColumn as LazyColumn

/**
 * Statistics destination: one card per referenced library, newest computation first.
 *
 * Computation, filtering and search still run in [LibReferenceViewModel]; this screen only renders
 * its state and forwards the menu/search interactions.
 */
@Composable
fun ReferenceScreen(
  viewModel: LibReferenceViewModel,
  onOpenReference: (LibReference) -> Unit,
  onOpenChart: () -> Unit = {},
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState(),
  isCurrentPage: Boolean = true,
  searchState: TopBarSearchState = rememberTopBarSearchState(),
  showSearchButton: Boolean = true
) {
  val references by viewModel.libReference.collectAsStateWithLifecycle()
  val loadingState by viewModel.loadingState.collectAsStateWithLifecycle()
  val scrollBehavior = MiuixScrollBehavior()
  var isRefreshing by remember { mutableStateOf(false) }
  val pullToRefreshState = rememberPullToRefreshState()

  val query = searchState.query
  var searchResult by remember { mutableStateOf<List<LibReference>?>(null) }
  var settledSearchQuery by remember { mutableStateOf<String?>(null) }
  var options by remember { mutableIntStateOf(viewModel.getLibReferenceOptions()) }
  var easterEgg by remember { mutableStateOf(false) }
  var treemapEnabled by remember { mutableStateOf(viewModel.treemapEnabled) }

  var hasActivated by rememberSaveable { mutableStateOf(isCurrentPage) }
  LaunchedEffect(isCurrentPage) {
    if (isCurrentPage) {
      hasActivated = true
    }
  }

  LaunchedEffect(hasActivated) {
    if (!hasActivated) return@LaunchedEffect
    viewModel.onReferencePageVisible(hasDisplayedReferences = references != null)
  }

  val searchLabels = LibReferenceSearchLabels(
    notMarkedLabel = stringResource(R.string.not_marked_lib),
    permissionFallbackLabel = stringResource(R.string.ref_category_perm),
    metadataLabel = stringResource(R.string.ref_category_metadata),
    packageLabel = stringResource(R.string.ref_category_package)
  )

  LaunchedEffect(query, hasActivated, references) {
    if (!hasActivated) return@LaunchedEffect
    if (references == null) {
      searchResult = null
      settledSearchQuery = null
      return@LaunchedEffect
    }
    if (query.isNotEmpty()) {
      delay(160)
    }
    viewModel.onSearchQueryChanged(query, searchLabels)
    val result = viewModel.buildCurrentSearchResult()
    searchResult = result?.references
    easterEgg = result?.shouldShowEasterEgg == true
    settledSearchQuery = query
  }

  val displayed = references.orEmpty()
  val isLoading = references == null
  LaunchedEffect(isLoading) {
    if (!isLoading) isRefreshing = false
  }

  var showFilterMenu by remember { mutableStateOf(false) }
  val homeBottomPadding = LocalHomeBottomPadding.current
  val isScrolled = if (treemapEnabled) false else listState.canScrollBackward

  MiuixPageHost(
    modifier = modifier.fillMaxSize(),
    enableMenuBackdrop = false,
    overlay = {
      ReferenceMenuSheet(
        show = showFilterMenu,
        viewModel = viewModel,
        treemapEnabled = treemapEnabled,
        onToggleTreemap = {
          treemapEnabled = !treemapEnabled
          viewModel.treemapEnabled = treemapEnabled
        },
        onDismiss = { optionsChanged ->
          showFilterMenu = false
          if (optionsChanged) {
            viewModel.requestComputeReference(isVisible = true, needShowLoading = true)
          }
        }
      )
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
            title = stringResource(R.string.title_statistics),
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
              AdaptiveTopBarIconButton(onClick = onOpenChart) {
                AsyncImage(
                  model = R.drawable.ic_chart,
                  contentDescription = stringResource(R.string.tab_chart),
                  colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
                  modifier = Modifier.size(24.dp)
                )
              }
              AdaptiveTopBarIconButton(onClick = { showFilterMenu = true }) {
                Icon(
                  imageVector = MiuixIcons.Glass.Menu,
                  contentDescription = stringResource(R.string.menu_filter)
                )
              }
            },
            bottomContent = {
              if (isLoading && displayed.isNotEmpty()) {
                LinearProgressIndicator(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                  progress = loadingState.overallProgress?.takeIf { it > 0 }?.let { it / 100f },
                  height = 3.dp
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
          val displayMode = when {
            isLoading && displayed.isEmpty() -> ReferenceDisplayMode.Loading
            displayed.isEmpty() -> ReferenceDisplayMode.Empty
            treemapEnabled -> ReferenceDisplayMode.Treemap
            else -> ReferenceDisplayMode.List
          }
          Crossfade(
            targetState = displayMode,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            label = "ReferenceModeTransition"
          ) { mode ->
            when (mode) {
              ReferenceDisplayMode.Loading -> CoverFlowReferenceLoading(
                message = stringResource(R.string.loading),
                progress = loadingState.overallProgress?.takeIf { it > 0 }?.let { it / 100f },
                modifier = Modifier.padding(padding)
              )

              ReferenceDisplayMode.Empty -> LcEmptyState(
                title = stringResource(R.string.empty_list),
                modifier = Modifier.padding(padding)
              )

              ReferenceDisplayMode.Treemap -> Box(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(padding)
                  .let { if (homeBottomPadding > 0.dp) it.padding(bottom = homeBottomPadding) else it }
              ) {
                LibReferenceTreemapContent(
                  references = displayed,
                  colorfulRuleIcon = viewModel.colorfulRuleIcon,
                  aggregateSmallReferences = true,
                  onReferenceClick = onOpenReference
                )
              }

              ReferenceDisplayMode.List -> PullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = {
                  isRefreshing = true
                  viewModel.requestComputeReference(isVisible = true, needShowLoading = true)
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
                    items(items = displayed, key = { "${it.type}:${it.libName}" }) { reference ->
                      ReferenceRow(reference = reference, onClick = { onOpenReference(reference) })
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
        val results = searchResult.orEmpty()
        val isSearchLoading = (isLoading && displayed.isEmpty()) ||
          searchResult == null ||
          (settledSearchQuery != activeQuery && (activeQuery.isNotEmpty() || results.isEmpty()))
        LaunchedEffect(activeQuery) {
          searchListState.scrollToItem(0)
        }
        if (isSearchLoading) {
          CoverFlowReferenceLoading(
            message = stringResource(R.string.loading),
            progress = loadingState.overallProgress?.takeIf { it > 0 }?.let { it / 100f }
          )
        } else if (results.isEmpty()) {
          LcEmptyState(title = if (easterEgg) "🥚" else stringResource(R.string.empty_list))
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
              items(items = results, key = { "${it.type}:${it.libName}" }) { reference ->
                ReferenceRow(reference = reference, onClick = { onOpenReference(reference) })
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

private enum class ReferenceDisplayMode {
  Loading,
  Empty,
  Treemap,
  List
}

@Composable
internal fun ReferenceRow(
  reference: LibReference,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
    .fillMaxWidth()
    .padding(horizontal = 12.dp)
    .padding(bottom = 12.dp),
  labelSuffix: String? = null
) {
  val colorScheme = MiuixTheme.colorScheme
  val baseLabel = reference.rule?.label
    ?: reference.resolvedLabel
    ?: stringResource(R.string.not_marked_lib)
  val label = if (!labelSuffix.isNullOrBlank()) "$baseLabel $labelSuffix" else baseLabel
  val iconRes = resolveReferenceIcon(reference.libName, reference.type, reference.rule)
  val placeholder = remember { ColorPainter(colorScheme.surfaceContainerHigh) }
  Card(
    modifier = modifier,
    onClick = onClick,
    showIndication = true,
    insideMargin = PaddingValues(start = 10.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      AsyncImage(
        model = iconRes,
        contentDescription = null,
        placeholder = placeholder,
        error = placeholder,
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .padding(end = 12.dp)
          .size(42.dp)
          .clip(RoundedCornerShape(10.dp))
      )
      Column(Modifier.weight(1f)) {
        Text(
          text = label,
          style = MiuixTheme.textStyles.title4,
          color = colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = reference.libName,
          style = MiuixTheme.textStyles.footnote1,
          color = colorScheme.onSurfaceVariantSummary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Text(
        text = reference.referredList.size.toString(),
        style = MiuixTheme.textStyles.title3,
        color = colorScheme.onSurface,
        modifier = Modifier.padding(start = 12.dp)
      )
    }
  }
}
