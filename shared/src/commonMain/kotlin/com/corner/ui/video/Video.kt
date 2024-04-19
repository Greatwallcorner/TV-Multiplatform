package com.corner.ui.video

import AppTheme
import SiteViewModel
import androidx.compose.animation.*
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.arkivanov.decompose.value.update
import com.corner.catvod.enum.bean.Site
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.bean.Type
import com.corner.catvodcore.config.api
import com.corner.catvodcore.config.setHome
import com.corner.catvodcore.enum.ConfigType
import com.corner.catvodcore.enum.Menu
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.database.Db
import com.corner.ui.decompose.VideoComponent
import com.corner.ui.decompose.component.DefaultVideoComponent
import com.corner.ui.scene.Dialog
import com.corner.ui.scene.RatioBtn
import com.corner.ui.scene.hideProgress
import com.corner.ui.scene.showProgress
import com.seiko.imageloader.ui.AutoSizeImage
import kotlinx.coroutines.launch

@Composable
fun VideoItem(modifier: Modifier, vod: Vod, showSite: Boolean, click: (Vod) -> Unit) {
    Card(
        modifier = modifier
            .clickable(enabled = true, onClick = { click(vod) }),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = modifier) {
            AutoSizeImage(url = vod.vodPic!!,
                modifier = Modifier.height(220.dp).width(200.dp),
                contentDescription = vod.vodName,
                contentScale = ContentScale.Crop,
                placeholderPainter = { painterResource("empty.png") },
                errorPainter = { painterResource("empty.png") })
            Text(
                text = vod.vodName!!,
                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth().padding(0.dp, 10.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1, softWrap = true,
                overflow = TextOverflow.Ellipsis, style = TextStyle(textAlign = TextAlign.Center)
            )
            // 左上角
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .zIndex(999f)
                    .padding(5.dp),
                text = if (showSite) vod.site?.name ?: "" else vod.vodRemarks!!,
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
fun VideoScene(
    component: DefaultVideoComponent,
    modifier: Modifier,
    onClickItem: (Vod) -> Unit,
    onClickSwitch: (Menu) -> Unit
) {
    val scope = rememberCoroutineScope()
    val state = rememberLazyGridState()
    val adapter = rememberScrollbarAdapter(state)
    val model = component.model.subscribeAsState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val last = state.layoutInfo.visibleItemsInfo.lastOrNull()
            last == null || last.index >= state.layoutInfo.totalItemsCount - 1
        }
    }

    DisposableEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            component.loadMore()
        }
        onDispose {
        }
    }

    var showChooseHome by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            VideoTopBar(
                component = component,
                onClickSearch = { onClickSwitch(Menu.SEARCH) },
                onClickChooseHome = { showChooseHome = true },
                onClickSetting = { onClickSwitch(Menu.SETTING) },
                onClickHistory = { onClickSwitch(Menu.HISTORY) })
        },
//        floatingActionButton = {
//            FloatButton(component)
//        }
    ) {
        Box(modifier = modifier.fillMaxSize().padding(it)) {
            if (model.value.homeVodResult?.isEmpty() == true) {
                Text("这里什么都没有...")
                Image(
                    modifier = modifier.align(Alignment.Center),
                    painter = painterResource("nothing.png"),
                    contentDescription = "nothing here",
                    contentScale = ContentScale.Crop
                )
            } else {
                Column {
                    if (model.value.classList.isNotEmpty()) {
                        ClassRow(model) {
                            component.model.update { it.copy(homeVodResult = SiteViewModel.result.value.list.toMutableSet()) }
                            model.value.page = 1
                            scope.launch {
                                state.animateScrollToItem(0)
                            }
                        }
                    }
                    LazyVerticalGrid(
                        modifier = modifier.padding(15.dp),
                        columns = GridCells.Adaptive(140.dp),
                        contentPadding = PaddingValues(5.dp),
                        state = state,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        userScrollEnabled = true
                    ) {
                        itemsIndexed(model.value.homeVodResult?.toList() ?: listOf()) { _, item ->
                            VideoItem(Modifier, item, false) {
                                if (item.isFolder()) {
                                    SiteViewModel.viewModelScope.launch {

                                    }
                                } else {
                                    onClickItem(it)
                                }
                            }
                        }
                    }
                }
            }
            VerticalScrollbar(
                adapter, modifier = Modifier.align(Alignment.CenterEnd)
                    .padding(end = 10.dp),
                style = defaultScrollbarStyle().copy(
                    unhoverColor = Color.DarkGray.copy(0.3F),
                    hoverColor = Color.DarkGray
                )
            )
            ChooseHomeDialog(component, showChooseHome, onClose = { showChooseHome = false }) {
                showChooseHome = false
                scope.launch {
                    state.animateScrollToItem(0)
                }
            }
        }
    }
}

@Composable
fun FloatButton(component: DefaultVideoComponent) {
    val show = remember { derivedStateOf { GlobalModel.chooseVod.value.isFolder() } }
    AnimatedVisibility(show.value,
        /*enter = slideInVertically(
        initialOffsetY = { fullHeight -> -fullHeight },
        animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing)
    ),
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> -fullHeight },
            animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing)
        )*/){
//        ElevatedButton()
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
            IconButton(modifier = Modifier.size(120.dp)
                .indication(
                    MutableInteractionSource(),
                    indication = rememberRipple(bounded = true, radius = 50.dp)
                ),
                onClick = { onClickChooseHome() }) {
                Row(Modifier.wrapContentWidth()) {
                    Icon(
                        Icons.Outlined.ArrowDropDown,
                        contentDescription = "Choose Home",
                        modifier = Modifier.padding(end = 3.dp)
                    )
                    Text(home.value.name, modifier = Modifier.wrapContentWidth())
                }
            }

            Box(modifier = Modifier.align(Alignment.Center)
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
            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = {
                    onClickHistory()
                }, modifier = Modifier.padding(end = 20.dp)) {
                    Icon(Icons.Outlined.History, "history")
                }
                IconButton(onClick = {
                    onClickSetting()
                }, modifier = Modifier.padding(end = 20.dp)) {
                    Icon(Icons.Outlined.Settings, "settings")
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

@Composable
fun ClassRow(model: State<VideoComponent.Model>, onCLick: (Type) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        contentPadding = PaddingValues(top = 5.dp, start = 5.dp, end = 5.dp),
        userScrollEnabled = true
    ) {
        items(model.value.classList.toList()) {
            RatioBtn(text = it.typeName, onClick = {
                SiteViewModel.viewModelScope.launch {
                    showProgress()
                    try {
                        for (type in model.value.classList) {
                            type.selected = it.typeId == type.typeId
                        }
                        model.value.currentClass = it
                        model.value.classList = model.value.classList.toSet().toMutableSet()
                        if (it.typeId == "home") {
                            SiteViewModel.homeContent()
                        } else {
                            SiteViewModel.categoryContent(
                                GlobalModel.home.value.key,
                                it.typeId,
                                "1",
                                true,
                                HashMap()
                            )
                        }
                    } finally {
                        hideProgress()
                    }
                    onCLick(it)
                }
            }, it.selected)
        }
    }
}

@Composable
@Preview
fun previewClassRow() {
    AppTheme {
        val list = listOf(Type("1", "ABC"), Type("2", "CDR"), Type("3", "ddr"))
//        ClassRow(list.toMutableSet()) {}
    }
}

@Composable
fun ChooseHomeDialog(
    component: DefaultVideoComponent,
    showDialog: Boolean,
    onClose: () -> Unit,
    onClick: (Site) -> Unit
) {
    val model = component.model.subscribeAsState()
    Box() {
        Dialog(
            Modifier
                .wrapContentWidth(Alignment.CenterHorizontally)
                .wrapContentHeight(Alignment.CenterVertically)
                .defaultMinSize(minWidth = 100.dp)
                .padding(20.dp), onClose = { onClose() }, showDialog = showDialog
        ) {
            val lazyListState = rememberLazyListState(0)
            LazyColumn(
                modifier = Modifier.padding(20.dp).wrapContentHeight(Alignment.CenterVertically),
                state = lazyListState
            ) {
                items(items = api?.sites?.toList() ?: listOf()) { item ->
                    OutlinedButton(modifier = Modifier.width(180.dp),
                        onClick = {
                            SiteViewModel.viewModelScope.launch {
                                setHome(item)
                                model.value.homeLoaded = false
                                Db.Config.setHome(api?.url, ConfigType.SITE.ordinal, item.key)
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