package com.absinthe.libchecker.domain.snapshot.list.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.options.SnapshotOptions
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/** Snapshot list display options. */
@Composable
fun SnapshotMenuSheet(
  show: Boolean,
  viewModel: SnapshotViewModel,
  onDismiss: () -> Unit
) {
  var options by remember(show) { mutableStateOf(viewModel.getSnapshotOptions()) }

  fun toggle(option: Int, checked: Boolean) {
    options = viewModel.setSnapshotOption(option, checked)
  }

  WindowBottomSheet(
    show = show,
    title = stringResource(R.string.advanced_menu),
    onDismissRequest = onDismiss
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp)
    ) {
      SnapshotOptionRow(
        label = stringResource(R.string.snapshot_menu_show_update_time),
        checked = options and SnapshotOptions.SHOW_UPDATE_TIME != 0,
        onCheckedChange = { toggle(SnapshotOptions.SHOW_UPDATE_TIME, it) }
      )
      SnapshotOptionRow(
        label = stringResource(R.string.snapshot_menu_hide_no_component_changes),
        checked = options and SnapshotOptions.HIDE_NO_COMPONENT_CHANGES != 0,
        onCheckedChange = { toggle(SnapshotOptions.HIDE_NO_COMPONENT_CHANGES, it) }
      )
      SnapshotOptionRow(
        label = stringResource(R.string.snapshot_menu_diff_highlight),
        checked = options and SnapshotOptions.DIFF_HIGHLIGHT != 0,
        onCheckedChange = { toggle(SnapshotOptions.DIFF_HIGHLIGHT, it) }
      )
      SnapshotOptionRow(
        label = stringResource(R.string.snapshot_menu_diff_emphasis),
        checked = options and SnapshotOptions.DIFF_EMPHASIS != 0,
        onCheckedChange = { toggle(SnapshotOptions.DIFF_EMPHASIS, it) }
      )
      SnapshotOptionRow(
        label = stringResource(R.string.snapshot_menu_use_iec_units),
        checked = options and SnapshotOptions.USE_IEC_UNITS != 0,
        onCheckedChange = { toggle(SnapshotOptions.USE_IEC_UNITS, it) }
      )
    }
  }
}

@Composable
private fun SnapshotOptionRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      modifier = Modifier.weight(1f),
      fontSize = 14.sp,
      fontWeight = FontWeight(500),
      color = MiuixTheme.colorScheme.onSurface
    )
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}