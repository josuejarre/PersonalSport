package com.example.personalsport.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.personalsport.data.model.GpsPoint
import com.example.personalsport.data.model.GpsRouteUtils

/**
 * Renders a stylized dark metallic map canvas for outdoor running and open water swimming route previews.
 * - Dark metallic map background (#1A1E24 to #12161A)
 * - Electric orange polyline path (#FF6500)
 * - Green start marker dot (inicio) and red finish marker dot (fin)
 * - Subtle grid lines representing terrain map
 * - Rounded thumbnail format
 */
@Composable
fun RouteMapPreviewThumbnail(
    gpsPointsJson: String? = null,
    gpsPoints: List<GpsPoint>? = null,
    width: Dp = 80.dp,
    height: Dp = 60.dp,
    modifier: Modifier = Modifier
) {
    val points = remember(gpsPointsJson, gpsPoints) {
        gpsPoints ?: GpsRouteUtils.parseGpsPoints(gpsPointsJson)
    }

    if (points.isEmpty()) return

    val shape = RoundedCornerShape(10.dp)
    val metallicBackground = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1A1E24),
                Color(0xFF12161A)
            )
        )
    }

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(shape)
            .background(brush = metallicBackground)
            .border(1.dp, Color(0xFF2A313A), shape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 1. Draw subtle grid lines (terrain map effect)
            val gridColor = Color(0xFF2A313A).copy(alpha = 0.5f)
            val numGridCols = 4
            val numGridRows = 3

            for (i in 1 until numGridCols) {
                val x = canvasWidth * i / numGridCols
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, canvasHeight),
                    strokeWidth = 1f
                )
            }

            for (j in 1 until numGridRows) {
                val y = canvasHeight * j / numGridRows
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1f
                )
            }

            // 2. Project GPS coordinates into canvas space
            val minLat = points.minOf { it.lat }
            val maxLat = points.maxOf { it.lat }
            val minLng = points.minOf { it.lng }
            val maxLng = points.maxOf { it.lng }

            val latSpan = (maxLat - minLat).let { if (it <= 0.0) 0.0001 else it }
            val lngSpan = (maxLng - minLng).let { if (it <= 0.0) 0.0001 else it }

            val paddingX = canvasWidth * 0.15f
            val paddingY = canvasHeight * 0.15f
            val usableWidth = canvasWidth - (2 * paddingX)
            val usableHeight = canvasHeight - (2 * paddingY)

            val screenPoints = points.map { pt ->
                val x = paddingX + ((pt.lng - minLng) / lngSpan * usableWidth).toFloat()
                val y = paddingY + ((maxLat - pt.lat) / latSpan * usableHeight).toFloat()
                Offset(x, y)
            }

            // 3. Draw polyline path in electric orange (#FF6500)
            if (screenPoints.size >= 2) {
                val path = Path().apply {
                    moveTo(screenPoints[0].x, screenPoints[0].y)
                    for (i in 1 until screenPoints.size) {
                        lineTo(screenPoints[i].x, screenPoints[i].y)
                    }
                }

                // Outer glow
                drawPath(
                    path = path,
                    color = Color(0xFFFF6500).copy(alpha = 0.25f),
                    style = Stroke(
                        width = 8f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Main line
                drawPath(
                    path = path,
                    color = Color(0xFFFF6500),
                    style = Stroke(
                        width = 4f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 4. Draw Start marker (green dot: inicio) and Finish marker (red dot: fin)
            val startPt = screenPoints.first()
            val finishPt = screenPoints.last()

            // Green Start Dot (#00E676)
            drawCircle(
                color = Color(0xFF00E676).copy(alpha = 0.4f),
                radius = 7f,
                center = startPt
            )
            drawCircle(
                color = Color(0xFF00E676),
                radius = 4f,
                center = startPt
            )

            // Red Finish Dot (#FF1744)
            drawCircle(
                color = Color(0xFFFF1744).copy(alpha = 0.4f),
                radius = 7f,
                center = finishPt
            )
            drawCircle(
                color = Color(0xFFFF1744),
                radius = 4f,
                center = finishPt
            )
        }
    }
}

@Preview
@Composable
fun RouteMapPreviewThumbnailPreview() {
    RouteMapPreviewThumbnail(
        gpsPoints = GpsRouteUtils.generateSampleRunRoute(),
        width = 80.dp,
        height = 60.dp
    )
}
