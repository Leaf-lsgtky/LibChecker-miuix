package com.absinthe.libchecker.domain.app.detail.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.app.detail.action.DetailItemResolver
import com.absinthe.libchecker.domain.app.detail.model.DetailInfoItemDisplay
import com.absinthe.libchecker.domain.app.detail.model.DetailInfoTextStyle
import com.absinthe.libchecker.domain.app.detail.model.LibraryDetailBottomSheetState
import com.absinthe.libchecker.domain.app.detail.model.LibraryDetailHeaderDisplay
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import com.absinthe.libchecker.utils.extensions.openUrlInBrowser
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/** Library detail sheet: header, locale switch and the rule description of the selected library. */
@Composable
fun LibDetailSheet(
  show: Boolean,
  viewModel: DetailViewModel,
  libName: String,
  @LibType type: Int,
  regexName: String?,
  isValidLib: Boolean,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val dialogTitle = remember(libName, type) {
    if (type == ACTION) "< $libName >" else libName
  }
  var state by remember(dialogTitle) {
    mutableStateOf<LibraryDetailBottomSheetState>(
      LibraryDetailBottomSheetState.Loading(dialogTitle)
    )
  }
  var selectedLocale by remember { mutableStateOf(GlobalValues.preferredRuleLanguage) }

  LaunchedEffect(show, libName, type, regexName, isValidLib) {
    if (!show) return@LaunchedEffect
    if (!isValidLib) {
      state = LibraryDetailBottomSheetState.NotFound(dialogTitle, FALLBACK_HEADER)
      return@LaunchedEffect
    }
    state = LibraryDetailBottomSheetState.Loading(dialogTitle)
    runCatching {
      coroutineScope {
        val header = async {
          viewModel.getLibraryDetailDialogHeader(libName, type, isValidLib)
        }
        val content = async {
          viewModel.getLibraryDetailDialogData(
            libName = libName,
            type = type,
            regexName = regexName,
            isValidLib = isValidLib,
            preferredLocale = selectedLocale
          )
        }
        header.await() to content.await()
      }
    }.onSuccess { (header, result) ->
      val displayHeader = header ?: FALLBACK_HEADER
      when (result) {
        is DetailItemResolver.Result.Found -> {
          state = LibraryDetailBottomSheetState.Content(
            title = dialogTitle,
            header = displayHeader,
            content = result.content
          )
        }

        DetailItemResolver.Result.NotFound -> {
          state = LibraryDetailBottomSheetState.NotFound(dialogTitle, displayHeader)
        }
      }
    }
  }

  val current = state
  WindowBottomSheet(
    show = show,
    title = current.title,
    onDismissRequest = onDismiss
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp)
    ) {
      when (current) {
        is LibraryDetailBottomSheetState.Loading -> {
          LcLoadingState(
            modifier = Modifier.height(200.dp),
            message = stringResource(R.string.loading)
          )
        }

        is LibraryDetailBottomSheetState.NotFound -> {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(160.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = stringResource(R.string.not_found),
              style = MiuixTheme.textStyles.body2,
              color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
          }
        }

        is LibraryDetailBottomSheetState.Content -> {
          val locale = current.content.locales.firstOrNull {
            it.localeTag == selectedLocale
          } ?: current.content.locales.first()
          if (current.content.locales.size > 1) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              current.content.locales.forEach { candidate ->
                val selected = candidate.localeTag == locale.localeTag
                Box(
                  modifier = Modifier
                    .squircleBackground(
                      if (selected) {
                        MiuixTheme.colorScheme.onSurface
                      } else {
                        MiuixTheme.colorScheme.surfaceContainerHigh
                      },
                      12.dp
                    )
                    .clickable {
                      selectedLocale = candidate.localeTag
                      GlobalValues.preferredRuleLanguage = candidate.localeTag
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Text(
                    text = candidate.localeName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight(550),
                    color = if (selected) {
                      MiuixTheme.colorScheme.surface
                    } else {
                      MiuixTheme.colorScheme.onSurface
                    }
                  )
                }
              }
            }
          }
          val listState = rememberLazyListState()
          LazyColumn(
            state = listState,
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 520.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
          ) {
            items(locale.items) { item ->
              DetailInfoRow(item = item)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DetailInfoRow(item: DetailInfoItemDisplay) {
  val context = LocalContext.current
  val isTitle = item.textStyle == DetailInfoTextStyle.TITLE
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp)
  ) {
    if (isTitle) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
          model = item.iconRes,
          contentDescription = null,
          modifier = Modifier
            .padding(end = 6.dp)
            .size(16.dp)
        )
        Text(
          text = item.text.toString(),
          fontSize = 13.sp,
          fontWeight = FontWeight(600),
          color = MiuixTheme.colorScheme.onSurface
        )
      }
    } else {
      Row(
        modifier = Modifier.then(
          if (item.linkUrl != null) {
            Modifier.clickable { context.openUrlInBrowser(item.linkUrl) }
          } else {
            Modifier
          }
        ),
        verticalAlignment = Alignment.CenterVertically
      ) {
        AsyncImage(
          model = item.iconRes,
          contentDescription = null,
          modifier = Modifier
            .padding(end = 6.dp)
            .size(16.dp)
        )
        Text(
          text = item.text.toString(),
          fontSize = 13.sp,
          color = MiuixTheme.colorScheme.onSurface
        )
      }
    }
    Spacer(Modifier.height(2.dp))
  }
}

private val FALLBACK_HEADER = LibraryDetailHeaderDisplay(
  iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_sdk_placeholder,
  isSimpleColorIcon = false
)
