package com.absinthe.libchecker.domain.settings.ui.compose

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.rules.CloudRulesVersionInfo
import com.absinthe.libchecker.domain.settings.model.CloudRulesDialogState
import com.absinthe.libchecker.domain.settings.model.toCloudRulesDialogState
import com.absinthe.libchecker.domain.settings.presentation.SettingsViewModel
import com.absinthe.libchecker.ui.compose.component.CloudRulesSheetSkeleton
import com.absinthe.libchecker.ui.compose.theme.lcCondensedFontFamily
import com.absinthe.libchecker.ui.compose.theme.withCustomFont
import com.absinthe.libchecker.ui.compose.util.sheetContentSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetHeightTransition
import com.absinthe.libchecker.utils.DownloadUtils
import com.absinthe.libchecker.utils.showToast
import com.jakewharton.processphoenix.ProcessPhoenix
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/** Cloud rules version check and update. */
@Composable
fun CloudRulesSheet(
  show: Boolean,
  viewModel: SettingsViewModel,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var state by remember { mutableStateOf<CloudRulesDialogState>(CloudRulesDialogState.Loading) }
  var versionInfo by remember { mutableStateOf<CloudRulesVersionInfo?>(null) }
  var downloading by remember { mutableStateOf(false) }
  var reloadToken by remember { mutableStateOf(0) }

  LaunchedEffect(show, reloadToken) {
    if (!show) return@LaunchedEffect
    state = CloudRulesDialogState.Loading
    versionInfo = null
    val localVersion = try {
      viewModel.getCloudRulesLocalVersion()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Timber.w(e)
      null
    }
    state = try {
      viewModel.getCloudRulesVersionInfo().also { versionInfo = it }
        ?.toCloudRulesDialogState() ?: CloudRulesDialogState.Error(localVersion)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Timber.w(e)
      CloudRulesDialogState.Error(localVersion)
    }
  }

  fun requestUpdate() {
    if (downloading) return
    val remoteVersion = versionInfo?.remoteVersion
    if (remoteVersion == null) {
      context.showToast(R.string.toast_cloud_rules_update_error)
      return
    }
    downloading = true
    val request = viewModel.getCloudRulesDownloadRequest()
    DownloadUtils.download(
      request.url,
      request.destination,
      object : DownloadUtils.OnDownloadListener {
        override fun onDownloadSuccess() {
          if (viewModel.installDownloadedCloudRules(request, remoteVersion)) {
            state = CloudRulesDialogState.Content(
              localVersion = remoteVersion,
              remoteVersion = remoteVersion,
              updateAvailable = false
            )
            runCatching {
              val intent = SystemServices.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?.apply { putExtra(Constants.PP_FROM_CLOUD_RULES_UPDATE, true) }
                ?: return@runCatching
              ProcessPhoenix.triggerRebirth(context, intent)
            }
          } else {
            downloading = false
            context.showToast(R.string.toast_cloud_rules_update_error)
          }
        }

        override fun onDownloadFailed() {
          downloading = false
          context.showToast(R.string.toast_cloud_rules_update_error)
        }
      }
    )
  }

  WindowBottomSheet(
    show = show,
    title = stringResource(R.string.cloud_rules),
    onDismissRequest = onDismiss
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp)
        .sheetContentSafePadding()
        .sheetHeightTransition(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      val currentState = state
      var lastContent by remember {
        mutableStateOf<CloudRulesDialogState.Content?>(null)
      }
      if (currentState is CloudRulesDialogState.Content) {
        lastContent = currentState
      }
      val mode = when (currentState) {
        CloudRulesDialogState.Loading -> CloudRulesSheetMode.Loading
        is CloudRulesDialogState.Error -> CloudRulesSheetMode.Error
        is CloudRulesDialogState.Content -> CloudRulesSheetMode.Content
      }
      Crossfade(
        targetState = mode,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "CloudRulesSheetTransition"
      ) { stateMode ->
        when (stateMode) {
          CloudRulesSheetMode.Loading -> CloudRulesSheetSkeleton()

          CloudRulesSheetMode.Error -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = stringResource(R.string.toast_cloud_rules_update_error),
              style = MiuixTheme.textStyles.body2,
              color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Spacer(Modifier.height(12.dp))
            TextButton(
              text = stringResource(R.string.rules_btn_update),
              onClick = { reloadToken++ },
              modifier = Modifier.fillMaxWidth(),
              colors = ButtonDefaults.textButtonColorsPrimary()
            )
          }

          CloudRulesSheetMode.Content -> {
            val current = lastContent ?: return@Crossfade
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 12.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                VersionColumn(
                  value = current.localVersion.toString(),
                  label = stringResource(R.string.rules_local_repo_version),
                  modifier = Modifier.weight(1f)
                )
                Icon(
                  painter = painterResource(R.drawable.ic_arrow_right),
                  contentDescription = null,
                  tint = MiuixTheme.colorScheme.onSurface,
                  modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(36.dp)
                )
                VersionColumn(
                  value = current.remoteVersion.toString(),
                  label = stringResource(R.string.rules_remote_repo_version),
                  modifier = Modifier.weight(1f)
                )
              }
              TextButton(
                text = stringResource(
                  if (current.updateAvailable) {
                    R.string.rules_btn_restart_to_update
                  } else {
                    R.string.rules_btn_update
                  }
                ),
                onClick = { requestUpdate() },
                enabled = current.updateAvailable && !downloading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary()
              )
            }
          }
        }
      }
    }
  }
}

private enum class CloudRulesSheetMode {
  Loading,
  Error,
  Content
}

@Composable
private fun VersionColumn(
  value: String,
  label: String,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = value,
      style = MiuixTheme.textStyles.headline1.copy(
        fontSize = 36.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Normal
      ).withCustomFont(lcCondensedFontFamily()),
      color = MiuixTheme.colorScheme.onSurface,
      maxLines = 1
    )
    Spacer(Modifier.height(6.dp))
    Text(
      text = label,
      style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
      color = MiuixTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center
    )
  }
}
