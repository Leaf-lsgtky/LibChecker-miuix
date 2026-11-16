package com.absinthe.libchecker.ui.compose.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TabRowColors
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassMotion
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRowDefaults
import top.yukonga.miuix.kmp.glass.GlassTabColors
import top.yukonga.miuix.kmp.glass.GlassTabRowDefaults
import top.yukonga.miuix.kmp.glass.glass
import top.yukonga.miuix.kmp.glass.glassPanel
import top.yukonga.miuix.kmp.glass.glassShadow
import top.yukonga.miuix.kmp.material.EdgeStroke
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.material.GlassStyle
import top.yukonga.miuix.kmp.material.drawEdgeStroke
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Available visual styles for [AdaptiveTabRow] in OS4 glass mode.
 */
enum class AdaptiveTabRowStyle {
  Segmented,
  Primary,
  Neutral
}

/**
 * Style-switching tab row that renders [AdaptiveGlassSegmentedTabRow] (default) or
 * [AdaptiveGlassTabRow] in OS4 glass mode, and Miuix's [TabRow] in OS3 mode.
 */
@Composable
fun AdaptiveTabRow(
  tabs: List<String>,
  selectedTabIndex: Int,
  onTabSelected: (Int) -> Unit,
  modifier: Modifier = Modifier,
  style: AdaptiveTabRowStyle = AdaptiveTabRowStyle.Segmented,
  backdrop: Backdrop? = LocalMiuixTopBarBackdrop.current,
  surfaceAlpha: Float = 1f,
  minWidth: Dp? = null,
  maxWidth: Dp = TabRowDefaults.TabRowMaxWidth,
  height: Dp? = null,
  itemSpacing: Dp = 8.dp,
  os3Colors: TabRowColors = TabRowDefaults.tabRowColors(),
  contentAlignment: Alignment = Alignment.Center,
  horizontalContentPadding: Dp = 0.dp
) {
  if (tabs.isEmpty()) return
  if (rememberUseMiuixOs4()) {
    val glassStyle = miuixGlassStyle()
    when (style) {
      AdaptiveTabRowStyle.Segmented -> {
        AdaptiveGlassSegmentedTabRow(
          tabs = tabs,
          selectedIndex = selectedTabIndex,
          onSelect = onTabSelected,
          backdrop = backdrop,
          modifier = modifier,
          style = glassStyle,
          surfaceAlpha = surfaceAlpha,
          height = height ?: GlassSegmentedTabRowDefaults.Height
        )
      }

      AdaptiveTabRowStyle.Primary -> {
        AdaptiveGlassTabRow(
          tabs = tabs,
          selectedIndex = selectedTabIndex,
          onSelect = onTabSelected,
          backdrop = backdrop,
          modifier = modifier,
          style = glassStyle,
          surfaceAlpha = surfaceAlpha,
          height = height ?: GlassTabRowDefaults.Height,
          tabGap = GlassTabRowDefaults.TabGap,
          colors = GlassTabRowDefaults.primaryColors(),
          horizontalContentPadding = horizontalContentPadding
        )
      }

      AdaptiveTabRowStyle.Neutral -> {
        AdaptiveGlassTabRow(
          tabs = tabs,
          selectedIndex = selectedTabIndex,
          onSelect = onTabSelected,
          backdrop = backdrop,
          modifier = modifier,
          style = glassStyle,
          surfaceAlpha = surfaceAlpha,
          height = height ?: GlassTabRowDefaults.NeutralHeight,
          tabGap = GlassTabRowDefaults.TabGap,
          colors = GlassTabRowDefaults.neutralColors(),
          horizontalContentPadding = horizontalContentPadding
        )
      }
    }
  } else {
    val fittingMinWidth = minWidth ?: rememberFittingTabMinWidth(
      tabs = tabs,
      ceil = maxOf(TabRowDefaults.TabRowMinWidth, maxWidth, 220.dp)
    )
    TabRow(
      tabs = tabs,
      selectedTabIndex = selectedTabIndex,
      onTabSelected = onTabSelected,
      modifier = modifier,
      colors = os3Colors,
      minWidth = fittingMinWidth,
      maxWidth = maxOf(fittingMinWidth, maxWidth),
      height = height ?: TabRowDefaults.TabRowHeight,
      itemSpacing = itemSpacing,
      contentAlignment = contentAlignment
    )
  }
}

/**
 * Adaptive-width, horizontally scrollable variant of [top.yukonga.miuix.kmp.glass.GlassTabRow]
 * supporting both [GlassTabRowDefaults.primaryColors] (40.dp) and
 * [GlassTabRowDefaults.neutralColors] (35.dp).
 */
@Composable
fun AdaptiveGlassTabRow(
  tabs: List<String>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  backdrop: Backdrop?,
  modifier: Modifier = Modifier,
  style: GlassStyle = GlassDefaults.Style,
  surfaceAlpha: Float = 1f,
  height: Dp = GlassTabRowDefaults.Height,
  tabGap: Dp = GlassTabRowDefaults.TabGap,
  colors: GlassTabColors = GlassTabRowDefaults.primaryColors(),
  stroke: EdgeStroke? = GlassTabRowDefaults.stroke(),
  horizontalContentPadding: Dp = 12.dp
) {
  if (tabs.isEmpty()) return
  val currentOnSelect by rememberUpdatedState(onSelect)
  val index = selectedIndex.coerceIn(0, tabs.lastIndex)
  val runtimeShaderSupported = isRuntimeShaderSupported()
  val ramp = if (backdrop != null && runtimeShaderSupported) {
    surfaceAlpha.coerceIn(0f, 1f)
  } else {
    0f
  }
  val shape = GlassTabRowDefaults.shape(height)
  val material = GlassTabRowDefaults.material()

  val scrollState = rememberScrollState()
  var viewportWidth by remember { mutableIntStateOf(0) }
  val tabCenters = remember(tabs) { IntArray(tabs.size) }
  var placementRev by remember(tabs) { mutableIntStateOf(0) }

  LaunchedEffect(tabs, index, viewportWidth, placementRev) {
    if (viewportWidth <= 0) return@LaunchedEffect
    val center = tabCenters.getOrNull(index) ?: 0
    if (center > 0) {
      val targetScroll = (center - viewportWidth / 2).coerceIn(0, scrollState.maxValue)
      scrollState.animateScrollTo(targetScroll)
    }
  }

  BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    val density = LocalDensity.current
    val availableInnerWidthPx = remember(constraints.maxWidth, constraints.hasBoundedWidth, horizontalContentPadding, density) {
      if (constraints.hasBoundedWidth) {
        val edgePaddingPx = with(density) { horizontalContentPadding.roundToPx() * 2 }
        (constraints.maxWidth - edgePaddingPx).coerceAtLeast(0)
      } else {
        0
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .onSizeChanged { viewportWidth = it.width }
        .horizontalScroll(scrollState)
        .padding(horizontal = horizontalContentPadding, vertical = 6.dp)
        .selectableGroup()
    ) {
      AdaptiveTabRowLayout(
        availableWidthPx = availableInnerWidthPx,
        gap = tabGap
      ) {
        tabs.forEachIndexed { position, label ->
          key(position, label) {
            val selected = position == index
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()

            val animatedBg by animateColorAsState(
              targetValue = if (selected) colors.selectedContainerColor else colors.containerColor,
              animationSpec = GlassMotion.navContent(),
              label = "adaptiveGlassTabContainer"
            )
            val contentColor by animateColorAsState(
              targetValue = if (selected) colors.selectedContentColor else colors.contentColor,
              animationSpec = GlassMotion.navContent(),
              label = "adaptiveGlassTabContent"
            )
            val overlayColor by animateColorAsState(
              targetValue = if (pressed) colors.pressedOverlayColor else Color.Transparent,
              animationSpec = if (pressed) GlassMotion.navPressEnter() else GlassMotion.navPressExit(),
              label = "adaptiveGlassTabOverlay"
            )

            val panelModifier = if (backdrop == null || ramp <= 0f) {
              Modifier
                .glassShadow(shape, GlassShadows.Float, 1f)
                .clip(shape)
            } else {
              Modifier.glassPanel(
                backdrop = backdrop,
                shape = shape,
                style = style,
                alpha = ramp,
                material = material,
                stroke = stroke,
                shadow = GlassShadows.Float,
                shading = false,
                fallback = Modifier.clip(shape)
              )
            }

            Box(
              modifier = Modifier
                .height(height)
                .onPlaced { coords ->
                  val newCenter = (coords.positionInParent().x + coords.size.width / 2f).roundToInt()
                  if (tabCenters[position] != newCenter) {
                    tabCenters[position] = newCenter
                    placementRev++
                  }
                }
                .then(panelModifier)
                .drawBehind {
                  val container = if (selected) {
                    animatedBg
                  } else {
                    lerp(colors.restingContainerColor, animatedBg, ramp)
                  }
                  drawRect(container)
                  drawRect(overlayColor)
                }
                .drawWithContent {
                  drawContent()
                  val restingRim = (1f - ramp) * if (selected) 0f else colors.restingStrokeAlpha
                  if (restingRim > 0.001f && stroke != null) {
                    drawEdgeStroke(shape, stroke, restingRim)
                  }
                }
                .selectable(
                  selected = selected,
                  role = Role.Tab,
                  interactionSource = interactionSource,
                  indication = null
                ) { currentOnSelect(position) }
                .padding(
                  horizontal = GlassTabRowDefaults.TabPaddingHorizontal,
                  vertical = GlassTabRowDefaults.TabPaddingVertical
                ),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = label,
                style = if (selected) MiuixTheme.textStyles.subtitle else MiuixTheme.textStyles.body2,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Adaptive-width, horizontally scrollable variant of
 * [top.yukonga.miuix.kmp.glass.GlassSegmentedTabRow].
 *
 * Each tab pill measures its own intrinsic label width so heterogeneous or long tab titles never
 * truncate, while keeping clean rounded track caps, edge-fade clipping, and a smoothly resizing
 * sliding indicator pill.
 */
@Composable
fun AdaptiveGlassSegmentedTabRow(
  tabs: List<String>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  backdrop: Backdrop?,
  modifier: Modifier = Modifier,
  style: GlassStyle = GlassDefaults.Style,
  surfaceAlpha: Float = 1f,
  height: Dp = GlassSegmentedTabRowDefaults.Height,
  trackColor: Color = GlassSegmentedTabRowDefaults.restingTrackColor(),
  indicatorColor: Color = GlassSegmentedTabRowDefaults.indicatorColor(),
  selectedContentColor: Color = MiuixTheme.colorScheme.onSurface,
  contentColor: Color = MiuixTheme.colorScheme.onSurface
) {
  if (tabs.isEmpty()) return
  val currentOnSelect by rememberUpdatedState(onSelect)
  val index = selectedIndex.coerceIn(0, tabs.lastIndex)
  val runtimeShaderSupported = isRuntimeShaderSupported()
  val ramp = if (backdrop != null && runtimeShaderSupported) {
    surfaceAlpha.coerceIn(0f, 1f)
  } else {
    0f
  }
  val trackPadding = GlassSegmentedTabRowDefaults.TrackPadding
  val trackShape = GlassSegmentedTabRowDefaults.shape(height)
  val indicatorHeight = (height - trackPadding * 2).coerceAtLeast(0.dp)
  val indicatorShape = GlassSegmentedTabRowDefaults.shape(indicatorHeight)
  val material = GlassSegmentedTabRowDefaults.material()
  val stroke = GlassSegmentedTabRowDefaults.stroke()

  val scrollState = rememberScrollState()
  val scope = rememberCoroutineScope()
  val layoutDirection = LocalLayoutDirection.current
  var viewportWidth by remember { mutableIntStateOf(0) }
  val tabLefts = remember(tabs) { FloatArray(tabs.size) }
  val tabWidths = remember(tabs) { FloatArray(tabs.size) }
  var placementRev by remember(tabs) { mutableIntStateOf(0) }
  var hasSnapped by remember(tabs) { mutableStateOf(false) }
  var dragging by remember(tabs) { mutableStateOf(false) }
  var draggedTabIndex by remember(tabs) { mutableIntStateOf(-1) }
  var settleGeneration by remember(tabs) { mutableIntStateOf(0) }

  val indicatorLeft = remember(tabs) { Animatable(0f) }
  val indicatorWidth = remember(tabs) { Animatable(0f) }

  LaunchedEffect(tabs, index, viewportWidth, placementRev, settleGeneration) {
    if (dragging) return@LaunchedEffect
    val targetLeft = tabLefts.getOrNull(index) ?: 0f
    val targetWidth = tabWidths.getOrNull(index) ?: 0f
    if (targetWidth <= 0f) return@LaunchedEffect

    if (!hasSnapped) {
      indicatorLeft.snapTo(targetLeft)
      indicatorWidth.snapTo(targetWidth)
      hasSnapped = true
    } else {
      launch { indicatorLeft.animateTo(targetLeft, GlassMotion.default()) }
      launch { indicatorWidth.animateTo(targetWidth, GlassMotion.default()) }
    }

    if (viewportWidth > 0) {
      val center = (targetLeft + targetWidth / 2f).roundToInt()
      val desiredScroll = (center - viewportWidth / 2).coerceIn(0, scrollState.maxValue)
      launch { scrollState.animateScrollTo(desiredScroll) }
    }
  }

  val glassModifier = if (backdrop != null && ramp > 0f) {
    Modifier.glass(
      backdrop = backdrop,
      shape = trackShape,
      style = style,
      alpha = ramp,
      material = material,
      stroke = stroke,
      enabled = true,
      shading = false
    )
  } else {
    Modifier
  }

  BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    val density = LocalDensity.current
    val availableInnerWidthPx = remember(constraints.maxWidth, constraints.hasBoundedWidth, trackPadding, density) {
      if (constraints.hasBoundedWidth) {
        val trackPaddingPx = with(density) { trackPadding.roundToPx() * 2 }
        (constraints.maxWidth - trackPaddingPx).coerceAtLeast(0)
      } else {
        0
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(height)
        .glassShadow(trackShape, GlassShadows.Float, if (backdrop != null && runtimeShaderSupported) ramp else 1f)
        .clip(trackShape)
        .then(glassModifier)
        .background(trackColor.copy(alpha = trackColor.alpha * (1f - ramp)))
        .padding(trackPadding)
        .onSizeChanged { viewportWidth = it.width }
        .clip(indicatorShape)
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
          drawContent()
          val fadePx = 14.dp.toPx()
          if (scrollState.canScrollBackward && size.width > fadePx) {
            drawRect(
              brush = Brush.horizontalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startX = 0f,
                endX = fadePx
              ),
              topLeft = Offset.Zero,
              size = Size(fadePx, size.height),
              blendMode = BlendMode.DstOut
            )
          }
          if (scrollState.canScrollForward && size.width > fadePx) {
            drawRect(
              brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startX = size.width - fadePx,
                endX = size.width
              ),
              topLeft = Offset(size.width - fadePx, 0f),
              size = Size(fadePx, size.height),
              blendMode = BlendMode.DstOut
            )
          }
        }
        .horizontalScroll(scrollState)
    ) {
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .pointerInput(tabs.size, layoutDirection, placementRev) {
            if (tabs.size <= 1) return@pointerInput
            val dragSpring = spring<Float>(dampingRatio = 0.88f, stiffness = 1200f)
            fun logicalX(rawX: Float): Float =
              if (layoutDirection == LayoutDirection.Rtl) size.width - rawX else rawX

            fun tabIndexAt(x: Float): Int {
              for (i in tabs.indices) {
                val left = tabLefts[i]
                val right = left + tabWidths[i]
                if (x in left..right) return i
              }
              var best = 0
              var bestDist = Float.MAX_VALUE
              for (i in tabs.indices) {
                val dist = abs(x - (tabLefts[i] + tabWidths[i] / 2f))
                if (dist < bestDist) {
                  bestDist = dist
                  best = i
                }
              }
              return best
            }

            awaitEachGesture {
              val down = awaitFirstDown(requireUnconsumed = false)
              val downX = logicalX(down.position.x)
              val downY = down.position.y
              val totalWidth = size.width.toFloat()
              if (totalWidth <= 0f) return@awaitEachGesture

              // When the strip overflows the viewport, let drags outside the active pill scroll
              // the strip while drags starting on the pill scrub tabs directly.
              if (scrollState.maxValue > 0) {
                val pillStart = indicatorLeft.value
                val pillEnd = pillStart + indicatorWidth.value
                if (downX < pillStart || downX > pillEnd) return@awaitEachGesture
              }

              var grabOffset = Float.NaN
              var hovered = tabIndexAt(downX)
              try {
                while (true) {
                  val event = awaitPointerEvent()
                  val change = event.changes.firstOrNull { it.id == down.id } ?: break
                  if (change.isConsumed) break
                  val x = logicalX(change.position.x)
                  val dx = x - downX
                  val dy = change.position.y - downY
                  if (!change.pressed) {
                    if (dragging && change.changedToUp()) {
                      val releaseIndex = tabIndexAt(x)
                      dragging = false
                      draggedTabIndex = -1
                      settleGeneration++
                      currentOnSelect(releaseIndex)
                      change.consume()
                    }
                    break
                  }
                  if (grabOffset.isNaN()) {
                    val slop = viewConfiguration.touchSlop
                    if (abs(dy) > slop && abs(dy) > abs(dx)) break
                    if (abs(dx) < slop) continue
                    dragging = true
                    hovered = tabIndexAt(x)
                    draggedTabIndex = hovered
                    val currentW = indicatorWidth.value
                    grabOffset = if (downX in indicatorLeft.value..(indicatorLeft.value + currentW)) {
                      downX - indicatorLeft.value
                    } else {
                      (tabWidths.getOrNull(hovered) ?: currentW) / 2f
                    }
                  }
                  val nextIndex = tabIndexAt(x)
                  val targetW = tabWidths.getOrNull(nextIndex)?.takeIf { it > 0f }
                    ?: indicatorWidth.value
                  val maxLeft = (totalWidth - targetW).coerceAtLeast(0f)
                  val desiredLeft = (x - grabOffset).coerceIn(0f, maxLeft)
                  scope.launch { indicatorLeft.animateTo(desiredLeft, dragSpring) }
                  if (nextIndex != hovered || draggedTabIndex != nextIndex) {
                    hovered = nextIndex
                    draggedTabIndex = nextIndex
                    scope.launch { indicatorWidth.animateTo(targetW, GlassMotion.default()) }
                    if (scrollState.maxValue > 0 && viewportWidth > 0) {
                      val center = (tabLefts[nextIndex] + targetW / 2f).roundToInt()
                      val desiredScroll = (center - viewportWidth / 2).coerceIn(0, scrollState.maxValue)
                      scope.launch { scrollState.animateScrollTo(desiredScroll) }
                    }
                  }
                  change.consume()
                }
              } finally {
                if (dragging) {
                  dragging = false
                  draggedTabIndex = -1
                  settleGeneration++
                }
              }
            }
          }
      ) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .absoluteOffset { IntOffset(indicatorLeft.value.roundToInt(), 0) }
            .layout { measurable, constraints ->
              val width = indicatorWidth.value.roundToInt().coerceAtLeast(0)
              val placeable = measurable.measure(
                constraints.copy(minWidth = width, maxWidth = width)
              )
              layout(width, placeable.height) {
                placeable.placeRelative(0, 0)
              }
            }
            .clip(indicatorShape)
            .background(indicatorColor)
        )

        AdaptiveTabRowLayout(
            availableWidthPx = availableInnerWidthPx,
          gap = GlassSegmentedTabRowDefaults.TabGap,
          modifier = Modifier
            .fillMaxHeight()
            .selectableGroup()
        ) {
          val activeVisualIndex = if (dragging && draggedTabIndex in tabs.indices) draggedTabIndex else index
          tabs.forEachIndexed { position, label ->
            key(position, label) {
              val selected = position == activeVisualIndex
              val labelColor by animateColorAsState(
                targetValue = if (selected) selectedContentColor else contentColor,
                animationSpec = GlassMotion.navContent(),
                label = "adaptiveSegmentedTabContent"
              )
              Box(
                modifier = Modifier
                  .fillMaxHeight()
                  .onPlaced { coords ->
                    val newLeft = coords.positionInParent().x
                    val newWidth = coords.size.width.toFloat()
                    if (tabLefts[position] != newLeft || tabWidths[position] != newWidth) {
                      tabLefts[position] = newLeft
                      tabWidths[position] = newWidth
                      placementRev++
                    }
                  }
                  .clip(indicatorShape)
                  .selectable(
                    selected = selected,
                    role = Role.Tab,
                    interactionSource = null,
                    indication = null
                  ) { currentOnSelect(position) }
                  .padding(horizontal = GlassSegmentedTabRowDefaults.TabPaddingHorizontal),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = label,
                  style = if (selected) MiuixTheme.textStyles.subtitle else MiuixTheme.textStyles.body2,
                  color = labelColor,
                  textAlign = TextAlign.Center,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }
        }
      }
    }
  }
}

private fun distributeAdaptiveTabWidths(
  naturalWidths: IntArray,
  availableWidthPx: Int,
  gapPx: Int
): IntArray? {
  val count = naturalWidths.size
  if (count == 0 || availableWidthPx <= 0) return null
  val totalGapsPx = gapPx.toLong() * (count - 1)
  val budgetLong = availableWidthPx.toLong() - totalGapsPx
  if (budgetLong <= 0L) return null
  var naturalSum = 0L
  for (w in naturalWidths) {
    naturalSum += w.coerceAtLeast(0)
  }
  if (naturalSum >= budgetLong) return null

  var remainingBudget = budgetLong.toInt()
  var unlockedCount = count
  val locked = BooleanArray(count)
  val result = IntArray(count)

  while (unlockedCount > 0) {
    val equalShare = remainingBudget / unlockedCount
    var didLock = false
    for (i in 0 until count) {
      if (!locked[i] && naturalWidths[i] > equalShare) {
        locked[i] = true
        result[i] = naturalWidths[i]
        remainingBudget -= naturalWidths[i]
        unlockedCount--
        didLock = true
      }
    }
    if (!didLock) break
  }

  if (unlockedCount > 0) {
    val perUnlocked = remainingBudget / unlockedCount
    var remainder = remainingBudget % unlockedCount
    for (i in 0 until count) {
      if (!locked[i]) {
        result[i] = perUnlocked + if (remainder-- > 0) 1 else 0
      }
    }
  }
  return result
}

@Composable
private fun AdaptiveTabRowLayout(
  availableWidthPx: Int,
  gap: Dp,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  Layout(
    content = content,
    modifier = modifier
  ) { measurables, constraints ->
    val gapPx = gap.roundToPx()
    // Measure the actual tab slots: estimating text widths separately can choose the
    // scrolling branch while the real row still fits, leaving centered gaps at some DPIs.
    val slotWidths = IntArray(measurables.size) { index ->
      measurables[index].maxIntrinsicWidth(constraints.maxHeight)
    }
    val expandedWidths = distributeAdaptiveTabWidths(
      naturalWidths = slotWidths,
      availableWidthPx = availableWidthPx,
      gapPx = gapPx
    )
    val placeables = measurables.mapIndexed { index, measurable ->
      val w = (expandedWidths?.get(index) ?: slotWidths[index]).coerceAtLeast(0)
      val childConstraints = constraints.copy(minWidth = w, maxWidth = w)
      measurable.measure(childConstraints)
    }

    val totalHeight = placeables.maxOfOrNull { it.height } ?: constraints.minHeight
    val totalWidth = if (expandedWidths != null) {
      availableWidthPx
    } else if (placeables.isEmpty()) {
      0
    } else {
      placeables.sumOf { it.width } + gapPx * (placeables.size - 1)
    }

    layout(totalWidth, totalHeight) {
      var x = 0
      placeables.forEachIndexed { index, placeable ->
        val y = (totalHeight - placeable.height) / 2
        placeable.placeRelative(x, y)
        x += placeable.width
        if (index < placeables.lastIndex) {
          x += gapPx
        }
      }
    }
  }
}
