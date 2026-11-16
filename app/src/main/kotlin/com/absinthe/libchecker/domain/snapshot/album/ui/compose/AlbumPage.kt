@file:Suppress("ktlint:standard:function-naming")

package com.absinthe.libchecker.domain.snapshot.album.ui.compose

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.absinthe.libchecker.domain.snapshot.backup.ui.SnapshotRoomBackupOwner
import com.absinthe.libchecker.domain.snapshot.backup.ui.compose.SnapshotBackupSheet
import com.absinthe.libchecker.domain.snapshot.comparison.ui.ComparisonActivity
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackActivity
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AlbumPage(intent: Intent, onBack: () -> Unit) {
  val activity = LocalContext.current as FragmentActivity
  val initialRestoreUri = remember(intent) { intent.data.takeIf { intent.action == Intent.ACTION_VIEW } }
  var pendingRestoreUri by rememberSaveable(intent) { mutableStateOf(initialRestoreUri) }
  var showBackupSheet by rememberSaveable(intent) { mutableStateOf(initialRestoreUri != null) }
  val roomBackup = (activity as SnapshotRoomBackupOwner).snapshotRoomBackup
  AlbumScreen(
    viewModel = koinViewModel(),
    onBack = onBack,
    onOpenComparison = { activity.startActivity(Intent(activity, ComparisonActivity::class.java)) },
    onOpenTrack = { activity.startActivity(Intent(activity, TrackActivity::class.java)) },
    onOpenBackup = { showBackupSheet = true }
  )
  SnapshotBackupSheet(
    show = showBackupSheet,
    viewModel = koinViewModel(),
    roomBackup = roomBackup,
    initialRestoreUri = pendingRestoreUri,
    onDismiss = {
      showBackupSheet = false
      pendingRestoreUri = null
    }
  )
}
