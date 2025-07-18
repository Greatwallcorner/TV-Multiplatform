package com.corner.ui.search

import SiteViewModel
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowScope
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.bean.Collect
import com.corner.catvodcore.config.ApiConfig
import com.corner.ui.nav.vm.SearchViewModel
import com.corner.ui.navigation.SearchScreen
import com.corner.ui.scene.ControlBar
import com.corner.ui.scene.RatioBtn
import com.corner.ui.video.VideoItem

enum class SearchPageType {
    page,
    content
}

private val gridConfig = object {
    val itemMinSize = 150.dp
    val spacing = 12.dp
    val padding = 16.dp
    val scrollbarWidth = 8.dp
}

@Composable
fun WindowScope.SearchScene(vm: SearchViewModel, onClickItem: (Vod) -> Unit, onClickBack: () -> Unit) {
    var selectPage by remember { mutableStateOf(SearchScreen.Search) }

    when (selectPage) {
        SearchScreen.Search -> SearchPage(vm, onClickBack = {
            onClickBack()
        }, onSearch = { s ->
            vm.onSearch(s)
            selectPage = SearchScreen.SearchResult
        })

        SearchScreen.SearchResult -> SearchResult(
            vm, onClickBack = { onClickBack() },
        ) {
            onClickItem(it)
        }
    }
}

/*
@Composable
@Preview
fun previewSearchPage(){
    AppTheme(useDarkTheme = true) {
        SearchPage {}
    }
}
*/

@Composable
private fun WindowScope.SearchResult(
    vm: SearchViewModel,
    onClickBack: () -> Unit,
    onClickItem: (Vod) -> Unit
) {
    val model = vm.state.collectAsState()
    val searchText = remember { derivedStateOf { model.value.keyword } }
    val result = remember { SiteViewModel.search }
//    var currentCollect by remember { mutableStateOf<Collect?>(SiteViewModel.search.value[0]) }
    val currentVodList by rememberUpdatedState(model.value.currentVodList)

    //焦点控制
    val focusRequester = remember { FocusRequester() }

    val state = rememberLazyGridState()
    val adapter = rememberScrollbarAdapter(state)

    val showLoadMore = remember {
        derivedStateOf {
            model.value.searchCompleteSites.size < (ApiConfig.api.sites.filter { it.searchable == 1 }.size)
        }
    }

    DisposableEffect(searchText.value) {
        if (searchText.value.isNotBlank()) {
            vm.search(searchText.value, false)
        }
        onDispose {
            vm.onPause()
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp, vertical = 8.dp)) {
            WindowDraggableArea {
                //TopBar
                ControlBar(
                    modifier = Modifier.height(64.dp), // 固定高度保证一致性
                    leading = {
                        FilledTonalIconButton(
                            modifier = Modifier
                                .height(40.dp).width(60.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                vm.clear()
                                onClickBack()
                            }
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
                            focusRequester = focusRequester,
                            searchText.value,
                            onSearch = { s ->
                                vm.onSearch(s)
                            }, model.value.isSearching,
                            // 自定义焦点请求逻辑
                            onFocusRequested = { focusRequester.requestFocus() }
                        )
                    }
                )
            }
//        Content
            Row {
                Box(
                    Modifier.widthIn(min = 120.dp, max = 200.dp)
                        .fillMaxHeight()
                        .padding(top = 12.dp, start = 8.dp, bottom = 10.dp)
                ) {
                    // 列表容器
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 5.dp, bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 1.dp,
                        tonalElevation = 1.dp
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .defaultMinSize(30.dp)
                                .padding(horizontal = 8.dp,vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(items = result.value.toList()) { item: Collect ->
                                RatioBtn(
                                    text = item.site?.name ?: "",
                                    onClick = {
//                                    currentCollect = item
                                        vm.onClickCollection(item)
                                        result.value.forEach { i ->
                                            i.activated.value = (i.site?.key == item.site?.key)
                                        }
                                    },
                                    selected = item.activated.value,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(5.dp), // 增加水平内边距
                                )
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .width(150.dp)
                            .padding(bottom = 20.dp),
                        shape = RoundedCornerShape(18.dp),
                        shadowElevation = 2.dp,
                        tonalElevation = 1.dp
                    ) {
                        FilledTonalButton(
                            onClick = { vm.search(searchText.value, true) },
                            modifier = Modifier
                                .height(36.dp) // 更紧凑的高度
                                .width(145.dp),
                            enabled = showLoadMore.value && !model.value.isSearching,
                        ) {
                            if (model.value.isSearching) {
                                // 更小的加载指示器
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            } else {
                                Text(
                                    text = if (showLoadMore.value) "加载更多" else "没有更多",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                //搜索结果提示
                Box(modifier = Modifier.fillMaxSize()) {
                    if (currentVodList.value.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // 1. 图标
                            Icon(
                                imageVector = Icons.Outlined.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )

                            // 2. 主提示文本
                            Text(
                                text = "暂无搜索结果",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // 3. 辅助说明
                            Text(
                                text = "暂未找到与\"${searchText.value}\"相关的内容",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            // 4. 操作按钮
                            Row(
                                modifier = Modifier.padding(top = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        if (searchText.value.isNotBlank()) {
                                            vm.search(searchText.value, true)
                                        }
                                    },
                                    modifier = Modifier.height(40.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("重试搜索")
                                }

                                OutlinedButton(
                                    onClick = {
                                        //焦点转移到搜索栏
                                        focusRequester.requestFocus()
                                    },
                                    modifier = Modifier.height(40.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("修改关键词")
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = gridConfig.padding),
                            columns = GridCells.Adaptive(minSize = gridConfig.itemMinSize),
                            contentPadding = PaddingValues(vertical = gridConfig.padding),
                            state = state,
                            verticalArrangement = Arrangement.spacedBy(gridConfig.spacing),
                            horizontalArrangement = Arrangement.spacedBy(gridConfig.spacing)
                        ) {
                            items(items = currentVodList.value.toList()) { item ->
                                VideoItem(
                                    Modifier
                                        .shadow(
                                            elevation = 2.dp,
                                            shape = RoundedCornerShape(10.dp),
                                            spotColor = MaterialTheme.colorScheme.primary
                                        ),
                                    vod = item,
                                    showSite = true
                                ) {
                                    onClickItem(it)
                                }
                            }
                        }
                        VerticalScrollbar(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp)
                                .width(gridConfig.scrollbarWidth),
                            adapter = adapter,
                            style = ScrollbarStyle(
                                minimalHeight = 16.dp,
                                thickness = gridConfig.scrollbarWidth,
                                shape = RoundedCornerShape(100),
                                hoverDurationMillis = 300,
                                unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                hoverColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        }
    }
}
