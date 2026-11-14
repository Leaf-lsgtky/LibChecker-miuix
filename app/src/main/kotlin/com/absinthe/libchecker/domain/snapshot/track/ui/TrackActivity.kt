package com.absinthe.libchecker.domain.snapshot.track.ui

import androidx.compose.runtime.Composable
import com.absinthe.libchecker.domain.snapshot.track.presentation.TrackViewModel
import com.absinthe.libchecker.domain.snapshot.track.ui.compose.TrackScreen
import com.absinthe.libchecker.ui.base.BaseComposeActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class TrackActivity : BaseComposeActivity() {

  private val viewModel: TrackViewModel by viewModel()

  @Composable
  override fun Content() {
    TrackScreen(
      viewModel = viewModel,
      onBack = { onBackPressedDispatcher.onBackPressed() }
    )
  }
}
