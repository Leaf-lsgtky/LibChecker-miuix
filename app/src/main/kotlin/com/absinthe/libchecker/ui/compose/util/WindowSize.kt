package com.absinthe.libchecker.ui.compose.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.anim.folmeSpring

/** Width from which the app switches to the wide-screen shell (rail + centred content). */
private val WideScreenMinWidth = 600.dp

/** Maximum content width on wide screens. */
val MaxContentWidth: Dp = 800.dp

/** Whether the current window is wide enough for the rail shell. */
@Composable
fun rememberIsWideScreen(): Boolean {
  val containerSize = LocalWindowInfo.current.containerSize
  val density = LocalDensity.current
  return with(density) { containerSize.width.toDp() >= WideScreenMinWidth }
}

/**
 * Centres content on wide screens while keeping the scrolling container full width: [content]
 * receives the side padding that should be added to the list's `contentPadding`.
 */
@Composable
fun WideContentBox(
  modifier: Modifier = Modifier,
  content: @Composable (sidePadding: Dp) -> Unit
) {
  val isWideScreen = rememberIsWideScreen()
  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val sidePadding = if (isWideScreen) {
      ((maxWidth - MaxContentWidth) / 2).coerceAtLeast(0.dp)
    } else {
      0.dp
    }
    content(sidePadding)
  }
}

/**
 * Adds horizontal display-cutout / navigation-bar padding so landscape content does not run under a
 * notch. The top bar handles its own insets, so this only applies to content roots.
 */
@Composable
fun Modifier.horizontalCutoutPadding(): Modifier = windowInsetsPadding(
  WindowInsets.displayCutout.union(WindowInsets.navigationBars).only(WindowInsetsSides.Horizontal)
)

/** Bottom safe area for bottom sheet content (excludes IME, which the sheet already pads). */
@Composable
fun Modifier.sheetContentSafePadding(): Modifier = windowInsetsPadding(
  WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Bottom)
)

@Composable
fun sheetBottomSafePadding(
  extraBottom: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp(16f)
): androidx.compose.ui.unit.Dp {
  val density = androidx.compose.ui.platform.LocalDensity.current
  val insetPx = WindowInsets.systemBars
    .union(WindowInsets.displayCutout)
    .getBottom(density)
  return with(density) { insetPx.toDp() } + extraBottom
}

/** Smooth height transition for bottom sheets without clipping child drop shadows horizontally. */
fun Modifier.sheetHeightTransition(): Modifier = this.then(SheetHeightTransitionElement)

private object SheetHeightTransitionElement : ModifierNodeElement<SheetHeightTransitionNode>() {
  override fun create(): SheetHeightTransitionNode = SheetHeightTransitionNode()

  override fun update(node: SheetHeightTransitionNode) = Unit

  override fun InspectorInfo.inspectableProperties() {
    name = "sheetHeightTransition"
  }

  override fun equals(other: Any?): Boolean = other === this

  override fun hashCode(): Int = System.identityHashCode(this)
}

private class SheetHeightTransitionNode :
  Modifier.Node(),
  LayoutModifierNode,
  DrawModifierNode {

  private var animatable: Animatable<Int, AnimationVector1D>? = null
  private var targetHeight: Int = -1
  private var animationJob: Job? = null
  private var animationToken: Int = 0
  private var isAnimating: Boolean = false

  override fun onDetach() {
    animationToken++
    animationJob?.cancel()
    animationJob = null
    isAnimating = false
    animatable = null
    targetHeight = -1
  }

  override fun MeasureScope.measure(
    measurable: Measurable,
    constraints: Constraints
  ): MeasureResult {
    val placeable = measurable.measure(constraints)
    val measuredHeight = placeable.height

    val anim = animatable
    if (anim == null) {
      targetHeight = measuredHeight
      animatable = Animatable(measuredHeight, Int.VectorConverter)
    } else if (targetHeight != measuredHeight) {
      targetHeight = measuredHeight
      if (isAttached) {
        val token = ++animationToken
        animationJob?.cancel()
        isAnimating = true
        animationJob = coroutineScope.launch {
          try {
            anim.animateTo(
              targetValue = measuredHeight,
              animationSpec = folmeSpring(damping = 0.9f, response = 0.38f, visibilityThreshold = 1)
            ) {
              if (isAttached) {
                invalidateMeasurement()
              }
            }
          } finally {
            if (animationToken == token) {
              isAnimating = false
              animationJob = null
              if (isAttached) {
                invalidateMeasurement()
                invalidateDraw()
              }
            }
          }
        }
      }
    }

    val currentAnim = animatable
    val displayHeight = if (currentAnim != null && (isAnimating || currentAnim.isRunning)) {
      currentAnim.value.coerceIn(constraints.minHeight, constraints.maxHeight)
    } else {
      measuredHeight
    }

    return layout(placeable.width, displayHeight) {
      placeable.placeRelative(0, 0)
    }
  }

  override fun ContentDrawScope.draw() {
    val anim = animatable
    if (isAnimating || anim?.isRunning == true) {
      val marginPx = 48.dp.toPx()
      clipRect(
        left = -marginPx,
        top = 0f,
        right = size.width + marginPx,
        bottom = size.height
      ) {
        this@draw.drawContent()
      }
    } else {
      drawContent()
    }
  }
}

/** Retains the last non-null [value] across dismissal animations before clearing in `onDismissFinished`. */
@Composable
fun <T> rememberActiveValue(value: T?): MutableState<T?> {
  val active = remember { mutableStateOf(value) }
  if (value != null) {
    active.value = value
  }
  return active
}
