package com.absinthe.libchecker.domain.snapshot.detail.ui.compose

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.ui.compose.LibDetailSheet
import com.absinthe.libchecker.domain.app.list.ui.compose.AppIcon
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitleDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.SnapshotDetailRow
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.interactionPolicy
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.CHANGED
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.REMOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.ui.compose.component.AdaptiveBackButton
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopBarIconButton
import com.absinthe.libchecker.ui.compose.component.CardSegment
import com.absinthe.libchecker.ui.compose.component.LcEmptyState
import com.absinthe.libchecker.ui.compose.component.MiuixPageHost
import com.absinthe.libchecker.ui.compose.component.adaptiveVerticalScroll
import com.absinthe.libchecker.ui.compose.theme.lcCondensedFontFamily
import com.absinthe.libchecker.ui.compose.theme.withCustomFont
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import com.absinthe.libchecker.ui.compose.util.rememberActiveValue
import com.absinthe.libchecker.ui.compose.util.toAnnotatedString
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.ChevronForward
import top.yukonga.miuix.kmp.icon.glass.Share
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.absinthe.libchecker.ui.compose.component.FastScrollLazyColumn as LazyColumn

/** Snapshot diff detail for one app: title summary card plus Miuix-styled collapsible sections. */
@Composable
fun SnapshotDetailScreen(
  title: SnapshotTitleDisplayData,
  sections: List<SnapshotDetailSection>,
  onBack: () -> Unit,
  onShare: () -> Unit,
  modifier: Modifier = Modifier,
  packageName: String = "",
  detailViewModel: DetailViewModel? = null,
  onTitleClick: (() -> Unit)? = null,
  onItemClick: ((SnapshotDetailItemDisplayData) -> Unit)? = null,
  onItemLongClick: ((SnapshotDetailItemDisplayData) -> Unit)? = null,
  listState: LazyListState = rememberLazyListState(),
  iconSource: SnapshotPackageIconSource? = null,
  fallbackIcon: Bitmap? = null,
  legacyDetail: Boolean = false
) {
  val colorScheme = MiuixTheme.colorScheme
  val scrollBehavior = MiuixScrollBehavior()
  var collapsedSections by remember { mutableStateOf(setOf<Int>()) }
  var selectedRuleItem by remember { mutableStateOf<SnapshotDetailItemDisplayData?>(null) }
  val isScrolled = listState.canScrollBackward
  val showCollapsedTitle by remember(listState) {
    derivedStateOf {
      listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 120
    }
  }

  MiuixPageHost(modifier = modifier.fillMaxSize()) {
    val pageState = this
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = colorScheme.surface,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      topBar = {
        AdaptiveTopAppBar(
          title = if (showCollapsedTitle) title.appName.toString() else "",
          subtitle = "",
          scrollBehavior = scrollBehavior,
          isContentScrolled = isScrolled,
          pinned = true,
          navigationIcon = {
            AdaptiveBackButton(onClick = onBack)
          },
          actions = {
            AdaptiveTopBarIconButton(onClick = onShare) {
              Icon(
                imageVector = MiuixIcons.Glass.Share,
                contentDescription = stringResource(R.string.snapshot_generate_text_report)
              )
            }
          }
        )
      }
    ) { padding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .then(pageState.topBarBackdropModifier)
      ) {
        WideContentBox { sidePadding ->
          LazyColumn(
            state = listState,
            modifier = Modifier
              .fillMaxSize()
              .horizontalCutoutPadding()
              .adaptiveVerticalScroll(scrollBehavior),
            contentPadding = PaddingValues(
              top = padding.calculateTopPadding() + 6.dp,
              start = sidePadding,
              end = sidePadding
            )
          ) {
            item(key = "title") {
              TitleCard(
                title = title,
                iconSource = iconSource,
                fallbackIcon = fallbackIcon,
                onClick = onTitleClick
              )
            }
            if (sections.isEmpty()) {
              item(key = "empty") {
                Box(
                  Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                ) {
                  LcEmptyState(title = stringResource(R.string.snapshot_empty_list_title))
                }
              }
            } else {
              sections.forEachIndexed { sectionIndex, section ->
                val collapsed = sectionIndex in collapsedSections
                item(key = "section-header-$sectionIndex") {
                  SectionHeader(
                    section = section,
                    collapsed = collapsed,
                    onToggle = {
                      collapsedSections = if (collapsed) {
                        collapsedSections - sectionIndex
                      } else {
                        collapsedSections + sectionIndex
                      }
                    },
                    modifier = Modifier.animateItem(),
                    legacyDetail = legacyDetail
                  )
                }
                if (!collapsed) {
                  itemsIndexed(
                    items = section.items,
                    key = { itemIndex, item ->
                      "section-$sectionIndex-item-$itemIndex-${item.item.name}-${item.item.diffType}"
                    }
                  ) { itemIndex, item ->
                    CardSegment(
                      isFirst = itemIndex == 0,
                      isLast = itemIndex == section.items.lastIndex,
                      outerBottomPadding = if (itemIndex == section.items.lastIndex) 10.dp else 0.dp,
                      modifier = Modifier.animateItem()
                    ) {
                      if (legacyDetail) {
                        LegacySnapshotDetailItemRow(
                          item = item,
                          packageName = packageName,
                          isFirst = itemIndex == 0,
                          isLast = itemIndex == section.items.lastIndex,
                          onItemClick = onItemClick,
                          onItemLongClick = onItemLongClick,
                          onChipClick = { selectedRuleItem = it }
                        )
                      } else {
                        CompactSnapshotDetailItemRow(
                          item = item,
                          packageName = packageName,
                          isFirst = itemIndex == 0,
                          isLast = itemIndex == section.items.lastIndex,
                          onItemClick = onItemClick,
                          onItemLongClick = onItemLongClick,
                          onChipClick = { selectedRuleItem = it }
                        )
                      }
                    }
                  }
                }
              }
            }
            item(key = "bottom-spacer") {
              Spacer(
                Modifier
                  .height(24.dp)
                  .navigationBarsPadding()
              )
            }
          }
        }
      }
    }
  }

  val activeRuleItem = rememberActiveValue(selectedRuleItem)
  val ruleTarget = activeRuleItem.value
  if (detailViewModel != null && ruleTarget != null) {
    LibDetailSheet(
      show = selectedRuleItem != null,
      viewModel = detailViewModel,
      libName = ruleTarget.item.name,
      type = ruleTarget.item.itemType,
      regexName = ruleTarget.ruleChip?.regexName,
      isValidLib = true,
      packageInfo = null,
      insightViewModel = null,
      onDismiss = { selectedRuleItem = null },
      onDismissFinished = { if (selectedRuleItem == null) activeRuleItem.value = null }
    )
  }
}

@Composable
private fun TitleCard(
  title: SnapshotTitleDisplayData,
  iconSource: SnapshotPackageIconSource?,
  fallbackIcon: Bitmap?,
  onClick: (() -> Unit)?
) {
  val colorScheme = MiuixTheme.colorScheme
  val labelText = title.appName.toString()
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .padding(bottom = 12.dp),
    onClick = onClick,
    showIndication = onClick != null,
    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      when (iconSource) {
        is SnapshotPackageIconSource.InstalledPackage -> AppIcon(
          packageInfo = iconSource.packageInfo,
          label = labelText,
          modifier = Modifier.size(44.dp)
        )

        else -> {
          if (fallbackIcon != null) {
            val imageBitmap = remember(fallbackIcon) { fallbackIcon.asImageBitmap() }
            Image(
              bitmap = imageBitmap,
              contentDescription = labelText,
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
            )
          } else {
            Image(
              painter = painterResource(R.drawable.ic_icon_blueprint),
              contentDescription = labelText,
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
            )
          }
        }
      }
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(start = 12.dp)
      ) {
        Text(
          text = title.appName.toAnnotatedString(),
          fontSize = 16.sp,
          fontWeight = FontWeight.SemiBold,
          color = colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = title.packageName.toAnnotatedString(),
          style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
          color = colorScheme.onSurfaceVariantSummary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
    Spacer(Modifier.height(12.dp))
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(0.5.dp)
        .background(colorScheme.dividerLine)
    )
    Spacer(Modifier.height(12.dp))
    TitleMetricRow(
      label = stringResource(R.string.signature_version),
      value = title.versionInfo.toAnnotatedString()
    )
    Spacer(Modifier.height(6.dp))
    TitleMetricRow(
      label = stringResource(R.string.snapshot_detail_sdk_label),
      value = title.apis.toAnnotatedString()
    )
    title.packageSize?.let {
      Spacer(Modifier.height(6.dp))
      TitleMetricRow(
        label = stringResource(R.string.snapshot_detail_size_label),
        value = it.text.toAnnotatedString()
      )
    }
  }
}

@Composable
private fun TitleMetricRow(
  label: String,
  value: AnnotatedString
) {
  val colorScheme = MiuixTheme.colorScheme
  val condensedFont = lcCondensedFontFamily()
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top
  ) {
    Text(
      text = label,
      modifier = Modifier
        .widthIn(min = 52.dp)
        .padding(end = 10.dp),
      style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp, lineHeight = 18.sp),
      color = colorScheme.onSurfaceVariantSummary,
      maxLines = 1
    )
    Text(
      text = value,
      modifier = Modifier.weight(1f),
      style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp, lineHeight = 18.sp)
        .withCustomFont(condensedFont),
      color = colorScheme.onSurface
    )
  }
}

@Composable
private fun SectionHeader(
  section: SnapshotDetailSection,
  collapsed: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
  legacyDetail: Boolean
) {
  if (!legacyDetail) {
    CompactSnapshotDetailSectionHeader(section, collapsed, onToggle, modifier)
    return
  }
  val colorScheme = MiuixTheme.colorScheme
  val condensedFont = lcCondensedFontFamily()
  val rotation by animateFloatAsState(
    targetValue = if (collapsed) 0f else 90f,
    label = "section-chevron"
  )
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .padding(top = 4.dp, bottom = 4.dp)
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onToggle)
      .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = section.title,
      modifier = Modifier.weight(1f),
      style = MiuixTheme.textStyles.body2.copy(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
      ),
      color = colorScheme.onSurfaceVariantSummary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      section.statusCounts.forEach { count ->
        val palette = snapshotStatusColors(count.diffType)
        Row(
          modifier = Modifier
            .clip(CircleShape)
            .background(palette.pillBackground)
            .padding(horizontal = 8.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${count.label} ${count.countText}",
            style = MiuixTheme.textStyles.footnote2.copy(
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              fontFeatureSettings = "tnum"
            ).withCustomFont(condensedFont),
            color = palette.ink,
            maxLines = 1
          )
        }
      }
      Box(
        modifier = Modifier
          .padding(start = 2.dp)
          .size(16.dp),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = MiuixIcons.Glass.ChevronForward,
          contentDescription = null,
          tint = colorScheme.onSurfaceVariantActions,
          modifier = Modifier
            .size(14.dp)
            .graphicsLayer { rotationZ = rotation }
        )
      }
    }
  }
}

internal data class SnapshotStatusPalette(
  val ink: Color,
  val pillBackground: Color,
  val bar: Color,
  val rowGradientStart: Color
)

@Composable
internal fun snapshotStatusColors(diffType: Int): SnapshotStatusPalette {
  val isDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
  return if (isDark) {
    when (diffType) {
      ADDED -> {
        val pillBg = Color(0xFF14532D).copy(alpha = 0.55f)
        SnapshotStatusPalette(
          ink = Color(0xFF6EE7B7),
          pillBackground = pillBg,
          bar = Color(0xFF34D399),
          rowGradientStart = pillBg.copy(alpha = 0.45f)
        )
      }

      REMOVED -> {
        val pillBg = Color(0xFF7F1D1D).copy(alpha = 0.55f)
        SnapshotStatusPalette(
          ink = Color(0xFFFCA5A5),
          pillBackground = pillBg,
          bar = Color(0xFFF87171),
          rowGradientStart = pillBg.copy(alpha = 0.45f)
        )
      }

      CHANGED -> {
        val pillBg = Color(0xFF78350F).copy(alpha = 0.55f)
        SnapshotStatusPalette(
          ink = Color(0xFFFCD34D),
          pillBackground = pillBg,
          bar = Color(0xFFFBBF24),
          rowGradientStart = pillBg.copy(alpha = 0.45f)
        )
      }

      MOVED -> {
        val pillBg = Color(0xFF1E3A8A).copy(alpha = 0.55f)
        SnapshotStatusPalette(
          ink = Color(0xFF93C5FD),
          pillBackground = pillBg,
          bar = Color(0xFF60A5FA),
          rowGradientStart = pillBg.copy(alpha = 0.45f)
        )
      }

      else -> {
        val pillBg = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        SnapshotStatusPalette(
          ink = MiuixTheme.colorScheme.primary,
          pillBackground = pillBg,
          bar = MiuixTheme.colorScheme.primary,
          rowGradientStart = pillBg.copy(alpha = 0.35f)
        )
      }
    }
  } else {
    when (diffType) {
      ADDED -> {
        val pillBg = Color(0xFFDCFCE7)
        SnapshotStatusPalette(
          ink = Color(0xFF15803D),
          pillBackground = pillBg,
          bar = Color(0xFF22C55E),
          rowGradientStart = pillBg.copy(alpha = 0.78f)
        )
      }

      REMOVED -> {
        val pillBg = Color(0xFFFEE2E2)
        SnapshotStatusPalette(
          ink = Color(0xFFB91C1C),
          pillBackground = pillBg,
          bar = Color(0xFFEF4444),
          rowGradientStart = pillBg.copy(alpha = 0.78f)
        )
      }

      CHANGED -> {
        val pillBg = Color(0xFFFEF3C7)
        SnapshotStatusPalette(
          ink = Color(0xFFB45309),
          pillBackground = pillBg,
          bar = Color(0xFFF59E0B),
          rowGradientStart = pillBg.copy(alpha = 0.78f)
        )
      }

      MOVED -> {
        val pillBg = Color(0xFFDBEAFE)
        SnapshotStatusPalette(
          ink = Color(0xFF1D4ED8),
          pillBackground = pillBg,
          bar = Color(0xFF3B82F6),
          rowGradientStart = pillBg.copy(alpha = 0.78f)
        )
      }

      else -> {
        val pillBg = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        SnapshotStatusPalette(
          ink = MiuixTheme.colorScheme.primary,
          pillBackground = pillBg,
          bar = MiuixTheme.colorScheme.primary,
          rowGradientStart = pillBg.copy(alpha = 0.35f)
        )
      }
    }
  }
}

private sealed interface SnapshotDetailExtraBlock {
  data class MetricDeltaRow(
    val transition: AnnotatedString,
    val delta: String
  ) : SnapshotDetailExtraBlock

  data class InlineTextRow(
    val text: AnnotatedString
  ) : SnapshotDetailExtraBlock

  data class RawAnnotated(
    val text: AnnotatedString
  ) : SnapshotDetailExtraBlock
}

private val ByteParentheticalRegex = Regex("\\s*\\([+\\-−]?\\d[\\d,\\u00A0\\u202F]*\\s+Bytes?\\)")

internal fun stripByteParenthetical(text: String): String =
  text.replace(ByteParentheticalRegex, "")

internal fun stripByteParenthetical(source: AnnotatedString): AnnotatedString {
  val matches = ByteParentheticalRegex.findAll(source.text).toList()
  if (matches.isEmpty()) return source
  return buildAnnotatedString {
    var cursor = 0
    for (match in matches) {
      if (match.range.first > cursor) {
        append(source.subSequence(cursor, match.range.first))
      }
      cursor = match.range.last + 1
    }
    if (cursor < source.length) {
      append(source.subSequence(cursor, source.length))
    }
  }
}

@Composable
private fun LegacySnapshotDetailItemRow(
  item: SnapshotDetailItemDisplayData,
  packageName: String,
  isFirst: Boolean,
  isLast: Boolean,
  onItemClick: ((SnapshotDetailItemDisplayData) -> Unit)?,
  onItemLongClick: ((SnapshotDetailItemDisplayData) -> Unit)?,
  onChipClick: (SnapshotDetailItemDisplayData) -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  val condensedFont = lcCondensedFontFamily()
  val palette = snapshotStatusColors(item.item.diffType)
  val previousPath = item.previousPackagePath
  val titleAnnotated = remember(item.title) { item.title.toAnnotatedString() }
  val policy = remember(item, packageName) {
    SnapshotDetailRow.Item(item.item.itemType, item).interactionPolicy(packageName)
  }
  val canClick = onItemClick != null && policy.opensDetail
  val canLongClick = onItemLongClick != null && policy.opensReference
  val titleStyle = MiuixTheme.textStyles.body2.copy(
    fontSize = 14.sp,
    fontWeight = FontWeight.Medium,
    lineHeight = 20.sp
  ).withCustomFont(condensedFont)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .combinedClickable(
        enabled = canClick || canLongClick,
        onClick = { if (canClick) onItemClick.invoke(item) },
        onLongClick = if (canLongClick) ({ onItemLongClick.invoke(item) }) else null
      )
      .drawBehind {
        val barWidthPx = 3.dp.toPx()
        val barLeftPx = 8.dp.toPx()
        val barCenterX = barLeftPx + barWidthPx / 2f
        val barTopPx = if (isFirst) 8.dp.toPx() else 4.dp.toPx()
        val barBottomPx = if (isLast) 8.dp.toPx() else 4.dp.toPx()
        val barHeight = (size.height - barTopPx - barBottomPx).coerceAtLeast(0f)
        if (barHeight > 0f) {
          val gradientWidth = (size.width - barCenterX).coerceAtLeast(0f)
          if (gradientWidth > 0f) {
            drawRect(
              brush = Brush.horizontalGradient(
                0f to palette.rowGradientStart,
                0.55f to palette.rowGradientStart.copy(alpha = palette.rowGradientStart.alpha * 0.28f),
                1f to Color.Transparent,
                startX = barCenterX,
                endX = size.width
              ),
              topLeft = Offset(barCenterX, barTopPx),
              size = Size(gradientWidth, barHeight)
            )
          }
          val radius = barWidthPx / 2f
          drawRoundRect(
            color = palette.bar,
            topLeft = Offset(barLeftPx, barTopPx),
            size = Size(barWidthPx, barHeight),
            cornerRadius = CornerRadius(radius, radius)
          )
        }
      }
      .padding(
        start = 18.dp,
        end = 12.dp,
        top = if (isFirst) 10.dp else 7.dp,
        bottom = if (isLast) 10.dp else 7.dp
      )
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.Top
    ) {
      Text(
        text = if (previousPath != null) AnnotatedString(previousPath.toString()) else titleAnnotated,
        modifier = Modifier.weight(1f),
        style = titleStyle,
        color = colorScheme.onSurface
      )
      Row(
        modifier = Modifier
          .padding(top = 1.dp)
          .clip(CircleShape)
          .background(palette.pillBackground)
          .padding(horizontal = 8.dp, vertical = 2.5.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        AsyncImage(
          model = item.status.iconRes,
          contentDescription = null,
          colorFilter = ColorFilter.tint(palette.ink),
          modifier = Modifier.size(if (item.item.diffType == MOVED) 13.dp else 11.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
          text = stringResource(item.status.labelRes),
          style = MiuixTheme.textStyles.footnote2.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          ),
          color = palette.ink,
          maxLines = 1
        )
      }
    }

    if (previousPath != null) {
      Spacer(Modifier.height(2.dp))
      Text(
        text = "\u2193",
        style = MiuixTheme.textStyles.body2.copy(
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          lineHeight = 16.sp
        ),
        color = palette.ink
      )
      Spacer(Modifier.height(2.dp))
      Text(
        text = item.item.name,
        style = titleStyle,
        color = colorScheme.onSurface
      )
    }

    item.ruleChip?.let { chip ->
      Spacer(Modifier.height(5.dp))
      val isDark = colorScheme.surface.luminance() < 0.5f
      val chipBg = colorScheme.onSurface.copy(alpha = if (isDark) 0.09f else 0.055f)
      val chipContentColor = colorScheme.onSurface.copy(alpha = if (isDark) 0.82f else 0.74f)
      val iconFilter = when {
        chip.isSimpleColorIcon -> ColorFilter.tint(chipContentColor)
        !chip.useColorfulIcon -> ColorFilter.colorMatrix(
          ColorMatrix().apply { setToSaturation(0f) }
        )
        else -> null
      }
      Row(
        modifier = Modifier
          .clip(CircleShape)
          .background(chipBg)
          .clickable { onChipClick(item) }
          .padding(horizontal = 8.dp, vertical = 2.5.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        AsyncImage(
          model = chip.iconRes,
          contentDescription = null,
          colorFilter = iconFilter,
          modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
          text = chip.label,
          style = MiuixTheme.textStyles.footnote2.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          ),
          color = chipContentColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    if (item.extra.isNotEmpty()) {
      Spacer(Modifier.height(4.dp))
      val extraBlocks = remember(item.extra) { buildExtraBlocks(item.extra) }
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
      ) {
        extraBlocks.forEach { block ->
          when (block) {
            is SnapshotDetailExtraBlock.MetricDeltaRow -> Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = block.transition,
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.footnote1.copy(
                  fontSize = 12.5.sp,
                  fontFeatureSettings = "tnum"
                ).withCustomFont(condensedFont),
                color = colorScheme.onSurfaceVariantSummary
              )
              Box(
                modifier = Modifier
                  .clip(CircleShape)
                  .background(colorScheme.onSurface.copy(alpha = 0.055f))
                  .padding(horizontal = 8.dp, vertical = 2.dp)
              ) {
                Text(
                  text = block.delta,
                  style = MiuixTheme.textStyles.footnote2.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = "tnum"
                  ).withCustomFont(condensedFont),
                  color = colorScheme.onSurfaceVariantSummary,
                  maxLines = 1
                )
              }
            }

            is SnapshotDetailExtraBlock.InlineTextRow -> Text(
              text = block.text,
              style = MiuixTheme.textStyles.footnote1.copy(
                fontSize = 12.5.sp,
                fontFeatureSettings = "tnum"
              ).withCustomFont(condensedFont),
              color = colorScheme.onSurfaceVariantSummary
            )

            is SnapshotDetailExtraBlock.RawAnnotated -> Text(
              text = block.text,
              style = MiuixTheme.textStyles.footnote1.copy(
                fontSize = 12.5.sp,
                lineHeight = 17.sp
              ).withCustomFont(condensedFont),
              color = colorScheme.onSurfaceVariantSummary
            )
          }
        }
      }
    }
  }
}

internal fun splitTrimmedAnnotatedLines(source: AnnotatedString): List<AnnotatedString> {
  val text = source.text
  val lines = mutableListOf<AnnotatedString>()
  var lineStart = 0
  while (lineStart <= text.length) {
    val newlineIndex = text.indexOf('\n', startIndex = lineStart)
    val lineEnd = if (newlineIndex >= 0) newlineIndex else text.length
    var trimStart = lineStart
    var trimEnd = lineEnd
    while (trimStart < trimEnd && text[trimStart].isWhitespace()) trimStart++
    while (trimEnd > trimStart && text[trimEnd - 1].isWhitespace()) trimEnd--
    if (trimStart < trimEnd) {
      lines += source.subSequence(trimStart, trimEnd)
    }
    if (newlineIndex < 0) break
    lineStart = newlineIndex + 1
  }
  return lines
}

private fun buildExtraBlocks(extra: CharSequence): List<SnapshotDetailExtraBlock> {
  val annotated = extra.toAnnotatedString()
  val lines = splitTrimmedAnnotatedLines(annotated)
  if (lines.isEmpty()) return emptyList()

  val blocks = mutableListOf<SnapshotDetailExtraBlock>()
  var index = 0
  var parsedAnyMetric = false
  while (index < lines.size) {
    val current = lines[index]
    val next = lines.getOrNull(index + 1)
    if (
      next != null &&
      current.text.contains(" → ") &&
      (next.text.startsWith("+") || next.text.startsWith("-"))
    ) {
      blocks += SnapshotDetailExtraBlock.MetricDeltaRow(
        transition = stripByteParenthetical(current),
        delta = stripByteParenthetical(next.text).replace(", ", " · ")
      )
      parsedAnyMetric = true
      index += 2
    } else if (ByteParentheticalRegex.containsMatchIn(current.text)) {
      blocks += SnapshotDetailExtraBlock.InlineTextRow(stripByteParenthetical(current))
      parsedAnyMetric = true
      index++
    } else {
      blocks += SnapshotDetailExtraBlock.InlineTextRow(current)
      index++
    }
  }

  return if (parsedAnyMetric) {
    blocks
  } else {
    listOf(SnapshotDetailExtraBlock.RawAnnotated(annotated))
  }
}
