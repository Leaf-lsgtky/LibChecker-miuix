package com.absinthe.libchecker.domain.snapshot.comparison.ui

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.Composable
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonSide
import com.absinthe.libchecker.domain.snapshot.comparison.presentation.ComparisonShareIntentParser
import com.absinthe.libchecker.domain.snapshot.comparison.presentation.SnapshotComparisonViewModel
import com.absinthe.libchecker.domain.snapshot.comparison.ui.compose.ComparisonScreen
import com.absinthe.libchecker.ui.base.BaseComposeActivity
import com.absinthe.libchecker.utils.showToast
import org.koin.androidx.viewmodel.ext.android.viewModel

class ComparisonActivity : BaseComposeActivity() {

  private val viewModel: SnapshotComparisonViewModel by viewModel()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    parseSharedIntent(intent)
  }

  override fun onDestroy() {
    viewModel.clearSnapshotComparisonArchiveCache(externalCacheDir)
    super.onDestroy()
  }

  private fun parseSharedIntent(intent: Intent) {
    when (val result = ComparisonShareIntentParser.parse(intent)) {
      ComparisonShareIntentParser.Result.None -> Unit

      ComparisonShareIntentParser.Result.InvalidSharedItems -> {
        showToast(R.string.album_item_comparison_invalid_shared_items)
      }

      is ComparisonShareIntentParser.Result.PackagePair -> {
        result.leftUri?.let { viewModel.selectArchive(SnapshotComparisonSide.LEFT, it) }
        result.rightUri?.let { viewModel.selectArchive(SnapshotComparisonSide.RIGHT, it) }
        repeat(result.invalidItemCount) {
          showToast(R.string.album_item_comparison_invalid_shared_items)
        }
      }
    }
  }

  @Composable
  override fun Content() {
    ComparisonScreen(
      viewModel = viewModel,
      onBack = { onBackPressedDispatcher.onBackPressed() }
    )
  }
}
