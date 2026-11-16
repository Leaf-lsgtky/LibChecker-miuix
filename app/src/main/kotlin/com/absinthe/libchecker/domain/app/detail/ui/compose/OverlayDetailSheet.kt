package com.absinthe.libchecker.domain.app.detail.ui.compose

import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.detail.model.OverlayDetailBottomSheetDisplay
import com.absinthe.libchecker.domain.app.detail.model.OverlayDetailBottomSheetResult
import com.absinthe.libchecker.domain.app.detail.model.OverlayTargetPackageDisplay
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.ui.AppDetailActivity
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.list.ui.compose.AppIcon
import com.absinthe.libchecker.ui.compose.component.SheetCardListSkeleton
import com.absinthe.libchecker.ui.compose.theme.lcCondensedFontFamily
import com.absinthe.libchecker.ui.compose.theme.withCustomFont
import com.absinthe.libchecker.ui.compose.util.sheetContentSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetHeightTransition
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/** Overlay / themed icon detail sheet, explaining which package an overlay belongs to. */
@Composable
fun OverlayDetailSheet(
  show: Boolean,
  viewModel: DetailViewModel,
  item: LCItem?,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var display by remember { mutableStateOf<OverlayDetailBottomSheetDisplay?>(null) }
  var loading by remember(show, item?.packageName) { mutableStateOf(show && item != null) }

  LaunchedEffect(show, item?.packageName) {
    if (!show || item == null) return@LaunchedEffect
    loading = true
    display = when (val result = viewModel.getOverlayDetailBottomSheetResult(item)) {
      is OverlayDetailBottomSheetResult.Available -> result.display
      OverlayDetailBottomSheetResult.NotFound -> null
    }
    loading = false
  }

  WindowBottomSheet(
    show = show,
    title = stringResource(R.string.overlay_detail_title),
    onDismissRequest = onDismiss
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp)
        .sheetContentSafePadding()
        .sheetHeightTransition(),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      var lastDisplay by remember(item?.packageName) {
        mutableStateOf<OverlayDetailBottomSheetDisplay?>(null)
      }
      if (!loading && display != null) {
        lastDisplay = display
      }
      val mode = when {
        loading -> OverlayDetailSheetMode.Loading
        display == null -> OverlayDetailSheetMode.Empty
        else -> OverlayDetailSheetMode.Content
      }
      Crossfade(
        targetState = mode,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "OverlayDetailSheetTransition"
      ) { stateMode ->
        when (stateMode) {
          OverlayDetailSheetMode.Loading -> SheetCardListSkeleton(
            rowCount = 2,
            showIcon = true,
            twoLine = true
          )

          OverlayDetailSheetMode.Empty -> Text(
            text = stringResource(R.string.empty_list),
            style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
          )

          OverlayDetailSheetMode.Content -> {
            val current = lastDisplay ?: return@Crossfade
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.defaultColors(
                  color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                ),
                insideMargin = PaddingValues(14.dp)
              ) {
                Column(Modifier.fillMaxWidth()) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = remember(current.applicationInfo) {
                      runCatching {
                        current.applicationInfo?.loadIcon(context.packageManager)
                      }.getOrNull()
                    }
                    if (icon != null) {
                      AsyncImage(
                        model = icon,
                        contentDescription = null,
                        modifier = Modifier
                          .padding(end = 12.dp)
                          .size(44.dp)
                      )
                    }
                    Column(Modifier.weight(1f)) {
                      Text(
                        text = current.appName ?: current.packageName,
                        style = MiuixTheme.textStyles.title4.copy(fontSize = 15.sp),
                        color = MiuixTheme.colorScheme.onSurface
                      )
                      Text(
                        text = current.packageName,
                        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                      )
                      Text(
                        text = current.versionInfo,
                        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp)
                          .withCustomFont(lcCondensedFontFamily()),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                      )
                    }
                  }
                  Spacer(Modifier.height(10.dp))
                  OverlayInfoRow(stringResource(R.string.overlay_detail_type), current.extraInfo.type)
                  OverlayInfoRow("Target SDK", current.extraInfo.targetSdkInfo)
                  OverlayInfoRow("Min SDK", current.extraInfo.minSdkInfo)
                  OverlayInfoRow("Compile SDK", current.extraInfo.compileSdkInfo)
                  OverlayInfoRow(stringResource(R.string.overlay_detail_size), current.extraInfo.sizeInfo)
                }
              }
              when (val target = current.target) {
                is OverlayTargetPackageDisplay.RelatedApp -> Card(
                  modifier = Modifier.fillMaxWidth(),
                  colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                  ),
                  onClick = {
                    context.startActivity(
                      Intent(context, AppDetailActivity::class.java).apply {
                        putExtra(EXTRA_PACKAGE_NAME, target.data.packageName)
                      }
                    )
                  },
                  showIndication = true,
                  insideMargin = PaddingValues(14.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    AppIcon(
                      packageInfo = target.data.packageInfo,
                      label = target.data.label,
                      modifier = Modifier
                        .padding(end = 12.dp)
                        .size(40.dp)
                    )
                    Column(Modifier.weight(1f)) {
                      Text(
                        text = target.data.label,
                        style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp),
                        color = MiuixTheme.colorScheme.onSurface
                      )
                      Text(
                        text = target.data.packageName,
                        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                      )
                    }
                  }
                }

                is OverlayTargetPackageDisplay.PackageName -> Card(
                  modifier = Modifier.fillMaxWidth(),
                  colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                  ),
                  insideMargin = PaddingValues(14.dp)
                ) {
                  OverlayInfoRow(
                    stringResource(R.string.overlay_detail_target),
                    target.value
                  )
                }

                OverlayTargetPackageDisplay.Empty -> Unit
              }
            }
          }
        }
      }
    }
  }
}

private enum class OverlayDetailSheetMode {
  Loading,
  Empty,
  Content
}

@Composable
private fun OverlayInfoRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      modifier = Modifier.weight(1f),
      style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
      color = MiuixTheme.colorScheme.onSurfaceVariantSummary
    )
    Text(
      text = value,
      style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp)
        .withCustomFont(lcCondensedFontFamily()),
      color = MiuixTheme.colorScheme.onSurface
    )
  }
}
