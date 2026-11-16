package com.absinthe.libchecker.domain.statistics.reference.ui.compose

import android.content.pm.PackageInfo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.reference.model.buildLibReferenceDemoCatalog
import com.absinthe.libchecker.domain.statistics.reference.model.buildLibReferenceMenuBottomSheetState
import com.absinthe.libchecker.domain.statistics.reference.presentation.LibReferenceViewModel
import com.absinthe.libchecker.domain.statistics.reference.ui.view.LibReferenceTreemapView
import com.absinthe.libchecker.ui.app.MenuOptionItem
import com.absinthe.libchecker.ui.compose.util.sheetContentSafePadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.GridView
import top.yukonga.miuix.kmp.icon.glass.ListView
import top.yukonga.miuix.kmp.icon.glass.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReferenceMenuSheet(
  show: Boolean,
  viewModel: LibReferenceViewModel,
  treemapEnabled: Boolean,
  onToggleTreemap: () -> Unit,
  onDismiss: (optionsChanged: Boolean) -> Unit
) {
  val context = LocalContext.current
  val colorfulRuleIcon by viewModel.colorfulRuleIconChanges.collectAsStateWithLifecycle(
    initialValue = viewModel.colorfulRuleIcon
  )
  var initialOptions by remember { mutableIntStateOf(viewModel.getLibReferenceOptions()) }
  var currentOptions by remember { mutableIntStateOf(initialOptions) }
  var demoPackages by remember { mutableStateOf<List<PackageInfo>>(emptyList()) }

  LaunchedEffect(show) {
    if (show) {
      val latest = viewModel.getLibReferenceOptions()
      initialOptions = latest
      currentOptions = latest
      if (demoPackages.isEmpty()) {
        demoPackages = withContext(Dispatchers.IO) {
          runCatching {
            context.packageManager.getInstalledPackages(0)
              .asSequence()
              .take(24)
              .toList()
          }.getOrDefault(emptyList())
        }
      }
    }
  }

  val menuState = remember(currentOptions, colorfulRuleIcon, demoPackages) {
    buildLibReferenceMenuBottomSheetState(
      currentOptions = currentOptions,
      colorfulRuleIcon = colorfulRuleIcon,
      demoIconPackages = demoPackages
    )
  }

  val colorScheme = MiuixTheme.colorScheme
  val dismissWithDiff = {
    onDismiss(viewModel.getLibReferenceOptions() != initialOptions)
  }

  WindowBottomSheet(
    show = show,
    title = stringResource(R.string.advanced_menu),
    endAction = {
      IconButton(onClick = onToggleTreemap) {
        Icon(
          imageVector = if (treemapEnabled) MiuixIcons.Glass.ListView else MiuixIcons.Glass.GridView,
          contentDescription = stringResource(
            if (treemapEnabled) {
              R.string.lib_reference_switch_to_list
            } else {
              R.string.lib_reference_switch_to_treemap
            }
          ),
          tint = colorScheme.onSurface
        )
      }
    },
    onDismissRequest = dismissWithDiff
  ) {
    val allDemoSlots = remember(demoPackages) {
      buildLibReferenceDemoCatalog(demoIconPackages = demoPackages)
    }
    var lastNonEmptyDemoItems by remember { mutableStateOf(menuState.demoItems) }
    if (menuState.demoItems.isNotEmpty()) {
      lastNonEmptyDemoItems = menuState.demoItems
    }
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(bottom = 16.dp)
        .sheetContentSafePadding()
    ) {
      val foldSizeSpec = folmeSpring(
        damping = 0.9f,
        response = 0.36f,
        visibilityThreshold = IntSize.VisibilityThreshold
      )
      val foldOffsetSpec = folmeSpring(
        damping = 0.9f,
        response = 0.36f,
        visibilityThreshold = IntOffset.VisibilityThreshold
      )
      AnimatedVisibility(
        visible = menuState.demoItems.isNotEmpty(),
        enter = expandVertically(
          animationSpec = foldSizeSpec,
          expandFrom = Alignment.Top
        ) + fadeIn(tween(durationMillis = 180, easing = FastOutSlowInEasing)),
        exit = shrinkVertically(
          animationSpec = foldSizeSpec,
          shrinkTowards = Alignment.Top
        ) + fadeOut(tween(durationMillis = 150, easing = FastOutSlowInEasing))
      ) {
        val displayDemoItems = if (menuState.demoItems.isNotEmpty()) {
          menuState.demoItems
        } else {
          lastNonEmptyDemoItems
        }
        val activeDemoByType = remember(displayDemoItems) {
          displayDemoItems.associateBy { it.type }
        }
        Column(modifier = Modifier.fillMaxWidth()) {
          AnimatedContent(
            targetState = treemapEnabled,
            transitionSpec = {
              (expandVertically(
                animationSpec = foldSizeSpec,
                expandFrom = Alignment.Top
              ) + slideInVertically(
                animationSpec = foldOffsetSpec,
                initialOffsetY = { if (targetState) -it / 5 else it / 5 }
              ) + fadeIn(tween(durationMillis = 200, easing = FastOutSlowInEasing)))
                .togetherWith(
                  shrinkVertically(
                    animationSpec = foldSizeSpec,
                    shrinkTowards = Alignment.Top
                  ) + slideOutVertically(
                    animationSpec = foldOffsetSpec,
                    targetOffsetY = { if (targetState) it / 5 else -it / 5 }
                  ) + fadeOut(tween(durationMillis = 160, easing = FastOutSlowInEasing))
                )
                .using(SizeTransform(clip = true) { _, _ -> foldSizeSpec })
            },
            label = "ReferenceMenuPreviewModeTransition"
          ) { isTreemap ->
            if (isTreemap) {
              AndroidView(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(220.dp),
                factory = { ctx ->
                  LibReferenceTreemapView(ctx, zoomEnabled = false)
                },
                update = { view ->
                  view.colorfulRuleIcon = menuState.colorfulRuleIcon
                  view.submitReferences(
                    displayDemoItems,
                    animate = true,
                    aggregateSmallReferences = false
                  )
                }
              )
            } else {
              val suffix = stringResource(R.string.lib_reference_demo_example_suffix)
              Column(modifier = Modifier.fillMaxWidth()) {
                allDemoSlots.forEach { slot ->
                  key(slot.type) {
                    val activeDemo = activeDemoByType[slot.type]
                    AnimatedVisibility(
                      visible = activeDemo != null,
                      enter = expandVertically(
                        animationSpec = foldSizeSpec,
                        expandFrom = Alignment.Top
                      ) + slideInVertically(
                        animationSpec = foldOffsetSpec,
                        initialOffsetY = { -it / 4 }
                      ) + fadeIn(tween(durationMillis = 180, easing = FastOutSlowInEasing)),
                      exit = shrinkVertically(
                        animationSpec = foldSizeSpec,
                        shrinkTowards = Alignment.Top
                      ) + slideOutVertically(
                        animationSpec = foldOffsetSpec,
                        targetOffsetY = { -it / 4 }
                      ) + fadeOut(tween(durationMillis = 150, easing = FastOutSlowInEasing))
                    ) {
                      ReferenceRow(
                        reference = activeDemo ?: slot,
                        labelSuffix = suffix,
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(bottom = 8.dp),
                        onClick = {}
                      )
                    }
                  }
                }
              }
            }
          }
          HorizontalDivider(
            modifier = Modifier.padding(
              top = if (treemapEnabled) 10.dp else 2.dp,
              bottom = 10.dp
            )
          )
        }
      }

      FlowRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        menuState.options.forEach { item ->
          ReferenceOptionChip(
            item = item,
            onClick = {
              currentOptions = viewModel.setLibReferenceOption(item.option, !item.isChecked)
            }
          )
        }
      }
    }
  }
}

@Composable
private fun ReferenceOptionChip(
  item: MenuOptionItem,
  onClick: () -> Unit
) {
  val colorScheme = MiuixTheme.colorScheme
  val shape = RoundedCornerShape(10.dp)
  val containerColor = if (item.isChecked) {
    colorScheme.primaryContainer
  } else {
    colorScheme.surfaceContainerHigh
  }
  val contentColor = if (item.isChecked) {
    colorScheme.onPrimaryContainer
  } else {
    colorScheme.onSurfaceVariantSummary
  }
  val border = if (item.isChecked) {
    BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.45f))
  } else {
    BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.25f))
  }

  Surface(
    shape = shape,
    color = containerColor,
    border = border,
    modifier = Modifier
      .clip(shape)
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      if (item.isChecked) {
        Icon(
          imageVector = MiuixIcons.Glass.Ok,
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier.size(16.dp)
        )
      }
      Text(
        text = stringResource(item.labelRes),
        style = MiuixTheme.textStyles.body2,
        fontWeight = if (item.isChecked) FontWeight.Medium else FontWeight.Normal,
        color = contentColor
      )
    }
  }
}
