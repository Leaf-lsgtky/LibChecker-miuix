package com.absinthe.libchecker.domain.app.detail.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.DISABLED
import com.absinthe.libchecker.domain.app.detail.model.EXPORTED
import com.absinthe.libchecker.domain.app.detail.model.ComponentMenuState
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/** Advanced component display menu: preview rows plus per-row display toggles. */
@Composable
fun ComponentMenuSheet(
  show: Boolean,
  viewModel: DetailViewModel,
  rulePackageName: String,
  onDismiss: () -> Unit
) {
  var state by remember { mutableStateOf<ComponentMenuState?>(null) }

  LaunchedEffect(show, rulePackageName) {
    if (show) {
      state = viewModel.getComponentMenuState(rulePackageName)
    }
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
      val current = state ?: return@Column
      Text(
        text = stringResource(R.string.lib_detail_component_demo),
        fontSize = 12.sp,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
      )
      Spacer(Modifier.height(6.dp))
      current.componentDemoItems.forEach { chip ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = chip.item.name,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurface,
            textDecoration = if (chip.item.source == DISABLED) {
              TextDecoration.LineThrough
            } else {
              null
            }
          )
          if (chip.item.source == EXPORTED) {
            Text(
              text = stringResource(R.string.adv_mark_exported),
              fontSize = 10.sp,
              color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
          }
        }
      }
      Spacer(Modifier.height(12.dp))
      current.componentOptions.forEach { option ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = stringResource(option.labelRes),
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight(500),
            color = MiuixTheme.colorScheme.onSurface
          )
          Switch(
            checked = option.isChecked,
            onCheckedChange = { checked ->
              viewModel.setComponentDisplayOption(option.option, checked)
              state = viewModel.getComponentMenuState(rulePackageName)
            }
          )
        }
      }
    }
  }
}
