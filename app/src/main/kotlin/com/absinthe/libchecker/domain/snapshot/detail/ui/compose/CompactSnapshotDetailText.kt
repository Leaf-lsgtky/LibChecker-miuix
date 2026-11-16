package com.absinthe.libchecker.domain.snapshot.detail.ui.compose

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.em
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.ui.compose.util.toAnnotatedString

internal data class CompactSnapshotMetric(
  val text: AnnotatedString,
  val delta: String? = null,
  val percentage: String? = null
)

internal data class CompactSnapshotDetailText(
  val headlineDelta: AnnotatedString?,
  val metrics: List<CompactSnapshotMetric>
)

internal fun buildCompactSnapshotDetailText(
  item: SnapshotDetailItemDisplayData,
  classCountLabel: String
): CompactSnapshotDetailText {
  val source = item.extra.toAnnotatedString()
  if (item.item.itemType != NATIVE && item.item.itemType != DEX) {
    return CompactSnapshotDetailText(
      headlineDelta = null,
      metrics = if (source.isEmpty()) emptyList() else listOf(CompactSnapshotMetric(source.withTechnicalBreaks()))
    )
  }

  val lines = splitTrimmedAnnotatedLines(source)
  val metrics = mutableListOf<CompactSnapshotMetric>()
  var index = 0
  while (index < lines.size) {
    val current = stripByteParenthetical(lines[index])
    // Class counts come from the shared, intentionally untranslated "class/classes" plural.
    // Only the compact DEX presentation replaces it with a localized leading label.
    val isClassCount = item.item.itemType == DEX && classCountSuffix.containsMatchIn(current.text)
    val metricText = if (isClassCount) {
      buildAnnotatedString {
        append(classCountLabel)
        append(' ')
        append(current.withoutClassCountSuffix())
      }
    } else {
      current.withSmallerSizeUnits(0.88f)
    }
    val next = lines.getOrNull(index + 1)?.text
    if (
      current.text.contains(" → ") &&
      next != null &&
      (next.startsWith('+') || next.startsWith('-') || next.startsWith('−'))
    ) {
      val deltaText = stripByteParenthetical(next).let {
        if (isClassCount) it.replace(classCountSuffix, "") else it
      }
      // The formatter appends ", " before the percentage. Keep localized numbers intact.
      val separator = deltaText.lastIndexOf(", ")
      val hasPercentage = separator >= 0 && deltaText.endsWith('%')
      metrics += CompactSnapshotMetric(
        text = metricText,
        delta = if (hasPercentage) deltaText.substring(0, separator) else deltaText,
        percentage = if (hasPercentage) deltaText.substring(separator + 2) else null
      )
      index += 2
    } else {
      metrics += CompactSnapshotMetric(metricText)
      index++
    }
  }
  return CompactSnapshotDetailText(
    headlineDelta = metrics.firstOrNull()?.delta?.let { AnnotatedString(it).withSmallerSizeUnits(0.75f) },
    metrics = metrics
  )
}

private val classCountSuffix = Regex("\\s+class(?:es)?\\b")

// Match the unit after a number, including localized unit names and bidi formatting characters.
private val sizeUnit = Regex(
  "\\p{Nd}[\\s\\u00A0\\u202F\\u200E\\u200F\\u061C]*" +
    "([\\p{L}.]+)(?=[\\s\\u200E\\u200F\\u061C\\u202C]*(?:→|$))"
)

private fun AnnotatedString.withSmallerSizeUnits(scale: Float): AnnotatedString = buildAnnotatedString {
  append(this@withSmallerSizeUnits)
  sizeUnit.findAll(this@withSmallerSizeUnits.text).forEach { match ->
    val unit = requireNotNull(match.groups[1])
    addStyle(SpanStyle(fontSize = scale.em), unit.range.first, unit.range.last + 1)
  }
}

private fun AnnotatedString.withoutClassCountSuffix(): AnnotatedString = buildAnnotatedString {
  var start = 0
  classCountSuffix.findAll(this@withoutClassCountSuffix.text).forEach { match ->
    append(this@withoutClassCountSuffix.subSequence(start, match.range.first))
    start = match.range.last + 1
  }
  append(this@withoutClassCountSuffix.subSequence(start, this@withoutClassCountSuffix.length))
}

internal fun AnnotatedString.withTechnicalBreaks(): AnnotatedString = buildAnnotatedString {
  var start = 0
  this@withTechnicalBreaks.text.forEachIndexed { index, character ->
    if (character == '.' || character == '_' || character == '/') {
      append(this@withTechnicalBreaks.subSequence(start, index + 1))
      if (index < this@withTechnicalBreaks.lastIndex) append('\u200B')
      start = index + 1
    }
  }
  append(this@withTechnicalBreaks.subSequence(start, this@withTechnicalBreaks.length))
}
