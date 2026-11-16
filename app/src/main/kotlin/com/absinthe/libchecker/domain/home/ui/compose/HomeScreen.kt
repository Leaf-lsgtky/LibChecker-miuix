package com.absinthe.libchecker.domain.home.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.absinthe.libchecker.R
import com.absinthe.libchecker.ui.compose.LocalHomeAppearance
import com.absinthe.libchecker.ui.compose.component.LocalHomeBottomPadding
import com.absinthe.libchecker.ui.compose.component.miuixGlassStyle
import com.absinthe.libchecker.ui.compose.component.rememberMiuixBlurBackdrop
import com.absinthe.libchecker.ui.compose.component.rememberUseMiuixBlur
import com.absinthe.libchecker.ui.compose.component.rememberUseMiuixOs4
import com.absinthe.libchecker.ui.compose.util.rememberIsWideScreen
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarDefaults
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationBarDefaults
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.Sidebar
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerGestureNestedScrollConnection
import top.yukonga.miuix.kmp.utils.PagerInterceptionMode
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.pagerGestureOverride
import top.yukonga.miuix.kmp.icon.extended.Search as ExtendedSearch
import top.yukonga.miuix.kmp.icon.glass.Search as GlassSearch

/**
 * Root shell of the app: an adaptive OS4 [GlassNavigationBar], OS3 [FloatingNavigationBar] /
 * [NavigationBar] on phones, an expandable [NavigationRail] on wide screens, and a swipeable
 * [HorizontalPager] synchronized with spring tab transitions.
 *
 * Each tab renders its own top app bar and search results. Search state is shared with the shell
 * so the optional bottom action opens the active tab's search and hides navigation while searching.
 */
@Composable
fun HomeScaffold(
  mainPagerState: MainPagerState,
  modifier: Modifier = Modifier,
  onTabReselected: (HomeTab) -> Unit = {},
  onSearchRequested: () -> Unit = {},
  searchExpanded: Boolean = false,
  userScrollEnabled: Boolean = true,
  content: @Composable (HomeTab, Boolean) -> Unit
) {
  val tabs = HomeTab.entries
  val pagerState = mainPagerState.pagerState
  val selectedIndex = mainPagerState.selectedPage.coerceIn(0, tabs.lastIndex)
  val tabLabels = tabs.map { stringResource(it.titleRes) }

  LaunchedEffect(pagerState.currentPage, pagerState.settledPage) {
    mainPagerState.syncPage()
  }

  val selectTab: (Int) -> Unit = { index ->
    tabs.getOrNull(index)?.let { tab ->
      if (index == selectedIndex && index == pagerState.currentPage) {
        onTabReselected(tab)
      } else {
        mainPagerState.animateToPage(index)
      }
    }
  }

  NavigationBackHandler(
    state = rememberNavigationEventState(NavigationEventInfo.None),
    isBackEnabled = selectedIndex != 0,
    onBackCompleted = { mainPagerState.animateToPage(0) }
  )

  var contentReady by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    withFrameNanos { }
    contentReady = true
  }

  val isWideScreen = rememberIsWideScreen()
  val appearance = LocalHomeAppearance.current
  val useOs4 = rememberUseMiuixOs4()
  val useBlur = rememberUseMiuixBlur()
  val glassStyle = miuixGlassStyle()
  val navBackdrop = rememberMiuixBlurBackdrop(enabled = !isWideScreen && useBlur)
  val edgeToEdgeContent = !isWideScreen && (useOs4 || appearance.floatingNavBar || navBackdrop != null)
  val bottomBarSearchAction: (() -> Unit)? = if (
    appearance.bottomBarSearch && selectedIndex != HomeTab.SETTINGS.ordinal
  ) {
    onSearchRequested
  } else {
    null
  }
  var railExpanded by rememberSaveable { mutableStateOf(false) }
  val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
  val navigationVisible = !imeVisible && !searchExpanded

  Scaffold(
    modifier = modifier,
    containerColor = MiuixTheme.colorScheme.surface,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    bottomBar = {
      if (!isWideScreen) {
        if (useOs4) {
          val navItems = remember(tabLabels) {
            tabs.mapIndexed { index, tab ->
              GlassNavigationItem(
                icon = tab.glassIcon,
                label = tabLabels[index]
              )
            }
          }
          GlassNavigationBar(
            items = navItems,
            selectedIndex = selectedIndex,
            onSelect = selectTab,
            backdrop = navBackdrop,
            modifier = Modifier.padding(horizontal = 20.dp),
            visible = navigationVisible,
            style = glassStyle,
            trailingAction = bottomBarSearchAction?.let { action ->
              {
                GlassIconButton(
                  onClick = action,
                  backdrop = navBackdrop,
                  size = GlassNavigationBarDefaults.Height,
                  style = glassStyle,
                  shadow = GlassShadows.Float
                ) {
                  Icon(
                    imageVector = MiuixIcons.Glass.GlassSearch,
                    contentDescription = stringResource(R.string.menu_search),
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp)
                  )
                }
              }
            }
          )
        } else {
          AnimatedVisibility(
            visible = navigationVisible,
            enter = slideInVertically(tween(260)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(160))
          ) {
            if (appearance.floatingNavBar) {
              Box(Modifier.fillMaxWidth()) {
                FloatingNavigationBar {
                  tabs.forEachIndexed { index, tab ->
                    FloatingNavigationBarItem(
                      selected = index == selectedIndex,
                      onClick = { selectTab(index) },
                      icon = tab.icon,
                      label = tabLabels[index]
                    )
                  }
                  bottomBarSearchAction?.let { action ->
                    HomeBottomBarSearchButton(onClick = action)
                  }
                }
              }
            } else {
              val blurNavModifier = if (navBackdrop != null) {
                val navColors = BlurDefaults.blurColors(
                  blendColors = listOf(
                    BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(alpha = 0.8f))
                  )
                )
                Modifier.textureBlur(
                  backdrop = navBackdrop,
                  shape = RectangleShape,
                  blurRadius = 25f,
                  colors = navColors
                )
              } else {
                Modifier
              }
              NavigationBar(
                color = if (navBackdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
                modifier = blurNavModifier
              ) {
                tabs.forEachIndexed { index, tab ->
                  NavigationBarItem(
                    selected = index == selectedIndex,
                    onClick = { selectTab(index) },
                    icon = tab.icon,
                    label = tabLabels[index]
                  )
                }
                bottomBarSearchAction?.let { action ->
                  HomeBottomBarSearchButton(
                    onClick = action,
                    modifier = Modifier.padding(start = 4.dp, end = 12.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  ) { padding ->
    Row(Modifier.fillMaxSize()) {
      if (isWideScreen) {
        NavigationRail(
          expanded = railExpanded,
          header = {
            IconButton(onClick = { railExpanded = !railExpanded }) {
              Icon(
                imageVector = MiuixIcons.Glass.Sidebar,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceContainer
              )
            }
          }
        ) {
          Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            tabs.forEachIndexed { index, tab ->
              NavigationRailItem(
                selected = index == selectedIndex,
                onClick = { selectTab(index) },
                icon = tab.icon,
                label = tabLabels[index]
              )
            }
          }
        }
      }
      val contentBottomPadding = if (edgeToEdgeContent) padding.calculateBottomPadding() else 0.dp
      val contentBoxModifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .let { if (edgeToEdgeContent) it else it.padding(bottom = padding.calculateBottomPadding()) }
        .let { if (navBackdrop != null) it.layerBackdrop(navBackdrop) else it }
      val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = PagerNavigationSpringSpec
      )
      CompositionLocalProvider(LocalHomeBottomPadding provides contentBottomPadding) {
        Box(modifier = contentBoxModifier) {
          HorizontalPager(
            state = pagerState,
            modifier = Modifier
              .fillMaxSize()
              .pagerGestureOverride(
                pagerState = pagerState,
                flingBehavior = flingBehavior,
                mode = PagerInterceptionMode.CrossAxis,
                enabled = userScrollEnabled
              ),
            flingBehavior = flingBehavior,
            beyondViewportPageCount = if (contentReady) tabs.lastIndex else 0,
            userScrollEnabled = false,
            overscrollEffect = null,
            pageNestedScrollConnection = PagerGestureNestedScrollConnection,
            key = { page -> tabs[page].name }
          ) { page ->
            val tab = tabs[page]
            val isCurrentPage = page == pagerState.settledPage
            CompositionLocalProvider(LocalHomePageActive provides isCurrentPage) {
              Box(modifier = Modifier.fillMaxSize()) {
                content(tab, isCurrentPage)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun HomeBottomBarSearchButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  IconButton(
    onClick = onClick,
    modifier = modifier,
    minWidth = 48.dp,
    minHeight = 48.dp,
    cornerRadius = 24.dp,
    backgroundColor = MiuixTheme.colorScheme.surfaceContainer
  ) {
    Icon(
      imageVector = MiuixIcons.ExtendedSearch,
      contentDescription = stringResource(R.string.menu_search),
      tint = MiuixTheme.colorScheme.onSurfaceContainer,
      modifier = Modifier.size(FloatingNavigationBarDefaults.IconSize)
    )
  }
}
