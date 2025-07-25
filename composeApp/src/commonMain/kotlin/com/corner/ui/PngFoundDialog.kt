package com.corner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corner.ui.nav.data.DialogState
import com.corner.ui.nav.vm.DetailViewModel
import com.corner.ui.video.log
import com.corner.util.BrowserUtils

@Composable
fun PngFoundDialog(
    m3u8Url: String,
    onDismiss: () -> Unit,
    onOpenInBrowser: () -> Unit,
    vm: DetailViewModel
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties()
        ) {
            Surface(
                modifier = Modifier
                    .shadow(16.dp, RoundedCornerShape(16.dp))
                    .fillMaxWidth(0.8f),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "M3U8 文件中发现 .png 链接，是否跳转到浏览器加载 M3U8 文件？",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "取消",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.padding(8.dp))

                        Button(
                            onClick = {
                                // 从 viewModel 的状态中获取当前选中的剧集 URL
                                val currentSelectedEpUrl = vm.currentSelectedEpUrl.value
                                // 从状态数据里找到对应的剧集
                                val currentEpisode = vm.state.value.detail.subEpisode.find { it.url == currentSelectedEpUrl }
                                val episodeName = vm.state.value.detail.vodName ?: ""
                                val episodeNumber = currentEpisode?.number ?: 0
                                // 记录用户选择在浏览器打开
                                DialogState.userChoseOpenInBrowser = true
                                log?.debug("Name is {},Number is {}",episodeName,episodeNumber)
                                BrowserUtils.openBrowserWithHtml(m3u8Url,episodeName,episodeNumber)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "跳转浏览器",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}