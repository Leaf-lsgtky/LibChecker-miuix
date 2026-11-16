package com.absinthe.libchecker.ui.compose.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastRoundToInt
import com.absinthe.libchecker.ui.compose.util.rememberIsWideScreen
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Top bar that adapts to UI style (OS3 / OS4 glass), blur preference, and window size:
 * - OS4 collapsible bars render [GlassTopAppBar] with progressive backdrop blur and glass pills.
 * - OS4 pinned / wide bars render [SmallTopAppBar] with progressive blur and glass pill context.
 * - OS3 bars render [TopAppBar] or [SmallTopAppBar] with optional backdrop texture blur.
 */
@Composable
fun AdaptiveTopAppBar(
  title: String,
  subtitle: String = "",
  modifier: Modifier = Modifier,
  color: Color = MiuixTheme.colorScheme.surface,
  scrollBehavior: ScrollBehavior? = null,
  backdrop: LayerBackdrop? = LocalMiuixTopBarBackdrop.current,
  isContentScrolled: Boolean? = null,
  pinned: Boolean = false,
  search: TopBarSearchConfig? = null,
  navigationIcon: @Composable () -> Unit = {},
  actions: @Composable RowScope.() -> Unit = {},
  bottomContent: @Composable () -> Unit = {}
) {
  val useOs4 = rememberUseMiuixOs4()
  val useBlur = rememberUseMiuixBlur()
  val isWideScreen = rememberIsWideScreen()
  val resolvedBackdrop = backdrop.takeIf { useBlur }
  val scrollHost = LocalFastScrollHost.current
  val barModifier = modifier.onGloballyPositioned { coordinates ->
    scrollHost?.topBarBottom = coordinates.boundsInWindow().bottom
  }
  val scrollEntry = scrollHost?.activeEntry
  val exactScrolled = isContentScrolled ?: scrollEntry?.listState?.canScrollBackward
  val density = LocalDensity.current
  val visualContentOffset: (() -> Float)? = remember(scrollEntry, scrollBehavior, density) {
    scrollEntry?.let { entry ->
      val rampDistance = with(density) { GlassTopAppBarDefaults.ProgressiveBlurRampDistance.toPx() }
      val readOffset: () -> Float = {
        val list = entry.listState
        val offset = if (list.firstVisibleItemIndex == 0) {
          -list.firstVisibleItemScrollOffset.toFloat()
        } else {
          -rampDistance
        }
        offset + (scrollBehavior?.state?.overscrollOffset ?: 0f)
      }
      readOffset
    }
  }
  val hasOverscrollUp = (scrollBehavior?.state?.overscrollOffset ?: 0f) < -0.5f
  val scrolled = (exactScrolled ?: ((scrollBehavior?.state?.contentOffset ?: 0f) < 0f)) || hasOverscrollUp

  if (search != null) {
    val transition = updateTransition(search.state.expanded, label = "adaptiveTopBarSearch")
    val barAlpha = rememberGlassSearchBarAlpha(transition)
    val standardModifier = if (useOs4) {
      barModifier.glassSearchBarTransition(transition) { barAlpha.value }
    } else {
      barModifier.graphicsLayer { alpha = barAlpha.value }
    }.let { mod ->
      if (search.state.expanded) mod.clearAndSetSemantics { } else mod
    }
    AdaptiveStandardTopAppBar(
      title = title,
      subtitle = subtitle,
      modifier = standardModifier,
      color = color,
      scrollBehavior = scrollBehavior,
      resolvedBackdrop = resolvedBackdrop,
      useOs4 = useOs4,
      isWideScreen = isWideScreen,
      isContentScrolled = exactScrolled,
      visualContentOffset = visualContentOffset,
      scrolled = scrolled,
      pinned = pinned,
      navigationIcon = navigationIcon,
      actions = actions,
      bottomContent = bottomContent
    )
    return
  }

  AdaptiveStandardTopAppBar(
    title = title,
    subtitle = subtitle,
    modifier = barModifier,
    color = color,
    scrollBehavior = scrollBehavior,
    resolvedBackdrop = resolvedBackdrop,
    useOs4 = useOs4,
    isWideScreen = isWideScreen,
    isContentScrolled = exactScrolled,
    visualContentOffset = visualContentOffset,
    scrolled = scrolled,
    pinned = pinned,
    navigationIcon = navigationIcon,
    actions = actions,
    bottomContent = bottomContent
  )
}

@Composable
private fun AdaptiveStandardTopAppBar(
  title: String,
  subtitle: String,
  modifier: Modifier,
  color: Color,
  scrollBehavior: ScrollBehavior?,
  resolvedBackdrop: LayerBackdrop?,
  useOs4: Boolean,
  isWideScreen: Boolean,
  isContentScrolled: Boolean?,
  visualContentOffset: (() -> Float)?,
  scrolled: Boolean,
  pinned: Boolean,
  navigationIcon: @Composable () -> Unit,
  actions: @Composable RowScope.() -> Unit,
  bottomContent: @Composable () -> Unit
) {

  if (useOs4) {
    val os4Actions: @Composable RowScope.() -> Unit = {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = actions
      )
    }
    if (pinned || isWideScreen) {
      val fallbackContext = remember(resolvedBackdrop, scrolled) {
        MiuixTopBarFallbackContext(backdrop = resolvedBackdrop, floating = scrolled)
      }
      CompositionLocalProvider(LocalMiuixTopBarFallbackContext provides fallbackContext) {
        PinnedGlassTopAppBar(
          title = title,
          subtitle = subtitle,
          modifier = modifier,
          color = color,
          backdrop = resolvedBackdrop,
          scrolled = scrolled,
          scrollBehavior = scrollBehavior,
          navigationIcon = navigationIcon,
          actions = os4Actions,
          bottomContent = bottomContent
        )
      }
    } else {
      val glassStyle = miuixGlassStyle()
      if (isContentScrolled != null) {
        GlassTopAppBar(
          title = title,
          isContentScrolled = isContentScrolled,
          visualContentOffset = visualContentOffset,
          modifier = modifier,
          progressiveBlur = resolvedBackdrop != null,
          backdrop = resolvedBackdrop,
          subtitle = subtitle,
          scrollBehavior = scrollBehavior,
          style = glassStyle,
          navigationIcon = navigationIcon,
          actions = os4Actions,
          bottomContent = bottomContent
        )
      } else {
        GlassTopAppBar(
          title = title,
          modifier = modifier,
          visualContentOffset = visualContentOffset,
          progressiveBlur = resolvedBackdrop != null,
          backdrop = resolvedBackdrop,
          subtitle = subtitle,
          scrollBehavior = scrollBehavior,
          style = glassStyle,
          navigationIcon = navigationIcon,
          actions = os4Actions,
          bottomContent = bottomContent
        )
      }
    }
    return
  }

  val os3BlurModifier = modifier.os3TextureBlur(resolvedBackdrop, color)
  val os3Color = if (resolvedBackdrop != null) Color.Transparent else color

  if (pinned || isWideScreen) {
    SmallTopAppBar(
      title = title,
      subtitle = subtitle,
      modifier = os3BlurModifier,
      color = os3Color,
      scrollBehavior = scrollBehavior,
      navigationIcon = navigationIcon,
      actions = actions,
      bottomContent = bottomContent
    )
  } else {
    TopAppBar(
      title = title,
      subtitle = subtitle,
      modifier = os3BlurModifier,
      color = os3Color,
      scrollBehavior = scrollBehavior,
      navigationIcon = navigationIcon,
      actions = actions,
      bottomContent = bottomContent
    )
  }
}

/** Unclipped single-row glass top bar that preserves [GlassIconButton] shadows and overhang blur. */
@Composable
private fun PinnedGlassTopAppBar(
  title: String,
  subtitle: String,
  modifier: Modifier,
  color: Color,
  backdrop: LayerBackdrop?,
  scrolled: Boolean,
  scrollBehavior: ScrollBehavior?,
  navigationIcon: @Composable () -> Unit,
  actions: @Composable RowScope.() -> Unit,
  bottomContent: @Composable () -> Unit
) {
  SideEffect {
    scrollBehavior?.state?.let { state ->
      if (state.heightOffsetLimit != 0f) state.heightOffsetLimit = 0f
      if (state.heightOffset != 0f) state.heightOffset = 0f
    }
  }

  val blurProgress by animateFloatAsState(
    targetValue = if (scrolled) 1f else 0f,
    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
    label = "os4PinnedBarBlur"
  )
  val topInsets = WindowInsets.statusBars
  var bottomContentHeightPx by remember { mutableIntStateOf(0) }
  val navPadding = GlassTopAppBarDefaults.HorizontalPadding
  val actionPadding = GlassTopAppBarDefaults.HorizontalPadding
  val titlePadding = TopAppBarDefaults.TitlePadding
  val titleColor = MiuixTheme.colorScheme.onSurface
  val subtitleColor = MiuixTheme.colorScheme.onSurfaceVariantSummary

  Layout(
    modifier = modifier,
    content = {
      // Background blur/scrim band rendered before controls so button drop shadows are never clipped.
      Box(
        modifier = if (backdrop != null) {
          val blurColors = BlurDefaults.blurColors(
            blendColors = listOf(BlendColorEntry(color.copy(alpha = 0.3f * blurProgress)))
          )
          Modifier
            .graphicsLayer { alpha = blurProgress }
            .progressiveTextureBlur(
              backdrop = backdrop,
              shape = RectangleShape,
              gradient = ProgressiveBlur.Top.copy(curve = 0.5f),
              blurRadius = GlassTopAppBarDefaults.ProgressiveBlurRadius.value * blurProgress,
              colors = blurColors,
              enabled = blurProgress > 0.01f
            )
        } else {
          Modifier
            .graphicsLayer { alpha = blurProgress }
            .background(GlassTopAppBarDefaults.bandBrush(color))
        }
      )

      Layout(
        content = {
          Box(
            Modifier
              .layoutId("navigationIcon")
              .padding(start = navPadding)
          ) {
            navigationIcon()
          }
          Box(
            Modifier
              .layoutId("title")
              .padding(horizontal = titlePadding)
          ) {
            Text(
              text = title,
              color = titleColor,
              maxLines = 1,
              fontSize = MiuixTheme.textStyles.title3.fontSize,
              fontWeight = FontWeight.Medium,
              overflow = TextOverflow.Ellipsis,
              softWrap = false
            )
          }
          Box(
            Modifier
              .layoutId("actionIcons")
              .padding(end = actionPadding)
          ) {
            Row(
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically,
              content = actions
            )
          }
          if (subtitle.isNotEmpty()) {
            Box(Modifier.layoutId("subtitle")) {
              Text(
                text = subtitle,
                color = subtitleColor,
                style = MiuixTheme.textStyles.body2
              )
            }
          }
          Box(Modifier.layoutId("bottomContent")) {
            bottomContent()
          }
        },
        modifier = Modifier
          .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
          .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
          .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
          .pointerInput(Unit) {
            detectTapGestures { }
          }
      ) { measurables, constraints ->
        val navigationIconPlaceable = measurables
          .fastFirst { it.layoutId == "navigationIcon" }
          .measure(constraints.copy(minWidth = 0, minHeight = 0))
        val actionIconsPlaceable = measurables
          .fastFirst { it.layoutId == "actionIcons" }
          .measure(constraints.copy(minWidth = 0, minHeight = 0))
        val maxTitleWidth = if (constraints.maxWidth == Constraints.Infinity) {
          constraints.maxWidth
        } else {
          (constraints.maxWidth - navigationIconPlaceable.width - actionIconsPlaceable.width).coerceAtLeast(0)
        }
        val titleMaxWidth = if (maxTitleWidth == Constraints.Infinity) {
          maxTitleWidth
        } else {
          (maxTitleWidth * 0.9f).fastRoundToInt()
        }
        val titlePlaceable = measurables
          .fastFirst { it.layoutId == "title" }
          .measure(constraints.copy(minWidth = 0, maxWidth = titleMaxWidth, minHeight = 0))
        val subtitlePlaceable = measurables
          .firstOrNull { it.layoutId == "subtitle" }
          ?.measure(constraints.copy(minWidth = 0, maxWidth = titleMaxWidth, minHeight = 0))
        val bottomContentPlaceable = measurables
          .fastFirst { it.layoutId == "bottomContent" }
          .measure(constraints.copy(minWidth = 0, minHeight = 0))
        bottomContentHeightPx = bottomContentPlaceable.height

        val subtitleHeight = subtitlePlaceable?.height ?: 0
        val collapsedHeight = TopAppBarDefaults.CollapsedHeight.roundToPx()
        val verticalCenter = TopAppBarDefaults.SmallTopAppBarCenterHeight.roundToPx() / 2
        val titleGroupHeight = titlePlaceable.height + subtitleHeight
        val titleGroupTop = verticalCenter - if (subtitlePlaceable != null) {
          titleGroupHeight / 2
        } else {
          titlePlaceable.height / 2
        }
        val subtitleY = titleGroupTop + titlePlaceable.height
        val subtitleBottomPadding = if (subtitlePlaceable != null) {
          TopAppBarDefaults.SubtitleBottomPadding.roundToPx()
        } else {
          0
        }
        val contentTop = maxOf(collapsedHeight, subtitleY + subtitleHeight + subtitleBottomPadding)
        val layoutHeight = contentTop + bottomContentPlaceable.height

        layout(constraints.maxWidth, layoutHeight) {
          navigationIconPlaceable.placeRelative(
            x = 0,
            y = verticalCenter - navigationIconPlaceable.height / 2
          )
          actionIconsPlaceable.placeRelative(
            x = constraints.maxWidth - actionIconsPlaceable.width,
            y = verticalCenter - actionIconsPlaceable.height / 2
          )
          val titleGroupWidth = maxOf(titlePlaceable.width, subtitlePlaceable?.width ?: 0)
          val leadingControlWidth = if (layoutDirection == LayoutDirection.Ltr) {
            navigationIconPlaceable.width
          } else {
            actionIconsPlaceable.width
          }
          val trailingControlWidth = if (layoutDirection == LayoutDirection.Ltr) {
            actionIconsPlaceable.width
          } else {
            navigationIconPlaceable.width
          }
          var groupX = (constraints.maxWidth - titleGroupWidth) / 2
          if (groupX < leadingControlWidth) {
            groupX = leadingControlWidth
          } else if (groupX + titleGroupWidth > constraints.maxWidth - trailingControlWidth) {
            groupX = constraints.maxWidth - trailingControlWidth - titleGroupWidth
          }
          titlePlaceable.place(
            x = groupX + (titleGroupWidth - titlePlaceable.width) / 2,
            y = titleGroupTop
          )
          subtitlePlaceable?.place(
            x = groupX + (titleGroupWidth - subtitlePlaceable.width) / 2,
            y = subtitleY
          )
          bottomContentPlaceable.placeRelative(x = 0, y = contentTop)
        }
      }
    }
  ) { measurables, constraints ->
    val foreground = measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0))
    val width = constraints.constrainWidth(foreground.width).coerceAtLeast(0)
    val height = constraints.constrainHeight(foreground.height).coerceAtLeast(0)
    val bandHeight = if (backdrop != null) {
      height
    } else {
      height + GlassTopAppBarDefaults.BandOverhang.roundToPx().coerceAtLeast(0)
    }
    val background = measurables[0].measure(Constraints.fixed(width, bandHeight.coerceAtLeast(0)))
    layout(width, height) {
      background.placeRelative(0, 0)
      foreground.placeRelative(0, 0)
    }
  }
}

@Composable
private fun Modifier.os4ProgressiveBlur(
  backdrop: LayerBackdrop?,
  color: Color,
  enabled: Boolean
): Modifier {
  if (backdrop == null) return this
  val blurProgress by animateFloatAsState(
    targetValue = if (enabled) 1f else 0f,
    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
    label = "os4PinnedBlur"
  )
  val blurColors = BlurDefaults.blurColors(
    blendColors = listOf(BlendColorEntry(color.copy(alpha = 0.3f * blurProgress)))
  )
  return progressiveTextureBlur(
    backdrop = backdrop,
    shape = RectangleShape,
    gradient = ProgressiveBlur.Top.copy(curve = 0.5f),
    blurRadius = GlassTopAppBarDefaults.ProgressiveBlurRadius.value * blurProgress,
    colors = blurColors,
    enabled = blurProgress > 0.01f
  )
}

@Composable
private fun Modifier.os3TextureBlur(
  backdrop: LayerBackdrop?,
  color: Color
): Modifier {
  if (backdrop == null) return this
  val blurColors = BlurDefaults.blurColors(
    blendColors = listOf(BlendColorEntry(color.copy(alpha = 0.8f)))
  )
  return textureBlur(
    backdrop = backdrop,
    shape = RectangleShape,
    blurRadius = 25f,
    colors = blurColors
  )
}

/**
 * Measures [tabs] using Miuix's selected tab typography so labels like "Broadcast Receivers" or
 * formatted timestamps do not truncate against `TabRowDefaults.TabRowMinWidth` (76 dp).
 */
@Composable
fun rememberFittingTabMinWidth(
  tabs: List<String>,
  floor: Dp = TabRowDefaults.TabRowMinWidth,
  ceil: Dp = 200.dp
): Dp {
  val textMeasurer = rememberTextMeasurer()
  val density = LocalDensity.current
  val tabStyle = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Bold)
  return remember(tabs, tabStyle, density, floor, ceil) {
    val widestPx = tabs.maxOfOrNull { label ->
      textMeasurer.measure(text = label, style = tabStyle, maxLines = 1).size.width
    } ?: 0
    // TabItem applies 12.dp horizontal padding per side plus a 1.dp stroke; add 28.dp so text
    // never ellipsizes even with sub-pixel font rounding.
    val needed = with(density) { widestPx.toDp() } + 28.dp
    needed.coerceIn(floor, ceil.coerceAtLeast(floor))
  }
}
