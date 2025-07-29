package com.corner.ui.search

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowScope
import com.corner.bean.HotData
import com.corner.bean.SearchHistoryCache
import com.corner.bean.SettingStore
import com.corner.bean.SettingType
import com.corner.ui.nav.vm.SearchViewModel
import com.corner.ui.scene.ControlBar

/**
 *历史记录
 *热门推荐
 */
@Composable
fun WindowScope.SearchPage(vm: SearchViewModel, onClickBack: () -> Unit, onSearch: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val model = vm.state.collectAsState()

    LaunchedEffect("focus") {
        focusRequester.requestFocus()
    }

    DisposableEffect(Unit) {
        vm.onCreate()
        onDispose {
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.align(alignment = Alignment.TopStart).padding(horizontal = 16.dp, vertical = 8.dp)) {
            // TopBar
            WindowDraggableArea {
                ControlBar(leading = {
                    Row(
                        modifier = Modifier.align(Alignment.Start)/*.background(MaterialTheme.colors.background)*/
                            .height(80.dp)
                    ) {
                        FilledTonalIconButton(
                            modifier = Modifier
                                .fillMaxHeight().width(60.dp).padding(vertical = 8.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            onClick = {
                                vm.clear()
                                onClickBack()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "返回",
                                modifier = Modifier.height(20.dp).width(20.dp)
                            )
                        }
                        SearchBar(
                            vm,
                            Modifier,
                            focusRequester,
                            "",
                            onSearch,
                            false
                        )
                    }
                }, center = {})
            }
            Column(Modifier.fillMaxSize()) {
                if (model.value.historyList.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.4f)
                            .padding(vertical = 8.dp) // 增加垂直间距
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(10.dp)
                                )
                        ) {
                            // 在历史记录标题区域添加删除全部按钮
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 24.dp, top = 12.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "搜索历史",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        // 删除全部历史记录
                                        SettingStore.getCache(SettingType.SEARCHHISTORY.id)?.let {
                                            (it as? SearchHistoryCache)?.let { cache ->
                                                cache.searchHistoryList.clear()
                                                SettingStore.write()
                                                vm.onCreate()
                                            }
                                        }
                                    }
                                ) {
                                    Text("清空全部", color = MaterialTheme.colorScheme.error)
                                }
                            }

                            // 内容区域
                            if (model.value.historyList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "暂无搜索历史",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))// 裁剪溢出内容
                                        .fillMaxSize()
                                ) {
                                    LazyVerticalGrid(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp),
                                        columns = GridCells.FixedSize(200.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(
                                            start = 16.dp,
                                            end = 16.dp
                                        )
                                    ) {
                                        items(model.value.historyList.toList()) { query ->
                                            HistoryItem(query = query, onClick = { onSearch(query) }, vm = vm)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                //热搜
                if (model.value.hotList.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {

                        Column(modifier = Modifier.fillMaxSize()) {
                            // 1. 标题区域
                            Text(
                                text = "热搜",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .padding(start = 24.dp, top = 12.dp, bottom = 8.dp)
                            )

                            // 2. 热搜内容区域
                            Box(modifier = Modifier.weight(1f)) {
                                LazyVerticalGrid(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    columns = GridCells.FixedSize(100.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        end = 16.dp
                                    )
                                ) {
                                    items(model.value.hotList) { item ->
                                        HotItem(
                                            hotData = item,
                                            onClick = { onSearch(it.title) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun HotPanel(modifier: Modifier, hots: List<HotData>, onClick: (HotData) -> Unit) {
    val hotList by rememberUpdatedState(hots)
    Column(modifier = modifier) {
        Text(
            "热搜",
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 15.dp),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium
        )
        LazyHorizontalStaggeredGrid(
            rows = StaggeredGridCells.Adaptive(80.dp),
            state = rememberLazyStaggeredGridState(), contentPadding = PaddingValues(10.dp),
            horizontalItemSpacing = 8.dp,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
            userScrollEnabled = true
        ) {
            items(hotList.toList(), key = { i -> i.title + i.hashCode() }) { hotData ->
                HotItem(Modifier.wrapContentHeight(), hotData) {
                    onClick(it)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HotItem(
    modifier: Modifier = Modifier,
    hotData: HotData,
    onClick: (HotData) -> Unit
) {
    // 统一样式配置
    val cardShape = RoundedCornerShape(12.dp)
    val tooltipShape = RoundedCornerShape(8.dp)
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val tooltipBorderColor = Color.Gray.copy(blue = 0.6f)

    TooltipArea(
        tooltip = {
            // 增强的工具提示
            Surface(
                modifier = Modifier.shadow(8.dp, tooltipShape),
                shape = tooltipShape,
                border = BorderStroke(1.dp, tooltipBorderColor),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .padding(16.dp)
                ) {
                    if (hotData.comment.isNotEmpty()) {
                        Text(
                            text = hotData.comment,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Text(
                        text = hotData.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        delayMillis = 500,
        tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset.Zero)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(80.dp),
            shape = cardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            onClick = { onClick(hotData) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 可滚动文本区域
                Box(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Column {
                        // 标题文本（自动滚动）
                        Text(
                            text = hotData.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 副文本（自动滚动）
                        Text(
                            text = hotData.upinfo.trim(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee()
                        )
                    }
                }
            }
        }
    }
}


/*
@Preview
@Composable
fun previewHotItem() {
    AppTheme {
        val hot = HotData("阿凡达", "潘多拉", "更新到第二季", "hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhfffffff")
        HotItem(Modifier, hot) {}
    }
}

@Preview
@Composable
fun previewHotPanel() {
    AppTheme {
        val hot = HotData("阿凡达", "潘多拉", "更新到第二季", "hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhfffffff")
        val hots = mutableListOf<HotData>()
        for (i in 0 until 10) {
            hots.add(hot.copy(title = hot.title + i))
        }
        HotPanel(Modifier, hots) {}
    }
}

@Composable
fun HistoryPanel(
    modifier: Modifier = Modifier,
    histories: Set<String>,
    onClick: (String) -> Unit
) {
    val list by rememberUpdatedState(histories)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp) // 固定高度保持一致性
            .border(1.dp, Color.Red) // 调试边框
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. 标题区域（与热搜样式一致）
            Text(
                text = "搜索历史",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .padding(start = 24.dp, top = 12.dp, bottom = 8.dp)
            )

            // 2. 横向滚动内容区域
            Box(modifier = Modifier.weight(1f)) {
                val scrollState = rememberLazyListState()

                LazyRow(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    state = scrollState,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(list.toList()) { query ->
                        HistoryItem(
                            modifier = Modifier.height(55.dp), // 固定高度
                            query = query,
                            onClick = onClick
                        )
                    }
                }

                // 3. 滚动条（与热搜样式一致）
                HorizontalScrollbar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp),
                    adapter = rememberScrollbarAdapter(scrollState),
                    style = ScrollbarStyle(
                        minimalHeight = 4.dp,
                        thickness = 4.dp,
                        shape = RoundedCornerShape(2.dp),
                        hoverDurationMillis = 300,
                        unhoverColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        hoverColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}
*/
@Composable
private fun HistoryItem(
    modifier: Modifier = Modifier,
    query: String,
    onClick: (String) -> Unit,
    vm: SearchViewModel
) {
    Card(
        modifier = modifier
            .widthIn(min = 80.dp, max = 200.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { onClick(query) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = query,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    // 调用删除单个历史记录的方法
                    SettingStore.deleteSearchHistory(query)
                    vm.onCreate()
                },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}