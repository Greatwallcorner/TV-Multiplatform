package com.corner.ui.search

import SiteViewModel
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.bean.Collect
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.decompose.component.DefaultSearchComponentComponent
import com.corner.ui.scene.RatioBtn
import com.corner.ui.video.VideoItem

enum class SearchPageType {
    page,
    content
}

@Composable
fun SearchScene(component: DefaultSearchComponentComponent,onClickItem: (Vod) -> Unit, onClickBack: () -> Unit) {
    val model = component.model.subscribeAsState()
    var currentPage by remember { mutableStateOf(SearchPageType.page) }
    var keyword by remember { mutableStateOf("") }
    when(currentPage){
        SearchPageType.page -> SearchPage(component, onClickBack = { onClickBack() }, onSearch = { s ->
            keyword = s
            currentPage = SearchPageType.content
        })
        SearchPageType.content -> SearchResult(component, keyword = keyword, onClickBack = { onClickBack() },
            searching = model.value.isSearching.value){
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
private fun SearchResult(
    component: DefaultSearchComponentComponent,
    keyword: String,
    onClickBack: () -> Unit,
    searching: Boolean,
    onClickItem:(Vod)->Unit) {
    var searchText by remember { mutableStateOf(keyword) }
    val result = remember { mutableStateOf(SiteViewModel.search) }
    var currentCollect by remember { mutableStateOf<Collect?>(SiteViewModel.search[0]) }
    var currentVodList by remember { mutableStateOf(SiteViewModel.search[0].getList()) }
    val state = rememberLazyGridState()
    val adapter = rememberScrollbarAdapter(state)

    DisposableEffect(searchText) {
        component.search(searchText)
        onDispose {
            component.clear()
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
//        TopBar
            Row(
                modifier = Modifier.align(Alignment.Start)
                    .background(MaterialTheme.colorScheme.surface)
                    .height(75.dp)
            ) {
                IconButton(
                    modifier = Modifier.align(Alignment.CenterVertically)
                        .padding(start = 20.dp, end = 20.dp),
                    onClick = { onClickBack() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "back to video home",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                SearchBar(
                    Modifier.align(Alignment.CenterVertically),
                    remember { FocusRequester() },
                    keyword,
                    onSearch = { s ->
                        searchText = s
                    }, searching
                )
                Spacer(Modifier.size(20.dp))
            }
//        Content
            Row {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(0.2f)
                        .defaultMinSize(30.dp)
                        .padding(horizontal = 8.dp)
                        .background(Color.Gray.copy(0.4f)),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(items = result.value) { item: Collect ->
                        RatioBtn(
                            text = item.getSite()?.name ?: "",
                            onClick = {
                                currentCollect = item
                                currentVodList = item.getList()
                                result.value.forEach { i ->
                                    i.setActivated(i.getSite()?.key == item.getSite()?.key)
                                }
                            },
                            item.isActivated().value,
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    if (currentVodList?.isEmpty() == true) {
                        Image(
                            modifier = Modifier.align(Alignment.Center),
                            painter = painterResource("nothing.png"),
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
                            itemsIndexed(currentVodList ?: listOf()) { _, item ->
                                VideoItem(Modifier, item, true) {
                                    GlobalModel.chooseVod.value = it
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
