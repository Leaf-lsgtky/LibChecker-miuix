package com.absinthe.libchecker.domain.app.detail.ui.compose

import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.AppInfoActionItem
import com.absinthe.libchecker.domain.app.detail.model.AppInfoBottomSheetState
import com.absinthe.libchecker.domain.app.detail.model.externalActions
import com.absinthe.libchecker.domain.app.detail.model.launcherActivityCopyText
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel.AppPackageShareActionResult
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel.AppPackageShareExportResult
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel.AppPackageShareTarget
import com.absinthe.libchecker.domain.app.list.ui.compose.AppIcon
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import com.absinthe.libchecker.utils.showToast
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/** App info sheet: launch/settings/share/export plus the external actions of the app. */
@Composable
fun AppInfoSheet(
  show: Boolean,
  viewModel: DetailViewModel,
  packageInfo: PackageInfo,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val colorScheme = MiuixTheme.colorScheme
  val packageName = packageInfo.packageName
  var state by remember { mutableStateOf<AppInfoBottomSheetState>(AppInfoBottomSheetState.Loading(packageName)) }

  var alternatives by remember { mutableStateOf<List<com.absinthe.libchecker.domain.app.detail.model.AlternativeLaunchItem>?>(null) }

  var pendingExportFile by remember { mutableStateOf<com.absinthe.libchecker.domain.app.detail.action.AppPackageShareFile?>(null) }

  val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/octet-stream")
  ) { uri: Uri? ->
    val file = pendingExportFile
    pendingExportFile = null
    if (uri != null && file != null) {
      scope.launch { viewModel.exportAppPackageShareFile(file, uri) }
    }
  }

  LaunchedEffect(show) {
    if (!show) return@LaunchedEffect
    state = viewModel.getAppInfoBottomSheetState(packageName)
    launch {
      viewModel.appPackageShareActionResults.collect { result: AppPackageShareActionResult ->
        result.result.onSuccess { action ->
          when (result.target) {
            AppPackageShareTarget.SHARE -> {
              runCatching {
                context.startActivity(
                  Intent.createChooser(action.shareIntent, context.getString(R.string.app_info_share))
                )
              }.onFailure { context.showToast(R.string.toast_cant_open_app) }
            }

            AppPackageShareTarget.EXPORT -> {
              pendingExportFile = action.shareFile
              exportLauncher.launch(action.shareFile.file.name)
            }
          }
        }.onFailure { context.showToast(R.string.toast_cant_open_app) }
      }
    }
    launch {
      viewModel.appPackageShareExportResults.collect { result: AppPackageShareExportResult ->
        result.result.onFailure {
          context.showToast(R.string.toast_cant_open_app)
        }
      }
    }
  }

  fun launchApp() {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (intent == null) {
      context.showToast(R.string.toast_cant_open_app)
      scope.launch {
        alternatives = runCatching { viewModel.getAlternativeLaunchItems(packageName) }
          .getOrNull()
          .orEmpty()
      }
      return
    }
    runCatching { context.startActivity(intent) }
      .onFailure {
        context.showToast(R.string.toast_cant_open_app)
        scope.launch {
          alternatives = runCatching { viewModel.getAlternativeLaunchItems(packageName) }
            .getOrNull()
            .orEmpty()
        }
      }
  }

  fun openSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
      .setData(Uri.fromParts("package", packageName, null))
    runCatching { context.startActivity(intent) }
      .onFailure { context.showToast(R.string.toast_cant_open_app) }
  }

  fun share(target: AppPackageShareTarget) {
    viewModel.prepareAppPackageShareAction(context.cacheDir, packageName, target)
  }

  WindowBottomSheet(
    show = show,
    title = stringResource(R.string.further_operation),
    onDismissRequest = onDismiss
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(
          packageInfo = packageInfo,
          label = packageInfo.packageName,
          modifier = Modifier
            .padding(end = 12.dp)
            .size(44.dp)
        )
        Column(Modifier.weight(1f)) {
          Text(
            text = packageInfo.applicationInfo
              ?.loadLabel(context.packageManager)
              ?.toString()
              ?: packageName,
            fontWeight = FontWeight(600),
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(Modifier.height(2.dp))
          Text(
            text = packageName,
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
      Spacer(Modifier.height(14.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PrimaryAction(
          label = stringResource(R.string.app_info_launch),
          onClick = ::launchApp,
          onLongClick = {
            state.launcherActivityCopyText?.let { text ->
              context.copyTextWithToast(text)
            }
          },
          modifier = Modifier.weight(1f)
        )
        PrimaryAction(
          label = stringResource(R.string.app_info_settings),
          onClick = ::openSettings,
          modifier = Modifier.weight(1f)
        )
        PrimaryAction(
          label = stringResource(R.string.app_info_share),
          onClick = { share(AppPackageShareTarget.SHARE) },
          modifier = Modifier.weight(1f)
        )
        PrimaryAction(
          label = stringResource(R.string.app_info_export),
          onClick = { share(AppPackageShareTarget.EXPORT) },
          modifier = Modifier.weight(1f)
        )
      }
      Spacer(Modifier.height(12.dp))
      when (val current = state) {
        is AppInfoBottomSheetState.Loading -> {
          Box(
            Modifier
              .fillMaxWidth()
              .height(120.dp)
          ) {
            LcLoadingState(message = stringResource(R.string.loading))
          }
        }

        is AppInfoBottomSheetState.Content -> {
          val actions = current.externalActions
          if (actions.isEmpty()) {
            Spacer(Modifier.height(8.dp))
          } else {
            actions.forEach { item ->
              ExternalActionRow(
                item = item,
                onClick = {
                  runCatching { context.startActivity(item.intent) }
                    .onFailure { context.showToast(R.string.toast_cant_open_app) }
                }
              )
            }
          }
        }
      }
    }
  }

  alternatives?.let { items ->
    WindowBottomSheet(
      show = true,
      title = stringResource(R.string.app_info_alternative_launch),
      onDismissRequest = { alternatives = null }
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .padding(bottom = 16.dp)
      ) {
        items.forEach { item ->
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                alternatives = null
                runCatching {
                  context.startActivity(
                    Intent().setClassName(packageName, item.className)
                  )
                }.onFailure { context.showToast(R.string.toast_cant_open_app) }
              }
              .padding(vertical = 8.dp)
          ) {
            Text(
              text = item.label,
              fontSize = 14.sp,
              fontWeight = FontWeight(550),
              color = MiuixTheme.colorScheme.onSurface
            )
            Text(
              text = item.className,
              fontSize = 12.sp,
              color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PrimaryAction(
  label: String,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .squircleBackground(MiuixTheme.colorScheme.surfaceContainerHigh, 14.dp)
      .combinedClickable(onClick = onClick, onLongClick = onLongClick)
      .padding(vertical = 10.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      fontSize = 13.sp,
      fontWeight = FontWeight(550),
      color = MiuixTheme.colorScheme.onSurface,
      maxLines = 1
    )
  }
}

@Composable
private fun ExternalActionRow(item: AppInfoActionItem, onClick: () -> Unit) {
  val colorScheme = MiuixTheme.colorScheme
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    val icon = item.icon
    if (icon != null) {
      val bitmap = remember(icon) { icon.toBitmap(24, 24).asImageBitmap() }
      androidx.compose.foundation.Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = Modifier
          .padding(end = 12.dp)
          .size(24.dp)
      )
    }
    Text(
      text = item.label.toString(),
      fontSize = 14.sp,
      color = colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

