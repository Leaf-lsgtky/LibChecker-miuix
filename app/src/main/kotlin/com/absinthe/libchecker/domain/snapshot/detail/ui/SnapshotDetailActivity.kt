package com.absinthe.libchecker.domain.snapshot.detail.ui

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.options.SnapshotOptions
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailDiffTextStyle
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailReportHeader
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailReportItemText
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailReportSectionText
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitleDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.repository.SnapshotDetailPayloadRepository
import com.absinthe.libchecker.domain.snapshot.detail.ui.compose.SnapshotDetailScreen
import com.absinthe.libchecker.domain.snapshot.detail.usecase.BuildSnapshotTitleDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.ui.base.BaseComposeActivity
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import com.absinthe.libchecker.utils.showToast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import top.yukonga.miuix.kmp.theme.MiuixTheme

const val EXTRA_ICON = "EXTRA_ICON"

/** Snapshot diff detail for a single app, rendered with Compose. */
class SnapshotDetailActivity : BaseComposeActivity() {

  private val viewModel: SnapshotViewModel by viewModel()
  private val buildSnapshotTitleDisplayData: BuildSnapshotTitleDisplayDataUseCase by inject()
  private val payloadRepository: SnapshotDetailPayloadRepository by inject()

  private var entity by mutableStateOf<SnapshotDiffItem?>(null)
  private var title by mutableStateOf<SnapshotTitleDisplayData?>(null)
  private var sections by mutableStateOf<List<SnapshotDetailSection>>(emptyList())
  private var failed by mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    lifecycleScope.launch {
      val loaded = try {
        intent.getStringExtra(EXTRA_PAYLOAD_ID)?.let { payloadRepository.load(it) }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Timber.e(e, "Failed to load snapshot detail")
        null
      }
      if (loaded == null) {
        failed = true
        showToast(R.string.toast_cant_open_app)
        finish()
        return@launch
      }
      entity = loaded
    }
  }

  private fun shareReport() {
    val currentTitle = title ?: return
    val text = buildString {
      append(buildSnapshotDetailReportHeader(currentTitle))
      sections.forEach { section ->
        append(buildSnapshotDetailReportSectionText(section.title))
        section.items.forEach { item ->
          append(buildSnapshotDetailReportItemText(item.item))
        }
      }
    }
    if (text.isBlank()) {
      showToast(R.string.snapshot_report_share_failed)
      return
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(
      Intent.createChooser(intent, getString(R.string.snapshot_generate_text_report))
    )
  }

  @Composable
  override fun Content() {
    val current = entity
    val colorScheme = MiuixTheme.colorScheme
    if (current == null) {
      Box(Modifier.fillMaxSize()) {
        LcLoadingState()
      }
      return
    }

    val primaryColor = colorScheme.primary.toArgb()
    val onSurfaceColor = colorScheme.onSurface.toArgb()
    LaunchedEffect(current, primaryColor, onSurfaceColor) {
      val options = viewModel.getSnapshotOptions()
      val diffTextStyle = SnapshotDetailDiffTextStyle(
        highlightColor = if (options and SnapshotOptions.DIFF_HIGHLIGHT > 0) primaryColor else null,
        emphasizeDiffs = options and SnapshotOptions.DIFF_EMPHASIS > 0,
        arrowColor = onSurfaceColor,
        metricDeltaColor = onSurfaceColor
      )
      title = buildSnapshotTitleDisplayData(
        BuildSnapshotTitleDisplayDataUseCase.Request(
          item = current,
          formatSplitPackageName = true,
          diffTextStyle = diffTextStyle
        )
      )
      sections = viewModel.buildDiffDetailContent(current, diffTextStyle)
    }

    val currentTitle = title
    if (currentTitle == null) {
      Box(Modifier.fillMaxSize()) {
        LcLoadingState()
      }
    } else {
      SnapshotDetailScreen(
        title = currentTitle,
        sections = sections,
        onBack = { onBackPressedDispatcher.onBackPressed() },
        onShare = ::shareReport
      )
    }
  }
}
