package com.absinthe.libchecker.domain.about.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
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
import com.absinthe.libchecker.ui.compose.component.AdaptiveBackButton
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.MiuixPageHost
import com.absinthe.libchecker.ui.compose.component.adaptiveVerticalScroll
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import com.absinthe.libchecker.utils.extensions.openUrlInBrowser
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.glass.FavoriteFill
import top.yukonga.miuix.kmp.icon.glass.File
import top.yukonga.miuix.kmp.icon.glass.Help
import top.yukonga.miuix.kmp.icon.glass.Info
import top.yukonga.miuix.kmp.icon.glass.Link
import top.yukonga.miuix.kmp.icon.glass.Send
import top.yukonga.miuix.kmp.icon.glass.Translate
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import com.absinthe.libchecker.ui.compose.component.FastScrollLazyColumn as LazyColumn

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

  MiuixPageHost(modifier = modifier.fillMaxSize()) {
    val pageState = this
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        AdaptiveTopAppBar(
          title = stringResource(R.string.settings_about),
          scrollBehavior = scrollBehavior,
          navigationIcon = {
            AdaptiveBackButton(onClick = onBack)
          }
        )
      }
    ) { padding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .then(pageState.topBarBackdropModifier)
      ) {
        WideContentBox { sidePadding ->
          LazyColumn(
            state = listState,
            modifier = Modifier
              .fillMaxSize()
              .horizontalCutoutPadding()
              .adaptiveVerticalScroll(scrollBehavior),
            contentPadding = PaddingValues(
              top = padding.calculateTopPadding() + 6.dp,
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
                  style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                  color = colorScheme.onSurfaceVariantSummary
                )
                Spacer(Modifier.height(12.dp))
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
                  startAction = { AboutIcon(MiuixIcons.Glass.Link) },
                  onClick = { context.openUrlInBrowser(URLManager.GITHUB_REPO_PAGE) }
                )
                ArrowPreference(
                  title = stringResource(R.string.join_telegram_group),
                  summary = stringResource(R.string.join_telegram_group_summary),
                  startAction = { AboutIcon(MiuixIcons.Glass.Send) },
                  onClick = { context.openUrlInBrowser(URLManager.TELEGRAM_GROUP) }
                )
                ArrowPreference(
                  title = stringResource(R.string.help_docs),
                  summary = stringResource(R.string.help_docs_summary),
                  startAction = { AboutIcon(MiuixIcons.Glass.Help) },
                  onClick = { context.openUrlInBrowser(URLManager.DOCS_PAGE) }
                )
                ArrowPreference(
                  title = stringResource(R.string.settings_translate),
                  summary = stringResource(R.string.settings_translate_summary),
                  startAction = { AboutIcon(MiuixIcons.Glass.Translate) },
                  onClick = { context.openUrlInBrowser(URLManager.CROWDIN_PAGE) }
                )
                ArrowPreference(
                  title = stringResource(R.string.settings_rate_us),
                  summary = stringResource(R.string.settings_rate_us_summary),
                  startAction = { AboutIcon(MiuixIcons.Glass.FavoriteFill) },
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
                  startAction = { AboutIcon(MiuixIcons.Glass.File) },
                  onClick = onOpenLicenses
                )
                ArrowPreference(
                  title = stringResource(R.string.library_declaration_title),
                  summary = stringResource(R.string.library_declaration_summary),
                  startAction = { AboutIcon(MiuixIcons.Glass.Info) },
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
    }
  }

  DevelopersSheet(
    show = showDevelopers,
    onDismiss = { showDevelopers = false }
  )

  WindowDialog(
    show = showDeclaration,
    title = stringResource(R.string.library_declaration_title),
    summary = stringResource(R.string.library_declaration),
    onDismissRequest = { showDeclaration = false }
  ) {
    TextButton(
      text = stringResource(android.R.string.ok),
      onClick = { showDeclaration = false },
      modifier = Modifier.fillMaxWidth()
    )
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
