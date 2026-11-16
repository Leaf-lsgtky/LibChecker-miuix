package com.absinthe.libchecker.domain.app.detail.ui.compose

import android.content.pm.PackageInfo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.SIGNATURES
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.domain.app.detail.abi.AppDetailAbiLabelData
import com.absinthe.libchecker.domain.app.detail.action.AppElfDetail
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureAction
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureIcon
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureItemData
import com.absinthe.libchecker.domain.app.detail.header.AppDetailHeaderExtraInfo
import com.absinthe.libchecker.domain.app.detail.model.AppIconItem
import com.absinthe.libchecker.domain.app.detail.model.DISABLED
import com.absinthe.libchecker.domain.app.detail.model.EXPORTED
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.model.MODE_SORT_BY_LIB
import com.absinthe.libchecker.domain.app.detail.model.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.domain.app.detail.model.PermissionDetailContent
import com.absinthe.libchecker.domain.app.detail.model.SignatureDetailItem
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.ui.AppDetailActivity
import com.absinthe.libchecker.domain.app.detail.ui.DetailTabSpecBuilder
import com.absinthe.libchecker.domain.app.list.ui.compose.AppIcon
import com.absinthe.libchecker.domain.app.model.PackageInfoFormatter
import com.absinthe.libchecker.domain.home.ui.compose.rememberMainPagerState
import com.absinthe.libchecker.ui.compose.component.AdaptiveBackButton
import com.absinthe.libchecker.ui.compose.component.AdaptiveGlassOverflowMenu
import com.absinthe.libchecker.ui.compose.component.AdaptiveSearchOverlay
import com.absinthe.libchecker.ui.compose.component.AdaptiveTabRow
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopBarIconButton
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopBarMenuAction
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopSearchButton
import com.absinthe.libchecker.ui.compose.component.CardSegment
import com.absinthe.libchecker.ui.compose.component.DetailTabListSkeleton
import com.absinthe.libchecker.ui.compose.component.LcEmptyState
import com.absinthe.libchecker.ui.compose.component.MiuixPageHost
import com.absinthe.libchecker.ui.compose.component.TopBarSearchConfig
import com.absinthe.libchecker.ui.compose.component.adaptiveVerticalScroll
import com.absinthe.libchecker.ui.compose.component.rememberFittingTabMinWidth
import com.absinthe.libchecker.ui.compose.component.rememberTopBarSearchState
import com.absinthe.libchecker.ui.compose.theme.lcCondensedFontFamily
import com.absinthe.libchecker.ui.compose.theme.withCustomFont
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import com.absinthe.libchecker.ui.compose.util.rememberActiveValue
import com.absinthe.libchecker.ui.compose.util.sheetBottomSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetContentSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetHeightTransition
import com.absinthe.libchecker.utils.extensions.copyBitmapToClipboard
import com.absinthe.libchecker.utils.extensions.launchLibReferencePage
import com.absinthe.libchecker.utils.showToast
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.CloudFill
import top.yukonga.miuix.kmp.icon.glass.Copy
import top.yukonga.miuix.kmp.icon.glass.HorizontalSplit
import top.yukonga.miuix.kmp.icon.glass.Menu
import top.yukonga.miuix.kmp.icon.glass.Sort
import top.yukonga.miuix.kmp.overlay.OverlayCascadingListPopup
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerGestureNestedScrollConnection
import top.yukonga.miuix.kmp.utils.PagerInterceptionMode
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.pagerGestureOverride
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog
import com.absinthe.libchecker.ui.compose.component.FastScrollLazyColumn as LazyColumn

/**
 * App detail page: header card with feature badges, category tabs and the component/library list of
 * the selected tab. Rows follow the flat, divider separated layout used by the system app info page.
 */
@Composable
fun AppDetailScreen(
  viewModel: DetailViewModel,
  packageInfo: PackageInfo,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  isHarmonyVariant: Boolean = false,
  insightViewModel: com.absinthe.libchecker.domain.app.detail.presentation.LibraryInsightViewModel? = null,
  onCompareWithInstalled: (() -> Unit)? = null,
  listState: LazyListState = rememberLazyListState()
) {
  val context = LocalContext.current
  val activity = context as? FragmentActivity
  val scrollBehavior = MiuixScrollBehavior()
  val colorScheme = MiuixTheme.colorScheme
  val scope = rememberCoroutineScope()
  var isHarmonyMode by rememberSaveable { mutableStateOf(false) }
  val tabSpec = remember(packageInfo.packageName, isHarmonyMode) {
    DetailTabSpecBuilder(context).build(
      isHarmonyMode = isHarmonyMode,
      isApkPreview = false
    )
  }
  val pages = tabSpec.items
  val pagerState = rememberPagerState(initialPage = 0) { pages.size.coerceAtLeast(1) }
  val detailPagerState = rememberMainPagerState(
    pagerState = pagerState,
    coroutineScope = scope
  )
  LaunchedEffect(pagerState.currentPage, pagerState.settledPage) {
    detailPagerState.syncPage()
  }
  LaunchedEffect(pages.size) {
    val maxIndex = (pages.size - 1).coerceAtLeast(0)
    if (pagerState.currentPage > maxIndex) {
      pagerState.scrollToPage(0)
      detailPagerState.syncPage()
    }
  }
  val selectedTab = detailPagerState.selectedPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
  val selectedType = pages.getOrNull(selectedTab)?.type ?: NATIVE
  val contentState = viewModel.contentState

  val searchState = rememberTopBarSearchState()
  val query = searchState.query
  val selectedProcesses = remember(packageInfo.packageName, isHarmonyMode) {
    mutableStateMapOf<Int, String?>()
  }
  var sortBySize by rememberSaveable {
    mutableStateOf(GlobalValues.libSortMode == MODE_SORT_BY_SIZE)
  }
  var nativeTabIndex by remember { mutableIntStateOf(0) }
  val featureItems = remember { mutableStateListOf<AppDetailFeatureItemData>() }
  var libDetailRequest by remember { mutableStateOf<LibDetailRequest?>(null) }
  var showAppInfo by remember { mutableStateOf(false) }
  var featureDetail by remember { mutableStateOf<AppDetailFeatureItemData?>(null) }
  var longPressChip by remember { mutableStateOf<LibStringItemChip?>(null) }
  var elfDetail by remember { mutableStateOf<AppElfDetail?>(null) }
  var permissionDetail by remember { mutableStateOf<PermissionDetailContent?>(null) }
  var signatureDetail by remember { mutableStateOf<List<SignatureDetailItem>?>(null) }
  var elfInfoIcon by remember { mutableStateOf<Int?>(null) }
  var elfDetailName by remember { mutableStateOf<String?>(null) }
  var showInstallSource by remember { mutableStateOf(false) }
  var showXposedInfo by remember { mutableStateOf(false) }
  var showComponentMenu by remember { mutableStateOf(false) }
  var showComponentDemo by remember { mutableStateOf(false) }
  var componentMenuState by remember(packageInfo.packageName) {
    mutableStateOf(viewModel.getComponentMenuState(packageInfo.packageName))
  }
  var showIconLayers by remember { mutableStateOf(false) }
  var showOnlineAnalysis by remember { mutableStateOf(false) }
  val onlineStatisticRulesAvailable by viewModel.onlineStatisticRulesAvailable.collectAsStateWithLifecycle()
  LaunchedEffect(packageInfo.packageName) {
    viewModel.refreshOnlineStatisticRulesAvailability()
  }
  var permissionProviders by remember {
    mutableStateOf<List<com.absinthe.libchecker.domain.app.detail.action.ProviderPermissionItem>?>(null)
  }
  val is64Bit by viewModel.featureState.is64Bit.collectAsStateWithLifecycle()
  val isFeatureLoading by viewModel.featureState.isLoading.collectAsStateWithLifecycle()
  val featureAppIcons by viewModel.featureState.appIconsFlow.collectAsStateWithLifecycle()
  val abiBundle by viewModel.featureState.abiBundleStateFlow.collectAsStateWithLifecycle()
  val abiLabelData = remember(abiBundle) {
    abiBundle?.let { bundle ->
      viewModel.buildAppDetailAbiLabelData(bundle.abi, bundle.abiSet, apkAnalyticsMode = false)
    }
  }
  val appLabel = remember(packageInfo.packageName) {
    packageInfo.applicationInfo?.loadLabel(context.packageManager)?.toString()
      ?: packageInfo.packageName
  }

  val nativeTabs by contentState.nativeLibTabs.collectAsStateWithLifecycle()
  val nativeItems by contentState.nativeLibItems.collectAsStateWithLifecycle()
  val staticItems by contentState.staticLibItems.collectAsStateWithLifecycle()
  val metaItems by contentState.metaDataItems.collectAsStateWithLifecycle()
  val permissionItems by contentState.permissionsItems.collectAsStateWithLifecycle()
  val signatureItems by contentState.signaturesLibItems.collectAsStateWithLifecycle()
  val serviceItems by contentState.componentsMap.get(SERVICE)!!.collectAsStateWithLifecycle()
  val activityItems by contentState.componentsMap.get(ACTIVITY)!!.collectAsStateWithLifecycle()
  val receiverItems by contentState.componentsMap.get(RECEIVER)!!.collectAsStateWithLifecycle()
  val providerItems by contentState.componentsMap.get(PROVIDER)!!.collectAsStateWithLifecycle()
  var headerExtraInfo by remember {
    mutableStateOf<AppDetailHeaderExtraInfo?>(null)
  }
  LaunchedEffect(packageInfo.packageName) {
    val showAndroidVersion =
      (GlobalValues.itemAdvancedOptions and AdvancedOptions.SHOW_ANDROID_VERSION) != 0
    headerExtraInfo = runCatching {
      viewModel.buildAppDetailHeaderExtraInfo(packageInfo, showAndroidVersion)
    }.getOrNull()
  }

  LaunchedEffect(packageInfo.packageName) {
    featureItems.clear()
    viewModel.featureState.featuresFlow.collect { feature ->
      viewModel.buildAppDetailFeatureItem(
        feature = feature,
        currentFeatureCount = featureItems.size,
        apkAnalyticsMode = false,
        canShowInstallSource = true,
        canShowAppIcons = viewModel.featureState.appIcons.isNotEmpty()
      )?.let { item ->
        if (featureItems.none { it.titleRes == item.titleRes }) {
          featureItems.add(item)
        }
      }
    }
  }

  LaunchedEffect(pagerState.currentPage, isHarmonyMode, pages) {
    if (isHarmonyMode) {
      viewModel.initAbilities(packageInfo.packageName)
      return@LaunchedEffect
    }
    val currentIndex = pagerState.currentPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    val preloadRange = ((currentIndex - 1).coerceAtLeast(0))..((currentIndex + 1).coerceAtMost(pages.lastIndex))
    preloadRange.forEach { index ->
      when (pages.getOrNull(index)?.type) {
        NATIVE -> viewModel.initSoAnalysisData()
        SERVICE, ACTIVITY, RECEIVER, PROVIDER -> viewModel.initComponentsData()
        PERMISSION -> viewModel.initPermissionData()
        METADATA -> viewModel.initMetaDataData()
        SIGNATURES -> viewModel.initSignatures()
        STATIC -> viewModel.initStaticData()
      }
    }
  }

  LaunchedEffect(selectedType, nativeTabs) {
    if (!isHarmonyMode && (selectedType == NATIVE || nativeItems == null)) {
      nativeTabs?.firstOrNull()?.let(viewModel::loadSoAnalysisData)
    }
  }

  LaunchedEffect(viewModel) {
    viewModel.nativeLibraryExtractionResults.collect { result ->
      result.result
        .onSuccess { context.showToast(R.string.lib_detail_elf_extract_success) }
        .onFailure { context.showToast(R.string.lib_detail_elf_extract_failed) }
    }
  }

  OnlineRuleAnalysisSheet(
    show = showOnlineAnalysis,
    viewModel = viewModel,
    onDismiss = { showOnlineAnalysis = false }
  )

  WindowDialog(
    show = showComponentDemo,
    title = stringResource(R.string.lib_detail_component_demo),
    onDismissRequest = { showComponentDemo = false }
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
          color = colorScheme.onSurface.copy(alpha = 0.04f)
        ),
        insideMargin = PaddingValues(16.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          componentMenuState.componentDemoItems.forEach { chip ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = chip.item.name,
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
                color = colorScheme.onSurface,
                textDecoration = if (chip.item.source == DISABLED) {
                  TextDecoration.LineThrough
                } else {
                  null
                }
              )
              if (chip.item.source == EXPORTED) {
                Text(
                  text = stringResource(R.string.adv_mark_exported),
                  style = MiuixTheme.textStyles.footnote2.copy(fontSize = 10.sp),
                  color = colorScheme.onSurfaceVariantSummary
                )
              }
            }
          }
        }
      }
      TextButton(
        text = stringResource(android.R.string.ok),
        onClick = { showComponentDemo = false },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.textButtonColorsPrimary()
      )
    }
  }

  XposedInfoSheet(
    show = showXposedInfo,
    viewModel = viewModel,
    packageName = packageInfo.packageName,
    onDismiss = { showXposedInfo = false }
  )

  InstallSourceSheet(
    show = showInstallSource,
    viewModel = viewModel,
    packageName = packageInfo.packageName,
    onOpenApp = { target ->
      showInstallSource = false
      runCatching {
        context.startActivity(
          android.content.Intent(context, AppDetailActivity::class.java).apply {
            putExtra(EXTRA_PACKAGE_NAME, target)
          }
        )
      }
    },
    onDismiss = { showInstallSource = false }
  )

  FeatureDetailSheet(
    show = featureDetail != null,
    viewModel = viewModel,
    packageInfo = packageInfo,
    item = featureDetail,
    onDismiss = { featureDetail = null }
  )

  AppInfoSheet(
    show = showAppInfo,
    viewModel = viewModel,
    packageInfo = packageInfo,
    onDismiss = { showAppInfo = false }
  )

  val activePermissionProviders = rememberActiveValue(permissionProviders)
  WindowBottomSheet(
    show = permissionProviders != null,
    title = stringResource(R.string.lib_detail_permission_providers),
    onDismissRequest = { permissionProviders = null },
    onDismissFinished = { if (permissionProviders == null) activePermissionProviders.value = null }
  ) {
    val providers = activePermissionProviders.value
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp)
        .sheetContentSafePadding()
        .sheetHeightTransition(),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      if (providers.isNullOrEmpty()) {
        Text(
          text = stringResource(R.string.empty_list),
          style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
          color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
      } else {
        providers.forEach { provider ->
          SheetActionRow(text = "${provider.providerName} (${provider.packageName})") {
            permissionProviders = null
            runCatching {
              context.startActivity(
                android.content.Intent(context, AppDetailActivity::class.java).apply {
                  putExtra(EXTRA_PACKAGE_NAME, provider.packageName)
                }
              )
            }
          }
        }
      }
    }
  }
  val activePermissionDetail = rememberActiveValue(permissionDetail)
  WindowBottomSheet(
    show = permissionDetail != null,
    title = activePermissionDetail.value?.let { it.label?.toString() ?: it.name } ?: "",
    onDismissRequest = { permissionDetail = null },
    onDismissFinished = { if (permissionDetail == null) activePermissionDetail.value = null }
  ) {
    activePermissionDetail.value?.let { detail ->
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp)
          .sheetContentSafePadding()
          .sheetHeightTransition()
      ) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.defaultColors(
            color = colorScheme.onSurface.copy(alpha = 0.04f)
          ),
          insideMargin = PaddingValues(16.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            detail.description?.toString()?.takeIf(String::isNotBlank)?.let { description ->
              SheetInfoBlock(
                label = stringResource(R.string.lib_detail_permission_detail),
                value = description
              )
            }
            detail.providerAppName?.takeIf(String::isNotBlank)?.let { provider ->
              SheetInfoBlock(
                label = stringResource(R.string.lib_detail_permission_provider),
                value = provider
              )
            }
          }
        }
      }
    }
  }
  val activeSignatureDetail = rememberActiveValue(signatureDetail)
  WindowBottomSheet(
    show = signatureDetail != null,
    title = stringResource(R.string.signature_detail),
    onDismissRequest = { signatureDetail = null },
    onDismissFinished = { if (signatureDetail == null) activeSignatureDetail.value = null }
  ) {
    activeSignatureDetail.value?.let { items ->
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .sheetHeightTransition()
      ) {
        Column(
          modifier = Modifier
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
            .padding(bottom = sheetBottomSafePadding(16.dp)),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 4.dp),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(colorScheme.surface)
                .border(1.dp, colorScheme.dividerLine, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Image(
                painter = painterResource(R.drawable.ic_signatures),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(32.dp)
              )
            }
          }
          items.forEach { item ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.defaultColors(
                color = colorScheme.onSurface.copy(alpha = 0.04f)
              ),
              insideMargin = PaddingValues(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(Modifier.weight(1f)) {
                  Text(
                    text = item.type,
                    style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
                    color = MiuixTheme.colorScheme.onSurface
                  )
                  Spacer(Modifier.height(2.dp))
                  Text(
                    text = item.content,
                    style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                  )
                }
                IconButton(
                  onClick = {
                    context.copyTextWithToast(item.content)
                  }
                ) {
                  Icon(
                    imageVector = MiuixIcons.Glass.Copy,
                    contentDescription = stringResource(android.R.string.copy),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
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
  val activeElfDetail = rememberActiveValue(elfDetail)
  val activeElfDetailName = rememberActiveValue(elfDetailName)
  WindowBottomSheet(
    show = elfDetail != null,
    title = activeElfDetailName.value ?: stringResource(R.string.lib_detail_elf_info),
    onDismissRequest = { elfDetail = null; elfDetailName = null },
    onDismissFinished = {
      if (elfDetail == null) {
        activeElfDetail.value = null
        activeElfDetailName.value = null
      }
    }
  ) {
    activeElfDetail.value?.let { detail ->
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp)
          .sheetContentSafePadding()
          .sheetHeightTransition()
      ) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.defaultColors(
            color = colorScheme.onSurface.copy(alpha = 0.04f)
          ),
          insideMargin = PaddingValues(16.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetInfoBlock(
              label = stringResource(R.string.lib_detail_elf_deps),
              value = detail.deps.joinToString("\n").ifEmpty { "-" }
            )
            SheetInfoBlock(
              label = stringResource(R.string.lib_detail_elf_entry_points),
              value = detail.entryPoints.joinToString("\n").ifEmpty { "-" }
            )
            SheetInfoBlock(
              label = stringResource(R.string.lib_detail_elf_stripped),
              value = stringResource(
                if (detail.isStripped) {
                  R.string.lib_detail_optimization_on
                } else {
                  R.string.lib_detail_optimization_off
                }
              )
            )
          }
        }
      }
    }
  }
  val activeLongPressChip = rememberActiveValue(longPressChip)
  WindowBottomSheet(
    show = longPressChip != null,
    title = activeLongPressChip.value?.item?.name ?: "",
    onDismissRequest = { longPressChip = null },
    onDismissFinished = { if (longPressChip == null) activeLongPressChip.value = null }
  ) {
    activeLongPressChip.value?.let { chip ->
      val actions = remember(chip) {
        viewModel.buildDetailItemLongClickActions(
          item = chip,
          packageName = packageInfo.packageName,
          detailType = selectedType,
          canReference = true
        )
      }
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp)
          .sheetContentSafePadding()
          .sheetHeightTransition(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        SheetActionRow(text = stringResource(android.R.string.copy)) {
          context.copyTextWithToast(actions.copyText)
          longPressChip = null
        }
        if (selectedType == PERMISSION) {
          SheetActionRow(text = stringResource(R.string.lib_detail_permission_detail)) {
            val name = chip.item.name
            longPressChip = null
            scope.launch {
              permissionDetail = runCatching { viewModel.getPermissionDetail(name) }.getOrNull()
            }
          }
        }
        if (selectedType == PERMISSION) {
          SheetActionRow(text = stringResource(R.string.lib_detail_permission_providers)) {
            val name = chip.item.name
            longPressChip = null
            scope.launch {
              permissionProviders = runCatching {
                viewModel.getPermissionProviders(name)
              }.getOrNull()
            }
          }
        }
        actions.elfInfo?.let { elfInfo ->
          SheetActionRow(text = stringResource(R.string.lib_detail_elf_info)) {
            elfDetailName = elfInfo.elfPath.substringAfterLast('/')
            viewModel.loadElfDetail(packageInfo.packageName, elfInfo.elfPath)
            longPressChip = null
          }
        }
        if (actions.elfExtractAvailable) {
          SheetActionRow(text = stringResource(R.string.lib_detail_elf_extract)) {
            viewModel.extractNativeLibrary(chip.item)
            longPressChip = null
          }
        }
        actions.reference?.let { reference ->
          SheetActionRow(text = stringResource(R.string.lib_detail_view_reference)) {
            (context as? androidx.fragment.app.FragmentActivity)?.launchLibReferencePage(
              refName = reference.refName,
              refLabel = reference.label,
              refType = reference.type,
              refList = null
            )
            longPressChip = null
          }
        }
        if (
          selectedType == ACTIVITY ||
          selectedType == SERVICE ||
          selectedType == RECEIVER ||
          selectedType == PROVIDER
        ) {
          val componentName = chip.item.name
          if (com.absinthe.libchecker.integrations.blocker.BlockerManager.isSupportInteraction) {
            val blocked = remember(componentName) {
              runCatching {
                com.absinthe.libchecker.integrations.blocker.BlockerManager()
                  .queryBlockedComponent(context, packageInfo.packageName)
                  .any { it.name == componentName || it.name == packageInfo.packageName + componentName }
              }.getOrDefault(false)
            }
            SheetActionRow(
              text = stringResource(
                if (blocked) {
                  R.string.integration_blocker_menu_unblock
                } else {
                  R.string.integration_blocker_menu_block
                }
              )
            ) {
              runCatching {
                com.absinthe.libchecker.integrations.blocker.BlockerManager().addBlockedComponent(
                  context = context,
                  packageName = packageInfo.packageName,
                  componentName = componentName,
                  type = selectedType,
                  shouldBlock = !blocked
                )
              }
              longPressChip = null
            }
          }
          if (com.absinthe.libchecker.integrations.monkeyking.MonkeyKingManager.isSupportInteraction) {
            val blocked = remember(componentName) {
              runCatching {
                com.absinthe.libchecker.integrations.monkeyking.MonkeyKingManager()
                  .queryBlockedComponent(context, packageInfo.packageName)
                  .any { it.name == componentName || it.name == packageInfo.packageName + componentName }
              }.getOrDefault(false)
            }
            SheetActionRow(
              text = stringResource(
                if (blocked) {
                  R.string.integration_monkey_king_menu_unblock
                } else {
                  R.string.integration_monkey_king_menu_block
                }
              )
            ) {
              runCatching {
                com.absinthe.libchecker.integrations.monkeyking.MonkeyKingManager().addBlockedComponent(
                  context = context,
                  packageName = packageInfo.packageName,
                  componentName = componentName,
                  type = selectedType,
                  shouldBlock = !blocked
                )
              }
              longPressChip = null
            }
          }
          if (
            selectedType == ACTIVITY &&
            com.absinthe.libchecker.integrations.anywhere.AnywhereManager.isSupportInteraction
          ) {
            SheetActionRow(
              text = stringResource(R.string.integration_anywhere_menu_editor)
            ) {
              runCatching {
                com.absinthe.libchecker.integrations.anywhere.AnywhereManager.launchActivityEditor(
                  context,
                  packageInfo.packageName,
                  componentName
                )
              }
              longPressChip = null
            }
          }
        }
      }
    }
  }
  val activeLibDetailRequest = rememberActiveValue(libDetailRequest)
  activeLibDetailRequest.value?.let { request ->
    LibDetailSheet(
      show = libDetailRequest != null,
      viewModel = viewModel,
      libName = request.name,
      type = request.type,
      regexName = request.regexName,
      isValidLib = request.isValid,
      packageInfo = packageInfo,
      insightViewModel = insightViewModel,
      onDismiss = { libDetailRequest = null },
      onDismissFinished = {
        if (libDetailRequest == null) {
          activeLibDetailRequest.value = null
        }
      }
    )
  }

  val componentMenuAnchor = rememberGlassPopupAnchor()
  val demoLabel = stringResource(R.string.lib_detail_component_demo)
  val optionItems = componentMenuState.componentOptions.map { option ->
    DropdownItem(
      text = stringResource(option.labelRes),
      selected = option.isChecked,
      onClick = {
        viewModel.setComponentDisplayOption(option.option, !option.isChecked)
        componentMenuState = viewModel.getComponentMenuState(packageInfo.packageName)
      }
    )
  }
  val menuEntries = listOf(
    DropdownEntry(items = optionItems),
    DropdownEntry(
      items = listOf(
        DropdownItem(
          text = demoLabel,
          onClick = {
            showComponentMenu = false
            showComponentDemo = true
          }
        )
      )
    )
  )
  val pageListStates = remember(isHarmonyMode) { mutableStateMapOf<Int, LazyListState>() }
  val density = LocalDensity.current
  val headerTopGapPx = with(density) { 6.dp.toPx() }
  var collapsibleHeightPx by remember { mutableFloatStateOf(0f) }
  var headerCollapsePx by rememberSaveable(packageInfo.packageName, isHarmonyMode) {
    mutableFloatStateOf(0f)
  }
  val maxCollapsePx = (collapsibleHeightPx + headerTopGapPx).coerceAtLeast(0f)
  val collapseTitleThresholdPx = with(density) { 60.dp.toPx() }
  val isScrolled by remember {
    derivedStateOf {
      headerCollapsePx > 4f || (pageListStates[pagerState.currentPage]?.canScrollBackward == true)
    }
  }
  val showCollapsedTitle by remember {
    derivedStateOf { headerCollapsePx > collapseTitleThresholdPx }
  }

  LaunchedEffect(pagerState.settledPage) {
    val settledListState = pageListStates[pagerState.settledPage]
    val canScroll = settledListState != null &&
      (settledListState.canScrollForward || settledListState.canScrollBackward)
    if (!canScroll && headerCollapsePx > 0f) {
      animate(
        initialValue = headerCollapsePx,
        targetValue = 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
      ) { value, _ ->
        headerCollapsePx = value
      }
    }
  }

  MiuixPageHost(
    modifier = modifier.fillMaxSize(),
    enableMenuBackdrop = true,
    overlay = { pageState ->
      AdaptiveGlassOverflowMenu(
        show = showComponentMenu,
        onDismissRequest = { showComponentMenu = false },
        anchor = componentMenuAnchor,
        backdrop = pageState.menuBackdrop,
        entries = menuEntries,
        collapseOnSelection = false
      ) {
        Icon(
          imageVector = MiuixIcons.Glass.Menu,
          contentDescription = stringResource(R.string.advanced_menu)
        )
      }
    }
  ) {
    val pageState = this
    Box(modifier = Modifier.fillMaxSize()) {
      Scaffold(
        modifier = Modifier
          .fillMaxSize()
          .then(pageState.menuBackdropModifier),
      containerColor = colorScheme.surface,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      topBar = {
        AdaptiveTopAppBar(
          title = if (showCollapsedTitle) appLabel else "",
          subtitle = "",
          pinned = true,
          scrollBehavior = scrollBehavior,
          isContentScrolled = isScrolled,
          search = TopBarSearchConfig(
            state = searchState,
            label = stringResource(R.string.search_hint)
          ),
          navigationIcon = {
            AdaptiveBackButton(onClick = onBack)
          },
          actions = {
            AdaptiveTopSearchButton(onClick = searchState::open)
            if (onCompareWithInstalled != null) {
              AdaptiveTopBarIconButton(onClick = onCompareWithInstalled) {
                Icon(
                  imageVector = MiuixIcons.Glass.HorizontalSplit,
                  contentDescription = stringResource(R.string.lib_detail_compare_with_installed)
                )
              }
            }
            if (isHarmonyVariant) {
              AdaptiveTopBarIconButton(onClick = { isHarmonyMode = !isHarmonyMode }) {
                AsyncImage(
                  model = R.drawable.ic_harmonyos_logo,
                  contentDescription = stringResource(R.string.ability),
                  modifier = Modifier
                    .size(20.dp)
                    .alpha(if (isHarmonyMode) 1f else 0.4f)
                )
              }
            }
            if (onlineStatisticRulesAvailable) {
              AdaptiveTopBarIconButton(onClick = { showOnlineAnalysis = true }) {
                Icon(
                  imageVector = MiuixIcons.Glass.CloudFill,
                  contentDescription = stringResource(R.string.app_detail_online_rules_action)
                )
              }
            }
            AdaptiveTopBarMenuAction(
              entries = menuEntries,
              menuAnchor = componentMenuAnchor,
              show = showComponentMenu,
              onShowChange = { showComponentMenu = it },
              isContentScrolled = isScrolled,
              collapseOnSelection = false
            ) {
              Icon(
                imageVector = MiuixIcons.Glass.Menu,
                contentDescription = stringResource(R.string.advanced_menu)
              )
            }
          }
        )
      }
    ) { padding ->
      Box(
        Modifier
          .fillMaxSize()
          .then(pageState.topBarBackdropModifier)
          .imePadding()
      ) {
        WideContentBox { sidePadding ->
          val componentTabs = remember(tabSpec.items) { tabSpec.items.map { it.title.toString() } }
          val componentTabMinWidth = rememberFittingTabMinWidth(componentTabs)
          val safeComponentTabIndex = selectedTab.coerceIn(0, (componentTabs.size - 1).coerceAtLeast(0))
          val nativeTabList = remember(selectedType, nativeTabs) {
            if (selectedType == NATIVE && (nativeTabs?.size ?: 0) > 1) nativeTabs.orEmpty().toList() else emptyList()
          }
          val nativeTabMinWidth = rememberFittingTabMinWidth(nativeTabList)
          val safeNativeTabIndex = nativeTabIndex.coerceIn(0, (nativeTabList.size - 1).coerceAtLeast(0))
          val hasFeatures = featureItems.isNotEmpty()
          val topBarInsetPx = with(density) { padding.calculateTopPadding().toPx() }
          val headerNestedScrollConnection = remember(maxCollapsePx, pagerState, pageListStates) {
            object : NestedScrollConnection {
              override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val deltaY = available.y
                val currentListState = pageListStates[pagerState.currentPage]
                val canCurrentPageScroll = currentListState != null &&
                  (currentListState.canScrollForward || currentListState.canScrollBackward)
                if (deltaY < 0f && canCurrentPageScroll && maxCollapsePx > 0f && headerCollapsePx < maxCollapsePx) {
                  val next = (headerCollapsePx - deltaY).coerceIn(0f, maxCollapsePx)
                  val consumed = headerCollapsePx - next
                  headerCollapsePx = next
                  return Offset(0f, consumed)
                }
                if (deltaY > 0f && headerCollapsePx > 0f && currentListState?.canScrollBackward != true) {
                  val next = (headerCollapsePx - deltaY).coerceIn(0f, maxCollapsePx)
                  val consumed = headerCollapsePx - next
                  headerCollapsePx = next
                  return Offset(0f, consumed)
                }
                return Offset.Zero
              }

              override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
              ): Offset {
                val deltaY = available.y
                if (deltaY > 0f && headerCollapsePx > 0f) {
                  val next = (headerCollapsePx - deltaY).coerceIn(0f, maxCollapsePx)
                  val consumedY = headerCollapsePx - next
                  headerCollapsePx = next
                  return Offset(0f, consumedY)
                }
                return Offset.Zero
              }

              override suspend fun onPreFling(available: Velocity): Velocity {
                val currentListState = pageListStates[pagerState.currentPage]
                if (available.y > 300f && headerCollapsePx > 0f && currentListState?.canScrollBackward != true) {
                  animate(
                    initialValue = headerCollapsePx,
                    targetValue = 0f,
                    initialVelocity = -available.y,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                  ) { value, _ ->
                    headerCollapsePx = value
                  }
                  return Velocity(0f, available.y)
                }
                return Velocity.Zero
              }
            }
          }
          val headerDragState = rememberDraggableState { deltaY ->
            val currentListState = pageListStates[pagerState.currentPage]
            val canCurrentPageScroll = currentListState != null &&
              (currentListState.canScrollForward || currentListState.canScrollBackward)
            if (deltaY < 0f) {
              if (canCurrentPageScroll) {
                val next = (headerCollapsePx - deltaY).coerceIn(0f, maxCollapsePx)
                val consumed = headerCollapsePx - next
                headerCollapsePx = next
                val remaining = deltaY - consumed
                if (remaining < -0.5f) {
                  currentListState.dispatchRawDelta(-remaining)
                }
              }
            } else if (deltaY > 0f) {
              val remaining = if (currentListState != null && currentListState.canScrollBackward) {
                val scrolledDrag = -currentListState.dispatchRawDelta(-deltaY)
                deltaY - scrolledDrag
              } else {
                deltaY
              }
              if (remaining > 0.5f) {
                val next = (headerCollapsePx - remaining).coerceIn(0f, maxCollapsePx)
                headerCollapsePx = next
              }
            }
          }
          val pagerFlingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            snapAnimationSpec = PagerNavigationSpringSpec
          )

          Column(
            modifier = Modifier
              .fillMaxSize()
              .horizontalCutoutPadding()
              .padding(horizontal = sidePadding)
              .nestedScroll(headerNestedScrollConnection)
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .draggable(
                  orientation = Orientation.Vertical,
                  state = headerDragState
                )
                .layout { measurable, constraints ->
                  val placeable = measurable.measure(
                    constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                  )
                  val contentHeight = placeable.height.toFloat()
                  if (collapsibleHeightPx != contentHeight) {
                    collapsibleHeightPx = contentHeight
                  }
                  val maxCollapse = (contentHeight + headerTopGapPx).coerceAtLeast(0f)
                  val clampedCollapse = headerCollapsePx.coerceIn(0f, maxCollapse)
                  if (headerCollapsePx != clampedCollapse) {
                    headerCollapsePx = clampedCollapse
                  }
                  val visibleHeight = (topBarInsetPx + headerTopGapPx + contentHeight - clampedCollapse)
                    .roundToInt()
                    .coerceAtLeast(topBarInsetPx.roundToInt())
                  layout(constraints.maxWidth, visibleHeight) {
                    val childY = (topBarInsetPx + headerTopGapPx - clampedCollapse).roundToInt()
                    placeable.placeRelative(0, childY)
                  }
                }
            ) {
              val collapseFadeAlpha = if (maxCollapsePx > 0f) {
                (1f - (headerCollapsePx / maxCollapsePx) * 1.15f).coerceIn(0f, 1f)
              } else {
                1f
              }
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .graphicsLayer { alpha = collapseFadeAlpha }
              ) {
                DetailHeader(
                  packageInfo = packageInfo,
                  is64Bit = is64Bit,
                  isNoLibs = abiBundle?.abi == Constants.OVERLAY,
                  abiLabelData = abiLabelData,
                  extraInfo = headerExtraInfo,
                  showIconLayers = showIconLayers,
                  onDismissIconLayers = { showIconLayers = false },
                  onIconClick = { showAppInfo = true },
                  onIconLongClick = { showIconLayers = true }
                )
                AnimatedVisibility(
                  visible = hasFeatures || isFeatureLoading,
                  enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                  exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                ) {
                  LazyRow(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(bottom = 12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    itemsIndexed(
                      items = featureItems,
                      key = { index, item -> "${item.titleRes}-$index" }
                    ) { _, item ->
                      FeatureChip(
                        item = item,
                        appIcons = featureAppIcons,
                        packageInfo = packageInfo,
                        modifier = Modifier.animateItem(),
                        onClick = {
                          when (item.action) {
                            AppDetailFeatureAction.InstallSource -> showInstallSource = true
                            AppDetailFeatureAction.XposedModule -> showXposedInfo = true
                            else -> featureDetail = item
                          }
                        }
                      )
                    }
                    if (isFeatureLoading) {
                      item(key = "feature_loading") {
                        FeatureLoadingDotsIndicator(
                          modifier = Modifier.animateItem()
                        )
                      }
                    }
                  }
                }
              }
            }

            DetailPinnedTabBar(
              componentTabs = componentTabs,
              selectedTab = safeComponentTabIndex,
              componentTabMinWidth = componentTabMinWidth,
              onSelectTab = { index -> detailPagerState.animateToPage(index) },
              nativeTabs = nativeTabList,
              selectedNativeTab = safeNativeTabIndex,
              nativeTabMinWidth = nativeTabMinWidth,
              onSelectNativeTab = { index ->
                nativeTabIndex = index
                nativeTabList.getOrNull(index)?.let(viewModel::loadSoAnalysisData)
              },
              modifier = Modifier
                .background(colorScheme.surface)
                .draggable(
                  orientation = Orientation.Vertical,
                  state = headerDragState
                )
            )

            HorizontalPager(
              state = pagerState,
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
                .pagerGestureOverride(
                  pagerState = pagerState,
                  flingBehavior = pagerFlingBehavior,
                  mode = PagerInterceptionMode.CrossAxis,
                  enabled = pages.size > 1
                ),
              flingBehavior = pagerFlingBehavior,
              beyondViewportPageCount = 1,
              userScrollEnabled = false,
              overscrollEffect = null,
              pageNestedScrollConnection = PagerGestureNestedScrollConnection,
              key = { page -> "${isHarmonyMode}_${pages[page].type}" }
            ) { page ->
              val pageType = pages[page].type
              val pageListState = remember(isHarmonyMode, pageType, page) {
                if (page == 0 && !isHarmonyMode) listState else LazyListState()
              }
              DisposableEffect(page, pageListState) {
                pageListStates[page] = pageListState
                onDispose {
                  if (pageListStates[page] === pageListState) {
                    pageListStates.remove(page)
                  }
                }
              }
              val harmonyItems = if (isHarmonyMode) {
                remember(pageType) { contentState.abilitiesMap.get(pageType) }
                  ?.collectAsStateWithLifecycle()?.value
              } else {
                null
              }
              val pageRawItems = if (isHarmonyMode) {
                harmonyItems
              } else when (pageType) {
                NATIVE -> nativeItems
                SERVICE -> serviceItems
                ACTIVITY -> activityItems
                RECEIVER -> receiverItems
                PROVIDER -> providerItems
                PERMISSION -> permissionItems
                METADATA -> metaItems
                SIGNATURES -> signatureItems
                STATIC -> staticItems
                else -> null
              }
              DetailTabPage(
                viewModel = viewModel,
                pageType = pageType,
                isHarmonyMode = isHarmonyMode,
                rawItems = pageRawItems,
                query = "",
                selectedProcess = selectedProcesses[pageType],
                onSelectProcess = { selectedProcesses[pageType] = it },
                sortBySize = sortBySize,
                onToggleSort = {
                  sortBySize = !sortBySize
                  GlobalValues.libSortMode = if (sortBySize) {
                    MODE_SORT_BY_SIZE
                  } else {
                    MODE_SORT_BY_LIB
                  }
                },
                listState = pageListState,
                scrollBehavior = scrollBehavior,
                onOpenPermissionDetail = { name ->
                  scope.launch {
                    permissionDetail = runCatching {
                      viewModel.getPermissionDetail(name)
                    }.getOrNull()
                  }
                },
                onOpenSignatureDetail = { name ->
                  signatureDetail = viewModel.buildSignatureDetailItems(name)
                },
                onOpenLibDetail = { chip ->
                  libDetailRequest = LibDetailRequest(
                    name = chip.item.name,
                    type = pageType,
                    isValid = chip.rule != null,
                    regexName = chip.rule?.regexName
                  )
                },
                onLongPressChip = { longPressChip = it }
              )
            }
          }
        }
      }
    }

      val activePageType = pages.getOrNull(pagerState.currentPage)?.type ?: NATIVE
      val activeHarmonyItems = if (isHarmonyMode) {
        remember(activePageType) { contentState.abilitiesMap.get(activePageType) }
          ?.collectAsStateWithLifecycle()?.value
      } else {
        null
      }
      val activeRawItems = if (isHarmonyMode) {
        activeHarmonyItems
      } else {
        when (activePageType) {
          NATIVE -> nativeItems
          SERVICE -> serviceItems
          ACTIVITY -> activityItems
          RECEIVER -> receiverItems
          PROVIDER -> providerItems
          PERMISSION -> permissionItems
          METADATA -> metaItems
          SIGNATURES -> signatureItems
          STATIC -> staticItems
          else -> null
        }
      }
      AdaptiveSearchOverlay(
        config = TopBarSearchConfig(
          state = searchState,
          label = stringResource(R.string.search_hint)
        )
      ) { activeQuery ->
        val searchListState = rememberLazyListState()
        val isActiveComponent = !isHarmonyMode && (
          activePageType == SERVICE ||
            activePageType == ACTIVITY ||
            activePageType == RECEIVER ||
            activePageType == PROVIDER
          )
        val searchItems = remember(activeRawItems, activeQuery, sortBySize, activePageType) {
          val filtered = activeRawItems.orEmpty().filter { chip ->
            chip.item.name.contains(activeQuery, ignoreCase = true) ||
              chip.rule?.label?.contains(activeQuery, ignoreCase = true) == true ||
              chip.labels.any { it.contains(activeQuery, ignoreCase = true) }
          }
          if (sortBySize) {
            filtered.sortedByDescending { it.item.size }
          } else if (activePageType == NATIVE) {
            filtered.sortedWith(
              compareByDescending<LibStringItemChip> { it.rule != null }
                .thenByDescending { it.item.size }
                .thenBy { it.item.name.lowercase() }
            )
          } else {
            filtered.sortedWith(
              compareByDescending<LibStringItemChip> { it.rule != null }
                .thenBy { it.item.name.lowercase() }
            )
          }
        }
        val permissionNotGrantedLabel = stringResource(R.string.permission_not_granted)
        val permissionNotGrantedColor = colorScheme.error.toArgb()
        val processColors = remember(activePageType, activeRawItems, isHarmonyMode, permissionNotGrantedLabel, permissionNotGrantedColor) {
          if (isHarmonyMode) {
            emptyMap()
          } else {
            viewModel.buildProcessFilterData(
              type = activePageType,
              permissionNotGrantedLabel = permissionNotGrantedLabel,
              permissionNotGrantedColor = permissionNotGrantedColor
            ).processMap
          }
        }
        LaunchedEffect(activeQuery, activePageType) {
          searchListState.scrollToItem(0)
        }
        if (activeRawItems == null) {
          WideContentBox { sidePadding ->
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(top = 6.dp, start = sidePadding, end = sidePadding)
            ) {
              DetailTabListSkeleton(rowCount = 8)
            }
          }
        } else if (searchItems.isEmpty()) {
          LcEmptyState(title = stringResource(R.string.empty_list))
        } else {
          LazyColumn(
            state = searchListState,
            modifier = Modifier
              .fillMaxSize()
              .horizontalCutoutPadding()
              .adaptiveVerticalScroll(),
            contentPadding = PaddingValues(top = 6.dp)
          ) {
            itemsIndexed(
              items = searchItems,
              key = { index, chip -> "${chip.item.name}:${chip.item.source.orEmpty()}:$index" }
            ) { index, chip ->
              val indicatorColor = when {
                activePageType == PERMISSION && chip.item.size == 0L -> colorScheme.error
                isActiveComponent && GlobalValues.processMode -> {
                  chip.item.process?.let { processColors[it] }?.let(::Color)
                }
                else -> null
              }
              DetailItemRow(
                chip = chip,
                highlight = activeQuery,
                indicatorColor = indicatorColor,
                isFirst = index == 0,
                isLast = index == searchItems.lastIndex,
                onClick = {
                  if (chip.item.name.isNotBlank()) {
                    when (activePageType) {
                      PERMISSION -> scope.launch {
                        permissionDetail = runCatching {
                          viewModel.getPermissionDetail(chip.item.name)
                        }.getOrNull()
                      }

                      SIGNATURES -> signatureDetail = viewModel.buildSignatureDetailItems(
                        chip.item.source ?: chip.item.name
                      )

                      else -> libDetailRequest = LibDetailRequest(
                        name = chip.item.name,
                        type = activePageType,
                        isValid = chip.rule != null,
                        regexName = chip.rule?.regexName
                      )
                    }
                  }
                },
                onLongClick = { longPressChip = chip }
              )
            }
            item {
              Spacer(
                Modifier
                  .height(24.dp)
                  .navigationBarsPadding()
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DetailTabPage(
  viewModel: DetailViewModel,
  pageType: Int,
  isHarmonyMode: Boolean,
  rawItems: List<LibStringItemChip>?,
  query: String,
  selectedProcess: String?,
  onSelectProcess: (String?) -> Unit,
  sortBySize: Boolean,
  onToggleSort: () -> Unit,
  listState: LazyListState,
  scrollBehavior: top.yukonga.miuix.kmp.basic.ScrollBehavior,
  onOpenPermissionDetail: (String) -> Unit,
  onOpenSignatureDetail: (String) -> Unit,
  onOpenLibDetail: (LibStringItemChip) -> Unit,
  onLongPressChip: (LibStringItemChip) -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  val isComponentType = !isHarmonyMode &&
    (pageType == SERVICE || pageType == ACTIVITY || pageType == RECEIVER || pageType == PROVIDER)
  val supportsProcessFilter = isComponentType || (!isHarmonyMode && pageType == PERMISSION)
  val permissionNotGrantedLabel = stringResource(R.string.permission_not_granted)
  val permissionNotGrantedColor = colorScheme.error.toArgb()
  val processFilterData = remember(
    pageType,
    rawItems,
    isHarmonyMode,
    permissionNotGrantedLabel,
    permissionNotGrantedColor
  ) {
    if (isHarmonyMode) {
      null
    } else {
      viewModel.buildProcessFilterData(
        type = pageType,
        permissionNotGrantedLabel = permissionNotGrantedLabel,
        permissionNotGrantedColor = permissionNotGrantedColor
      )
    }
  }
  val processColors = processFilterData?.processMap.orEmpty()
  val isLoadingItems = rawItems == null
  val filteredItems = remember(rawItems, query, selectedProcess, supportsProcessFilter, pageType) {
    rawItems?.let { list ->
      list.filter { chip ->
        val processMatches = when {
          !supportsProcessFilter || selectedProcess == null -> true
          pageType == PERMISSION -> chip.item.size == 0L
          else -> chip.item.process == null || chip.item.process == selectedProcess
        }
        val queryMatches = query.isBlank() ||
          chip.item.name.contains(query, ignoreCase = true) ||
          chip.rule?.label?.contains(query, ignoreCase = true) == true ||
          chip.labels.any { it.contains(query, ignoreCase = true) }
        processMatches && queryMatches
      }
    } ?: emptyList()
  }
  val displayItems = remember(filteredItems, sortBySize, pageType) {
    if (sortBySize) {
      filteredItems.sortedByDescending { it.item.size }
    } else if (pageType == NATIVE) {
      filteredItems.sortedWith(
        compareByDescending<LibStringItemChip> { it.rule != null }
          .thenByDescending { it.item.size }
          .thenBy { it.item.name.lowercase() }
      )
    } else {
      filteredItems.sortedWith(
        compareByDescending<LibStringItemChip> { it.rule != null }
          .thenBy { it.item.name.lowercase() }
      )
    }
  }
  val itemPlacementSpec = remember {
    spring<IntOffset>(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMediumLow,
      visibilityThreshold = IntOffset.VisibilityThreshold
    )
  }

  LazyColumn(
    state = listState,
    modifier = Modifier
      .fillMaxSize()
      .adaptiveVerticalScroll(scrollBehavior),
    contentPadding = PaddingValues(top = 4.dp)
  ) {
    item(key = "count") {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "${stringResource(R.string.items_count)}${displayItems.size}",
          style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
          color = colorScheme.onSurfaceVariantSummary
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onToggleSort) {
          Icon(
            imageVector = MiuixIcons.Glass.Sort,
            contentDescription = stringResource(R.string.menu_sort),
            tint = if (sortBySize) colorScheme.primary else colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
    if (
      supportsProcessFilter &&
      processColors.isNotEmpty() &&
      (pageType == PERMISSION || GlobalValues.processMode || processColors.size > 1)
    ) {
      item(key = "processes") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ProcessChip(
            label = stringResource(R.string.process_all),
            color = 0,
            selected = selectedProcess == null,
            onClick = { onSelectProcess(null) }
          )
          processColors.forEach { (process, color) ->
            ProcessChip(
              label = process,
              color = color,
              selected = selectedProcess == process,
              onClick = { onSelectProcess(process) }
            )
          }
        }
      }
    }
    when {
      isLoadingItems -> item(key = "loading") {
        DetailTabListSkeleton(
          modifier = Modifier.animateItem(
            fadeInSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            placementSpec = itemPlacementSpec,
            fadeOutSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
          )
        )
      }

      displayItems.isEmpty() -> item(key = "empty") {
        Box(
          Modifier
            .fillMaxWidth()
            .height(260.dp)
            .animateItem(
              fadeInSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
              placementSpec = itemPlacementSpec,
              fadeOutSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
            )
        ) {
          LcEmptyState(title = stringResource(R.string.empty_list))
        }
      }

      else -> itemsIndexed(
        items = displayItems,
        key = { index, chip -> "${chip.item.name}:${chip.item.source.orEmpty()}:$index" }
      ) { index, chip ->
        val indicatorColor = when {
          pageType == PERMISSION && chip.item.size == 0L -> colorScheme.error
          isComponentType && GlobalValues.processMode -> {
            chip.item.process?.let { processColors[it] }?.let(::Color)
          }
          else -> null
        }
        DetailItemRow(
          chip = chip,
          highlight = query,
          indicatorColor = indicatorColor,
          isFirst = index == 0,
          isLast = index == displayItems.lastIndex,
          modifier = Modifier.animateItem(
            fadeInSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            placementSpec = itemPlacementSpec,
            fadeOutSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
          ),
          onClick = {
            if (chip.item.name.isNotBlank()) {
              when (pageType) {
                PERMISSION -> onOpenPermissionDetail(chip.item.name)
                SIGNATURES -> onOpenSignatureDetail(chip.item.source ?: chip.item.name)
                else -> onOpenLibDetail(chip)
              }
            }
          },
          onLongClick = { onLongPressChip(chip) }
        )
      }
    }
    item {
      Spacer(
        Modifier
          .height(24.dp)
          .navigationBarsPadding()
      )
    }
  }
}


@Composable
private fun DetailHeader(
  packageInfo: PackageInfo,
  is64Bit: Boolean?,
  isNoLibs: Boolean,
  abiLabelData: AppDetailAbiLabelData?,
  extraInfo: AppDetailHeaderExtraInfo?,
  showIconLayers: Boolean,
  onDismissIconLayers: () -> Unit,
  onIconClick: () -> Unit,
  onIconLongClick: (() -> Unit)? = null
) {
  val context = LocalContext.current
  val colorScheme = MiuixTheme.colorScheme
  val appInfo = packageInfo.applicationInfo
  val label = appInfo?.loadLabel(context.packageManager)?.toString() ?: packageInfo.packageName
  val icon = remember(packageInfo.packageName) {
    runCatching { appInfo?.loadIcon(context.packageManager) }.getOrNull()
  }
  val adaptive = icon as? android.graphics.drawable.AdaptiveIconDrawable
  val fullLabel = stringResource(R.string.adaptive_icon_copy_full)
  val bgLabel = stringResource(R.string.adaptive_icon_copy_background)
  val fgLabel = stringResource(R.string.adaptive_icon_copy_foreground)
  val themedLabel = stringResource(R.string.dialog_themed)

  fun copyLayer(drawable: android.graphics.drawable.Drawable?) {
    val bitmap = runCatching {
      drawable?.toBitmap(192, 192)
    }.getOrNull() ?: return
    context.copyBitmapToClipboard(bitmap)
    onDismissIconLayers()
  }

  val iconLayerActions = buildList<Pair<String, () -> Unit>> {
    add(fullLabel to { copyLayer(icon) })
    if (adaptive != null) {
      add(bgLabel to { copyLayer(adaptive.background) })
      add(fgLabel to { copyLayer(adaptive.foreground) })
      if (
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
        adaptive.monochrome != null
      ) {
        add(themedLabel to { copyLayer(adaptive.monochrome) })
      }
    }
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .padding(bottom = 12.dp),
    insideMargin = PaddingValues(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(modifier = Modifier.padding(end = 16.dp)) {
        AppIcon(
          packageInfo = packageInfo,
          label = label,
          modifier = Modifier
            .size(56.dp)
            .squircleClip(12.dp)
            .combinedClickable(onClick = onIconClick, onLongClick = onIconLongClick)
        )
        OverlayListPopup(
          show = showIconLayers,
          alignment = PopupPositionProvider.Align.Start,
          onDismissRequest = onDismissIconLayers
        ) {
          val dismissState = LocalDismissState.current
          ListPopupColumn {
            iconLayerActions.forEachIndexed { index, (actionLabel, onSelect) ->
              DropdownImpl(
                text = actionLabel,
                optionSize = iconLayerActions.size,
                isSelected = false,
                index = index,
                onSelectedIndexChange = {
                  onSelect()
                  dismissState?.invoke()
                }
              )
            }
          }
        }
      }
      Column(Modifier.weight(1f)) {
        Text(
          text = label,
          style = MiuixTheme.textStyles.title4.copy(
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
          ),
          color = colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = packageInfo.packageName,
          style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
          color = colorScheme.onSurfaceVariantSummary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        val condensedFont = lcCondensedFontFamily()
        Text(
          text = "${packageInfo.versionName} (${PackageInfoCompat.getLongVersionCode(packageInfo)})",
          style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp).withCustomFont(condensedFont),
          color = colorScheme.onSurfaceVariantSummary,
          maxLines = 1
        )
      }
    }
    Spacer(Modifier.height(8.dp))
    val condensedFont = lcCondensedFontFamily()
    val fallbackSize = remember(packageInfo.packageName) {
      appInfo?.sourceDir?.takeIf(String::isNotBlank)?.let { dir ->
        android.text.format.Formatter.formatFileSize(context, java.io.File(dir).length())
      }
    }
    val infoLineText = if (condensedFont != null) {
      val labelSpan = SpanStyle(
        fontWeight = FontWeight.Medium,
        color = colorScheme.onSurface.copy(alpha = 0.85f)
      )
      buildAnnotatedString {
        if (extraInfo != null) {
          withStyle(labelSpan) { append("Target: ") }
          append(extraInfo.targetSdkInfo)
          if (extraInfo.minSdkInfo.isNotEmpty()) {
            append("  ")
            withStyle(labelSpan) { append("Min: ") }
            append(extraInfo.minSdkInfo)
          }
          if (extraInfo.compileSdkInfo.isNotEmpty()) {
            append("  ")
            withStyle(labelSpan) { append("Compile: ") }
            append(extraInfo.compileSdkInfo)
          }
          if (extraInfo.sizeInfo.isNotEmpty()) {
            append("  ")
            withStyle(labelSpan) { append("Size: ") }
            append(extraInfo.sizeInfo)
          }
        } else {
          withStyle(labelSpan) { append("Target: ") }
          append(appInfo?.targetSdkVersion?.toString().orEmpty())
          fallbackSize?.let {
            append("  ")
            withStyle(labelSpan) { append("Size: ") }
            append(it)
          }
        }
      }
    } else {
      val plain = when {
        extraInfo != null -> buildString {
          append("Target: ${extraInfo.targetSdkInfo}")
          if (extraInfo.minSdkInfo.isNotEmpty()) append("  Min: ${extraInfo.minSdkInfo}")
          if (extraInfo.compileSdkInfo.isNotEmpty()) append("  Compile: ${extraInfo.compileSdkInfo}")
          if (extraInfo.sizeInfo.isNotEmpty()) append("  Size: ${extraInfo.sizeInfo}")
        }

        else -> buildString {
          append("Target: ${appInfo?.targetSdkVersion}")
          fallbackSize?.let { append("  Size: $it") }
        }
      }
      AnnotatedString(plain)
    }
    Text(
      text = infoLineText,
      style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp).withCustomFont(condensedFont),
      color = colorScheme.onSurfaceVariantSummary.copy(alpha = 0.85f),
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .padding(start = 70.dp)
        .combinedClickable(
          onClick = {},
          onLongClick = { context.copyTextWithToast(infoLineText.text) }
        )
    )
    extraInfo?.sharedUserId?.let { sharedUserId ->
      Spacer(Modifier.height(2.dp))
      Text(
        text = "sharedUserId: $sharedUserId",
        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp).withCustomFont(condensedFont),
        color = colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(start = 70.dp)
      )
    }
    val abiLabels = abiLabelData?.labels.orEmpty()
    val fallbackAbi = appInfo?.nativeLibraryDir
      ?.substringAfterLast('/')
      ?.takeIf(String::isNotBlank)
      ?: android.os.Build.SUPPORTED_ABIS.firstOrNull()
    if (abiLabels.isNotEmpty() || isNoLibs || fallbackAbi != null) {
      Spacer(Modifier.height(6.dp))
      FlowRow(
        modifier = Modifier.padding(start = 70.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        // the original header wrapped its abi labels instead of clipping them
        when {
          abiLabels.isNotEmpty() -> {
            abiLabels.forEach { itemLabel ->
              val badgeRes = if (itemLabel.opensMultiArchInfo) {
                R.drawable.ic_abi_label_multi_arch
              } else {
                PackageInfoFormatter.getLargeAbiBadgeResource(itemLabel.abi)
              }
              if (badgeRes != 0) {
                Icon(
                  painter = painterResource(badgeRes),
                  contentDescription = itemLabel.contentDescription,
                  tint = colorScheme.onSurface.copy(alpha = if (itemLabel.isActive) 1f else 0.4f),
                  modifier = Modifier.size(width = 39.dp, height = 26.dp)
                )
              } else {
                AbiBadge(
                  text = PackageInfoFormatter.getAbiString(
                    context = context,
                    abi = itemLabel.abi,
                    showExtraInfo = false
                  ).uppercase()
                )
              }
            }
          }

          isNoLibs -> {
            Icon(
              painter = painterResource(R.drawable.ic_abi_label_no_libs),
              contentDescription = Constants.OVERLAY_STRING,
              tint = colorScheme.onSurface,
              modifier = Modifier.size(width = 39.dp, height = 26.dp)
            )
          }

          fallbackAbi != null -> {
            if (is64Bit != null) {
              AbiBadge(text = if (is64Bit == true) "64 BIT" else "32 BIT")
            }
            AbiBadge(text = fallbackAbi.uppercase())
          }
        }
      }
    }
  }
}

@Composable
private fun AbiBadge(text: String) {
  com.absinthe.libchecker.ui.compose.component.LcTag(
    text = text,
    containerColor = MiuixTheme.colorScheme.onSurface,
    contentColor = MiuixTheme.colorScheme.surface,
    horizontalPadding = 4,
    verticalPadding = 2,
    cornerRadius = 4,
    textStyle = MiuixTheme.textStyles.footnote2.copy(fontSize = 9.sp)
      .withCustomFont(lcCondensedFontFamily())
  )
}

@Composable
private fun DetailPinnedTabBar(
  componentTabs: List<String>,
  selectedTab: Int,
  componentTabMinWidth: Dp,
  onSelectTab: (Int) -> Unit,
  nativeTabs: List<String>,
  selectedNativeTab: Int,
  nativeTabMinWidth: Dp,
  onSelectNativeTab: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth()) {
    AdaptiveTabRow(
      tabs = componentTabs,
      selectedTabIndex = selectedTab,
      onTabSelected = onSelectTab,
      backdrop = null,
      minWidth = componentTabMinWidth,
      maxWidth = maxOf(componentTabMinWidth, 160.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp)
        .padding(bottom = 8.dp)
    )
    var cachedNativeTabs by remember { mutableStateOf(nativeTabs) }
    if (nativeTabs.isNotEmpty()) {
      cachedNativeTabs = nativeTabs
    }
    AnimatedVisibility(
      visible = nativeTabs.isNotEmpty(),
      enter = fadeIn(tween(180)) + expandVertically(tween(200), expandFrom = Alignment.Top),
      exit = fadeOut(tween(150)) + shrinkVertically(tween(180), shrinkTowards = Alignment.Top)
    ) {
      if (cachedNativeTabs.isNotEmpty()) {
        AdaptiveTabRow(
          tabs = cachedNativeTabs,
          selectedTabIndex = selectedNativeTab.coerceIn(0, (cachedNativeTabs.size - 1).coerceAtLeast(0)),
          onTabSelected = onSelectNativeTab,
          backdrop = null,
          minWidth = nativeTabMinWidth,
          maxWidth = maxOf(nativeTabMinWidth, 160.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp)
        )
      }
    }
  }
}

@Composable
private fun DetailItemRow(
  chip: LibStringItemChip,
  highlight: String,
  indicatorColor: Color?,
  isFirst: Boolean,
  isLast: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colorScheme = MiuixTheme.colorScheme
  CardSegment(
    isFirst = isFirst,
    isLast = isLast,
    modifier = modifier
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .heightIn(min = 48.dp)
        .height(IntrinsicSize.Min)
        .padding(
          start = if (indicatorColor != null) 6.dp else 16.dp,
          end = 16.dp,
          top = 10.dp,
          bottom = 10.dp
        ),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (indicatorColor != null) {
        Box(
          modifier = Modifier
            .width(3.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(99.dp))
            .background(indicatorColor)
        )
        Spacer(Modifier.width(7.dp))
      }
      Column(Modifier.weight(1f)) {
        Text(
          text = highlightText(chip.item.name, highlight),
          style = MiuixTheme.textStyles.body2.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
          ),
          color = colorScheme.onSurface,
          textDecoration = if (chip.item.source == DISABLED) TextDecoration.LineThrough else null,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        val sizeText = chip.nativeDisplayData?.sizeText?.takeIf(String::isNotBlank)
        val badgeLabels = (chip.nativeDisplayData?.labels ?: chip.labels)
          .asSequence()
          .filter(String::isNotBlank)
          .distinct()
          .toList()
        if (sizeText != null || badgeLabels.isNotEmpty()) {
          Spacer(Modifier.height(3.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            sizeText?.let { text ->
              Text(
                text = text,
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                color = colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.CenterVertically)
              )
            }
            badgeLabels.forEach { label ->
              com.absinthe.libchecker.ui.compose.component.LcTag(
                text = label,
                containerColor = colorScheme.secondaryContainer,
                contentColor = colorScheme.onSecondaryContainer,
                horizontalPadding = 6,
                verticalPadding = 1,
                cornerRadius = 5,
                textStyle = MiuixTheme.textStyles.footnote2.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Medium
                )
              )
            }
          }
        }
      }
      chip.rule?.let { rule ->
        RuleChip(iconRes = rule.iconRes, label = rule.label)
      }
    }
  }
}

@Composable
private fun RuleChip(iconRes: Int, label: String) {
  val colorScheme = MiuixTheme.colorScheme
  Row(
    modifier = Modifier
      .padding(start = 12.dp)
      .widthIn(max = 148.dp)
      .squircleBackground(colorScheme.tertiaryContainer, 10.dp)
      .padding(horizontal = 8.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    AsyncImage(
      model = iconRes,
      contentDescription = null,
      modifier = Modifier
        .padding(end = 4.dp)
        .size(16.dp)
    )
    Text(
      text = label,
      style = MiuixTheme.textStyles.footnote2.copy(fontSize = 11.sp),
      color = colorScheme.onTertiaryContainer,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun FeatureChip(
  item: AppDetailFeatureItemData,
  appIcons: List<AppIconItem>,
  packageInfo: PackageInfo,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colorScheme = MiuixTheme.colorScheme
  Box(
    modifier = modifier
      .size(40.dp)
      .clip(CircleShape)
      .squircleBackground(colorScheme.surfaceContainer, 20.dp)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    when (val icon = item.icon) {
      is AppDetailFeatureIcon.Resource -> AsyncImage(
        model = icon.res,
        contentDescription = stringResource(item.titleRes),
        modifier = Modifier.size(22.dp),
        colorFilter = icon.tint?.let { ColorFilter.tint(Color(it)) }
      )

      AppDetailFeatureIcon.AppIcons -> {
        if (appIcons.isEmpty()) {
          AppIcon(
            packageInfo = packageInfo,
            label = packageInfo.packageName,
            modifier = Modifier
              .size(26.dp)
              .squircleClip(8.dp)
          )
        } else {
          var currentIndex by remember(appIcons) { mutableIntStateOf(0) }
          LaunchedEffect(appIcons) {
            currentIndex = 0
            if (appIcons.size > 1) {
              while (true) {
                delay(3000L)
                currentIndex = (currentIndex + 1) % appIcons.size
              }
            }
          }
          val displayIcon = appIcons.getOrElse(currentIndex) { appIcons.first() }
          AnimatedContent(
            targetState = displayIcon,
            transitionSpec = {
              (fadeIn(tween(260)) + scaleIn(initialScale = 0.88f, animationSpec = tween(260))) togetherWith
                (fadeOut(tween(200)) + scaleOut(targetScale = 0.88f, animationSpec = tween(200)))
            },
            label = "FeatureAppIconCycle"
          ) { iconItem ->
            AppIconItemImage(
              icon = iconItem,
              size = 26.dp,
              cornerRadius = 8.dp
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FeatureLoadingDotsIndicator(
  modifier: Modifier = Modifier
) {
  val colorScheme = MiuixTheme.colorScheme
  val infiniteTransition = rememberInfiniteTransition(label = "FeatureLoadingDots")
  val progress by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900, easing = LinearEasing)
    ),
    label = "FeatureLoadingProgress"
  )
  val dotColor = colorScheme.onSurfaceVariantSummary
  Canvas(
    modifier = modifier
      .height(40.dp)
      .width(36.dp)
  ) {
    val dotRadius = 2.5.dp.toPx()
    val dotSpacing = 8.dp.toPx()
    val travelY = 3.dp.toPx()
    val startX = (size.width - dotSpacing * 2f) / 2f
    val centerY = size.height / 2f + travelY / 2f

    repeat(3) { index ->
      val localProgress = (progress - index * 0.18f + 1f) % 1f
      val wave = ((sin(localProgress * PI * 2 - PI / 2) + 1f) / 2f).toFloat()
      val easedWave = FastOutSlowInEasing.transform(wave)
      val radius = dotRadius * (0.72f + 0.34f * easedWave)
      val alpha = (86 + 169 * easedWave).roundToInt().coerceIn(0, 255) / 255f
      drawCircle(
        color = dotColor.copy(alpha = alpha),
        radius = radius,
        center = Offset(
          x = startX + dotSpacing * index,
          y = centerY - travelY * easedWave
        )
      )
    }
  }
}

private data class LibDetailRequest(
  val name: String,
  val type: Int,
  val isValid: Boolean,
  val regexName: String? = null
)

@Composable
private fun SheetActionRow(text: String, onClick: () -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.defaultColors(
      color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.04f)
    ),
    onClick = onClick,
    showIndication = true,
    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
  ) {
    Text(
      text = text,
      style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp),
      color = MiuixTheme.colorScheme.onSurface
    )
  }
}

@Composable
private fun SheetInfoBlock(label: String, value: String) {
  Column {
    Text(
      text = label,
      style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
      color = MiuixTheme.colorScheme.onSurfaceVariantSummary
    )
    Spacer(Modifier.height(2.dp))
    Text(
      text = value,
      style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
      color = MiuixTheme.colorScheme.onSurface
    )
  }
}

@Composable
private fun ProcessChip(
  label: String,
  color: Int,
  selected: Boolean,
  onClick: () -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  Row(
    modifier = Modifier
      .squircleClip(14.dp)
      .squircleBackground(
        if (selected) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
        14.dp
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (color != 0) {
      Box(
        modifier = Modifier
          .padding(end = 6.dp)
          .size(8.dp)
          .squircleBackground(Color(color), 4.dp)
      )
    }
    Text(
      text = label,
      style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp)
        .withCustomFont(lcCondensedFontFamily()),
      color = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurface
    )
  }
}

@Composable
private fun highlightText(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
  if (query.isBlank()) {
    return androidx.compose.ui.text.AnnotatedString(text)
  }
  val accent = MiuixTheme.colorScheme.primary
  return androidx.compose.ui.text.buildAnnotatedString {
    var index = 0
    while (index < text.length) {
      val match = text.indexOf(query, index, ignoreCase = true)
      if (match < 0) {
        append(text.substring(index))
        break
      }
      append(text.substring(index, match))
      withStyle(androidx.compose.ui.text.SpanStyle(color = accent)) {
        append(text.substring(match, match + query.length))
      }
      index = match + query.length
    }
  }
}
