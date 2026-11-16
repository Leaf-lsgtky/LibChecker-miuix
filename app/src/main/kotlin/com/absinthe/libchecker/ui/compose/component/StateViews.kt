package com.absinthe.libchecker.ui.compose.component

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import com.absinthe.libchecker.R
import com.absinthe.libchecker.data.app.AppDataSource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Miuix-native orbital squircle-matrix loading emblem supporting indeterminate and progress modes. */
@Composable
fun MiuixOrbitalEmblem(
  modifier: Modifier = Modifier,
  size: Dp = 136.dp,
  progress: Float? = null
) {
  val colorScheme = MiuixTheme.colorScheme
  val primary = colorScheme.primary
  val onSurface = colorScheme.onSurface
  val isDark = colorScheme.surface.luminance() < 0.5f
  val coreSurface = if (isDark) colorScheme.surfaceContainerHigh else colorScheme.surfaceContainer
  val baseTileColor = onSurface.copy(alpha = if (isDark) 0.16f else 0.11f)
  val outerTrackColor = onSurface.copy(alpha = if (isDark) 0.10f else 0.065f)
  val innerTrackColor = onSurface.copy(alpha = if (isDark) 0.05f else 0.035f)
  val coreBorderColor = onSurface.copy(alpha = if (isDark) 0.10f else 0.06f)

  val infiniteTransition = rememberInfiniteTransition(label = "MiuixOrbitalTransition")
  val sweepAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "OrbitalSweep"
  )
  val pulse by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "OrbitalPulse"
  )
  val gridPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1800, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "MatrixWave"
  )
  val animatedProgress by animateFloatAsState(
    targetValue = (progress ?: 0f).coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
    label = "DeterminateProgress"
  )

  Canvas(modifier = modifier.size(size)) {
    val minDim = this.size.minDimension
    val radius = minDim / 2f
    val center = Offset(this.size.width / 2f, this.size.height / 2f)
    val outerRadius = radius * 0.85f
    val innerTrackRadius = radius * 0.70f
    val strokeWidth = (minDim * 0.044f).coerceIn(2.5.dp.toPx(), 5.dp.toPx())
    val outerArcSize = Size(outerRadius * 2f, outerRadius * 2f)
    val outerArcTopLeft = Offset(center.x - outerRadius, center.y - outerRadius)
    val innerArcSize = Size(innerTrackRadius * 2f, innerTrackRadius * 2f)
    val innerArcTopLeft = Offset(center.x - innerTrackRadius, center.y - innerTrackRadius)

    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(primary.copy(alpha = 0.07f + 0.045f * pulse), Color.Transparent),
        center = center,
        radius = radius
      ),
      center = center,
      radius = radius
    )

    drawCircle(
      color = outerTrackColor,
      radius = outerRadius,
      center = center,
      style = Stroke(width = strokeWidth)
    )

    drawCircle(
      color = innerTrackColor,
      radius = innerTrackRadius,
      center = center,
      style = Stroke(width = (strokeWidth * 0.42f).coerceAtLeast(1.dp.toPx()))
    )

    if (progress != null) {
      val progressSweep = (animatedProgress * 360f).coerceIn(0f, 360f)
      if (progressSweep > 0.5f) {
        drawArc(
          color = primary,
          startAngle = -90f,
          sweepAngle = progressSweep,
          useCenter = false,
          topLeft = outerArcTopLeft,
          size = outerArcSize,
          style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
      }
      val headRad = ((sweepAngle - 90f) * PI / 180.0).toFloat()
      val headOffset = Offset(
        x = center.x + outerRadius * cos(headRad),
        y = center.y + outerRadius * sin(headRad)
      )
      drawCircle(
        color = primary.copy(alpha = 0.22f),
        radius = strokeWidth * 1.45f,
        center = headOffset
      )
      drawCircle(
        color = primary,
        radius = strokeWidth * 0.58f,
        center = headOffset
      )
    } else {
      val primarySweep = 112f + 84f * pulse
      val sweepRatio = (primarySweep / 360f).coerceIn(0.05f, 0.9f)
      val tailBrush = Brush.sweepGradient(
        0.0f to Color.Transparent,
        (sweepRatio * 0.45f) to primary.copy(alpha = 0.28f),
        sweepRatio to primary,
        1.0f to Color.Transparent,
        center = center
      )
      rotate(degrees = sweepAngle - 90f, pivot = center) {
        drawArc(
          brush = tailBrush,
          startAngle = 0f,
          sweepAngle = primarySweep,
          useCenter = false,
          topLeft = outerArcTopLeft,
          size = outerArcSize,
          style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
      }
      val headRad = ((sweepAngle - 90f + primarySweep) * PI / 180.0).toFloat()
      val headOffset = Offset(
        x = center.x + outerRadius * cos(headRad),
        y = center.y + outerRadius * sin(headRad)
      )
      drawCircle(
        color = primary.copy(alpha = 0.20f),
        radius = strokeWidth * 1.4f,
        center = headOffset
      )
      drawCircle(
        color = primary,
        radius = strokeWidth * 0.52f,
        center = headOffset
      )

      drawArc(
        color = primary.copy(alpha = 0.28f),
        startAngle = 140f - sweepAngle * 0.65f,
        sweepAngle = 46f,
        useCenter = false,
        topLeft = innerArcTopLeft,
        size = innerArcSize,
        style = Stroke(width = (strokeWidth * 0.48f).coerceAtLeast(1.2.dp.toPx()), cap = StrokeCap.Round)
      )
    }

    val coreSize = minDim * 0.46f
    val coreTopLeft = Offset(center.x - coreSize / 2f, center.y - coreSize / 2f)
    val coreCorner = CornerRadius(coreSize * 0.28f)
    drawRoundRect(
      color = coreSurface,
      topLeft = coreTopLeft,
      size = Size(coreSize, coreSize),
      cornerRadius = coreCorner
    )
    drawRoundRect(
      color = coreBorderColor,
      topLeft = coreTopLeft,
      size = Size(coreSize, coreSize),
      cornerRadius = coreCorner,
      style = Stroke(width = 1.dp.toPx())
    )

    val cellPadding = coreSize * 0.23f
    val cellGap = coreSize * 0.10f
    val baseTileSize = (coreSize - cellPadding * 2f - cellGap) / 2f
    for (row in 0..1) {
      for (col in 0..1) {
        val offset = (row + col) * 0.22f
        val theta = (gridPhase - offset) * (2.0 * PI)
        val wave = (0.5f + 0.5f * sin(theta).toFloat()).coerceIn(0f, 1f)
        val scale = 0.78f + 0.22f * wave
        val tileSize = baseTileSize * scale
        val cellCenterX = coreTopLeft.x + cellPadding + col * (baseTileSize + cellGap) + baseTileSize / 2f
        val cellCenterY = coreTopLeft.y + cellPadding + row * (baseTileSize + cellGap) + baseTileSize / 2f
        val tileColor = lerp(baseTileColor, primary.copy(alpha = 0.92f), wave)
        drawRoundRect(
          color = tileColor,
          topLeft = Offset(cellCenterX - tileSize / 2f, cellCenterY - tileSize / 2f),
          size = Size(tileSize, tileSize),
          cornerRadius = CornerRadius(tileSize * 0.32f)
        )
      }
    }
  }
}

/** Centred Miuix orbital loading indicator with optional percentage pill and status label. */
@Composable
fun LcMiuixLoading(
  modifier: Modifier = Modifier,
  message: String? = null,
  progress: Float? = null
) {
  val colorScheme = MiuixTheme.colorScheme
  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    MiuixOrbitalEmblem(
      size = 136.dp,
      progress = progress
    )
    if (progress != null) {
      val percent = (progress.coerceIn(0f, 1f) * 100).toInt()
      Spacer(Modifier.height(14.dp))
      Box(
        modifier = Modifier
          .clip(CircleShape)
          .background(colorScheme.primary.copy(alpha = 0.12f))
          .padding(horizontal = 12.dp, vertical = 4.dp)
      ) {
        Text(
          text = "$percent%",
          style = MiuixTheme.textStyles.footnote1.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = "tnum"
          ),
          color = colorScheme.primary
        )
      }
    }
    if (message != null) {
      Spacer(Modifier.height(12.dp))
      Text(
        text = message,
        style = MiuixTheme.textStyles.body2,
        color = colorScheme.onSurfaceVariantSummary,
        textAlign = TextAlign.Center
      )
    }
  }
}

/**
 * iOS 6-inspired 3D Cover Flow carousel of random installed app icons paired with a
 * clean loading label and compact horizontal progress indicator.
 */
@Composable
fun CoverFlowReferenceLoading(
  modifier: Modifier = Modifier,
  message: String? = null,
  progress: Float? = null
) {
  val context = LocalContext.current
  val colorScheme = MiuixTheme.colorScheme
  val isDark = colorScheme.surface.luminance() < 0.5f
  val cardSurface = if (isDark) colorScheme.surfaceContainerHigh else colorScheme.surfaceContainer
  val cardBorder = colorScheme.onSurface.copy(alpha = if (isDark) 0.14f else 0.08f)
  val density = LocalDensity.current.density
  val fallbackBrush = rememberSkeletonBrush()

  var icons by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
  LaunchedEffect(context) {
    val loaded = withContext(Dispatchers.IO) {
      loadRandomCoverFlowIcons(context, targetCount = 12)
    }
    if (loaded.isNotEmpty()) {
      icons = loaded
    }
  }

  val slotCount = if (icons.isNotEmpty()) icons.size else 7
  val focus = remember { Animatable(0f) }
  LaunchedEffect(slotCount) {
    var index = 0
    while (true) {
      delay(340L)
      index++
      focus.animateTo(
        targetValue = index.toFloat(),
        animationSpec = tween(durationMillis = 660, easing = FastOutSlowInEasing)
      )
    }
  }

  val animatedProgress = progress?.let { target ->
    animateFloatAsState(
      targetValue = target.coerceIn(0f, 1f),
      animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
      label = "CoverFlowLinearProgress"
    ).value
  }
  val iconRevealAlpha by animateFloatAsState(
    targetValue = if (icons.isNotEmpty()) 1f else 0f,
    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
    label = "CoverFlowIconReveal"
  )

  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    val currentFocus = focus.value
    val centerSlot = floor(currentFocus).toInt()
    val cardSizeDp = 76.dp
    val reflectionHeightDp = 22.dp
    val cardShape = RoundedCornerShape(18.dp)
    val reflectionShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    val surfaceFadeBrush = remember(colorScheme.surface) {
      Brush.verticalGradient(
        0f to colorScheme.surface.copy(alpha = 0.28f),
        0.85f to colorScheme.surface.copy(alpha = 0.94f),
        1f to colorScheme.surface
      )
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(114.dp),
      contentAlignment = Alignment.Center
    ) {
      for (offset in -3..3) {
        val slot = centerSlot + offset
        val delta = slot - currentFocus
        val absD = abs(delta)
        val itemAlpha = ((2.85f - absD) / 0.85f).coerceIn(0f, 1f)
        if (itemAlpha <= 0.01f) continue

        val sign = if (delta < 0f) -1f else 1f
        val shiftDp = sign * (absD * 64f - absD * absD * 5.8f)
        val tiltDeg = (-atan((delta * 1.45f).toDouble()) * 34.0).toFloat()
        val scale = (1f - absD * 0.13f).coerceIn(0.60f, 1f)
        val z = 10f - absD
        val itemIndex = ((slot % slotCount) + slotCount) % slotCount
        val iconBitmap = icons.getOrNull(itemIndex)

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .zIndex(z)
            .graphicsLayer {
              translationX = shiftDp.dp.toPx()
              rotationY = tiltDeg
              scaleX = scale
              scaleY = scale
              alpha = itemAlpha
              cameraDistance = 10f * density
            }
        ) {
          Box(
            modifier = Modifier
              .size(cardSizeDp)
              .clip(cardShape)
              .background(cardSurface)
              .border(0.75.dp, cardBorder, cardShape),
            contentAlignment = Alignment.Center
          ) {
            if (iconRevealAlpha < 0.99f) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .graphicsLayer { alpha = 1f - iconRevealAlpha }
                  .background(fallbackBrush)
              )
            }
            if (iconBitmap != null) {
              Image(
                bitmap = iconBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .fillMaxSize()
                  .graphicsLayer { alpha = iconRevealAlpha }
              )
            }
          }
          Box(
            modifier = Modifier
              .padding(top = 3.dp)
              .size(width = cardSizeDp, height = reflectionHeightDp)
              .clip(reflectionShape),
            contentAlignment = Alignment.TopCenter
          ) {
            if (iconRevealAlpha < 0.99f) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .graphicsLayer { alpha = 1f - iconRevealAlpha }
                  .background(fallbackBrush)
              )
            }
            if (iconBitmap != null) {
              Image(
                bitmap = iconBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .requiredSize(cardSizeDp)
                  .graphicsLayer {
                    scaleY = -1f
                    alpha = 0.24f * iconRevealAlpha
                  }
              )
            }
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(surfaceFadeBrush)
            )
          }
        }
      }
    }

    Spacer(Modifier.height(14.dp))
    Text(
      text = message ?: stringResource(R.string.loading),
      style = MiuixTheme.textStyles.body2,
      color = colorScheme.onSurfaceVariantSummary,
      textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(10.dp))
    LinearProgressIndicator(
      progress = animatedProgress,
      height = 4.dp,
      modifier = Modifier.width(148.dp)
    )
  }
}

private fun loadRandomCoverFlowIcons(context: Context, targetCount: Int): List<ImageBitmap> {
  val appDataSource = runCatching {
    GlobalContext.getOrNull()?.getOrNull<AppDataSource>()
  }.getOrNull() ?: return emptyList()
  val pm = context.packageManager
  val iconPx = (76f * context.resources.displayMetrics.density).toInt().coerceIn(96, 256)
  val packages = appDataSource.getApplicationList()
  val (systemApps, userApps) = packages.partition {
    ((it.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM) != 0
  }
  val pool = if (userApps.size >= 8) {
    userApps.shuffled() + systemApps.shuffled()
  } else {
    packages.shuffled()
  }
  val decoded = ArrayList<ImageBitmap>(targetCount)
  for (pkg in pool) {
    if (decoded.size >= targetCount) break
    val info = pkg.applicationInfo ?: continue
    val bitmap = runCatching {
      info.loadIcon(pm).toBitmap(width = iconPx, height = iconPx).asImageBitmap()
    }.getOrNull()
    if (bitmap != null) {
      decoded.add(bitmap)
    }
  }
  return decoded
}

/** Centred empty state with an optional action. */
@Composable
fun LcEmptyState(
  title: String,
  modifier: Modifier = Modifier,
  icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
  @DrawableRes drawableRes: Int? = null,
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
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.size(56.dp)
      )
      Spacer(Modifier.height(12.dp))
    } else if (drawableRes != null) {
      Icon(
        painter = painterResource(drawableRes),
        contentDescription = null,
        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.6f),
        modifier = Modifier
          .width(120.dp)
          .aspectRatio(160f / 137f)
      )
      Spacer(Modifier.height(12.dp))
    }
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

/** Centred error state with a retry action. */
@Composable
fun LcErrorState(
  title: String,
  modifier: Modifier = Modifier,
  message: String? = null,
  retryLabel: String? = null,
  onRetry: (() -> Unit)? = null
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
    if (retryLabel != null && onRetry != null) {
      Spacer(Modifier.height(20.dp))
      Button(onClick = onRetry, colors = ButtonDefaults.buttonColorsPrimary()) {
        Text(text = retryLabel)
      }
    }
  }
}
