package com.absinthe.libchecker.domain.snapshot.list.model

import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem

/**
 * Loaded state of the snapshot tab, kept by [com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel].
 *
 * A navigation entry drops out of the composition once two opaque pages cover it, which discards
 * every `remember` value while the entry-scoped view model survives. Holding the dashboard here
 * keeps the tab from re-reading timestamps, re-running the snapshot comparison and snapping back to
 * the newest time node when the user returns from a deeper page.
 */
class SnapshotHomeScreenState {
  var timeStamps: List<TimeStampItem> = emptyList()
  var selectedTimestamp: Long = 0L
  var items: List<SnapshotDiffItem> = emptyList()
  var renderState: SnapshotListRenderState = SnapshotListRenderState()
  var snapshotCount: Int = 0
  var appCount: Int = 0
  var systemProps: List<SnapshotSystemPropDisplayData> = emptyList()
  var loading: Boolean = true
  var initialized: Boolean = false
}
