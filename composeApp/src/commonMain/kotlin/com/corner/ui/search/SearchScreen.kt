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
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.bean.Collect
import com.corner.catvodcore.config.ApiConfig
import com.corner.ui.nav.vm.SearchViewModel
import com.corner.ui.navigation.SearchScreen
import com.corner.ui.scene.ControlBar
import com.corner.ui.scene.RatioBtn
import com.corner.ui.video.VideoItem
import org.jetbrains.compose.resources.painterResource
import tv_multiplatform.composeapp.generated.resources.Res
import tv_multiplatform.composeapp.generated.resources.nothing

enum class SearchPageType {
    page,
    content
}

@Composable
fun WindowScope.SearchScene(vm: SearchViewModel, onClickItem: (Vod) -> Unit, onClickBack: () -> Unit) {
    var selectPage by remember { mutableStateOf(SearchScreen.Search) }

    when(selectPage) {
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

//@Composable
//@Preview
//fun previewSearchPage(){
//    AppTheme(useDarkTheme = true) {
//        SearchPage {}
//    }
//}

@Composable
private fun WindowScope.SearchResult(
    vm: SearchViewModel,
    onClickBack: () -> Unit,
    onClickItem: (Vod) -> Unit
) {
    val model = vm.state.collectAsState()
    val searchText = remember { derivedStateOf { model.value.keyword } }
    val result = remember { SiteViewModel.search }
    var currentCollect by remember { mutableStateOf<Collect?>(SiteViewModel.search.value[0]) }
    val currentVodList by rememberUpdatedState(model.value.currentVodList)

    val state = rememberLazyGridState()
    val adapter = rememberScrollbarAdapter(state)

    val showLoadMore = remember {
        derivedStateOf {
            model.value.searchCompleteSites.size < (ApiConfig.api.sites.filter { it.searchable == 1 }.size)
        }
    }

    DisposableEffect(searchText.value) {
        vm.search(searchText.value, false)
        onDispose {
            vm.onPause()
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            WindowDraggableArea {
                //        TopBar
                ControlBar(leading = {
                    IconButton(
                        modifier = Modifier
                            .padding(start = 20.dp, end = 20.dp),
                        onClick = {
                            vm.clear()
                            onClickBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "back to video home",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    SearchBar(
                        vm,
                        Modifier,
                        remember { FocusRequester() },
                        searchText.value,
                        onSearch = { s ->
                            vm.onSearch(s)
                        }, model.value.isSearching
                    )
                }) { }
            }
//        Content
            Row {
                Box(Modifier.fillMaxWidth(0.2f).fillMaxHeight()) {
                    LazyColumn(
                        modifier = Modifier
                            .defaultMinSize(30.dp)
                            .padding(horizontal = 8.dp)
                            .background(Color.Gray.copy(0.4f)),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        items(items = result.value.toList()) { item: Collect ->
                            RatioBtn(
                                text = item.site?.name ?: "",
                                onClick = {
                                    currentCollect = item
                                    vm.onClickCollection(item)
                                    result.value.forEach { i ->
                                        i.activated.value = (i.site?.key == item.site?.key)
                                    }
                                },
                                selected = item.activated.value,
                            )
                        }
                    }
                    Surface(Modifier.align(Alignment.BottomCenter).padding(vertical = 10.dp, horizontal = 8.dp)) {
                        RatioBtn(
                            Modifier.height(45.dp),
                            text = if (showLoadMore.value) "加载更多" else "没有更多",
                            onClick = {
                                vm.search(searchText.value, true)
                            },
                            selected = false,
                            loading = model.value.isSearching,
                            enableTooltip = false
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    if (currentVodList.value.isEmpty()) {
                        Image(
                            modifier = Modifier.align(Alignment.Center),
                            painter = painterResource(Res.drawable.nothing),
                            contentDescription = "nothing here",
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        LazyVerticalGrid(
                            modifier = Modifier.padding(15.dp),
                            columns = GridCells.Adaptive(140.dp),
                            contentPadding = PaddingValues(5.dp),
                            state = state,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            userScrollEnabled = true
                        ) {
                            items(items = currentVodList.value.toList()) { item ->
                                VideoItem(Modifier, item, true) {
                                    onClickItem(it)
                                }
                            }
                        }
                        VerticalScrollbar(
                            adapter, modifier = Modifier.align(Alignment.CenterEnd),
                            style = defaultScrollbarStyle().copy(
                                unhoverColor = Color.DarkGray.copy(0.3F),
                                hoverColor = Color.DarkGray
                            )
                        )
                    }
                }
            }
        }
    }
}
