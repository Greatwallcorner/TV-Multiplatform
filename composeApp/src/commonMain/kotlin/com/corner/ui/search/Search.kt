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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.pages.Pages
import com.arkivanov.decompose.extensions.compose.jetbrains.pages.PagesScrollAnimation
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.arkivanov.decompose.value.update
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.bean.Collect
import com.corner.catvodcore.config.ApiConfig
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.decompose.component.DefaultSearchComponent
import com.corner.ui.decompose.component.DefaultSearchPageComponent
import com.corner.ui.decompose.component.DefaultSearchPagesComponent
import com.corner.ui.scene.ControlBar
import com.corner.ui.scene.RatioBtn
import com.corner.ui.video.VideoItem

enum class SearchPageType {
    page,
    content
}

@OptIn(ExperimentalDecomposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun SearchScene(component: DefaultSearchPagesComponent, onClickItem: (Vod) -> Unit, onClickBack: () -> Unit) {
    Pages(
        component.pages,
        onPageSelected = component::selectPage,
        modifier = Modifier.fillMaxSize(),
        scrollAnimation = PagesScrollAnimation.Default
    ) { _, p ->
        when (p) {
            is DefaultSearchComponent -> SearchResult(
                p, onClickBack = { onClickBack() },
            ) {
                onClickItem(it)
            }

            is DefaultSearchPageComponent -> SearchPage(p, onClickBack = {
                onClickBack()
            }, onSearch = { s ->
                component.onSearch(s)
            })
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
private fun SearchResult(
    component: DefaultSearchComponent,
    onClickBack: () -> Unit,
    onClickItem: (Vod) -> Unit
) {
    val model = component.model.subscribeAsState()
    val searchText = GlobalModel.keyword.subscribeAsState()
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

    DisposableEffect(result.value){
        println("result 更改")
        onDispose {  }
    }

    DisposableEffect(searchText.value) {
        component.search(searchText.value, false)
        onDispose {
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
//        TopBar
            ControlBar(leading = {
                IconButton(
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp),
                    onClick = {
                        component.clear()
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
                    Modifier,
                    remember { FocusRequester() },
                    searchText.value,
                    onSearch = { s ->
                        GlobalModel.keyword.update { s }
                    }, model.value.isSearching
                )
            }) {  }
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
                                    component.onClickCollection(item)
                                    result.value.forEach { i ->
                                        i.activated.value = (i.site?.key == item.site?.key)
                                    }
                                },
                                item.activated.value,
                            )
                        }
                    }
                    Surface(Modifier.align(Alignment.BottomCenter).padding(vertical = 10.dp, horizontal = 8.dp)) {
                        RatioBtn(text = if (showLoadMore.value) "加载更多" else "没有更多", onClick = {
                            component.search( searchText.value, true)
                        }, false)
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    if (currentVodList.value.isEmpty()) {
                        Image(
                            modifier = Modifier.align(Alignment.Center),
                            painter = painterResource("/pic/nothing.png"),
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
