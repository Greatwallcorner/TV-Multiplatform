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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.arkivanov.decompose.value.update
import com.corner.bean.SettingStore
import com.corner.catvod.enum.bean.Vod
import com.corner.catvod.enum.bean.Vod.Companion.getPage
import com.corner.catvodcore.bean.v
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.decompose.DetailComponent
import com.corner.ui.player.vlcj.VlcjFrameController
import com.corner.ui.scene.*
import com.corner.ui.video.QuickSearchItem
import com.corner.util.Constants
import com.corner.util.play.Play
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils


@Composable
fun DetailScene(component: DetailComponent, onClickBack: () -> Unit) {
    val model = component.model.subscribeAsState()
    val scope = rememberCoroutineScope()

    val detail by rememberUpdatedState(model.value.detail)

    val controller = rememberUpdatedState(component.controller)

    val isFullScreen = GlobalModel.videoFullScreen.subscribeAsState()

    val videoHeight = derivedStateOf { if (isFullScreen.value) 1f else 0.6f }
    val videoWidth = derivedStateOf { if (isFullScreen.value) 1f else 0.7f }


    LaunchedEffect("detail") {
        component.load()
    }

    DisposableEffect(model.value.isLoading) {
        if (model.value.isLoading) {
            showProgress()
        } else {
            hideProgress()
        }
        onDispose { }
    }

    val focus = remember { FocusRequester() }

    LaunchedEffect(isFullScreen.value) {
        focus.requestFocus()
    }
    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier) {
            if (!isFullScreen.value) {
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
                                detail?.vodName ?: "",
                                textStyle = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                                modifier = Modifier.padding(start = 50.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.End) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        component.clear()
                                        component.quickSearch()
                                        SnackBar.postMsg("重新加载")
                                    }
                                },
                                enabled = !model.value.isLoading
                            ) {
                                Icon(
                                    Icons.Default.Autorenew,
                                    contentDescription = "renew",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
            val mrl = derivedStateOf { model.value.currentPlayUrl }
            Row(
                modifier = Modifier.fillMaxHeight(videoHeight.value),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val internalPlayer = derivedStateOf {
                    SettingStore.getPlayerSetting()[0] as Boolean
                }
                if (internalPlayer.value) {
                    SideEffect {
                        focus.requestFocus()
                    }
                    Player(
                        mrl.value, controller.value, Modifier.fillMaxWidth(videoWidth.value).focusable(),
                        component,
                        focusRequester = focus
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth(videoWidth.value)
                            .fillMaxHeight()
                            .background(Color.Black)
                    ) {
                        Text(
                            "使用外部播放器",
                            modifier = Modifier.align(Alignment.Center).focusRequester(focus),
                            fontWeight = FontWeight.Bold,
                            fontSize = TextUnit(23f, TextUnitType.Sp),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                AnimatedVisibility(!isFullScreen.value, modifier = Modifier.fillMaxSize()) {
                    EpChooser(
                        component, Modifier.fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp)
                    )
                }
            }
            AnimatedVisibility(!isFullScreen.value) {
                val searchResultList = derivedStateOf { model.value.quickSearchResult.toList() }
                Box(Modifier) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.size(15.dp))
                        Column(Modifier.fillMaxWidth(0.3f)) {
                            quickSearchResult(model, searchResultList, component)
                        }
                        Column(
                            modifier = Modifier.padding(start = 10.dp)
                                .fillMaxSize()
                        ) {
                            if (model.value.detail == null) {
                                emptyShow(onRefresh = { component.load() })
                            } else {
                                vodInfo(detail)
                            }
                            Spacer(modifier = Modifier.size(15.dp))
                            // 线路
                            flags(scope, controller.value, component)
                            Spacer(Modifier.size(15.dp))
                            val urls = rememberUpdatedState(component.model.value.currentUrl)
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
                                            RatioBtn(item.n ?: (i + 1).toString(), onClick = {
                                                component.model.update {
                                                    it.copy(
                                                        currentUrl = urls.value?.copy(position = i),
                                                        currentPlayUrl = item.v ?: ""
                                                    )
                                                }
                                            }, i == urls.value?.position!!)
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
        Dialog(Modifier.align(Alignment.CenterEnd)
            .fillMaxWidth(0.3f)
            .fillMaxHeight(0.8f)
            .padding(end = 20.dp),
            showDialog = showEpChooserDialog.value,
            onClose = { component.model.update { it.copy(showEpChooserDialog = false) } }) {
            EpChooser(
                component, Modifier.fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 5.dp)
            )
        }

    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun flags(
    scope: CoroutineScope,
    controller: VlcjFrameController,
    component: DetailComponent
) {
    val model = component.model.subscribeAsState()
    val detail = derivedStateOf { model.value.detail }
    Row(Modifier.padding(start = 10.dp)) {
        Text(
            "线路",
            fontSize = TextUnit(25F, TextUnitType.Sp),
            modifier = Modifier.padding(bottom = 5.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.size(10.dp))
        val detailIsNotEmpty = derivedStateOf { detail.value?.vodFlags?.isNotEmpty() }
        if (detailIsNotEmpty.value == true) {
            val state = rememberLazyListState(0)
            Box() {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    state = state,
                    modifier = Modifier.padding(bottom = 10.dp)
                        .fillMaxWidth()
                        .onPointerEvent(PointerEventType.Scroll) {
                            scope.launch {
//                                            if(it.changes.size == 0) return@launch
                                state.scrollBy(it.changes.first().scrollDelta.y * state.layoutInfo.visibleItemsInfo.first().size)
                            }
                        },
                ) {
                    val flagList = derivedStateOf { detail.value?.vodFlags?.toList() ?: listOf() }
                    items(flagList.value) {
                        RatioBtn(it?.show ?: "", onClick = {
                            scope.launch {
                                for (vodFlag in detail.value!!.vodFlags) {
                                    if (it?.show == vodFlag?.show) {
                                        it?.activated = true
                                        detail.value!!.currentFlag = it
                                    } else {
                                        vodFlag?.activated = false
                                    }
                                }
                                val dt = detail.value!!.copy(
                                    currentFlag = it,
                                    subEpisode = it?.episodes?.getPage(detail.value!!.currentTabIndex)
                                        ?.toMutableList()
                                )
                                controller.doWithHistory { it.copy(vodFlag = detail.value!!.currentFlag?.flag) }
                                val history = controller.history.value
                                if (history != null) {
                                    val findEp =
                                        detail.value!!.findAndSetEpByName(controller.history.value!!)
                                    if (findEp != null) component.playEp(dt, findEp)
                                }
                                component.model.update { model ->
                                    model.copy(
                                        detail = dt,
                                        shouldPlay = true,
                                    )
                                }
                            }
                        }, selected = it?.activated ?: false)
                    }
                }
                val showScrollBar =
                    derivedStateOf { state.layoutInfo.visibleItemsInfo.size < (detail.value!!.vodFlags.size) }
                if (showScrollBar.value) {
                    HorizontalScrollbar(
                        rememberScrollbarAdapter(state),
                        style = defaultScrollbarStyle().copy(
                            unhoverColor = Color.Gray.copy(0.45F),
                            hoverColor = Color.DarkGray
                        ), modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun quickSearchResult(
    model: State<DetailComponent.Model>,
    searchResultList: State<List<Vod>>,
    component: DetailComponent
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
                modifier = Modifier.align(Alignment.CenterEnd),
                adapter = adapter, style = defaultScrollbarStyle().copy(
                    unhoverColor = Color.Gray.copy(0.45F),
                    hoverColor = Color.DarkGray
                )
            )
        }
    }
}

@Composable
private fun vodInfo(detail: Vod?) {
    Column(Modifier.padding(10.dp)) {
        Row() {
            if (detail?.site?.name?.isNotBlank() == true) {
                Text(
                    "站源: " + detail.site?.name,
                    color = MaterialTheme.colorScheme.onSurface
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
                s.joinToString(separator = " | "),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            "导演：${detail?.vodDirector ?: "无"}",
            color = MaterialTheme.colorScheme.onSurface
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
fun EpChooser(component: DetailComponent, modifier: Modifier) {
    val model = component.model.subscribeAsState()
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
                detail.value?.currentFlag != null && (detail.value?.currentFlag?.episodes?.size ?: 0) > 0
            }
            if (show.value) {
                Text(
                    "共${detail.value?.currentFlag?.episodes?.size}集",
                    textAlign = TextAlign.End,
                    fontSize = TextUnit(15F, TextUnitType.Sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        val epSize = derivedStateOf { detail.value?.currentFlag?.episodes?.size ?: 0 }

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
                                selected = detail.value?.currentTabIndex == (i / Constants.EpSize),
                                onClick = {
                                    detail.value?.currentTabIndex = i / Constants.EpSize
                                    val dt = detail.value?.copy(
                                        subEpisode = detail.value?.currentFlag?.episodes?.getPage(
                                            detail.value!!.currentTabIndex
                                        )
                                            ?.toMutableList()
                                    )
                                    component.model.update { it.copy(detail = dt) }
                                },
                                text = "${i + 1}-${i + Constants.EpSize}"
                            )
                        }
                    }
                }
                HorizontalScrollbar(
                    adapter = scrollBarAdapter,
                    modifier = Modifier.padding(bottom = 5.dp)
                        .align(Alignment.BottomCenter),
                    style = defaultScrollbarStyle().copy(
                        unhoverColor = Color.Gray.copy(0.45F),
                        hoverColor = Color.DarkGray
                    )
                )
            }
        }
        val videoLoading = remember { mutableStateOf(false) }
        val epList = derivedStateOf { detail.value?.subEpisode ?: listOf() }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = rememberLazyGridState(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items(
                epList.value, key = { it.url + it.number }
            ) {
                TooltipArea(
                    tooltip = {
                        // composable tooltip content
                        Surface(
                            modifier = Modifier.shadow(4.dp),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = it.name,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                    },
                    delayMillis = 600
                ) {
                    RatioBtn(text = it.name, onClick = {
                        videoLoading.value = true
                        SiteViewModel.viewModelScope.launch {
                            for (i in detail.value?.currentFlag?.episodes ?: listOf()) {
                                i.activated = (i.name == it.name)
                                if (i.activated) {
                                    component.model.update { model ->
                                        if (model.currentEp?.name != it.name) {
                                            component.controller.doWithHistory { it.copy(position = 0L) }
                                        }
                                        component.controller.doWithHistory {
                                            it.copy(
                                                episodeUrl = i.url,
                                                vodRemarks = i.name
                                            )
                                        }
                                        model.copy(currentEp = i)
                                    }
                                }
                            }
                            val dt = detail.value?.copy(
                                subEpisode = detail.value?.currentFlag?.episodes?.getPage(
                                    detail.value!!.currentTabIndex
                                )
                                    ?.toMutableList()?.toList()?.toMutableList(),
                            )
                            component.model.update { it.copy(detail = dt) }
                            val result = SiteViewModel.playerContent(
                                detail.value?.site?.key ?: "",
                                detail.value?.currentFlag?.flag ?: "",
                                it.url
                            )
                            component.model.update { it.copy(currentUrl = result?.url) }
                            val internalPlayer = SettingStore.getPlayerSetting()[0] as Boolean
                            if (internalPlayer) {
                                component.play(result)
                            } else {
                                Play.start(result?.url?.v() ?: "", model.value.currentEp?.name)
                            }
                        }.invokeOnCompletion {
                            videoLoading.value = false
                        }
                    }, selected = it.activated, it.activated && videoLoading.value)
                }
            }
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
