package com.absinthe.libchecker.ui.compose.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Centred circular progress with an optional label. */
@Composable
fun LcLoadingState(
  modifier: Modifier = Modifier,
  message: String? = null
) {
  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    CircularProgressIndicator(modifier = Modifier.size(32.dp))
    if (message != null) {
      Spacer(Modifier.height(12.dp))
      Text(
        text = message,
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        textAlign = TextAlign.Center
      )
    }
  }
}

/** Centred empty state with an optional action. */
@Composable
fun LcEmptyState(
  title: String,
  modifier: Modifier = Modifier,
  message: String? = null,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      text = title,
      style = MiuixTheme.textStyles.title4,
      color = MiuixTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center
    )
    if (message != null) {
      Spacer(Modifier.height(8.dp))
      Text(
        text = message,
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        textAlign = TextAlign.Center
      )
    }
    if (actionLabel != null && onAction != null) {
      Spacer(Modifier.height(20.dp))
      Button(onClick = onAction, colors = ButtonDefaults.buttonColorsPrimary()) {
        Text(text = actionLabel)
      }
    }
  }
}
