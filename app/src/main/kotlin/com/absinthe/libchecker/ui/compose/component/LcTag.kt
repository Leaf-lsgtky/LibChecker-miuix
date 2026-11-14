package com.absinthe.libchecker.ui.compose.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Small status tag used by list rows, chips and section headers.
 *
 * One radius, one padding and one text role keep every tag in the app the same size, so a row with
 * a tag no longer changes height depending on which screen drew it.
 */
@Composable
fun LcTag(
  text: String,
  modifier: Modifier = Modifier,
  containerColor: Color = MiuixTheme.colorScheme.surfaceContainerHigh,
  contentColor: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
  horizontalPadding: Int = 6,
  verticalPadding: Int = 2,
  cornerRadius: Int = 6,
  textStyle: androidx.compose.ui.text.TextStyle = MiuixTheme.textStyles.footnote2.copy(fontSize = 10.sp)
) {
  Text(
    text = text,
    modifier = modifier
      .squircleBackground(containerColor, cornerRadius.dp)
      .padding(horizontal = horizontalPadding.dp, vertical = verticalPadding.dp),
    style = textStyle,
    color = contentColor,
    maxLines = 1
  )
}