package com.absinthe.libchecker.domain.snapshot.detail.model

import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource

/**
 * Loaded state of one snapshot detail page, kept by
 * [com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel].
 *
 * The page loses its `remember` values whenever two opaque pages cover its navigation entry, but
 * the entry-scoped view model survives. Caching the payload with its built sections keeps a return
 * from a deeper page from flashing the skeleton and rebuilding every rule lookup.
 */
class SnapshotDetailScreenState {
  var payload: SnapshotDiffItem? = null
  var title: SnapshotTitleDisplayData? = null
  var sections: List<SnapshotDetailSection> = emptyList()
  var iconSource: SnapshotPackageIconSource? = null

  /** Color key of [sections]; a theme color change rebuilds the diff highlight spans. */
  var colorKey: Int = 0
}
