package com.absinthe.libchecker.domain.app.detail.ui.compose

import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.Rocket
import top.yukonga.miuix.kmp.icon.glass.Settings
import top.yukonga.miuix.kmp.icon.glass.Share
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.AppInfoActionItem
import com.absinthe.libchecker.domain.app.detail.action.AppInfoLaunchAction
import com.absinthe.libchecker.domain.app.detail.model.AppInfoBottomSheetState
import com.absinthe.libchecker.domain.app.detail.model.externalActions
import com.absinthe.libchecker.domain.app.detail.model.launcherActivityCopyText
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel.AppPackageShareActionResult
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel.AppPackageShareExportResult
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel.AppPackageShareTarget
import com.absinthe.libchecker.ui.compose.component.AppInfoExternalActionsSkeleton
import com.absinthe.libchecker.ui.compose.util.rememberActiveValue
import com.absinthe.libchecker.ui.compose.util.sheetContentSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetHeightTransition
import com.absinthe.libchecker.utils.showToast
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
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

  suspend fun loadAlternatives() {
    alternatives = runCatching { viewModel.getAlternativeLaunchItems(packageName) }
      .getOrNull()
      .orEmpty()
  }

  fun launchApp() {
    scope.launch {
      val action = runCatching {
        viewModel.getAppInfoPrimaryActions(packageName).launchAction
      }.getOrNull()
      when (action) {
        AppInfoLaunchAction.Self -> context.showToast(R.string.app_info_launch_self_hint)

        AppInfoLaunchAction.Alternative -> loadAlternatives()

        is AppInfoLaunchAction.Available -> runCatching {
          context.startActivity(action.action.intent)
        }.onFailure {
          context.showToast(R.string.toast_cant_open_app)
          loadAlternatives()
        }

        null -> {
          context.showToast(R.string.toast_cant_open_app)
          loadAlternatives()
        }
      }
    }
  }

  fun openSettings() {
    scope.launch {
      val intent = runCatching {
        viewModel.getAppInfoPrimaryActions(packageName).settingsIntent
      }.getOrNull()
        ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
          .setData(Uri.fromParts("package", packageName, null))
      runCatching { context.startActivity(intent) }
        .onFailure { context.showToast(R.string.toast_cant_open_app) }
    }
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
        .padding(bottom = 16.dp)
        .sheetContentSafePadding()
        .sheetHeightTransition()
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        PrimaryAction(
          icon = MiuixIcons.Glass.Rocket,
          circleColor = Color(0xFF3482FF),
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
          icon = MiuixIcons.Glass.Settings,
          circleColor = Color(0xFF7B61FF),
          label = stringResource(R.string.app_info_settings),
          onClick = ::openSettings,
          modifier = Modifier.weight(1f)
        )
        PrimaryAction(
          icon = MiuixIcons.Glass.Share,
          circleColor = Color(0xFF22B592),
          label = stringResource(R.string.app_info_share),
          onClick = { share(AppPackageShareTarget.SHARE) },
          onLongClick = { share(AppPackageShareTarget.EXPORT) },
          modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.weight(1f))
      }
      val current = state
      var lastContent by remember(packageName) {
        mutableStateOf<AppInfoBottomSheetState.Content?>(null)
      }
      if (current is AppInfoBottomSheetState.Content) {
        lastContent = current
      }
      Crossfade(
        targetState = current is AppInfoBottomSheetState.Loading,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "AppInfoExternalActionsTransition"
      ) { isLoading ->
        if (isLoading) {
          Column(Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(10.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colorScheme.dividerLine)
            )
            AppInfoExternalActionsSkeleton()
          }
        } else {
          val actions = lastContent?.externalActions.orEmpty()
          if (actions.isNotEmpty()) {
            Column(Modifier.fillMaxWidth()) {
              Spacer(Modifier.height(10.dp))
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(0.5.dp)
                  .background(colorScheme.dividerLine)
              )
              Spacer(Modifier.height(12.dp))
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(max = 360.dp)
                  .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                actions.chunked(4).forEach { rowItems ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    rowItems.forEach { item ->
                      ExternalActionGridItem(
                        item = item,
                        onClick = {
                          runCatching { context.startActivity(item.intent) }
                            .onFailure { context.showToast(R.string.toast_cant_open_app) }
                        },
                        modifier = Modifier.weight(1f)
                      )
                    }
                    repeat(4 - rowItems.size) {
                      Spacer(Modifier.weight(1f))
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

  val activeAlternatives = rememberActiveValue(alternatives)
  WindowBottomSheet(
    show = alternatives != null,
    title = stringResource(R.string.app_info_alternative_launch),
    onDismissRequest = { alternatives = null },
    onDismissFinished = { if (alternatives == null) activeAlternatives.value = null }
  ) {
    activeAlternatives.value?.let { items ->
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp)
          .sheetContentSafePadding()
          .sheetHeightTransition(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items.forEach { item ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(
              color = colorScheme.onSurface.copy(alpha = 0.04f)
            ),
            onClick = {
              alternatives = null
              runCatching {
                context.startActivity(
                  Intent().setClassName(packageName, item.className)
                )
              }.onFailure { context.showToast(R.string.toast_cant_open_app) }
            },
            showIndication = true,
            insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
          ) {
            Column(Modifier.fillMaxWidth()) {
              Text(
                text = item.label,
                style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp),
                color = MiuixTheme.colorScheme.onSurface
              )
              Text(
                text = item.className,
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PrimaryAction(
  icon: ImageVector,
  circleColor: Color,
  label: String,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .combinedClickable(onClick = onClick, onLongClick = onLongClick)
      .padding(horizontal = 4.dp, vertical = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .size(45.dp)
        .clip(CircleShape)
        .background(circleColor),
      contentAlignment = Alignment.Center
    ) {
      top.yukonga.miuix.kmp.basic.Icon(
        imageVector = icon,
        contentDescription = label,
        tint = Color.White,
        modifier = Modifier.size(22.dp)
      )
    }
    Spacer(Modifier.height(6.dp))
    Text(
      text = label,
      style = MiuixTheme.textStyles.footnote1.copy(fontSize = 11.sp, lineHeight = 14.sp),
      color = MiuixTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun ExternalActionGridItem(
  item: AppInfoActionItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colorScheme = MiuixTheme.colorScheme
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 4.dp, vertical = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    val icon = item.icon
    if (icon != null) {
      val bitmap = remember(icon) { icon.toBitmap(128, 128).asImageBitmap() }
      androidx.compose.foundation.Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = Modifier.size(45.dp)
      )
    } else {
      Box(
        modifier = Modifier
          .size(45.dp)
          .clip(CircleShape)
          .background(colorScheme.surfaceContainer)
      )
    }
    Spacer(Modifier.height(6.dp))
    Text(
      text = item.label.toString(),
      style = MiuixTheme.textStyles.footnote1.copy(fontSize = 11.sp, lineHeight = 14.sp),
      color = colorScheme.onSurface,
      textAlign = TextAlign.Center,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis
    )
  }
}
