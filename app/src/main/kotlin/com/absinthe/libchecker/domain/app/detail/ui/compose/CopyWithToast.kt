package com.absinthe.libchecker.domain.app.detail.ui.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.absinthe.libchecker.compat.VersionCompat

/** Copies [text] to the clipboard and shows the platform "copied" confirmation. */
internal fun Context.copyTextWithToast(text: String) {
  val clipboard = getSystemService(ClipboardManager::class.java)
  clipboard?.setPrimaryClip(ClipData.newPlainText("", text))
  VersionCompat.showCopiedOnClipboardToast(this)
}
