@file:Suppress("ktlint:standard:function-naming")

package com.absinthe.libchecker.domain.snapshot.detail.ui.compose

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.options.SnapshotOptions
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.snapshot.SnapshotSettingsRepository
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailDiffTextStyle
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotReportExportTarget
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitleDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailReportHeader
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailReportItemText
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailReportSectionText
import com.absinthe.libchecker.domain.snapshot.detail.model.chooseSnapshotReportExportTarget
import com.absinthe.libchecker.domain.snapshot.detail.repository.SnapshotDetailPayloadRepository
import com.absinthe.libchecker.domain.snapshot.detail.ui.EXTRA_ICON
import com.absinthe.libchecker.domain.snapshot.detail.ui.EXTRA_PAYLOAD_ID
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.SnapshotDetailRow
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.interactionPolicy
import com.absinthe.libchecker.domain.snapshot.detail.usecase.BuildSnapshotTitleDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.ui.compose.component.SnapshotDetailSkeleton
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.utils.extensions.launchLibReferencePage
import com.absinthe.libchecker.utils.showToast
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import rikka.core.util.ClipboardUtils
import timber.log.Timber
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SnapshotDetailPage(intent: Intent, onBack: () -> Unit) {
  val context = LocalContext.current as FragmentActivity
  val scope = rememberCoroutineScope()
  val currentOnBack by rememberUpdatedState(onBack)
  val viewModel: SnapshotViewModel = koinViewModel()
  val detailViewModel: DetailViewModel = koinViewModel()
  val buildSnapshotTitleDisplayData: BuildSnapshotTitleDisplayDataUseCase = koinInject()
  val payloadRepository: SnapshotDetailPayloadRepository = koinInject()
  val snapshotSettings: SnapshotSettingsRepository = koinInject()
  // The page drops out of the composition while two opaque pages cover its entry; restore the
  // payload it already built instead of reloading it and flashing the skeleton again.
  val retainedState = remember(viewModel) { viewModel.detailScreenState }
  var legacyDetail by remember { mutableStateOf(snapshotSettings.legacyDetail) }
  var entity by remember(intent) { mutableStateOf<SnapshotDiffItem?>(retainedState.payload) }
  var iconSource by remember { mutableStateOf<SnapshotPackageIconSource?>(retainedState.iconSource) }
  val fallbackIcon = remember(intent) { IntentCompat.getParcelableExtra(intent, EXTRA_ICON, Bitmap::class.java) }
  var title by remember { mutableStateOf<SnapshotTitleDisplayData?>(retainedState.title) }
  var sections by remember { mutableStateOf<List<SnapshotDetailSection>>(retainedState.sections) }
  SideEffect {
    retainedState.payload = entity
    retainedState.title = title
    retainedState.sections = sections
    retainedState.iconSource = iconSource
  }
  LifecycleResumeEffect(snapshotSettings) {
    legacyDetail = snapshotSettings.legacyDetail
    onPauseOrDispose { }
  }
  LaunchedEffect(intent) {
    if (entity != null) return@LaunchedEffect
    val loaded = try {
      intent.getStringExtra(EXTRA_PAYLOAD_ID)?.let { payloadRepository.load(it) }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Timber.e(e, "Failed to load snapshot detail")
      null
    }
    if (loaded == null) {
      context.showToast(R.string.toast_cant_open_app)
      currentOnBack()
    } else {
      entity = loaded
    }
  }

  fun shareReport() {
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
      context.showToast(R.string.snapshot_report_share_failed)
      return
    }
    // Short reports go straight to the clipboard; long ones are shared as a text file so the
    // receiving app never truncates them.
    when (chooseSnapshotReportExportTarget(text)) {
      SnapshotReportExportTarget.CLIPBOARD -> {
        ClipboardUtils.put(context, text)
        // Always confirm the copy here: the exported report is the action result and Android 13+
        // suppresses the compat toast when the system clipboard overlay shows instead.
        context.showToast(R.string.toast_copied_to_clipboard)
      }

      SnapshotReportExportTarget.TEXT_FILE -> scope.launch { context.shareSnapshotReportFile(text) }
    }
  }

  val current = entity
  val colorScheme = MiuixTheme.colorScheme
  val primaryColor = colorScheme.primary.toArgb()
  val onSurfaceColor = colorScheme.onSurface.toArgb()
  if (current != null) {
    LaunchedEffect(current, primaryColor, onSurfaceColor) {
      val colorKey = primaryColor xor onSurfaceColor
      if (retainedState.payload === current && retainedState.sections.isNotEmpty() && retainedState.colorKey == colorKey) {
        return@LaunchedEffect
      }
      val options = viewModel.getSnapshotOptions()
      val diffTextStyle = SnapshotDetailDiffTextStyle(
        highlightColor = if (options and SnapshotOptions.DIFF_HIGHLIGHT > 0) primaryColor else null,
        emphasizeDiffs = options and SnapshotOptions.DIFF_EMPHASIS > 0,
        arrowColor = onSurfaceColor,
        metricDeltaColor = onSurfaceColor
      )
      val computedTitle = buildSnapshotTitleDisplayData(
        BuildSnapshotTitleDisplayDataUseCase.Request(
          item = current,
          formatSplitPackageName = true,
          diffTextStyle = diffTextStyle
        )
      )
      iconSource = viewModel.getSnapshotPackageIconSources(listOf(current.packageName))[current.packageName]
      sections = viewModel.buildDiffDetailContent(current, diffTextStyle)
      title = computedTitle
      retainedState.colorKey = colorKey
    }
  }

  val currentTitle = if (current != null) title else null
  Crossfade(
    targetState = currentTitle,
    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
    label = "SnapshotDetailLoadTransition"
  ) { resolvedTitle ->
    if (resolvedTitle == null) {
      SnapshotDetailSkeleton(onBack = onBack)
    } else if (current != null) {
      SnapshotDetailScreen(
        title = resolvedTitle,
        legacyDetail = legacyDetail,
        packageName = current.packageName,
        sections = sections,
        detailViewModel = detailViewModel,
        onBack = onBack,
        onShare = ::shareReport,
        onTitleClick = if (current.deleted) {
          null
        } else {
          {
            scope.launch {
              val lcItem = viewModel.getAppListItem(current.packageName) ?: return@launch
              context.launchDetailPage(lcItem)
            }
          }
        },
        onItemClick = { displayData: SnapshotDetailItemDisplayData ->
          val row = SnapshotDetailRow.Item(displayData.item.itemType, displayData)
          if (row.interactionPolicy(current.packageName).opensDetail) {
            scope.launch {
              val lcItem = viewModel.getAppListItem(current.packageName) ?: return@launch
              context.launchDetailPage(
                item = lcItem,
                refName = displayData.item.name,
                refType = displayData.item.itemType
              )
            }
          }
        },
        onItemLongClick = { displayData: SnapshotDetailItemDisplayData ->
          val row = SnapshotDetailRow.Item(displayData.item.itemType, displayData)
          val policy = row.interactionPolicy(current.packageName)
          if (policy.opensReference) {
            context.launchLibReferencePage(
              refName = displayData.item.name,
              refLabel = policy.referenceLabel,
              refType = displayData.item.itemType,
              refList = null
            )
          }
        },
        iconSource = iconSource,
        fallbackIcon = fallbackIcon
      )
    }
  }
}

private const val SNAPSHOT_REPORT_CACHE_DIR = "shared_snapshot_reports"

/**
 * Writes the report into the shared cache directory and hands it to the chooser as a text file,
 * which is how long reports leave the app without hitting binder or intent size limits.
 */
private suspend fun Context.shareSnapshotReportFile(report: String) {
  val reportUri = withContext(Dispatchers.IO) {
    runCatching {
      val reportDir = File(cacheDir, SNAPSHOT_REPORT_CACHE_DIR).apply {
        check(exists() || mkdirs()) { "Failed to create snapshot report cache directory" }
      }
      val reportFile = File(reportDir, "LibChecker-snapshot-report-${System.currentTimeMillis()}.txt")
      reportFile.writeText(report, Charsets.UTF_8)
      FileProvider.getUriForFile(this@shareSnapshotReportFile, "$packageName.fileprovider", reportFile)
    }.onFailure {
      Timber.e(it, "Failed to create snapshot report file")
    }.getOrNull()
  }
  if (reportUri == null) {
    showToast(R.string.snapshot_report_share_failed)
    return
  }
  val shareIntent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_STREAM, reportUri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  runCatching {
    startActivity(Intent.createChooser(shareIntent, getString(R.string.app_info_share)))
  }.onFailure {
    Timber.e(it, "Failed to share snapshot report")
    showToast(R.string.snapshot_report_share_failed)
  }
}
