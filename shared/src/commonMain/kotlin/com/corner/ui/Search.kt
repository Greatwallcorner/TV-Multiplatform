package com.corner.ui

import SiteViewModel
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.bean.Collect
import com.corner.catvodcore.config.api
import com.corner.ui.scene.RatioBtn
import com.corner.ui.video.DetailDialog
import com.corner.ui.video.VideoItem
import kotlinx.coroutines.*
import org.apache.commons.lang3.StringUtils

/**
@author heatdesert
@date 2024-01-17 22:11
@description
 */

enum class SearchPageType {
    page,
    content
}

private val searchScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
private var isSearching = mutableStateOf(false)

@Composable
fun SearchScene(onClickBack: () -> Unit) {
    var currentPage by remember { mutableStateOf(SearchPageType.page) }
    var keyword by remember { mutableStateOf("") }
    if (currentPage == SearchPageType.page) {
        SearchPage(onClickBack = { onClickBack() }, onSearch = { s ->
            keyword = s
            currentPage = SearchPageType.content
        })
    } else {
        SearchResult(keyword = keyword, onClickBack = { onClickBack() }, searching = isSearching.value)
    }
}


/**
 *历史记录
 *热门推荐
 */
@Composable
fun SearchPage(onClickBack: () -> Unit, onSearch: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect("focus") {
        focusRequester.requestFocus()
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        Column(modifier = Modifier.align(alignment = Alignment.TopStart)) {
            // TopBar
            Row(modifier = Modifier.align(Alignment.Start).background(MaterialTheme.colors.background).height(80.dp)) {
                IconButton(
                    modifier = Modifier.align(Alignment.CenterVertically)
                        .padding(start = 20.dp, end = 20.dp),
                    onClick = { onClickBack() }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = "back to video home",
                        tint = MaterialTheme.colors.onBackground
                    )
                }
                searchBar(Modifier.align(Alignment.CenterVertically), focusRequester, "", onSearch, false)
            }
        }
    }
}

@Composable
private fun searchBar(
    modifier: Modifier,
    focusRequester: FocusRequester?,
    initValue: String,
    onSearch: (String) -> Unit,
    isSearching: Boolean
) {

    var searchText by remember { mutableStateOf(initValue) }
    TextField(
        modifier = modifier
            .focusRequester(focusRequester ?: FocusRequester())
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.6f)
            .background(color = Color.Gray.copy(0.3f), shape = RoundedCornerShape(50)),
        onValueChange = { i -> searchText = i },
        shape = RoundedCornerShape(50),
        placeholder = {
            Text(
                "请输入",
                modifier = Modifier.fillMaxSize(),
                fontSize = TextUnit(12f, TextUnitType.Sp)
            )
        },
        value = searchText,
        leadingIcon = {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxHeight(),
                    color = MaterialTheme.colors.secondary,
                    backgroundColor = MaterialTheme.colors.secondaryVariant
                )
            }
        },
        trailingIcon = {
            Icon(
                Icons.Outlined.Search, contentDescription = "Search", modifier = Modifier
                    .clickable(
                        interactionSource = MutableInteractionSource(),
                        indication = rememberRipple(
                            bounded = true,
                            radius = 8.dp,
                            color = Color.LightGray.copy(0.5f)
                        ),
                        onClick = { onSearch(searchText) }, enabled = StringUtils.isNotBlank(searchText)
                    )
            )
        },
        textStyle = TextStyle(fontSize = TextUnit(12f, TextUnitType.Sp)),
        maxLines = 1,
        keyboardActions = KeyboardActions(onDone = { onSearch(searchText) }),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
        colors = TextFieldDefaults.textFieldColors(
            focusedIndicatorColor = Color.Transparent, // 焦点时下划线颜色
            unfocusedIndicatorColor = Color.Transparent,
            backgroundColor = Color.Gray.copy(alpha = 0.3f),
            textColor = MaterialTheme.colors.onBackground
        )
    )
}

//@Composable
//@Preview
//fun previewSearchPage(){
//    AppTheme(useDarkTheme = true) {
//        SearchPage {}
//    }
//}

private var jobList = mutableListOf<Job>()

@Composable
private fun SearchResult(keyword: String, onClickBack: () -> Unit, searching: Boolean) {
    var searchText by remember { mutableStateOf(keyword) }
    val result = remember { mutableStateOf(SiteViewModel.search) }
    var currentCollect by remember { mutableStateOf<Collect?>(SiteViewModel.search[0]) }
    var currentVodList by remember { mutableStateOf(SiteViewModel.search[0].getList()) }
    val state = rememberLazyGridState()
    val adapter = rememberScrollbarAdapter(state)

    var showDetailDialog by remember { mutableStateOf(false) }
    var chooseVod by remember { mutableStateOf<Vod?>(null) }

    DisposableEffect(searchText) {
        isSearching.value = true
        cancleAndClearJobList()
        SiteViewModel.clearSearch()
        SiteViewModel.viewModelScope.launch {
            val searchableSites = api?.sites?.filter { it.searchable == 1 }
            searchableSites?.forEach {
                val job = searchScope.launch {
                    SiteViewModel.searchContent(it, searchText, false)
                }
                job.invokeOnCompletion {
                    currentVodList =
                        SiteViewModel.search.find { it.isActivated().value }?.getList()?.toList()?.toMutableList()
                }
                jobList.add(job)
            }
            searchScope.coroutineContext[Job]?.children?.forEach { it.join() }
            isSearching.value = false
        }

        onDispose {
            SiteViewModel.clearSearch()
            cancleAndClearJobList()
            isSearching.value = false
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
//        TopBar
            Row(
                modifier = Modifier.align(Alignment.Start)
                    .background(MaterialTheme.colors.background)
                    .height(75.dp)
            ) {
                IconButton(
                    modifier = Modifier.align(Alignment.CenterVertically)
                        .padding(start = 20.dp, end = 20.dp),
                    onClick = { onClickBack() }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = "back to video home",
                        tint = MaterialTheme.colors.onBackground
                    )
                }
                searchBar(
                    Modifier.align(Alignment.CenterVertically),
                    FocusRequester(),
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

private fun cancleAndClearJobList() {
    jobList.forEach { i -> i.cancel("search") }
    jobList.clear()
}

private fun searchSites() {

}