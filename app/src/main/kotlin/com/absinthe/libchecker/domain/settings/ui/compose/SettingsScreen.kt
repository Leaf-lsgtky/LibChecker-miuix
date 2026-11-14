package com.absinthe.libchecker.domain.settings.ui.compose

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.absinthe.libchecker.BuildConfig
import coil.compose.AsyncImage
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.domain.about.ui.AboutActivity
import com.absinthe.libchecker.domain.home.presentation.HomeViewModel
import com.absinthe.libchecker.domain.settings.model.GetUpdatesAction
import com.absinthe.libchecker.domain.settings.presentation.SettingsViewModel
import com.absinthe.libchecker.ui.base.ThemeTransitionController
import com.absinthe.libchecker.ui.compose.LocalHomeAppearance
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.utils.extensions.openUrlInBrowser
import com.absinthe.libchecker.utils.showToast
import androidx.fragment.app.FragmentActivity
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.CloudFill
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Translate
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.icon.extended.UploadCloud
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog

private const val THRESHOLD_MIN = 1
private const val THRESHOLD_MAX = 50

/** Settings destination with an in-place appearance sub page. */
@Composable
fun SettingsScreen(
  settingsViewModel: SettingsViewModel,
  homeViewModel: HomeViewModel,
  modifier: Modifier = Modifier,
  mainListState: LazyListState = rememberLazyListState(),
  appearanceListState: LazyListState = rememberLazyListState()
) {
  var showAppearance by rememberSaveable { mutableStateOf(false) }
  AnimatedContent(
    targetState = showAppearance,
    modifier = modifier,
    transitionSpec = {
      if (targetState) {
        (slideInHorizontally(tween(280)) { it / 4 } + fadeIn(tween(200))) togetherWith
          (slideOutHorizontally(tween(240)) { -it / 8 } + fadeOut(tween(140)))
      } else {
        (slideInHorizontally(tween(280)) { -it / 4 } + fadeIn(tween(200))) togetherWith
          (slideOutHorizontally(tween(240)) { it / 8 } + fadeOut(tween(140)))
      }
    },
    label = "settings-page"
  ) { appearance ->
    if (appearance) {
      AppearanceSettingsPage(
        settingsViewModel = settingsViewModel,
        listState = appearanceListState,
        onBack = { showAppearance = false }
      )
    } else {
      SettingsMainPage(
        settingsViewModel = settingsViewModel,
        homeViewModel = homeViewModel,
        listState = mainListState,
        onOpenAppearance = { showAppearance = true }
      )
    }
  }
}

@Composable
private fun SettingsMainPage(
  settingsViewModel: SettingsViewModel,
  homeViewModel: HomeViewModel,
  listState: LazyListState,
  onOpenAppearance: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val activity = context as? FragmentActivity
  val scrollBehavior = MiuixScrollBehavior()
  var showThresholdDialog by remember { mutableStateOf(false) }
  var showTokenDialog by remember { mutableStateOf(false) }
  var showReloadDialog by remember { mutableStateOf(false) }
  var showUpdatesDialog by remember { mutableStateOf(false) }
  var showExportDialog by remember { mutableStateOf(false) }
  var showCloudRulesDialog by remember { mutableStateOf(false) }
  var showInAppUpdateDialog by remember { mutableStateOf(false) }
  var threshold by remember { mutableIntStateOf(settingsViewModel.getLibReferenceThreshold()) }
  var apkAnalytics by remember { mutableStateOf(SPUtils.getValue(Constants.PREF_APK_ANALYTICS, true)) }
  var token by remember { mutableStateOf(GlobalValues.githubApiToken) }
  var repo by remember { mutableStateOf(GlobalValues.repo) }
  var snapshotKeep by remember { mutableStateOf(GlobalValues.snapshotKeep) }
  var analytics by remember { mutableStateOf(GlobalValues.isAnonymousAnalyticsEnabled) }
  val updateBadge by settingsViewModel.updateBadgeVisible.collectAsStateWithLifecycle(initialValue = false)

  val repoLabels = stringArrayResource(R.array.list_repo)
  val repoValues = stringArrayResource(R.array.list_repo_value)
  val keepLabels = stringArrayResource(R.array.snapshot_keep)
  val keepValues = stringArrayResource(R.array.snapshot_keep_value)

  Column(Modifier.fillMaxSize()) {
    AdaptiveTopAppBar(
      title = stringResource(R.string.title_settings),
      scrollBehavior = scrollBehavior
    )
    WideContentBox { sidePadding ->
      LazyColumn(
        state = listState,
        modifier = Modifier
          .fillMaxSize()
          .horizontalCutoutPadding()
          .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(start = sidePadding, end = sidePadding)
      ) {
        item { Spacer(Modifier.height(12.dp)) }
        item {
          SmallTitle(text = stringResource(R.string.pref_group_normal))
          Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
            SwitchPreference(
              checked = apkAnalytics,
              onCheckedChange = {
                apkAnalytics = it
                settingsViewModel.setApkAnalysisEnabled(it).onFailure { error ->
                  context.showToast(error.toString())
                }
              },
              title = stringResource(R.string.apk_analytics),
              summary = stringResource(R.string.apk_analytics_summary),
              startAction = { SettingsIcon(MiuixIcons.Notes) }
            )
            WindowDropdownPreference(
              items = keepLabels.toList(),
              selectedIndex = keepValues.indexOf(snapshotKeep).coerceAtLeast(0),
              title = stringResource(R.string.snapshot_keep),
              startAction = { SettingsIcon(MiuixIcons.Album) },
              onSelectedIndexChange = { index ->
                snapshotKeep = keepValues[index]
                settingsViewModel.setSnapshotKeepRule(snapshotKeep)
              }
            )
            ArrowPreference(
              title = stringResource(R.string.lib_ref_threshold),
              summary = threshold.toString(),
              startAction = { SettingsIcon(MiuixIcons.Tune) },
              onClick = { showThresholdDialog = true }
            )
            ArrowPreference(
              title = stringResource(R.string.pref_group_appearance),
              summary = stringResource(R.string.pref_appearance_summary),
              startAction = { SettingsIcon(MiuixIcons.Theme) },
              onClick = onOpenAppearance
            )
          }
        }
        item {
          SmallTitle(text = stringResource(R.string.pref_group_rules))
          Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
            WindowDropdownPreference(
              items = repoLabels.toList(),
              selectedIndex = repoValues.indexOf(repo).coerceAtLeast(0),
              title = stringResource(R.string.rules_repo_title),
              startAction = { SettingsIcon(MiuixIcons.Link) },
              onSelectedIndexChange = { index ->
                repo = repoValues[index]
                settingsViewModel.selectRemoteRulesRepository(repo)
              }
            )
            ArrowPreference(
              title = stringResource(R.string.settings_github_token),
              summary = stringResource(
                if (token.isBlank()) {
                  R.string.settings_github_token_summary_not_set
                } else {
                  R.string.settings_github_token_summary_set
                }
              ),
              startAction = { SettingsIcon(MiuixIcons.Lock) },
              onClick = { showTokenDialog = true }
            )
            ArrowPreference(
              title = stringResource(R.string.cloud_rules),
              summary = stringResource(R.string.cloud_rules_summary),
              startAction = { SettingsIcon(MiuixIcons.CloudFill) },
              onClick = { showCloudRulesDialog = true }
            )
          }
        }
        item {
          SmallTitle(text = stringResource(R.string.pref_group_data))
          Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
            ArrowPreference(
              title = stringResource(R.string.reload_apps),
              summary = stringResource(R.string.reload_apps_summary),
              startAction = { SettingsIcon(MiuixIcons.Refresh) },
              onClick = { showReloadDialog = true }
            )
            ArrowPreference(
              title = stringResource(R.string.export_apps),
              summary = stringResource(R.string.export_apps_webui_tip),
              startAction = { SettingsIcon(MiuixIcons.UploadCloud) },
              onClick = { showExportDialog = true }
            )
            ArrowPreference(
              title = stringResource(R.string.export_log),
              summary = stringResource(R.string.export_log_summary),
              startAction = { SettingsIcon(MiuixIcons.File) },
              onClick = {
                scope.launch {
                  val intent = settingsViewModel.buildLogShareIntent().getOrNull() ?: return@launch
                  runCatching {
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.export_log)))
                  }
                }
              }
            )
          }
        }
        item {
          SmallTitle(text = stringResource(R.string.pref_group_about))
          Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
            ArrowPreference(
              title = stringResource(R.string.settings_get_updates),
              summary = stringResource(R.string.settings_get_updates_summary),
              startAction = { SettingsIcon(MiuixIcons.Update) },
              endActions = {
                if (updateBadge) {
                  Text(
                    text = "NEW",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.primary
                  )
                }
              },
              onClick = { showUpdatesDialog = true }
            )
            ArrowPreference(
              title = stringResource(R.string.settings_about),
              summary = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})",
              startAction = { SettingsIcon(MiuixIcons.Info) },
              onClick = { context.startActivity(Intent(context, AboutActivity::class.java)) }
            )
            ArrowPreference(
              title = stringResource(R.string.help_docs),
              summary = stringResource(R.string.help_docs_summary),
              startAction = { SettingsIcon(MiuixIcons.Help) },
              onClick = { context.openUrlInBrowser(URLManager.DOCS_PAGE) }
            )
            ArrowPreference(
              title = stringResource(R.string.settings_translate),
              summary = stringResource(R.string.settings_translate_summary),
              startAction = { SettingsIcon(MiuixIcons.Translate) },
              onClick = { context.openUrlInBrowser(URLManager.CROWDIN_PAGE) }
            )
            ArrowPreference(
              title = stringResource(R.string.settings_rate_us),
              summary = stringResource(R.string.settings_rate_us_summary),
              startAction = { SettingsIcon(MiuixIcons.Favorites) },
              onClick = { context.openUrlInBrowser(URLManager.PLAY_STORE_DETAIL_PAGE) }
            )
            ArrowPreference(
              title = stringResource(R.string.join_telegram_group),
              summary = stringResource(R.string.join_telegram_group_summary),
              startAction = { SettingsIcon(MiuixIcons.Send) },
              onClick = { context.openUrlInBrowser(URLManager.TELEGRAM_GROUP) }
            )
            if (!BuildConfig.IS_FOSS) {
              SwitchPreference(
                checked = analytics,
                onCheckedChange = {
                  analytics = it
                  SPUtils.putValue(Constants.PREF_ANONYMOUS_ANALYTICS, it)
                },
                title = stringResource(R.string.anonymous_statistics),
                summary = stringResource(R.string.anonymous_statistics_summary),
                startAction = { SettingsIcon(MiuixIcons.Report) }
              )
            }
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

  if (showThresholdDialog) {
    WindowDialog(
      show = true,
      title = stringResource(R.string.lib_ref_threshold),
      onDismissRequest = { showThresholdDialog = false }
    ) {
      Column(Modifier.padding(horizontal = 24.dp)) {
        Text(
          text = stringResource(R.string.lib_ref_threshold_summary),
          style = MiuixTheme.textStyles.body2,
          color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        Spacer(Modifier.height(12.dp))
        Slider(
          value = threshold.toFloat(),
          onValueChange = { threshold = it.toInt().coerceIn(THRESHOLD_MIN, THRESHOLD_MAX) },
          valueRange = THRESHOLD_MIN.toFloat()..THRESHOLD_MAX.toFloat(),
          steps = THRESHOLD_MAX - THRESHOLD_MIN - 1
        )
        Text(
          text = threshold.toString(),
          style = MiuixTheme.textStyles.title3,
          color = MiuixTheme.colorScheme.onSurface,
          modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TextButton(
            text = stringResource(android.R.string.cancel),
            onClick = { showThresholdDialog = false },
            modifier = Modifier.weight(1f)
          )
          TextButton(
            text = stringResource(android.R.string.ok),
            onClick = {
              settingsViewModel.setLibReferenceThreshold(threshold)
              showThresholdDialog = false
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.textButtonColorsPrimary()
          )
        }
      }
    }
  }

  if (showTokenDialog) {
    val tokenState = rememberTextFieldState(initialText = token)
    WindowDialog(
      show = true,
      title = stringResource(R.string.settings_github_token),
      onDismissRequest = { showTokenDialog = false }
    ) {
      Column(Modifier.padding(horizontal = 24.dp)) {
        TextField(
          state = tokenState,
          label = stringResource(R.string.settings_github_token),
          lineLimits = TextFieldLineLimits.SingleLine,
          modifier = Modifier.fillMaxWidth()
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TextButton(
            text = stringResource(R.string.settings_github_token_clear),
            onClick = {
              token = ""
              GlobalValues.githubApiToken = ""
              showTokenDialog = false
            },
            modifier = Modifier.weight(1f)
          )
          TextButton(
            text = stringResource(android.R.string.ok),
            onClick = {
              token = tokenState.text.toString()
              GlobalValues.githubApiToken = token
              showTokenDialog = false
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.textButtonColorsPrimary()
          )
        }
      }
    }
  }

  if (showReloadDialog) {
    WindowDialog(
      show = true,
      title = stringResource(R.string.dialog_title_reload_apps),
      summary = stringResource(R.string.dialog_subtitle_reload_apps),
      onDismissRequest = { showReloadDialog = false }
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        TextButton(
          text = stringResource(android.R.string.cancel),
          onClick = { showReloadDialog = false },
          modifier = Modifier.weight(1f)
        )
        TextButton(
          text = stringResource(android.R.string.ok),
          onClick = {
            homeViewModel.reloadApps()
            showReloadDialog = false
          },
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.textButtonColorsPrimary()
        )
      }
    }
  }

  InAppUpdateSheet(
    show = showInAppUpdateDialog,
    viewModel = settingsViewModel,
    onDismiss = { showInAppUpdateDialog = false }
  )

  CloudRulesSheet(
    show = showCloudRulesDialog,
    viewModel = settingsViewModel,
    onDismiss = { showCloudRulesDialog = false }
  )

  ExportAppsSheet(
    show = showExportDialog,
    viewModel = settingsViewModel,
    onDismiss = { showExportDialog = false }
  )

  if (showUpdatesDialog) {
    val updateItems = remember { settingsViewModel.buildGetUpdatesDialogState().items }
    WindowBottomSheet(
      show = showUpdatesDialog,
      title = stringResource(R.string.settings_get_updates),
      onDismissRequest = { showUpdatesDialog = false }
    ) {
      Column(Modifier.fillMaxWidth()) {
        updateItems.forEach { item ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp)
              .padding(bottom = 8.dp),
            onClick = {
              showUpdatesDialog = false
              when (val action = item.action) {
                is GetUpdatesAction.OpenUri -> context.openUrlInBrowser(action.uri)
                GetUpdatesAction.OpenInAppUpdate -> {
                  showUpdatesDialog = false
                  showInAppUpdateDialog = true
                }
              }
            },
            showIndication = true,
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              AsyncImage(
                model = item.iconRes,
                contentDescription = null,
                modifier = Modifier
                  .padding(end = 12.dp)
                  .size(22.dp)
              )
              Text(
                text = item.text,
                fontWeight = FontWeight(500),
                color = MiuixTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AppearanceSettingsPage(
  settingsViewModel: SettingsViewModel,
  listState: LazyListState,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val activity = context as? AppCompatActivity
  val appearance = LocalHomeAppearance.current
  val scrollBehavior = MiuixScrollBehavior()
  var darkMode by remember { mutableStateOf(GlobalValues.darkMode) }
  var amoled by remember { mutableStateOf(GlobalValues.isAmoledTheme) }
  var colorfulIcon by remember { mutableStateOf(GlobalValues.isColorfulIcon) }
  var floatingNavBar by remember { mutableStateOf(appearance.floatingNavBar) }
  var blurDesign by remember { mutableStateOf(appearance.blurDesign) }
  var legacySnapshotDetail by remember { mutableStateOf(SPUtils.getValue(Constants.PREF_LEGACY_SNAPSHOT_DETAIL, false)) }

  val darkModeLabels = stringArrayResource(R.array.list_dark_mode)
  val darkModeValues = stringArrayResource(R.array.list_dark_mode_value)
  val languageLabels = stringArrayResource(R.array.language)
  val languageValues = stringArrayResource(R.array.language_value)

  Column(Modifier.fillMaxSize()) {
    AdaptiveTopAppBar(
      title = stringResource(R.string.pref_group_appearance),
      scrollBehavior = scrollBehavior,
      navigationIcon = {
        IconButton(onClick = onBack) {
          Icon(
            imageVector = MiuixIcons.Back,
            contentDescription = null
          )
        }
      }
    )
    WideContentBox { sidePadding ->
      LazyColumn(
        state = listState,
        modifier = Modifier
          .fillMaxSize()
          .horizontalCutoutPadding()
          .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(start = sidePadding, end = sidePadding)
      ) {
        item { Spacer(Modifier.height(12.dp)) }
        item {
          Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
            WindowDropdownPreference(
              items = darkModeLabels.toList(),
              selectedIndex = darkModeValues.indexOf(darkMode).coerceAtLeast(0),
              title = stringResource(R.string.dark_mode),
              startAction = { SettingsIcon(MiuixIcons.Theme) },
              onSelectedIndexChange = { index ->
                darkMode = darkModeValues[index]
                val nightMode = settingsViewModel.selectDarkMode(darkMode)
                if (activity != null) {
                  ThemeTransitionController.applyNightMode(activity, nightMode)
                }
              }
            )
            SwitchPreference(
              checked = amoled,
              onCheckedChange = { enabled ->
                amoled = enabled
                GlobalValues.isAmoledTheme = enabled
                activity?.window?.decorView?.postDelayed({
                  activity.let { ThemeTransitionController.recreateWithTransition(it) }
                }, 300L)
              },
              title = stringResource(R.string.amoled_theme),
              summary = stringResource(R.string.amoled_theme_summary),
              startAction = { SettingsIcon(MiuixIcons.Theme) }
            )
            if (!OsUtils.atLeastT()) {
              WindowDropdownPreference(
                items = languageLabels.toList(),
                selectedIndex = languageValues.indexOf(SPUtils.getValue(Constants.PREF_LOCALE, "SYSTEM"))
                  .coerceAtLeast(0),
                title = stringResource(R.string.languages),
                startAction = { SettingsIcon(MiuixIcons.Translate) },
                onSelectedIndexChange = { index ->
                  settingsViewModel.selectLocale(languageValues[index])
                  activity?.recreate()
                }
              )
            }
            SwitchPreference(
              checked = colorfulIcon,
              onCheckedChange = {
                colorfulIcon = it
                settingsViewModel.setColorfulRuleIcon(it)
              },
              title = stringResource(R.string.colorful_icon),
              summary = stringResource(R.string.colorful_icon_summary),
              startAction = { SettingsIcon(MiuixIcons.Theme) }
            )
            SwitchPreference(
              checked = floatingNavBar,
              onCheckedChange = {
                floatingNavBar = it
                GlobalValues.isFloatingNavBar = it
                appearance.floatingNavBar = it
              },
              title = stringResource(R.string.floating_nav_bar),
              summary = stringResource(R.string.floating_nav_bar_summary),
              startAction = { SettingsIcon(MiuixIcons.Tune) }
            )
            if (OsUtils.atLeastT()) {
              SwitchPreference(
                checked = blurDesign,
                onCheckedChange = {
                  blurDesign = it
                  GlobalValues.isBlurDesign = it
                  appearance.blurDesign = it
                },
                title = stringResource(R.string.blur_design),
                summary = stringResource(R.string.blur_design_summary),
                startAction = { SettingsIcon(MiuixIcons.Theme) }
              )
            }
            SwitchPreference(
              checked = legacySnapshotDetail,
              onCheckedChange = {
                legacySnapshotDetail = it
                SPUtils.putValue(Constants.PREF_LEGACY_SNAPSHOT_DETAIL, it)
              },
              title = stringResource(R.string.legacy_snapshot_detail),
              summary = stringResource(R.string.legacy_snapshot_detail_summary),
              startAction = { SettingsIcon(MiuixIcons.Album) }
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

@Composable
private fun SettingsIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
  Icon(
    imageVector = icon,
    contentDescription = null,
    tint = MiuixTheme.colorScheme.onSurfaceContainer,
    modifier = Modifier.size(22.dp)
  )
}
