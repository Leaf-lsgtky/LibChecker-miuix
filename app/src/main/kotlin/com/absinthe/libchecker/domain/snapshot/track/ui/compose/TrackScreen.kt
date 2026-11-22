package com.absinthe.libchecker.domain.snapshot.track.ui.compose

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.list.ui.compose.AppIcon
import com.absinthe.libchecker.domain.snapshot.track.model.TrackedAppListItem
import com.absinthe.libchecker.domain.snapshot.track.presentation.TrackViewModel
import com.absinthe.libchecker.ui.compose.component.AdaptiveBackButton
import com.absinthe.libchecker.ui.compose.component.AdaptiveSearchOverlay
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopSearchButton
import com.absinthe.libchecker.ui.compose.component.LcEmptyState
import com.absinthe.libchecker.ui.compose.component.MiuixPageHost
import com.absinthe.libchecker.ui.compose.component.TopBarSearchConfig
import com.absinthe.libchecker.ui.compose.component.TrackListSkeleton
import com.absinthe.libchecker.ui.compose.component.adaptiveVerticalScroll
import com.absinthe.libchecker.ui.compose.component.rememberTopBarSearchState
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.absinthe.libchecker.ui.compose.component.FastScrollLazyColumn as LazyColumn

/** Snapshot tracking list: every app with a switch that opts it into snapshot diffs. */
@Composable
fun TrackScreen(
  viewModel: TrackViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState()
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val scrollBehavior = MiuixScrollBehavior()
  val searchState = rememberTopBarSearchState()
  val query = searchState.query

  LaunchedEffect(Unit) {
    viewModel.loadTrackList()
  }

  LaunchedEffect(query) {
    viewModel.setQuery(query)
  }

  MiuixPageHost(modifier = modifier.fillMaxSize()) {
    val pageState = this
    Box(modifier = Modifier.fillMaxSize()) {
      Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
          AdaptiveTopAppBar(
            title = stringResource(R.string.album_item_track_title),
            scrollBehavior = scrollBehavior,
            search = TopBarSearchConfig(
              state = searchState,
              label = stringResource(R.string.search_hint)
            ),
            navigationIcon = {
              AdaptiveBackButton(onClick = onBack)
            },
            actions = {
              AdaptiveTopSearchButton(onClick = searchState::open)
            }
          )
        }
      ) { padding ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .then(pageState.topBarBackdropModifier)
            .imePadding()
        ) {
          val screenMode = when {
            uiState.loadFailed -> TrackScreenMode.Failed
            uiState.isLoading -> TrackScreenMode.Loading
            uiState.items.isEmpty() -> TrackScreenMode.Empty
            else -> TrackScreenMode.Content
          }
          Crossfade(
            targetState = screenMode,
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            label = "TrackScreenTransition"
          ) { mode ->
            when (mode) {
              TrackScreenMode.Failed -> Box(Modifier.fillMaxSize().padding(padding)) {
                LcEmptyState(
                  title = stringResource(R.string.package_list_load_failed),
                  actionLabel = stringResource(R.string.loading),
                  onAction = { viewModel.loadTrackList() }
                )
              }

              TrackScreenMode.Loading -> WideContentBox { sidePadding ->
                TrackListSkeleton(
                  contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 6.dp,
                    start = sidePadding,
                    end = sidePadding
                  )
                )
              }

              TrackScreenMode.Empty -> Box(Modifier.fillMaxSize().padding(padding)) {
                LcEmptyState(title = stringResource(R.string.empty_list))
              }

              TrackScreenMode.Content -> WideContentBox { sidePadding ->
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
                    TrackRow(
                      item = item,
                      onCheckedChange = { tracked ->
                        viewModel.setPackageTracked(item.packageName, tracked)
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

      AdaptiveSearchOverlay(
        config = TopBarSearchConfig(
          state = searchState,
          label = stringResource(R.string.search_hint)
        )
      ) { activeQuery ->
        val searchListState = rememberLazyListState()
        LaunchedEffect(activeQuery) {
          searchListState.scrollToItem(0)
        }
        if (uiState.isLoading && uiState.searchItems.isEmpty()) {
          WideContentBox { sidePadding ->
            TrackListSkeleton(
              contentPadding = PaddingValues(
                top = 6.dp,
                start = sidePadding,
                end = sidePadding
              )
            )
          }
        } else if (uiState.searchItems.isEmpty()) {
          LcEmptyState(title = stringResource(R.string.empty_list))
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
              items(items = uiState.searchItems, key = { it.packageName }) { item ->
                TrackRow(
                  item = item,
                  onCheckedChange = { tracked ->
                    viewModel.setPackageTracked(item.packageName, tracked)
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
private fun TrackRow(
  item: TrackedAppListItem,
  onCheckedChange: (Boolean) -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .padding(bottom = 12.dp),
    onClick = { onCheckedChange(!item.switchState) },
    showIndication = true,
    insideMargin = PaddingValues(start = 10.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      AppIcon(
        packageInfo = item.packageInfo,
        label = item.label,
        modifier = Modifier
          .padding(end = 12.dp)
          .size(44.dp)
      )
      Column(Modifier.weight(1f)) {
        Text(
          text = item.label,
          fontWeight = FontWeight(500),
          color = colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = item.packageName,
          style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
          color = colorScheme.onSurfaceVariantSummary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Checkbox(
        state = if (item.switchState) ToggleableState.On else ToggleableState.Off,
        onClick = { onCheckedChange(!item.switchState) },
        modifier = Modifier.padding(start = 12.dp)
      )
    }
  }
}

private enum class TrackScreenMode {
  Failed,
  Loading,
  Empty,
  Content
}
