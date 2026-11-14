package com.absinthe.libchecker.domain.snapshot.track.ui.compose

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.list.ui.compose.AppIcon
import com.absinthe.libchecker.domain.snapshot.track.model.TrackedAppListItem
import com.absinthe.libchecker.domain.snapshot.track.presentation.TrackViewModel
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.LcEmptyState
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

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
  val focusManager = LocalFocusManager.current
  var query by rememberSaveable { mutableStateOf("") }
  var searchExpanded by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    viewModel.loadTrackList()
  }

  LaunchedEffect(query) {
    viewModel.setQuery(query)
  }

  Column(modifier = modifier.fillMaxSize()) {
    AdaptiveTopAppBar(
      title = stringResource(R.string.album_item_track_title),
      scrollBehavior = scrollBehavior,
      navigationIcon = {
        IconButton(onClick = onBack) {
          Icon(MiuixIcons.Back, contentDescription = stringResource(R.string.action_back))
        }
      },
      bottomContent = {
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
      }
    )

    Box(Modifier.weight(1f)) {
      when {
        uiState.loadFailed -> LcEmptyState(
          title = stringResource(R.string.package_list_load_failed),
          actionLabel = stringResource(R.string.loading),
          onAction = { viewModel.loadTrackList() }
        )

        uiState.isLoading -> LcLoadingState(message = stringResource(R.string.loading))

        uiState.items.isEmpty() -> LcEmptyState(title = stringResource(R.string.empty_list))

        else -> WideContentBox { sidePadding ->
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
      }
      Switch(
        checked = item.switchState,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.padding(start = 12.dp)
      )
    }
  }
}
