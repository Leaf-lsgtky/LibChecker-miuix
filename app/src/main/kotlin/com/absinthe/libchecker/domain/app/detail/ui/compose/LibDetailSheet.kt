package com.absinthe.libchecker.domain.app.detail.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.app.detail.action.DetailItemResolver
import com.absinthe.libchecker.domain.app.detail.model.DetailInfoItemDisplay
import com.absinthe.libchecker.domain.app.detail.model.DetailInfoTextStyle
import com.absinthe.libchecker.domain.app.detail.model.LibraryDetailBottomSheetState
import com.absinthe.libchecker.domain.app.detail.model.LibraryDetailHeaderDisplay
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.ui.compose.component.AdaptiveTabRow
import com.absinthe.libchecker.ui.compose.component.InsightCardSkeleton
import com.absinthe.libchecker.ui.compose.component.LibDetailSheetSkeleton
import com.absinthe.libchecker.ui.compose.component.SkeletonBox
import com.absinthe.libchecker.ui.compose.theme.lcCondensedFontFamily
import com.absinthe.libchecker.ui.compose.theme.lcMonospaceFontFamily
import com.absinthe.libchecker.ui.compose.theme.withCustomFont
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.graphicsLayer
import com.absinthe.libchecker.ui.compose.util.sheetBottomSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetContentSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetHeightTransition
import com.absinthe.libchecker.ui.compose.util.toAnnotatedString
import com.absinthe.libchecker.utils.extensions.openUrlInBrowser
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/** Library detail sheet: header, locale switch and the rule description of the selected library. */
@Composable
fun LibDetailSheet(
  show: Boolean,
  viewModel: DetailViewModel,
  libName: String,
  @LibType type: Int,
  regexName: String?,
  isValidLib: Boolean,
  packageInfo: android.content.pm.PackageInfo? = null,
  insightViewModel: com.absinthe.libchecker.domain.app.detail.presentation.LibraryInsightViewModel? = null,
  onDismiss: () -> Unit,
  onDismissFinished: (() -> Unit)? = null
) {
  val context = LocalContext.current
  val dialogTitle = remember(libName, type) {
    if (type == ACTION) "< $libName >" else libName
  }
  var state by remember(dialogTitle) {
    mutableStateOf<LibraryDetailBottomSheetState>(
      LibraryDetailBottomSheetState.Loading(dialogTitle)
    )
  }
  var selectedLocale by remember { mutableStateOf(GlobalValues.preferredRuleLanguage) }
  var libraryUuid by remember(dialogTitle) { mutableStateOf<String?>(null) }
  val insightState = insightViewModel?.state?.collectAsStateWithLifecycle()?.value

  LaunchedEffect(show, libraryUuid, packageInfo, selectedLocale) {
    val uuid = libraryUuid
    val info = packageInfo
    if (show && insightViewModel != null && uuid != null && info != null) {
      insightViewModel.preload()
      insightViewModel.load(uuid, info, selectedLocale)
    }
  }

  LaunchedEffect(show, libName, type, regexName, isValidLib) {
    if (!show) return@LaunchedEffect
    libraryUuid = null
    insightViewModel?.reset()
    insightViewModel?.preload()
    if (!isValidLib) {
      state = LibraryDetailBottomSheetState.NotFound(dialogTitle, FALLBACK_HEADER)
      return@LaunchedEffect
    }
    state = LibraryDetailBottomSheetState.Loading(dialogTitle)
    runCatching {
      coroutineScope {
        val header = async {
          viewModel.getLibraryDetailDialogHeader(libName, type, isValidLib)
        }
        val content = async {
          viewModel.getLibraryDetailDialogData(
            libName = libName,
            type = type,
            regexName = regexName,
            isValidLib = isValidLib,
            preferredLocale = selectedLocale
          )
        }
        header.await() to content.await()
      }
    }.onSuccess { (header, result) ->
      when (result) {
        is DetailItemResolver.Result.Found -> {
          libraryUuid = result.libraryUuid
          state = LibraryDetailBottomSheetState.Content(
            title = dialogTitle,
            header = header,
            content = result.content
          )
        }

        DetailItemResolver.Result.NotFound -> {
          state = LibraryDetailBottomSheetState.NotFound(dialogTitle, header)
        }
      }
    }
  }

  val current = state
  val colorScheme = MiuixTheme.colorScheme
  val headerDisplay = when (current) {
    is LibraryDetailBottomSheetState.Content -> current.header
    is LibraryDetailBottomSheetState.NotFound -> current.header
    is LibraryDetailBottomSheetState.Loading -> null
  }
  WindowBottomSheet(
    show = show,
    title = null,
    onDismissRequest = onDismiss,
    onDismissFinished = onDismissFinished
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .sheetHeightTransition()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Crossfade(
          targetState = headerDisplay,
          animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
          label = "LibDetailIconTransition"
        ) { resolvedHeader ->
          if (resolvedHeader != null) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colorScheme.surface)
                .border(
                  BorderStroke(1.dp, colorScheme.dividerLine),
                  CircleShape
                ),
              contentAlignment = Alignment.Center
            ) {
              AsyncImage(
                model = resolvedHeader.iconRes,
                contentDescription = null,
                colorFilter = if (resolvedHeader.isSimpleColorIcon) {
                  ColorFilter.tint(colorScheme.onSurface)
                } else {
                  null
                },
                modifier = Modifier.size(28.dp)
              )
            }
          } else {
            SkeletonBox(
              modifier = Modifier.size(48.dp),
              shape = CircleShape
            )
          }
        }
        Spacer(Modifier.height(8.dp))
        Text(
          text = current.title,
          style = MiuixTheme.textStyles.body1.copy(
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
          ).withCustomFont(lcCondensedFontFamily()),
          color = colorScheme.onSurface,
          textAlign = TextAlign.Center,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
      var lastInsightContent by remember(dialogTitle) {
        mutableStateOf<com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightContent?>(null)
      }
      if (insightState is com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightUiState.Content) {
        lastInsightContent = insightState.content
      }
      val showInsightCard =
        insightState is com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightUiState.Loading ||
          insightState is com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightUiState.Content
      AnimatedVisibility(
        visible = showInsightCard,
        enter = fadeIn(animationSpec = tween(220)) +
          expandVertically(
            animationSpec = spring(
              dampingRatio = 0.88f,
              stiffness = Spring.StiffnessMediumLow
            ),
            expandFrom = Alignment.Top
          ),
        exit = fadeOut(animationSpec = tween(150)) +
          shrinkVertically(
            animationSpec = tween(180),
            shrinkTowards = Alignment.Top
          )
      ) {
        val ready = insightState is com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightUiState.Content
        Crossfade(
          targetState = ready,
          animationSpec = tween(200),
          label = "LibInsightTransition"
        ) { hasContent ->
          val content = lastInsightContent
          if (hasContent && content != null) {
            InsightCard(content = content)
          } else {
            InsightCardSkeleton()
          }
        }
      }
      var lastContent by remember(dialogTitle) {
        mutableStateOf<LibraryDetailBottomSheetState.Content?>(null)
      }
      if (current is LibraryDetailBottomSheetState.Content) {
        lastContent = current
      }
      val bodyMode = when (current) {
        is LibraryDetailBottomSheetState.Loading -> LibDetailBodyMode.Loading
        is LibraryDetailBottomSheetState.NotFound -> LibDetailBodyMode.NotFound
        is LibraryDetailBottomSheetState.Content -> LibDetailBodyMode.Content
      }
      Crossfade(
        targetState = bodyMode,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f, fill = false)
          .then(
            if (bodyMode != LibDetailBodyMode.Content) {
              Modifier.padding(bottom = sheetBottomSafePadding(16.dp))
            } else {
              Modifier
            }
          ),
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "LibDetailBodyTransition"
      ) { mode ->
        when (mode) {
          LibDetailBodyMode.Loading -> {
            LibDetailSheetSkeleton()
          }

          LibDetailBodyMode.NotFound -> {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = stringResource(R.string.not_found),
                style = MiuixTheme.textStyles.body2,
                color = colorScheme.onSurfaceVariantSummary
              )
            }
          }

          LibDetailBodyMode.Content -> {
            val readyContent = lastContent ?: return@Crossfade
            val locale = readyContent.content.locales.firstOrNull {
              it.localeTag == selectedLocale
            } ?: readyContent.content.locales.first()
            Column(Modifier.fillMaxWidth()) {
              if (readyContent.content.locales.size > 1) {
                val selectedLocaleIndex = readyContent.content.locales.indexOfFirst {
                  it.localeTag == locale.localeTag
                }.coerceAtLeast(0)
                AdaptiveTabRow(
                  tabs = readyContent.content.locales.map { it.localeName },
                  selectedTabIndex = selectedLocaleIndex,
                  onTabSelected = { index ->
                    val candidate = readyContent.content.locales[index]
                    selectedLocale = candidate.localeTag
                    GlobalValues.preferredRuleLanguage = candidate.localeTag
                  },
                  backdrop = null,
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                )
              }
              val scrollState = rememberScrollState()
              val localeAlpha = remember { androidx.compose.animation.core.Animatable(1f) }
              var renderedLocaleTag by remember { mutableStateOf(locale.localeTag) }
              LaunchedEffect(locale.localeTag) {
                if (renderedLocaleTag != locale.localeTag) {
                  renderedLocaleTag = locale.localeTag
                  scrollState.scrollTo(0)
                  localeAlpha.snapTo(0.35f)
                  localeAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                  )
                }
              }
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f, fill = false)
                  .verticalScroll(scrollState)
                  .padding(bottom = sheetBottomSafePadding(16.dp))
                  .graphicsLayer { alpha = localeAlpha.value },
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                locale.items.forEach { item ->
                  DetailInfoRow(item = item)
                }
              }
            }
          }
        }
      }
    }
  }
}

private enum class LibDetailBodyMode {
  Loading,
  NotFound,
  Content
}

@Composable
private fun DetailInfoRow(item: DetailInfoItemDisplay) {
  val context = LocalContext.current
  val colorScheme = MiuixTheme.colorScheme
  val isTitle = item.textStyle == DetailInfoTextStyle.TITLE
  val linkUrl = item.linkUrl
  val cardContent: @Composable () -> Unit = {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      AsyncImage(
        model = item.iconRes,
        contentDescription = null,
        colorFilter = ColorFilter.tint(colorScheme.onSurface),
        modifier = Modifier
          .padding(end = 14.dp)
          .size(22.dp)
      )
      Column(Modifier.weight(1f)) {
        if (item.tipRes != 0) {
          Text(
            text = stringResource(item.tipRes),
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 11.sp),
            color = colorScheme.onSurfaceVariantSummary
          )
          Spacer(Modifier.height(2.dp))
        }
        val bodyStyle = when {
          linkUrl != null -> MiuixTheme.textStyles.body2.copy(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textDecoration = TextDecoration.Underline
          )

          isTitle -> MiuixTheme.textStyles.body1.copy(
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
          )

          else -> MiuixTheme.textStyles.body2.copy(
            fontSize = 14.sp,
            lineHeight = 20.sp
          )
        }
        Text(
          text = item.text.toAnnotatedString(),
          style = bodyStyle,
          color = if (linkUrl != null) colorScheme.primary else colorScheme.onSurface
        )
      }
    }
  }
  if (linkUrl != null) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.defaultColors(
        color = colorScheme.onSurface.copy(alpha = 0.04f)
      ),
      onClick = { context.openUrlInBrowser(linkUrl) },
      showIndication = true,
      insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
      cardContent()
    }
  } else {
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.defaultColors(
        color = colorScheme.onSurface.copy(alpha = 0.04f)
      ),
      insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
      cardContent()
    }
  }
}

private val FALLBACK_HEADER = LibraryDetailHeaderDisplay(
  iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_sdk_placeholder,
  isSimpleColorIcon = false
)

@Composable
private fun InsightCard(
  content: com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightContent
) {
  val colorScheme = MiuixTheme.colorScheme
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 12.dp),
    colors = CardDefaults.defaultColors(
      color = colorScheme.onSurface.copy(alpha = 0.04f)
    ),
    insideMargin = PaddingValues(14.dp)
  ) {
    Column(Modifier.fillMaxWidth()) {
      Text(
        text = stringResource(R.string.lib_insight_title),
        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
        color = colorScheme.onSurfaceVariantSummary
      )
      content.summary.forEach { field ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = field.label,
            modifier = Modifier.weight(1f),
            style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
            color = colorScheme.onSurface
          )
          Text(
            text = field.values.joinToString(", "),
            style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp)
              .withCustomFont(lcMonospaceFontFamily()),
            color = colorScheme.onSurfaceVariantSummary
          )
        }
      }
    }
  }
}
