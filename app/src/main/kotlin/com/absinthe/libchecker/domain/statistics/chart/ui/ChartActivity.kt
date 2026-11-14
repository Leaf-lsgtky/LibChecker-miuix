package com.absinthe.libchecker.domain.statistics.chart.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.compose.runtime.Composable
import com.absinthe.libchecker.domain.statistics.chart.presentation.ChartViewModel
import com.absinthe.libchecker.domain.statistics.chart.ui.compose.ChartScreen
import com.absinthe.libchecker.services.IWorkerService
import com.absinthe.libchecker.services.WorkerService
import com.absinthe.libchecker.ui.base.BaseComposeActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChartActivity : BaseComposeActivity() {

  private val viewModel: ChartViewModel by viewModel()
  private var isWorkerServiceBound = false
  private var workerBinder: IWorkerService? = null
  private val workerServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
      if (service?.pingBinder() == true) {
        workerBinder = IWorkerService.Stub.asInterface(service)
        workerBinder?.initFeatures()
      }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
      workerBinder = null
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isWorkerServiceBound = bindService(
      Intent(this, WorkerService::class.java).apply {
        setPackage(packageName)
      },
      workerServiceConnection,
      BIND_AUTO_CREATE
    )
  }

  override fun onDestroy() {
    if (isWorkerServiceBound) {
      unbindService(workerServiceConnection)
      isWorkerServiceBound = false
    }
    workerBinder = null
    super.onDestroy()
  }

  @Composable
  override fun Content() {
    ChartScreen(
      viewModel = viewModel,
      onBack = { onBackPressedDispatcher.onBackPressed() }
    )
  }
}
