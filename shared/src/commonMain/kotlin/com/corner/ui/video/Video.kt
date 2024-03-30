package com.corner.ui.video

import SiteViewModel
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.corner.catvod.enum.bean.Site
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.bean.Type
import com.corner.catvodcore.config.api
import com.corner.catvodcore.config.setHome
import com.corner.catvodcore.enum.ConfigType
import com.corner.catvodcore.enum.Menu
import com.corner.database.Db
import com.corner.ui.AppTheme
import com.corner.ui.scene.Dialog
import com.corner.ui.scene.RatioBtn
import com.corner.ui.scene.hideProgress
import com.corner.ui.scene.showProgress
import com.corner.util.Utils.addIfAbsent
import com.seiko.imageloader.ui.AutoSizeImage
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
@author heatdesert
@date 2023-12-18 23:03
@description
 */
private val log: Logger = LoggerFactory.getLogger("Video")

@OptIn(ExperimentalResourceApi::class)
@Composable
fun VideoItem(modifier: Modifier, vod: Vod, showSite: Boolean, click: (Vod) -> Unit) {
    Card(
        modifier = modifier
            .clickable(enabled = true, onClick = { click(vod) }),
        elevation = 8.dp,
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
                modifier = Modifier.background(Color.DarkGray.copy(alpha = 0.6F)).align(Alignment.BottomCenter)
                    .fillMaxWidth().padding(0.dp, 10.dp),
                color = darkColors().onBackground, maxLines = 1, softWrap = true,
                overflow = TextOverflow.Ellipsis, style = TextStyle(textAlign = TextAlign.Center)
            )
        }
        Text(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .width(10.dp)
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

private var homeVodResult by mutableStateOf<MutableSet<Vod>?>(null)
private var homeLoaded by mutableStateOf(false)
private var classList by mutableStateOf<MutableSet<Type>>(mutableSetOf())
private var currentClass by mutableStateOf<Type?>(null)
private var page = 1
var Home by mutableStateOf<Site?>(null)

@Composable
fun videoScene(
    modifier: Modifier,
    onClickSwitch: (Menu) -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = SnackbarHostState()
    val scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState)
    val state = rememberLazyGridState()
    val adapter = rememberScrollbarAdapter(state)

    LaunchedEffect("homeLoad") {
        homeLoad()
    }

    val canLoad = rememberUpdatedState(state.canScrollForward)
    DisposableEffect(canLoad.value) {
        if (!canLoad.value) {
            showProgress()
            page += 1
            SiteViewModel.viewModelScope.launch {
                try {
                    if (currentClass == null || currentClass?.typeId == "home") return@launch
                    SiteViewModel.categoryContent(
                        api?.home?.value?.key ?: "",
                        currentClass?.typeId,
                        page.toString(),
                        true,
                        HashMap()
                    )
                    val list = SiteViewModel.result.value.list
                    if (list.isNotEmpty()) {
                        val vodList = homeVodResult?.toMutableList()
                        vodList?.addAll(list)
                        homeVodResult = vodList?.toSet()?.toMutableSet()
                    }
                } finally {
                    hideProgress()
                }
            }
        }
        onDispose {
//            page = 1
        }
    }

    var showChooseHome by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) }

    var chooseVod by remember { mutableStateOf<Vod?>(null) }
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            VideoTopBar(
                onClickSearch = { onClickSwitch(Menu.SEARCH) },
                onClickChooseHome = { showChooseHome = true },
                onClickSetting = { onClickSwitch(Menu.SETTING) },
                onClickHistory = { onClickSwitch(Menu.HISTORY) })
        }
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            if (homeVodResult?.isEmpty() == true) {
                Image(
                    modifier = modifier.align(Alignment.Center),
                    painter = androidx.compose.ui.res.painterResource("nothing.png"),
                    contentDescription = "nothing here",
                    contentScale = ContentScale.Crop
                )
            } else {
                Column {
                    if (classList.isNotEmpty()) {
                        ClassRow(classList) {
                            page = 1
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
                        itemsIndexed(homeVodResult?.toList() ?: listOf()) { _, item ->
                            VideoItem(Modifier, item, false) {
                                chooseVod = it
                                showDetailDialog = true
                            }
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
            DetailDialog(showDetailDialog, chooseVod, api?.home?.value?.key ?: "") { showDetailDialog = false }
            ChooseHomeDialog(showChooseHome, onClose = { showChooseHome = false }) {
                scope.launch {
                    state.animateScrollToItem(0)
                }
            }
        }
    }
}

private fun homeLoad() {
    SiteViewModel.viewModelScope.launch {
        showProgress()
        try {
            if (!homeLoaded) {
                if (api?.home == null) return@launch
                homeVodResult = SiteViewModel.homeContent().list.toMutableSet()
                classList = SiteViewModel.result.value.types.toMutableSet()

                // 有首页内容
                if (homeVodResult?.isNotEmpty() == true) {
                    classList = (mutableSetOf(Type.home()) + classList) as MutableSet<Type>
                } else {
                    val types = SiteViewModel.result.value.types
                    if (types.isEmpty()) return@launch
                    SiteViewModel.categoryContent(api?.home?.value?.key ?: "", types.get(0).typeId, page.toString(), true, HashMap())
                    homeVodResult = SiteViewModel.result.value.list.toMutableSet()
                }
                if (classList.size > 0) {
                    currentClass = classList.first()
                }
                homeLoaded = true
            }
        } catch (e: Exception) {
            log.error("homeLoad", e)
        } finally {
//            page = 2
            hideProgress()
        }
    }
    page+=1
}

@Composable
fun VideoTopBar(
    onClickSearch: () -> Unit,
    onClickChooseHome: () -> Unit,
    onClickSetting: () -> Unit,
    onClickHistory: () -> Unit
) {
    TopAppBar(modifier = Modifier.height(50.dp), elevation = 5.dp, contentPadding = PaddingValues(1.dp)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            IconButton(modifier = Modifier,
                onClick = { onClickChooseHome() }) {
                Row {
                    Icon(
                        Icons.Outlined.ArrowDropDown,
                        contentDescription = "Choose Home",
                        modifier = Modifier.padding(end = 3.dp)
                    )
                    Text(Home?.name ?: "无")
                }
            }

            Box(modifier = Modifier.align(Alignment.Center)
                .fillMaxWidth(0.3f)
                .fillMaxHeight(0.6f)
                .background(Color.Gray.copy(alpha = 0.3f), shape = RoundedCornerShape(percent = 50))
                .clickable {
                    onClickSearch()
                }) {
                Text(
                    text = "请输入",
                    modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
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
    }
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
fun ClassRow(list: MutableSet<Type>, onCLick: () -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        contentPadding = PaddingValues(top = 5.dp, start = 5.dp, end = 5.dp),
        userScrollEnabled = true
    ) {
        items(list.toList()) {
            RatioBtn(text = it.typeName, onClick = {
                SiteViewModel.viewModelScope.launch {
                    showProgress()
                    try {
                        for (type in list) {
                            type.selected = it.typeId == type.typeId
                        }
                        currentClass = it
                        classList = classList.toSet().toMutableSet()
                        if (it.typeId == "home") {
                            SiteViewModel.homeContent()
                        } else {
                            SiteViewModel.categoryContent(api?.home?.value?.key ?: "", it.typeId, "1", true, HashMap())
                        }
                        homeVodResult = SiteViewModel.result.value.list.toMutableSet()
                    } finally {
                        hideProgress()
                        onCLick()
                    }
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
        ClassRow(list.toMutableSet(), {})
    }
}

@Composable
fun ChooseHomeDialog(showDialog: Boolean, onClose: () -> Unit, onClick: () -> Unit) {
    Dialog(Modifier
        .wrapContentWidth(Alignment.CenterHorizontally)
        .wrapContentHeight(Alignment.CenterVertically)
        .defaultMinSize(minWidth = 100.dp)
        .padding(20.dp), showDialog, onClose = { onClose() }) {
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
                            homeLoaded = false
                            homeLoad()
                            Db.Config.setHome(api?.url, ConfigType.SITE.ordinal, item.key)
                        }
                        onClose()
                        onClick()
                    }) {
                    Text(item.name, textAlign = TextAlign.Center)
                }
            }
        }
        VerticalScrollbar(ScrollbarAdapter(lazyListState))
    }
}