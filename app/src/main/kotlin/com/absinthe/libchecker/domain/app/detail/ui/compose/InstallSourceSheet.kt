package com.absinthe.libchecker.domain.app.detail.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import top.yukonga.miuix.kmp.squircle.squircleBackground
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.absinthe.libchecker.domain.app.detail.action.AppInstalledTimeDisplayData
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceBottomSheetDisplay
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceItemContent
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceRequesterAccess
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.list.ui.compose.AppIcon
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import com.absinthe.libchecker.utils.dex.DexOptimizationReader
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/** Install source sheet: originating/installing packages, install times and dexopt state. */
@Composable
fun InstallSourceSheet(
  show: Boolean,
  viewModel: DetailViewModel,
  packageName: String,
  onOpenApp: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var display by remember { mutableStateOf<AppInstallSourceBottomSheetDisplay?>(null) }
  var loading by remember { mutableStateOf(false) }

  LaunchedEffect(show, packageName) {
    if (!show) return@LaunchedEffect
    loading = true
    display = null
    viewModel.loadAppInstallSourceDetails(
      packageName = packageName,
      requesterAccess = AppInstallSourceRequesterAccess.Available
    )
  }

  LaunchedEffect(viewModel) {
    viewModel.appInstallSourceDetailsResults.collect { result ->
      if (result.packageName == packageName) {
        display = result.display
        loading = false
      }
    }
  }

  WindowBottomSheet(
    show = show,
    title = stringResource(R.string.lib_detail_app_install_source_title),
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
          style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
          color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )

        else -> {
          val content = display ?: return@Column
          content.originatingApp?.let { item ->
            InstallSourceAppRow(
              title = stringResource(R.string.lib_detail_app_install_source_originating_package),
              item = item,
              onOpenApp = onOpenApp
            )
          }
          content.installingApp?.let { item ->
            InstallSourceAppRow(
              title = stringResource(R.string.lib_detail_app_install_source_installing_package),
              item = item,
              onOpenApp = onOpenApp
            )
          }
          content.installedTime?.let { time ->
            InstalledTimeBlock(time)
          }
          content.dexoptInfo?.let { info ->
            DexoptBlock(info)
          }
        }
      }
    }
  }
}

@Composable
private fun InstallSourceAppRow(
  title: String,
  item: com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceItemDisplay,
  onOpenApp: (String) -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  Column(modifier = Modifier.padding(vertical = 6.dp)) {
    Text(
      text = title,
      style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
      color = colorScheme.onSurfaceVariantSummary
    )
    Spacer(Modifier.height(4.dp))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable(enabled = item.action != null) {
          when (val action = item.action) {
            is com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceAction.OpenApp ->
              onOpenApp(action.item.packageName)

            else -> Unit
          }
        },
      verticalAlignment = Alignment.CenterVertically
    ) {
      when (val content = item.content) {
        is AppInstallSourceItemContent.RelatedApp -> {
          AppIcon(
            packageInfo = content.data.packageInfo,
            label = content.data.label,
            modifier = Modifier
              .padding(end = 12.dp)
              .size(40.dp)
          )
          Column(Modifier.weight(1f)) {
            Text(
              text = content.data.label,
              style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp),
              color = colorScheme.onSurface
            )
            Text(
              text = content.data.packageName,
              style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
              color = colorScheme.onSurfaceVariantSummary
            )
            Text(
              text = content.data.versionInfo,
              style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
              color = colorScheme.onSurfaceVariantSummary
            )
          }
        }

        is AppInstallSourceItemContent.Message -> {
          if (content.iconRes != 0) {
            AsyncImage(
              model = content.iconRes,
              contentDescription = null,
              modifier = Modifier
                .padding(end = 12.dp)
                .size(40.dp)
            )
          }
          Column(Modifier.weight(1f)) {
            Text(
              text = content.appName,
              style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp),
              color = colorScheme.onSurface
            )
            Text(
              text = content.packageName,
              style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
              color = colorScheme.onSurfaceVariantSummary
            )
            Text(
              text = content.versionInfo,
              style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
              color = colorScheme.onSurfaceVariantSummary
            )
          }
        }
      }
    }
  }
}

@Composable
private fun InstalledTimeBlock(time: AppInstalledTimeDisplayData) {
  val colorScheme = MiuixTheme.colorScheme
  Column(modifier = Modifier.padding(vertical = 6.dp)) {
    Text(
      text = stringResource(R.string.lib_detail_app_first_installed_time) + " " + time.firstInstalledTime,
      style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
      color = colorScheme.onSurface
    )
    Text(
      text = stringResource(R.string.lib_detail_app_last_updated_time) + " " + time.lastUpdatedTime,
      style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
      color = colorScheme.onSurface
    )
  }
}

@Composable
private fun DexoptBlock(info: DexOptimizationReader.DexFileOptimizationInfo) {
  val context = LocalContext.current
  val colorScheme = MiuixTheme.colorScheme
  val allText = buildString {
    append(context.getString(R.string.lib_detail_app_dexopt_title))
    append('\n')
    append(context.getString(R.string.lib_detail_app_dexopt_status))
    append(' ')
    append(info.status)
    append('\n')
    append(context.getString(R.string.lib_detail_app_dexopt_reason))
    append(' ')
    append(info.reason)
  }
  Column(modifier = Modifier.padding(vertical = 6.dp)) {
    Text(
      text = stringResource(R.string.lib_detail_app_dexopt_title),
      style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
      color = colorScheme.onSurfaceVariantSummary
    )
    Spacer(Modifier.height(6.dp))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .combinedClickable(
          onClick = {},
          onLongClick = { context.copyTextWithToast(allText) }
        )
    ) {
      DexoptValueCard(
        label = stringResource(R.string.lib_detail_app_dexopt_status),
        value = info.status,
        modifier = Modifier.weight(1f)
      )
      Spacer(Modifier.width(8.dp))
      DexoptValueCard(
        label = stringResource(R.string.lib_detail_app_dexopt_reason),
        value = info.reason,
        modifier = Modifier.weight(1f)
      )
    }
    info.instructionSet?.let { instructionSet ->
      Spacer(Modifier.height(6.dp))
      Text(
        text = instructionSet,
        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
        color = colorScheme.onSurfaceVariantSummary
      )
    }
  }
}

@Composable
private fun DexoptValueCard(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  val colorScheme = MiuixTheme.colorScheme
  Column(
    modifier = modifier
      .squircleBackground(colorScheme.surfaceContainerHigh, 12.dp)
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    Text(
      text = label,
      style = MiuixTheme.textStyles.footnote2.copy(fontSize = 11.sp),
      color = colorScheme.onSurfaceVariantSummary
    )
    Text(
      text = value,
      style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp),
      color = colorScheme.onSurface
    )
  }
}
