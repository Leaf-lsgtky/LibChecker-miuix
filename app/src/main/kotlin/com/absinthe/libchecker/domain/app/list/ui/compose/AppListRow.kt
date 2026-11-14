package com.absinthe.libchecker.domain.app.list.ui.compose

import android.content.pm.PackageInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import com.absinthe.libchecker.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import top.yukonga.miuix.kmp.basic.Card
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
  modifier: Modifier = Modifier
) {
  val colorScheme = MiuixTheme.colorScheme
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .padding(bottom = 12.dp),
    onClick = onClick,
    showIndication = true,
    insideMargin = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      AppIcon(
        packageInfo = packageInfo ?: viewState?.packageInfo,
        label = item.label,
        modifier = Modifier
          .padding(end = 12.dp)
          .size(48.dp)
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = highlightText(item.label, highlight, colorScheme.primary),
          modifier = Modifier.basicMarquee(),
          fontWeight = FontWeight(550),
          color = colorScheme.onSurface,
          maxLines = 1,
          softWrap = false
        )
        Spacer(Modifier.size(2.dp))
        Text(
          text = item.packageName,
          modifier = Modifier.basicMarquee(),
          fontSize = 12.sp,
          fontWeight = FontWeight(500),
          color = colorScheme.onSurfaceVariantSummary,
          maxLines = 1,
          softWrap = false
        )
        if (viewState != null) {
          Spacer(Modifier.size(1.dp))
          Text(
            text = buildString {
              append(viewState.versionInfo)
              if (viewState.abiInfo.isNotEmpty()) {
                append(" · ")
                append(viewState.abiInfo)
              }
            },
            fontSize = 12.sp,
            fontWeight = FontWeight(500),
            color = colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
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
  val placeholderColor = MiuixTheme.colorScheme.secondaryContainer
  val placeholder = remember(placeholderColor) { ColorPainter(placeholderColor) }
  val request = remember(packageInfo) {
    packageInfo?.let {
      ImageRequest.Builder(context)
        .data(it)
        .crossfade(false)
        .build()
    }
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