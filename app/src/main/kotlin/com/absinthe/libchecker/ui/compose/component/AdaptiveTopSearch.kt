package com.absinthe.libchecker.ui.compose.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.home.ui.compose.LocalHomePageActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SearchBarDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.SearchCleanup
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.basic.Search as BasicSearch
import top.yukonga.miuix.kmp.icon.extended.Search as ExtendedSearch
import top.yukonga.miuix.kmp.icon.glass.Close as GlassClose
import top.yukonga.miuix.kmp.icon.glass.Search as GlassSearch

/** Holds query text, expansion state, and explicit open-focus requests for top-bar search. */
@Stable
class TopBarSearchState(
  initialQuery: String = "",
  initialExpanded: Boolean = false
) {
  var query by mutableStateOf(initialQuery)
  var expanded by mutableStateOf(initialExpanded)
  var focusTrigger by mutableIntStateOf(0)
    private set

  fun open() {
    expanded = true
    focusTrigger++
  }

  fun close(clearQuery: Boolean = true) {
    if (clearQuery) {
      query = ""
    }
    expanded = false
  }

  companion object {
    val Saver: Saver<TopBarSearchState, Any> = listSaver(
      save = { listOf(it.query, it.expanded) },
      restore = { saved ->
        TopBarSearchState(
          initialQuery = saved[0] as String,
          initialExpanded = saved[1] as Boolean
        )
      }
    )
  }
}

/** Creates and remembers a [TopBarSearchState] across configuration changes. */
@Composable
fun rememberTopBarSearchState(
  initialQuery: String = "",
  initialExpanded: Boolean = false
): TopBarSearchState = rememberSaveable(saver = TopBarSearchState.Saver) {
  TopBarSearchState(initialQuery = initialQuery, initialExpanded = initialExpanded)
}

/** Configuration passed to [AdaptiveTopAppBar] to support inline top-bar search expansion. */
@Immutable
data class TopBarSearchConfig(
  val state: TopBarSearchState,
  val label: String,
  val onSearch: (String) -> Unit = {}
)

/** Top-bar search action button adapted to OS4 glass or OS3 icons. */
@Composable
fun AdaptiveTopSearchButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true
) {
  val useOs4 = rememberUseMiuixOs4()
  AdaptiveTopBarIconButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled
  ) {
    Icon(
      imageVector = if (useOs4) MiuixIcons.Glass.GlassSearch else MiuixIcons.ExtendedSearch,
      contentDescription = stringResource(R.string.menu_search),
      modifier = if (useOs4) Modifier.size(28.dp) else Modifier
    )
  }
}

/** Expanded top-bar search row paired with optional [bottomContent] under shared blur backdrop. */
private const val SEARCH_TOP_BAR_BLUR_RADIUS = 50f

// Map one second of spring motion onto the 400ms search transition (from miuix GlassSearchOverlay).
internal val SearchModeEasing: Easing = run {
  val omega = 2.0 * PI / 0.75f
  val stiffness = (omega * omega).toFloat()
  val damping = (0.98f * 2.0 * omega).toFloat()
  val decay = -damping / 2f
  val frequency = sqrt(-(damping.toDouble() * damping - stiffness.toDouble() * 4)).toFloat() / 2f
  val sineCoefficient = decay / frequency
  Easing { fraction ->
    (
      1.0 + exp((decay * fraction).toDouble()) *
        (-cos((frequency * fraction).toDouble()) + sineCoefficient * sin((frequency * fraction).toDouble()))
      ).toFloat()
  }
}

@Composable
internal fun rememberGlassSearchBarAlpha(transition: Transition<Boolean>): State<Float> {
  return transition.animateFloat(
    transitionSpec = {
      tween(
        durationMillis = 150,
        easing = if (targetState) CubicBezierEasing(0f, 0f, 0.67f, 1f) else CubicBezierEasing(0.26f, 0f, 0.6f, 0.2f)
      )
    },
    label = "glassSearchTopBarAlpha"
  ) { expanded ->
    if (expanded) 0f else 1f
  }
}

@Composable
internal fun Modifier.glassSearchBarTransition(
  transition: Transition<Boolean>,
  barAlpha: () -> Float
): Modifier {
  val density = LocalDensity.current
  val layerOutset = remember(density) {
    val shadow = GlassShadows.Regular
    val shadowOutset = ((shadow.radius + maxOf(abs(shadow.offsetX), abs(shadow.offsetY))) / GlassDefaults.SourceDensity).dp
    with(density) { shadowOutset.toPx() } + SEARCH_TOP_BAR_BLUR_RADIUS * 3f
  }
  val scale = transition.animateFloat(
    transitionSpec = { tween(durationMillis = 400, easing = SearchModeEasing) },
    label = "glassSearchTopBarScale"
  ) { expanded ->
    if (expanded) 0.9f else 1f
  }
  val blur = transition.animateFloat(
    transitionSpec = { tween(durationMillis = if (targetState) 150 else 120, easing = LinearEasing) },
    label = "glassSearchTopBarBlur"
  ) { expanded ->
    if (expanded) SEARCH_TOP_BAR_BLUR_RADIUS else 0f
  }
  return drawWithCache {
    val layer = obtainGraphicsLayer()
    val halo = ceil(layerOutset).toInt()
    val layerSize = IntSize(
      width = (size.width.toInt() + halo * 2).coerceAtLeast(1),
      height = (size.height.toInt() + halo * 2).coerceAtLeast(1)
    )
    onDrawWithContent {
      val opacity = barAlpha().coerceIn(0f, 1f)
      val currentScale = scale.value
      val currentBlur = blur.value
      if (opacity > 0f) {
        if (opacity == 1f && currentScale == 1f && currentBlur == 0f) {
          drawContent()
        } else {
          layer.record(density = this@drawWithCache, size = layerSize) {
            translate(halo.toFloat(), halo.toFloat()) { this@record.drawContent() }
          }
          layer.alpha = opacity
          layer.scaleX = currentScale
          layer.scaleY = currentScale
          layer.renderEffect = if (currentBlur > 0.5f) {
            BlurEffect(currentBlur, currentBlur, TileMode.Decal)
          } else {
            null
          }
          translate(-halo.toFloat(), -halo.toFloat()) { drawLayer(layer) }
        }
      }
    }
  }
}

/**
 * Full-screen search overlay styled after Miuix's `GlassSearchOverlay` so opening and typing
 * renders a dedicated search results list without scrolling or replacing the underlying main list.
 */
@Composable
fun AdaptiveSearchOverlay(
  config: TopBarSearchConfig,
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.(String) -> Unit
) {
  val state = config.state
  val useOs4 = rememberUseMiuixOs4()
  val isPageActive = LocalHomePageActive.current
  val focusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current
  val keyboard = LocalSoftwareKeyboardController.current
  val transition = updateTransition(state.expanded, label = "adaptiveSearchOverlay")
  val active = state.expanded || transition.currentState || transition.isRunning

  val progress = transition.animateFloat(
    transitionSpec = { tween(durationMillis = 400, easing = SearchModeEasing) },
    label = "glassSearchGeometry"
  ) { expanded ->
    if (expanded) 1f else 0f
  }
  val cancelAlpha = transition.animateFloat(
    transitionSpec = {
      tween(
        durationMillis = 150,
        delayMillis = if (targetState) 100 else 0,
        easing = LinearEasing
      )
    },
    label = "glassSearchCancelMaterial"
  ) { expanded ->
    if (expanded) 1f else 0f
  }

  val dismissSearch = {
    focusManager.clearFocus()
    keyboard?.hide()
    state.close(clearQuery = true)
  }

  LaunchedEffect(state.expanded, state.focusTrigger, isPageActive) {
    if (state.expanded && isPageActive) {
      focusRequester.requestFocus()
      keyboard?.show()
    } else if (!state.expanded) {
      focusManager.clearFocus()
      keyboard?.hide()
    }
  }

  if (!active) return

  NavigationBackHandler(
    state = rememberNavigationEventState(NavigationEventInfo.None),
    isBackEnabled = state.expanded && isPageActive,
    onBackCompleted = dismissSearch
  )

  val colors = MiuixTheme.colorScheme
  val hasQuery = state.query.isNotEmpty()
  val density = LocalDensity.current
  val statusBarOffsetPx = with(density) { 24.dp.toPx() }

  FastScrollLayer(
    modifier = modifier
      .fillMaxSize()
      .background(colors.surface.copy(alpha = progress.value.coerceIn(0f, 1f)))
      .pointerInput(Unit) {
        awaitPointerEventScope {
          while (true) {
            awaitPointerEvent()
          }
        }
      }
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer { alpha = progress.value.coerceIn(0f, 1f) }
        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
        .imePadding()
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .graphicsLayer {
            translationY = -statusBarOffsetPx * (1f - progress.value)
          }
      ) {
        if (useOs4) {
          Os4TopSearchRow(
            query = state.query,
            label = config.label,
            enabled = state.expanded,
            onQueryChange = { state.query = it },
            onSearch = {
              focusManager.clearFocus()
              keyboard?.hide()
              config.onSearch(state.query)
            },
            onClear = {
              state.query = ""
              focusRequester.requestFocus()
              keyboard?.show()
            },
            onDismiss = dismissSearch,
            focusRequester = focusRequester,
            progress = progress.value,
            cancelAlpha = cancelAlpha.value
          )
        } else {
          Os3TopSearchRow(
            query = state.query,
            label = config.label,
            enabled = state.expanded,
            onQueryChange = { state.query = it },
            onSearch = {
              focusManager.clearFocus()
              keyboard?.hide()
              config.onSearch(state.query)
            },
            onClear = {
              state.query = ""
              focusRequester.requestFocus()
              keyboard?.show()
            },
            onDismiss = dismissSearch,
            focusRequester = focusRequester,
            enterProgress = progress.value
          )
        }
      }

      AnimatedContent(
        targetState = hasQuery,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        transitionSpec = {
          fadeIn(tween(durationMillis = 180)) togetherWith fadeOut(tween(durationMillis = 140))
        },
        label = "adaptiveSearchResultsVisibility"
      ) { showingResults ->
        if (showingResults) {
          Box(modifier = Modifier.fillMaxSize()) {
            content(state.query)
          }
        } else {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .clickable(
                interactionSource = null,
                indication = null,
                onClick = dismissSearch
              )
          )
        }
      }
    }
  }
}

@Composable
private fun Os4TopSearchRow(
  query: String,
  label: String,
  enabled: Boolean,
  onQueryChange: (String) -> Unit,
  onSearch: () -> Unit,
  onClear: () -> Unit,
  onDismiss: () -> Unit,
  focusRequester: FocusRequester,
  progress: Float,
  cancelAlpha: Float,
  modifier: Modifier = Modifier
) {
  val colors = MiuixTheme.colorScheme
  val direction = LocalLayoutDirection.current
  val buttonSize = GlassTopAppBarDefaults.ButtonSize
  val endGap = 10.dp
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = GlassTopAppBarDefaults.HorizontalPadding, vertical = 4.dp)
      .graphicsLayer {
        scaleX = 0.96f + 0.04f * progress
        scaleY = 0.96f + 0.04f * progress
      }
  ) {
    BasicTextField(
      value = query,
      onValueChange = onQueryChange,
      enabled = enabled,
      singleLine = true,
      textStyle = MiuixTheme.textStyles.body1.copy(color = colors.onSurface),
      cursorBrush = SolidColor(colors.primary),
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = { onSearch() }),
      modifier = Modifier
        .fillMaxWidth()
        .layout { measurable, constraints ->
          val endSpace = ((buttonSize + endGap).toPx() * progress).toInt()
          val width = (constraints.maxWidth - endSpace).coerceAtLeast(0)
          val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
          layout(constraints.maxWidth, placeable.height) {
            placeable.placeRelative(0, 0)
          }
        }
        .height(buttonSize)
        .focusRequester(focusRequester),
      decorationBox = { innerField ->
        Row(
          modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceContainerHigh, CircleShape)
            .padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = MiuixIcons.Glass.GlassSearch,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = colors.disabledOnSurface
          )
          Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
          ) {
            if (query.isEmpty()) {
              Text(
                text = label,
                color = colors.disabledOnSurface,
                style = MiuixTheme.textStyles.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            innerField()
          }
          AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
          ) {
            IconButton(
              onClick = onClear,
              enabled = enabled,
              minWidth = 24.dp,
              minHeight = 24.dp
            ) {
              Icon(
                imageVector = MiuixIcons.Glass.GlassClose,
                contentDescription = stringResource(R.string.menu_search),
                modifier = Modifier.size(18.dp),
                tint = colors.disabledOnSurface
              )
            }
          }
        }
      }
    )

    Box(
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .graphicsLayer {
          translationX = 66.dp.toPx() * (1f - progress) * if (direction == LayoutDirection.Ltr) 1f else -1f
          scaleX = progress
          scaleY = progress
          alpha = cancelAlpha
          val blur = SEARCH_TOP_BAR_BLUR_RADIUS * (1f - cancelAlpha)
          renderEffect = if (blur > 0.5f) BlurEffect(blur, blur, TileMode.Decal) else null
        }
    ) {
      IconButton(
        onClick = onDismiss,
        enabled = enabled,
        minWidth = buttonSize,
        minHeight = buttonSize,
        cornerRadius = buttonSize / 2,
        backgroundColor = colors.surfaceContainerHigh
      ) {
        Icon(
          imageVector = MiuixIcons.Glass.GlassClose,
          contentDescription = stringResource(android.R.string.cancel),
          modifier = Modifier.size(28.dp),
          tint = colors.onSurface
        )
      }
    }
  }
}

@Composable
private fun Os3TopSearchRow(
  query: String,
  label: String,
  enabled: Boolean,
  onQueryChange: (String) -> Unit,
  onSearch: () -> Unit,
  onClear: () -> Unit,
  onDismiss: () -> Unit,
  focusRequester: FocusRequester,
  enterProgress: Float,
  modifier: Modifier = Modifier
) {
  val colors = MiuixTheme.colorScheme
  val textStyle = MiuixTheme.textStyles.main.copy(
    fontSize = SearchBarDefaults.InputFieldFontSize,
    fontWeight = FontWeight.Medium,
    color = colors.onSurface
  )
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    BasicTextField(
      value = query,
      onValueChange = onQueryChange,
      enabled = enabled,
      singleLine = true,
      textStyle = textStyle,
      cursorBrush = SolidColor(colors.primary),
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = { onSearch() }),
      modifier = Modifier
        .weight(1f)
        .defaultMinSize(minHeight = SearchBarDefaults.InputFieldMinHeight)
        .graphicsLayer {
          alpha = (0.3f + 0.7f * enterProgress).coerceIn(0f, 1f)
          scaleX = 0.94f + 0.06f * enterProgress
        }
        .focusRequester(focusRequester),
      decorationBox = { innerField ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerHigh, CircleShape),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = MiuixIcons.Basic.BasicSearch,
            contentDescription = null,
            tint = colors.onSurfaceContainerHigh,
            modifier = Modifier.padding(
              start = SearchBarDefaults.LeadingIconStartPadding,
              end = SearchBarDefaults.LeadingIconEndPadding
            )
          )
          Box(
            modifier = Modifier
              .weight(1f)
              .heightIn(min = SearchBarDefaults.InputFieldMinHeight),
            contentAlignment = Alignment.CenterStart
          ) {
            if (query.isEmpty()) {
              Text(
                text = label,
                style = textStyle,
                color = colors.onSurfaceContainerHigh,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            innerField()
          }
          AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
          ) {
            Box(
              modifier = Modifier.padding(
                start = SearchBarDefaults.TrailingIconStartPadding,
                end = SearchBarDefaults.TrailingIconEndPadding
              ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = MiuixIcons.Basic.SearchCleanup,
                contentDescription = null,
                tint = colors.onSurfaceContainerHighest,
                modifier = Modifier
                  .clip(CircleShape)
                  .clickable(enabled = enabled, onClick = onClear)
              )
            }
          }
        }
      }
    )
    Spacer(Modifier.width(12.dp))
    Text(
      text = stringResource(android.R.string.cancel),
      fontSize = 17.sp,
      fontWeight = FontWeight.Bold,
      color = colors.primary,
      modifier = Modifier
        .graphicsLayer {
          alpha = enterProgress
          translationX = 16.dp.toPx() * (1f - enterProgress)
        }
        .clickable(
          enabled = enabled,
          interactionSource = null,
          indication = null,
          onClick = onDismiss
        )
        .padding(horizontal = 4.dp, vertical = 8.dp)
    )
  }
}
