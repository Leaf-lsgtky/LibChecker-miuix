package com.absinthe.libchecker.domain.snapshot.backup.ui.compose

import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.backup.RoomBackup
import com.absinthe.libchecker.domain.snapshot.backup.presentation.SnapshotBackupViewModel
import com.absinthe.libchecker.domain.snapshot.backup.presentation.SnapshotBackupViewModel.LocalBackupAction
import com.absinthe.libchecker.domain.snapshot.backup.presentation.SnapshotBackupViewModel.RestoreBackupResult
import com.absinthe.libchecker.domain.snapshot.backup.usecase.SnapshotArchiveBackupResult
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import com.absinthe.libchecker.utils.extensions.requireAvailableCacheDir
import com.absinthe.libchecker.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog

/** Snapshot backup and restore sheet. */
@Composable
fun SnapshotBackupSheet(
  show: Boolean,
  viewModel: SnapshotBackupViewModel,
  roomBackup: RoomBackup?,
  initialRestoreUri: Uri? = null,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var busy by remember { mutableStateOf(false) }
  var message by remember { mutableStateOf<String?>(null) }
  var pendingFileName by remember { mutableStateOf<String?>(null) }

  val createLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/octet-stream")
  ) { uri: Uri? ->
    if (uri == null) return@rememberLauncherForActivityResult
    busy = true
    viewModel.backup(uri) { result ->
      busy = false
      when (result) {
        is SnapshotArchiveBackupResult.Success ->
          message = context.getString(R.string.album_backup_detail, result.itemCount)

        SnapshotArchiveBackupResult.Empty ->
          context.showToast(R.string.snapshot_no_snapshot)

        SnapshotArchiveBackupResult.Failed ->
          context.showToast(R.string.toast_cant_open_app)
      }
    }
  }

  fun restore(uri: Uri) {
    busy = true
    val backup = roomBackup ?: run {
      context.showToast(R.string.toast_cant_open_app)
      return
    }
    viewModel.restoreBackup(
      roomBackup = backup,
      uri = uri,
      cacheDir = context.requireAvailableCacheDir()
    ) { result ->
      busy = false
      when (result) {
        is RestoreBackupResult.DatabaseBackup -> {
          if (!result.success) {
            context.showToast(R.string.toast_cant_open_app)
          }
        }

        is RestoreBackupResult.ArchiveBackup -> {
          val summary = result.summary
          if (summary == null) {
            context.showToast(R.string.toast_cant_open_app)
          } else {
            val total = summary.items.sumOf { it.count }
            message = context.getString(R.string.album_restore) + ": " + total
          }
        }
      }
    }
  }

  val restoreLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      restore(uri)
    }
  }

  fun requestBackup() {
    scope.launch {
      val action = withContext(Dispatchers.IO) {
        viewModel.onLocalBackupRequested(
          Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        )
      }
      when (action) {
        is LocalBackupAction.CreateArchive -> {
          pendingFileName = action.fileName
          createLauncher.launch(action.fileName)
        }

        LocalBackupAction.CreateDatabase -> {
          val backup = roomBackup
          if (backup == null) {
            context.showToast(R.string.toast_cant_open_app)
            return@launch
          }
          busy = true
          viewModel.createDatabaseBackup(backup) { result ->
            busy = false
            if (result.success) {
              message = context.getString(R.string.album_backup_database_detail)
            }
          }.onFailure {
            busy = false
            Timber.w(it)
          }
        }

        LocalBackupAction.StorageUnavailable ->
          context.showToast(R.string.toast_not_enough_storage_space)

        LocalBackupAction.NoSnapshot ->
          context.showToast(R.string.snapshot_no_snapshot)
      }
    }
  }

  LaunchedEffect(show, initialRestoreUri) {
    if (show && initialRestoreUri != null) {
      restore(initialRestoreUri)
    }
  }

  WindowBottomSheet(
    show = show,
    title = stringResource(R.string.album_item_backup_restore_title),
    onDismissRequest = onDismiss
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp)
    ) {
      if (busy) {
        LcLoadingState(
          modifier = Modifier.height(120.dp),
          message = stringResource(R.string.loading)
        )
      } else {
        ActionRow(
          title = stringResource(R.string.album_backup_summary),
          summary = stringResource(R.string.album_backup_sheet_backup_description),
          onClick = ::requestBackup
        )
        Spacer(Modifier.height(8.dp))
        ActionRow(
          title = stringResource(R.string.album_restore_from_file),
          summary = stringResource(R.string.album_restore_auto_detect_summary),
          onClick = { restoreLauncher.launch(arrayOf("*/*")) }
        )
      }
    }
  }

  message?.let { text ->
    WindowDialog(
      show = true,
      title = stringResource(R.string.album_item_backup_restore_title),
      summary = text,
      onDismissRequest = { message = null }
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.End
      ) {
        TextButton(
          text = stringResource(android.R.string.ok),
          onClick = { message = null }
        )
      }
    }
  }
}

@Composable
private fun ActionRow(
  title: String,
  summary: String,
  onClick: () -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .squircleBackground(colorScheme.surfaceContainerHigh, 16.dp)
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp)
  ) {
    Text(
      text = title,
      fontSize = 14.sp,
      fontWeight = FontWeight(550),
      color = colorScheme.onSurface
    )
    Spacer(Modifier.height(2.dp))
    Text(
      text = summary,
      fontSize = 12.sp,
      color = colorScheme.onSurfaceVariantSummary
    )
  }
}
