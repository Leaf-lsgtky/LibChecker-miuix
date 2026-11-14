package com.absinthe.libchecker.domain.app.list.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.constant.options.withOption
import com.absinthe.libchecker.domain.app.list.model.buildAdvancedMenuBottomSheetState
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.menu.OverlayIconCascadingDropdownMenu

/**
 * Sort / filter / appearance menu for the app list.
 *
 * A cascading dropdown anchored to the icon instead of a bottom sheet, so it matches the Miuix
 * menu language: sort options are top level, filter and appearance options live in a second level.
 */
@Composable
fun AppListMenu(
  displayOptions: Int,
  onDisplayOptionsChange: (Int) -> Unit,
  content: @Composable () -> Unit
) {
  val state = remember(displayOptions) {
    buildAdvancedMenuBottomSheetState(displayOptions = displayOptions)
  }
  val currentOnChange by rememberUpdatedState(onDisplayOptionsChange)
  val sortByName = stringResource(R.string.adv_sort_by_name)
  val sortByTime = stringResource(R.string.adv_sort_by_time)
  val sortByTarget = stringResource(R.string.adv_sort_by_target_version)
  val filterTitle = stringResource(R.string.adv_app_filter)
  val appearanceTitle = stringResource(R.string.adv_app_appearance)
  val filterLabels = state.filterOptions.map { stringResource(it.labelRes) }
  val viewLabels = state.viewOptions.map { stringResource(it.labelRes) }

  val entries = remember(
    displayOptions,
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
            }
          )
        )
      )
    )
  }

  OverlayIconCascadingDropdownMenu(
    entries = entries,
    collapseOnSelection = false,
    content = content
  )
}

private const val SORT_MASK =
  AdvancedOptions.SORT_BY_NAME or AdvancedOptions.SORT_BY_UPDATE_TIME or AdvancedOptions.SORT_BY_TARGET_API
