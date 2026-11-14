package com.absinthe.libchecker.ui.compose.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.absinthe.libchecker.ui.compose.util.rememberIsWideScreen
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Top bar that adapts to the window size: phones get the collapsible large-title [TopAppBar], wide
 * screens get the pinned [SmallTopAppBar] because the rail already consumes vertical space.
 */
@Composable
fun AdaptiveTopAppBar(
  title: String,
  subtitle: String = "",
  modifier: Modifier = Modifier,
  color: Color = MiuixTheme.colorScheme.surface,
  scrollBehavior: ScrollBehavior? = null,
  navigationIcon: @Composable () -> Unit = {},
  actions: @Composable RowScope.() -> Unit = {},
  bottomContent: @Composable () -> Unit = {}
) {
  if (rememberIsWideScreen()) {
    SmallTopAppBar(
      title = title,
      subtitle = subtitle,
      modifier = modifier,
      color = color,
      scrollBehavior = scrollBehavior,
      navigationIcon = navigationIcon,
      actions = actions,
      bottomContent = bottomContent
    )
  } else {
    TopAppBar(
      title = title,
      subtitle = subtitle,
      modifier = modifier,
      color = color,
      scrollBehavior = scrollBehavior,
      navigationIcon = navigationIcon,
      actions = actions,
      bottomContent = bottomContent
    )
  }
}
