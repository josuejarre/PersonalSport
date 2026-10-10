package com.example.personalsport.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modifier that applies a subtle dark metallic perforated mesh pattern (grid of subtle dots).
 */
fun Modifier.perforatedMeshBackground(
    backgroundColor: Color = CarbonDark,
    dotColor: Color = DarkSteelAccent,
    dotRadiusDp: Dp = 1.2.dp,
    spacingDp: Dp = 12.dp
): Modifier = composed {
    val density = LocalDensity.current
    val dotRadiusPx = with(density) { dotRadiusDp.toPx() }
    val spacingPx = with(density) { spacingDp.toPx() }

    this.drawBehind {
        drawRect(color = backgroundColor)
        
        val width = size.width
        val height = size.height

        var row = 0
        var y = spacingPx / 2f
        while (y < height) {
            val offsetX = if (row % 2 == 1) spacingPx / 2f else 0f
            var x = offsetX + spacingPx / 2f
            while (x < width) {
                drawCircle(
                    color = dotColor,
                    radius = dotRadiusPx,
                    center = Offset(x, y)
                )
                x += spacingPx
            }
            y += spacingPx
            row++
        }
    }
}

/**
 * Box composable helper with perforated mesh background.
 */
@Composable
fun PerforatedMeshBackground(
    modifier: Modifier = Modifier,
    backgroundColor: Color = CarbonDark,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .perforatedMeshBackground(backgroundColor = backgroundColor)
    ) {
        content()
    }
}

/**
 * Pro Gym Dark & Electric Orange Card container.
 * Features a subtle dark metallic background, crisp orange left accent border / chevrons,
 * subtle metallic border stroke, and rounded corners.
 */
@Composable
fun ProSportCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = DarkMetallicCard,
    borderColor: Color = MetallicGray,
    accentColor: Color = ElectricSportOrange,
    showAccentLine: Boolean = true,
    showChevrons: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardShape = shape
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Surface(
        modifier = modifier
            .clip(cardShape)
            .border(1.dp, borderColor, cardShape)
            .then(clickableModifier),
        shape = cardShape,
        color = backgroundColor,
        tonalElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (showAccentLine) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    accentColor,
                                    ElectricOrangeBright,
                                    accentColor.copy(alpha = 0.6f)
                                )
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (showAccentLine) 16.dp else 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    content()
                }

                if (showChevrons) {
                    Spacer(modifier = Modifier.width(8.dp))
                    ChevronAccent(accentColor = accentColor)
                }
            }
        }
    }
}

/**
 * Decorative chevron pattern / indicator for Pro Gym styling.
 */
@Composable
fun ChevronAccent(
    modifier: Modifier = Modifier,
    accentColor: Color = ElectricSportOrange
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((-6).dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = accentColor.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * TopAppBar themed for Pro Gym Dark & Electric Orange style.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProSportTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkMetallicCard,
            scrolledContainerColor = DarkMetallicCard,
            navigationIconContentColor = ElectricSportOrange,
            titleContentColor = TextPrimaryWhite,
            actionIconContentColor = ElectricSportOrange
        ),
        modifier = modifier.border(
            width = 1.dp,
            color = MetallicGray.copy(alpha = 0.5f)
        )
    )
}
