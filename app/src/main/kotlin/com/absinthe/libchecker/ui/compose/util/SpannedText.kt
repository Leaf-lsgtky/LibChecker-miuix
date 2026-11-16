package com.absinthe.libchecker.ui.compose.util

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import com.absinthe.libchecker.view.span.DiffHighlightSpan

/**
 * Converts a [Spanned] produced by the view-system diff formatting into an [AnnotatedString] so
 * diff highlights, emphasis markers, relative scaling and bold text keep their styling in Compose.
 */
fun CharSequence.toAnnotatedString(): AnnotatedString {
  val text = toString()
  if (this !is Spanned) {
    return AnnotatedString(text)
  }
  return buildAnnotatedString {
    append(text)
    getSpans(0, length, Any::class.java).forEach { span ->
      val start = getSpanStart(span)
      val end = getSpanEnd(span)
      if (start < 0 || end > text.length || start >= end) {
        return@forEach
      }
      when (span) {
        is ForegroundColorSpan -> {
          addStyle(SpanStyle(color = Color(span.foregroundColor)), start, end)
        }

        is DiffHighlightSpan -> {
          val accent = span.color?.let(::Color)
          addStyle(
            SpanStyle(
              color = accent ?: Color.Unspecified,
              fontWeight = FontWeight.Bold,
              background = (accent ?: Color(0xFF808080)).copy(alpha = 0.18f)
            ),
            start,
            end
          )
        }

        is RelativeSizeSpan -> {
          addStyle(SpanStyle(fontSize = span.sizeChange.em), start, end)
        }

        is StyleSpan -> {
          if (span.style and Typeface.BOLD != 0) {
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
          }
        }
      }
    }
  }
}
