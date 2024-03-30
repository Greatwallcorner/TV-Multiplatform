package com.corner.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun ControlBar(onClickMinimize: () -> Unit, onClickMaximize: () -> Unit, onClickClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(35.dp)
            .background(MaterialTheme.colors.background.copy(blue = 0.15f))
            .clip(RoundedCornerShape(12.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Icon(
            Icons.Default.Minimize, contentDescription = "minimize", tint = MaterialTheme.colors.onBackground,
            modifier = Modifier.clickable(onClick = { onClickMinimize() }).size(30.dp).padding(2.dp)
        )
        Icon(
            Icons.Default.KeyboardArrowUp, contentDescription = "maximize", tint = MaterialTheme.colors.onBackground,
            modifier = Modifier.clickable(onClick = { onClickMaximize() }).size(30.dp).padding(2.dp)
        )
        Icon(
            Icons.Default.Close, contentDescription = "close", tint = MaterialTheme.colors.onBackground,
            modifier = Modifier.clickable(onClick = { onClickClose() }).size(30.dp).padding(2.dp)
        )
    }
}

@Composable
@Preview
fun previewControlBar() {
    MaterialTheme {
        ControlBar({}, {}, {})
    }
}