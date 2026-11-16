package com.absinthe.libchecker.domain.app.detail.ui.compose

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.absinthe.libchecker.ui.compose.component.SheetCardListSkeleton
import com.absinthe.libchecker.ui.compose.util.sheetBottomSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetContentSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetHeightTransition
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
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
  var loading by remember(show, packageName) { mutableStateOf(show) }

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
        .sheetHeightTransition()
        .verticalScroll(rememberScrollState())
        .padding(bottom = sheetBottomSafePadding(16.dp)),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      var lastDisplay by remember(packageName) {
        mutableStateOf<XposedInfoBottomSheetDisplay?>(null)
      }
      if (!loading && display != null) {
        lastDisplay = display
      }
      val mode = when {
        loading -> XposedInfoSheetMode.Loading
        display == null -> XposedInfoSheetMode.Empty
        else -> XposedInfoSheetMode.Content
      }
      Crossfade(
        targetState = mode,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "XposedInfoSheetTransition"
      ) { stateMode ->
        when (stateMode) {
          XposedInfoSheetMode.Loading -> SheetCardListSkeleton(
            rowCount = 3,
            showIcon = false,
            twoLine = true
          )

          XposedInfoSheetMode.Empty -> Text(
            text = stringResource(R.string.empty_list),
            style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
          )

          XposedInfoSheetMode.Content -> {
            val content = lastDisplay ?: return@Crossfade
            val openSettingsIntent = (content.settingsAction as? XposedInfoAction.OpenSettings)?.intent
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.defaultColors(
                  color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                ),
                onClick = openSettingsIntent?.let { intent ->
                  { scope.launch { runCatching { context.startActivity(intent) } } }
                },
                showIndication = openSettingsIntent != null,
                insideMargin = PaddingValues(14.dp)
              ) {
                Text(
                  text = content.appName,
                  style = MiuixTheme.textStyles.title4.copy(fontSize = 15.sp),
                  color = MiuixTheme.colorScheme.onSurface
                )
              }
              content.items.forEach { item ->
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                  ),
                  insideMargin = PaddingValues(14.dp)
                ) {
                  when (item) {
                    is XposedInfoItemDisplay.Text -> Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      AsyncImage(
                        model = item.iconRes,
                        contentDescription = null,
                        modifier = Modifier
                          .padding(end = 12.dp)
                          .size(18.dp)
                      )
                      Column(Modifier.weight(1f)) {
                        Text(
                          text = item.tip,
                          style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
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

                    is XposedInfoItemDisplay.ScopeApps -> Column(Modifier.fillMaxWidth()) {
                      Text(
                        text = item.tip,
                        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                      )
                      Spacer(Modifier.height(6.dp))
                      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                              style = MiuixTheme.textStyles.footnote2.copy(fontSize = 10.sp),
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
    }
  }
}

private enum class XposedInfoSheetMode {
  Loading,
  Empty,
  Content
}
