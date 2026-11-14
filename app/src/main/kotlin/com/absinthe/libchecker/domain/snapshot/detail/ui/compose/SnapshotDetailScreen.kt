package com.absinthe.libchecker.domain.snapshot.detail.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitleDisplayData
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import com.absinthe.libchecker.ui.compose.component.LcEmptyState
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import com.absinthe.libchecker.ui.compose.util.toAnnotatedString
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.UploadCloud
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** Snapshot diff detail for one app: title block plus collapsible change sections. */
@Composable
fun SnapshotDetailScreen(
  title: SnapshotTitleDisplayData,
  sections: List<SnapshotDetailSection>,
  onBack: () -> Unit,
  onShare: () -> Unit,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState()
) {
  val scrollBehavior = MiuixScrollBehavior()
  var collapsedSections by remember { mutableStateOf(setOf<Int>()) }

  Column(modifier = modifier.fillMaxSize()) {
    SmallTopAppBar(
      title = title.appName.toString(),
      subtitle = title.packageName.toString(),
      navigationIcon = {
        IconButton(onClick = onBack) {
          Icon(MiuixIcons.Back, contentDescription = null)
        }
      },
      actions = {
        IconButton(onClick = onShare) {
          Icon(
            painter = painterResource(R.drawable.ic_report),
            contentDescription = stringResource(R.string.snapshot_generate_text_report)
          )
        }
      }
    )
    WideContentBox { sidePadding ->
      LazyColumn(
        state = listState,
        modifier = Modifier
          .fillMaxSize()
          .horizontalCutoutPadding()
          .scrollEndHaptic()
          .overScrollVertical()
          .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(
          top = 6.dp,
          start = sidePadding,
          end = sidePadding
        )
      ) {
        item(key = "title") {
          TitleCard(title = title)
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
          sections.forEachIndexed { index, section ->
            item(key = "section-$index") {
              SectionCard(
                section = section,
                collapsed = index in collapsedSections,
                onToggle = {
                  collapsedSections = if (index in collapsedSections) {
                    collapsedSections - index
                  } else {
                    collapsedSections + index
                  }
                }
              )
            }
          }
        }
        item {
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

@Composable
private fun TitleCard(title: SnapshotTitleDisplayData) {
  val colorScheme = MiuixTheme.colorScheme
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .padding(bottom = 12.dp),
    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
  ) {
    Text(
      text = title.appName.toString(),
      fontWeight = FontWeight(600),
      color = colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Spacer(Modifier.height(2.dp))
    Text(
      text = title.versionInfo.toString(),
      fontSize = 12.sp,
      color = colorScheme.onSurfaceVariantSummary
    )
    Spacer(Modifier.height(2.dp))
    Text(
      text = title.apis.toString(),
      fontSize = 12.sp,
      color = colorScheme.onSurfaceVariantSummary
    )
    title.packageSize?.let {
      Spacer(Modifier.height(2.dp))
      Text(
        text = it.text.toString(),
        fontSize = 12.sp,
        color = colorScheme.onSurfaceVariantSummary
      )
    }
  }
}

@Composable
private fun SectionCard(
  section: SnapshotDetailSection,
  collapsed: Boolean,
  onToggle: () -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .padding(bottom = 12.dp),
    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onToggle),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = section.title,
        modifier = Modifier.weight(1f),
        fontWeight = FontWeight(600),
        color = colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      section.statusCounts.forEach { count ->
        val statusColor = colorResource(count.status.colorRes)
        Box(
          modifier = Modifier
            .padding(start = 6.dp)
            .squircleBackground(statusColor.copy(alpha = 0.14f), 6.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = "${count.label} ${count.countText}",
            fontSize = 10.sp,
            fontWeight = FontWeight(550),
            color = statusColor
          )
        }
      }
    }
    if (!collapsed) {
      Spacer(Modifier.height(8.dp))
      section.items.forEachIndexed { index, item ->
        if (index > 0) {
          Spacer(Modifier.height(10.dp))
        }
        DetailItem(item = item)
      }
    }
  }
}

@Composable
private fun DetailItem(item: SnapshotDetailItemDisplayData) {
  val colorScheme = MiuixTheme.colorScheme
  val statusColor = colorResource(item.status.colorRes)
  Column {
    Row(verticalAlignment = Alignment.CenterVertically) {
      AsyncImage(
        model = item.status.iconRes,
        contentDescription = null,
        colorFilter = ColorFilter.tint(statusColor),
        modifier = Modifier
          .padding(end = 8.dp)
          .size(14.dp)
      )
      Text(
        text = item.title.toAnnotatedString(),
        modifier = Modifier.weight(1f),
        fontSize = 13.sp,
        fontWeight = FontWeight(500),
        color = colorScheme.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
      )
      item.ruleChip?.let { chip ->
        Row(
          modifier = Modifier
            .padding(start = 8.dp)
            .squircleBackground(colorScheme.tertiaryContainer, 6.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          AsyncImage(
            model = chip.iconRes,
            contentDescription = null,
            modifier = Modifier
              .padding(end = 4.dp)
              .size(12.dp)
          )
          Text(
            text = chip.label,
            fontSize = 10.sp,
            fontWeight = FontWeight(550),
            color = colorScheme.onTertiaryContainer,
            maxLines = 1
          )
        }
      }
    }
    if (item.extra.isNotEmpty()) {
      Spacer(Modifier.height(2.dp))
      Text(
        text = item.extra.toAnnotatedString(),
        modifier = Modifier.padding(start = 16.dp),
        fontSize = 12.sp,
        color = colorScheme.onSurfaceVariantSummary,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis
      )
    }
    item.previousPackagePath?.let { path ->
      Spacer(Modifier.height(2.dp))
      Text(
        text = path.toString(),
        modifier = Modifier.padding(start = 16.dp),
        fontSize = 11.sp,
        color = colorScheme.onSurfaceVariantSummary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}