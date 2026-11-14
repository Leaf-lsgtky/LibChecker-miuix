package com.absinthe.libchecker.domain.statistics.reference.ui.compose

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceLoadingState
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceSearchLabels
import com.absinthe.libchecker.domain.statistics.reference.model.resolveReferenceIcon
import com.absinthe.libchecker.domain.statistics.reference.presentation.LibReferenceViewModel
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.LcEmptyState
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.menu.OverlayIconCascadingDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.ui.input.nestedscroll.nestedScroll

/**
 * Statistics destination: one card per referenced library, newest computation first.
 *
 * Computation, filtering and search still run in [LibReferenceViewModel]; this screen only renders
 * its state and forwards the menu/search interactions.
 */
@Composable
fun ReferenceScreen(
  viewModel: LibReferenceViewModel,
  onOpenReference: (LibReference) -> Unit,
  onOpenChart: () -> Unit = {},
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState()
) {
  val references by viewModel.libReference.collectAsStateWithLifecycle()
  val loadingState by viewModel.loadingState.collectAsStateWithLifecycle()
  val focusManager = LocalFocusManager.current
  val scrollBehavior = MiuixScrollBehavior()

  var query by rememberSaveable { mutableStateOf("") }
  var searchExpanded by rememberSaveable { mutableStateOf(false) }
  var searchResult by remember { mutableStateOf<List<LibReference>?>(null) }
  var options by remember { mutableIntStateOf(viewModel.getLibReferenceOptions()) }
  var easterEgg by remember { mutableStateOf(false) }
  var treemapEnabled by remember { mutableStateOf(viewModel.treemapEnabled) }

  LaunchedEffect(Unit) {
    viewModel.onReferencePageVisible(hasDisplayedReferences = false)
  }

  val searchLabels = LibReferenceSearchLabels(
    notMarkedLabel = stringResource(R.string.not_marked_lib),
    permissionFallbackLabel = stringResource(R.string.ref_category_perm),
    metadataLabel = stringResource(R.string.ref_category_metadata),
    packageLabel = stringResource(R.string.ref_category_package)
  )

  LaunchedEffect(query) {
    delay(160)
    viewModel.onSearchQueryChanged(query, searchLabels)
    val result = viewModel.buildCurrentSearchResult()
    searchResult = result?.references
    easterEgg = result?.shouldShowEasterEgg == true
  }

  val displayed = (searchResult ?: references).orEmpty()
  val isLoading = references == null && searchResult == null

  Column(modifier = modifier.fillMaxSize()) {
    AdaptiveTopAppBar(
      title = stringResource(R.string.title_statistics),
      scrollBehavior = scrollBehavior,
      actions = {
        IconButton(
          onClick = {
            treemapEnabled = !treemapEnabled
            viewModel.treemapEnabled = treemapEnabled
          }
        ) {
          Icon(
            painter = painterResource(
              if (treemapEnabled) R.drawable.ic_reference_list else R.drawable.ic_reference_treemap
            ),
            contentDescription = stringResource(
              if (treemapEnabled) {
                R.string.lib_reference_switch_to_list
              } else {
                R.string.lib_reference_switch_to_treemap
              }
            )
          )
        }
        IconButton(onClick = onOpenChart) {
          AsyncImage(
            model = R.drawable.ic_chart,
            contentDescription = stringResource(R.string.tab_chart),
            colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
            modifier = Modifier.size(24.dp)
          )
        }
        val entries = buildReferenceMenuEntries(options) { option, enabled ->
          options = viewModel.setLibReferenceOption(option, enabled)
          viewModel.requestComputeReference(isVisible = true, needShowLoading = true)
        }
        OverlayIconCascadingDropdownMenu(
          entries = entries,
          collapseOnSelection = false
        ) {
          Icon(
            painter = painterResource(R.drawable.ic_menu),
            contentDescription = stringResource(R.string.menu_filter)
          )
        }
      },
      bottomContent = {
        Column {
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
          if (isLoading) {
            LinearProgressIndicator(
              modifier = Modifier.fillMaxWidth(),
              progress = loadingState.overallProgress?.let { it / 100f },
              height = 2.dp
            )
          }
        }
      }
    )

    Box(
      Modifier
        .weight(1f)
        .imePadding()
    ) {
      when {
        isLoading && displayed.isEmpty() -> LcLoadingState(
          message = stringResource(R.string.loading)
        )

        displayed.isEmpty() -> LcEmptyState(
          title = if (easterEgg) "🥚" else stringResource(R.string.empty_list)
        )

        else -> if (treemapEnabled) {
          LibReferenceTreemapContent(
            references = displayed,
            colorfulRuleIcon = viewModel.colorfulRuleIcon,
            onReferenceClick = onOpenReference
          )
        } else WideContentBox { sidePadding ->
          LazyColumn(
            state = listState,
            modifier = Modifier
              .fillMaxSize()
              .scrollEndHaptic()
              .overScrollVertical()
              .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
              top = 6.dp,
              start = sidePadding,
              end = sidePadding
            )
          ) {
            items(items = displayed, key = { "${it.type}:${it.libName}" }) { reference ->
              ReferenceRow(reference = reference, onClick = { onOpenReference(reference) })
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

@Composable
private fun ReferenceRow(reference: LibReference, onClick: () -> Unit) {
  val colorScheme = MiuixTheme.colorScheme
  val label = reference.rule?.label
    ?: reference.resolvedLabel
    ?: stringResource(R.string.not_marked_lib)
  val iconRes = resolveReferenceIcon(reference.libName, reference.type, reference.rule)
  val placeholder = remember { ColorPainter(colorScheme.surfaceContainerHigh) }
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .padding(bottom = 12.dp),
    onClick = onClick,
    showIndication = true,
    insideMargin = PaddingValues(start = 10.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      AsyncImage(
        model = iconRes,
        contentDescription = null,
        placeholder = placeholder,
        error = placeholder,
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .padding(end = 12.dp)
          .size(42.dp)
          .clip(RoundedCornerShape(10.dp))
      )
      Column(Modifier.weight(1f)) {
        Text(
          text = label,
          fontWeight = FontWeight(550),
          color = colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = reference.libName,
          fontSize = 12.sp,
          fontWeight = FontWeight(500),
          color = colorScheme.onSurfaceVariantSummary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Text(
        text = reference.referredList.size.toString(),
        fontSize = 13.sp,
        fontWeight = FontWeight(550),
        color = colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(start = 12.dp)
      )
    }
  }
}

@Composable
private fun buildReferenceMenuEntries(
  options: Int,
  onToggleOption: (Int, Boolean) -> Unit
): List<DropdownEntry> {
  val filters = listOf(
    LibReferenceOptions.NATIVE_LIBS to stringResource(R.string.ref_category_native),
    LibReferenceOptions.SERVICES to stringResource(R.string.ref_category_service),
    LibReferenceOptions.ACTIVITIES to stringResource(R.string.ref_category_activity),
    LibReferenceOptions.RECEIVERS to stringResource(R.string.ref_category_service),
    LibReferenceOptions.PROVIDERS to stringResource(R.string.ref_category_service),
    LibReferenceOptions.METADATA to stringResource(R.string.ref_category_metadata),
    LibReferenceOptions.PACKAGES to stringResource(R.string.ref_category_package),
    LibReferenceOptions.SHARED_UID to stringResource(R.string.ref_category_shared_uid),
    LibReferenceOptions.ONLY_NOT_MARKED to stringResource(R.string.ref_category_only_not_marked)
  )
  return listOf(
    DropdownEntry(
      items = listOf(
        DropdownItem(
          text = stringResource(R.string.menu_filter),
          children = filters.map { (option, label) ->
            DropdownItem(
              text = label,
              selected = options and option != 0,
              onClick = { onToggleOption(option, options and option == 0) }
            )
          }
        )
      )
    )
  )
}
