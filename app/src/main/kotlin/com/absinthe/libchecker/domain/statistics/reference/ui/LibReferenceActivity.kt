package com.absinthe.libchecker.domain.statistics.reference.ui

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.domain.home.presentation.RecentVisitsViewModel
import com.absinthe.libchecker.domain.home.recent.RecentVisit
import com.absinthe.libchecker.domain.statistics.reference.presentation.LibReferenceViewModel
import com.absinthe.libchecker.domain.statistics.reference.ui.compose.LibReferenceDetailScreen
import com.absinthe.libchecker.ui.base.BaseComposeActivity
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import org.koin.androidx.viewmodel.ext.android.viewModel

const val EXTRA_REF_NAME = "REF_NAME"
const val EXTRA_REF_LABEL = "REF_LABEL"
const val EXTRA_REF_TYPE = "REF_TYPE"
const val EXTRA_REF_LIST = "REF_LIST"

class LibReferenceActivity : BaseComposeActivity() {

  private val viewModel: LibReferenceViewModel by viewModel()
  private val recentVisitsViewModel: RecentVisitsViewModel by viewModel()

  private var recordVisit = true
  private var refName: String? = null
  private var refLabel: String? = null
  private var refType: Int = NATIVE
  private var refList: Array<String>? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    recordVisit = savedInstanceState == null
    readIntentExtras()
    if (refName == null) {
      finish()
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    recordVisit = true
    setIntent(intent)
    readIntentExtras()
  }

  private fun readIntentExtras() {
    refName = intent.extras?.getString(EXTRA_REF_NAME)
    refLabel = intent.extras?.getString(EXTRA_REF_LABEL)
    refType = intent.extras?.getInt(EXTRA_REF_TYPE) ?: NATIVE
    refList = intent.extras?.getStringArray(EXTRA_REF_LIST)
  }

  private fun resolveTarget(packageName: String): Pair<String, Int> {
    val name = refName ?: packageName
    return when {
      refType == ACTION -> {
        val target = viewModel.getActionTarget(packageName)
        (target?.first ?: packageName) to (target?.second ?: ACTION)
      }

      refList != null && refType == PROVIDER -> {
        val providerName = refList
          ?.find { it.startsWith("$packageName|") }
          ?.substringAfter("|")
        (providerName ?: name) to refType
      }

      else -> name to refType
    }
  }

  @Composable
  override fun Content() {
    val name = refName
    if (name == null) {
      return
    }

    LaunchedEffect(Unit) {
      viewModel.libRefListFlow.collect {
        if (recordVisit) {
          recentVisitsViewModel.record(
            RecentVisit(name, refType, refLabel, refList?.toList())
          )
          recordVisit = false
        }
      }
    }

    LibReferenceDetailScreen(
      viewModel = viewModel,
      refName = name,
      refLabel = refLabel,
      refType = refType,
      refPackages = refList?.map { it.substringBefore("|") },
      onBack = { finish() },
      onOpenApp = { item ->
        val (targetName, targetType) = resolveTarget(item.packageName)
        launchDetailPage(item = item, refName = targetName, refType = targetType)
      }
    )
  }
}
