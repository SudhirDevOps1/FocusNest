package com.example.core.design

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun FocusNestLogoMark(
  modifier: Modifier = Modifier,
  size: Dp = 44.dp,
) {
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val tertiary = MaterialTheme.colorScheme.tertiary
  Canvas(modifier = modifier.size(size)) {
    val c = Offset(this.size.width / 2f, this.size.height / 2f)
    val r = this.size.minDimension / 2f
    drawCircle(
      color = primary.copy(alpha = 0.22f),
      radius = r * 0.94f,
      center = c,
      style = Stroke(width = r * 0.14f),
    )
    drawArc(
      color = primary,
      startAngle = -45f,
      sweepAngle = 250f,
      useCenter = false,
      topLeft = Offset(c.x - r * 0.78f, c.y - r * 0.78f),
      size = Size(r * 1.56f, r * 1.56f),
      style = Stroke(width = r * 0.15f, cap = StrokeCap.Round),
    )
    drawArc(
      color = secondary,
      startAngle = 110f,
      sweepAngle = 230f,
      useCenter = false,
      topLeft = Offset(c.x - r * 0.54f, c.y - r * 0.54f),
      size = Size(r * 1.08f, r * 1.08f),
      style = Stroke(width = r * 0.13f, cap = StrokeCap.Round),
    )
    drawCircle(
      color = tertiary,
      radius = r * 0.24f,
      center = c,
    )
  }
}

@Composable
fun SanctuaryCard(
  modifier: Modifier = Modifier,
  highlight: Boolean = false,
  content: @Composable ColumnScope.() -> Unit,
) {
  val containerColor =
    if (highlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    else MaterialTheme.colorScheme.surfaceContainerLow
  val borderColor =
    if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = BorderStroke(1.dp, borderColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    content = content,
  )
}

@Composable
fun AnimatedGoalRing(
  progress: Float,
  centerTitle: String,
  centerSubtitle: String,
  modifier: Modifier = Modifier,
  ringSize: Dp = 156.dp,
  overLimit: Boolean = false,
) {
  val animatedProgress by
    animateFloatAsState(
      targetValue = progress.coerceIn(0f, 1.05f),
      animationSpec = spring(stiffness = Spring.StiffnessLow),
      label = "goalRing",
    )
  val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
  val startColor =
    if (overLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
  val endColor =
    if (overLimit) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.tertiary

  Box(modifier = modifier.size(ringSize), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.size(ringSize)) {
      val strokeWidth = 14.dp.toPx()
      val pad = strokeWidth / 2f
      val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
      drawArc(
        color = trackColor,
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(pad, pad),
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
      )
      drawArc(
        brush = Brush.sweepGradient(listOf(startColor, endColor, startColor)),
        startAngle = -90f,
        sweepAngle = 360f * animatedProgress.coerceIn(0f, 1f),
        useCenter = false,
        topLeft = Offset(pad, pad),
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
      )
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = centerTitle,
        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFontFamily),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = centerSubtitle,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun SectionHeader(
  title: String,
  subtitle: String? = null,
  actionLabel: String? = null,
  onActionClick: (() -> Unit)? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (subtitle != null) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    if (actionLabel != null && onActionClick != null) {
      androidx.compose.material3.TextButton(onClick = onActionClick) {
        Text(actionLabel)
      }
    }
  }
}

@Composable
fun MeasuredBadge(isMeasured: Boolean) {
  val bg =
    if (isMeasured) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHigh
  val fg =
    if (isMeasured) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
  Surface(
    color = bg,
    shape = RoundedCornerShape(50),
  ) {
    Text(
      text = if (isMeasured) "Measured" else "No System Access",
      style = MaterialTheme.typography.labelSmall,
      color = fg,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun BarChartCard(
  title: String,
  subtitle: String,
  entries: List<Pair<String, Float>>,
  valueFormatter: (Float) -> String,
  modifier: Modifier = Modifier,
) {
  val barColor = MaterialTheme.colorScheme.primary
  val barTrack = MaterialTheme.colorScheme.surfaceContainerHigh
  val maxVal = (entries.maxOfOrNull { it.second } ?: 1f).coerceAtLeast(1f)

  SanctuaryCard(modifier = modifier) {
    Column(modifier = Modifier.padding(18.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(
        subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(16.dp))
      if (entries.isEmpty() || entries.all { it.second <= 0f }) {
        Text(
          text = "No recorded data for this range yet.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 18.dp),
        )
      } else {
        Row(
          modifier = Modifier.fillMaxWidth().height(132.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.Bottom,
        ) {
          entries.forEach { (label, value) ->
            val ratio = (value / maxVal).coerceIn(0.04f, 1f)
            Column(
              modifier = Modifier.weight(1f),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Bottom,
            ) {
              Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Box(
                modifier =
                  Modifier
                    .fillMaxWidth(0.62f)
                    .height(84.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(barTrack),
                contentAlignment = Alignment.BottomCenter,
              ) {
                Box(
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .height((84f * ratio).dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(
                        Brush.verticalGradient(
                          listOf(barColor, barColor.copy(alpha = 0.65f))
                        )
                      )
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun FocusNestEmptyState(
  icon: ImageVector,
  title: String,
  message: String,
  actionLabel: String? = null,
  actionTestTag: String = "empty_state_action",
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
  @androidx.annotation.DrawableRes illustrationRes: Int? = null,
) {
  val primary = MaterialTheme.colorScheme.primary
  val tertiary = MaterialTheme.colorScheme.tertiary
  SanctuaryCard(modifier = modifier) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (illustrationRes != null) {
        androidx.compose.foundation.Image(
          painter = androidx.compose.ui.res.painterResource(illustrationRes),
          contentDescription = title,
          modifier = Modifier.size(96.dp),
        )
      } else {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(92.dp)) {
          Canvas(modifier = Modifier.size(92.dp)) {
            drawCircle(
              color = primary.copy(alpha = 0.12f),
              radius = size.minDimension / 2f,
            )
            drawCircle(
              color = tertiary.copy(alpha = 0.16f),
              radius = size.minDimension * 0.36f,
            )
          }
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(52.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp),
              )
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (actionLabel != null && onAction != null) {
        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = onAction,
          modifier = Modifier.testTag(actionTestTag),
        ) {
          Text(actionLabel)
        }
      }
    }
  }
}

@Composable
fun MetricTile(
  label: String,
  value: String,
  icon: ImageVector,
  tint: Color = MaterialTheme.colorScheme.primary,
  modifier: Modifier = Modifier,
) {
  SanctuaryCard(modifier = modifier) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = tint,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = label,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge.copy(
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
        ),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun InstalledAppIcon(
  packageName: String,
  appName: String,
  size: Dp = 36.dp,
  modifier: Modifier = Modifier,
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val iconBitmap =
    androidx.compose.runtime.remember(packageName) {
      runCatching {
        val drawable = context.packageManager.getApplicationIcon(packageName)
        val px = 96
        val bmp = createBitmap(px, px)
        val canvas = android.graphics.Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bmp
      }.getOrNull()
    }

  if (iconBitmap != null) {
    androidx.compose.foundation.Image(
      bitmap = iconBitmap.asImageBitmap(),
      contentDescription = "$appName icon",
      modifier = modifier.size(size).clip(RoundedCornerShape(10.dp)),
    )
  } else {
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = MaterialTheme.colorScheme.primaryContainer,
      modifier = modifier.size(size),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(
          text = appName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "A",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
    }
  }
}

/**
 * Responsive container switching cleanly between single-column centered scrolling on compact phones
 * and side-by-side canonical two-pane columns on foldables, tablets, and desktop/ChromeOS widths.
 */
@Composable
fun AdaptiveTwoPane(
  modifier: Modifier = Modifier,
  breakpoint: Dp = 720.dp,
  listTestTag: String? = null,
  headerContent: (@Composable () -> Unit)? = null,
  primaryWeight: Float = 0.48f,
  secondaryWeight: Float = 0.52f,
  primaryPane: LazyListScope.() -> Unit,
  secondaryPane: LazyListScope.() -> Unit,
) {
  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val twoPane = maxWidth >= breakpoint
    if (twoPane) {
      Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        if (headerContent != null) {
          Spacer(modifier = Modifier.height(12.dp))
          headerContent()
          Spacer(modifier = Modifier.height(12.dp))
        }
        Row(
          modifier = Modifier.fillMaxSize(),
          horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
          LazyColumn(
            modifier =
              Modifier
                .weight(primaryWeight)
                .let { if (listTestTag != null) it.testTag(listTestTag) else it },
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = primaryPane,
          )
          LazyColumn(
            modifier = Modifier.weight(secondaryWeight),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = secondaryPane,
          )
        }
      }
    } else {
      LazyColumn(
        modifier =
          Modifier
            .fillMaxSize()
            .let { if (listTestTag != null) it.testTag(listTestTag) else it },
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        if (headerContent != null) {
          item {
            Box(modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp)) {
              headerContent()
            }
          }
        }
        primaryPane()
        secondaryPane()
      }
    }
  }
}
