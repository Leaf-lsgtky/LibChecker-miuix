package com.absinthe.libchecker.domain.settings.ui.compose

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.settings.presentation.SettingsViewModel
import com.absinthe.libchecker.ui.compose.LocalHomeAppearance
import com.absinthe.libchecker.ui.compose.component.AdaptiveBackButton
import com.absinthe.libchecker.ui.compose.component.AdaptiveDropdownPreference
import com.absinthe.libchecker.ui.compose.component.AdaptiveTopAppBar
import com.absinthe.libchecker.ui.compose.component.LocalHomeBottomPadding
import com.absinthe.libchecker.ui.compose.component.MiuixPageHost
import com.absinthe.libchecker.ui.compose.component.adaptiveVerticalScroll
import com.absinthe.libchecker.ui.compose.component.isMiuixOs4Supported
import com.absinthe.libchecker.ui.compose.util.WideContentBox
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.SPUtils
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.Album
import top.yukonga.miuix.kmp.icon.glass.NotesFill
import top.yukonga.miuix.kmp.icon.glass.Search
import top.yukonga.miuix.kmp.icon.glass.Theme
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Appearance settings page hosted in [com.absinthe.libchecker.domain.settings.ui.AppearanceActivity]. */
@Composable
fun AppearanceScreen(
  settingsViewModel: SettingsViewModel,
  onBack: () -> Unit,
  listState: LazyListState = rememberLazyListState()
) {
  val context = LocalContext.current
  val activity = context as? AppCompatActivity
  val appearance = LocalHomeAppearance.current
  val scrollBehavior = MiuixScrollBehavior()
  var darkMode by remember { mutableStateOf(appearance.darkMode) }
  var amoled by remember { mutableStateOf(appearance.amoledTheme) }
  var colorfulIcon by remember { mutableStateOf(GlobalValues.isColorfulIcon) }
  var useCustomFont by remember { mutableStateOf(appearance.useCustomFont) }
  var bottomBarSearch by remember { mutableStateOf(appearance.bottomBarSearch) }
  var floatingNavBar by remember { mutableStateOf(appearance.floatingNavBar) }
  var blurDesign by remember { mutableStateOf(appearance.blurDesign) }
  var legacySnapshotDetail by remember { mutableStateOf(SPUtils.getValue(Constants.PREF_LEGACY_SNAPSHOT_DETAIL, false)) }

  val darkModeLabels = stringArrayResource(R.array.list_dark_mode)
  val darkModeValues = stringArrayResource(R.array.list_dark_mode_value)
  val languageLabels = stringArrayResource(R.array.language)
  val languageValues = stringArrayResource(R.array.language_value)
  val homeBottomPadding = LocalHomeBottomPadding.current

  MiuixPageHost {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = MiuixTheme.colorScheme.surface,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      topBar = {
        AdaptiveTopAppBar(
          title = stringResource(R.string.pref_group_appearance),
          scrollBehavior = scrollBehavior,
          navigationIcon = {
            AdaptiveBackButton(onClick = onBack)
          }
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
              Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                AdaptiveDropdownPreference(
                  items = darkModeLabels.toList(),
                  selectedIndex = darkModeValues.indexOf(darkMode).coerceAtLeast(0),
                  title = stringResource(R.string.dark_mode),
                  startAction = { SettingsIcon(R.drawable.ic_blur) },
                  onSelectedIndexChange = { index ->
                    darkMode = darkModeValues[index]
                    val nightMode = settingsViewModel.selectDarkMode(darkMode)
                    appearance.darkMode = darkMode
                    AppCompatDelegate.setDefaultNightMode(nightMode)
                  }
                )
                SwitchPreference(
                  checked = amoled,
                  onCheckedChange = { enabled ->
                    amoled = enabled
                    GlobalValues.isAmoledTheme = enabled
                    appearance.amoledTheme = enabled
                  },
                  title = stringResource(R.string.amoled_theme),
                  summary = stringResource(R.string.amoled_theme_summary),
                  startAction = { SettingsIcon(R.drawable.ic_blur) }
                )
                if (!OsUtils.atLeastT()) {
                  AdaptiveDropdownPreference(
                    items = languageLabels.toList(),
                    selectedIndex = languageValues.indexOf(SPUtils.getValue(Constants.PREF_LOCALE, "SYSTEM"))
                      .coerceAtLeast(0),
                    title = stringResource(R.string.languages),
                    startAction = { SettingsIcon(R.drawable.ic_language) },
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
                  startAction = { SettingsIcon(R.drawable.ic_blur) }
                )
                SwitchPreference(
                  checked = useCustomFont,
                  onCheckedChange = {
                    useCustomFont = it
                    GlobalValues.isUseCustomFont = it
                    appearance.useCustomFont = it
                  },
                  title = stringResource(R.string.use_custom_font),
                  summary = stringResource(R.string.use_custom_font_summary),
                  startAction = { SettingsIcon(MiuixIcons.Glass.NotesFill) }
                )
                if (isMiuixOs4Supported()) {
                  val uiVersionItems = listOf(
                    stringResource(R.string.miuix_ui_os3),
                    stringResource(R.string.miuix_ui_os4)
                  )
                  AdaptiveDropdownPreference(
                    items = uiVersionItems,
                    selectedIndex = if (appearance.miuixOs4) 1 else 0,
                    title = stringResource(R.string.miuix_ui_version),
                    summary = stringResource(R.string.miuix_ui_version_summary),
                    startAction = { SettingsIcon(MiuixIcons.Glass.Theme) },
                    onSelectedIndexChange = { index ->
                      val nextOs4 = index == 1
                      GlobalValues.isMiuixOs4 = nextOs4
                      appearance.miuixOs4 = nextOs4
                    }
                  )
                }
                if (!appearance.miuixOs4 || !isMiuixOs4Supported()) {
                  SwitchPreference(
                    checked = floatingNavBar,
                    onCheckedChange = {
                      floatingNavBar = it
                      GlobalValues.isFloatingNavBar = it
                      appearance.floatingNavBar = it
                    },
                    title = stringResource(R.string.floating_nav_bar),
                    summary = stringResource(R.string.floating_nav_bar_summary),
                    startAction = { SettingsIcon(R.drawable.ic_floating_bar) }
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
                      startAction = { SettingsIcon(R.drawable.ic_blur) }
                    )
                  }
                }
                SwitchPreference(
                  checked = bottomBarSearch,
                  onCheckedChange = {
                    bottomBarSearch = it
                    GlobalValues.isBottomBarSearch = it
                    appearance.bottomBarSearch = it
                  },
                  title = stringResource(R.string.bottom_bar_search),
                  summary = stringResource(R.string.bottom_bar_search_summary),
                  startAction = { SettingsIcon(MiuixIcons.Glass.Search) }
                )
                SwitchPreference(
                  checked = legacySnapshotDetail,
                  onCheckedChange = {
                    legacySnapshotDetail = it
                    SPUtils.putValue(Constants.PREF_LEGACY_SNAPSHOT_DETAIL, it)
                  },
                  title = stringResource(R.string.legacy_snapshot_detail),
                  summary = stringResource(R.string.legacy_snapshot_detail_summary),
                  startAction = { SettingsIcon(MiuixIcons.Glass.Album) }
                )
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
}
