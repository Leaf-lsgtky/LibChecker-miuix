@file:Suppress("ktlint:standard:function-naming")

package com.absinthe.libchecker.domain.snapshot.detail.ui.compose

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
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
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitleDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailReportHeader
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailReportItemText
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailReportSectionText
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
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
  var legacyDetail by remember { mutableStateOf(snapshotSettings.legacyDetail) }
  var entity by remember(intent) { mutableStateOf<SnapshotDiffItem?>(null) }
  var iconSource by remember { mutableStateOf<SnapshotPackageIconSource?>(null) }
  val fallbackIcon = remember(intent) { IntentCompat.getParcelableExtra(intent, EXTRA_ICON, Bitmap::class.java) }
  var title by remember { mutableStateOf<SnapshotTitleDisplayData?>(null) }
  var sections by remember { mutableStateOf<List<SnapshotDetailSection>>(emptyList()) }
  LifecycleResumeEffect(snapshotSettings) {
    legacyDetail = snapshotSettings.legacyDetail
    onPauseOrDispose { }
  }
  LaunchedEffect(intent) {
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
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(
      Intent.createChooser(intent, context.getString(R.string.snapshot_generate_text_report))
    )
  }

  val current = entity
  val colorScheme = MiuixTheme.colorScheme
  val primaryColor = colorScheme.primary.toArgb()
  val onSurfaceColor = colorScheme.onSurface.toArgb()
  if (current != null) {
    LaunchedEffect(current, primaryColor, onSurfaceColor) {
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
