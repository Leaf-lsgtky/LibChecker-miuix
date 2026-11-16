@file:Suppress("ktlint:standard:function-naming")

package com.absinthe.libchecker.domain.snapshot.detail.ui.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.SnapshotDetailRow
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.interactionPolicy
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.ui.compose.theme.lcCondensedFontFamily
import com.absinthe.libchecker.ui.compose.theme.withCustomFont
import com.absinthe.libchecker.ui.compose.util.toAnnotatedString
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CompactSnapshotDetailSectionHeader(
  section: SnapshotDetailSection,
  collapsed: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = MiuixTheme.colorScheme
  val font = lcCondensedFontFamily()
  val expandedDescription = stringResource(
    if (collapsed) R.string.a11y_state_collapsed else R.string.a11y_state_expanded
  )
  val rotation by animateFloatAsState(if (collapsed) 0f else 90f, label = "snapshot-section")
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onToggle)
      .semantics {
        heading()
        stateDescription = expandedDescription
      }
      .padding(horizontal = 24.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(
      text = section.title,
      modifier = Modifier.weight(1f),
      style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
      color = colors.onSurfaceVariantSummary
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      section.statusCounts.forEach { count ->
        val palette = snapshotStatusColors(count.diffType)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
          AsyncImage(
            model = count.status.iconRes,
            contentDescription = count.label,
            colorFilter = ColorFilter.tint(palette.ink),
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = count.countText,
            style = MiuixTheme.textStyles.footnote2.copy(
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              fontFeatureSettings = "tnum"
            ).withCustomFont(font),
            color = palette.ink
          )
        }
      }
    }
    Icon(
      imageVector = MiuixIcons.Glass.ChevronForward,
      contentDescription = null,
      tint = colors.onSurfaceVariantActions,
      modifier = Modifier.size(14.dp).graphicsLayer { rotationZ = rotation }
    )
  }
}

@Composable
internal fun CompactSnapshotDetailItemRow(
  item: SnapshotDetailItemDisplayData,
  packageName: String,
  isFirst: Boolean,
  isLast: Boolean,
  onItemClick: ((SnapshotDetailItemDisplayData) -> Unit)?,
  onItemLongClick: ((SnapshotDetailItemDisplayData) -> Unit)?,
  onChipClick: (SnapshotDetailItemDisplayData) -> Unit
) {
  val colors = MiuixTheme.colorScheme
  val font = lcCondensedFontFamily()
  val palette = snapshotStatusColors(item.item.diffType)
  val classCountLabel = stringResource(R.string.snapshot_detail_class_count_label)
  val content = remember(item.extra, item.item.itemType, classCountLabel) {
    buildCompactSnapshotDetailText(item, classCountLabel)
  }
  val previousName = item.item.previousName.takeIf { item.item.diffType == MOVED }
  val title = remember(item.title, item.item.name, previousName) {
    (if (previousName != null) AnnotatedString(item.item.name) else item.title.toAnnotatedString())
      .withTechnicalBreaks()
  }
  val previousPath = remember(previousName) { previousName?.let { AnnotatedString(it).withTechnicalBreaks() } }
  val policy = remember(item, packageName) {
    SnapshotDetailRow.Item(item.item.itemType, item).interactionPolicy(packageName)
  }
  val statusDescription = stringResource(item.status.labelRes)
  val canClick = onItemClick != null && policy.opensDetail
  val canLongClick = onItemLongClick != null && policy.opensReference

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .combinedClickable(
        enabled = canClick || canLongClick,
        onClick = { if (canClick) onItemClick.invoke(item) },
        onLongClick = if (canLongClick) ({ onItemLongClick.invoke(item) }) else null
      )
      .semantics { stateDescription = statusDescription }
      .snapshotDetailItemBackground(palette, isFirst, isLast)
      .padding(
        start = 18.dp,
        end = 12.dp,
        top = if (isFirst) 12.dp else 10.dp,
        bottom = if (isLast) 12.dp else 10.dp
      )
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.Top
    ) {
      Text(
        text = title,
        modifier = Modifier.weight(1f),
        style = MiuixTheme.textStyles.body2.copy(
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          lineHeight = 20.sp
        ).withCustomFont(font),
        color = colors.onSurface
      )
      val delta = content.headlineDelta
      if (delta != null) {
        Text(
          text = delta,
          modifier = Modifier.widthIn(max = 140.dp),
          style = MiuixTheme.textStyles.body1.copy(fontFeatureSettings = "tnum").withCustomFont(font),
          color = colors.onSurface
        )
      } else {
        AsyncImage(
          model = item.status.iconRes,
          contentDescription = null,
          colorFilter = ColorFilter.tint(palette.ink),
          modifier = Modifier.padding(top = 3.dp).size(15.dp)
        )
      }
    }

    if (content.metrics.isNotEmpty()) {
      Spacer(Modifier.height(5.dp))
      content.metrics.forEachIndexed { index, metric ->
        if (index > 0) Spacer(Modifier.height(2.dp))
        val summary = listOfNotNull(
          metric.delta.takeUnless { index == 0 && content.headlineDelta != null },
          metric.percentage
        ).joinToString(" · ")
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          Text(
            text = metric.text,
            modifier = Modifier.padding(end = 10.dp),
            style = MiuixTheme.textStyles.footnote1.copy(
              fontSize = 12.5.sp,
              lineHeight = 17.sp,
              fontFeatureSettings = "tnum"
            ).withCustomFont(font),
            color = colors.onSurfaceVariantSummary
          )
          if (summary.isNotEmpty()) {
            Text(
              text = summary,
              style = MiuixTheme.textStyles.footnote2.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum"
              ).withCustomFont(font),
              color = colors.onSurfaceVariantSummary
            )
          }
        }
      }
    }

    if (previousPath != null) {
      Spacer(Modifier.height(7.dp))
      Text(
        text = stringResource(R.string.snapshot_detail_previous_path),
        style = MiuixTheme.textStyles.footnote2.copy(fontSize = 11.sp),
        color = colors.onSurfaceVariantSummary
      )
      Text(
        text = previousPath,
        style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp, lineHeight = 20.sp).withCustomFont(font),
        color = colors.onSurfaceVariantSummary
      )
    }

    item.ruleChip?.let { chip ->
      Spacer(Modifier.height(3.dp))
      val iconFilter = when {
        chip.isSimpleColorIcon -> ColorFilter.tint(colors.onSurfaceVariantSummary)
        !chip.useColorfulIcon -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        else -> null
      }
      Row(
        modifier = Modifier
          .clickable { onChipClick(item) }
          .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        AsyncImage(
          model = chip.iconRes,
          contentDescription = null,
          colorFilter = iconFilter,
          modifier = Modifier.size(14.dp)
        )
        Text(
          text = chip.label,
          style = MiuixTheme.textStyles.footnote2.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
          color = colors.onSurfaceVariantSummary
        )
      }
    }
  }
}

private fun Modifier.snapshotDetailItemBackground(
  palette: SnapshotStatusPalette,
  isFirst: Boolean,
  isLast: Boolean
): Modifier = drawWithCache {
  val barWidth = 3.dp.toPx()
  val barLeft = 8.dp.toPx()
  val gradientLeft = barLeft + barWidth / 2f
  val topInset = if (isFirst) 8.dp.toPx() else 4.dp.toPx()
  val bottomInset = if (isLast) 8.dp.toPx() else 4.dp.toPx()
  val height = (size.height - topInset - bottomInset).coerceAtLeast(0f)
  val brush = Brush.horizontalGradient(
    0f to palette.rowGradientStart,
    0.55f to palette.rowGradientStart.copy(alpha = palette.rowGradientStart.alpha * 0.28f),
    1f to Color.Transparent,
    startX = gradientLeft,
    endX = size.width
  )
  onDrawBehind {
    if (height > 0f && size.width > gradientLeft) {
      drawRect(
        brush = brush,
        topLeft = Offset(gradientLeft, topInset),
        size = Size(size.width - gradientLeft, height)
      )
      drawRoundRect(
        color = palette.bar,
        topLeft = Offset(barLeft, topInset),
        size = Size(barWidth, height),
        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
      )
    }
  }
}
