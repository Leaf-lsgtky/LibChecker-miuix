package com.absinthe.libchecker.domain.snapshot.album.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.domain.snapshot.album.ui.compose.AlbumScreen
import com.absinthe.libchecker.domain.snapshot.backup.ui.SnapshotBackupBottomSheetDialogFragment
import com.absinthe.libchecker.domain.snapshot.backup.ui.SnapshotRoomBackupOwner
import com.absinthe.libchecker.domain.snapshot.comparison.ui.ComparisonActivity
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackActivity
import com.absinthe.libchecker.ui.base.BaseComposeActivity
import com.absinthe.libchecker.utils.extensions.addBackStateHandler
import com.absinthe.libchecker.database.backup.RoomBackup
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class AlbumActivity : BaseComposeActivity(), SnapshotRoomBackupOwner {

  private val viewModel: SnapshotViewModel by viewModel()
  override val snapshotRoomBackup = RoomBackup(this)
  private var pendingRestoreUri: Uri? = null
  private val fragmentLifecycleCallbacks =
    object : FragmentManager.FragmentLifecycleCallbacks() {
      override fun onFragmentDetached(fragmentManager: FragmentManager, fragment: Fragment) {
        if (fragment is SnapshotBackupBottomSheetDialogFragment) {
          window.decorView.post(::dispatchPendingRestoreUri)
        }
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false)
    if (
      savedInstanceState == null &&
      intent?.action == Intent.ACTION_VIEW &&
      intent?.data != null
    ) {
      showBackupBottomSheet(intent.data)
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
    dispatchPendingRestoreUri()
  }

  override fun onResumeFragments() {
    super.onResumeFragments()
    dispatchPendingRestoreUri()
  }

  override fun onDestroy() {
    supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
    super.onDestroy()
  }

  private fun dispatchPendingRestoreUri() {
    val restoreUri = pendingRestoreUri ?: return
    if (supportFragmentManager.isStateSaved) {
      return
    }
    val tag = SnapshotBackupBottomSheetDialogFragment::class.java.name
    val currentSheet =
      supportFragmentManager.findFragmentByTag(tag) as? SnapshotBackupBottomSheetDialogFragment
    if (currentSheet?.isRemoving == true) {
      return
    }
    if (currentSheet != null) {
      currentSheet.restoreFromLaunchUri(restoreUri)
      pendingRestoreUri = null
    } else if (showBackupBottomSheet(restoreUri)) {
      pendingRestoreUri = null
    }
  }

  private fun showBackupBottomSheet(restoreUri: Uri? = null): Boolean {
    val tag = SnapshotBackupBottomSheetDialogFragment::class.java.name
    if (supportFragmentManager.findFragmentByTag(tag) != null) {
      return false
    }
    return runCatching {
      SnapshotBackupBottomSheetDialogFragment
        .newInstance(restoreUri)
        .showNow(supportFragmentManager, tag)
    }.onFailure(Timber::e).isSuccess
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
      onOpenBackup = { showBackupBottomSheet() }
    )
  }
}
