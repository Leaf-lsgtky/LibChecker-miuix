@file:OptIn(top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi::class)

package com.absinthe.libchecker.ui.compose.component

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.absinthe.libchecker.R
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ScrollBarAdapter
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.VerticalScrollBar

@Stable
internal class FastScrollEntry(val listState: LazyListState, val adapter: ScrollBarAdapter) {
  var boundsInWindow by mutableStateOf(Rect.Zero)
  var trackPadding by mutableStateOf<PaddingValues>(PaddingValues(0.dp))
  var enabled by mutableStateOf(true)
}

@Stable
internal class FastScrollHost(val view: View) {
  val entries = mutableStateListOf<FastScrollEntry>()
  var coveringLayers by mutableIntStateOf(0)
  var origin by mutableStateOf(Offset.Zero)
  var topBarBottom by mutableFloatStateOf(0f)

  /** Page scroll behavior, so the scrollbar can drive the top bar while it is dragged. */
  var scrollBehavior by mutableStateOf<ScrollBehavior?>(null)
  val activeEntry by derivedStateOf {
    // Search/results and pager pages can coexist during transitions. Use the foremost
    // visible viewport; clipped off-screen pager lists must not paint another thumb.
    entries.lastOrNull { it.enabled && !it.boundsInWindow.isEmpty }
  }
}

internal val LocalFastScrollHost = staticCompositionLocalOf<FastScrollHost?> { null }

/** Paints scroll controls above page chrome, outside the backdrop sampled by that chrome. */
@Composable
internal fun FastScrollLayer(
  modifier: Modifier = Modifier,
  foreground: @Composable BoxScope.() -> Unit = {},
  content: @Composable BoxScope.() -> Unit
) {
  val view = LocalView.current
  val parent = LocalFastScrollHost.current?.takeIf { it.view === view }
  val host = remember(view) { FastScrollHost(view) }
  DisposableEffect(parent) {
    parent?.let { it.coveringLayers++ }
    onDispose { parent?.let { it.coveringLayers-- } }
  }
  CompositionLocalProvider(LocalFastScrollHost provides host) {
    Box(modifier.onGloballyPositioned { host.origin = it.positionInWindow() }) {
      content()
      if (host.coveringLayers == 0) {
        host.activeEntry?.let { entry ->
          key(entry) {
            Box(Modifier.matchParentSize().clipToBounds()) {
              Box(
                Modifier
                  .offset {
                    val topLeft = entry.boundsInWindow.topLeft - host.origin
                    IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt())
                  }
                  .layout { measurable, constraints ->
                    val bounds = entry.boundsInWindow
                    val width = bounds.width.roundToInt().coerceIn(0, constraints.maxWidth)
                    val height = bounds.height.roundToInt().coerceIn(0, constraints.maxHeight)
                    val placeable = measurable.measure(Constraints.fixed(width, height))
                    layout(width, height) { placeable.place(0, 0) }
                  }
              ) {
                PageScrollBar(
                  entry = entry,
                  topBarBottom = host.topBarBottom,
                  scrollBehavior = host.scrollBehavior,
                  modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                )
              }
            }
          }
        }
      }
      foreground()
    }
  }
}

/**
 * Miuix scrollbar for [entry], kept below the top bar. The library moves the bar itself, so a
 * thumb drag also drives the page's top bar and its progressive blur.
 */
@Composable
internal fun PageScrollBar(
  entry: FastScrollEntry,
  topBarBottom: Float,
  scrollBehavior: ScrollBehavior?,
  modifier: Modifier = Modifier
) {
  val density = LocalDensity.current
  val adapter = entry.adapter
  val scope = rememberCoroutineScope()
  val description = stringResource(R.string.fast_scroll)
  val contentPadding = entry.trackPadding
  val trackPadding = with(density) {
    val topBarInset = (topBarBottom - entry.boundsInWindow.top).coerceAtLeast(0f).toDp()
    PaddingValues(
      top = maxOf(contentPadding.calculateTopPadding(), topBarInset),
      bottom = contentPadding.calculateBottomPadding()
    )
  }
  VerticalScrollBar(
    adapter = adapter,
    modifier = modifier.semantics {
      contentDescription = description
      val range = (adapter.contentSize - adapter.viewportSize).coerceAtLeast(0.0)
      progressBarRangeInfo = ProgressBarRangeInfo(
        current = if (range > 0.0) (adapter.scrollOffset / range).toFloat().coerceIn(0f, 1f) else 0f,
        range = 0f..1f
      )
      setProgress { progress ->
        if (entry.enabled && range > 0.0) {
          scope.launch { adapter.scrollTo(progress.coerceIn(0f, 1f) * range) }
          true
        } else {
          false
        }
      }
    },
    trackPadding = trackPadding,
    topAppBarScrollBehavior = scrollBehavior
  )
}
