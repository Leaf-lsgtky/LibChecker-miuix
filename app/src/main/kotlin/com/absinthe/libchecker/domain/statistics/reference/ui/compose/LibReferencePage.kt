@file:Suppress("ktlint:standard:function-naming")

package com.absinthe.libchecker.domain.statistics.reference.ui.compose

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.domain.home.presentation.RecentVisitsViewModel
import com.absinthe.libchecker.domain.home.recent.RecentVisit
import com.absinthe.libchecker.domain.statistics.reference.presentation.LibReferenceViewModel
import com.absinthe.libchecker.domain.statistics.reference.ui.EXTRA_REF_LABEL
import com.absinthe.libchecker.domain.statistics.reference.ui.EXTRA_REF_LIST
import com.absinthe.libchecker.domain.statistics.reference.ui.EXTRA_REF_NAME
import com.absinthe.libchecker.domain.statistics.reference.ui.EXTRA_REF_TYPE
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LibReferencePage(intent: Intent, onBack: () -> Unit) {
  val activity = LocalContext.current as FragmentActivity
  val viewModel: LibReferenceViewModel = koinViewModel()
  val recentVisits: RecentVisitsViewModel = koinViewModel()
  val name = intent.getStringExtra(EXTRA_REF_NAME)
  val label = intent.getStringExtra(EXTRA_REF_LABEL)
  val type = intent.getIntExtra(EXTRA_REF_TYPE, NATIVE)
  val packages = intent.getStringArrayExtra(EXTRA_REF_LIST)
  var recordVisit by rememberSaveable(intent) { mutableStateOf(true) }
  LaunchedEffect(intent) {
    if (name == null) {
      onBack()
    } else {
      viewModel.libRefListFlow.collect {
        if (recordVisit) {
          recentVisits.record(RecentVisit(name, type, label, packages?.toList()))
          recordVisit = false
        }
      }
    }
  }
  if (name == null) return
  LibReferenceDetailScreen(
    viewModel = viewModel,
    refName = name,
    refLabel = label,
    refType = type,
    refPackages = packages?.map { it.substringBefore("|") },
    onBack = onBack,
    onOpenApp = { item ->
      val target = when {
        type == ACTION -> viewModel.getActionTarget(item.packageName) ?: (item.packageName to ACTION)
        packages != null && type == PROVIDER -> {
          (packages.find { it.startsWith("${item.packageName}|") }?.substringAfter("|") ?: name) to type
        }
        else -> name to type
      }
      activity.launchDetailPage(item, target.first, target.second)
    }
  )
}
