package com.absinthe.libchecker.domain.about.ui.compose

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.ui.compose.component.AdaptiveBackButton
import com.absinthe.libchecker.ui.compose.component.AdaptiveSearchOverlay
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopSearchButton
import com.absinthe.libchecker.ui.compose.component.CardSegment
import com.absinthe.libchecker.ui.compose.component.LcEmptyState
import com.absinthe.libchecker.ui.compose.component.MiuixPageHost
import com.absinthe.libchecker.ui.compose.component.TopBarSearchConfig
import com.absinthe.libchecker.ui.compose.component.adaptiveVerticalScroll
import com.absinthe.libchecker.ui.compose.component.rememberTopBarSearchState
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import com.absinthe.libchecker.utils.extensions.openUrlInBrowser
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.util.withContext
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.absinthe.libchecker.ui.compose.component.FastScrollLazyColumn as LazyColumn

/** Open source licence list rendered from the AboutLibraries data. */
@Composable
fun LicensesScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState()
) {
  val context = LocalContext.current
  val scrollBehavior = MiuixScrollBehavior()
  val searchState = rememberTopBarSearchState()
  val query = searchState.query
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

  val isScrolled = if (libraries.isEmpty()) false else listState.canScrollBackward

  MiuixPageHost(modifier = modifier.fillMaxSize()) {
    val pageState = this
    Box(modifier = Modifier.fillMaxSize()) {
      Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
          AdaptiveTopAppBar(
            title = stringResource(R.string.about_open_source_licenses),
            scrollBehavior = scrollBehavior,
            isContentScrolled = isScrolled,
            search = TopBarSearchConfig(
              state = searchState,
              label = stringResource(R.string.search_hint)
            ),
            navigationIcon = {
              AdaptiveBackButton(onClick = onBack)
            },
            actions = {
              AdaptiveTopSearchButton(onClick = searchState::open)
            }
          )
        }
      ) { padding ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .then(pageState.topBarBackdropModifier)
            .imePadding()
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
              if (libraries.isEmpty()) {
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
                itemsIndexed(items = libraries, key = { _, it -> it.uniqueId }) { index, library ->
                  CardSegment(
                    isFirst = index == 0,
                    isLast = index == libraries.lastIndex
                  ) {
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

      AdaptiveSearchOverlay(
        config = TopBarSearchConfig(
          state = searchState,
          label = stringResource(R.string.search_hint)
        )
      ) { activeQuery ->
        val searchListState = rememberLazyListState()
        LaunchedEffect(activeQuery) {
          searchListState.scrollToItem(0)
        }
        if (filtered.isEmpty()) {
          LcEmptyState(title = stringResource(R.string.empty_list))
        } else {
          WideContentBox { sidePadding ->
            LazyColumn(
              state = searchListState,
              modifier = Modifier
                .fillMaxSize()
                .horizontalCutoutPadding()
                .adaptiveVerticalScroll(),
              contentPadding = PaddingValues(
                top = 6.dp,
                start = sidePadding,
                end = sidePadding
              )
            ) {
              itemsIndexed(items = filtered, key = { _, it -> it.uniqueId }) { index, library ->
                CardSegment(
                  isFirst = index == 0,
                  isLast = index == filtered.lastIndex
                ) {
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
      style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp),
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
        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
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
              style = MiuixTheme.textStyles.footnote2.copy(fontSize = 10.sp),
              color = colorScheme.onTertiaryContainer,
              maxLines = 1
            )
          }
        }
      }
    }
  }
}
