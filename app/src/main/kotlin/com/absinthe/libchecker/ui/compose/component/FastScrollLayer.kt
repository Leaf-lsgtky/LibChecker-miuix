@file:Suppress("ktlint:standard:function-naming")
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.ScrollBarAdapter

@Stable
internal class FastScrollEntry(val listState: LazyListState, val adapter: ScrollBarAdapter) {
  var boundsInWindow by mutableStateOf(Rect.Zero)
  var trackPadding by mutableStateOf<PaddingValues>(PaddingValues(0.dp))
  var reverseLayout by mutableStateOf(false)
  var enabled by mutableStateOf(true)
}

@Stable
internal class FastScrollHost(val view: View) {
  val entries = mutableStateListOf<FastScrollEntry>()
  var coveringLayers by mutableIntStateOf(0)
  var origin by mutableStateOf(Offset.Zero)
  var topBarBottom by mutableFloatStateOf(0f)
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
                FastScrollBar(
                  entry = entry,
                  minimumTrackTop = { (host.topBarBottom - entry.boundsInWindow.top).coerceAtLeast(0f) },
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
