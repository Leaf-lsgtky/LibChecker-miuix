package com.absinthe.libchecker.domain.settings.ui.compose

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.rules.CloudRulesVersionInfo
import com.absinthe.libchecker.domain.settings.model.CloudRulesDialogState
import com.absinthe.libchecker.domain.settings.model.toCloudRulesDialogState
import com.absinthe.libchecker.domain.settings.presentation.SettingsViewModel
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import com.absinthe.libchecker.utils.DownloadUtils
import com.absinthe.libchecker.utils.showToast
import com.jakewharton.processphoenix.ProcessPhoenix
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber
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
  val scope = rememberCoroutineScope()
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
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      when (val current = state) {
        CloudRulesDialogState.Loading -> {
          LcLoadingState(
            modifier = Modifier.height(140.dp),
            message = stringResource(R.string.loading)
          )
        }

        is CloudRulesDialogState.Error -> {
          Text(
            text = stringResource(R.string.toast_cloud_rules_update_error),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
          )
          Spacer(Modifier.height(12.dp))
          TextButton(
            text = stringResource(R.string.rules_btn_update),
            onClick = { reloadToken++ },
            modifier = Modifier.fillMaxWidth()
          )
        }

        is CloudRulesDialogState.Content -> {
          VersionRow(
            title = stringResource(R.string.rules_local_repo_version),
            value = current.localVersion.toString()
          )
          Spacer(Modifier.height(8.dp))
          VersionRow(
            title = stringResource(R.string.rules_remote_repo_version),
            value = current.remoteVersion.toString()
          )
          Spacer(Modifier.height(16.dp))
          TextButton(
            text = stringResource(
              if (current.updateAvailable) {
                R.string.rules_btn_update
              } else {
                R.string.rules_btn_restart_to_update
              }
            ),
            onClick = { requestUpdate() },
            enabled = !downloading,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
      Spacer(Modifier.height(8.dp))
    }
  }
}

@Composable
private fun VersionRow(title: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = title,
      fontSize = 13.sp,
      color = MiuixTheme.colorScheme.onSurfaceVariantSummary
    )
    Text(
      text = value,
      fontSize = 13.sp,
      fontWeight = FontWeight(550),
      color = MiuixTheme.colorScheme.onSurface
    )
  }
}
