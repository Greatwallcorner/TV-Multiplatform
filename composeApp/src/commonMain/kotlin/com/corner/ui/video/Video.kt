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
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.zIndex
import com.corner.catvod.enum.bean.Site
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.bean.Type
import com.corner.catvodcore.config.ApiConfig
import com.corner.catvodcore.enum.ConfigType
import com.corner.catvodcore.enum.Menu
import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.database.Db
import com.corner.ui.nav.vm.VideoViewModel
import com.corner.ui.scene.*
import com.corner.util.isScrollingUp
import com.seiko.imageloader.ui.AutoSizeImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import tv_multiplatform.composeapp.generated.resources.Res
import tv_multiplatform.composeapp.generated.resources.empty
import tv_multiplatform.composeapp.generated.resources.folder_back

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
                    painter = painterResource(Res.drawable.folder_back),
                    contentDescription = "This is a folder ${vod.vodName}",
                    contentScale = ContentScale.Fit
                )
            } else {
                AutoSizeImage(
                    url = vod.vodPic ?: "",
                    modifier = picModifier,
                    contentDescription = vod.vodName,
                    contentScale = ContentScale.Crop,
                    placeholderPainter = { painterResource(Res.drawable.empty) },
                    errorPainter = { painterResource(Res.drawable.empty) })
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
fun WindowScope.VideoScene(
    vm: VideoViewModel,
    modifier: Modifier,
    onClickItem: (Vod) -> Unit,
    onClickSwitch: (Menu) -> Unit
) {
    val scope = rememberCoroutineScope()
    val state = rememberLazyGridState()
    val model = vm.state.collectAsState()
    val result = derivedStateOf { model.value.homeVodResult }
    val list = derivedStateOf { result.value.toTypedArray() }

    LaunchedEffect(state) {
        snapshotFlow { state.layoutInfo }
            .collect { layoutInfo ->
                val visibleItemsInfo = layoutInfo.visibleItemsInfo
                if (visibleItemsInfo.isNotEmpty()) {
                    val lastVisibleItem = visibleItemsInfo.last()
                    val isEnd = lastVisibleItem.index == layoutInfo.totalItemsCount - 1
                    if (isEnd && (model.value.currentClass?.failTime ?: 0) < 2) {
                        vm.loadMore()
                    }
                }
            }
    }

    var showChooseHome by remember { mutableStateOf(false) }
    var showFiltersDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            WindowDraggableArea {
                VideoTopBar(
                    vm = vm,
                    onClickSearch = { onClickSwitch(Menu.SEARCH) },
                    onClickChooseHome = { showChooseHome = true },
                    onClickSetting = { onClickSwitch(Menu.SETTING) },
                    onClickHistory = { onClickSwitch(Menu.HISTORY) })
            }
        },
        floatingActionButton = {
            FloatButton(vm, state, scope, showFiltersDialog) {
                showFiltersDialog = !showFiltersDialog
            }
        }
    ) {
        Box(modifier = modifier.fillMaxSize().padding(it)) {
            Column {
                val classIsEmpty = derivedStateOf { model.value.classList.isNotEmpty() }
                if (classIsEmpty.value) {
                    ClassRow(vm) {
                        vm.setClassData()
                        scope.launch {
                            state.animateScrollToItem(0)
                        }
                    }
                }
                val listEmpty = derivedStateOf { model.value.homeVodResult.isEmpty() }
                if (listEmpty.value) {
                    emptyShow(onRefresh = { vm.homeLoad() })
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
                                VideoItem(Modifier.animateItem(), item, false) {
                                    if (item.isFolder()) {
                                        vm.clickFolder(it)
                                    } else {
                                        onClickItem(it)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            ChooseHomeDialog(vm, showChooseHome, onClose = { showChooseHome = false }) {
                showChooseHome = false
                vm.clear()
                scope.launch {
                    state.animateScrollToItem(0)
                }
            }

            FiltersDialog(Modifier.align(Alignment.BottomCenter), showFiltersDialog, vm) {
                showFiltersDialog = false
            }
            val show = derivedStateOf {
                model.value.dirPaths.size > 1
            }
            AnimatedVisibility(show.value) {
                Box(Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.wrapContentHeight()
                            .wrapContentWidth()
                            .align(Alignment.BottomStart)
                            .shadow(8.dp, shape = RoundedCornerShape(10.dp))
                            .padding(1.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        LazyRow {
                            items(model.value.dirPaths) {
                                HoverableText(text = it) {
                                    SiteViewModel.viewModelScope.launch {
                                        vm.clickFolder(
                                            Vod(
                                                vodId = model.value.dirPaths.subList(
                                                    0,
                                                    model.value.dirPaths.indexOf(it) + 1
                                                ).joinToString("/")
                                            )
                                        )
                                    }
                                }
                                Text(text = "/")
                            }
                        }
                    }
                }
            }

//            DirPath(vm = vm)
        }
    }
}

//@OptIn(ExperimentalAnimationApi::class)
//@Composable
//fun DirPath(showDialog: Boolean = false, vm: VideoViewModel){
//    val state = vm.model.subscribeAsState()
//    val show = derivedStateOf {
//        state.value.dirPaths.size > 1
//    }
//    AnimatedVisibility(show.value){
//        Box(modifier = Modifier.height(80.dp)
//            .fillMaxHeight(0.3f)
//            .align(Alignment.BottomStart)
//            .shadow(8.dp, shape = RoundedCornerShape(10.dp)),
//            shape = RoundedCornerShape(10.dp)
//        ) {
//            LazyRow {
//                items(state.value.dirPaths){
//                    HoverableText(text = it){
//                        SiteViewModel.viewModelScope.launch {
//                            vm.clickFolder(Vod(vodId = state.value.dirPaths.subList(0, state.value.dirPaths.indexOf(it)).joinToString("/")))
//                        }
//                    }
//                }
//            }
//        }
//    }
//}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FiltersDialog(
    modifier: Modifier,
    showFiltersDialog: Boolean,
    vm: VideoViewModel,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val model = vm.state.collectAsState()
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
                    val f by rememberUpdatedState(filter)
                    LazyRow(
                        state = state, horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.onPointerEvent(PointerEventType.Scroll) {
                            scope.launch {
                                state.scrollBy(it.changes.first().scrollDelta.y * state.layoutInfo.visibleItemsInfo.first().size)
                            }
                        }) {
                        items(f.value ?: listOf()) {
                            RatioBtn(text = it.n ?: "", onClick = {
                                vm.chooseFilter(f, it)
//                                scope.launch {
//                                    f.init = it.v ?: ""
//                                    f.value?.filter { i -> i.n != it.n }?.map { t -> t.selected = false }
//                                    it.selected = true
//                                    vm.model.update { it.copy(currentFilters = model.value.currentFilters) }
//                                }
                                vm.chooseCate(it.v ?: "")
                            }, selected = it.selected, loading = false)
                        }
                    }
                }
            }
            VerticalScrollbar(
                rememberScrollbarAdapter(listState),
                modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 5.dp, horizontal = 8.dp),
                style = defaultScrollbarStyle().copy(
                    unhoverColor = Color.Gray.copy(0.45F),
                    hoverColor = Color.DarkGray
                )
            )
        }
    }

}

@Composable
fun FloatButton(
    vm: VideoViewModel,
    state: LazyGridState,
    scope: CoroutineScope,
    showFiltersDialog: Boolean,
    onClickFilter: () -> Unit
) {
    val show = derivedStateOf { GlobalAppState.chooseVod.value.isFolder() }
    val model = vm.state.collectAsState()
    val showButton = derivedStateOf { model.value.currentFilters.isNotEmpty() || state.firstVisibleItemIndex > 8 }
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
                AnimatedContent(
                    state.isScrollingUp(),
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

@Composable
fun VideoTopBar(
    vm: VideoViewModel,
    onClickSearch: () -> Unit,
    onClickChooseHome: () -> Unit,
    onClickSetting: () -> Unit,
    onClickHistory: () -> Unit
) {
    val home = GlobalAppState.home.collectAsState()
    val model = vm.state.collectAsState()

    ControlBar(
        title = {},
        modifier = Modifier.height(50.dp).padding(1.dp),
        leading = {
            ElevatedButton(
                modifier = Modifier.wrapContentWidth().padding(start = 5.dp),
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
        },
        center = {
            Box(
                modifier = Modifier
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
        },
        actions = {
            IconButton(onClick = {
                onClickHistory()
            }, modifier = Modifier.padding(end = 10.dp)) {
                Icon(Icons.Outlined.History, "history")
            }
            IconButton(onClick = {
                onClickSetting()
            }, modifier = Modifier.padding(end = 25.dp)) {
                Icon(Icons.Outlined.Settings, "settings")
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
fun ClassRow(vm: VideoViewModel, onCLick: (Type) -> Unit) {
    val model = vm.state.collectAsState()
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
                    vm.chooseClass(type){
                        onCLick(type)
                    }
                }, selected = type.selected)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChooseHomeDialog(
    vm: VideoViewModel,
    showDialog: Boolean,
    onClose: () -> Unit,
    onClick: (Site) -> Unit
) {
    val model = vm.state.collectAsState()
    val apiState = ApiConfig.apiFlow.collectAsState()
//    val siteList by remember { mutableStateOf(ApiConfig.api.sites.toList()) }
//    val sitesFlow = remember { MutableStateFlow(ApiConfig.api.sites.toList()) }
//    val sites by sitesFlow.collectAsState()
    val sites by derivedStateOf { apiState.value.sites.toList() }

    Dialog(
        Modifier
            .wrapContentWidth(Alignment.CenterHorizontally)
            .wrapContentHeight(Alignment.CenterVertically)
            .defaultMinSize(minWidth = 100.dp)
            .padding(20.dp), onClose = { onClose() }, showDialog = showDialog
    ) {
        Box {
            val lazyListState = rememberLazyListState(0)
            LazyColumn(
                modifier = Modifier.padding(20.dp).wrapContentHeight(Alignment.CenterVertically),
                state = lazyListState
            ) {
                items(items = sites) { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        OutlinedButton(
                            modifier = Modifier.width(180.dp),
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
                        TooltipArea(tooltip = {Text("开启/禁用搜索", Modifier.background(MaterialTheme.colorScheme.background))}, delayMillis = 1000){
                            IconButton(onClick = {
                                vm.changeSite {
                                    if (item.isSearchable()) {
                                        item.searchable = 0
                                    } else {
                                        item.searchable = 1
                                    }
                                    return@changeSite item
                                }
                            }) {
                                if (item.isSearchable()) {
                                    Icon(Icons.Default.Search, contentDescription = "enable search")
                                } else {
                                    Icon(Icons.Default.SearchOff, contentDescription = "disable search")
                                }
                            }
                        }
                        TooltipArea(tooltip = {Text("开启/禁用换源", Modifier.background(MaterialTheme.colorScheme.background))}, delayMillis = 1000){
                            IconButton(onClick = {
                                vm.changeSite {
                                    if (item.isChangeable()) {
                                        item.changeable = 0
                                    } else {
                                        item.changeable = 1
                                    }
                                    return@changeSite item
                                }
                            }) {
                                if (item.isChangeable()) {
                                    Icon(Icons.Default.Sync, contentDescription = "enable change")
                                } else {
                                    Icon(Icons.Default.SyncDisabled, contentDescription = "disable change")
                                }
                            }
                        }

                    }
                }
            }
            VerticalScrollbar(rememberScrollbarAdapter(lazyListState))
        }
    }
}