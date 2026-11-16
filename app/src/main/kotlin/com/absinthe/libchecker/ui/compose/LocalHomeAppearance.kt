package com.absinthe.libchecker.ui.compose

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.absinthe.libchecker.constant.GlobalValues

/**
 * Transient shell preferences that must trigger recomposition when changed from the settings
 * screen. Persisted values still live in [GlobalValues].
 */
@Stable
class HomeAppearanceState {
  var floatingNavBar by mutableStateOf(GlobalValues.isFloatingNavBar)
  var blurDesign by mutableStateOf(GlobalValues.isBlurDesign)
  var miuixOs4 by mutableStateOf(GlobalValues.isMiuixOs4)
  var useCustomFont by mutableStateOf(GlobalValues.isUseCustomFont)
  var bottomBarSearch by mutableStateOf(GlobalValues.isBottomBarSearch)
  var darkMode by mutableStateOf(GlobalValues.darkMode)
  var amoledTheme by mutableStateOf(GlobalValues.isAmoledTheme)
}

object AppAppearanceStore {
  val current = HomeAppearanceState()
}

val LocalHomeAppearance = staticCompositionLocalOf { AppAppearanceStore.current }
