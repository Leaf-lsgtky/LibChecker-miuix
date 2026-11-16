package com.absinthe.libchecker.ui.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.ui.compose.AppAppearanceStore
import com.absinthe.libchecker.ui.compose.LocalHomeAppearance
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * App wide Miuix theme.
 *
 * Uses the stock Miuix palettes; the only override is the AMOLED option, which turns the page
 * background pure black while keeping the card container slightly raised so cards stay visible.
 * Night mode itself is applied by `AppCompatDelegate`, so the controller follows the app dark state.
 */
@Composable
fun LcTheme(content: @Composable () -> Unit) {
  val appearance = AppAppearanceStore.current
  val colorSchemeMode = when (appearance.darkMode) {
    Constants.DARK_MODE_OFF -> ColorSchemeMode.Light
    Constants.DARK_MODE_ON -> ColorSchemeMode.Dark
    else -> ColorSchemeMode.System
  }
  val amoled = appearance.amoledTheme
  val controller = remember(colorSchemeMode, amoled) {
    ThemeController(
      colorSchemeMode = colorSchemeMode,
      lightColors = lightColorScheme(),
      darkColors = if (amoled) AmoledColors else darkColorScheme()
    )
  }
  CompositionLocalProvider(LocalHomeAppearance provides appearance) {
    MiuixTheme(controller = controller, content = content)
  }
}

private val AmoledColors = darkColorScheme(
  background = Color.Black,
  surface = Color.Black
)
