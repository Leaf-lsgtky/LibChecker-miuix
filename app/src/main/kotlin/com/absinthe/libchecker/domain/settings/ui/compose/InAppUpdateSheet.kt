package com.absinthe.libchecker.domain.settings.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.bean.GetAppUpdateInfo
import com.absinthe.libchecker.domain.app.update.AppSelfUpdatePolicy
import com.absinthe.libchecker.domain.app.update.AppUpdateChannel
import com.absinthe.libchecker.domain.app.update.AppUpdateInstallResult
import com.absinthe.libchecker.domain.app.update.BuildInAppUpdateDiffDataUseCase
import com.absinthe.libchecker.domain.settings.model.InAppUpdateDialogContent
import com.absinthe.libchecker.domain.settings.model.InAppUpdateDialogState
import com.absinthe.libchecker.domain.settings.model.selectChannel
import com.absinthe.libchecker.domain.settings.model.showContent
import com.absinthe.libchecker.domain.settings.model.showInstallProgress
import com.absinthe.libchecker.domain.settings.presentation.SettingsViewModel
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemCardPresentation
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotItemDisplayDataUseCase
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import com.absinthe.libchecker.utils.showToast
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/** In-app self update: channel switch, diff preview and install. */
@Composable
fun InAppUpdateSheet(
  show: Boolean,
  viewModel: SettingsViewModel,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val koin = remember { GlobalContext.get() }
  val buildDiffData = remember { koin.get<BuildInAppUpdateDiffDataUseCase>() }
  val buildDisplayData = remember { koin.get<BuildSnapshotItemDisplayDataUseCase>() }

  var state by remember {
    mutableStateOf(
      InAppUpdateDialogState(
        selectedChannel = AppSelfUpdatePolicy.defaultUpdateChannel(BuildConfig.IS_DEV_VERSION),
        content = InAppUpdateDialogContent.Loading(),
        isChannelSelectionEnabled = true,
        isUpdateEnabled = false
      )
    )
  }
  var updateInfo by remember { mutableStateOf<GetAppUpdateInfo?>(null) }

  fun renderContent() {
    val diffData = buildDiffData(updateInfo)
    state = state.showContent(
      item = diffData?.item?.let { item ->
        buildDisplayData(
          BuildSnapshotItemDisplayDataUseCase.Request(
            item = item,
            cardPresentation = SnapshotItemCardPresentation.Rounded,
            iconSource = null,
            showUpdateTime = false,
            isApexPackage = false,
            animateStateIndicator = false,
            tintChangedAbiBadge = false,
            highlightDiffColor = null,
            emphasizeDiffs = false,
            highlightText = ""
          )
        )
      },
      hasUpdate = diffData?.hasUpdate == true
    )
  }

  LaunchedEffect(show) {
    if (!show) return@LaunchedEffect
    launch {
      viewModel.respStateFlow.collect { result ->
        if (result.channel != state.selectedChannel) {
          return@collect
        }
        updateInfo = result.updateInfo
        renderContent()
      }
    }
    viewModel.requestUpdate(state.selectedChannel)
  }

  fun selectChannel(channel: AppUpdateChannel) {
    val next = state.selectChannel(channel)
    if (next === state) {
      return
    }
    state = next
    viewModel.requestUpdate(channel)
  }

  fun install() {
    val url = updateInfo?.appForFlavor(BuildConfig.IS_FOSS)?.link
    if (url == null) {
      state = state.showInstallProgress()
      viewModel.requestUpdate(state.selectedChannel)
      return
    }
    scope.launch {
      state = state.showInstallProgress()
      val result = viewModel.installUpdate(url)
      renderContent()
      when (result) {
        AppUpdateInstallResult.Started ->
          context.showToast(R.string.toast_app_update_started)

        AppUpdateInstallResult.Unsupported ->
          context.showToast(R.string.toast_app_update_unsupported)

        is AppUpdateInstallResult.Failure ->
          context.showToast(
            context.getString(
              R.string.toast_app_update_failed,
              result.message.orEmpty()
            )
          )
      }
    }
  }

  WindowBottomSheet(
    show = show,
    title = stringResource(R.string.settings_get_updates),
    onDismissRequest = onDismiss
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp)
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChannelChip(
          label = stringResource(R.string.settings_get_updates_in_app_chip_stable),
          selected = state.selectedChannel == AppUpdateChannel.STABLE,
          enabled = state.isChannelSelectionEnabled,
          onClick = { selectChannel(AppUpdateChannel.STABLE) }
        )
        ChannelChip(
          label = stringResource(R.string.settings_get_updates_in_app_chip_ci),
          selected = state.selectedChannel == AppUpdateChannel.CI,
          enabled = state.isChannelSelectionEnabled,
          onClick = { selectChannel(AppUpdateChannel.CI) }
        )
      }
      Spacer(Modifier.height(12.dp))
      when (val content = state.content) {
        is InAppUpdateDialogContent.Loading -> {
          Box(
            Modifier
              .fillMaxWidth()
              .height(160.dp)
          ) {
            LcLoadingState(message = stringResource(R.string.loading))
          }
        }

        is InAppUpdateDialogContent.Ready -> {
          val item = content.item
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .squircleBackground(MiuixTheme.colorScheme.surfaceContainerHigh, 16.dp)
              .padding(horizontal = 16.dp, vertical = 12.dp)
          ) {
            Text(
              text = item?.appName?.text?.toString()
                ?: stringResource(R.string.settings_get_updates),
              fontWeight = FontWeight(600),
              color = MiuixTheme.colorScheme.onSurface
            )
            if (item != null) {
              Spacer(Modifier.height(2.dp))
              Text(
                text = item.packageName,
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
              )
              Spacer(Modifier.height(2.dp))
              Text(
                text = item.versionInfo.toString(),
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
              )
            }
          }
        }
      }
      Spacer(Modifier.height(16.dp))
      TextButton(
        text = stringResource(R.string.rules_btn_update),
        onClick = { install() },
        enabled = state.isUpdateEnabled,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}

@Composable
private fun ChannelChip(
  label: String,
  selected: Boolean,
  enabled: Boolean,
  onClick: () -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  Box(
    modifier = Modifier
      .squircleBackground(
        when {
          selected -> colorScheme.onSurface
          enabled -> colorScheme.surfaceContainerHigh
          else -> colorScheme.surfaceContainer
        },
        14.dp
      )
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    Text(
      text = label,
      fontSize = 13.sp,
      fontWeight = FontWeight(550),
      color = when {
        selected -> colorScheme.surface
        enabled -> colorScheme.onSurface
        else -> colorScheme.onSurfaceVariantSummary
      }
    )
  }
}