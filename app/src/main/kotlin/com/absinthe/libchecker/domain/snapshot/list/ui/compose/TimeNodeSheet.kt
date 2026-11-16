package com.absinthe.libchecker.domain.snapshot.list.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.app.list.ui.compose.AppIcon
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.domain.snapshot.timenode.model.DayContribution
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotContributionData
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotPalette
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotTimeNodeItem
import com.absinthe.libchecker.domain.snapshot.timenode.usecase.ComputeSnapshotContributionsUseCase
import com.absinthe.libchecker.ui.compose.component.SheetCardListSkeleton
import com.absinthe.libchecker.ui.compose.theme.lcCondensedFontFamily
import com.absinthe.libchecker.ui.compose.theme.withCustomFont
import com.absinthe.libchecker.ui.compose.util.rememberActiveValue
import com.absinthe.libchecker.ui.compose.util.sheetBottomSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetContentSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetHeightTransition
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.Ok
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog

/** Time node sheet: contribution wall heatmap, snapshot timestamp switching and auto-remove toggle. */
@Composable
fun TimeNodeSheet(
  show: Boolean,
  viewModel: SnapshotViewModel,
  timeStamps: List<TimeStampItem>,
  currentTimestamp: Long?,
  onSelect: (Long) -> Unit,
  onDismiss: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val colorScheme = MiuixTheme.colorScheme
  var items by remember(timeStamps, currentTimestamp) {
    mutableStateOf<List<SnapshotTimeNodeItem>>(emptyList())
  }
  var iconSources by remember(timeStamps, currentTimestamp) {
    mutableStateOf<Map<String, SnapshotPackageIconSource>>(emptyMap())
  }
  var contributionData by remember(show) {
    mutableStateOf<SnapshotContributionData?>(null)
  }
  var autoRemoveThreshold by remember(show) {
    mutableIntStateOf(viewModel.getSnapshotAutoRemoveThreshold())
  }
  var isLoading by remember(show, timeStamps, currentTimestamp) {
    mutableStateOf(show && timeStamps.isNotEmpty() && items.isEmpty())
  }

  suspend fun reloadSheetData(stamps: List<TimeStampItem>) {
    isLoading = stamps.isNotEmpty() && items.isEmpty()
    val (listData, contributions) = coroutineScope {
      val listDeferred = async {
        runCatching {
          viewModel.buildSnapshotTimeNodeListData(stamps)
        }.getOrNull()
      }
      val contribDeferred = async {
        runCatching {
          viewModel.computeSnapshotContributions(stamps)
        }.getOrNull()
      }
      listDeferred.await() to contribDeferred.await()
    }
    items = listData?.items.orEmpty()
    iconSources = listData?.packageIconSources.orEmpty()
    autoRemoveThreshold = viewModel.getSnapshotAutoRemoveThreshold()
    contributionData = contributions
    isLoading = false
  }

  LaunchedEffect(show, timeStamps, currentTimestamp) {
    if (!show) return@LaunchedEffect
    reloadSheetData(timeStamps)
  }

  SnapshotTimeNodeSheet(
    show = show,
    title = stringResource(R.string.snapshot_time_node_title),
    items = items,
    iconSources = iconSources,
    contributionData = contributionData,
    currentTimestamp = currentTimestamp,
    onSelect = onSelect,
    onDismiss = onDismiss,
    isLoading = isLoading,
    topCard = {
      SnapshotAutoRemoveCard(
        autoRemoveThreshold = autoRemoveThreshold,
        onDisable = {
          viewModel.disableSnapshotAutoRemoveThreshold()
          autoRemoveThreshold = 0
        },
        onConfirmThreshold = { threshold ->
          autoRemoveThreshold = threshold
          scope.launch {
            val remaining = withContext(Dispatchers.IO) {
              runCatching {
                viewModel.enableSnapshotAutoRemoveAndRetainLatest(threshold)
              }.getOrNull()
            }
            if (remaining != null) {
              reloadSheetData(remaining)
            }
          }
        }
      )
    }
  )
}

@Composable
internal fun SnapshotAutoRemoveCard(
  autoRemoveThreshold: Int,
  onDisable: () -> Unit,
  onConfirmThreshold: (Int) -> Unit
) {
  var showThresholdDialog by remember { mutableStateOf(false) }
  var sliderThreshold by remember {
    mutableIntStateOf(
      autoRemoveThreshold.takeIf { it in AUTO_REMOVE_MIN..AUTO_REMOVE_MAX }
        ?: DEFAULT_AUTO_REMOVE_THRESHOLD
    )
  }
  val isEnabled = autoRemoveThreshold > 0

  fun openThresholdDialog() {
    sliderThreshold = autoRemoveThreshold.takeIf { it in AUTO_REMOVE_MIN..AUTO_REMOVE_MAX }
      ?: DEFAULT_AUTO_REMOVE_THRESHOLD
    showThresholdDialog = true
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.defaultColors(
      color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.04f)
    ),
    onClick = ::openThresholdDialog,
    showIndication = true,
    insideMargin = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(Modifier.weight(1f)) {
        Text(
          text = if (isEnabled) {
            stringResource(
              R.string.album_item_management_snapshot_auto_remove_specific_title,
              autoRemoveThreshold
            )
          } else {
            stringResource(R.string.album_item_management_snapshot_auto_remove_default_title)
          },
          style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp),
          color = MiuixTheme.colorScheme.onSurface
        )
        Text(
          text = if (isEnabled) {
            stringResource(
              R.string.snapshot_auto_remove_summary,
              autoRemoveThreshold.coerceAtLeast(1)
            )
          } else {
            stringResource(R.string.snapshot_auto_remove_title)
          },
          style = MiuixTheme.textStyles.footnote2.copy(fontSize = 11.sp),
          color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
      }
      Switch(
        checked = isEnabled,
        onCheckedChange = { enabled ->
          if (enabled) {
            openThresholdDialog()
          } else {
            onDisable()
          }
        }
      )
    }
  }

  WindowDialog(
    show = showThresholdDialog,
    title = stringResource(R.string.album_item_management_snapshot_auto_remove_default_title),
    onDismissRequest = { showThresholdDialog = false }
  ) {
    val okLabel = stringResource(android.R.string.ok)
    Column {
      Text(
        text = stringResource(
          R.string.album_item_management_snapshot_auto_remove_desc,
          okLabel
        ),
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
      )
      Spacer(Modifier.height(12.dp))
      Slider(
        value = sliderThreshold.toFloat(),
        onValueChange = {
          sliderThreshold = it.toInt().coerceIn(AUTO_REMOVE_MIN, AUTO_REMOVE_MAX)
        },
        valueRange = AUTO_REMOVE_MIN.toFloat()..AUTO_REMOVE_MAX.toFloat(),
        steps = AUTO_REMOVE_MAX - AUTO_REMOVE_MIN - 1
      )
      Text(
        text = sliderThreshold.toString(),
        style = MiuixTheme.textStyles.title3,
        color = MiuixTheme.colorScheme.onSurface,
        modifier = Modifier.align(Alignment.CenterHorizontally)
      )
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        TextButton(
          text = stringResource(android.R.string.cancel),
          onClick = { showThresholdDialog = false },
          modifier = Modifier.weight(1f)
        )
        TextButton(
          text = okLabel,
          onClick = {
            showThresholdDialog = false
            onConfirmThreshold(sliderThreshold)
          },
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.textButtonColorsPrimary()
        )
      }
    }
  }
}

@Composable
internal fun SnapshotTimeNodeSheet(
  show: Boolean,
  title: String,
  items: List<SnapshotTimeNodeItem>,
  iconSources: Map<String, SnapshotPackageIconSource>,
  contributionData: SnapshotContributionData?,
  currentTimestamp: Long?,
  onSelect: (Long) -> Unit,
  onDismiss: () -> Unit,
  topCard: (@Composable () -> Unit)? = null,
  isLoading: Boolean = false,
  showSelectedState: Boolean = true,
  showDaySnapshotAction: Boolean = true,
  trailingIcon: ImageVector? = null,
  trailingIconTint: Color = MiuixTheme.colorScheme.primary
) {
  val colorScheme = MiuixTheme.colorScheme
  var dayDetail by remember(show) {
    mutableStateOf<DayContribution?>(null)
  }

  WindowBottomSheet(
    show = show,
    title = title,
    onDismissRequest = onDismiss
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .sheetHeightTransition(),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      topCard?.invoke()
      contributionData?.let { data ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.04f)
          ),
          insideMargin = PaddingValues(12.dp)
        ) {
          ContributionGrid(
            data = data,
            onTileClick = { day -> dayDetail = day }
          )
        }
      }
      if (isLoading && items.isEmpty()) {
        SheetCardListSkeleton(
          rowCount = 3,
          showIcon = false,
          twoLine = true,
          modifier = Modifier.padding(bottom = sheetBottomSafePadding(16.dp))
        )
      } else if (items.isEmpty()) {
        Text(
          text = stringResource(R.string.empty_list),
          style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
          color = colorScheme.onSurfaceVariantSummary,
          modifier = Modifier.padding(bottom = sheetBottomSafePadding(16.dp))
        )
      } else {
        LazyColumn(
          modifier = Modifier.weight(1f, fill = false),
          contentPadding = PaddingValues(bottom = sheetBottomSafePadding(16.dp)),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(items, key = { it.timestamp }) { node ->
            val tagColorInt = contributionData?.snapshotColors?.get(node.timestamp) ?: node.tagColor
            val isSelected = showSelectedState &&
              (node.isCurrent || (currentTimestamp != null && node.timestamp == currentTimestamp))
            val cardBorder = if (isSelected) {
              BorderStroke(1.5.dp, colorScheme.primary)
            } else {
              null
            }
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .then(
                  if (cardBorder != null) {
                    Modifier.border(cardBorder, RoundedCornerShape(16.dp))
                  } else {
                    Modifier
                  }
                ),
              colors = CardDefaults.defaultColors(
                color = if (isSelected) {
                  colorScheme.primaryContainer.copy(alpha = 0.22f)
                } else {
                  colorScheme.onSurface.copy(alpha = 0.04f)
                }
              ),
              onClick = { onSelect(node.timestamp) },
              showIndication = true,
              insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier
                        .size(8.dp)
                        .background(
                          color = tagColorInt?.let(::Color) ?: colorScheme.primary,
                          shape = CircleShape
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                      text = node.timestampText,
                      style = MiuixTheme.textStyles.body1.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                      ).withCustomFont(lcCondensedFontFamily()),
                      color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                    )
                  }
                  if (node.description.isNotBlank() && node.description != node.timestampText) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                      text = node.description,
                      style = MiuixTheme.textStyles.footnote2.copy(fontSize = 11.sp),
                      color = colorScheme.onSurfaceVariantSummary
                    )
                  }
                  if (node.topAppPackageNames.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    val visiblePackages = node.topAppPackageNames.take(MAX_VISIBLE_APP_COUNT)
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      visiblePackages.forEach { pkg ->
                        val pkgInfo = (iconSources[pkg] as? SnapshotPackageIconSource.InstalledPackage)
                          ?.packageInfo
                        AppIcon(
                          packageInfo = pkgInfo,
                          label = pkg,
                          modifier = Modifier
                            .size(22.dp)
                            .squircleClip(5.dp)
                        )
                      }
                      if (node.appCount > visiblePackages.size || node.topAppPackageNames.size > visiblePackages.size) {
                        Text(
                          text = "...",
                          style = MiuixTheme.textStyles.footnote1,
                          color = colorScheme.onSurfaceVariantSummary
                        )
                      }
                    }
                  }
                }
                Column(
                  modifier = Modifier.padding(start = 12.dp),
                  horizontalAlignment = Alignment.End,
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  if (node.appCount > 0) {
                    Text(
                      text = pluralStringResource(
                        R.plurals.snapshot_time_node_apps_count,
                        node.appCount,
                        node.appCount
                      ),
                      style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp)
                        .withCustomFont(lcCondensedFontFamily()),
                      color = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariantSummary
                    )
                  }
                  if (trailingIcon != null) {
                    Icon(
                      imageVector = trailingIcon,
                      contentDescription = null,
                      tint = trailingIconTint,
                      modifier = Modifier.size(18.dp)
                    )
                  } else if (isSelected) {
                    Icon(
                      imageVector = MiuixIcons.Glass.Ok,
                      contentDescription = stringResource(R.string.snapshot_time_node_current),
                      tint = colorScheme.primary,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  val activeDayDetail = rememberActiveValue(dayDetail)
  WindowDialog(
    show = dayDetail != null,
    title = activeDayDetail.value?.date?.toString() ?: "",
    summary = activeDayDetail.value?.let { day ->
      if (day.updatedApps.isEmpty()) {
        stringResource(R.string.snapshot_contribution_no_updates)
      } else {
        day.updatedApps.joinToString("\n") { it.label }
      }
    } ?: "",
    onDismissRequest = { dayDetail = null },
    onDismissFinished = { if (dayDetail == null) activeDayDetail.value = null }
  ) {
    val snapshotTimestamp = activeDayDetail.value?.snapshotTimestamp
    if (!showDaySnapshotAction || snapshotTimestamp == null) {
      TextButton(
        text = stringResource(android.R.string.ok),
        onClick = { dayDetail = null },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.textButtonColorsPrimary()
      )
    } else {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        TextButton(
          text = stringResource(R.string.snapshot_contribution_view_snapshot),
          onClick = {
            dayDetail = null
            onSelect(snapshotTimestamp)
          },
          modifier = Modifier.weight(1f)
        )
        TextButton(
          text = stringResource(android.R.string.ok),
          onClick = { dayDetail = null },
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.textButtonColorsPrimary()
        )
      }
    }
  }
}

private const val DEFAULT_AUTO_REMOVE_THRESHOLD = 5
private const val AUTO_REMOVE_MIN = 2
private const val AUTO_REMOVE_MAX = 10
private const val MAX_VISIBLE_APP_COUNT = 6

@Composable
private fun ContributionGrid(
  data: SnapshotContributionData,
  onTileClick: (DayContribution) -> Unit
) {
  val context = LocalContext.current
  val colorScheme = MiuixTheme.colorScheme
  val scrollState = rememberScrollState()
  val weeks = remember(data) {
    val firstMonday = data.startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = generateSequence(firstMonday) { it.plusDays(1) }
      .takeWhile { !it.isAfter(data.endDate) }
      .toList()
    days.chunked(7)
  }
  var selectedDay by remember(data) {
    mutableStateOf(
      data.days.values
        .filter { it.isSnapshotDay || it.updateCount > 0 }
        .maxByOrNull { it.date }
    )
  }

  LaunchedEffect(weeks) {
    scrollState.scrollTo(scrollState.maxValue)
  }

  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = stringResource(
          R.string.snapshot_contribution_recent_days,
          ComputeSnapshotContributionsUseCase.RECENT_DAYS_COUNT
        ),
        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
        color = colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.weight(1f)
      )
      Text(
        text = pluralStringResource(
          R.plurals.snapshot_contribution_updates_count,
          data.totalUpdates,
          data.totalUpdates
        ),
        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp)
          .withCustomFont(lcCondensedFontFamily()),
        color = colorScheme.onSurfaceVariantSummary
      )
    }
    Spacer(Modifier.height(8.dp))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(scrollState),
      horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
    ) {
      weeks.forEach { week ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          week.forEach { date ->
            val day = data.days[date]
            val fillColor = when {
              day == null -> colorScheme.surfaceContainerHigh
              day.updateCount > 0 -> {
                val intensity = (0.4f + day.updateCount.coerceAtMost(5) / 5f * 0.55f)
                Color(SnapshotPalette.getColor(day.date.dayOfYear)).copy(alpha = intensity)
              }

              day.isSnapshotDay -> colorScheme.surface
              else -> colorScheme.surfaceContainerHigh
            }
            val snapshotRingColor = if (day?.isSnapshotDay == true) {
              day.snapshotColor?.let(::Color) ?: colorScheme.primary
            } else {
              null
            }
            val tileShape = RoundedCornerShape(3.5.dp)
            Box(
              modifier = Modifier
                .size(12.dp)
                .squircleBackground(fillColor, 3.dp)
                .then(
                  if (snapshotRingColor != null) {
                    Modifier.border(1.75.dp, snapshotRingColor, tileShape)
                  } else {
                    Modifier
                  }
                )
                .clickable(enabled = day != null) {
                  day?.let { clicked ->
                    selectedDay = clicked
                    onTileClick(clicked)
                  }
                }
            )
          }
        }
      }
    }
    Spacer(Modifier.height(8.dp))
    val detailText = selectedDay?.let { describeContributionDay(context, it) }
      ?: stringResource(R.string.snapshot_contribution_hint)
    Text(
      text = detailText,
      style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
      color = colorScheme.onSurfaceVariantSummary,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth()
    )
  }
}

private fun describeContributionDay(
  context: android.content.Context,
  day: DayContribution
): String {
  val dateStr = day.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()))
  val updatesStr = context.resources.getQuantityString(
    R.plurals.snapshot_contribution_updates_count,
    day.updateCount,
    day.updateCount
  )
  return if (day.isSnapshotDay) {
    context.getString(R.string.snapshot_contribution_tile_detail_with_snapshot, dateStr, updatesStr)
  } else {
    context.getString(R.string.snapshot_contribution_tile_detail, dateStr, updatesStr)
  }
}
