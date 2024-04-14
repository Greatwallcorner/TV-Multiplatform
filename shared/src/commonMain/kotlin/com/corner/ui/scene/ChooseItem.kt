package com.corner.ui.scene

import AppTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun RatioBtn(text: String, onClick: () -> Unit, selected: Boolean, loading: Boolean = false) {
    Surface(
        border = BorderStroke(1.dp, Color.Gray.copy(0.6F)),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .shadow(1.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(
                onClick = { onClick() },
//                enabled = !selected
            ),
    ) {
        Box() {
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
                        .align(Alignment.CenterStart),
                    color = Color.White,
                    trackColor = Color.Gray
                )
            }
        }
    }
}

@Preview
@Composable
fun RatioBtnPreview() {
    AppTheme {
        Column {
            RatioBtn("测试", onClick = {}, true, loading = true)
            Spacer(Modifier.size(15.dp))
            RatioBtn("测试2", onClick = {}, false, loading = true)
        }
    }
}
