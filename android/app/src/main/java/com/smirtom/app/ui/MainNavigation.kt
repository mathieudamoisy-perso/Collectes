package com.smirtom.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smirtom.app.data.WasteType
import kotlin.math.abs

@Composable
fun BottomBarOverlay(
    pagerState: PagerState,
    scrollState: BottomBarScrollState,
    onTabSelected: (AppTab) -> Unit,
    onHeightChanged: (Dp) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibility = rememberBottomBarVisibility(scrollState)
    SmirtomBottomBar(
        pagerState = pagerState,
        onTabSelected = onTabSelected,
        visibility = visibility,
        onHeightChanged = onHeightChanged,
        modifier = modifier
    )
}

enum class AppTab(val label: String) {
    Collectes("Collectes"),
    Guide("Guide tri"),
    Settings("Paramètres")
}

private data class TabIconSet(
    val outlined: ImageVector,
    val filled: ImageVector
)

private val tabIcons = mapOf(
    AppTab.Collectes to TabIconSet(Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    AppTab.Guide to TabIconSet(Icons.Outlined.Recycling, Icons.Filled.Recycling),
    AppTab.Settings to TabIconSet(Icons.Outlined.Settings, Icons.Filled.Settings)
)

@Composable
fun SmirtomBottomBar(
    pagerState: PagerState,
    onTabSelected: (AppTab) -> Unit,
    visibility: Float,
    onHeightChanged: (Dp) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tabs = AppTab.entries
    val density = LocalDensity.current
    val pagerPosition by remember {
        derivedStateOf { pagerState.currentPage + pagerState.currentPageOffsetFraction }
    }
    val barColor = MaterialTheme.colorScheme.surface
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                onHeightChanged(with(density) { size.height.toDp() })
            }
            .alpha(if (visibility < 0.05f) 0f else 1f)
            .graphicsLayer {
                val hiddenOffset = size.height * (1f - visibility)
                translationY = hiddenOffset
                alpha = visibility.coerceIn(0f, 1f)
            }
            .background(barColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f * visibility)
            )
            TabTopStripe(
                pagerPosition = pagerPosition,
                tabCount = tabs.size,
                color = selectedColor
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 2.dp)
                .navigationBarsPadding()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val weight = tabSelectionWeight(pagerPosition, index)
                val tint = lerp(unselectedColor, selectedColor, weight)
                val icons = tabIcons.getValue(tab)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember(tab) { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(tab) }
                        )
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    TabBarIcon(
                        outlinedIcon = icons.outlined,
                        filledIcon = icons.filled,
                        label = tab.label,
                        selectionWeight = weight,
                        tint = tint,
                        selectedColor = selectedColor
                    )
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.graphicsLayer {
                            alpha = 0.72f + 0.28f * weight
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabBarIcon(
    outlinedIcon: ImageVector,
    filledIcon: ImageVector,
    label: String,
    selectionWeight: Float,
    tint: androidx.compose.ui.graphics.Color,
    selectedColor: androidx.compose.ui.graphics.Color
) {
    val animatedWeight by animateFloatAsState(
        targetValue = selectionWeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tabIconWeight"
    )

    Box(
        modifier = Modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .graphicsLayer {
                    val pillScale = 0.6f + 0.4f * animatedWeight
                    scaleX = pillScale
                    scaleY = pillScale
                    alpha = animatedWeight * 0.22f
                }
                .background(selectedColor.copy(alpha = 0.14f), CircleShape)
        )

        Icon(
            imageVector = outlinedIcon,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    alpha = 1f - animatedWeight
                    scaleX = 0.92f - 0.04f * animatedWeight
                    scaleY = 0.92f - 0.04f * animatedWeight
                },
            tint = tint.copy(alpha = tint.alpha * (1f - animatedWeight * 0.35f))
        )

        Icon(
            imageVector = filledIcon,
            contentDescription = label,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    alpha = animatedWeight
                    scaleX = 0.82f + 0.18f * animatedWeight
                    scaleY = 0.82f + 0.18f * animatedWeight
                    translationY = -2.5f * animatedWeight
                },
            tint = tint
        )
    }
}

@Composable
private fun TabTopStripe(
    pagerPosition: Float,
    tabCount: Int,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
    ) {
        val tabWidthPx = with(density) { (maxWidth / tabCount).toPx() }
        val stripeWidthPx = tabWidthPx * 0.68f
        val offsetXPx = tabWidthPx * pagerPosition + (tabWidthPx - stripeWidthPx) / 2f

        Box(
            modifier = Modifier
                .width(with(density) { stripeWidthPx.toDp() })
                .height(2.dp)
                .graphicsLayer {
                    translationX = offsetXPx
                }
                .background(color)
        )
    }
}

private fun tabSelectionWeight(pagerPosition: Float, tabIndex: Int): Float {
    val distance = abs(pagerPosition - tabIndex)
    return (1f - distance).coerceIn(0f, 1f)
}

data class AppNavigationState(
    val selectedTab: AppTab = AppTab.Collectes,
    val guideDetailType: WasteType? = null
)
