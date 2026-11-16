package com.absinthe.libchecker.domain.snapshot.backup.ui.compose

import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.absinthe.libchecker.ui.compose.component.SheetCardListSkeleton
import com.absinthe.libchecker.ui.compose.util.rememberActiveValue
import com.absinthe.libchecker.ui.compose.util.sheetContentSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetHeightTransition
import com.absinthe.libchecker.utils.extensions.requireAvailableCacheDir
import com.absinthe.libchecker.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
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
        .padding(bottom = 16.dp)
        .sheetContentSafePadding()
        .sheetHeightTransition(),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Crossfade(
        targetState = busy,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "SnapshotBackupSheetTransition"
      ) { isBusy ->
        if (isBusy) {
          SheetCardListSkeleton(
            rowCount = 2,
            showIcon = true,
            twoLine = true
          )
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionRow(
              iconRes = R.drawable.ic_backup,
              title = stringResource(R.string.album_backup_summary),
              summary = stringResource(R.string.album_backup_sheet_backup_description),
              onClick = ::requestBackup
            )
            ActionRow(
              iconRes = R.drawable.ic_restore,
              title = stringResource(R.string.album_restore_from_file),
              summary = stringResource(R.string.album_restore_auto_detect_summary),
              onClick = { restoreLauncher.launch(arrayOf("*/*")) }
            )
          }
        }
      }
    }
  }

  val activeMessage = rememberActiveValue(message)
  WindowDialog(
    show = message != null,
    title = stringResource(R.string.album_item_backup_restore_title),
    summary = activeMessage.value ?: "",
    onDismissRequest = { message = null },
    onDismissFinished = { if (message == null) activeMessage.value = null }
  ) {
    TextButton(
      text = stringResource(android.R.string.ok),
      onClick = { message = null },
      modifier = Modifier.fillMaxWidth(),
      colors = ButtonDefaults.textButtonColorsPrimary()
    )
  }
}

@Composable
private fun ActionRow(
  @DrawableRes iconRes: Int,
  title: String,
  summary: String,
  onClick: () -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.defaultColors(
      color = colorScheme.onSurface.copy(alpha = 0.04f)
    ),
    onClick = onClick,
    showIndication = true,
    insideMargin = PaddingValues(16.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        tint = colorScheme.onSurface,
        modifier = Modifier.size(24.dp)
      )
      Column(Modifier.weight(1f)) {
        Text(
          text = title,
          style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp),
          color = colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = summary,
          style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
          color = colorScheme.onSurfaceVariantSummary
        )
      }
      Icon(
        imageVector = MiuixIcons.Basic.ArrowRight,
        contentDescription = null,
        tint = colorScheme.onSurfaceVariantActions,
        modifier = Modifier.size(width = 10.dp, height = 16.dp)
      )
    }
  }
}
