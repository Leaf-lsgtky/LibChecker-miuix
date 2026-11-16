@file:Suppress("ktlint:standard:function-naming")

package com.absinthe.libchecker.domain.snapshot.comparison.ui.compose

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonSide
import com.absinthe.libchecker.domain.snapshot.comparison.presentation.ComparisonShareIntentParser
import com.absinthe.libchecker.domain.snapshot.comparison.presentation.SnapshotComparisonViewModel
import com.absinthe.libchecker.utils.showToast
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ComparisonPage(intent: Intent, onBack: () -> Unit) {
  val context = LocalContext.current
  val viewModel: SnapshotComparisonViewModel = koinViewModel()
  LaunchedEffect(intent) {
    if (viewModel.sharedIntentHandled) return@LaunchedEffect
    viewModel.sharedIntentHandled = true
    when (val result = ComparisonShareIntentParser.parse(intent)) {
      ComparisonShareIntentParser.Result.None -> Unit
      ComparisonShareIntentParser.Result.InvalidSharedItems -> context.showToast(R.string.album_item_comparison_invalid_shared_items)
      is ComparisonShareIntentParser.Result.PackagePair -> {
        result.leftUri?.let { viewModel.selectArchive(SnapshotComparisonSide.LEFT, it) }
        result.rightUri?.let { viewModel.selectArchive(SnapshotComparisonSide.RIGHT, it) }
        repeat(result.invalidItemCount) { context.showToast(R.string.album_item_comparison_invalid_shared_items) }
      }
    }
  }
  ComparisonScreen(viewModel = viewModel, onBack = onBack)
}
