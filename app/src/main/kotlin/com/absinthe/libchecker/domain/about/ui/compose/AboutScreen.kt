package com.absinthe.libchecker.domain.about.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import com.absinthe.libchecker.utils.extensions.openUrlInBrowser
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.icon.extended.Translate
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** About page: hero, project links and the open-source licence entry. */
@Composable
fun AboutScreen(
  onBack: () -> Unit,
  onOpenLicenses: () -> Unit,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState()
) {
  val context = LocalContext.current
  val scrollBehavior = MiuixScrollBehavior()
  val colorScheme = MiuixTheme.colorScheme
  val placeholder = ColorPainter(colorScheme.surfaceContainerHigh)
  var showDevelopers by remember { mutableStateOf(false) }
  var showDeclaration by remember { mutableStateOf(false) }

  Column(modifier = modifier.fillMaxSize()) {
    AdaptiveTopAppBar(
      title = stringResource(R.string.settings_about),
      scrollBehavior = scrollBehavior,
      navigationIcon = {
        IconButton(onClick = onBack) {
          Icon(MiuixIcons.Back, contentDescription = null)
        }
      }
    )
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
        item(key = "hero") {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 24.dp)
              .padding(top = 8.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(76.dp)
                .squircleClip(18.dp)
            ) {
              AsyncImage(
                model = R.mipmap.ic_launcher,
                contentDescription = null,
                placeholder = placeholder,
                error = placeholder,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
            }
            Spacer(Modifier.height(12.dp))
            Text(
              text = stringResource(R.string.app_name),
              style = MiuixTheme.textStyles.title3,
              fontWeight = FontWeight(600),
              color = colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
              text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
              fontSize = 12.sp,
              color = colorScheme.onSurfaceVariantSummary
            )
            Spacer(Modifier.height(10.dp))
            Text(
              text = stringResource(R.string.about_info),
              style = MiuixTheme.textStyles.body2,
              color = colorScheme.onSurfaceVariantSummary,
              textAlign = TextAlign.Center
            )
          }
        }
        item(key = "links-title") {
          SmallTitle(text = stringResource(R.string.pref_group_about))
        }
        item(key = "links") {
          Card(
            modifier = Modifier
              .padding(horizontal = 12.dp)
              .padding(bottom = 12.dp)
          ) {
            ArrowPreference(
              title = "GitHub",
              summary = "LibChecker/LibChecker",
              startAction = { AboutIcon(MiuixIcons.Link) },
              onClick = { context.openUrlInBrowser(URLManager.GITHUB_REPO_PAGE) }
            )
            ArrowPreference(
              title = stringResource(R.string.join_telegram_group),
              summary = stringResource(R.string.join_telegram_group_summary),
              startAction = { AboutIcon(MiuixIcons.Send) },
              onClick = { context.openUrlInBrowser(URLManager.TELEGRAM_GROUP) }
            )
            ArrowPreference(
              title = stringResource(R.string.help_docs),
              summary = stringResource(R.string.help_docs_summary),
              startAction = { AboutIcon(MiuixIcons.Help) },
              onClick = { context.openUrlInBrowser(URLManager.DOCS_PAGE) }
            )
            ArrowPreference(
              title = stringResource(R.string.settings_translate),
              summary = stringResource(R.string.settings_translate_summary),
              startAction = { AboutIcon(MiuixIcons.Translate) },
              onClick = { context.openUrlInBrowser(URLManager.CROWDIN_PAGE) }
            )
            ArrowPreference(
              title = stringResource(R.string.settings_rate_us),
              summary = stringResource(R.string.settings_rate_us_summary),
              startAction = { AboutIcon(MiuixIcons.Favorites) },
              onClick = { context.openUrlInBrowser(URLManager.PLAY_STORE_DETAIL_PAGE) }
            )
            ArrowPreference(
              title = stringResource(R.string.about_developers),
              summary = stringResource(R.string.about_developers_summary),
              startAction = { AboutIcon(MiuixIcons.Contacts) },
              onClick = { showDevelopers = true }
            )
            ArrowPreference(
              title = stringResource(R.string.about_open_source_licenses),
              summary = stringResource(R.string.about_open_source_licenses_summary),
              startAction = { AboutIcon(MiuixIcons.File) },
              onClick = onOpenLicenses
            )
            ArrowPreference(
              title = stringResource(R.string.library_declaration_title),
              summary = stringResource(R.string.library_declaration_summary),
              startAction = { AboutIcon(MiuixIcons.Info) },
              onClick = { showDeclaration = true }
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

  DevelopersSheet(
    show = showDevelopers,
    onDismiss = { showDevelopers = false }
  )

  if (showDeclaration) {
    WindowDialog(
      show = true,
      title = stringResource(R.string.library_declaration_title),
      summary = stringResource(R.string.library_declaration),
      onDismissRequest = { showDeclaration = false }
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.End
      ) {
        TextButton(
          text = stringResource(android.R.string.ok),
          onClick = { showDeclaration = false }
        )
      }
    }
  }
}

@Composable
private fun AboutIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
  Icon(
    imageVector = icon,
    contentDescription = null,
    tint = MiuixTheme.colorScheme.onSurfaceContainer,
    modifier = Modifier.size(22.dp)
  )
}
