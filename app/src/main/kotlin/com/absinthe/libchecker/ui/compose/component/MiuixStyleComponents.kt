package com.absinthe.libchecker.ui.compose.component

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.ui.compose.LocalHomeAppearance
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.IconButtonDefaults
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassMotion
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.glass.GlassPopupSizing
import top.yukonga.miuix.kmp.glass.GlassPopupVisuals
import top.yukonga.miuix.kmp.glass.GlassSecondaryPopup
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.glass.GlassTransformPopup
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
import top.yukonga.miuix.kmp.layout.CascadingPopupDefaults
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.material.GlassStyle
import top.yukonga.miuix.kmp.material.isRenderEffectSupported
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.overlay.OverlayCascadingListPopup
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

fun isMiuixOs4Supported(): Boolean =
  Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
    isRenderEffectSupported() &&
    isRuntimeShaderSupported()

fun isMiuixBlurSupported(): Boolean =
  Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
    isRenderEffectSupported()

@Composable
fun rememberUseMiuixOs4(): Boolean =
  LocalHomeAppearance.current.miuixOs4 && isMiuixOs4Supported()

@Composable
fun rememberUseMiuixBlur(): Boolean {
  val appearance = LocalHomeAppearance.current
  return (appearance.miuixOs4 && isMiuixOs4Supported()) ||
    (appearance.blurDesign && isMiuixBlurSupported())
}

@Composable
fun miuixIsDark(): Boolean = MiuixTheme.colorScheme.surface.luminance() < 0.5f

@Composable
fun miuixGlassStyle(): GlassStyle = GlassDefaults.style(miuixIsDark())

@Composable
fun rememberMiuixBlurBackdrop(enabled: Boolean = rememberUseMiuixBlur()): LayerBackdrop? {
  if (!enabled) return null
  val surfaceColor = MiuixTheme.colorScheme.surface
  return rememberLayerBackdrop {
    drawRect(surfaceColor)
    drawContent()
  }
}

val LocalMiuixTopBarBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }
val LocalMiuixMenuBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }
val LocalHomeBottomPadding = compositionLocalOf { 0.dp }

@Immutable
internal data class MiuixTopBarFallbackContext(
  val backdrop: Backdrop?,
  val floating: Boolean
)

internal val LocalMiuixTopBarFallbackContext =
  staticCompositionLocalOf<MiuixTopBarFallbackContext?> { null }

internal val LocalMiuixMenuAnchorBounds =
  staticCompositionLocalOf<SnapshotStateMap<GlassPopupAnchor, Rect>?> { null }

@Stable
class MiuixPageState(
  val useOs4: Boolean,
  val useBlur: Boolean,
  val topBarBackdrop: LayerBackdrop?,
  val menuBackdrop: LayerBackdrop?,
  internal val menuTriggerBounds: SnapshotStateMap<GlassPopupAnchor, Rect> = mutableStateMapOf()
) {
  val topBarBackdropModifier: Modifier
    get() = topBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier

  val menuBackdropModifier: Modifier
    get() = menuBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier
}

@Composable
fun rememberMiuixPageState(
  enableTopBarBackdrop: Boolean = true,
  enableMenuBackdrop: Boolean = false
): MiuixPageState {
  val useOs4 = rememberUseMiuixOs4()
  val useBlur = rememberUseMiuixBlur()
  val topBarBackdrop = rememberMiuixBlurBackdrop(enabled = enableTopBarBackdrop && useBlur)
  val menuBackdrop = rememberMiuixBlurBackdrop(enabled = enableMenuBackdrop && useOs4)
  return remember(useOs4, useBlur, topBarBackdrop, menuBackdrop) {
    MiuixPageState(
      useOs4 = useOs4,
      useBlur = useBlur,
      topBarBackdrop = topBarBackdrop,
      menuBackdrop = menuBackdrop
    )
  }
}

/**
 * Wraps a Miuix Compose screen so OS4 glass popups rendered in [overlay] sit outside the page's
 * [menuBackdrop] and top bars can sample [topBarBackdrop] without self-sampling cycles.
 */
@Composable
fun MiuixPageHost(
  modifier: Modifier = Modifier,
  enableTopBarBackdrop: Boolean = true,
  enableMenuBackdrop: Boolean = false,
  overlay: @Composable BoxScope.(MiuixPageState) -> Unit = {},
  content: @Composable MiuixPageState.() -> Unit
) {
  val state = rememberMiuixPageState(
    enableTopBarBackdrop = enableTopBarBackdrop,
    enableMenuBackdrop = enableMenuBackdrop
  )
  CompositionLocalProvider(
    LocalMiuixTopBarBackdrop provides state.topBarBackdrop,
    LocalMiuixMenuBackdrop provides state.menuBackdrop,
    LocalMiuixMenuAnchorBounds provides state.menuTriggerBounds
  ) {
    FastScrollLayer(
      modifier = modifier.fillMaxSize(),
      foreground = { overlay(state) }
    ) {
      state.content()
    }
  }
}

@Composable
fun adaptiveBackIcon(): ImageVector =
  if (rememberUseMiuixOs4()) MiuixIcons.Glass.ChevronBackward else MiuixIcons.Back

@Composable
fun AdaptiveBackButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val useOs4 = rememberUseMiuixOs4()
  AdaptiveTopBarIconButton(
    onClick = onClick,
    modifier = modifier
  ) {
    Icon(
      imageVector = adaptiveBackIcon(),
      contentDescription = stringResource(R.string.action_back),
      modifier = if (useOs4) Modifier.size(28.dp) else Modifier
    )
  }
}

/**
 * Top bar action button that switches between [GlassIconButton] in OS4 mode and [IconButton] in
 * OS3 mode. Inside a pinned [SmallTopAppBar] it uses [LocalMiuixTopBarFallbackContext] to supply
 * the backdrop and floating shadow state explicitly.
 */
@Composable
fun AdaptiveTopBarIconButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  content: @Composable () -> Unit
) {
  if (rememberUseMiuixOs4()) {
    if (!enabled) {
      IconButton(
        onClick = onClick,
        enabled = false,
        modifier = modifier,
        minHeight = GlassTopAppBarDefaults.ButtonSize,
        minWidth = GlassTopAppBarDefaults.ButtonSize,
        cornerRadius = GlassTopAppBarDefaults.ButtonSize / 2
      ) {
        content()
      }
      return
    }
    val fallback = LocalMiuixTopBarFallbackContext.current
    val glassStyle = miuixGlassStyle()
    if (fallback != null) {
      val surfaceAlpha by animateFloatAsState(
        targetValue = if (fallback.floating) 1f else 0f,
        animationSpec = spring(
          dampingRatio = 0.85f,
          stiffness = Spring.StiffnessMediumLow,
          visibilityThreshold = 0.0005f
        ),
        label = "topBarFallbackButtonAlpha"
      )
      val showSurface = surfaceAlpha > 0.008f
      GlassIconButton(
        onClick = onClick,
        modifier = modifier,
        inheritTopBarMaterial = false,
        backdrop = if (showSurface) fallback.backdrop else null,
        surfaceAlpha = if (showSurface) surfaceAlpha else 0f,
        style = glassStyle,
        fill = if (showSurface) GlassTopAppBarDefaults.buttonFill() else Color.Transparent,
        stroke = if (showSurface) GlassTopAppBarDefaults.buttonStroke() else null,
        shadow = if (showSurface) GlassShadows.Regular else null
      ) {
        content()
      }
    } else {
      GlassIconButton(
        onClick = onClick,
        modifier = modifier,
        style = glassStyle
      ) {
        content()
      }
    }
  } else {
    IconButton(
      onClick = onClick,
      enabled = enabled,
      modifier = modifier
    ) {
      content()
    }
  }
}

/**
 * Top-bar menu button paired with [AdaptiveGlassOverflowMenu]: in OS3 mode this owns its anchored
 * [OverlayCascadingListPopup] with [holdDownState]; in OS4 mode this acts as the morphing
 * [GlassIconButton] anchor and invokes `onShowChange(true)` so the menu popup renders outside the
 * page backdrop.
 */
@Composable
fun AdaptiveTopBarMenuAction(
  entries: List<DropdownEntry>,
  menuAnchor: GlassPopupAnchor,
  show: Boolean,
  onShowChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isContentScrolled: Boolean = true,
  collapseOnSelection: Boolean = entries.size <= 1,
  cornerRadius: Dp = IconButtonDefaults.CornerRadius,
  iconContent: @Composable () -> Unit
) {
  if (rememberUseMiuixOs4()) {
    if (!enabled) {
      IconButton(
        onClick = {},
        enabled = false,
        modifier = modifier,
        minHeight = GlassTopAppBarDefaults.ButtonSize,
        minWidth = GlassTopAppBarDefaults.ButtonSize,
        cornerRadius = GlassTopAppBarDefaults.ButtonSize / 2
      ) {
        iconContent()
      }
      return
    }
    val fallback = LocalMiuixTopBarFallbackContext.current
    val anchorBoundsMap = LocalMiuixMenuAnchorBounds.current
    DisposableEffect(anchorBoundsMap, menuAnchor) {
      onDispose { anchorBoundsMap?.remove(menuAnchor) }
    }
    val floating = fallback?.floating ?: isContentScrolled
    val anchorModifier = modifier
      .onGloballyPositioned { coords ->
        anchorBoundsMap?.set(menuAnchor, coords.boundsInWindow())
      }
      .glassPopupAnchor(
        anchor = menuAnchor,
        cornerRadius = GlassTopAppBarDefaults.ButtonSize / 2,
        floating = floating
      )
    val glassStyle = miuixGlassStyle()
    if (fallback != null) {
      val surfaceAlpha by animateFloatAsState(
        targetValue = if (floating) 1f else 0f,
        animationSpec = spring(
          dampingRatio = 0.85f,
          stiffness = Spring.StiffnessMediumLow,
          visibilityThreshold = 0.0005f
        ),
        label = "topBarFallbackMenuAlpha"
      )
      val showSurface = surfaceAlpha > 0.008f
      GlassIconButton(
        onClick = { onShowChange(true) },
        modifier = anchorModifier,
        inheritTopBarMaterial = false,
        backdrop = if (showSurface) fallback.backdrop else null,
        surfaceAlpha = if (showSurface) surfaceAlpha else 0f,
        style = glassStyle,
        fill = if (showSurface) GlassTopAppBarDefaults.buttonFill() else Color.Transparent,
        stroke = if (showSurface) GlassTopAppBarDefaults.buttonStroke() else null,
        shadow = if (showSurface) GlassShadows.Regular else null
      ) {
        Box(modifier = Modifier.glassPopupAnchorContent(menuAnchor)) {
          iconContent()
        }
      }
    } else {
      GlassIconButton(
        onClick = { onShowChange(true) },
        modifier = anchorModifier,
        style = glassStyle
      ) {
        Box(modifier = Modifier.glassPopupAnchorContent(menuAnchor)) {
          iconContent()
        }
      }
    }
  } else {
    Box(modifier = modifier) {
      IconButton(
        onClick = { onShowChange(true) },
        enabled = enabled,
        holdDownState = show,
        cornerRadius = cornerRadius
      ) {
        iconContent()
      }
      OverlayCascadingListPopup(
        show = show,
        entries = entries,
        collapseOnSelection = collapseOnSelection,
        onDismissRequest = { onShowChange(false) }
      )
    }
  }
}

@Composable
fun BoxScope.AdaptiveGlassOverflowMenu(
  show: Boolean,
  onDismissRequest: () -> Unit,
  anchor: GlassPopupAnchor,
  backdrop: Backdrop?,
  entries: List<DropdownEntry>,
  modifier: Modifier = Modifier,
  collapseOnSelection: Boolean = entries.size <= 1,
  sizing: GlassPopupSizing = GlassPopupSizing(),
  anchorContent: @Composable () -> Unit
) {
  if (!rememberUseMiuixOs4()) return
  val glassStyle = miuixGlassStyle()
  val visuals = GlassPopupDefaults.visuals(style = glassStyle)
  val triggerBounds = LocalMiuixMenuAnchorBounds.current?.get(anchor) ?: Rect.Zero
  AdaptiveGlassCascadingDropdownMenu(
    show = show,
    onDismissRequest = onDismissRequest,
    anchor = anchor,
    triggerBounds = triggerBounds,
    anchorContent = anchorContent,
    entries = entries,
    backdrop = backdrop,
    modifier = modifier,
    secondaryBackdrop = backdrop,
    sizing = sizing,
    visuals = visuals,
    collapseOnSelection = collapseOnSelection
  )
}

@Composable
private fun BoxScope.AdaptiveGlassCascadingDropdownMenu(
  show: Boolean,
  onDismissRequest: () -> Unit,
  anchor: GlassPopupAnchor,
  triggerBounds: Rect,
  anchorContent: @Composable () -> Unit,
  entries: List<DropdownEntry>,
  backdrop: Backdrop?,
  modifier: Modifier = Modifier,
  secondaryBackdrop: Backdrop? = backdrop,
  sizing: GlassPopupSizing = GlassPopupSizing(),
  visuals: GlassPopupVisuals = GlassPopupDefaults.visuals(),
  collapseOnSelection: Boolean = entries.size <= 1
) {
  val density = LocalDensity.current
  var submenu by remember { mutableStateOf(false) }
  var submenuPresent by remember { mutableStateOf(false) }
  var submenuPath by remember { mutableStateOf<Pair<Int, Int>?>(null) }
  var submenuAnchor by remember { mutableStateOf(Rect.Zero) }
  var primaryWidth by remember { mutableStateOf(sizing.minWidth) }
  var hostOrigin by remember { mutableStateOf(Offset.Zero) }
  val groups = remember(entries) { entries.filter { it.items.isNotEmpty() } }
  val submenuChildren = remember(groups) {
    groups.flatMap { group -> group.items }.flatMap { item -> item.children.orEmpty() }
  }
  val containerSize = LocalWindowInfo.current.containerSize
  val bottomInsetsPx = with(density) {
    WindowInsets.navigationBars.getBottom(this) +
      WindowInsets.captionBar.getBottom(this) +
      sizing.safeMargin.roundToPx()
  }
  val secondaryMaxHeight = if (!submenuAnchor.isEmpty) {
    val availableBelowDp = with(density) { (containerSize.height - bottomInsetsPx - submenuAnchor.top).toDp() }
    if (availableBelowDp >= 200.dp) minOf(sizing.maxHeight, availableBelowDp) else sizing.maxHeight
  } else {
    sizing.maxHeight
  }
  val submenuEntry = submenuPath?.let { groups.getOrNull(it.first) }
  val submenuItem = submenuPath?.let { submenuEntry?.items?.getOrNull(it.second) }
  val submenuShown = show && submenu && !submenuItem?.children.isNullOrEmpty()
  val arrowTurn = CascadingPopupDefaults.arrowRotation(LocalLayoutDirection.current)
  val submenuArrow = animateFloatAsState(
    targetValue = if (submenuShown) arrowTurn else 0f,
    animationSpec = CascadingPopupDefaults.arrowSpring(submenuShown),
    label = "adaptiveGlassMenuArrow"
  )
  val menuVisibility = animateFloatAsState(
    targetValue = if (show) 1f else 0f,
    animationSpec = GlassMotion.transformBounds(show),
    label = "adaptiveGlassCascadeVisibility"
  )
  LaunchedEffect(show) {
    if (!show) submenu = false
  }

  val primaryBackdrop = rememberLayerBackdrop()
  val layeredBackdrop = remember(secondaryBackdrop, primaryBackdrop) {
    secondaryBackdrop?.let { scene ->
      object : Backdrop {
        override val isCoordinatesDependent = true
        override val offsetResidualX: Float get() = scene.offsetResidualX
        override val offsetResidualY: Float get() = scene.offsetResidualY

        override fun DrawScope.drawBackdrop(
          density: Density,
          coordinates: LayoutCoordinates?,
          layerBlock: (GraphicsLayerScope.() -> Unit)?,
          downscaleFactor: Float
        ) {
          with(scene) { drawBackdrop(density, coordinates, layerBlock, downscaleFactor) }
          with(primaryBackdrop) { drawBackdrop(density, coordinates, layerBlock, downscaleFactor) }
        }
      }
    }
  }
  val currentOnDismiss by rememberUpdatedState(onDismissRequest)
  val currentCollapseOnSelection by rememberUpdatedState(collapseOnSelection)

  Box(
    Modifier
      .matchParentSize()
      .layerBackdrop(primaryBackdrop)
  ) {
    GlassTransformPopup(
      show = show,
      onDismissRequest = {
        submenu = false
        currentOnDismiss()
      },
      anchor = anchor,
      backdrop = backdrop,
      anchorContent = anchorContent,
      modifier = modifier,
      stacked = submenuShown,
      sizing = sizing,
      visuals = visuals,
      onMeasured = { primaryWidth = with(density) { it.width.toDp() } }
    ) {
      if (submenuChildren.isNotEmpty()) {
        // Reserve max submenu width inside the primary panel so secondary expansion never
        // overflows horizontally and shifts the anchored header/chevron on open or close.
        Layout(
          content = {
            submenuChildren.forEachIndexed { childIndex, child ->
              key(childIndex, child.text) {
                GlassPopupItem(
                  text = child.text,
                  summary = child.summary,
                  onClick = {}
                )
              }
            }
          },
          modifier = Modifier.clearAndSetSemantics { },
          measurePolicy = SubmenuWidthProbeMeasurePolicy
        )
      }
      groups.forEachIndexed { groupIndex, entry ->
        key(groupIndex) {
          entry.items.forEachIndexed { itemIndex, item ->
            key(itemIndex, item.text) {
              val children = item.children.orEmpty()
              var rowBounds by remember(groupIndex, itemIndex) { mutableStateOf(Rect.Zero) }
              GlassPopupItem(
                text = item.text,
                summary = item.summary,
                enabled = entry.enabled && item.enabled,
                selected = item.selected,
                showArrow = children.isNotEmpty(),
                arrowRotation = {
                  if (submenuPath?.first == groupIndex && submenuPath?.second == itemIndex) {
                    submenuArrow.value * (1f - anchor.secondaryBackProgress)
                  } else {
                    0f
                  }
                },
                modifier = if (children.isNotEmpty()) {
                  Modifier.onGloballyPositioned { coordinates ->
                    if (!submenuPresent) rowBounds = coordinates.boundsInWindow()
                  }
                } else {
                  Modifier
                },
                onClick = {
                  if (children.isNotEmpty()) {
                    if (show && !submenuPresent) {
                      submenuAnchor = rowBounds
                      submenuPath = groupIndex to itemIndex
                      submenuPresent = true
                      submenu = true
                    }
                  } else {
                    item.onClick?.invoke()
                    if (currentCollapseOnSelection) {
                      submenu = false
                      currentOnDismiss()
                    }
                  }
                }
              )
            }
          }
          if (groupIndex < groups.lastIndex) {
            HorizontalDivider(
              modifier = Modifier.padding(
                horizontal = GlassPopupDefaults.DividerPaddingHorizontal,
                vertical = GlassPopupDefaults.DividerPaddingVertical
              ),
              thickness = GlassPopupDefaults.DividerThickness
            )
          }
        }
      }
    }
  }
  Box(
    Modifier
      .matchParentSize()
      .onGloballyPositioned { hostOrigin = it.positionInWindow() }
      .graphicsLayer {
        val progress = menuVisibility.value.coerceIn(0f, 1f)
        if (!triggerBounds.isEmpty && primaryWidth > 0.dp) {
          val closedScale = (triggerBounds.width / primaryWidth.toPx()).coerceIn(0f, 1f)
          val scale = closedScale + (1f - closedScale) * progress
          val centerX = triggerBounds.center.x - hostOrigin.x
          val centerY = triggerBounds.center.y - hostOrigin.y
          transformOrigin = TransformOrigin(
            if (size.width > 0f) centerX / size.width else 0.5f,
            if (size.height > 0f) centerY / size.height else 0.5f
          )
          scaleX = scale
          scaleY = scale
        }
        alpha = progress
      }
  ) {
    // Keep the unified width, but retain the library's vertical content padding so the
    // expanded parent row has the same height and baseline as the Example menu.
    GlassSecondaryPopup(
      show = submenuShown,
      onDismissRequest = { submenu = false },
      anchorBounds = submenuAnchor,
      backdrop = layeredBackdrop,
      materialAnchor = anchor,
      sizing = sizing.copy(
        minWidth = primaryWidth,
        maxWidth = primaryWidth,
        maxHeight = secondaryMaxHeight
      ),
      visuals = visuals,
      onDismissFinished = {
        submenuPresent = false
        submenuPath = null
      }
    ) {
      submenuItem?.let { parent ->
        GlassPopupItem(
          text = parent.text,
          summary = parent.summary,
          selected = parent.selected,
          showArrow = true,
          arrowRotation = { submenuArrow.value * (1f - anchor.secondaryBackProgress) },
          onClick = { submenu = false }
        )
        HorizontalDivider(
          modifier = Modifier.padding(
            horizontal = GlassPopupDefaults.DividerPaddingHorizontal,
            vertical = GlassPopupDefaults.DividerPaddingVertical
          ),
          thickness = GlassPopupDefaults.DividerThickness
        )
        parent.children.orEmpty().forEach { item ->
          key(item.text) {
            GlassPopupItem(
              text = item.text,
              summary = item.summary,
              enabled = submenuEntry?.enabled == true && parent.enabled && item.enabled,
              selected = item.selected,
              onClick = {
                item.onClick?.invoke()
                if (currentCollapseOnSelection) {
                  submenu = false
                  currentOnDismiss()
                }
              }
            )
          }
        }
      }
    }
  }
}

private object SubmenuWidthProbeMeasurePolicy : MeasurePolicy {
  override fun MeasureScope.measure(
    measurables: List<Measurable>,
    constraints: Constraints
  ): MeasureResult = layout(0, 0) {}

  override fun IntrinsicMeasureScope.minIntrinsicWidth(
    measurables: List<IntrinsicMeasurable>,
    height: Int
  ): Int = measurables.maxOfOrNull { it.minIntrinsicWidth(height) } ?: 0

  override fun IntrinsicMeasureScope.maxIntrinsicWidth(
    measurables: List<IntrinsicMeasurable>,
    height: Int
  ): Int = measurables.maxOfOrNull { it.maxIntrinsicWidth(height) } ?: 0

  override fun IntrinsicMeasureScope.minIntrinsicHeight(
    measurables: List<IntrinsicMeasurable>,
    width: Int
  ): Int = 0

  override fun IntrinsicMeasureScope.maxIntrinsicHeight(
    measurables: List<IntrinsicMeasurable>,
    width: Int
  ): Int = 0
}

@Composable
fun AdaptiveDropdownPreference(
  items: List<String>,
  selectedIndex: Int,
  title: String,
  onSelectedIndexChange: (Int) -> Unit,
  modifier: Modifier = Modifier,
  backdrop: Backdrop? = LocalMiuixTopBarBackdrop.current,
  enabled: Boolean = true,
  showValue: Boolean = true,
  renderInRootScaffold: Boolean = true,
  summary: String? = null,
  titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
  summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
  startAction: @Composable (() -> Unit)? = null,
  bottomAction: (@Composable () -> Unit)? = null,
  insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
  onExpandedChange: ((Boolean) -> Unit)? = null
) {
  if (rememberUseMiuixOs4()) {
    val visuals = GlassPopupDefaults.visuals(style = miuixGlassStyle())
    GlassDropdownPreference(
      onSelectedIndexChange = onSelectedIndexChange,
      items = items,
      selectedIndex = selectedIndex,
      title = title,
      backdrop = backdrop,
      modifier = modifier,
      enabled = enabled,
      showValue = showValue,
      renderInRootScaffold = renderInRootScaffold,
      titleColor = titleColor,
      summary = summary,
      summaryColor = summaryColor,
      startAction = startAction,
      bottomAction = bottomAction,
      insideMargin = insideMargin,
      visuals = visuals,
      onExpandedChange = onExpandedChange
    )
  } else {
    WindowDropdownPreference(
      items = items,
      selectedIndex = selectedIndex,
      title = title,
      modifier = modifier,
      titleColor = titleColor,
      summary = summary,
      summaryColor = summaryColor,
      startAction = startAction,
      bottomAction = bottomAction,
      insideMargin = insideMargin,
      enabled = enabled,
      showValue = showValue,
      onExpandedChange = onExpandedChange,
      onSelectedIndexChange = onSelectedIndexChange
    )
  }
}

/**
 * Fallback translucent card surface helper when OS4 is unavailable on legacy containers.
 */
internal fun Modifier.fallbackGlassSurface(
  shape: Shape,
  color: Color
): Modifier = this.background(color = color, shape = shape)

/**
 * Attaches vertical overscroll reporting into [ScrollBehavior.state] so even sub-screen-height
 * lists smoothly drive GlassTopAppBar pill floating and progressive blur transitions.
 */
@Composable
fun Modifier.adaptiveVerticalScroll(
  scrollBehavior: ScrollBehavior? = null,
  enableScrollEndHaptic: Boolean = true
): Modifier {
  val state = scrollBehavior?.state
  val onOffsetChanged: ((Float) -> Unit)? = remember(state) {
    state?.let { s -> { offset: Float -> s.overscrollOffset = offset } }
  }
  return this
    .then(if (enableScrollEndHaptic) Modifier.scrollEndHaptic() else Modifier)
    .overScrollVertical(onOffsetChanged = onOffsetChanged)
    .then(if (scrollBehavior != null) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier)
}

/** Localized state labels matching [top.yukonga.miuix.kmp.basic.PullToRefresh] slot order. */
@Composable
fun rememberMiuixRefreshTexts(): List<String> {
  val pullDown = stringResource(R.string.pull_to_refresh_pull_down)
  val release = stringResource(R.string.pull_to_refresh_release)
  val refreshing = stringResource(R.string.pull_to_refresh_refreshing)
  val complete = stringResource(R.string.pull_to_refresh_complete)
  return remember(pullDown, release, refreshing, complete) {
    listOf(pullDown, release, refreshing, complete)
  }
}
