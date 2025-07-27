package com.corner.ui.video

import SiteViewModel
import androidx.compose.animation.*
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
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.corner.init.Init
import com.corner.init.Init.Companion.initConfig
import com.corner.ui.nav.vm.VideoViewModel
import com.corner.ui.scene.*
import com.corner.util.isScrollingUp
import com.seiko.imageloader.ui.AutoSizeImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tv_multiplatform.composeapp.generated.resources.Res
import tv_multiplatform.composeapp.generated.resources.folder_back
import tv_multiplatform.composeapp.generated.resources.loading
import tv_multiplatform.composeapp.generated.resources.undraw_loading

val log: Logger? = LoggerFactory.getLogger("VideoScreen")

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
                    placeholderPainter = { painterResource(Res.drawable.undraw_loading) },
                    errorPainter = { painterResource(Res.drawable.loading) }
                )
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
                val isInitialized by Init.isInitializedSuccessfully
                //加载站源成功后，加载站源中的数据的状态
                val isLoading by vm.isLoading
                //加载配置文件时，调用了showProgress，通过监听showProgress的值来决定显示加载图标
                val showProgress by GlobalAppState.showProgress.collectAsState()

                if (!isInitialized){
                    emptyShow(
                        onRefresh = { initConfig() },  // 点击重新初始化
                        title = "初始化失败",
                        subtitle = "请检查配置文件地址或重新加载配置",
                        isLoading = showProgress
                    )
                }
                if (listEmpty.value) {
                    emptyShow(
                        onRefresh = {
                            vm.homeLoad(forceRefresh = true)
                        },
                        isLoading = isLoading
                    )
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
//    val show = derivedStateOf { GlobalAppState.chooseVod.value.isFolder() }
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
    val isLoading by vm.isLoading
    val showProgress by GlobalAppState.showProgress.collectAsState()

    ControlBar(
        title = {},
        modifier = Modifier.height(80.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        leading = {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 8.dp), // 增加垂直padding
                contentAlignment = Alignment.Center
            ) {
                // 首页选择按钮
                FilledTonalButton(
                    onClick = { if (!isLoading) onClickChooseHome() }, // 仅在非加载状态响应点击
                    modifier = Modifier
                        .height(40.dp)
                        .alpha(if (isLoading) 0.5f else 1f), // 加载时半透明
                    enabled = !isLoading || showProgress, // 关键：禁用状态
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), // 禁用状态背景色
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)  // 禁用状态内容色
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 1.dp,
                        pressedElevation = 2.dp,
                        disabledElevation = 0.dp // 禁用时取消阴影
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = "Choose Home",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = home.value.name.ifEmpty { "暂无数据" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 5.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (isLoading) 0.5f else 0.8f // 根据状态调整透明度
                            ),
                        )
                        // 加载动画（仅在加载时显示）
                        if (isLoading || showProgress) {
                            Spacer(modifier = Modifier.width(4.dp)) // 添加间距
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        },
        center = {
            // 搜索框（带动态提示和微交互）
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(40.dp)
                    .shadow(1.dp, RoundedCornerShape(20.dp))
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onClickSearch() }
                    .animateContentSize()
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    AnimatedContent(
                        targetState = model.value.prompt,
                        transitionSpec = {
                            slideInVertically { it } + fadeIn() togetherWith
                                    slideOutVertically { -it } + fadeOut()
                        },
                        modifier = Modifier.weight(1f)
                    ) { prompt ->
                        Text(
                            text = prompt,
                            modifier = Modifier.padding(start = 16.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "搜索",
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        actions = {
            // 右侧操作按钮组
            Row(
                modifier = Modifier.padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 历史记录按钮
                IconButton(
                    onClick = { onClickHistory() },
                    modifier = Modifier
                        .size(36.dp)  // 适当减小整体尺寸
                        .padding(2.dp),  // 增加内边距替代背景
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = "历史记录",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 设置按钮
                IconButton(
                    onClick = { onClickSetting() },
                    modifier = Modifier
                        .size(36.dp)
                        .padding(2.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "设置",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    )
}

//@Composable
//@Preview
//fun previewImageItem() {
//    MaterialTheme {
//        val vod = Vod()
//        vod.vodId = "/index.php/voddetail/82667.html"
//        vod.vodName = "Test"
//        vod.vodPic = "https://pic1.yzzyimg.com/upload/vod/2024-01-09/17047994131.jpg"
//        vod.vodRemarks = "更新至第10集"
//        VideoItem(Modifier, vod, true, {})
//    }
//}

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
                    vm.chooseClass(type) {
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

//@Composable
//@Preview
//fun previewClassRow() {
//    AppTheme {
//        val list = listOf(Type("1", "ABC"), Type("2", "CDR"), Type("3", "ddr"))
//        ClassRow(list.toMutableSet()) {}
//    }
//}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChooseHomeDialog(
    vm: VideoViewModel,
    showDialog: Boolean,
    onClose: () -> Unit,
    onClick: (Site) -> Unit
) {
    var isClosing by remember { mutableStateOf(false) }
    val model = vm.state.collectAsState()
    val apiState = ApiConfig.apiFlow.collectAsState()
    val sites by derivedStateOf { apiState.value.sites.toList() }

    AnimatedVisibility(
        visible = showDialog && !isClosing,
        enter = EnterTransition.None, // 完全禁用进入动画
        exit = ExitTransition.None    // 完全禁用退出动画
    ) {
        Dialog(
            onDismissRequest = onClose,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false // 禁用平台默认动画
            ),
        ) {
            Surface(
                modifier = Modifier
                    .width(300.dp)
                    .heightIn(min = 200.dp, max = 500.dp),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.background
            ) {
                Column {
                    // 标题
                    Text(
                        text = "选择首页站点",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )

                    // 内容区域
                    Box {
                        val lazyListState = rememberLazyListState()
                        LazyColumn(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            state = lazyListState,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(items = sites) { item ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    tonalElevation = 1.dp,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 站点按钮
                                        OutlinedButton(
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                SiteViewModel.viewModelScope.launch {
                                                    ApiConfig.setHome(item)
                                                    model.value.homeLoaded = false
                                                    Db.Config.setHome(ApiConfig.api.url, ConfigType.SITE.ordinal, item.key)
                                                }
                                                onClick(item)
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth(),
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }

                                        // 搜索开关
                                        IconToggleButton(
                                            checked = item.isSearchable(),
                                            onCheckedChange = {
                                                vm.changeSite {
                                                    if (item.isSearchable()) {
                                                        item.searchable = 0
                                                    } else {
                                                        item.searchable = 1
                                                    }
                                                    return@changeSite item
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (item.isSearchable()) Icons.Default.Search else Icons.Default.SearchOff,
                                                contentDescription = if (item.isSearchable()) "禁用搜索" else "启用搜索",
                                                tint = if (item.isSearchable()) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // 换源开关
                                        IconToggleButton(
                                            checked = item.isChangeable(),
                                            onCheckedChange = {
                                                vm.changeSite {
                                                    if (item.isChangeable()) {
                                                        item.changeable = 0
                                                    } else {
                                                        item.changeable = 1
                                                    }
                                                    return@changeSite item
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (item.isChangeable()) Icons.Default.Sync else Icons.Default.SyncDisabled,
                                                contentDescription = if (item.isChangeable()) "禁用换源" else "启用换源",
                                                tint = if (item.isChangeable()) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 滚动条
                        VerticalScrollbar(
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 5.dp),
                            adapter = rememberScrollbarAdapter(lazyListState),
                            style = LocalScrollbarStyle.current.copy(
                                unhoverColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                hoverColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                thickness = 6.dp
                            )
                        )
                    }
                }
            }
        }
    }
}