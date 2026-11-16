package com.absinthe.libchecker.ui.compose.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.absinthe.libchecker.ui.compose.util.horizontalCutoutPadding
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Shimmer brush calibrated for Miuix light and dark surfaces. */
@Composable
fun rememberSkeletonBrush(): Brush {
  val colorScheme = MiuixTheme.colorScheme
  val isDark = colorScheme.surface.luminance() < 0.5f
  val baseAlpha = if (isDark) 0.08f else 0.055f
  val peakAlpha = if (isDark) 0.16f else 0.12f
  val baseColor = colorScheme.onSurface.copy(alpha = baseAlpha)
  val peakColor = colorScheme.onSurface.copy(alpha = peakAlpha)

  val transition = rememberInfiniteTransition(label = "SkeletonShimmer")
  val shift by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1200f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1150, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "SkeletonShift"
  )

  return Brush.linearGradient(
    colors = listOf(baseColor, peakColor, baseColor),
    start = Offset(shift - 380f, 0f),
    end = Offset(shift, 220f)
  )
}

@Composable
fun SkeletonBox(
  modifier: Modifier,
  brush: Brush = rememberSkeletonBrush(),
  shape: Shape = RoundedCornerShape(6.dp)
) {
  Box(
    modifier = modifier
      .clip(shape)
      .background(brush)
  )
}

/** Staggered Miuix card skeleton rows for bottom sheets. */
@Composable
fun SheetCardListSkeleton(
  rowCount: Int = 4,
  modifier: Modifier = Modifier,
  showIcon: Boolean = true,
  twoLine: Boolean = true,
  tallLastCard: Boolean = false
) {
  val brush = rememberSkeletonBrush()
  val cardColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.04f)
  val titleFractions = listOf(0.32f, 0.28f, 0.36f, 0.26f)
  val bodyFractions = listOf(0.68f, 0.56f, 0.78f, 0.88f)

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    repeat(rowCount) { index ->
      val isTall = tallLastCard && index == rowCount - 1
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(color = cardColor),
        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (showIcon) {
            SkeletonBox(
              modifier = Modifier
                .padding(end = 14.dp)
                .size(22.dp),
              brush = brush,
              shape = CircleShape
            )
          }
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            if (twoLine) {
              SkeletonBox(
                modifier = Modifier
                  .fillMaxWidth(titleFractions[index % titleFractions.size])
                  .height(10.dp),
                brush = brush,
                shape = CircleShape
              )
            }
            SkeletonBox(
              modifier = Modifier
                .fillMaxWidth(bodyFractions[index % bodyFractions.size])
                .height(14.dp),
              brush = brush
            )
            if (isTall) {
              SkeletonBox(
                modifier = Modifier
                  .fillMaxWidth(0.72f)
                  .height(14.dp),
                brush = brush
              )
            }
          }
        }
      }
    }
  }
}

/** Skeleton matching [com.absinthe.libchecker.domain.app.detail.ui.compose.LibDetailSheet]. */
@Composable
fun LibDetailSheetSkeleton(
  modifier: Modifier = Modifier,
  showLocaleBar: Boolean = true
) {
  val brush = rememberSkeletonBrush()
  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    if (showLocaleBar) {
      SkeletonBox(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 10.dp)
          .height(38.dp),
        brush = brush,
        shape = CircleShape
      )
    }
    SheetCardListSkeleton(
      rowCount = 4,
      showIcon = true,
      twoLine = true,
      tallLastCard = true
    )
  }
}

/** Compact inline skeleton card shown while SDK insight data is resolving. */
@Composable
fun InsightCardSkeleton(modifier: Modifier = Modifier) {
  val colorScheme = MiuixTheme.colorScheme
  val brush = rememberSkeletonBrush()
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(bottom = 12.dp),
    colors = CardDefaults.defaultColors(
      color = colorScheme.onSurface.copy(alpha = 0.04f)
    ),
    insideMargin = PaddingValues(14.dp)
  ) {
    Column(Modifier.fillMaxWidth()) {
      SkeletonBox(
        modifier = Modifier
          .width(56.dp)
          .height(11.dp),
        brush = brush,
        shape = CircleShape
      )
      Spacer(Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        SkeletonBox(
          modifier = Modifier
            .width(42.dp)
            .height(13.dp),
          brush = brush
        )
        Spacer(Modifier.weight(1f))
        SkeletonBox(
          modifier = Modifier
            .width(64.dp)
            .height(13.dp),
          brush = brush
        )
      }
    }
  }
}

/** Four-column icon grid skeleton for external actions in AppInfoSheet. */
@Composable
fun AppInfoExternalActionsSkeleton(modifier: Modifier = Modifier) {
  val brush = rememberSkeletonBrush()
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(top = 12.dp, bottom = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    repeat(4) {
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        SkeletonBox(
          modifier = Modifier.size(48.dp),
          brush = brush,
          shape = CircleShape
        )
        Spacer(Modifier.height(8.dp))
        SkeletonBox(
          modifier = Modifier
            .width(44.dp)
            .height(11.dp),
          brush = brush,
          shape = CircleShape
        )
      }
    }
  }
}

/** Card-grouped row skeletons for AppDetail tab lists. */
@Composable
fun DetailTabListSkeleton(
  rowCount: Int = 6,
  modifier: Modifier = Modifier
) {
  val colorScheme = MiuixTheme.colorScheme
  val brush = rememberSkeletonBrush()
  val titleWidths = listOf(0.64f, 0.78f, 0.52f, 0.72f, 0.58f, 0.68f)
  val metaWidths = listOf(0.28f, 0.36f, 0.24f, 0.42f, 0.30f, 0.34f)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 4.dp),
    verticalArrangement = Arrangement.spacedBy(1.dp)
  ) {
    repeat(rowCount) { index ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        colors = CardDefaults.defaultColors(
          color = colorScheme.surfaceContainer
        ),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          SkeletonBox(
            modifier = Modifier
              .fillMaxWidth(titleWidths[index % titleWidths.size])
              .height(14.dp),
            brush = brush
          )
          SkeletonBox(
            modifier = Modifier
              .fillMaxWidth(metaWidths[index % metaWidths.size])
              .height(11.dp),
            brush = brush,
            shape = CircleShape
          )
        }
      }
    }
  }
}

/** App list card skeletons mirroring [AppListRow] structure for primary AppList and related screens. */
@Composable
fun AppListSkeleton(
  contentPadding: PaddingValues,
  homeBottomPadding: Dp,
  modifier: Modifier = Modifier,
  showHeaderBanner: Boolean = false,
  rowCount: Int = 7
) {
  val brush = rememberSkeletonBrush()
  val titleWidths = listOf(0.42f, 0.56f, 0.36f, 0.62f, 0.48f, 0.38f, 0.52f)
  val pkgWidths = listOf(0.68f, 0.52f, 0.76f, 0.58f, 0.72f, 0.54f, 0.64f)

  LazyColumn(
    userScrollEnabled = false,
    contentPadding = contentPadding,
    modifier = modifier
      .fillMaxSize()
      .horizontalCutoutPadding()
  ) {
    if (showHeaderBanner) {
      item(key = "header_skeleton") {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
          insideMargin = PaddingValues(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonBox(
              modifier = Modifier
                .padding(end = 12.dp)
                .size(44.dp),
              brush = brush,
              shape = RoundedCornerShape(10.dp)
            )
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              SkeletonBox(
                modifier = Modifier
                  .fillMaxWidth(0.48f)
                  .height(16.dp),
                brush = brush
              )
              SkeletonBox(
                modifier = Modifier
                  .fillMaxWidth(0.72f)
                  .height(12.dp),
                brush = brush,
                shape = CircleShape
              )
            }
            SkeletonBox(
              modifier = Modifier
                .padding(start = 12.dp)
                .width(34.dp)
                .height(20.dp),
              brush = brush,
              shape = CircleShape
            )
          }
        }
      }
    }
    items(count = rowCount, key = { "app_skeleton_$it" }) { index ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp)
          .padding(bottom = 12.dp),
        insideMargin = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          SkeletonBox(
            modifier = Modifier
              .padding(end = 12.dp)
              .size(48.dp),
            brush = brush,
            shape = RoundedCornerShape(12.dp)
          )
          Column(modifier = Modifier.weight(1f)) {
            SkeletonBox(
              modifier = Modifier
                .fillMaxWidth(titleWidths[index % titleWidths.size])
                .height(15.dp),
              brush = brush
            )
            Spacer(Modifier.height(6.dp))
            SkeletonBox(
              modifier = Modifier
                .fillMaxWidth(pkgWidths[index % pkgWidths.size])
                .height(12.dp),
              brush = brush,
              shape = CircleShape
            )
            Spacer(Modifier.height(6.dp))
            SkeletonBox(
              modifier = Modifier
                .fillMaxWidth(0.34f)
                .height(12.dp),
              brush = brush,
              shape = CircleShape
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              SkeletonBox(
                modifier = Modifier
                  .width(46.dp)
                  .height(16.dp),
                brush = brush,
                shape = RoundedCornerShape(4.dp)
              )
              Spacer(Modifier.width(6.dp))
              SkeletonBox(
                modifier = Modifier
                  .fillMaxWidth(0.45f)
                  .height(12.dp),
                brush = brush,
                shape = CircleShape
              )
            }
          }
        }
      }
    }
    item(key = "bottom_spacer") {
      SkeletonBottomSpacer(homeBottomPadding)
    }
  }
}

/** Single snapshot diff card skeleton mirroring [SnapshotDiffRow] right-edge indicator strip and metrics. */
@Composable
fun SnapshotDiffCardSkeleton(
  index: Int,
  brush: Brush,
  modifier: Modifier = Modifier
) {
  val titleWidths = listOf(0.44f, 0.56f, 0.36f, 0.50f, 0.42f)
  val pkgWidths = listOf(0.68f, 0.74f, 0.56f, 0.64f, 0.70f)

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .padding(bottom = 12.dp),
    insideMargin = PaddingValues(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min),
      verticalAlignment = Alignment.CenterVertically
    ) {
      SkeletonBox(
        modifier = Modifier
          .padding(end = 12.dp)
          .size(44.dp),
        brush = brush,
        shape = RoundedCornerShape(10.dp)
      )
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(5.dp)
      ) {
        SkeletonBox(
          modifier = Modifier
            .fillMaxWidth(titleWidths[index % titleWidths.size])
            .height(15.dp),
          brush = brush
        )
        SkeletonBox(
          modifier = Modifier
            .fillMaxWidth(pkgWidths[index % pkgWidths.size])
            .height(12.dp),
          brush = brush,
          shape = CircleShape
        )
        SkeletonBox(
          modifier = Modifier
            .fillMaxWidth(0.62f)
            .height(12.dp),
          brush = brush,
          shape = CircleShape
        )
        SkeletonBox(
          modifier = Modifier
            .fillMaxWidth(0.52f)
            .height(12.dp),
          brush = brush,
          shape = CircleShape
        )
        Row(
          modifier = Modifier.padding(top = 2.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          SkeletonBox(
            modifier = Modifier
              .width(46.dp)
              .height(16.dp),
            brush = brush,
            shape = RoundedCornerShape(4.dp)
          )
          Spacer(Modifier.width(6.dp))
          SkeletonBox(
            modifier = Modifier
              .width(72.dp)
              .height(12.dp),
            brush = brush,
            shape = CircleShape
          )
        }
      }
      SkeletonBox(
        modifier = Modifier
          .padding(start = 10.dp)
          .width(4.dp)
          .fillMaxHeight(),
        brush = brush,
        shape = CircleShape
      )
    }
  }
}

/** Full-page snapshot loading skeleton shown while timestamps initialize. */
@Composable
fun SnapshotInitialSkeleton(
  contentPadding: PaddingValues,
  homeBottomPadding: Dp,
  modifier: Modifier = Modifier,
  rowCount: Int = 5
) {
  val brush = rememberSkeletonBrush()
  LazyColumn(
    userScrollEnabled = false,
    contentPadding = contentPadding,
    modifier = modifier
      .fillMaxSize()
      .horizontalCutoutPadding()
  ) {
    item(key = "snapshot_dashboard_skeleton") {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp)
          .padding(bottom = 12.dp),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            SkeletonBox(
              modifier = Modifier
                .width(88.dp)
                .height(11.dp),
              brush = brush,
              shape = CircleShape
            )
            SkeletonBox(
              modifier = Modifier
                .width(156.dp)
                .height(17.dp),
              brush = brush
            )
            Spacer(Modifier.height(2.dp))
            SkeletonBox(
              modifier = Modifier
                .width(120.dp)
                .height(11.dp),
              brush = brush,
              shape = CircleShape
            )
            SkeletonBox(
              modifier = Modifier
                .width(76.dp)
                .height(17.dp),
              brush = brush
            )
          }
          Column(
            modifier = Modifier.padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            repeat(4) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                SkeletonBox(
                  modifier = Modifier
                    .width(24.dp)
                    .height(11.dp),
                  brush = brush,
                  shape = CircleShape
                )
                SkeletonBox(
                  modifier = Modifier
                    .width(20.dp)
                    .height(5.dp),
                  brush = brush,
                  shape = CircleShape
                )
              }
            }
          }
        }
      }
    }
    items(count = rowCount, key = { "snapshot_diff_skeleton_$it" }) { index ->
      SnapshotDiffCardSkeleton(index = index, brush = brush)
    }
    item(key = "bottom_spacer") {
      SkeletonBottomSpacer(homeBottomPadding)
    }
  }
}

@Composable
private fun SkeletonBottomSpacer(homeBottomPadding: Dp) {
  val bottomModifier = if (homeBottomPadding > 0.dp) {
    Modifier.height(24.dp + homeBottomPadding)
  } else {
    Modifier
      .height(24.dp)
      .navigationBarsPadding()
  }
  Spacer(bottomModifier)
}

/** Skeleton matching [com.absinthe.libchecker.domain.settings.ui.compose.CloudRulesSheet]. */
@Composable
fun CloudRulesSheetSkeleton(modifier: Modifier = Modifier) {
  val brush = rememberSkeletonBrush()
  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 12.dp, bottom = 24.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        SkeletonBox(
          modifier = Modifier.size(width = 48.dp, height = 38.dp),
          brush = brush,
          shape = RoundedCornerShape(10.dp)
        )
        SkeletonBox(
          modifier = Modifier.size(width = 92.dp, height = 13.dp),
          brush = brush,
          shape = CircleShape
        )
      }
      SkeletonBox(
        modifier = Modifier
          .padding(horizontal = 8.dp)
          .size(32.dp),
        brush = brush,
        shape = CircleShape
      )
      Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        SkeletonBox(
          modifier = Modifier.size(width = 48.dp, height = 38.dp),
          brush = brush,
          shape = RoundedCornerShape(10.dp)
        )
        SkeletonBox(
          modifier = Modifier.size(width = 92.dp, height = 13.dp),
          brush = brush,
          shape = CircleShape
        )
      }
    }
    SkeletonBox(
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
      brush = brush,
      shape = CircleShape
    )
  }
}

/** App list skeleton matching [com.absinthe.libchecker.domain.snapshot.track.ui.compose.TrackScreen] rows. */
@Composable
fun TrackListSkeleton(
  contentPadding: PaddingValues,
  modifier: Modifier = Modifier,
  rowCount: Int = 10
) {
  val brush = rememberSkeletonBrush()
  val titleWidths = listOf(0.44f, 0.58f, 0.36f, 0.64f, 0.50f, 0.40f, 0.54f)
  val pkgWidths = listOf(0.70f, 0.54f, 0.78f, 0.60f, 0.74f, 0.56f, 0.66f)

  LazyColumn(
    userScrollEnabled = false,
    contentPadding = contentPadding,
    modifier = modifier
      .fillMaxSize()
      .horizontalCutoutPadding()
  ) {
    items(count = rowCount, key = { "track_skeleton_$it" }) { index ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp)
          .padding(bottom = 12.dp),
        insideMargin = PaddingValues(start = 10.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          SkeletonBox(
            modifier = Modifier
              .padding(end = 12.dp)
              .size(44.dp),
            brush = brush,
            shape = RoundedCornerShape(11.dp)
          )
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            SkeletonBox(
              modifier = Modifier
                .fillMaxWidth(titleWidths[index % titleWidths.size])
                .height(15.dp),
              brush = brush
            )
            SkeletonBox(
              modifier = Modifier
                .fillMaxWidth(pkgWidths[index % pkgWidths.size])
                .height(12.dp),
              brush = brush,
              shape = CircleShape
            )
          }
          SkeletonBox(
            modifier = Modifier
              .padding(start = 12.dp)
              .size(20.dp),
            brush = brush,
            shape = RoundedCornerShape(6.dp)
          )
        }
      }
    }
    item(key = "bottom_spacer") {
      SkeletonBottomSpacer(0.dp)
    }
  }
}

/** Detail page skeleton matching [com.absinthe.libchecker.domain.app.detail.ui.compose.AppDetailScreen]. */
@Composable
fun AppDetailSkeleton(
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val brush = rememberSkeletonBrush()
  MiuixPageHost(modifier = modifier.fillMaxSize()) {
    top.yukonga.miuix.kmp.basic.Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surface,
      contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
      topBar = {
        AdaptiveTopAppBar(
          title = "",
          subtitle = "",
          pinned = true,
          isContentScrolled = false,
          navigationIcon = {
            AdaptiveBackButton(onClick = onBack)
          }
        )
      }
    ) { innerPadding ->
      com.absinthe.libchecker.ui.compose.util.WideContentBox(modifier = Modifier.fillMaxSize()) { sidePadding ->
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = sidePadding + 16.dp, vertical = 10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              SkeletonBox(
                modifier = Modifier.size(56.dp),
                brush = brush,
                shape = RoundedCornerShape(14.dp)
              )
              Spacer(Modifier.size(width = 14.dp, height = 1.dp))
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                SkeletonBox(
                  modifier = Modifier
                    .fillMaxWidth(0.46f)
                    .height(18.dp),
                  brush = brush
                )
                SkeletonBox(
                  modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(13.dp),
                  brush = brush,
                  shape = CircleShape
                )
                SkeletonBox(
                  modifier = Modifier
                    .fillMaxWidth(0.38f)
                    .height(12.dp),
                  brush = brush,
                  shape = CircleShape
                )
              }
            }
            Spacer(Modifier.height(8.dp))
            SkeletonBox(
              modifier = Modifier
                .padding(start = 70.dp)
                .fillMaxWidth(0.78f)
                .height(12.dp),
              brush = brush,
              shape = CircleShape
            )
            Spacer(Modifier.height(8.dp))
            Row(
              modifier = Modifier.padding(start = 70.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              SkeletonBox(
                modifier = Modifier.size(width = 39.dp, height = 26.dp),
                brush = brush,
                shape = RoundedCornerShape(6.dp)
              )
              SkeletonBox(
                modifier = Modifier.size(width = 58.dp, height = 26.dp),
                brush = brush,
                shape = RoundedCornerShape(6.dp)
              )
            }
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = sidePadding + 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            repeat(4) {
              SkeletonBox(
                modifier = Modifier
                  .weight(1f)
                  .height(34.dp),
                brush = brush,
                shape = CircleShape
              )
            }
          }
          Column(
            modifier = Modifier
              .weight(1f)
              .padding(top = 6.dp, start = sidePadding, end = sidePadding)
          ) {
            DetailTabListSkeleton(rowCount = 8)
          }
        }
      }
    }
  }
}

/** Snapshot detail skeleton matching [com.absinthe.libchecker.domain.snapshot.detail.ui.compose.SnapshotDetailScreen]. */
@Composable
fun SnapshotDetailSkeleton(
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val brush = rememberSkeletonBrush()
  MiuixPageHost(modifier = modifier.fillMaxSize()) {
    top.yukonga.miuix.kmp.basic.Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surface,
      contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
      topBar = {
        AdaptiveTopAppBar(
          title = "",
          subtitle = "",
          pinned = true,
          isContentScrolled = false,
          navigationIcon = {
            AdaptiveBackButton(onClick = onBack)
          }
        )
      }
    ) { innerPadding ->
      com.absinthe.libchecker.ui.compose.util.WideContentBox(modifier = Modifier.fillMaxSize()) { sidePadding ->
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())
        ) {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalCutoutPadding()
              .padding(horizontal = sidePadding + 12.dp, vertical = 8.dp),
            insideMargin = PaddingValues(16.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonBox(
                  modifier = Modifier
                    .padding(end = 14.dp)
                    .size(48.dp),
                  brush = brush,
                  shape = RoundedCornerShape(12.dp)
                )
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  SkeletonBox(
                    modifier = Modifier
                      .fillMaxWidth(0.46f)
                      .height(17.dp),
                    brush = brush
                  )
                  SkeletonBox(
                    modifier = Modifier
                      .fillMaxWidth(0.72f)
                      .height(12.dp),
                    brush = brush,
                    shape = CircleShape
                  )
                }
              }
              SkeletonBox(
                modifier = Modifier
                  .fillMaxWidth(0.64f)
                  .height(12.dp),
                brush = brush,
                shape = CircleShape
              )
              SkeletonBox(
                modifier = Modifier
                  .fillMaxWidth(0.80f)
                  .height(12.dp),
                brush = brush,
                shape = CircleShape
              )
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonBox(
                  modifier = Modifier.size(width = 48.dp, height = 22.dp),
                  brush = brush,
                  shape = CircleShape
                )
                SkeletonBox(
                  modifier = Modifier.size(width = 64.dp, height = 22.dp),
                  brush = brush,
                  shape = CircleShape
                )
              }
            }
          }
          Column(
            modifier = Modifier
              .weight(1f)
              .padding(top = 4.dp, start = sidePadding, end = sidePadding)
          ) {
            DetailTabListSkeleton(rowCount = 7)
          }
        }
      }
    }
  }
}
