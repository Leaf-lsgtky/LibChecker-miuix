package com.absinthe.libchecker.domain.app.detail.ui.compose

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
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
  var loading by remember { mutableStateOf(false) }

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
    title = item?.label ?: stringResource(R.string.overlay_detail_title),
    onDismissRequest = onDismiss
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp)
    ) {
      val current = display
      when {
        loading -> LcLoadingState(
          modifier = Modifier.height(140.dp),
          message = stringResource(R.string.loading)
        )

        current == null -> Text(
          text = stringResource(R.string.empty_list),
          fontSize = 13.sp,
          color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )

        else -> {
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
                fontSize = 15.sp,
                fontWeight = FontWeight(600),
                color = MiuixTheme.colorScheme.onSurface
              )
              Text(
                text = current.packageName,
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
              )
              Text(
                text = current.versionInfo,
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
              )
            }
          }
          Spacer(Modifier.height(12.dp))
          OverlayInfoRow(stringResource(R.string.overlay_detail_type), current.extraInfo.type)
          OverlayInfoRow("Target SDK", current.extraInfo.targetSdkInfo)
          OverlayInfoRow("Min SDK", current.extraInfo.minSdkInfo)
          OverlayInfoRow("Compile SDK", current.extraInfo.compileSdkInfo)
          OverlayInfoRow(stringResource(R.string.overlay_detail_size), current.extraInfo.sizeInfo)
          Spacer(Modifier.height(12.dp))
          when (val target = current.target) {
            is OverlayTargetPackageDisplay.RelatedApp -> Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  context.startActivity(
                    Intent(context, AppDetailActivity::class.java).apply {
                      putExtra(EXTRA_PACKAGE_NAME, target.data.packageName)
                    }
                  )
                },
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
                  fontSize = 14.sp,
                  fontWeight = FontWeight(550),
                  color = MiuixTheme.colorScheme.onSurface
                )
                Text(
                  text = target.data.packageName,
                  fontSize = 12.sp,
                  color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
              }
            }

            is OverlayTargetPackageDisplay.PackageName -> OverlayInfoRow(
              stringResource(R.string.overlay_detail_target),
              target.value
            )

            OverlayTargetPackageDisplay.Empty -> Unit
          }
        }
      }
    }
  }
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
      fontSize = 12.sp,
      color = MiuixTheme.colorScheme.onSurfaceVariantSummary
    )
    Text(
      text = value,
      fontSize = 12.sp,
      color = MiuixTheme.colorScheme.onSurface
    )
  }
}