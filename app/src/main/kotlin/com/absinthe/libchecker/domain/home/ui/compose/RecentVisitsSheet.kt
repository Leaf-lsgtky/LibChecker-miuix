package com.absinthe.libchecker.domain.home.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.home.presentation.RecentVisitsViewModel
import com.absinthe.libchecker.domain.home.recent.RecentVisit
import com.absinthe.libchecker.domain.home.ui.view.RecentVisitItem
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/** Recently viewed apps and libraries, mirroring the original long press on a home tab. */
@Composable
fun RecentVisitsSheet(
  show: Boolean,
  viewModel: RecentVisitsViewModel,
  onOpenApp: (String) -> Unit,
  onOpenLibrary: (RecentVisit) -> Unit,
  onDismiss: () -> Unit
) {
  val lists by viewModel.items.collectAsStateWithLifecycle()

  LaunchedEffect(show) {
    if (show) {
      viewModel.refresh()
    }
  }

  WindowBottomSheet(
    show = show,
    title = stringResource(R.string.recent_apps),
    onDismissRequest = onDismiss
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp)
    ) {
      val current = lists
      if (current == null || (current.apps.recent.isEmpty() && current.libraries.recent.isEmpty())) {
        Text(
          text = stringResource(R.string.recent_visits_empty),
          fontSize = 13.sp,
          color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        return@Column
      }

      LazyColumn(modifier = Modifier.heightIn(max = 460.dp)) {
        val pinned = current.apps.pinned + current.libraries.pinned
        if (pinned.isNotEmpty()) {
          item(key = "pinned-title") {
            RecentVisitsSectionTitle(stringResource(R.string.recent_visits_pinned))
          }
          items(pinned, key = { item -> "pinned-${item.visit.type}-${item.visit.name}" }) { item ->
            RecentVisitRow(
              item = item,
              onOpen = {
                if (item.visit.isLibrary) {
                  onOpenLibrary(item.visit)
                } else {
                  onOpenApp(item.visit.name)
                }
                onDismiss()
              },
              onRemove = { viewModel.remove(item.visit) }
            )
          }
        }

        if (current.apps.recent.isNotEmpty()) {
          item(key = "apps-title") {
            RecentVisitsSectionTitle(stringResource(R.string.recent_apps))
          }
          items(current.apps.recent, key = { "app-${it.visit.name}" }) { item ->
            RecentVisitRow(
              item = item,
              onOpen = {
                onOpenApp(item.visit.name)
                onDismiss()
              },
              onRemove = { viewModel.remove(item.visit) }
            )
          }
        }
        if (current.libraries.recent.isNotEmpty()) {
          item(key = "libs-title") {
            RecentVisitsSectionTitle(stringResource(R.string.recent_libraries))
          }
          items(current.libraries.recent, key = { "lib-${it.visit.type}-${it.visit.name}" }) { item ->
            RecentVisitRow(
              item = item,
              onOpen = {
                onOpenLibrary(item.visit)
                onDismiss()
              },
              onRemove = { viewModel.remove(item.visit) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RecentVisitsSectionTitle(text: String) {
  Text(
    text = text,
    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    fontSize = 12.sp,
    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
  )
}

@Composable
private fun RecentVisitRow(
  item: RecentVisitItem,
  onOpen: () -> Unit,
  onRemove: () -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onOpen)
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    AsyncImage(
      model = item.icon,
      contentDescription = null,
      modifier = Modifier
        .padding(end = 12.dp)
        .size(28.dp)
    )
    Text(
      text = item.label,
      modifier = Modifier.weight(1f),
      fontSize = 14.sp,
      fontWeight = FontWeight(500),
      color = colorScheme.onSurface
    )
    Text(
      text = stringResource(R.string.dialog_action_delete),
      modifier = Modifier
        .clickable(onClick = onRemove)
        .padding(horizontal = 6.dp, vertical = 2.dp),
      fontSize = 11.sp,
      color = colorScheme.onSurfaceVariantSummary
    )
  }
}
