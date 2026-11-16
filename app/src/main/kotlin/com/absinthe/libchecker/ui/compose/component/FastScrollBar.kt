@file:Suppress("ktlint:standard:function-naming")
@file:OptIn(top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi::class)

package com.absinthe.libchecker.ui.compose.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.absinthe.libchecker.R
import kotlin.math.roundToInt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import top.yukonga.miuix.kmp.basic.ScrollBarAdapter
import top.yukonga.miuix.kmp.basic.ScrollBarDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Miuix styling and adapter, with an actual 48dp hit strip and a padded thumb grab area. */
@Composable
internal fun FastScrollBar(
  entry: FastScrollEntry,
  modifier: Modifier = Modifier,
  minimumTrackTop: () -> Float = { 0f }
) {
  val adapter = entry.adapter
  val interactions = remember { MutableInteractionSource() }
  val hovered by interactions.collectIsHoveredAsState()
  var dragging by remember { mutableStateOf(false) }
  var dragThumbLength by remember { mutableFloatStateOf(0f) }
  var viewportHeight by remember { mutableIntStateOf(0) }
  var visible by remember { mutableStateOf(false) }
  val targets = remember(adapter) { Channel<Double>(Channel.CONFLATED) }
  LaunchedEffect(targets) {
    try {
      for (target in targets) adapter.scrollTo(target)
    } finally {
      targets.cancel()
    }
  }
  LaunchedEffect(adapter) {
    snapshotFlow { Triple(adapter.scrollOffset, dragging, hovered) }.drop(1).collectLatest { (_, dragging, hovered) ->
      visible = true
      if (!dragging && !hovered) {
        delay(ScrollBarDefaults.FadeDelayMillis.toLong())
        visible = false
      }
    }
  }
  val opacity = animateFloatAsState(
    targetValue = if (visible) 1f else 0f,
    animationSpec = tween(if (visible) 0 else ScrollBarDefaults.FadeDurationMillis),
    label = "fastScrollVisibility"
  )
  val hitTargetVisible = remember { derivedStateOf { opacity.value > 0f } }
  val highlighted = dragging || hovered
  val thumbWidth = animateFloatAsState(
    targetValue = if (highlighted) ScrollBarDefaults.DragThumbWidth.value else ScrollBarDefaults.ThumbWidth.value,
    animationSpec = tween(ScrollBarDefaults.DragAnimationDurationMillis),
    label = "fastScrollWidth"
  )
  val thumbAlpha = animateFloatAsState(
    targetValue = if (highlighted) ScrollBarDefaults.DragThumbAlpha else ScrollBarDefaults.ThumbAlpha,
    animationSpec = tween(ScrollBarDefaults.DragAnimationDurationMillis),
    label = "fastScrollAlpha"
  )
  val color = MiuixTheme.colorScheme.onSurface
  val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
  val description = stringResource(R.string.fast_scroll)
  val density = LocalDensity.current
  fun trackTop() = with(density) {
    maxOf(entry.trackPadding.calculateTopPadding().toPx(), minimumTrackTop())
  }
  fun thumb() = with(density) {
    scrollThumb(
      adapter,
      viewportHeight.toFloat(),
      trackTop(),
      entry.trackPadding.calculateBottomPadding().toPx(),
      entry.reverseLayout,
      if (dragging) dragThumbLength else 0f
    )
  }
  fun grabTop() = with(density) {
    maxOf(trackTop(), thumb().top - 16.dp.toPx())
  }
  fun grabHeight() = with(density) {
    if (!hitTargetVisible.value || !entry.enabled || (!entry.listState.canScrollBackward && !entry.listState.canScrollForward)) {
      0f
    } else {
      val thumb = thumb()
      val bottom = minOf(
        viewportHeight - entry.trackPadding.calculateBottomPadding().toPx(),
        thumb.top + thumb.length + 16.dp.toPx()
      )
      (bottom - grabTop()).coerceAtLeast(0f)
    }
  }
  Box(
    modifier
      .width(ScrollBarDefaults.TouchTargetWidth)
      .onSizeChanged { viewportHeight = it.height }
      .drawBehind {
        if (opacity.value <= 0f || !entry.enabled ||
          (!entry.listState.canScrollBackward && !entry.listState.canScrollForward)
        ) {
          return@drawBehind
        }
        val thumb = thumb()
        if (thumb.travel <= 0f) return@drawBehind
        val width = thumbWidth.value.dp.toPx()
        val endPadding = ScrollBarDefaults.EndPadding.toPx()
        drawRoundRect(
          color = color.copy(alpha = thumbAlpha.value * opacity.value),
          topLeft = Offset(if (rtl) endPadding else size.width - endPadding - width, thumb.top),
          size = Size(width, thumb.length),
          cornerRadius = CornerRadius(width / 2f)
        )
      }
  ) {
    // Only the expanded thumb rectangle owns touch input. The rest of the right edge
    // remains available to the underlying list's clicks and ordinary vertical scrolling.
    if (hitTargetVisible.value && entry.enabled) {
      Box(
        Modifier
          .fillMaxWidth()
          .offset { IntOffset(0, grabTop().roundToInt()) }
          .layout { measurable, constraints ->
            val height = grabHeight().roundToInt().coerceIn(0, constraints.maxHeight)
            val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
            layout(placeable.width, height) { placeable.place(0, 0) }
          }
          .hoverable(interactions)
          .semantics {
            contentDescription = description
            val range = (adapter.contentSize - adapter.viewportSize).coerceAtLeast(0.0)
            progressBarRangeInfo = ProgressBarRangeInfo(
              current = if (range > 0.0) (adapter.scrollOffset / range).toFloat().coerceIn(0f, 1f) else 0f,
              range = 0f..1f
            )
            setProgress { progress ->
              entry.enabled && range > 0.0 && targets.trySend(progress.coerceIn(0f, 1f) * range).isSuccess
            }
          }
          .pointerInput(entry, targets) {
            awaitEachGesture {
              val down = awaitFirstDown(requireUnconsumed = false)
              if (!entry.enabled || (!entry.listState.canScrollBackward && !entry.listState.canScrollForward)) {
                return@awaitEachGesture
              }
              val top = trackTop()
              val thumb = thumb()
              if (thumb.travel <= 0f) return@awaitEachGesture
              val grabOffset = down.position.y + grabTop() - thumb.top
              // Variable row heights must not resize the thumb under the finger mid-drag.
              dragThumbLength = thumb.length
              dragging = true
              down.consume()
              try {
                drag(down.id) { change ->
                  val position = ((change.position.y + grabTop() - top - grabOffset) / thumb.travel).coerceIn(0f, 1f)
                  val fraction = if (entry.reverseLayout) 1f - position else position
                  val range = (adapter.contentSize - adapter.viewportSize).coerceAtLeast(0.0)
                  targets.trySend(fraction * range)
                  change.consume()
                }
              } finally {
                dragging = false
              }
            }
          }
      )
    }
  }
}

private data class ScrollThumb(val top: Float, val length: Float, val travel: Float)

private fun androidx.compose.ui.unit.Density.scrollThumb(
  adapter: ScrollBarAdapter,
  height: Float,
  topPadding: Float,
  bottomPadding: Float,
  reverseLayout: Boolean,
  fixedLength: Float = 0f
): ScrollThumb {
  val track = (height - topPadding - bottomPadding).coerceAtLeast(0f)
  val visibleFraction = if (adapter.contentSize > 0.0) {
    (adapter.viewportSize / adapter.contentSize).coerceIn(0.0, 1.0)
  } else {
    1.0
  }
  val length = (
    fixedLength.takeIf { it > 0f }
      ?: maxOf(ScrollBarDefaults.ThumbMinLength.toPx(), (track * visibleFraction).toFloat())
    ).coerceAtMost(track)
  val travel = track - length
  val range = (adapter.contentSize - adapter.viewportSize).coerceAtLeast(0.0)
  val fraction = if (range > 0.0) (adapter.scrollOffset / range).toFloat().coerceIn(0f, 1f) else 0f
  val offset = (if (reverseLayout) 1f - fraction else fraction) * travel
  return ScrollThumb(topPadding + offset, length, travel)
}
