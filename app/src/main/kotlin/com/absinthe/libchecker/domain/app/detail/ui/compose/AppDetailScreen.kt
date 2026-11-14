package com.absinthe.libchecker.domain.app.detail.ui.compose

import android.content.pm.PackageInfo
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
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
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureAction
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureIcon
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureItemData
import com.absinthe.libchecker.domain.app.detail.model.DISABLED
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.ui.DetailTabSpecBuilder
import com.absinthe.libchecker.domain.app.list.ui.compose.AppIcon
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.LcEmptyState
import com.absinthe.libchecker.utils.extensions.launchLibReferencePage
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog

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
  listState: LazyListState = rememberLazyListState()
) {
  val context = LocalContext.current
  val focusManager = LocalFocusManager.current
  val activity = context as? FragmentActivity
  val scrollBehavior = MiuixScrollBehavior()
  val colorScheme = MiuixTheme.colorScheme
  val tabSpec = remember(packageInfo.packageName) {
    DetailTabSpecBuilder(context).build(isHarmonyMode = false, isApkPreview = false)
  }
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  val selectedType = tabSpec.itemAt(selectedTab)?.type ?: NATIVE
  val contentState = viewModel.contentState

  var query by rememberSaveable { mutableStateOf("") }
  var searchExpanded by rememberSaveable { mutableStateOf(false) }
  var sortBySize by rememberSaveable { mutableStateOf(false) }
  var nativeTabIndex by remember { mutableIntStateOf(0) }
  val featureItems = remember { mutableStateListOf<AppDetailFeatureItemData>() }
  var libDetailRequest by remember { mutableStateOf<LibDetailRequest?>(null) }
  var showAppInfo by remember { mutableStateOf(false) }
  var featureDetail by remember { mutableStateOf<AppDetailFeatureItemData?>(null) }
  var longPressChip by remember { mutableStateOf<LibStringItemChip?>(null) }
  val is64Bit by viewModel.featureState.is64Bit.collectAsStateWithLifecycle()

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

  LaunchedEffect(selectedType) {
    when (selectedType) {
      NATIVE -> viewModel.initSoAnalysisData()
      SERVICE, ACTIVITY, RECEIVER, PROVIDER -> viewModel.initComponentsData()
      PERMISSION -> viewModel.initPermissionData()
      METADATA -> viewModel.initMetaDataData()
      SIGNATURES -> viewModel.initSignatures()
      STATIC -> viewModel.initStaticData()
    }
  }

  LaunchedEffect(selectedType, nativeTabs) {
    if (selectedType == NATIVE) {
      nativeTabs?.firstOrNull()?.let(viewModel::loadSoAnalysisData)
    }
  }

  val rawItems: List<LibStringItemChip>? = when (selectedType) {
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

  val isLoadingItems = rawItems == null
  val filteredItems = remember(rawItems, query) {
    rawItems?.let { list ->
      if (query.isBlank()) {
        list
      } else {
        list.filter { chip ->
          chip.item.name.contains(query, ignoreCase = true) ||
            chip.rule?.label?.contains(query, ignoreCase = true) == true ||
            chip.labels.any { it.contains(query, ignoreCase = true) }
        }
      }
    } ?: emptyList()
  }
  val displayItems = remember(filteredItems, sortBySize) {
    if (sortBySize) {
      filteredItems.sortedByDescending { it.item.size }
    } else {
      filteredItems.sortedBy { it.item.name.lowercase() }
    }
  }

  FeatureDetailSheet(
    show = featureDetail != null,
    viewModel = viewModel,
    packageInfo = packageInfo,
    item = featureDetail,
    onDismiss = { featureDetail = null }
  )

  if (showAppInfo) {
    AppInfoSheet(
      show = true,
      viewModel = viewModel,
      packageInfo = packageInfo,
      onDismiss = { showAppInfo = false }
    )
  }

  longPressChip?.let { chip ->
    val actions = remember(chip) {
      viewModel.buildDetailItemLongClickActions(
        item = chip,
        packageName = packageInfo.packageName,
        detailType = selectedType,
        canReference = true
      )
    }
    WindowBottomSheet(
      show = true,
      title = chip.item.name,
      onDismissRequest = { longPressChip = null }
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .padding(bottom = 16.dp)
      ) {
        SheetActionRow(text = stringResource(android.R.string.copy)) {
          val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
          clipboard?.setPrimaryClip(
            android.content.ClipData.newPlainText("", actions.copyText)
          )
          longPressChip = null
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
      }
    }
  }
  libDetailRequest?.let { request ->
    LibDetailSheet(
      show = true,
      viewModel = viewModel,
      libName = request.name,
      type = request.type,
      regexName = null,
      isValidLib = request.isValid,
      onDismiss = { libDetailRequest = null }
    )
  }

  Column(modifier = modifier.fillMaxSize()) {
    AdaptiveTopAppBar(
      title = packageInfo.applicationInfo?.loadLabel(context.packageManager)?.toString()
        ?: packageInfo.packageName,
      subtitle = packageInfo.packageName,
      scrollBehavior = scrollBehavior,
      navigationIcon = {
        IconButton(onClick = onBack) {
          Icon(MiuixIcons.Back, contentDescription = null)
        }
      },
      bottomContent = {
        InputField(
          query = query,
          onQueryChange = { query = it },
          onSearch = { focusManager.clearFocus() },
          expanded = searchExpanded,
          onExpandedChange = { searchExpanded = it },
          label = stringResource(R.string.search_hint),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp)
        )
      }
    )

    ScrollableTabs(
      tabs = tabSpec.items.map { it.title.toString() },
      selectedIndex = selectedTab.coerceIn(0, (tabSpec.items.size - 1).coerceAtLeast(0)),
      onSelected = { selectedTab = it }
    )

    if (selectedType == NATIVE && (nativeTabs?.size ?: 0) > 1) {
      val tabs = nativeTabs.orEmpty().toList()
      ScrollableTabs(
        tabs = tabs,
        selectedIndex = nativeTabIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0)),
        onSelected = { index ->
          nativeTabIndex = index
          tabs.getOrNull(index)?.let(viewModel::loadSoAnalysisData)
        }
      )
    }

    Box(Modifier.weight(1f)) {
      WideContentBox { sidePadding ->
        LazyColumn(
          state = listState,
          modifier = Modifier
            .fillMaxSize()
            .horizontalCutoutPadding()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
          contentPadding = PaddingValues(
            top = 6.dp,
            start = sidePadding,
            end = sidePadding
          )
        ) {
          item(key = "header") {
            DetailHeader(
              packageInfo = packageInfo,
              is64Bit = is64Bit,
              onIconClick = { showAppInfo = true }
            )
          }
          if (featureItems.isNotEmpty()) {
            item(key = "features") {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp)
                  .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                featureItems.forEach { item ->
                  FeatureChip(
                    item = item,
                    packageInfo = packageInfo,
                    onClick = { featureDetail = item }
                  )
                }
              }
            }
          }
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
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariantSummary
              )
              Spacer(Modifier.weight(1f))
              IconButton(onClick = { sortBySize = !sortBySize }) {
                Icon(
                  imageVector = MiuixIcons.Tune,
                  contentDescription = stringResource(R.string.menu_sort),
                  tint = if (sortBySize) colorScheme.primary else colorScheme.onSurfaceVariantSummary,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
          when {
            isLoadingItems -> item(key = "loading") {
              Box(
                Modifier
                  .fillMaxWidth()
                  .height(240.dp)
              ) {
                LcLoadingState(message = stringResource(R.string.loading))
              }
            }

            displayItems.isEmpty() -> item(key = "empty") {
              Box(
                Modifier
                  .fillMaxWidth()
                  .height(260.dp)
              ) {
                LcEmptyState(title = stringResource(R.string.empty_list))
              }
            }

            else -> itemsIndexed(
              items = displayItems,
              key = { index, chip -> "${chip.item.name}:${chip.item.source.orEmpty()}:$index" }
            ) { index, chip ->
              DetailItemRow(
                chip = chip,
                showDivider = index < displayItems.lastIndex,
                onClick = {
                  if (chip.item.name.isNotBlank()) {
                    libDetailRequest = LibDetailRequest(
                      name = chip.item.name,
                      type = selectedType,
                      isValid = chip.rule != null
                    )
                  }
                },
                onLongClick = { longPressChip = chip }
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
    }
  }
}

@Composable
private fun ScrollableTabs(
  tabs: List<String>,
  selectedIndex: Int,
  onSelected: (Int) -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  LazyRow(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 4.dp),
    contentPadding = PaddingValues(horizontal = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    itemsIndexed(tabs) { index, title ->
      val selected = index == selectedIndex
      val contentColor by animateColorAsState(
        targetValue = if (selected) colorScheme.primary else colorScheme.onSurfaceVariantSummary,
        animationSpec = tween(180),
        label = "tab-color"
      )
      val indicatorColor by animateColorAsState(
        targetValue = if (selected) colorScheme.primary else Color.Transparent,
        animationSpec = tween(180),
        label = "tab-indicator"
      )
      Column(
        modifier = Modifier
          .clickable { onSelected(index) }
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = title,
          fontSize = 14.sp,
          fontWeight = if (selected) FontWeight(600) else FontWeight(500),
          color = contentColor,
          maxLines = 1
        )
        Spacer(Modifier.height(5.dp))
        Box(
          modifier = Modifier
            .width(16.dp)
            .height(3.dp)
            .squircleBackground(indicatorColor, 2.dp)
        )
      }
    }
  }
}

@Composable
private fun DetailHeader(
  packageInfo: PackageInfo,
  is64Bit: Boolean?,
  onIconClick: () -> Unit
) {
  val context = LocalContext.current
  val colorScheme = MiuixTheme.colorScheme
  val appInfo = packageInfo.applicationInfo
  val label = appInfo?.loadLabel(context.packageManager)?.toString() ?: packageInfo.packageName
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .padding(bottom = 12.dp),
    insideMargin = PaddingValues(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      AppIcon(
        packageInfo = packageInfo,
        label = label,
        modifier = Modifier
          .padding(end = 14.dp)
          .size(56.dp)
          .clickable(onClick = onIconClick)
      )
      Column(Modifier.weight(1f)) {
        Text(
          text = label,
          fontWeight = FontWeight(600),
          color = colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = packageInfo.packageName,
          fontSize = 12.sp,
          color = colorScheme.onSurfaceVariantSummary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = "${packageInfo.versionName} (${PackageInfoCompat.getLongVersionCode(packageInfo)})",
          fontSize = 12.sp,
          color = colorScheme.onSurfaceVariantSummary,
          maxLines = 1
        )
      }
    }
    Spacer(Modifier.height(8.dp))
    val sizeText = remember(packageInfo.packageName) {
      appInfo?.sourceDir?.takeIf(String::isNotBlank)?.let { dir ->
        android.text.format.Formatter.formatFileSize(context, java.io.File(dir).length())
      }
    }
    val target = appInfo?.targetSdkVersion
    val min = appInfo?.minSdkVersion
    val compile = if (android.os.Build.VERSION.SDK_INT >= 31) appInfo?.compileSdkVersion else null
    Text(
      text = buildString {
        append("Target: $target")
        if (min != null) append("  Min: $min")
        if (sizeText != null) append("  Size: $sizeText")
        if (compile != null) append("  Compile: $compile")
      },
      fontSize = 12.sp,
      color = colorScheme.onSurfaceVariantSummary
    )
    val abi = appInfo?.nativeLibraryDir
      ?.substringAfterLast('/')
      ?.takeIf(String::isNotBlank)
      ?: android.os.Build.SUPPORTED_ABIS.firstOrNull()
    if (abi != null) {
      Spacer(Modifier.height(6.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (is64Bit != null) {
          AbiBadge(text = if (is64Bit == true) "64 BIT" else "32 BIT")
        }
        AbiBadge(text = abi.uppercase())
      }
    }
  }
}

@Composable
private fun AbiBadge(text: String) {
  val colorScheme = MiuixTheme.colorScheme
  Box(
    modifier = Modifier
      .squircleBackground(colorScheme.onSurface, 4.dp)
      .padding(horizontal = 5.dp, vertical = 2.dp)
  ) {
    Text(
      text = text,
      fontSize = 9.sp,
      fontWeight = FontWeight(700),
      color = colorScheme.surface
    )
  }
}

@Composable
private fun DetailItemRow(
  chip: LibStringItemChip,
  showDivider: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  Column {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(colorScheme.surfaceContainer)
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(Modifier.weight(1f)) {
        Text(
          text = chip.item.name,
          fontSize = 14.sp,
          fontWeight = FontWeight(500),
          color = colorScheme.onSurface,
          textDecoration = if (chip.item.source == DISABLED) TextDecoration.LineThrough else null,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        val secondary = buildList {
          chip.nativeDisplayData?.sizeText?.takeIf(String::isNotBlank)?.let(::add)
          chip.labels.forEach { label ->
            if (label.isNotBlank() && size < 2) {
              add(label)
            }
          }
        }.joinToString(" · ")
        if (secondary.isNotEmpty()) {
          Spacer(Modifier.height(2.dp))
          Text(
            text = secondary,
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
      chip.rule?.let { rule ->
        RuleChip(iconRes = rule.iconRes, label = rule.label)
      }
    }
    if (showDivider) {
      Box(
        Modifier
          .fillMaxWidth()
          .padding(start = 16.dp)
          .height(0.5.dp)
          .background(colorScheme.dividerLine)
      )
    }
  }
}

@Composable
private fun RuleChip(iconRes: Int, label: String) {
  val colorScheme = MiuixTheme.colorScheme
  Row(
    modifier = Modifier
      .padding(start = 12.dp)
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
      fontSize = 11.sp,
      fontWeight = FontWeight(550),
      color = colorScheme.onTertiaryContainer,
      maxLines = 1
    )
  }
}

@Composable
private fun FeatureChip(
  item: AppDetailFeatureItemData,
  packageInfo: PackageInfo,
  onClick: () -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  Box(
    modifier = Modifier
      .size(40.dp)
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

      AppDetailFeatureIcon.AppIcons -> AppIcon(
        packageInfo = packageInfo,
        label = packageInfo.packageName,
        modifier = Modifier
          .size(26.dp)
          .squircleClip(8.dp)
      )
    }
  }
}

private data class LibDetailRequest(
  val name: String,
  val type: Int,
  val isValid: Boolean
)

@Composable
private fun SheetActionRow(text: String, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = text,
      fontSize = 14.sp,
      color = MiuixTheme.colorScheme.onSurface
    )
  }
}