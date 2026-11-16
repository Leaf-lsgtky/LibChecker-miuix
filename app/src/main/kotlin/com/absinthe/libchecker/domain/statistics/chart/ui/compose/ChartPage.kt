@file:Suppress("ktlint:standard:function-naming")

package com.absinthe.libchecker.domain.statistics.chart.ui.compose

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.absinthe.libchecker.services.IWorkerService
import com.absinthe.libchecker.services.WorkerService
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChartPage(onBack: () -> Unit) {
  val context = LocalContext.current
  DisposableEffect(context) {
    val connection = object : ServiceConnection {
      override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if (service?.pingBinder() == true) IWorkerService.Stub.asInterface(service)?.initFeatures()
      }

      override fun onServiceDisconnected(name: ComponentName?) = Unit
    }
    val bound = context.bindService(
      Intent(context, WorkerService::class.java).setPackage(context.packageName),
      connection,
      Context.BIND_AUTO_CREATE
    )
    onDispose { if (bound) context.unbindService(connection) }
  }
  ChartScreen(viewModel = koinViewModel(), onBack = onBack)
}
