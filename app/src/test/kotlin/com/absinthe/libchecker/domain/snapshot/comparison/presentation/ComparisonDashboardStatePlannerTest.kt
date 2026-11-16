package com.absinthe.libchecker.domain.snapshot.comparison.presentation

import com.absinthe.libchecker.domain.snapshot.comparison.model.ComparisonDashboardLabels
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonInput
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonInputs
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComparisonDashboardStatePlannerTest {

  private val labels = ComparisonDashboardLabels(
    timestampTitle = "Current snapshot timestamp",
    chooseTimestampText = "Click to choose",
    appsCountTitle = "Count of apps in Snapshot",
    defaultAppsCountText = "0"
  )

  @Test
  fun plansUnselectedSideDescriptionFromDefaultDisplayText() {
    val state = ComparisonDashboardStatePlanner.planSideState(
      input = SnapshotComparisonInput(),
      labels = labels,
      formatTimestamp = { "unused" }
    )

    assertEquals("Click to choose", state.timestampText)
    assertEquals("0", state.appsCountText)
    assertEquals(
      "Current snapshot timestamp, Click to choose, Count of apps in Snapshot, 0",
      state.contentDescription
    )
    assertNull(state.dashboardCountTimestamp)
  }

  @Test
  fun plansSnapshotSideDescriptionFromTimestampText() {
    val state = ComparisonDashboardStatePlanner.planSideState(
      input = SnapshotComparisonInput.snapshot(1680000000000L),
      labels = labels,
      formatTimestamp = { "2026-06-11, 20:26:22" }
    )

    assertEquals("2026-06-11, 20:26:22", state.timestampText)
    assertEquals("0", state.appsCountText)
    assertEquals(
      "Current snapshot timestamp, 2026-06-11, 20:26:22, Count of apps in Snapshot, 0",
      state.contentDescription
    )
    assertEquals(1680000000000L, state.dashboardCountTimestamp)
  }

  @Test
  fun updatesSnapshotAppsCountDescription() {
    val state = ComparisonDashboardStatePlanner.planSideState(
      input = SnapshotComparisonInput.snapshot(1680000000000L),
      labels = labels,
      formatTimestamp = { "2026-06-11, 20:26:22" }
    )

    val updatedState = state.withAppsCountText("330", labels)

    assertEquals("330", updatedState.appsCountText)
    assertEquals(
      "Current snapshot timestamp, 2026-06-11, 20:26:22, Count of apps in Snapshot, 330",
      updatedState.contentDescription
    )
  }

  @Test
  fun plansCompleteDashboardAndUpdatesOnlySelectedSide() {
    val state = ComparisonDashboardStatePlanner.planState(
      inputs = SnapshotComparisonInputs(
        left = SnapshotComparisonInput.snapshot(1680000000000L)
      ),
      labels = labels,
      formatTimestamp = { "2026-06-11, 20:26:22" }
    )

    assertEquals("2026-06-11, 20:26:22", state.left.timestampText)
    assertEquals("Click to choose", state.right.timestampText)

    val updatedState = state.withAppsCountText(
      side = SnapshotComparisonSide.LEFT,
      appsCountText = "330",
      labels = labels
    )

    assertEquals("330", updatedState.left.appsCountText)
    assertEquals(
      "Current snapshot timestamp, 2026-06-11, 20:26:22, Count of apps in Snapshot, 330",
      updatedState.left.contentDescription
    )
    assertEquals(state.right, updatedState.right)
  }

  @Test
  fun preservesUnchangedSideAppsCountWhenSelectingOtherSide() {
    val leftLoaded = ComparisonDashboardStatePlanner.planState(
      inputs = SnapshotComparisonInputs(
        left = SnapshotComparisonInput.snapshot(1680000000000L)
      ),
      labels = labels,
      formatTimestamp = { "2026-06-11, 20:26:22" }
    ).withAppsCountText(
      side = SnapshotComparisonSide.LEFT,
      appsCountText = "619",
      labels = labels
    )

    val withRightSelected = ComparisonDashboardStatePlanner.planState(
      inputs = SnapshotComparisonInputs(
        left = SnapshotComparisonInput.snapshot(1680000000000L),
        right = SnapshotComparisonInput.snapshot(1680001000000L)
      ),
      labels = labels,
      formatTimestamp = { ts ->
        if (ts == 1680000000000L) "2026-06-11, 20:26:22" else "2026-06-11, 20:43:02"
      },
      previousState = leftLoaded
    )

    assertEquals("619", withRightSelected.left.appsCountText)
    assertEquals("0", withRightSelected.right.appsCountText)
  }
}
