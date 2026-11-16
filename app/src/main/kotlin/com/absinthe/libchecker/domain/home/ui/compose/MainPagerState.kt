package com.absinthe.libchecker.domain.home.ui.compose

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.utils.springAnimateToPage

/** Coordinates horizontal pager swipes and bottom-bar or rail tab switches with spring motion. */
@Stable
class MainPagerState(
  val pagerState: PagerState,
  private val coroutineScope: CoroutineScope,
  private val animatePageChanges: Boolean = true
) {
  var selectedPage by mutableIntStateOf(pagerState.currentPage)
    private set

  var isNavigating by mutableStateOf(false)
    private set

  private var navJob: Job? = null

  fun animateToPage(targetIndex: Int) {
    val maxIndex = (pagerState.pageCount - 1).coerceAtLeast(0)
    val clamped = targetIndex.coerceIn(0, maxIndex)
    if (clamped == selectedPage) return

    navJob?.cancel()
    selectedPage = clamped
    isNavigating = true

    navJob = coroutineScope.launch {
      val myJob = coroutineContext.job
      try {
        if (animatePageChanges) {
          pagerState.springAnimateToPage(clamped)
        } else {
          pagerState.scrollToPage(clamped)
        }
      } finally {
        if (navJob == myJob) {
          isNavigating = false
          if (pagerState.currentPage != clamped) {
            selectedPage = pagerState.currentPage
          }
        }
      }
    }
  }

  fun syncPage() {
    if (!isNavigating && selectedPage != pagerState.currentPage) {
      selectedPage = pagerState.currentPage
    }
  }
}

/** Creates and remembers a [MainPagerState] bound to [pagerState]. */
@Composable
fun rememberMainPagerState(
  pagerState: PagerState,
  coroutineScope: CoroutineScope = rememberCoroutineScope(),
  animatePageChanges: Boolean = true
): MainPagerState = remember(pagerState, coroutineScope, animatePageChanges) {
  MainPagerState(pagerState, coroutineScope, animatePageChanges)
}

/**
 * Whether the hosting home tab is currently settled in front of the user; defaults to `true` for
 * standalone secondary destinations outside [HomeScaffold].
 */
val LocalHomePageActive = compositionLocalOf { true }
