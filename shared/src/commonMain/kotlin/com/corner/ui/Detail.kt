package com.corner.ui

import SiteViewModel
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.arkivanov.decompose.value.update
import com.corner.catvod.enum.bean.Vod.Companion.getPage
import com.corner.database.Db
import com.corner.ui.decompose.DetailComponent
import com.corner.ui.scene.BackRow
import com.corner.ui.scene.ExpandedText
import com.corner.ui.scene.LoadingIndicator
import com.corner.ui.scene.RatioBtn
import com.corner.ui.video.QuickSearchItem
import com.corner.util.play.Play
import com.seiko.imageloader.ui.AutoSizeImage
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils


@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun DetailScene(component: DetailComponent, onClickBack: () -> Unit) {
    val model = component.model.subscribeAsState()
    val scope = rememberCoroutineScope()

    val detail by rememberUpdatedState(model.value.detail)

    LaunchedEffect("detail") {
        component.load()
    }
    DisposableEffect(model.value.detail) {
        println("detail修改")
        onDispose {
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.padding(8.dp)) {
            BackRow(Modifier, onClickBack = {
                component.clear()
                onClickBack()
            }) {
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically){
                    Row(horizontalArrangement = Arrangement.Start){
                        Text(
                            detail?.vodName ?: "",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(start = 50.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.End){
                        IconButton(
                            onClick = {
                                SiteViewModel.viewModelScope.launch {
                                    component.quickSearch()
                                }
                            },
                            enabled = !model.value.isQuickSearch
                        ) {
                            if(model.value.isQuickSearch){
                                LoadingIndicator(true)
                            }else{
                                Icon(Icons.Default.Autorenew, contentDescription = "renew", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }

            }

            if (model.value.detail == null) {
                LoadingIndicator(true)
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(0.3f)) {
                        AutoSizeImage(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .fillMaxHeight(0.4f),
//                                .height(180.dp).width(160.dp),
                            url = detail?.vodPic ?: "",
                            contentDescription = detail?.vodName,
                            contentScale = ContentScale.FillHeight,
                            placeholderPainter = { painterResource("empty.png") },
                            errorPainter = { painterResource("empty.png") }
                        )
                        if (model.value.quickSearchResult.isNotEmpty()) {
                            Spacer(Modifier.size(20.dp))
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
                                    items(model.value.quickSearchResult) {
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
                    val rememberScrollState = rememberScrollState(0)
                    Column(
                        modifier = Modifier.padding(start = 10.dp)
                            .scrollable(state = rememberScrollState, orientation = Orientation.Vertical)
                            .weight(0.8f)
                    ) {
//                        ToolTipText(detail?.vodName ?: "无", MaterialTheme.typography.headlineMedium)
//                        Spacer(modifier = Modifier.size(10.dp))
                        Row {
                            if (detail?.site?.name?.isNotBlank() == true) {
                                Text("站源: " + detail?.site?.name, color = MaterialTheme.colorScheme.onSurface)
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
                            Text(s.joinToString(separator = " | "), color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("导演：${detail?.vodDirector ?: "无"}", color = MaterialTheme.colorScheme.onSurface)
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
                        // 线路
                        Spacer(modifier = Modifier.size(20.dp))
                        Text(
                            "线路",
                            fontSize = TextUnit(25F, TextUnitType.Sp),
                            modifier = Modifier.padding(bottom = 5.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (detail?.vodFlags?.isNotEmpty() == true) {
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
                                    items(detail?.vodFlags?.toList() ?: listOf()) {
                                        RatioBtn(it?.show ?: "", onClick = {

                                            for (vodFlag in detail?.vodFlags ?: listOf()) {
                                                if (it?.show == vodFlag?.show) {
                                                    it?.activated = true
                                                } else {
                                                    vodFlag?.activated = false
                                                }
                                            }
                                            val dt = detail?.copy(
                                                currentFlag = it,
                                                subEpisode = it?.episodes?.getPage(detail!!.currentTabIndex)
                                                    ?.toMutableList()
                                            )
                                            component.model.update { it.copy(detail = dt) }
                                        }, selected = it?.activated ?: false)
                                    }
                                }
                                if (state.layoutInfo.visibleItemsInfo.size < (detail?.vodFlags?.size ?: 0)) {
                                    HorizontalScrollbar(
                                        rememberScrollbarAdapter(state),
                                        style = defaultScrollbarStyle().copy(
                                            unhoverColor = Color.Gray.copy(0.45F),
                                            hoverColor = Color.DarkGray
                                        ), modifier = Modifier.align(Alignment.BottomCenter)
                                    )
                                }
                            }
                            //
                            Spacer(modifier = Modifier.size(20.dp))
                            Row {
                                if (detail?.currentFlag != null && (detail?.currentFlag?.episodes?.size ?: 0) > 0) {
                                    Text(
                                        "选集",
                                        fontSize = TextUnit(25F, TextUnitType.Sp),
                                        modifier = Modifier.padding(bottom = 5.dp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.size(10.dp))
                                    Text(
                                        "共${detail?.currentFlag?.episodes?.size}集",
                                        textAlign = TextAlign.End,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            val epSize = detail?.currentFlag?.episodes?.size ?: 0

                            val scrollState = rememberLazyListState(0)
                            val scrollBarAdapter = rememberScrollbarAdapter(scrollState)
                            if (epSize > 15) {
                                Box(modifier = Modifier.padding(bottom = 2.dp)) {
                                    LazyRow(
                                        state = scrollState,
                                        modifier = Modifier.padding(bottom = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        for (i in 0 until epSize step 15) {
                                            item {
                                                RatioBtn(
                                                    selected = detail?.currentTabIndex == (i / 15),
                                                    onClick = {
                                                        detail?.currentTabIndex = i / 15
                                                        val dt = detail?.copy(
                                                            subEpisode = detail?.currentFlag?.episodes?.getPage(detail!!.currentTabIndex)
                                                                ?.toMutableList()
                                                        )
                                                        component.model.update { it.copy(detail = dt) }
                                                    },
                                                    text = "${i + 1}-${i + 15}"
                                                )
                                            }
                                        }
                                    }
                                    HorizontalScrollbar(
                                        adapter = scrollBarAdapter,
                                        modifier = Modifier.padding(bottom = 5.dp).align(Alignment.BottomCenter),
                                        style = defaultScrollbarStyle().copy(
                                            unhoverColor = Color.Gray.copy(0.45F),
                                            hoverColor = Color.DarkGray
                                        )
                                    )
                                }
                            }
                            val videoLoading = remember { mutableStateOf(false) }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                state = rememberLazyGridState(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                items(
                                    detail?.subEpisode ?: listOf()
                                ) {
                                    TooltipArea(
                                        tooltip = {
                                            // composable tooltip content
                                            Surface(
                                                modifier = Modifier.shadow(4.dp),
//                                                    color = MaterialTheme.colors.surface,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = it.name ?: "",
                                                    modifier = Modifier.padding(10.dp),
//                                                        color = MaterialTheme.colors.onSurface
                                                )
                                            }
                                        },
                                        delayMillis = 600
                                    ) {
                                        RatioBtn(text = it.name ?: "", onClick = {
                                            videoLoading.value = true
                                            SiteViewModel.viewModelScope.launch {
                                                for (i in detail?.currentFlag?.episodes ?: listOf()) {
                                                    i.activated = (i.name == it.name)
                                                }
                                                val dt = detail?.copy(
                                                    subEpisode = detail?.currentFlag?.episodes?.getPage(detail!!.currentTabIndex)
                                                        ?.toMutableList()?.toList()?.toMutableList(),
                                                    version = (detail!!.version++)
                                                )
                                                component.model.update { it.copy(detail = dt) }
                                                val result = SiteViewModel.playerContent(
                                                    detail?.site?.key ?: "",
                                                    detail?.currentFlag?.flag ?: "",
                                                    it.url ?: ""
                                                )
                                                Play.start(result, it.name ?: detail?.vodName)
                                                Db.History.create(detail!!, detail?.currentFlag?.flag!!, it.name!!)
                                            }.invokeOnCompletion {
                                                videoLoading.value = false
                                            }
                                        }, selected = it.activated, it.activated && videoLoading.value)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}