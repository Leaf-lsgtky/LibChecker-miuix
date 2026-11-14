package com.absinthe.libchecker.domain.home.ui.compose

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.absinthe.libchecker.R
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Settings

/** Top level destinations of the home shell. */
enum class HomeTab(
  @StringRes val titleRes: Int,
  val icon: ImageVector
) {
  APP_LIST(R.string.title_app_list, MiuixIcons.ListView),
  STATISTICS(R.string.title_statistics, MiuixIcons.GridView),
  SNAPSHOT(R.string.title_snapshot, MiuixIcons.Album),
  SETTINGS(R.string.title_settings, MiuixIcons.Settings)
}