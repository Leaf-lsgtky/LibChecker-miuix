package com.absinthe.libchecker.domain.snapshot.album.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.absinthe.libchecker.database.backup.RoomBackup
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.domain.snapshot.album.ui.compose.AlbumScreen
import com.absinthe.libchecker.domain.snapshot.backup.presentation.SnapshotBackupViewModel
import com.absinthe.libchecker.domain.snapshot.backup.ui.SnapshotRoomBackupOwner
import com.absinthe.libchecker.domain.snapshot.backup.ui.compose.SnapshotBackupSheet
import com.absinthe.libchecker.domain.snapshot.comparison.ui.ComparisonActivity
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackActivity
import com.absinthe.libchecker.ui.base.BaseComposeActivity
import com.absinthe.libchecker.utils.extensions.addBackStateHandler
import org.koin.androidx.viewmodel.ext.android.viewModel

class AlbumActivity : BaseComposeActivity(), SnapshotRoomBackupOwner {

  private val viewModel: SnapshotViewModel by viewModel()
  private val backupViewModel: SnapshotBackupViewModel by viewModel()
  override val snapshotRoomBackup = RoomBackup(this)

  private var showBackupSheet by mutableStateOf(false)
  private var pendingRestoreUri by mutableStateOf<Uri?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (
      savedInstanceState == null &&
      intent?.action == Intent.ACTION_VIEW &&
      intent?.data != null
    ) {
      pendingRestoreUri = intent.data
      showBackupSheet = true
    }
    onBackPressedDispatcher.addBackStateHandler(
      lifecycleOwner = this,
      enabledState = { intent?.action == Intent.ACTION_VIEW && intent?.data != null },
      handler = {
        startActivity(
          Intent(this, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        finish()
      }
    )
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (intent.action != Intent.ACTION_VIEW) {
      return
    }
    val restoreUri = intent.data ?: return
    pendingRestoreUri = restoreUri
    showBackupSheet = true
  }

  @Composable
  override fun Content() {
    AlbumScreen(
      viewModel = viewModel,
      onBack = { onBackPressedDispatcher.onBackPressed() },
      onOpenComparison = {
        startActivity(Intent(this, ComparisonActivity::class.java))
      },
      onOpenTrack = {
        startActivity(Intent(this, TrackActivity::class.java))
      },
      onOpenBackup = { showBackupSheet = true }
    )

    SnapshotBackupSheet(
      show = showBackupSheet,
      viewModel = backupViewModel,
      roomBackup = snapshotRoomBackup,
      initialRestoreUri = pendingRestoreUri,
      onDismiss = {
        showBackupSheet = false
        pendingRestoreUri = null
      }
    )
  }
}
