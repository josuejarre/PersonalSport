package com.example.personalsport.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalsport.data.model.GpsPoint
import com.example.personalsport.data.model.GpsRouteUtils
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import java.util.Locale

/**
 * Embedded Google Maps Route Window with GPS route line rendering, Start/Finish markers,
 * interactive fallback vector map renderer, and a route metrics overlay.
 */
@Composable
fun GoogleMapRouteWindow(
    modifier: Modifier = Modifier,
    routeGpsPoints: String? = null,
    gpsPoints: List<GpsPoint>? = null,
    totalDistanceKm: Double? = null,
    paceMinKm: String? = null,
    elevationGainMeters: Int? = null,
    startTimeStr: String? = null,
    finishTimeStr: String? = null,
    durationMinutes: Int? = null,
    startTimestampMs: Long? = null,
    height: Dp = 220.dp,
    showMetricsOverlay: Boolean = true
) {
    val points = remember(routeGpsPoints, gpsPoints) {
        if (!gpsPoints.isNullOrEmpty()) {
            gpsPoints
        } else {
            val parsed = GpsRouteUtils.parseGpsRoutePoints(routeGpsPoints)
            parsed.ifEmpty { GpsRouteUtils.generateSampleRunRoute() }
        }
    }

    // Calculate metrics if not provided
    val calcDistance = remember(points, totalDistanceKm) {
        totalDistanceKm ?: GpsRouteUtils.calculateTotalDistanceKm(points).let { if (it <= 0.0) 5.2 else it }
    }
    val calcDuration = remember(durationMinutes) { durationMinutes ?: 28 }
    val calcPace = remember(calcDistance, calcDuration, paceMinKm) {
        paceMinKm ?: GpsRouteUtils.calculatePaceMinKm(calcDistance, calcDuration.toDouble())
    }
    val calcElevation = remember(points, elevationGainMeters) {
        elevationGainMeters ?: GpsRouteUtils.calculateElevationGainMeters(points)
    }
    val calcTimeRange = remember(startTimeStr, finishTimeStr, startTimestampMs, calcDuration) {
        if (!startTimeStr.isNullOrBlank() && !finishTimeStr.isNullOrBlank()) {
            "$startTimeStr - $finishTimeStr"
        } else {
            val startMs = startTimestampMs ?: (System.currentTimeMillis() - (calcDuration * 60 * 1000L))
            GpsRouteUtils.formatStartAndFinishTime(startMs, calcDuration)
        }
    }

    val context = LocalContext.current
    val isInspectionMode = LocalInspectionMode.current

    // Check Google Play Services availability safely
    val isPlayServicesAvailable = remember(context, isInspectionMode) {
        if (isInspectionMode) {
            false
        } else {
            try {
                val apiAvailability = GoogleApiAvailability.getInstance()
                val resultCode = apiAvailability.isGooglePlayServicesAvailable(context)
                resultCode == ConnectionResult.SUCCESS
            } catch (_: Exception) {
                false
            }
        }
    }

    var mapFailedToRender by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF3A424D), RoundedCornerShape(20.dp))
            .background(Color(0xFF14181D))
    ) {
        if (isPlayServicesAvailable && !mapFailedToRender) {
            // Render Native Google Maps
            val latLngList = remember(points) {
                points.map { LatLng(it.lat, it.lng) }
            }

            val bounds = remember(latLngList) {
                if (latLngList.isEmpty()) null
                else {
                    val builder = LatLngBounds.builder()
                    latLngList.forEach { builder.include(it) }
                    builder.build()
                }
            }

            val cameraPositionState = rememberCameraPositionState()

            LaunchedEffect(bounds) {
                bounds?.let {
                    try {
                        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(it, 80)
                        cameraPositionState.animate(cameraUpdate)
                    } catch (_: Exception) {
                        try {
                            val center = it.center
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(center, 14f)
                        } catch (_: Exception) {
                            mapFailedToRender = true
                        }
                    }
                }
            }

            val mapProperties = remember {
                MapProperties(
                    mapType = MapType.NORMAL,
                    isMyLocationEnabled = false
                )
            }
            val mapUiSettings = remember {
                MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = true,
                    mapToolbarEnabled = false
                )
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = mapUiSettings
            ) {
                if (latLngList.isNotEmpty()) {
                    // Polyline electric orange (#FF6500)
                    Polyline(
                        points = latLngList,
                        color = Color(0xFFFF6500),
                        width = 12f
                    )

                    // Start Marker (Green dot / marker)
                    val startLatLng = latLngList.first()
                    Marker(
                        state = rememberMarkerState(position = startLatLng),
                        title = "Inicio / Start",
                        snippet = "Punto de salida",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )

                    // Finish Marker (Red dot / marker)
                    val finishLatLng = latLngList.last()
                    Marker(
                        state = rememberMarkerState(position = finishLatLng),
                        title = "Fin / Finish",
                        snippet = "Punto de llegada",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }
            }
        }

        // Fallback Vector Map Renderer if Google Play Services is missing or map fails or in Preview/UnitTest
        if (!isPlayServicesAvailable || mapFailedToRender) {
            FallbackInteractiveVectorMap(
                points = points,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Route Metrics Overlay Box
        if (showMetricsOverlay) {
            RouteMetricsOverlay(
                totalDistanceKm = calcDistance,
                paceMinKm = calcPace,
                elevationGainMeters = calcElevation,
                timeRangeStr = calcTimeRange,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            )
        }

        // Indicator Badge in Top-Left
        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Map,
                    contentDescription = null,
                    tint = Color(0xFFFF6500),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isPlayServicesAvailable && !mapFailedToRender) "Google Maps GPS" else "GPS Route Map",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Interactive Fallback Vector Map Renderer when Google Play Services or API key is unconfigured.
 */
@Composable
fun FallbackInteractiveVectorMap(
    points: List<GpsPoint>,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val metallicBackground = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E242C),
                Color(0xFF12161B)
            )
        )
    }

    Box(
        modifier = modifier
            .background(brush = metallicBackground)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.8f, 3.5f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Grid lines
            val gridColor = Color(0xFF2A323D)
            val numGridCols = 6
            val numGridRows = 4

            for (i in 1 until numGridCols) {
                val x = (canvasWidth * i) / numGridCols
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, canvasHeight),
                    strokeWidth = 1f
                )
            }
            for (j in 1 until numGridRows) {
                val y = (canvasHeight * j) / numGridRows
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1f
                )
            }

            if (points.isNotEmpty()) {
                val minLat = points.minOf { it.lat }
                val maxLat = points.maxOf { it.lat }
                val minLng = points.minOf { it.lng }
                val maxLng = points.maxOf { it.lng }

                val latSpan = (maxLat - minLat).let { if (it <= 0.0) 0.0001 else it }
                val lngSpan = (maxLng - minLng).let { if (it <= 0.0) 0.0001 else it }

                val paddingX = canvasWidth * 0.18f
                val paddingY = canvasHeight * 0.22f
                val usableWidth = canvasWidth - (2 * paddingX)
                val usableHeight = canvasHeight - (2 * paddingY)

                val screenPoints = points.map { pt ->
                    val rawX = paddingX + ((pt.lng - minLng) / lngSpan * usableWidth).toFloat()
                    val rawY = paddingY + ((maxLat - pt.lat) / latSpan * usableHeight).toFloat()
                    val scaledX = (rawX - canvasWidth / 2f) * scale + canvasWidth / 2f + offsetX
                    val scaledY = (rawY - canvasHeight / 2f) * scale + canvasHeight / 2f + offsetY
                    Offset(scaledX, scaledY)
                }

                // Electric orange polyline path (#FF6500, width 12f)
                if (screenPoints.size >= 2) {
                    val path = Path().apply {
                        moveTo(screenPoints[0].x, screenPoints[0].y)
                        for (i in 1 until screenPoints.size) {
                            lineTo(screenPoints[i].x, screenPoints[i].y)
                        }
                    }

                    // Glow line
                    drawPath(
                        path = path,
                        color = Color(0xFFFF6500).copy(alpha = 0.3f),
                        style = Stroke(
                            width = 20f * scale,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Main polyline line (12f)
                    drawPath(
                        path = path,
                        color = Color(0xFFFF6500),
                        style = Stroke(
                            width = 12f * scale,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Start Marker (🟢 Green dot) and Finish Marker (🔴 Red dot)
                val startPt = screenPoints.first()
                val finishPt = screenPoints.last()

                // Start Green Dot
                drawCircle(
                    color = Color(0xFF00E676).copy(alpha = 0.4f),
                    radius = 14f * scale,
                    center = startPt
                )
                drawCircle(
                    color = Color(0xFF00E676),
                    radius = 8f * scale,
                    center = startPt
                )

                // Finish Red Dot
                drawCircle(
                    color = Color(0xFFFF1744).copy(alpha = 0.4f),
                    radius = 14f * scale,
                    center = finishPt
                )
                drawCircle(
                    color = Color(0xFFFF1744),
                    radius = 8f * scale,
                    center = finishPt
                )
            }
        }
    }
}

/**
 * Route Metrics Overlay component displaying Total Distance, Pace, Elevation Gain, and Start/Finish Time.
 */
@Composable
fun RouteMetricsOverlay(
    totalDistanceKm: Double,
    paceMinKm: String,
    elevationGainMeters: Int,
    timeRangeStr: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0F1318).copy(alpha = 0.88f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF2E3844)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricOverlayItem(
                icon = Icons.Rounded.Straighten,
                label = "Distancia",
                value = String.format(Locale.US, "%.2f km", totalDistanceKm),
                tint = Color(0xFFFF9100)
            )

            MetricOverlayItem(
                icon = Icons.Rounded.Speed,
                label = "Ritmo",
                value = paceMinKm,
                tint = Color(0xFF00E676)
            )

            MetricOverlayItem(
                icon = Icons.Rounded.FilterHdr,
                label = "Desnivel",
                value = "+$elevationGainMeters m",
                tint = Color(0xFF29B6F6)
            )

            MetricOverlayItem(
                icon = Icons.Rounded.Schedule,
                label = "Horario",
                value = timeRangeStr,
                tint = Color(0xFFE0E0E0)
            )
        }
    }
}

@Composable
private fun MetricOverlayItem(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = Color.LightGray
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = Color.White
        )
    }
}

@Preview
@Composable
fun GoogleMapRouteWindowPreview() {
    GoogleMapRouteWindow(
        gpsPoints = GpsRouteUtils.generateSampleRunRoute(),
        totalDistanceKm = 5.25,
        paceMinKm = "4'38\" /km",
        elevationGainMeters = 42,
        startTimeStr = "08:15",
        finishTimeStr = "08:43"
    )
}
