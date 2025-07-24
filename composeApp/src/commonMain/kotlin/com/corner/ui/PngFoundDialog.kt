package com.corner.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import com.corner.util.BrowserUtils

@Composable
fun PngFoundDialog(
    m3u8Url: String,
    onDismiss: () -> Unit,
    onOpenInBrowser: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties()
    ) {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "M3U8 文件中发现 .png 链接，是否跳转到浏览器加载 M3U8 文件？")
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("取消")
                    }
                    Button(onClick = {
                        BrowserUtils.openBrowserWithHtml(m3u8Url)
                    }) {
                        Text("跳转浏览器")
                    }
                }
            }
        }
    }
}