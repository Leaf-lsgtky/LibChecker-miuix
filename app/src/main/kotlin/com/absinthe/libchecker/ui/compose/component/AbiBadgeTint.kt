package com.absinthe.libchecker.ui.compose.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Tint for an ABI badge, shared by the app list rows and the app detail header.
 *
 * Mirrors the view-system tinting: turning the ABI label option on paints the badges with the theme
 * accent, otherwise they stay on the summary surface colour. Miuix exposes a single accent role, so
 * every tinted badge uses it and callers dim the inactive ones with alpha.
 */
@Composable
fun resolveAbiBadgeTint(tintAbiLabels: Boolean): Color {
  val colorScheme = MiuixTheme.colorScheme
  return if (tintAbiLabels) colorScheme.primary else colorScheme.onSurfaceVariantSummary
}
