package com.absinthe.libchecker.domain.app.detail.ui.compose

import android.content.pm.PackageInfo
import android.text.Html
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureAction
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureItemData
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import top.yukonga.miuix.kmp.basic.Text
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
  var loading by remember { mutableStateOf(false) }

  LaunchedEffect(show, item) {
    if (!show || item == null) return@LaunchedEffect
    loading = true
    rows = emptyList()
    description = null
    when (val action = item.action) {
      is AppDetailFeatureAction.Kotlin -> {
        rows = action.extras.orEmpty()
          .filterValues { !it.isNullOrBlank() }
          .map { FeatureRow(it.key, it.value) }
      }

      is AppDetailFeatureAction.Agp -> {
        rows = buildList {
          action.version?.takeIf(String::isNotBlank)?.let { add(FeatureRow("AGP", it)) }
          action.extras.orEmpty()
            .filterValues { !it.isNullOrBlank() }
            .forEach { add(FeatureRow(it.key, it.value)) }
        }
      }

      is AppDetailFeatureAction.JetpackCompose -> {
        rows = listOfNotNull(action.version?.takeIf(String::isNotBlank)?.let { FeatureRow("Jetpack Compose", it) })
      }

      is AppDetailFeatureAction.ComposeMultiplatformResources -> {
        rows = listOfNotNull(
          action.version?.takeIf(String::isNotBlank)?.let {
            FeatureRow("Compose Multiplatform Resources", it)
          }
        )
      }

      is AppDetailFeatureAction.SplitApks -> {
        rows = runCatching { viewModel.getAppBundleItems(packageInfo) }
          .getOrDefault(emptyList())
          .map { FeatureRow(it.nameText, it.sizeText, it.iconRes) }
      }

      AppDetailFeatureAction.AppProp -> {
        rows = runCatching { viewModel.getAppManifestProperties(packageInfo, null) }
          .getOrDefault(emptyList())
          .map { FeatureRow(it.key, it.visibleValue.toString()) }
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
      is AppDetailFeatureAction.AppFunction -> {
        rows = action.functions.map { FeatureRow(it.name, it.description) }
      }

      AppDetailFeatureAction.ElfPageSize16Kb -> {
        description = context.getString(R.string.lib_detail_dialog_content_16kb_page_size)
      }

      AppDetailFeatureAction.ElfPageSize16KbCompat -> {
        description = Html.fromHtml(
          context.getString(R.string.lib_detail_dialog_content_16kb_page_size_compat),
          Html.FROM_HTML_MODE_LEGACY
        ).toString()
      }

      AppDetailFeatureAction.PlaySigning ->
        description = context.getString(R.string.feature_play_signing_description)

      AppDetailFeatureAction.Pwa ->
        description = context.getString(R.string.feature_pwa_description)

      AppDetailFeatureAction.XposedModule ->
        description = context.getString(R.string.feature_xposed_description)

      AppDetailFeatureAction.LiveUpdateNotification ->
        description = context.getString(R.string.feature_live_update_description)

      is AppDetailFeatureAction.AppIcons -> {
        description = context.getString(R.string.feature_app_icons_description)
      }
    }
    loading = false
  }

  WindowBottomSheet(
    show = show,
    title = item?.let { context.getString(it.titleRes) } ?: "",
    onDismissRequest = onDismiss
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp)
    ) {
      when {
        loading -> LcLoadingState(
          modifier = Modifier.height(140.dp),
          message = context.getString(R.string.loading)
        )

        else -> {
          description?.let { text ->
            Text(
              text = text,
              fontSize = 13.sp,
              color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Spacer(Modifier.height(8.dp))
          }
          if (rows.isNotEmpty()) {
            LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
              items(rows) { row ->
                FeatureRowItem(row)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FeatureRowItem(row: FeatureRow) {
  val colorScheme = MiuixTheme.colorScheme
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    row.iconRes?.let { iconRes ->
      AsyncImage(
        model = iconRes,
        contentDescription = null,
        modifier = Modifier
          .padding(end = 10.dp)
          .size(28.dp)
      )
    }
    Column(Modifier.weight(1f)) {
      Text(
        text = row.title,
        fontSize = 13.sp,
        fontWeight = FontWeight(550),
        color = colorScheme.onSurface
      )
      row.subtitle?.takeIf(String::isNotBlank)?.let { subtitle ->
        Spacer(Modifier.height(2.dp))
        Text(
          text = subtitle,
          fontSize = 12.sp,
          color = colorScheme.onSurfaceVariantSummary
        )
      }
    }
  }
}
