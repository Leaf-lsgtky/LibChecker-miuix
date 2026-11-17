package com.absinthe.libchecker.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import java.lang.ref.WeakReference
import timber.log.Timber

/**
 * Thin wrapper over the platform toast.
 *
 * <pre>
 * author : Absinthe
 * time : 2020/08/12
 * </pre>
 */
object Toasty {
  private val handler = Handler(Looper.getMainLooper())
  private var toastRef: WeakReference<Toast>? = null

  @AnyThread
  fun showShort(context: Context, message: String) {
    handler.post { show(context, message, Toast.LENGTH_SHORT) }
  }

  @AnyThread
  fun showShort(context: Context, @StringRes res: Int) {
    showShort(context, context.getString(res))
  }

  @AnyThread
  fun showLong(context: Context, message: String) {
    handler.post { show(context, message, Toast.LENGTH_LONG) }
  }

  @AnyThread
  fun showLong(context: Context, @StringRes res: Int) {
    showLong(context, context.getString(res))
  }

  @MainThread
  private fun show(context: Context, message: String, duration: Int) {
    // Drop the toast that is still on screen so a fresh message is not queued behind it.
    toastRef?.get()?.cancel()

    val toast = Toast.makeText(context.applicationContext ?: context, message, duration)
    toastRef = WeakReference(toast)
    toast.show()
  }
}

@AnyThread
fun Context.showToast(message: String) {
  Toasty.showShort(this, message)
}

@AnyThread
fun Context.showToast(@StringRes res: Int) {
  Timber.d("showToast: ${getString(res)}")
  Toasty.showShort(this, res)
}
