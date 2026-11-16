package com.absinthe.libchecker.domain.settings.ui.compose

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.domain.settings.model.ExportAppsDialogState
import com.absinthe.libchecker.domain.settings.presentation.SettingsViewModel
import com.absinthe.libchecker.ui.compose.util.sheetContentSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetHeightTransition
import com.absinthe.libchecker.utils.extensions.openUrlInBrowser
import com.absinthe.libchecker.utils.showToast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

private const val MIME_TYPE_LCAPPS = "application/octet-stream"

/** Export installed apps to a WebUI readable archive. */
@Composable
fun ExportAppsSheet(
  show: Boolean,
  viewModel: SettingsViewModel,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var state by remember { mutableStateOf<ExportAppsDialogState>(ExportAppsDialogState.Ready) }

  fun startExport(uri: Uri) {
    scope.launch {
      state = ExportAppsDialogState.Exporting(progress = 0)
      try {
        val result = viewModel.exportInstalledApps(uri) { progress ->
          state = ExportAppsDialogState.Exporting(progress)
        }
        state = ExportAppsDialogState.Done
        context.showToast(
          context.getString(R.string.export_apps_success, result.appCount)
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Throwable) {
        Timber.e(e)
        state = ExportAppsDialogState.Ready
        context.showToast(
          context.getString(R.string.export_apps_failed, e.localizedMessage ?: e.toString())
        )
      }
    }
  }

  val createDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument(MIME_TYPE_LCAPPS)
  ) { uri: Uri? ->
    if (uri == null) {
      state = ExportAppsDialogState.Ready
    } else {
      startExport(uri)
    }
  }

  WindowBottomSheet(
    show = show,
    title = stringResource(R.string.export_apps),
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
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
          color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.04f)
        ),
        insideMargin = PaddingValues(16.dp)
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = stringResource(R.string.export_apps_webui_tip),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
          )
          Spacer(Modifier.height(6.dp))
          TextButton(
            text = "WebUI",
            onClick = { context.openUrlInBrowser(URLManager.WEBUI_PAGE) }
          )
          Spacer(Modifier.height(12.dp))
          AsyncImage(
            model = R.drawable.ic_webui_skeleton_preview,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .fillMaxWidth()
              .height(170.dp)
          )
        }
      }
      val current = state
      if (current is ExportAppsDialogState.Exporting || current == ExportAppsDialogState.Done) {
        Spacer(Modifier.height(16.dp))
        val progress = when (current) {
          is ExportAppsDialogState.Exporting -> current.progress.coerceIn(0, 100)
          else -> 100
        }
        LinearProgressIndicator(
          progress = progress / 100f,
          modifier = Modifier.fillMaxWidth()
        )
      }
      Spacer(Modifier.height(16.dp))
      TextButton(
        text = if (current == ExportAppsDialogState.Done) {
          stringResource(android.R.string.ok)
        } else {
          stringResource(R.string.export_apps_start)
        },
        onClick = {
          when (current) {
            ExportAppsDialogState.Ready -> {
              state = ExportAppsDialogState.Preparing
              createDocumentLauncher.launch(viewModel.buildInstalledAppsExportFileName())
            }

            ExportAppsDialogState.Done -> onDismiss()

            else -> Unit
          }
        },
        enabled = current == ExportAppsDialogState.Ready || current == ExportAppsDialogState.Done,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.textButtonColorsPrimary()
      )
    }
  }
}
