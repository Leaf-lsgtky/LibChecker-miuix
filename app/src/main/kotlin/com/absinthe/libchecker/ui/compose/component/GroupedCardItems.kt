package com.absinthe.libchecker.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * One segment of a visually continuous card. Splitting a card into independent lazy items keeps
 * scrolling cheap: only visible rows are composed. Cornered segments clip their content so ripples
 * stay inside the card radius; middle segments use a plain background (no offscreen layer).
 */
@Composable
fun CardSegment(
  isFirst: Boolean,
  isLast: Boolean,
  modifier: Modifier = Modifier,
  color: Color = MiuixTheme.colorScheme.surfaceContainer,
  contentColor: Color = MiuixTheme.colorScheme.onSurfaceContainer,
  cornerRadius: Dp = 16.dp,
  topCornerRadius: Dp = if (isFirst) cornerRadius else 0.dp,
  bottomCornerRadius: Dp = if (isLast) cornerRadius else 0.dp,
  outerHorizontalPadding: Dp = 12.dp,
  outerTopPadding: Dp = 0.dp,
  outerBottomPadding: Dp = 0.dp,
  insidePadding: PaddingValues = PaddingValues(0.dp),
  content: @Composable ColumnScope.() -> Unit
) {
  val background = if (topCornerRadius == 0.dp && bottomCornerRadius == 0.dp) {
    Modifier.background(color)
  } else {
    Modifier.squircleSurface(color, topCornerRadius, topCornerRadius, bottomCornerRadius, bottomCornerRadius)
  }
  CompositionLocalProvider(LocalContentColor provides contentColor) {
    Column(
      modifier = modifier
        .fillMaxWidth()
        .padding(
          start = outerHorizontalPadding,
          end = outerHorizontalPadding,
          top = outerTopPadding,
          bottom = outerBottomPadding
        )
        .then(background)
        .padding(insidePadding),
      content = content
    )
  }
}

/** One row definition for [groupedCardItems]: a stable key plus its content. */
class CardItem(
  val key: String,
  val content: @Composable ColumnScope.() -> Unit
)

/**
 * Emits each row of a card as its own lazy item while keeping the rounded card silhouette.
 */
fun LazyListScope.groupedCardItems(
  keyPrefix: String,
  items: List<CardItem>,
  outerTopPadding: Dp = 0.dp,
  outerBottomPadding: Dp = 6.dp,
  outerHorizontalPadding: Dp = 12.dp,
  insidePadding: PaddingValues = PaddingValues(0.dp)
) {
  if (items.isEmpty()) return
  val lastIndex = items.lastIndex
  items.forEachIndexed { index, cardItem ->
    item(key = "$keyPrefix:${cardItem.key}") {
      CardSegment(
        isFirst = index == 0,
        isLast = index == lastIndex,
        outerHorizontalPadding = outerHorizontalPadding,
        outerTopPadding = if (index == 0) outerTopPadding else 0.dp,
        outerBottomPadding = if (index == lastIndex) outerBottomPadding else 0.dp,
        insidePadding = insidePadding,
        content = cardItem.content
      )
    }
  }
}
