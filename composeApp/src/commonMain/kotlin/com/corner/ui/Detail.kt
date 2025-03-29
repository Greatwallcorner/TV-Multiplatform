package com.corner.ui

import AppTheme
import SiteViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import com.corner.bean.SettingStore
import com.corner.catvod.enum.bean.Vod
import com.corner.catvod.enum.bean.Vod.Companion.isEmpty
import com.corner.catvodcore.util.Utils
import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.catvodcore.viewmodel.GlobalAppState.hideProgress
import com.corner.catvodcore.viewmodel.GlobalAppState.showProgress
import com.corner.ui.nav.data.DetailScreenState
import com.corner.ui.nav.vm.DetailViewModel
import com.corner.ui.scene.*
import com.corner.ui.video.QuickSearchItem
import com.corner.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import org.jetbrains.compose.resources.painterResource
import tv_multiplatform.composeapp.generated.resources.Res
import tv_multiplatform.composeapp.generated.resources.TV_icon_x


@Composable
fun WindowScope.DetailScene(vm: DetailViewModel, onClickBack: () -> Unit) {
    val model = vm.state.collectAsState()
    val scope = rememberCoroutineScope()

    val detail by rememberUpdatedState(model.value.detail)

    val controller = rememberUpdatedState(vm.controller)

    val isFullScreen = GlobalAppState.videoFullScreen.collectAsState()

    val videoHeight = derivedStateOf { if (isFullScreen.value) 1f else 0.6f }
    val videoWidth = derivedStateOf { if (isFullScreen.value) 1f else 0.7f }


//    LaunchedEffect(Unit){
//    }

    DisposableEffect(Unit) {
        vm.load()
        onDispose {
            vm.clear()
        }

    }

    LaunchedEffect(model.value.isLoading) {
        if (model.value.isLoading) {
            showProgress()
        } else {
            hideProgress()
        }
    }

    val focus = remember { FocusRequester() }

    LaunchedEffect(isFullScreen.value) {
        focus.requestFocus()
    }
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier) {
            if (!isFullScreen.value) {
                WindowDraggableArea {
                    ControlBar(leading = {
                        BackRow(Modifier, onClickBack = {
                            onClickBack()
                        }) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.Start) {
                                    ToolTipText(
                                        detail.vodName ?: "",
                                        textStyle = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                                        modifier = Modifier.padding(start = 50.dp)
                                    )
                                }
                            }
                        }
                    }, actions = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    vm.clear()
                                    vm.quickSearch()
                                    SnackBar.postMsg("重新加载")
                                }
                            }, enabled = !model.value.isLoading
                        ) {
                            Icon(
                                Icons.Default.Autorenew,
                                contentDescription = "renew",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    })
                }
            }
            val mrl = derivedStateOf { model.value.currentPlayUrl }
            Row(
                modifier = Modifier.fillMaxHeight(videoHeight.value), horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val internalPlayer = derivedStateOf {
                    SettingStore.getPlayerSetting()[0] as Boolean
                }
                if (internalPlayer.value) {
                    SideEffect {
                        focus.requestFocus()
                    }
                    Player(
                        mrl.value,
                        controller.value,
                        Modifier.fillMaxWidth(videoWidth.value).focusable(),
                        vm,
                        focusRequester = focus
                    )
                } else {
                    Box(
                        Modifier.fillMaxWidth(videoWidth.value).fillMaxHeight().background(Color.Black)
                    ) {
                        Column(
                            Modifier.fillMaxSize().align(Alignment.Center),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                painter = painterResource(Res.drawable.TV_icon_x),
                                contentDescription = "nothing here",
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                "使用外部播放器",
                                modifier = Modifier.align(Alignment.CenterHorizontally).focusRequester(focus),
                                fontWeight = FontWeight.Bold,
                                fontSize = TextUnit(23f, TextUnitType.Sp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
                AnimatedVisibility(!isFullScreen.value, modifier = Modifier.fillMaxSize()) {
                    EpChooser(
                        vm, Modifier.fillMaxSize().background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(4.dp)
                        ).padding(horizontal = 5.dp)
                    )
                }
            }
            AnimatedVisibility(!isFullScreen.value) {
                val searchResultList = derivedStateOf { model.value.quickSearchResult.toList() }
                Box(Modifier) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.size(15.dp))
                        Column(Modifier.fillMaxWidth(0.3f)) {
                            quickSearchResult(model, searchResultList, vm)
                        }
                        Column(
                            modifier = Modifier.padding(start = 10.dp).fillMaxSize()
                        ) {
                            if (model.value.detail.isEmpty()) {
                                emptyShow(onRefresh = { vm.load() })
                            } else {
                                VodInfo(detail)
                            }
                            Spacer(modifier = Modifier.size(15.dp))
                            // 线路
                            Flags(scope, vm)
                            Spacer(Modifier.size(15.dp))
                            val urls = rememberUpdatedState(vm.state.value.currentUrl)
                            val showUrl = derivedStateOf { (urls.value?.values?.size ?: 0) > 1 }
                            if (showUrl.value) {
                                Row {
                                    Text(
                                        "清晰度",
                                        fontSize = TextUnit(25F, TextUnitType.Sp),
                                        modifier = Modifier.padding(bottom = 5.dp, end = 5.dp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        itemsIndexed(urls.value?.values ?: listOf()) { i, item ->
                                            RatioBtn(text = item.n ?: (i + 1).toString(), onClick = {
                                                vm.chooseLevel(urls.value?.copy(position = i), item.v)
                                            }, selected = i == urls.value?.position!!)
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
        val showEpChooserDialog = derivedStateOf { isFullScreen.value && model.value.showEpChooserDialog }
        Dialog(
            Modifier.align(Alignment.CenterEnd).fillMaxWidth(0.3f).fillMaxHeight(0.8f).padding(end = 20.dp),
            showDialog = showEpChooserDialog.value,
            onClose = { vm.showEpChooser() }) {
            EpChooser(
                vm, Modifier.fillMaxSize().background(
                    MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(8.dp)
                ).padding(horizontal = 5.dp)
            )
        }

    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Flags(
    scope: CoroutineScope, vm: DetailViewModel
) {
    val model = vm.state.collectAsState()
    val detail = derivedStateOf { model.value.detail }
    LaunchedEffect(detail.value) {
        println("detail detail 修改")
    }
    Row(Modifier.padding(start = 10.dp)) {
        Text(
            "线路",
            fontSize = TextUnit(25F, TextUnitType.Sp),
            modifier = Modifier.padding(bottom = 5.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.size(10.dp))
        val detailIsNotEmpty = derivedStateOf { detail.value.vodFlags.isNotEmpty() }
        if (detailIsNotEmpty.value) {
            val state = rememberLazyListState(0)
            Box() {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    state = state,
                    modifier = Modifier.padding(bottom = 10.dp).fillMaxWidth().onPointerEvent(PointerEventType.Scroll) {
                        scope.launch {
                            state.scrollBy(it.changes.first().scrollDelta.y * state.layoutInfo.visibleItemsInfo.first().size)
                        }
                    },
                ) {
                    val flagList = derivedStateOf { detail.value.vodFlags.toList() }
                    items(flagList.value) {
                        RatioBtn(text = it.show ?: "", onClick = {
                            vm.chooseFlag(detail.value, it)
                        }, selected = it.activated)
                    }
                }
                val showScrollBar =
                    derivedStateOf { state.layoutInfo.visibleItemsInfo.size < (detail.value.vodFlags.size) }
                if (showScrollBar.value) {
                    HorizontalScrollbar(
                        rememberScrollbarAdapter(state), style = defaultScrollbarStyle().copy(
                            unhoverColor = Color.Gray.copy(0.45F), hoverColor = Color.DarkGray
                        ), modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun quickSearchResult(
    model: State<DetailScreenState>, searchResultList: State<List<Vod>>, component: DetailViewModel
) {
    if (model.value.quickSearchResult.isNotEmpty()) {
        val quickState = rememberLazyGridState()
        val adapter = rememberScrollbarAdapter(quickState)
        Box {
            LazyVerticalGrid(
                modifier = Modifier.padding(end = 10.dp),
                columns = GridCells.Fixed(2),
                state = quickState,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(searchResultList.value) {
                    QuickSearchItem(it) {
                        SiteViewModel.viewModelScope.launch {
                            component.loadDetail(it)
                        }
                    }
                }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd), adapter = adapter, style = defaultScrollbarStyle().copy(
                    unhoverColor = Color.Gray.copy(0.45F), hoverColor = Color.DarkGray
                )
            )
        }
    }
}

@Composable
private fun VodInfo(detail: Vod?) {
    Column(Modifier.padding(10.dp)) {
        Row() {
            if (detail?.site?.name?.isNotBlank() == true) {
                Text(
                    "站源: " + detail.site?.name, color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(5.dp))
            }
            val s = mutableListOf<String>()
            Text(detail?.vodYear ?: "", color = MaterialTheme.colorScheme.onSurface)
            if (StringUtils.isNotBlank(detail?.vodArea)) {
                s.add(detail?.vodArea!!)
            }
            if (StringUtils.isNotBlank(detail?.cate)) {
                s.add(detail?.cate!!)
            }
            if (StringUtils.isNotBlank(detail?.typeName)) {
                s.add(detail?.typeName!!)
            }
            Text(
                s.joinToString(separator = " | "), color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            "导演：${detail?.vodDirector ?: "无"}", color = MaterialTheme.colorScheme.onSurface
        )
        ExpandedText(
            "演员：${detail?.vodActor ?: "无"}",
            2,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
        )
        ExpandedText(
            "简介：${detail?.vodContent?.trim() ?: "无"}",
            3,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpChooser(vm: DetailViewModel, modifier: Modifier) {
    val model = vm.state.collectAsState()
    val detail = rememberUpdatedState(model.value.detail)
    Column(modifier = modifier) {
        Row(Modifier.padding(vertical = 3.dp, horizontal = 8.dp)) {
            Text(
                "选集",
                fontSize = TextUnit(20F, TextUnitType.Sp),
                modifier = Modifier.padding(bottom = 5.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.size(10.dp))
            val show = derivedStateOf {
                (detail.value.currentFlag.episodes.size) > 0
            }
            if (show.value) {
                Text(
                    "共${detail.value.currentFlag.episodes.size}集",
                    textAlign = TextAlign.End,
                    fontSize = TextUnit(15F, TextUnitType.Sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        val epSize = derivedStateOf { detail.value.currentFlag.episodes.size }

        val scrollState = rememberLazyListState(0)
        val scrollBarAdapter = rememberScrollbarAdapter(scrollState)
        if (epSize.value > 15) {
            Box(modifier = Modifier.padding(bottom = 2.dp)) {
                LazyRow(
                    state = scrollState,
                    modifier = Modifier.padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    for (i in 0 until epSize.value step Constants.EpSize) {
                        item {
                            RatioBtn(
                                selected = detail.value.currentTabIndex == (i / Constants.EpSize),
                                onClick = {
                                    vm.chooseEpBatch(i)
                                },
                                text = "${i + 1}-${if ((i + Constants.EpSize) > epSize.value) epSize.value else i + Constants.EpSize}"
                            )
                        }
                    }
                }
                HorizontalScrollbar(
                    adapter = scrollBarAdapter,
                    modifier = Modifier.padding(bottom = 5.dp).align(Alignment.BottomCenter),
                    style = defaultScrollbarStyle().copy(
                        unhoverColor = Color.Gray.copy(0.45F), hoverColor = Color.DarkGray
                    )
                )
            }
        }

        val uriHandler = LocalUriHandler.current
        val epList = derivedStateOf { detail.value.subEpisode }
        val state = rememberLazyGridState()
        Box {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                state = state,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(epList.value, key = { it.url + it.number }) {
                    TooltipArea(
                        tooltip = {
                            // composable tooltip content
                            Surface(
                                modifier = Modifier.shadow(4.dp), shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = it.name,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                        }, delayMillis = 600
                    ) {
                        RatioBtn(
                            text = it.name, onClick = {
                                vm.chooseEp(it) {
                                    uriHandler.openUri(it)
                                }
                            }, selected = it.activated,
                            loading = it.activated && vm.videoLoading.value,
                            tag = {
                                if (Utils.isDownloadLink(it.url)) {
                                    true to "下载"
                                } else {
                                    false to ""
                                }
                            })
                    }
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(state),
                modifier = Modifier.align(Alignment.CenterEnd),
                style = defaultScrollbarStyle().copy(
                    unhoverColor = Color.Gray.copy(0.45F), hoverColor = Color.DarkGray
                )
            )
        }
    }
}

@androidx.compose.desktop.ui.tooling.preview.Preview
@Composable
fun previewEmptyShow() {
    AppTheme {
        emptyShow(onRefresh = { println("ddd") })
    }
}
