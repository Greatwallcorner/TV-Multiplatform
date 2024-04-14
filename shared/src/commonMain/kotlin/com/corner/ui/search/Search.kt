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
import com.corner.bean.SettingStore
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.bean.Collect
import com.corner.catvodcore.config.api
import com.corner.ui.decompose.SearchComponent
import com.corner.ui.decompose.component.DefaultSearchComponentComponent
import com.corner.ui.scene.RatioBtn
import com.corner.ui.video.DetailDialog
import com.corner.ui.video.VideoItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class SearchPageType {
    page,
    content
}

@Composable
fun SearchScene(component: DefaultSearchComponentComponent, onClickBack: () -> Unit) {
    val model = component.models.subscribeAsState()
    var currentPage by remember { mutableStateOf(SearchPageType.page) }
    var keyword by remember { mutableStateOf("") }
    when(currentPage){
        SearchPageType.page -> SearchPage(component, onClickBack = { onClickBack() }, onSearch = { s ->
            keyword = s
            currentPage = SearchPageType.content
        })
        SearchPageType.content -> SearchResult(model, keyword = keyword, onClickBack = { onClickBack() }, searching = model.value.isSearching.value)
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
private fun SearchResult(model:State<SearchComponent.Model>, keyword: String, onClickBack: () -> Unit, searching: Boolean) {
    var searchText by remember { mutableStateOf(keyword) }
    val result = remember { mutableStateOf(SiteViewModel.search) }
    var currentCollect by remember { mutableStateOf<Collect?>(SiteViewModel.search[0]) }
    var currentVodList by remember { mutableStateOf(SiteViewModel.search[0].getList()) }
    val state = rememberLazyGridState()
    val adapter = rememberScrollbarAdapter(state)

    var showDetailDialog by remember { mutableStateOf(false) }
    var chooseVod by remember { mutableStateOf<Vod?>(null) }
 
    DisposableEffect(searchText) {
        model.value.isSearching.value = true
        model.value.cancelAndClearJobList()
        SiteViewModel.clearSearch()
        SiteViewModel.viewModelScope.launch {
            SettingStore.addSearchHistory(keyword)
            val searchableSites = api?.sites?.filter { it.searchable == 1 }
            searchableSites?.forEach {
                val job = model.value.searchScope.launch {
                    SiteViewModel.searchContent(it, searchText, false)
                }
                job.invokeOnCompletion {
                    currentVodList =
                        SiteViewModel.search.find { it.isActivated().value }?.getList()?.toList()?.toMutableList()
                }
                model.value.jobList.add(job)
            }
            model.value.searchScope.coroutineContext[Job]?.children?.forEach { it.join() }
            model.value.isSearching.value = false
        }

        onDispose {
            SiteViewModel.clearSearch()
            model.value.cancelAndClearJobList()
            model.value.isSearching.value = false
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
                                    chooseVod = it
                                    showDetailDialog = true
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

        DetailDialog(showDetailDialog, chooseVod, chooseVod?.site?.key ?: "") {
            showDetailDialog = false
        }
    }
}
