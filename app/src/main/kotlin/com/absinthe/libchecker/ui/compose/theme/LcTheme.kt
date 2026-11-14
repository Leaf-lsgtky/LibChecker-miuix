package com.absinthe.libchecker.ui.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.absinthe.libchecker.constant.GlobalValues
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
  val amoled = GlobalValues.isAmoledTheme
  val controller = remember(amoled) {
    ThemeController(
      colorSchemeMode = ColorSchemeMode.System,
      lightColors = lightColorScheme(),
      darkColors = if (amoled) AmoledColors else darkColorScheme()
    )
  }
  MiuixTheme(controller = controller, content = content)
}

private val AmoledColors = darkColorScheme(
  background = Color.Black,
  surface = Color.Black
)