package com.absinthe.libchecker.domain.app.list.ui.compose

import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import com.absinthe.libchecker.domain.app.list.model.AppListItemViewState
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.constant.Constants
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.constant.options.withOption
import com.absinthe.libchecker.domain.app.list.model.buildAdvancedMenuBottomSheetState
import com.absinthe.libchecker.ui.compose.util.sheetContentSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetHeightTransition
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.menu.OverlayIconCascadingDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Sort / filter / appearance menu for the app list.
 *
 * A cascading dropdown anchored to the icon instead of a bottom sheet, so it matches the Miuix
 * menu language: sort options are top level, filter and appearance options live in a second level.
 */
class AppListMenuState(
  val entries: List<DropdownEntry>,
  private val sheetsContent: @Composable () -> Unit
) {
  @Composable
  fun Sheets() {
    sheetsContent()
  }
}

@Composable
fun rememberAppListMenuState(
  displayOptions: Int,
  onDisplayOptionsChange: (Int) -> Unit,
  onDismissMenu: () -> Unit = {}
): AppListMenuState {
  val state = remember(displayOptions) {
    buildAdvancedMenuBottomSheetState(displayOptions = displayOptions)
  }
  val currentOnChange by rememberUpdatedState(onDisplayOptionsChange)
  val currentOnDismissMenu by rememberUpdatedState(onDismissMenu)
  val latestDisplayOptions by rememberUpdatedState(displayOptions)
  var showPreview by remember { mutableStateOf(false) }
  val previewTitle = stringResource(R.string.adv_app_appearance_preview)
  val sortByName = stringResource(R.string.adv_sort_by_name)
  val sortByTime = stringResource(R.string.adv_sort_by_time)
  val sortByTarget = stringResource(R.string.adv_sort_by_target_version)
  val filterTitle = stringResource(R.string.adv_app_filter)
  val appearanceTitle = stringResource(R.string.adv_app_appearance)
  val filterLabels = state.filterOptions.map { stringResource(it.labelRes) }
  val viewLabels = state.viewOptions.map { stringResource(it.labelRes) }

  val entries = remember(
    displayOptions,
    previewTitle,
    sortByName,
    sortByTime,
    sortByTarget,
    filterTitle,
    appearanceTitle,
    filterLabels,
    viewLabels
  ) {
    fun sorted(mode: Int): Int = (displayOptions and SORT_MASK.inv()) or mode

    listOf(
      DropdownEntry(
        items = listOf(
          DropdownItem(
            text = sortByName,
            selected = displayOptions and AdvancedOptions.SORT_BY_NAME != 0,
            onClick = { currentOnChange(sorted(AdvancedOptions.SORT_BY_NAME)) }
          ),
          DropdownItem(
            text = sortByTime,
            selected = displayOptions and AdvancedOptions.SORT_BY_UPDATE_TIME != 0,
            onClick = { currentOnChange(sorted(AdvancedOptions.SORT_BY_UPDATE_TIME)) }
          ),
          DropdownItem(
            text = sortByTarget,
            selected = displayOptions and AdvancedOptions.SORT_BY_TARGET_API != 0,
            onClick = { currentOnChange(sorted(AdvancedOptions.SORT_BY_TARGET_API)) }
          )
        )
      ),
      DropdownEntry(
        items = listOf(
          DropdownItem(
            text = filterTitle,
            children = state.filterOptions.mapIndexed { index, option ->
              val checked = displayOptions and option.option != 0
              DropdownItem(
                text = filterLabels[index],
                selected = checked,
                onClick = { currentOnChange(displayOptions.withOption(option.option, !checked)) }
              )
            }
          )
        )
      ),
      DropdownEntry(
        items = listOf(
          DropdownItem(
            text = appearanceTitle,
            children = state.viewOptions.mapIndexed { index, option ->
              val checked = displayOptions and option.option != 0
              DropdownItem(
                text = viewLabels[index],
                selected = checked,
                onClick = { currentOnChange(displayOptions.withOption(option.option, !checked)) }
              )
            } + DropdownItem(
              text = previewTitle,
              onClick = {
                currentOnDismissMenu()
                showPreview = true
              }
            )
          )
        )
      )
    )
  }

  val sheetsContent: @Composable () -> Unit = remember {
    {
      AppListPreviewSheet(
        show = showPreview,
        displayOptions = latestDisplayOptions,
        onDismiss = { showPreview = false }
      )
    }
  }
  return remember(entries, sheetsContent) {
    AppListMenuState(
      entries = entries,
      sheetsContent = sheetsContent
    )
  }
}

private const val SORT_MASK =
  AdvancedOptions.SORT_BY_NAME or AdvancedOptions.SORT_BY_UPDATE_TIME or AdvancedOptions.SORT_BY_TARGET_API

/** Live preview of the list appearance options applied to a demo row. */
@Composable
private fun AppListPreviewSheet(
  show: Boolean,
  displayOptions: Int,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val item = remember {
    LCItem(
      packageName = Constants.EXAMPLE_PACKAGE,
      label = "LibChecker",
      versionName = "2.0.0",
      versionCode = 2000L,
      installedTime = System.currentTimeMillis(),
      lastUpdatedTime = System.currentTimeMillis(),
      isSystem = false,
      abi = Constants.ARMV8.toShort(),
      features = 0,
      targetApi = 34.toShort(),
      variant = 0.toShort()
    )
  }
  val viewState = remember(item, displayOptions) {
    AppListItemViewState.createPending(context, item, displayOptions)
  }

  WindowBottomSheet(
    show = show,
    title = stringResource(R.string.adv_app_appearance_preview),
    insideMargin = DpSize(0.dp, 0.dp),
    onDismissRequest = onDismiss
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp)
        .sheetContentSafePadding()
        .sheetHeightTransition()
    ) {
      AppListRow(
        item = item,
        viewState = viewState,
        packageInfo = null,
        chips = emptyList(),
        highlight = "",
        onClick = {},
        colors = CardDefaults.defaultColors(
          color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.04f)
        )
      )
    }
  }
}
