package com.absinthe.libchecker.ui.compose.util

import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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

/** Smooth height transition for bottom sheets whose content switches between states. */
fun Modifier.sheetHeightTransition(): Modifier = animateContentSize(
  folmeSpring(damping = 0.9f, response = 0.38f, visibilityThreshold = IntSize(1, 1))
)
