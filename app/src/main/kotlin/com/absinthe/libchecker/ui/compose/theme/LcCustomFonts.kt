package com.absinthe.libchecker.ui.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.absinthe.libchecker.ui.compose.LocalHomeAppearance

/**
 * Classic LibChecker font families matching the legacy view styles (`sans-serif-condensed`,
 * `sans-serif-condensed-medium`, `sans-serif-black`, and `monospace`) used for technical metadata,
 * component paths, version details, and dashboard counters when the user enables custom fonts.
 */
object LcCustomFontFamilies {
  private val condensedName = DeviceFontFamilyName("sans-serif-condensed")
  private val condensedMediumName = DeviceFontFamilyName("sans-serif-condensed-medium")
  private val blackName = DeviceFontFamilyName("sans-serif-black")

  val Condensed: FontFamily = FontFamily(
    Font(condensedName, FontWeight.Normal),
    Font(condensedMediumName, FontWeight.Medium),
    Font(condensedMediumName, FontWeight.SemiBold),
    Font(condensedName, FontWeight.Bold)
  )

  val Black: FontFamily = FontFamily(
    Font(blackName, FontWeight.Normal),
    Font(blackName, FontWeight.Medium),
    Font(blackName, FontWeight.SemiBold),
    Font(blackName, FontWeight.Bold),
    Font(blackName, FontWeight.ExtraBold),
    Font(blackName, FontWeight.Black)
  )
}

@Composable
fun rememberUseCustomFont(): Boolean = LocalHomeAppearance.current.useCustomFont

@Composable
fun lcCondensedFontFamily(): FontFamily? = if (rememberUseCustomFont()) LcCustomFontFamilies.Condensed else null

@Composable
fun lcBlackFontFamily(): FontFamily? = if (rememberUseCustomFont()) LcCustomFontFamilies.Black else null

@Composable
fun lcMonospaceFontFamily(): FontFamily? = if (rememberUseCustomFont()) FontFamily.Monospace else null

fun TextStyle.withCustomFont(fontFamily: FontFamily?): TextStyle = if (fontFamily != null) copy(fontFamily = fontFamily) else this
