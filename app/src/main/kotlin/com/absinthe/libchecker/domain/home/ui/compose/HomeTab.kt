package com.absinthe.libchecker.domain.home.ui.compose

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.absinthe.libchecker.R
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.glass.Album as GlassAlbum
import top.yukonga.miuix.kmp.icon.glass.GridView as GlassGridView
import top.yukonga.miuix.kmp.icon.glass.ListView as GlassListView
import top.yukonga.miuix.kmp.icon.glass.Settings as GlassSettings

/** Top level destinations of the home shell. */
enum class HomeTab(
  @StringRes val titleRes: Int,
  val icon: ImageVector,
  val glassIcon: ImageVector
) {
  APP_LIST(R.string.title_app_list, MiuixIcons.ListView, MiuixIcons.Glass.GlassListView),
  STATISTICS(R.string.title_statistics, MiuixIcons.GridView, MiuixIcons.Glass.GlassGridView),
  SNAPSHOT(R.string.title_snapshot, MiuixIcons.Album, MiuixIcons.Glass.GlassAlbum),
  SETTINGS(R.string.title_settings, MiuixIcons.Settings, MiuixIcons.Glass.GlassSettings)
}
