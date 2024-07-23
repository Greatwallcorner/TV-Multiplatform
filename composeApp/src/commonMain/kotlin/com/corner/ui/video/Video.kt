package com.corner.ui.video

import SiteViewModel
import androidx.compose.animation.*
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.arkivanov.decompose.value.update
import com.corner.catvod.enum.bean.Site
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.bean.Type
import com.corner.catvodcore.config.ApiConfig
import com.corner.catvodcore.enum.ConfigType
import com.corner.catvodcore.enum.Menu
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.database.Db
import com.corner.ui.decompose.component.DefaultVideoComponent
import com.corner.ui.scene.*
import com.corner.util.isScrollingUp
import com.seiko.imageloader.ui.AutoSizeImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun VideoItem(modifier: Modifier, vod: Vod, showSite: Boolean, click: (Vod) -> Unit) {
    Card(
        modifier = modifier
            .clickable(enabled = true, onClick = { click(vod) }),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        val picModifier = remember { Modifier.height(220.dp).width(200.dp) }
        Box(modifier = modifier) {
            if (vod.isFolder()) {
                Image(
                    modifier = modifier,
                    painter = painterResource("/pic/folder-back.png"),
                    contentDescription = "This is a folder ${vod.vodName}",
                    contentScale = ContentScale.Fit
                )
            } else {
                AutoSizeImage(url = vod.vodPic ?: "",
                    modifier = picModifier,
                    contentDescription = vod.vodName,
                    contentScale = ContentScale.Crop,
                    placeholderPainter = { painterResource("/pic/empty.png") },
                    errorPainter = { painterResource("/pic/empty.png") })
            }
            Box(Modifier.align(Alignment.BottomCenter)) {
                ToolTipText(
                    text = vod.vodName!!,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                        .fillMaxWidth().padding(0.dp, 10.dp)
                )
            }
            // 左上角
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .zIndex(999f)
                    .padding(5.dp),
                text = if (showSite) vod.site?.name ?: "" else vod.vodRemarks ?: "",
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    color = Color.White,
                    shadow = Shadow(Color.Black, offset = Offset(2F, 2F), blurRadius = 1.5F)
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoScene(
    component: DefaultVideoComponent,
    modifier: Modifier,
    onClickItem: (Vod) -> Unit,
    onClickSwitch: (Menu) -> Unit
) {
    val scope = rememberCoroutineScope()
    val state = rememberLazyGridState()
    val model = component.model.subscribeAsState()
    val result = derivedStateOf { model.value.homeVodResult }
    val list = derivedStateOf { result.value.toTypedArray() }

//    LaunchedEffect(list.value) {
//        println("list 修改")
//    }

    LaunchedEffect(state) {
        snapshotFlow { state.layoutInfo }
            .collect { layoutInfo ->
                val visibleItemsInfo = layoutInfo.visibleItemsInfo
                if (visibleItemsInfo.isNotEmpty()) {
                    val lastVisibleItem = visibleItemsInfo.last()
                    val isEnd = lastVisibleItem.index == layoutInfo.totalItemsCount - 1
                    if (isEnd && (model.value.currentClass?.failTime ?: 0) < 2) {
                        component.loadMore()
                    }
                }
            }
    }

    var showChooseHome by remember { mutableStateOf(false) }
    var showFiltersDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            VideoTopBar(
                component = component,
                onClickSearch = { onClickSwitch(Menu.SEARCH) },
                onClickChooseHome = { showChooseHome = true },
                onClickSetting = { onClickSwitch(Menu.SETTING) },
                onClickHistory = { onClickSwitch(Menu.HISTORY) })
        },
        floatingActionButton = {
            FloatButton(component, state, scope, showFiltersDialog) {
                showFiltersDialog = !showFiltersDialog
            }
        }
    ) {
        Box(modifier = modifier.fillMaxSize().padding(it)) {
            Column {
                val classIsEmpty = derivedStateOf { model.value.classList.isNotEmpty() }
                if (classIsEmpty.value) {
                    ClassRow(component) {
                        component.model.update { it.copy(homeVodResult = SiteViewModel.result.value.list.toMutableSet()) }
                        model.value.page.set(1)
                        scope.launch {
                            state.animateScrollToItem(0)
                        }
                    }
                }
                val listEmpty = derivedStateOf { model.value.homeVodResult.isEmpty() }
                if (listEmpty.value) {
                    emptyShow(onRefresh = { component.homeLoad() })
                } else {
                    Box {
                        LazyVerticalGrid(
                            modifier = modifier.padding(15.dp),
                            columns = GridCells.Adaptive(140.dp),
                            contentPadding = PaddingValues(5.dp),
                            state = state,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            itemsIndexed(list.value, key = { i, item -> item.vodId + item.vodName + i }) { _, item ->
                                VideoItem(Modifier.animateItemPlacement(), item, false) {
                                    if (item.isFolder()) {
                                        component.clickFolder(it)
                                    } else {
                                        onClickItem(it)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            ChooseHomeDialog(component, showChooseHome, onClose = { showChooseHome = false }) {
                showChooseHome = false
                component.clear()
                scope.launch {
                    state.animateScrollToItem(0)
                }
            }

            FiltersDialog(Modifier.align(Alignment.BottomCenter), showFiltersDialog, component) {
                showFiltersDialog = false
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FiltersDialog(
    modifier: Modifier,
    showFiltersDialog: Boolean,
    component: DefaultVideoComponent,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val model = component.model.subscribeAsState()
    Dialog(
        modifier
            .fillMaxWidth(0.7f)
            .fillMaxHeight(0.3f)
            .defaultMinSize(minWidth = 100.dp)
            .padding(20.dp), onClose = { onClose() }, showDialog = showFiltersDialog,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box {
            val listState = rememberLazyListState(0)
            LazyColumn(
                state = listState,
                modifier = Modifier.align(Alignment.Center),
                contentPadding = PaddingValues(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(model.value.currentFilters) { filter ->
                    val state = rememberLazyListState(0)
                    val f = rememberUpdatedState(filter)
                    LazyRow(state = state , horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.onPointerEvent(PointerEventType.Scroll) {
                        scope.launch {
                            state.scrollBy(it.changes.first().scrollDelta.y * state.layoutInfo.visibleItemsInfo.first().size)
                        }
                    }) {
                        items(f.value.value ?: listOf()) {
                            RatioBtn(it.n ?: "", onClick = {
                                scope.launch {
                                    f.value.init = it.v ?: ""
                                    f.value.value?.filter { i -> i.n != it.n  }?.map { t -> t.selected = false }
                                    it.selected = true
                                    component.model.update { it.copy(currentFilters = model.value.currentFilters) }
                                }
                                component.chooseCate(it.v ?: "")
                            }, selected = it.selected, loading = false)
                        }
                    }
                }
            }
            VerticalScrollbar(rememberScrollbarAdapter(listState),
                modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 5.dp, horizontal = 8.dp),
                style = defaultScrollbarStyle().copy(
                    unhoverColor = Color.Gray.copy(0.45F),
                    hoverColor = Color.DarkGray
                ))
        }
    }

}

@Composable
fun FloatButton(
    component: DefaultVideoComponent,
    state: LazyGridState,
    scope: CoroutineScope,
    showFiltersDialog: Boolean,
    onClickFilter: () -> Unit
) {
    val show = derivedStateOf { GlobalModel.chooseVod.value.isFolder() }
    val model = component.model.subscribeAsState()
    val showButton = derivedStateOf { !model.value.currentFilters.isEmpty() || state.firstVisibleItemIndex > 8 }
    AnimatedVisibility(
        showButton.value,
        enter = slideInVertically(
            initialOffsetY = { it },
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
        ),
    ) {
        Box(
            Modifier.fillMaxHeight(0.8f)
                .fillMaxWidth(0.2f)
                .padding(10.dp)
        ) {
            Box(Modifier.align(Alignment.BottomEnd)) {
                AnimatedContent(state.isScrollingUp(),
                    contentAlignment = Alignment.BottomEnd,
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) {
                    val modifier = Modifier.size(70.dp).shadow(5.dp)
                    val shape = RoundedCornerShape(8.dp)
                    val buttonColors = ButtonDefaults.buttonColors().copy(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (it && model.value.currentFilters.isNotEmpty()) {
                        ElevatedButton(
                            onClick = {
                                onClickFilter()
                            },
                            modifier = modifier,
                            colors = buttonColors,
                            shape = shape,
                            contentPadding = PaddingValues(8.dp)
                        )
                        {
                            Icon(
                                if (showFiltersDialog) Icons.Outlined.Close else Icons.Outlined.FilterAlt,
                                "show filter dialog",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    } else {
                        ElevatedButton(
                            onClick = { scope.launch { state.animateScrollToItem(0) } },
                            modifier = modifier,
                            colors = buttonColors,
                            shape = shape, contentPadding = PaddingValues(8.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Back to top",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTopBar(
    component: DefaultVideoComponent,
    onClickSearch: () -> Unit,
    onClickChooseHome: () -> Unit,
    onClickSetting: () -> Unit,
    onClickHistory: () -> Unit
) {
    val home = GlobalModel.home.subscribeAsState()
    val model = component.model.subscribeAsState()

    TopAppBar(modifier = Modifier.height(50.dp).padding(1.dp), title = {}, actions = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ElevatedButton(
                    modifier = Modifier.align(Alignment.Top).wrapContentWidth().padding(start = 5.dp),
                    onClick = { onClickChooseHome() },
                    colors = ButtonDefaults.elevatedButtonColors().copy(
                        containerColor = MaterialTheme.colorScheme.background,
                        disabledContentColor = MaterialTheme.colorScheme.background
                    ),
                    elevation = ButtonDefaults.buttonElevation(),
                ) {
                    Row(Modifier.wrapContentWidth()) {
                        Icon(
                            Icons.Outlined.ArrowDropDown,
                            contentDescription = "Choose Home",
                            modifier = Modifier.padding(end = 3.dp)
                        )
                        Text(
                            home.value.name,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            fontSize = TextUnit(15f, TextUnitType.Sp)
                        )
                    }
                }

                Box(modifier = Modifier.align(Alignment.CenterVertically)
                    .fillMaxWidth(0.3f)
                    .fillMaxHeight(0.6f)
                    .background(Color.Gray.copy(alpha = 0.3f), shape = RoundedCornerShape(percent = 50))
                    .clickable {
                        onClickSearch()
                    }) {
                    AnimatedContent(
                        targetState = model.value.prompt,
                        contentAlignment = Alignment.Center,
                        transitionSpec = {
                            slideInVertically { height -> height } + fadeIn() togetherWith
                                    slideOutVertically { height -> -height } + fadeOut()
                        },
                        modifier = Modifier.fillMaxHeight()/*.padding(top = 4.dp)*/
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.align(Alignment.Center)
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            textAlign = TextAlign.Center
                        )
                    }
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = "搜索",
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 15.dp)
                    )
                }
                Row(modifier = Modifier.align(Alignment.Bottom)) {
                    IconButton(onClick = {
                        onClickHistory()
                    }, modifier = Modifier.padding(end = 20.dp)) {
                        Icon(Icons.Outlined.History, "history")
                    }
                    IconButton(onClick = {
                        onClickSetting()
                    }, modifier = Modifier) {
                        Icon(Icons.Outlined.Settings, "settings")
                    }
                }
            }
        }
    })
}

@Composable
@Preview
fun previewImageItem() {
    MaterialTheme {
        val vod = Vod()
        vod.vodId = "/index.php/voddetail/82667.html"
        vod.vodName = "Test"
        vod.vodPic = "https://pic1.yzzyimg.com/upload/vod/2024-01-09/17047994131.jpg"
        vod.vodRemarks = "更新至第10集"
        VideoItem(Modifier, vod, true, {})
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ClassRow(component: DefaultVideoComponent, onCLick: (Type) -> Unit) {
    val model = component.model.subscribeAsState()
    val state = rememberLazyListState(0)
    val scope = rememberCoroutineScope()
    val visible = derivedStateOf { state.layoutInfo.visibleItemsInfo.size < model.value.classList.size }
    Box(modifier = Modifier) {
        LazyRow(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .onPointerEvent(PointerEventType.Scroll) {
                    scope.launch {
                        state.scrollBy(it.changes.first().scrollDelta.y * state.layoutInfo.visibleItemsInfo.first().size)
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            contentPadding = PaddingValues(top = 5.dp, start = 5.dp, end = 5.dp, bottom = 5.dp),
            userScrollEnabled = true
        ) {
            val list = derivedStateOf { model.value.classList.toList() }
            items(list.value) { type ->
                RatioBtn(text = type.typeName, onClick = {
                    if (component.isLoading.get()) return@RatioBtn
                    component.isLoading.set(true)
                    SiteViewModel.viewModelScope.launch {
                        showProgress()
                        for (tp in model.value.classList) {
                            tp.selected = type.typeId == tp.typeId
                        }
                        component.model.update {
                            it.copy(
                                currentClass = type,
                                classList = model.value.classList,
                                currentFilters = component.getFilters(type)
                            )
                        }
                        SiteViewModel.cancelAll()
                        if (type.typeId == "home") {
                            SiteViewModel.homeContent()
                        } else {
                            model.value.page.set(1)
                            val result = SiteViewModel.categoryContent(
                                GlobalModel.home.value.key,
                                type.typeId,
                                model.value.page.get().toString(),
                                false,
                                HashMap()
                            )
                            if (!result.isSuccess) {
                                model.value.currentClass?.failTime?.plus(1)
                            }
                        }
                    }.invokeOnCompletion {
                        onCLick(type)
                        component.isLoading.set(false)
                        hideProgress()
                    }
                }, type.selected)
            }
        }
        if (visible.value) {
            HorizontalScrollbar(
                rememberScrollbarAdapter(state), modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(top = 10.dp)
            )
        }
    }
}

@Composable
@Preview
fun previewClassRow() {
//    AppTheme {
//        val list = listOf(Type("1", "ABC"), Type("2", "CDR"), Type("3", "ddr"))
////        ClassRow(list.toMutableSet()) {}
//    }
}

@Composable
fun ChooseHomeDialog(
    component: DefaultVideoComponent,
    showDialog: Boolean,
    onClose: () -> Unit,
    onClick: (Site) -> Unit
) {
    val model = component.model.subscribeAsState()
    Dialog(
        Modifier
            .wrapContentWidth(Alignment.CenterHorizontally)
            .wrapContentHeight(Alignment.CenterVertically)
            .defaultMinSize(minWidth = 100.dp)
            .padding(20.dp), onClose = { onClose() }, showDialog = showDialog
    ) {
        Box() {
            val lazyListState = rememberLazyListState(0)
            LazyColumn(
                modifier = Modifier.padding(20.dp).wrapContentHeight(Alignment.CenterVertically),
                state = lazyListState
            ) {
                items(items = ApiConfig.api.sites.toList()) { item ->
                    OutlinedButton(modifier = Modifier.width(180.dp),
                        onClick = {
                            SiteViewModel.viewModelScope.launch {
                                ApiConfig.setHome(item)
                                model.value.homeLoaded = false
                                Db.Config.setHome(ApiConfig.api.url, ConfigType.SITE.ordinal, item.key)
                            }
                            onClick(item)
                        }) {
                        Text(item.name, textAlign = TextAlign.Center)
                    }
                }
            }
            VerticalScrollbar(rememberScrollbarAdapter(lazyListState))
        }
    }
}