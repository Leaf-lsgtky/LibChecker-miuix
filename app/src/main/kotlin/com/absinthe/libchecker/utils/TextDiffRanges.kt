package com.absinthe.libchecker.utils

internal data class TextDiffRanges(
  val oldRanges: List<IntRange>,
  val newRanges: List<IntRange>
)

internal fun calculateTextDiffRanges(oldText: String, newText: String): TextDiffRanges {
  if (oldText == newText) return TextDiffRanges(emptyList(), emptyList())

  val oldCodePoints = oldText.codePoints().toArray()
  val newCodePoints = newText.codePoints().toArray()
  var prefixLength = 0
  while (
    prefixLength < oldCodePoints.size &&
    prefixLength < newCodePoints.size &&
    oldCodePoints[prefixLength] == newCodePoints[prefixLength]
  ) {
    prefixLength++
  }

  var oldEnd = oldCodePoints.size
  var newEnd = newCodePoints.size
  // Protect shared suffixes when lengths differ, then compare the middle by position.
  // Matching a subsequence would move repeated digits between places, e.g. 334 -> 344.
  while (
    oldEnd > prefixLength &&
    newEnd > prefixLength &&
    oldCodePoints[oldEnd - 1] == newCodePoints[newEnd - 1]
  ) {
    oldEnd--
    newEnd--
  }

  return TextDiffRanges(
    oldRanges = changedCodePointRanges(oldCodePoints) { index ->
      index in prefixLength until oldEnd &&
        (index >= newEnd || oldCodePoints[index] != newCodePoints[index])
    },
    newRanges = changedCodePointRanges(newCodePoints) { index ->
      index in prefixLength until newEnd &&
        (index >= oldEnd || newCodePoints[index] != oldCodePoints[index])
    }
  )
}

private inline fun changedCodePointRanges(
  codePoints: IntArray,
  isChanged: (Int) -> Boolean
): List<IntRange> {
  val ranges = mutableListOf<IntRange>()
  var offset = 0
  var rangeStart = -1
  codePoints.forEachIndexed { index, codePoint ->
    if (isChanged(index)) {
      if (rangeStart < 0) rangeStart = offset
    } else if (rangeStart >= 0) {
      ranges += rangeStart until offset
      rangeStart = -1
    }
    // Android spans and Compose annotations both use UTF-16 offsets.
    offset += Character.charCount(codePoint)
  }
  if (rangeStart >= 0) ranges += rangeStart until offset
  return ranges
}
