package com.absinthe.libchecker.domain.home.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.absinthe.libchecker.ui.compose.LocalHomeAppearance
import com.absinthe.libchecker.ui.compose.util.rememberIsWideScreen
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Sidebar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Root shell of the app: a bottom [NavigationBar] on phones, an expandable [NavigationRail] on wide
 * screens, and a directional fade/slide transition between tabs.
 *
 * The shell owns no page chrome; each tab renders its own top app bar so search/actions can differ
 * per page without hoisting transient UI state.
 */
@Composable
fun HomeScaffold(
  selectedTab: HomeTab,
  onTabSelected: (HomeTab) -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable (HomeTab) -> Unit
) {
  val isWideScreen = rememberIsWideScreen()
  var railExpanded by rememberSaveable { mutableStateOf(false) }
  val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

  Scaffold(
    modifier = modifier,
    containerColor = MiuixTheme.colorScheme.surface,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    bottomBar = {
      if (!isWideScreen) {
        AnimatedVisibility(
          visible = !imeVisible,
          enter = slideInVertically(tween(260)) { it } + fadeIn(tween(200)),
          exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(160))
        ) {
          if (LocalHomeAppearance.current.floatingNavBar) {
            Box(Modifier.fillMaxWidth()) {
              FloatingNavigationBar {
                HomeTab.entries.forEach { tab ->
                  FloatingNavigationBarItem(
                    selected = tab == selectedTab,
                    onClick = { onTabSelected(tab) },
                    icon = tab.icon,
                    label = stringResource(tab.titleRes)
                  )
                }
              }
            }
          } else {
            NavigationBar {
              HomeTab.entries.forEach { tab ->
                NavigationBarItem(
                  selected = tab == selectedTab,
                  onClick = { onTabSelected(tab) },
                  icon = tab.icon,
                  label = stringResource(tab.titleRes)
                )
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
                imageVector = MiuixIcons.Sidebar,
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
            HomeTab.entries.forEach { tab ->
              NavigationRailItem(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                icon = tab.icon,
                label = stringResource(tab.titleRes)
              )
            }
          }
        }
      }
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .padding(bottom = padding.calculateBottomPadding())
      ) {
        AnimatedContent(
          targetState = selectedTab,
          transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            val enterOffset: (Int) -> Int = { if (forward) it / 10 else -it / 10 }
            val exitOffset: (Int) -> Int = { if (forward) -it / 14 else it / 14 }
            (fadeIn(tween(220)) + slideInHorizontally(tween(280), enterOffset)) togetherWith
              (fadeOut(tween(140)) + slideOutHorizontally(tween(220), exitOffset))
          },
          label = "home-tab"
        ) { tab ->
          content(tab)
        }
      }
    }
  }
}