package com.corner.ui

import SiteViewModel
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowScope
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.viewmodel.GlobalAppState.hideProgress
import com.corner.catvodcore.viewmodel.GlobalAppState.showProgress
import com.corner.database.dao.buildVod
import com.corner.database.entity.History
import com.corner.ui.nav.vm.HistoryViewModel
import com.corner.ui.scene.BackRow
import com.corner.ui.scene.ControlBar
import com.seiko.imageloader.ui.AutoSizeImage
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import tv_multiplatform.composeapp.generated.resources.Res
import tv_multiplatform.composeapp.generated.resources.loading

@Composable
fun HistoryItem(
    modifier: Modifier,
    history: History,
    showSite: Boolean,
    onDelete: (String) -> Unit,
    click: (History) -> Unit
) {
    Card(
        modifier = modifier.clickable(enabled = true, onClick = { click(history) }),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        ContextMenuArea(items = {
            listOf(ContextMenuItem("删除") {
                onDelete(history.key)
            })
        }) {
            Box(modifier = modifier) {
                // 新增删除按钮
                IconButton(
                    onClick = { onDelete(history.key) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                AutoSizeImage(
                    url = history.vodPic!!,
                    modifier = Modifier.height(220.dp),
                    contentDescription = history.vodName,
                    contentScale = ContentScale.Crop,
                    placeholderPainter = { painterResource(Res.drawable.loading) },
                    errorPainter = { painterResource(Res.drawable.loading) })
                Text(
                    text = history.vodName!!,
                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth().padding(0.dp, 10.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(textAlign = TextAlign.Center)
                )
                Text(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .padding(5.dp),
                    text = if (showSite) history.vodFlag ?: "" else history.vodRemarks!!,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        color = Color.White,
                        shadow = Shadow(Color.Black, offset = Offset(2F, 2F), blurRadius = 1.5F)
                    )
                )
            }
        }
    }
}

@Composable
fun WindowScope.HistoryScene(vm: HistoryViewModel, onClickItem: (Vod) -> Unit, onClickBack: () -> Unit) {
    val model = vm.state.collectAsState()
    var chooseHistory by remember { mutableStateOf<History?>(null) }
    LaunchedEffect(Unit) {
        showProgress()
        SiteViewModel.viewModelScope.launch {
            try {
                vm.fetchHistoryList()
            } finally {
                hideProgress()
            }
        }
    }
    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),) {
            WindowDraggableArea {
                ControlBar(
                    leading = {
                        BackRow(modifier = Modifier.align(Alignment.Start), { onClickBack() }) {
                            Row(
                                modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "历史记录",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.15.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        var showConfirmDialog by remember { mutableStateOf(false) }

                        // 主按钮
                        FilledTonalButton(
                            modifier = Modifier
                                .height(40.dp)
                                .padding(end = 12.dp),
                            onClick = { showConfirmDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "清空全部",
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "清空",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        // 确认对话框
                        if (showConfirmDialog) {
                            AlertDialog(
                                onDismissRequest = { showConfirmDialog = false },
                                title = {
                                    Text(
                                        "确认清空历史记录",
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                },
                                text = {
                                    Text(
                                        "此操作将永久删除所有历史记录，且不可恢复。",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                confirmButton = {
                                    FilledTonalButton(
                                        onClick = {
                                            showConfirmDialog = false
                                            vm.clearHistory()
                                            vm.fetchHistoryList()
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("确认清空")
                                    }
                                },
                                dismissButton = {
                                    OutlinedButton(
                                        onClick = { showConfirmDialog = false },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("取消")
                                    }
                                }
                            )
                        }
                    }
                )
            }
            val gridState = remember { LazyGridState() }
            LazyVerticalGrid(
                modifier = Modifier.padding(horizontal = 10.dp),
                columns = GridCells.Adaptive(140.dp),
                state = gridState,
                contentPadding = PaddingValues(5.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                userScrollEnabled = true,
            ) {
                itemsIndexed(items = model.value.historyList) { _:Int, it:History ->
                    HistoryItem(
                        Modifier,
                        it, showSite = false, onDelete = { key ->
                            vm.deleteBatchHistory(listOf(key))
                        }) {
                        onClickItem(it.buildVod())
                        chooseHistory = it
//                        showDetailDialog = true
                    }
                }
            }
            VerticalScrollbar(rememberScrollbarAdapter(gridState))
        }
    }
}