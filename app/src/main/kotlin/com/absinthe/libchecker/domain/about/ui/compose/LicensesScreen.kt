package com.absinthe.libchecker.domain.about.ui.compose

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.LcEmptyState
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import com.absinthe.libchecker.utils.extensions.openUrlInBrowser
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.util.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** Open source licence list rendered from the AboutLibraries data. */
@Composable
fun LicensesScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState()
) {
  val context = LocalContext.current
  val focusManager = LocalFocusManager.current
  val scrollBehavior = MiuixScrollBehavior()
  val colorScheme = MiuixTheme.colorScheme
  var query by rememberSaveable { mutableStateOf("") }
  var searchExpanded by rememberSaveable { mutableStateOf(false) }

  val libraries = remember {
    runCatching {
      Libs.Builder().withContext(context).build().libraries
    }.getOrDefault(emptyList())
  }
  val filtered = remember(libraries, query) {
    if (query.isBlank()) {
      libraries
    } else {
      libraries.filter { library ->
        library.name?.contains(query, ignoreCase = true) == true ||
          library.artifactId?.contains(query, ignoreCase = true) == true ||
          library.website?.contains(query, ignoreCase = true) == true
      }
    }
  }

  Column(modifier = modifier.fillMaxSize()) {
    AdaptiveTopAppBar(
      title = stringResource(R.string.about_open_source_licenses),
      scrollBehavior = scrollBehavior,
      navigationIcon = {
        IconButton(onClick = onBack) {
          Icon(MiuixIcons.Back, contentDescription = null)
        }
      },
      bottomContent = {
        InputField(
          query = query,
          onQueryChange = { query = it },
          onSearch = { focusManager.clearFocus() },
          expanded = searchExpanded,
          onExpandedChange = { searchExpanded = it },
          label = stringResource(R.string.search_hint),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp)
        )
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
        if (filtered.isEmpty()) {
          item(key = "empty") {
            Box(
              Modifier
                .fillMaxWidth()
                .height(260.dp)
            ) {
              LcEmptyState(title = stringResource(R.string.empty_list))
            }
          }
        } else {
          items(items = filtered, key = { it.uniqueId }) { library ->
            LibraryRow(
              library = library,
              onClick = {
                val url = library.website?.takeIf(String::isNotBlank)
                if (url != null) {
                  context.openUrlInBrowser(url)
                }
              }
            )
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
private fun LibraryRow(library: Library, onClick: () -> Unit) {
  val colorScheme = MiuixTheme.colorScheme
  val licenses = library.licenses.mapNotNull { it.name }.distinct()
  val authors = library.developers.mapNotNull { it.name }.distinct()
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    Text(
      text = library.name ?: library.artifactId ?: "",
      fontSize = 14.sp,
      fontWeight = FontWeight(550),
      color = colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    val subtitle = buildString {
      if (authors.isNotEmpty()) append(authors.joinToString(", "))
      library.artifactVersion?.takeIf(String::isNotBlank)?.let { version ->
        if (isNotEmpty()) append(" · ")
        append(version)
      }
    }
    if (subtitle.isNotEmpty()) {
      Spacer(Modifier.height(2.dp))
      Text(
        text = subtitle,
        fontSize = 12.sp,
        color = colorScheme.onSurfaceVariantSummary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    if (licenses.isNotEmpty()) {
      Spacer(Modifier.height(6.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        licenses.take(3).forEach { license ->
          Box(
            modifier = Modifier
              .squircleBackground(colorScheme.tertiaryContainer, 6.dp)
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = license,
              fontSize = 10.sp,
              fontWeight = FontWeight(550),
              color = colorScheme.onTertiaryContainer,
              maxLines = 1
            )
          }
        }
      }
    }
  }
  Box(
    Modifier
      .fillMaxWidth()
      .padding(start = 16.dp)
      .height(0.5.dp)
      .background(colorScheme.dividerLine)
  )
}
