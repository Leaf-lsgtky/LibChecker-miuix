package com.absinthe.libchecker.domain.statistics.chart.ui.compose

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.widget.FrameLayout
import com.absinthe.libchecker.domain.statistics.chart.presentation.ChartRenderRequest
import com.absinthe.libchecker.domain.statistics.chart.presentation.ChartViewModel
import com.absinthe.libchecker.domain.statistics.chart.source.ChartDataSourcePlan
import com.absinthe.libchecker.domain.statistics.chart.source.IntegerFormatter
import com.absinthe.libchecker.domain.statistics.chart.ui.ChartDataRenderer
import com.absinthe.libchecker.domain.statistics.chart.ui.animateEntrance
import com.absinthe.libchecker.domain.statistics.chart.ui.view.AsymmetricBarChartRenderer
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import kotlinx.coroutines.CoroutineScope

/**
 * Bridges the existing chart data sources to a Compose hosted [ViewGroup].
 *
 * The chart rendering pipeline (classification, progress, entrance animation) stays in the shared
 * [ChartDataRenderer]; this controller only builds and configures the chart views.
 */
internal class ChartHostController(
  private val context: Context,
  private val scope: CoroutineScope,
  private val viewModel: ChartViewModel,
  private val onProgress: (Int) -> Unit
) {

  private var host: ViewGroup? = null
  private var renderer: ChartDataRenderer? = null

  fun attach(host: ViewGroup) {
    if (this.host === host) {
      return
    }
    this.host = host
    renderer = ChartDataRenderer(scope, host, onProgress)
  }

  fun render(request: ChartRenderRequest) {
    val host = host ?: return
    val renderer = renderer ?: return
    val plan = viewModel.createChartDataSourcePlan(request)
    when (plan) {
      is ChartDataSourcePlan.Pie -> {
        viewModel.setLoadingProgress(plan.initialLoadingProgress, allowDecrease = true)
        renderer.render(
          newChartView = createPieChart(),
          fillChart = plan.dataSource::fillChartView
        ) { committed ->
          committed.animateEntrance()
          if (committed.parent == null) {
            host.addView(committed)
          }
        }
      }

      is ChartDataSourcePlan.Bar -> {
        viewModel.setLoadingProgress(plan.initialLoadingProgress, allowDecrease = true)
        renderer.render(
          newChartView = createBarChart(),
          fillChart = plan.dataSource::fillChartView
        ) { committed ->
          committed.animateEntrance()
          if (committed.parent == null) {
            host.addView(committed)
          }
        }
      }
    }
  }

  private fun createPieChart(): PieChart {
    val colorOnSurface = context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface)
    return PieChart(context).apply {
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
      )
      dragDecelerationFrictionCoef = 0.95f
      description.isEnabled = false
      legend.apply {
        verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
        orientation = Legend.LegendOrientation.HORIZONTAL
        textColor = colorOnSurface
        xEntrySpace = 7f
        yEntrySpace = 0f
        isWordWrapEnabled = true
      }
      isUsePercentValuesEnabled = true
      setExtraOffsets(32f, 0f, 32f, 0f)
      entryLabelColor = colorOnSurface
      entryLabelTextSize = 11f
      holeColor = Color.TRANSPARENT
    }
  }

  private fun createBarChart(): HorizontalBarChart {
    val colorOnSurface = context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface)
    return HorizontalBarChart(context).apply {
      renderer = AsymmetricBarChartRenderer(this)
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
      )
      description.isEnabled = false
      legend.isEnabled = false
      isDragEnabled = true
      isDrawBordersEnabled = false
      isDrawGridBackgroundEnabled = false
      isFitBarsEnabled = true
      xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        isDrawGridLinesEnabled = false
        granularity = 1f
        textSize = 10f
        textColor = colorOnSurface
        isDrawLabelsEnabled = true
      }
      axisLeft.apply {
        valueFormatter = IntegerFormatter()
        isDrawGridLinesEnabled = false
        isDrawZeroLineEnabled = false
        textColor = colorOnSurface
      }
      axisRight.apply {
        valueFormatter = IntegerFormatter()
        isDrawGridLinesEnabled = false
        isDrawZeroLineEnabled = false
        textColor = colorOnSurface
      }
      maxVisibleCount = 50
      isDrawMarkersEnabled = false
    }
  }
}
