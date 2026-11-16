@file:Suppress("ktlint:standard:function-naming")

package com.absinthe.libchecker.domain.snapshot.album.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.domain.snapshot.album.ui.compose.AlbumPage
import com.absinthe.libchecker.ui.base.BaseComposeActivity

class AlbumActivity : BaseComposeActivity() {
  @Composable
  override fun Content() {
    val fromImport = navigationIntent.action == Intent.ACTION_VIEW && navigationIntent.data != null
    val onBack: () -> Unit = {
      if (fromImport) {
        startActivity(
          Intent(this, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        finish()
      } else {
        onBackPressedDispatcher.onBackPressed()
      }
    }
    NavigationBackHandler(
      state = rememberNavigationEventState(NavigationEventInfo.None),
      isBackEnabled = fromImport,
      onBackCompleted = onBack
    )
    AlbumPage(intent = navigationIntent, onBack = onBack)
  }
}
