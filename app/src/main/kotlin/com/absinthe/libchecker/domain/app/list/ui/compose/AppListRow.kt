package com.absinthe.libchecker.domain.app.list.ui.compose

import android.content.Context
import android.content.pm.PackageInfo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import com.absinthe.libchecker.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.list.model.AppListItemViewState
import com.absinthe.libchecker.ui.compose.component.resolveAbiBadgeTint
import com.absinthe.libchecker.ui.compose.theme.lcCondensedFontFamily
import com.absinthe.libchecker.ui.compose.theme.withCustomFont
import me.zhanghai.android.appiconloader.AppIconLoader
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * One app row, rendered as a standalone Miuix card.
 *
 * Layout follows the Miuix list convention: 48 dp leading icon, a two/three line text column with
 * a marquee for long labels, and right-aligned status tags.
 */
@Composable
fun AppListRow(
  item: LCItem,
  viewState: AppListItemViewState?,
  packageInfo: PackageInfo?,
  chips: List<String>,
  highlight: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  colors: CardColors = CardDefaults.defaultColors()
) {
  val colorScheme = MiuixTheme.colorScheme
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .padding(bottom = 12.dp),
    onClick = onClick,
    colors = colors,
    showIndication = true,
    insideMargin = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
  ) {
    val badgeTint by animateColorAsState(
      targetValue = resolveAbiBadgeTint(tintAbiLabels = viewState?.tintAbiLabels == true),
      animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
      label = "AppListAbiBadgeTint"
    )
    val multiArchTint by animateColorAsState(
      targetValue = resolveAbiBadgeTint(tintAbiLabels = viewState?.tintAbiLabels == true),
      animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
      label = "AppListMultiArchTint"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(
        modifier = Modifier
          .padding(end = 12.dp)
          .animateContentSize(tween(durationMillis = 220, easing = FastOutSlowInEasing)),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        AppIcon(
          packageInfo = packageInfo ?: viewState?.packageInfo,
          label = item.label,
          modifier = Modifier.size(48.dp)
        )
        val showDetached = viewState != null && viewState.useDetachedAbiBadges &&
          (viewState.largeAbiBadgeRes != 0 || viewState.showMultiArchBadge)
        AnimatedVisibility(
          visible = showDetached,
          enter = fadeIn(tween(durationMillis = 220, easing = FastOutSlowInEasing)) +
            expandVertically(tween(durationMillis = 220, easing = FastOutSlowInEasing)),
          exit = fadeOut(tween(durationMillis = 160, easing = FastOutSlowInEasing)) +
            shrinkVertically(tween(durationMillis = 180, easing = FastOutSlowInEasing)),
          label = "AppListDetachedAbiVisibility"
        ) {
          if (viewState != null && (viewState.largeAbiBadgeRes != 0 || viewState.showMultiArchBadge)) {
            val bothShown = viewState.largeAbiBadgeRes != 0 && viewState.showMultiArchBadge
            val badgeWidth = if (bothShown) 23.5.dp else 36.dp
            val badgeHeight = if (bothShown) 15.67.dp else 24.dp
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Spacer(Modifier.size(4.dp))
              Row(
                horizontalArrangement = Arrangement.spacedBy(if (bothShown) 1.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (viewState.largeAbiBadgeRes != 0) {
                  Icon(
                    painter = painterResource(viewState.largeAbiBadgeRes),
                    contentDescription = null,
                    tint = badgeTint,
                    modifier = Modifier.size(width = badgeWidth, height = badgeHeight)
                  )
                }
                if (viewState.showMultiArchBadge) {
                  Icon(
                    painter = painterResource(R.drawable.ic_abi_label_multi_arch),
                    contentDescription = null,
                    tint = multiArchTint,
                    modifier = Modifier.size(width = badgeWidth, height = badgeHeight)
                  )
                }
              }
            }
          }
        }
      }
      Column(
        modifier = Modifier
          .weight(1f)
          .animateContentSize(tween(durationMillis = 220, easing = FastOutSlowInEasing))
      ) {
        Text(
          text = highlightText(item.label, highlight, colorScheme.primary),
          style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp),
          fontWeight = FontWeight(600),
          color = colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.size(2.dp))
        Text(
          text = item.packageName,
          style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
          color = colorScheme.onSurfaceVariantSummary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (viewState != null) {
          val condensedFont = lcCondensedFontFamily()
          AnimatedContent(
            targetState = viewState.versionInfo,
            transitionSpec = {
              (
                fadeIn(tween(durationMillis = 220, easing = FastOutSlowInEasing)) togetherWith
                  fadeOut(tween(durationMillis = 160, easing = FastOutSlowInEasing))
                ).using(SizeTransform(clip = false))
            },
            label = "AppListVersionFade"
          ) { versionText ->
            if (versionText.isNotEmpty()) {
              Column {
                Spacer(Modifier.size(2.dp))
                Text(
                  text = versionText,
                  style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp).withCustomFont(condensedFont),
                  color = colorScheme.onSurfaceVariantSummary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
          val abiRowKey = if (viewState.abiInfo.isEmpty()) {
            null
          } else {
            AppListAbiLineKey(
              abiText = viewState.abiInfo.toString(),
              useDetached = viewState.useDetachedAbiBadges,
              abiBadgeRes = viewState.abiBadgeRes,
              showMultiArch = viewState.showMultiArchBadge
            )
          }
          AnimatedContent(
            targetState = abiRowKey,
            transitionSpec = {
              (
                fadeIn(tween(durationMillis = 220, easing = FastOutSlowInEasing)) togetherWith
                  fadeOut(tween(durationMillis = 160, easing = FastOutSlowInEasing))
                ).using(SizeTransform(clip = false))
            },
            label = "AppListAbiFade"
          ) { line ->
            if (line != null) {
              Column {
                Spacer(Modifier.size(2.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  if (!line.useDetached && line.abiBadgeRes != 0) {
                    Icon(
                      painter = painterResource(line.abiBadgeRes),
                      contentDescription = null,
                      tint = badgeTint,
                      modifier = Modifier.size(width = 24.dp, height = 16.dp)
                    )
                    if (line.showMultiArch) {
                      Icon(
                        painter = painterResource(R.drawable.ic_multi_arch),
                        contentDescription = null,
                        tint = multiArchTint,
                        modifier = Modifier.size(13.dp)
                      )
                    }
                  }
                  Text(
                    text = line.abiText,
                    style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp).withCustomFont(condensedFont),
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }
      }
      val tags = remember(chips, viewState?.packageBadge) {
        buildList {
          addAll(chips.take(2))
          when (viewState?.packageBadge) {
            AppListItemViewState.PackageBadge.Harmony -> add("Harmony")
            AppListItemViewState.PackageBadge.Frozen -> add("Frozen")
            null -> Unit
          }
        }
      }
      if (tags.isNotEmpty() || viewState?.packageBadge == AppListItemViewState.PackageBadge.Frozen) {
        Column(
          modifier = Modifier.padding(start = 12.dp),
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          tags.take(2).forEach { tag ->
            when (tag) {
              "Frozen" -> Icon(
                painter = painterResource(R.drawable.ic_disabled_package),
                contentDescription = null,
                tint = colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(16.dp)
              )

              else -> StatusTag(text = tag)
            }
          }
        }
      }
    }
  }
}

/** Rounded app icon with a neutral placeholder while Coil decodes the package icon. */
@Composable
fun AppIcon(
  packageInfo: PackageInfo?,
  label: String,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val iconSizePx = with(LocalDensity.current) { 48.dp.roundToPx() }
  val placeholderColor = MiuixTheme.colorScheme.secondaryContainer
  val placeholder = remember(placeholderColor) { ColorPainter(placeholderColor) }
  val request = remember(packageInfo, iconSizePx, context) {
    packageInfo?.let { buildAppIconImageRequest(context, it, iconSizePx) }
  }
  AsyncImage(
    model = request,
    contentDescription = label,
    placeholder = placeholder,
    error = placeholder,
    fallback = placeholder,
    contentScale = ContentScale.Crop,
    modifier = modifier.clip(RoundedCornerShape(12.dp))
  )
}

internal fun buildAppIconImageRequest(
  context: Context,
  packageInfo: PackageInfo,
  iconSizePx: Int
): ImageRequest {
  val iconKey = packageInfo.applicationInfo?.let {
    AppIconLoader.getIconKey(packageInfo, context)
  }
  return ImageRequest.Builder(context)
    .data(packageInfo)
    .size(iconSizePx)
    .apply {
      if (iconKey != null) {
        memoryCacheKey(iconKey)
        placeholderMemoryCacheKey(iconKey)
      }
    }
    .crossfade(false)
    .build()
}

/** Compact MIUI style tag used for rule labels and platform badges. */
@Composable
fun StatusTag(text: String, modifier: Modifier = Modifier) {
  com.absinthe.libchecker.ui.compose.component.LcTag(
    text = text,
    modifier = modifier,
    containerColor = MiuixTheme.colorScheme.tertiaryContainer,
    contentColor = MiuixTheme.colorScheme.onTertiaryContainer
  )
}

/** Highlights every case-insensitive occurrence of [query] with [highlightColor]. */
@Composable
fun highlightText(text: String, query: String, highlightColor: Color): AnnotatedString {
  if (query.isBlank()) return AnnotatedString(text)
  return remember(text, query, highlightColor) {
    buildAnnotatedString {
      var start = 0
      while (start < text.length) {
        val index = text.indexOf(query, start, ignoreCase = true)
        if (index < 0) {
          append(text.substring(start))
          break
        }
        append(text.substring(start, index))
        withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) {
          append(text.substring(index, index + query.length))
        }
        start = index + query.length
      }
    }
  }
}

private data class AppListAbiLineKey(
  val abiText: String,
  val useDetached: Boolean,
  val abiBadgeRes: Int,
  val showMultiArch: Boolean
)
