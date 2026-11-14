package com.absinthe.libchecker.domain.app.detail.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoAction
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoBottomSheetDisplay
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoItemDisplay
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoTextStyle
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.list.ui.compose.AppIcon
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/** Xposed module information sheet. */
@Composable
fun XposedInfoSheet(
  show: Boolean,
  viewModel: DetailViewModel,
  packageName: String,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var display by remember { mutableStateOf<XposedInfoBottomSheetDisplay?>(null) }
  var loading by remember { mutableStateOf(false) }

  LaunchedEffect(show, packageName) {
    if (!show) return@LaunchedEffect
    loading = true
    display = runCatching { viewModel.getXposedInfoBottomSheetDisplay(packageName) }.getOrNull()
    loading = false
  }

  WindowBottomSheet(
    show = show,
    title = stringResource(R.string.xposed_module),
    onDismissRequest = onDismiss
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp)
        .heightIn(max = 520.dp)
        .verticalScroll(rememberScrollState())
    ) {
      when {
        loading -> LcLoadingState(
          modifier = Modifier.height(140.dp),
          message = stringResource(R.string.loading)
        )

        display == null -> Text(
          text = stringResource(R.string.empty_list),
          fontSize = 13.sp,
          color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )

        else -> {
          val content = display ?: return@Column
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                (content.settingsAction as? XposedInfoAction.OpenSettings)?.intent?.let { intent ->
                  scope.launch { runCatching { context.startActivity(intent) } }
                }
              }
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = content.appName,
              modifier = Modifier.weight(1f),
              fontSize = 15.sp,
              fontWeight = FontWeight(600),
              color = MiuixTheme.colorScheme.onSurface
            )
          }
          content.items.forEach { item ->
            when (item) {
              is XposedInfoItemDisplay.Text -> Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                AsyncImage(
                  model = item.iconRes,
                  contentDescription = null,
                  modifier = Modifier
                    .padding(end = 10.dp)
                    .size(18.dp)
                )
                Column(Modifier.weight(1f)) {
                  Text(
                    text = item.tip,
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                  )
                  Text(
                    text = item.text,
                    fontSize = if (item.textStyle == XposedInfoTextStyle.Title) 14.sp else 13.sp,
                    fontWeight = if (item.textStyle == XposedInfoTextStyle.Title) {
                      FontWeight(550)
                    } else {
                      FontWeight(400)
                    },
                    color = MiuixTheme.colorScheme.onSurface
                  )
                }
              }

              is XposedInfoItemDisplay.ScopeApps -> Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Text(
                  text = item.tip,
                  fontSize = 12.sp,
                  color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                  items(item.apps, key = { it.packageName }) { app ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      AppIcon(
                        packageInfo = app.packageInfo,
                        label = app.label,
                        modifier = Modifier.size(36.dp)
                      )
                      Spacer(Modifier.height(4.dp))
                      Text(
                        text = app.label,
                        fontSize = 10.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
