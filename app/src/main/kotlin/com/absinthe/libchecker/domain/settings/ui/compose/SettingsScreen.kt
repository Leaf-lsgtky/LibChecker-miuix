package com.absinthe.libchecker.domain.settings.ui.compose

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.domain.about.ui.AboutActivity
import com.absinthe.libchecker.domain.home.presentation.HomeViewModel
import com.absinthe.libchecker.domain.settings.model.GetUpdatesAction
import com.absinthe.libchecker.domain.settings.presentation.SettingsViewModel
import com.absinthe.libchecker.domain.settings.ui.AppearanceActivity
import com.absinthe.libchecker.ui.compose.component.AdaptiveDropdownPreference
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.LocalHomeBottomPadding
import com.absinthe.libchecker.ui.compose.component.MiuixPageHost
import com.absinthe.libchecker.ui.compose.component.adaptiveVerticalScroll
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import com.absinthe.libchecker.ui.compose.util.sheetContentSafePadding
import com.absinthe.libchecker.ui.compose.util.sheetHeightTransition
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.utils.extensions.openUrlInBrowser
import com.absinthe.libchecker.utils.showToast
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.Album
import top.yukonga.miuix.kmp.icon.glass.CloudFill
import top.yukonga.miuix.kmp.icon.glass.Favorites
import top.yukonga.miuix.kmp.icon.glass.File
import top.yukonga.miuix.kmp.icon.glass.Help
import top.yukonga.miuix.kmp.icon.glass.Info
import top.yukonga.miuix.kmp.icon.glass.Link
import top.yukonga.miuix.kmp.icon.glass.Refresh
import top.yukonga.miuix.kmp.icon.glass.Report
import top.yukonga.miuix.kmp.icon.glass.Send
import top.yukonga.miuix.kmp.icon.glass.Translate
import top.yukonga.miuix.kmp.icon.glass.Update
import top.yukonga.miuix.kmp.icon.glass.UploadCloud
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog

private const val THRESHOLD_MIN = 1
private const val THRESHOLD_MAX = 50

/** Main settings tab screen. */
@Composable
fun SettingsScreen(
  settingsViewModel: SettingsViewModel,
  homeViewModel: HomeViewModel,
  modifier: Modifier = Modifier,
  mainListState: LazyListState = rememberLazyListState()
) {
  val context = LocalContext.current
  Box(modifier = modifier.fillMaxSize()) {
    SettingsMainPage(
      settingsViewModel = settingsViewModel,
      homeViewModel = homeViewModel,
      listState = mainListState,
      onOpenAppearance = {
        context.startActivity(Intent(context, AppearanceActivity::class.java))
      }
    )
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
  val homeBottomPadding = LocalHomeBottomPadding.current

  MiuixPageHost {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = MiuixTheme.colorScheme.surface,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      topBar = {
        AdaptiveTopAppBar(
          title = stringResource(R.string.title_settings),
          scrollBehavior = scrollBehavior,
          isContentScrolled = listState.canScrollBackward
        )
      }
    ) { padding ->
      Box(modifier = Modifier.fillMaxSize().then(topBarBackdropModifier)) {
        WideContentBox { sidePadding ->
          LazyColumn(
            state = listState,
            modifier = Modifier
              .fillMaxSize()
              .horizontalCutoutPadding()
              .adaptiveVerticalScroll(scrollBehavior),
            contentPadding = PaddingValues(
              top = padding.calculateTopPadding() + 12.dp,
              start = sidePadding,
              end = sidePadding
            )
          ) {
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
                  startAction = { SettingsIcon(R.drawable.ic_apk_analytics) }
                )
                AdaptiveDropdownPreference(
                  items = keepLabels.toList(),
                  selectedIndex = keepValues.indexOf(snapshotKeep).coerceAtLeast(0),
                  title = stringResource(R.string.snapshot_keep),
                  startAction = { SettingsIcon(MiuixIcons.Glass.Album) },
                  onSelectedIndexChange = { index ->
                    snapshotKeep = keepValues[index]
                    settingsViewModel.setSnapshotKeepRule(snapshotKeep)
                  }
                )
                ArrowPreference(
                  title = stringResource(R.string.lib_ref_threshold),
                  summary = threshold.toString(),
                  startAction = { SettingsIcon(R.drawable.ic_floating_bar) },
                  onClick = { showThresholdDialog = true }
                )
                ArrowPreference(
                  title = stringResource(R.string.pref_group_appearance),
                  summary = stringResource(R.string.pref_appearance_summary),
                  startAction = { SettingsIcon(R.drawable.ic_blur) },
                  onClick = onOpenAppearance
                )
              }
            }
            item {
              SmallTitle(text = stringResource(R.string.pref_group_rules))
              Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                AdaptiveDropdownPreference(
                  items = repoLabels.toList(),
                  selectedIndex = repoValues.indexOf(repo).coerceAtLeast(0),
                  title = stringResource(R.string.rules_repo_title),
                  startAction = { SettingsIcon(MiuixIcons.Glass.Link) },
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
              startAction = { SettingsIcon(R.drawable.ic_github) },
              onClick = { showTokenDialog = true }
            )
            ArrowPreference(
              title = stringResource(R.string.cloud_rules),
              summary = stringResource(R.string.cloud_rules_summary),
              startAction = { SettingsIcon(MiuixIcons.Glass.CloudFill) },
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
              startAction = { SettingsIcon(MiuixIcons.Glass.Refresh) },
              onClick = { showReloadDialog = true }
            )
            ArrowPreference(
              title = stringResource(R.string.export_apps),
              summary = stringResource(R.string.export_apps_webui_tip),
              startAction = { SettingsIcon(MiuixIcons.Glass.UploadCloud) },
              onClick = { showExportDialog = true }
            )
            ArrowPreference(
              title = stringResource(R.string.export_log),
              summary = stringResource(R.string.export_log_summary),
              startAction = { SettingsIcon(MiuixIcons.Glass.File) },
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
              startAction = { SettingsIcon(MiuixIcons.Glass.Update) },
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
              startAction = { SettingsIcon(MiuixIcons.Glass.Info) },
              onClick = { context.startActivity(Intent(context, AboutActivity::class.java)) }
            )
            ArrowPreference(
              title = stringResource(R.string.help_docs),
              summary = stringResource(R.string.help_docs_summary),
              startAction = { SettingsIcon(MiuixIcons.Glass.Help) },
              onClick = { context.openUrlInBrowser(URLManager.DOCS_PAGE) }
            )
            ArrowPreference(
              title = stringResource(R.string.settings_translate),
              summary = stringResource(R.string.settings_translate_summary),
              startAction = { SettingsIcon(MiuixIcons.Glass.Translate) },
              onClick = { context.openUrlInBrowser(URLManager.CROWDIN_PAGE) }
            )
            ArrowPreference(
              title = stringResource(R.string.settings_rate_us),
              summary = stringResource(R.string.settings_rate_us_summary),
              startAction = { SettingsIcon(MiuixIcons.Glass.Favorites) },
              onClick = { context.openUrlInBrowser(URLManager.PLAY_STORE_DETAIL_PAGE) }
            )
            ArrowPreference(
              title = stringResource(R.string.join_telegram_group),
              summary = stringResource(R.string.join_telegram_group_summary),
              startAction = { SettingsIcon(MiuixIcons.Glass.Send) },
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
                startAction = { SettingsIcon(MiuixIcons.Glass.Report) }
              )
            }
          }
        }
        item {
          val bottomModifier = if (homeBottomPadding > 0.dp) {
            Modifier.height(24.dp + homeBottomPadding)
          } else {
            Modifier.height(24.dp).navigationBarsPadding()
          }
          Spacer(bottomModifier)
        }
      }
    }
  }
  }
}

  WindowDialog(
    show = showThresholdDialog,
    title = stringResource(R.string.lib_ref_threshold),
    onDismissRequest = { showThresholdDialog = false }
  ) {
    Column {
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
          .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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

  val tokenState = rememberTextFieldState(initialText = token)
  LaunchedEffect(showTokenDialog) {
    if (showTokenDialog) {
      tokenState.setTextAndPlaceCursorAtEnd(token)
    }
  }
  WindowDialog(
    show = showTokenDialog,
    title = stringResource(R.string.settings_github_token),
    onDismissRequest = { showTokenDialog = false }
  ) {
    Column {
      TextField(
        state = tokenState,
        label = stringResource(R.string.settings_github_token),
        lineLimits = TextFieldLineLimits.SingleLine,
        modifier = Modifier.fillMaxWidth()
      )
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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

  WindowDialog(
    show = showReloadDialog,
    title = stringResource(R.string.dialog_title_reload_apps),
    summary = stringResource(R.string.dialog_subtitle_reload_apps),
    onDismissRequest = { showReloadDialog = false }
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
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

  val updateItems = remember { settingsViewModel.buildGetUpdatesDialogState().items }
  WindowBottomSheet(
    show = showUpdatesDialog,
    title = stringResource(R.string.settings_get_updates),
    onDismissRequest = { showUpdatesDialog = false }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp)
        .sheetContentSafePadding()
        .sheetHeightTransition(),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      updateItems.forEach { item ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.04f)
          ),
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

@Composable
internal fun SettingsIcon(@androidx.annotation.DrawableRes iconRes: Int) {
  Icon(
    painter = painterResource(iconRes),
    contentDescription = null,
    tint = MiuixTheme.colorScheme.onBackground,
    modifier = Modifier
      .padding(end = 6.dp)
      .size(24.dp)
  )
}

@Composable
internal fun SettingsIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
  Icon(
    imageVector = icon,
    contentDescription = null,
    tint = MiuixTheme.colorScheme.onBackground,
    modifier = Modifier
      .padding(end = 6.dp)
      .size(26.dp)
  )
}
