@file:Suppress("ktlint:standard:function-naming")

package com.absinthe.libchecker.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi

/** Shares the Miuix adapter with an overlay thumb outside the page's blur sampling layer. */
@OptIn(ExperimentalScrollBarApi::class)
@Composable
fun FastScrollLazyColumn(
  modifier: Modifier = Modifier,
  state: LazyListState = rememberLazyListState(),
  contentPadding: PaddingValues = PaddingValues(0.dp),
  reverseLayout: Boolean = false,
  verticalArrangement: Arrangement.Vertical = if (reverseLayout) Arrangement.Bottom else Arrangement.Top,
  horizontalAlignment: Alignment.Horizontal = Alignment.Start,
  userScrollEnabled: Boolean = true,
  content: LazyListScope.() -> Unit
) {
  val bottomInset = maxOf(
    contentPadding.calculateBottomPadding(),
    LocalHomeBottomPadding.current,
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
  )
  val adapter = rememberScrollBarAdapter(state)
  val entry = remember(state, adapter) { FastScrollEntry(state, adapter) }
  val view = LocalView.current
  // Native sheets use a separate window; keep their thumb in that window.
  val host = LocalFastScrollHost.current?.takeIf { it.view === view }
  SideEffect {
    entry.trackPadding = PaddingValues(top = contentPadding.calculateTopPadding(), bottom = bottomInset)
    entry.enabled = userScrollEnabled
  }
  DisposableEffect(host, entry) {
    host?.entries?.add(entry)
    onDispose { host?.entries?.remove(entry) }
  }
  Box(modifier.onGloballyPositioned { entry.boundsInWindow = it.boundsInWindow() }) {
    LazyColumn(
      modifier = Modifier.fillMaxWidth(),
      state = state,
      contentPadding = contentPadding,
      reverseLayout = reverseLayout,
      verticalArrangement = verticalArrangement,
      horizontalAlignment = horizontalAlignment,
      userScrollEnabled = userScrollEnabled,
      content = content
    )
    if (host == null) {
      // A match-parent overlay does not change wrap-content lists or sheet measurements.
      Box(Modifier.matchParentSize()) {
        PageScrollBar(
          entry = entry,
          topBarBottom = 0f,
          scrollBehavior = null,
          modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
      }
    }
  }
}
