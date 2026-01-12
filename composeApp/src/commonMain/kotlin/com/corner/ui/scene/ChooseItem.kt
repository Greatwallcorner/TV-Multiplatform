package com.corner.ui.scene

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RatioBtn(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    selected: Boolean,
    loading: Boolean = false,
    tag: () -> Pair<Boolean, String> = { false to "" },
    enableTooltip: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val animatedProgress by animateFloatAsState(
        targetValue = if (loading) 1f else 0f,
        animationSpec = tween(300),
        label = "loading_indicator"
    )
    Spacer(modifier = Modifier.width(12.dp))

    val buttonContent = @Composable {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = when {
                selected -> MaterialTheme.colorScheme.primaryContainer
                pressed -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            border = BorderStroke(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            shadowElevation = if (selected) 4.dp else 1.dp,
            tonalElevation = if (selected) 2.dp else 0.dp,
            modifier = modifier
                .height(36.dp)
                .width(IntrinsicSize.Min)
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true),
                    onClick = { onClick() },
                    enabled = !loading
                )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (loading) {
                        Box(modifier = modifier.width(24.dp).height(24.dp)) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .width(24.dp)
                                    .height(24.dp)
                                    .alpha(animatedProgress),
                                strokeWidth = 2.5.dp,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                },
                                strokeCap = StrokeCap.Round,
                                trackColor = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    val tags = remember { tag() }
                    Text(
                        text = text,
                        color = when {
                            selected -> MaterialTheme.colorScheme.onPrimaryContainer
                            pressed -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (tags.first) Modifier.weight(1f, fill = false) else Modifier
                    )

                    if (tags.first) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.secondaryContainer,
                            border = BorderStroke(
                                0.5.dp,
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        ) {
                            Text(
                                tags.second,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (enableTooltip) {
        TooltipArea(
            tooltip = {
                Surface(
                    modifier = Modifier.shadow(8.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = text,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            delayMillis = 500,
            tooltipPlacement = TooltipPlacement.CursorPoint(
                alignment = Alignment.BottomCenter,
                offset = if (selected) DpOffset(0.dp, 8.dp) else DpOffset.Zero
            )
        ) {
            buttonContent()
        }
    } else {
        buttonContent()
    }
}