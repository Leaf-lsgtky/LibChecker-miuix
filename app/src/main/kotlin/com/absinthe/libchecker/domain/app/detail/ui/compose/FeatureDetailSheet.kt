package com.absinthe.libchecker.domain.app.detail.ui.compose

import android.content.pm.PackageInfo
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import android.text.Html
import androidx.annotation.RequiresApi
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureAction
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureItemData
import com.absinthe.libchecker.domain.app.detail.feature.FeatureDialogTitleEntry
import com.absinthe.libchecker.domain.app.detail.feature.FeatureDialogTitleLabel
import com.absinthe.libchecker.domain.app.detail.feature.toDialogSpec
import com.absinthe.libchecker.domain.app.detail.model.AppIconItem
import com.absinthe.libchecker.domain.app.detail.model.AppPropItem
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.resource.AppResourcePreview
import com.absinthe.libchecker.ui.compose.component.SheetCardListSkeleton
import com.absinthe.libchecker.ui.compose.theme.lcCondensedFontFamily
import com.absinthe.libchecker.ui.compose.theme.withCustomFont
import com.absinthe.libchecker.ui.compose.util.rememberActiveValue
import com.absinthe.libchecker.ui.compose.util.sheetBottomSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetContentSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetHeightTransition
import com.absinthe.libchecker.ui.compose.util.toAnnotatedString
import com.absinthe.libchecker.utils.extensions.openUrlInBrowser
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.ConvertFile
import top.yukonga.miuix.kmp.icon.glass.File
import top.yukonga.miuix.kmp.icon.glass.Undo
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

private data class FeatureRow(
  val title: String,
  val subtitle: String? = null,
  val iconRes: Int? = null
)

/** Detail sheet of a single detected feature (split APKs, Kotlin/AGP versions, install source, ...). */
@Composable
fun FeatureDetailSheet(
  show: Boolean,
  viewModel: DetailViewModel,
  packageInfo: PackageInfo,
  item: AppDetailFeatureItemData?,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var rows by remember { mutableStateOf<List<FeatureRow>>(emptyList()) }
  var description by remember { mutableStateOf<String?>(null) }
  var loading by remember(show, item) {
    mutableStateOf(
      show && item != null &&
        (item.action as? com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureAction.Dialog)
          ?.toDialogSpec() == null
    )
  }
  var propItems by remember { mutableStateOf<List<AppPropItem>>(emptyList()) }
  var appIcons by remember {
    mutableStateOf<List<com.absinthe.libchecker.domain.app.detail.model.AppIconItem>>(emptyList())
  }
  var xmlText by remember { mutableStateOf<String?>(null) }

  val activeItem = rememberActiveValue(item)
  val dialogSpec = remember(activeItem.value) {
    (activeItem.value?.action as? AppDetailFeatureAction.Dialog)?.toDialogSpec()
  }

  LaunchedEffect(show, item) {
    if (!show || item == null) return@LaunchedEffect
    val spec = (item.action as? AppDetailFeatureAction.Dialog)?.toDialogSpec()
    if (spec != null) return@LaunchedEffect
    loading = true
    rows = emptyList()
    propItems = emptyList()
    appIcons = emptyList()
    description = null
    when (val action = item.action) {
      is AppDetailFeatureAction.SplitApks -> {
        rows = runCatching { viewModel.getAppBundleItems(packageInfo) }
          .getOrDefault(emptyList())
          .map { FeatureRow(it.nameText, it.sizeText, it.iconRes) }
      }

      AppDetailFeatureAction.AppProp -> {
        propItems = runCatching { viewModel.getAppManifestProperties(packageInfo, null) }
          .getOrDefault(emptyList())
      }

      AppDetailFeatureAction.InstallSource -> {
        val formatter = java.text.DateFormat.getDateTimeInstance(
          java.text.DateFormat.MEDIUM,
          java.text.DateFormat.SHORT
        )
        rows = listOf(
          FeatureRow(
            context.getString(R.string.lib_detail_app_first_installed_time),
            formatter.format(java.util.Date(packageInfo.firstInstallTime))
          ),
          FeatureRow(
            context.getString(R.string.lib_detail_app_last_updated_time),
            formatter.format(java.util.Date(packageInfo.lastUpdateTime))
          )
        )
      }

      AppDetailFeatureAction.XposedModule ->
        description = context.getString(R.string.feature_xposed_description)

      is AppDetailFeatureAction.AppIcons -> {
        appIcons = viewModel.featureState.appIcons
        if (appIcons.isEmpty()) {
          description = context.getString(R.string.feature_app_icons_description)
        }
      }

      is AppDetailFeatureAction.Dialog -> Unit
    }
    loading = false
  }


  val activeXmlText = rememberActiveValue(xmlText)
  WindowBottomSheet(
    show = xmlText != null,
    title = stringResource(R.string.xml_detail),
    onDismissRequest = { xmlText = null },
    onDismissFinished = { if (xmlText == null) activeXmlText.value = null }
  ) {
    activeXmlText.value?.let { text ->
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .sheetHeightTransition()
          .verticalScroll(rememberScrollState())
          .padding(bottom = sheetBottomSafePadding(16.dp))
      ) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.04f)
          ),
          insideMargin = PaddingValues(16.dp)
        ) {
          androidx.compose.foundation.text.selection.SelectionContainer {
            Text(
              text = text,
              style = MiuixTheme.textStyles.footnote2.copy(fontSize = 11.sp),
              fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
              color = MiuixTheme.colorScheme.onSurface
            )
          }
        }
      }
    }
  }

  WindowBottomSheet(
    show = show,
    title = if (dialogSpec == null) {
      activeItem.value?.let { context.getString(it.titleRes) } ?: ""
    } else {
      null
    },
    onDismissRequest = onDismiss,
    onDismissFinished = { if (!show && item == null) activeItem.value = null }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .sheetHeightTransition()
    ) {
      val sheetMode = when {
        dialogSpec != null -> FeatureDetailSheetMode.DialogSpec
        loading -> FeatureDetailSheetMode.Loading
        else -> FeatureDetailSheetMode.Content
      }
      Crossfade(
        targetState = sheetMode,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "FeatureDetailSheetTransition"
      ) { mode ->
        when (mode) {
          FeatureDetailSheetMode.DialogSpec -> dialogSpec?.let { spec ->
            FeatureDialogSheetContent(spec = spec)
          }

          FeatureDetailSheetMode.Loading -> Box(
            modifier = Modifier.padding(bottom = sheetBottomSafePadding(16.dp))
          ) {
            SheetCardListSkeleton(
              rowCount = 3,
              showIcon = false,
              twoLine = true
            )
          }

          FeatureDetailSheetMode.Content -> Column(Modifier.fillMaxWidth()) {
            description?.let { text ->
              Text(
                text = text,
                style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
              )
              Spacer(Modifier.height(8.dp))
            }
            if (appIcons.isNotEmpty()) {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(appIcons) { index, icon ->
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AppIconItemImage(
                      icon = icon,
                      size = 48.dp,
                      cornerRadius = 12.dp
                    )
                    if (index == 0 && icon.isMonochrome) {
                      Spacer(Modifier.height(2.dp))
                      Text(
                        text = stringResource(R.string.dialog_themed),
                        style = MiuixTheme.textStyles.footnote2.copy(fontSize = 10.sp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                      )
                    }
                  }
                }
              }
              Spacer(Modifier.height(8.dp))
            }
            if (propItems.isNotEmpty()) {
              Column {
                LazyColumn(
                  modifier = Modifier.weight(1f, fill = false),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  items(propItems, key = { it.key }) { item ->
                    AppPropRow(
                      item = item,
                      onShowXml = { xmlText = it },
                      onTogglePreview = {
                        propItems = propItems.map { current ->
                          if (current.key != item.key) {
                            current
                          } else if (current.isTransformed) {
                            current.restore()
                          } else {
                            viewModel.resolveAppPropPreview(packageInfo, current)
                              ?.let { preview -> current.copy(preview = preview) }
                              ?: current
                          }
                        }
                      }
                    )
                  }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                  text = stringResource(R.string.lib_detail_app_props_tip),
                  onClick = {
                    context.openUrlInBrowser(URLManager.ANDROID_DEV_MANIFEST_APPLICATION)
                  },
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = sheetBottomSafePadding(16.dp)),
                  colors = ButtonDefaults.textButtonColorsPrimary()
                )
              }
            } else if (rows.isNotEmpty()) {
              LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(bottom = sheetBottomSafePadding(16.dp)),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                items(rows) { row ->
                  FeatureRowItem(row)
                }
              }
            } else {
              Spacer(Modifier.height(sheetBottomSafePadding(16.dp)))
            }
          }
        }
      }
    }
  }
}

private enum class FeatureDetailSheetMode {
  DialogSpec,
  Loading,
  Content
}

@Composable
private fun FeatureRowItem(row: FeatureRow) {
  val colorScheme = MiuixTheme.colorScheme
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.defaultColors(
      color = colorScheme.onSurface.copy(alpha = 0.04f)
    ),
    insideMargin = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      row.iconRes?.let { iconRes ->
        AsyncImage(
          model = iconRes,
          contentDescription = null,
          modifier = Modifier
            .padding(end = 12.dp)
            .size(28.dp)
        )
      }
      Column(Modifier.weight(1f)) {
        Text(
          text = row.title,
          style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
          color = colorScheme.onSurface
        )
        row.subtitle?.takeIf(String::isNotBlank)?.let { subtitle ->
          Spacer(Modifier.height(2.dp))
          Text(
            text = subtitle,
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp)
              .withCustomFont(lcCondensedFontFamily()),
            color = colorScheme.onSurfaceVariantSummary
          )
        }
      }
    }
  }
}

@Composable
private fun AppPropRow(
  item: AppPropItem,
  onShowXml: (String) -> Unit,
  onTogglePreview: () -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.defaultColors(
      color = colorScheme.onSurface.copy(alpha = 0.04f)
    ),
    insideMargin = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      val condensedFont = lcCondensedFontFamily()
      Column(Modifier.weight(1f)) {
        Text(
          text = item.key,
          style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp).withCustomFont(condensedFont),
          color = colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        when (val preview = item.preview) {
          is AppResourcePreview.DrawableValue -> AsyncImage(
            model = preview.drawable,
            contentDescription = null,
            modifier = Modifier.size(28.dp)
          )

          is AppResourcePreview.ColorValue -> Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .padding(end = 6.dp)
                .size(16.dp)
                .squircleBackground(Color(preview.color), 4.dp)
            )
            Text(
              text = "#%08X".format(preview.color),
              style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp)
                .withCustomFont(condensedFont),
              color = colorScheme.onSurfaceVariantSummary
            )
          }

          else -> Text(
            text = item.visibleValue.toString(),
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp)
              .withCustomFont(condensedFont),
            color = colorScheme.onSurfaceVariantSummary
          )
        }
      }
      if (item.resource?.type == "xml" && item.isTransformed) {
        IconButton(onClick = { onShowXml(item.visibleValue.toString()) }) {
          Icon(
            imageVector = MiuixIcons.Glass.File,
            contentDescription = stringResource(R.string.xml_detail),
            tint = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(18.dp)
          )
        }
      }
      if (item.resource != null) {
        IconButton(onClick = onTogglePreview) {
          Icon(
            imageVector = if (item.isTransformed) MiuixIcons.Glass.Undo else MiuixIcons.Glass.ConvertFile,
            contentDescription = stringResource(
              if (item.isTransformed) R.string.app_prop_restore else R.string.app_prop_transform
            ),
            tint = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun FeatureDialogSheetContent(
  spec: com.absinthe.libchecker.domain.app.detail.feature.FeatureDialogSpec
) {
  val context = LocalContext.current
  val colorScheme = MiuixTheme.colorScheme
  val headerTitle = remember(spec, context) {
    formatFeatureDialogEntries(context, spec.titleEntries)
      ?.takeIf(String::isNotBlank)
      ?: context.getString(spec.titleRes)
  }
  val headerSubtitle = remember(spec, context) {
    formatFeatureDialogEntries(context, spec.subtitleEntries)?.takeIf(String::isNotBlank)
  }
  val messageText = remember(spec.messageRes, context) {
    spec.messageRes?.let { res ->
      Html.fromHtml(context.getString(res), Html.FROM_HTML_MODE_LEGACY).toAnnotatedString()
    }
  }

  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      AsyncImage(
        model = spec.iconRes,
        contentDescription = null,
        colorFilter = spec.iconTint?.let { ColorFilter.tint(Color(it)) },
        modifier = Modifier.size(28.dp)
      )
      Spacer(Modifier.width(12.dp))
      Text(
        text = headerTitle,
        style = MiuixTheme.textStyles.title4.copy(fontWeight = FontWeight.Medium),
        color = colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
      )
    }

    headerSubtitle?.let { subtitle ->
      Spacer(Modifier.height(10.dp))
      Text(
        text = subtitle,
        style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp),
        color = colorScheme.onSurface
      )
    }

    val dialogItems = spec.items.orEmpty()
    val sourceUrl = spec.sourceUrl
    if (messageText != null || dialogItems.isNotEmpty()) {
      Spacer(Modifier.height(12.dp))
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f, fill = false)
          .verticalScroll(rememberScrollState())
          .let { base ->
            if (sourceUrl == null) base.padding(bottom = sheetBottomSafePadding(16.dp)) else base
          },
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        messageText?.let { body ->
          Text(
            text = body,
            style = MiuixTheme.textStyles.body2.copy(
              fontSize = 14.sp,
              lineHeight = 20.sp
            ),
            color = colorScheme.onSurfaceVariantSummary
          )
        }
        dialogItems.forEach { entry ->
          val labelText = entry.label.resolve(context)
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(
              color = colorScheme.onSurface.copy(alpha = 0.04f)
            ),
            insideMargin = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              spec.itemIconRes?.let { itemIcon ->
                AsyncImage(
                  model = itemIcon,
                  contentDescription = null,
                  modifier = Modifier
                    .padding(end = 10.dp)
                    .size(20.dp)
                )
              }
              Column(Modifier.weight(1f)) {
                Text(
                  text = labelText,
                  style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
                  color = colorScheme.onSurface
                )
                entry.value?.takeIf(String::isNotBlank)?.let { valueText ->
                  Spacer(Modifier.height(2.dp))
                  Text(
                    text = valueText,
                    style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp)
                      .withCustomFont(lcCondensedFontFamily()),
                    color = colorScheme.onSurfaceVariantSummary
                  )
                }
              }
            }
          }
        }
      }
    }

    if (sourceUrl != null) {
      Spacer(Modifier.height(16.dp))
      TextButton(
        text = stringResource(R.string.lib_detail_app_props_tip),
        onClick = { context.openUrlInBrowser(sourceUrl) },
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = sheetBottomSafePadding(16.dp)),
        colors = ButtonDefaults.textButtonColorsPrimary()
      )
    } else if (messageText == null && dialogItems.isEmpty()) {
      Spacer(Modifier.height(sheetBottomSafePadding(16.dp)))
    }
  }
}

private fun FeatureDialogTitleLabel.resolve(context: android.content.Context): String {
  return when (this) {
    is FeatureDialogTitleLabel.Resource -> context.getString(res)
    is FeatureDialogTitleLabel.Text -> value
  }
}

private fun formatFeatureDialogEntries(
  context: android.content.Context,
  entries: List<FeatureDialogTitleEntry>?
): String? {
  if (entries.isNullOrEmpty()) return null
  return entries.joinToString(", ") { entry ->
    buildString {
      append(entry.label.resolve(context))
      entry.value?.takeIf(String::isNotBlank)?.let { value ->
        append(' ').append(value)
      }
    }
  }
}

/** Renders an [AppIconItem], styling Android 13+ monochrome layers as Monet circles. */
@Composable
internal fun AppIconItemImage(
  icon: AppIconItem,
  size: Dp,
  cornerRadius: Dp,
  modifier: Modifier = Modifier
) {
  val iconPx = with(LocalDensity.current) { size.roundToPx() }.coerceAtLeast(1)
  val monochromeBitmap = if (icon.isMonochrome && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    remember(icon, iconPx) {
      val adaptive = icon.drawable as? AdaptiveIconDrawable ?: return@remember null
      renderMonochromeLayerBitmap(adaptive, iconPx)
    }
  } else {
    null
  }

  if (monochromeBitmap != null) {
    // The themed-icon preview follows Android's wallpaper palette even when LibChecker
    // uses the default blue Miuix theme (Launcher3's light/dark accent1 pairing).
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val background = androidx.compose.ui.res.colorResource(
      if (dark) android.R.color.system_accent1_800 else android.R.color.system_accent1_100
    )
    val foreground = androidx.compose.ui.res.colorResource(
      if (dark) android.R.color.system_accent1_100 else android.R.color.system_accent1_700
    )
    Box(
      modifier = modifier
        .size(size)
        .clip(CircleShape)
        .background(background),
      contentAlignment = Alignment.Center
    ) {
      androidx.compose.foundation.Image(
        bitmap = monochromeBitmap,
        contentDescription = null,
        colorFilter = ColorFilter.tint(foreground),
        modifier = Modifier.size(size)
      )
    }
  } else {
    val regularBitmap = remember(icon, iconPx) {
      runCatching {
        val copy = icon.drawable.constantState?.newDrawable() ?: icon.drawable.mutate()
        copy.toBitmap(iconPx, iconPx).asImageBitmap()
      }.getOrNull()
    }
    if (regularBitmap != null) {
      androidx.compose.foundation.Image(
        bitmap = regularBitmap,
        contentDescription = null,
        modifier = modifier
          .size(size)
          .squircleClip(cornerRadius)
      )
    }
  }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun renderMonochromeLayerBitmap(
  adaptive: AdaptiveIconDrawable,
  iconPx: Int
): ImageBitmap? = runCatching {
  val mono = adaptive.monochrome ?: return@runCatching null
  val monoCopy = mono.constantState?.newDrawable()?.mutate() ?: mono.mutate()
  monoCopy.colorFilter = null
  monoCopy.setTintList(null)
  val bitmap = android.graphics.Bitmap.createBitmap(
    iconPx,
    iconPx,
    android.graphics.Bitmap.Config.ARGB_8888
  )
  val canvas = android.graphics.Canvas(bitmap)
  val inset = (iconPx * 0.25f).roundToInt()
  monoCopy.setBounds(-inset, -inset, iconPx + inset, iconPx + inset)
  monoCopy.draw(canvas)

  var opaqueCount = 0
  var transparentCount = 0
  val step = (iconPx / 12).coerceAtLeast(1)
  for (y in 0 until iconPx step step) {
    for (x in 0 until iconPx step step) {
      val alpha = bitmap.getPixel(x, y) ushr 24
      if (alpha >= 245) opaqueCount++ else if (alpha <= 10) transparentCount++
    }
  }
  if (transparentCount == 0 && opaqueCount > 0) {
    bitmap.recycle()
    return@runCatching null
  }
  bitmap.asImageBitmap()
}.getOrNull()
