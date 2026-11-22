package com.absinthe.libchecker.domain.statistics.reference.ui.compose

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.list.model.AppListItemViewState
import com.absinthe.libchecker.domain.app.list.ui.compose.AppListRow
import com.absinthe.libchecker.domain.statistics.reference.model.resolveReferenceIcon
import com.absinthe.libchecker.domain.statistics.reference.presentation.LibReferenceViewModel
import com.absinthe.libchecker.ui.compose.component.AdaptiveBackButton
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.AppListSkeleton
import com.absinthe.libchecker.ui.compose.component.LcEmptyState
import com.absinthe.libchecker.ui.compose.component.MiuixPageHost
import com.absinthe.libchecker.ui.compose.component.adaptiveVerticalScroll
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.absinthe.libchecker.ui.compose.component.FastScrollLazyColumn as LazyColumn

/**
 * Detail page for one referenced library: header card plus every app that references it.
 */
@Composable
fun LibReferenceDetailScreen(
  viewModel: LibReferenceViewModel,
  refName: String,
  refLabel: String?,
  refType: Int,
  refPackages: List<String>?,
  onBack: () -> Unit,
  onOpenApp: (LCItem) -> Unit,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState()
) {
  val scrollBehavior = MiuixScrollBehavior()
  var apps by remember { mutableStateOf<List<LCItem>>(emptyList()) }
  var viewStates by remember { mutableStateOf<Map<String, AppListItemViewState>>(emptyMap()) }
  var loading by remember { mutableStateOf(true) }
  var refIconRes by remember(refName, refType) {
    mutableStateOf(resolveReferenceIcon(refName, refType, null))
  }

  LaunchedEffect(refName, refType) {
    if (refPackages != null) {
      viewModel.setData(refName, refType, refPackages)
    } else {
      viewModel.setData(refName, refType)
    }
  }

  LaunchedEffect(refName, refType) {
    refIconRes = viewModel.getReferenceIcon(refName, refType)
  }

  LaunchedEffect(Unit) {
    viewModel.libRefListFlow.collect { items ->
      apps = items
      viewStates = viewModel.buildAppListItemViewStates(items)
      loading = false
    }
  }

  MiuixPageHost(modifier = modifier.fillMaxSize()) {
    val pageState = this
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = MiuixTheme.colorScheme.surface,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      topBar = {
        AdaptiveTopAppBar(
          title = refLabel ?: refName,
          scrollBehavior = scrollBehavior,
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
        val displayMode = when {
          loading -> LibReferenceDetailMode.Loading
          apps.isEmpty() -> LibReferenceDetailMode.Empty
          else -> LibReferenceDetailMode.Content
        }
        Crossfade(
          targetState = displayMode,
          animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
          label = "LibReferenceDetailTransition"
        ) { mode ->
          when (mode) {
            LibReferenceDetailMode.Loading -> WideContentBox { sidePadding ->
              AppListSkeleton(
                contentPadding = PaddingValues(
                  top = padding.calculateTopPadding() + 6.dp,
                  start = sidePadding,
                  end = sidePadding
                ),
                homeBottomPadding = 0.dp,
                showHeaderBanner = true
              )
            }

            LibReferenceDetailMode.Empty -> LcEmptyState(
              title = stringResource(R.string.empty_list),
              modifier = Modifier.padding(padding)
            )

            LibReferenceDetailMode.Content -> WideContentBox { sidePadding ->
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
                item(key = "header") {
                  ReferenceHeader(
                    refName = refName,
                    refLabel = refLabel,
                    refType = refType,
                    iconRes = refIconRes,
                    count = apps.size
                  )
                }
                items(items = apps, key = { it.packageName }) { item ->
                  AppListRow(
                    item = item,
                    viewState = viewStates[item.packageName],
                    packageInfo = viewStates[item.packageName]?.packageInfo,
                    chips = emptyList(),
                    highlight = "",
                    onClick = { onOpenApp(item) }
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
}

@Composable
private fun ReferenceHeader(
  refName: String,
  refLabel: String?,
  refType: Int,
  iconRes: Int,
  count: Int
) {
  val colorScheme = MiuixTheme.colorScheme
  val placeholder = remember { ColorPainter(colorScheme.surfaceContainerHigh) }
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .padding(bottom = 12.dp),
    insideMargin = PaddingValues(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      AsyncImage(
        model = iconRes,
        contentDescription = null,
        placeholder = placeholder,
        error = placeholder,
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .padding(end = 16.dp)
          .size(48.dp)
          .clip(RoundedCornerShape(12.dp))
      )
      Column(Modifier.weight(1f)) {
        Text(
          text = refLabel ?: refName,
          fontWeight = FontWeight(500),
          color = colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (refLabel != null) {
          Spacer(Modifier.height(2.dp))
          Text(
            text = refName,
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
            color = colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
      Text(
        text = count.toString(),
        style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
        color = colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(start = 12.dp)
      )
    }
  }
}

private enum class LibReferenceDetailMode {
  Loading,
  Empty,
  Content
}
