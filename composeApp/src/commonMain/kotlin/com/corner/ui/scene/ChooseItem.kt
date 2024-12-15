package com.corner.ui.scene

import AppTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RatioBtn(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    selected: Boolean,
    loading: Boolean = false,
    tag: () -> Pair<Boolean, String> = { false to "" }
) {
    Surface(
        border = BorderStroke(1.dp, Color.Gray.copy(0.6F)),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
            .shadow(1.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(
                onClick = { onClick() },
                enabled = !loading
            ),
    ) {
        Box() {
            TooltipArea(
                tooltip = {
                    // composable tooltip content
                    Surface(
                        modifier = Modifier.shadow(4.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = text,
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                delayMillis = 600
            ) {
                Text(
                    text,
                    modifier = Modifier.background(if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    color = if (selected) MaterialTheme.colorScheme.onSecondary else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(1.dp),
                        color = Color.White,
                        trackColor = Color.Gray
                    )
                }

                val tags = remember { tag() }
                if (tags.first) {
                    Text(
                        tags.second,
                        Modifier.background(if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.secondary /*if (selected) Color.Blue.copy(0.5f) else MaterialTheme.colorScheme.onSecondary*/ ,
                            shape = RoundedCornerShape(0.dp, 6.dp, 0.dp,6.dp))
                            .align(Alignment.TopEnd)
                            .border(1.dp, Color.Gray)
                            .padding(3.dp)
                            , maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSecondary /*if(selected) Color.Black else Color.White*/,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun RatioBtnPreview() {
    AppTheme {
        Column {
//            RatioBtn("测试", onClick = {}, true, loading = true)
//            Spacer(Modifier.size(15.dp))
            RatioBtn(Modifier, "测试2", onClick = {}, false, loading = true)
        }
    }
}
