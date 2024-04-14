package com.corner.ui.video

import AppTheme
import SiteViewModel
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.corner.catvod.enum.bean.Vod
import com.corner.catvod.enum.bean.Vod.Companion.getPage
import com.corner.catvodcore.bean.Episode
import com.corner.catvodcore.bean.detailIsEmpty
import com.corner.catvodcore.config.api
import com.corner.database.Db
import com.corner.ui.scene.*
import com.corner.util.Utils.cancelAll
import com.corner.util.play.Play
import com.seiko.imageloader.ui.AutoSizeImage
import kotlinx.coroutines.*
import org.apache.commons.lang3.StringUtils
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.slf4j.LoggerFactory

private var detail by mutableStateOf<Vod?>(null)
private var supervisor = SupervisorJob()
private val searchScope = CoroutineScope(Dispatchers.Default + supervisor)
private val jobList = mutableListOf<Job>()
private val log = LoggerFactory.getLogger("Detail")

@OptIn(ExperimentalResourceApi::class, ExperimentalFoundationApi::class)
@Composable
fun DetailDialog(showDetailDialog: Boolean, vod: Vod?, key: String, onClose: () -> Unit) {
    Dialog(modifier = Modifier.fillMaxSize(0.9f),
        showDetailDialog,
        onClose = {
            closeDetailDialog(onClose)
        }) {
        LaunchedEffect("detail") {
            SiteViewModel.viewModelScope.launch {
                val dt = SiteViewModel.detailContent(key, vod?.vodId!!)
                if (dt == null || dt.detailIsEmpty()) {
                    quickSearch(vod)
                } else {
                    detail = dt.list[0]
                    detail =
                        detail!!.copy(subEpisode = detail?.currentFlag?.episodes?.getPage(detail!!.currentTabIndex))
                    if (StringUtils.isNotBlank(vod.vodRemarks)) {
                        for (it: Episode in detail?.subEpisode ?: listOf()) {
                            if (it.name.equals(vod.vodRemarks)) {
                                it.activated = true
                                break;
                            }
                        }
                    }
                    detail?.site = vod.site
                }
            }
        }
        DisposableEffect(detail) {
            println("detail修改")
            onDispose {
            }
        }
        Box(modifier = Modifier.padding(10.dp)) {
            IconButton(onClick = {
                closeDetailDialog(onClose)
            }, modifier = Modifier.align(Alignment.TopEnd).zIndex(999F)) {
                Icon(Icons.Outlined.Close, "close")
            }
            Column(Modifier.scrollable(state = rememberScrollState(0), orientation = Orientation.Vertical)) {
                if (detail == null) {
                    LoadingIndicator(true)
                } else {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(0.3f)) {
                            AutoSizeImage(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                                url = detail?.vodPic!!,
                                contentDescription = detail?.vodName,
                                contentScale = ContentScale.Crop,
                                placeholderPainter = { painterResource("empty.png") },
                                errorPainter = { painterResource("empty.png") }
                            )
                            if (quickSearchResult.value.isNotEmpty()) {
                                Spacer(Modifier.size(20.dp))
                                val quickState = rememberLazyGridState()
                                val adapter = rememberScrollbarAdapter(quickState)
                                Box{
                                    LazyVerticalGrid(
                                        modifier = Modifier.padding(end = 10.dp),
                                        columns = GridCells.Fixed(2),
                                        state = quickState,
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        items(quickSearchResult.value) {
                                            QuickSearchItem(it) {
                                                SiteViewModel.viewModelScope.launch {
                                                    loadDetail(it)
                                                }
                                            }
                                        }
                                    }
                                    VerticalScrollbar(modifier = Modifier.align(Alignment.CenterEnd),
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
                            ToolTipText(detail?.vodName ?: "无", MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.size(10.dp))
                            Row {
                                if (detail?.site?.name?.isNotBlank() == true) {
                                    Text("站源: " + detail?.site?.name)
                                    Spacer(Modifier.width(5.dp))
                                }
                                val s = mutableListOf<String>()
                                Text(detail?.vodYear ?: "")
                                if (StringUtils.isNotBlank(detail?.vodArea)) {
                                    s.add(detail?.vodArea!!)
                                }
                                if (StringUtils.isNotBlank(detail?.cate)) {
                                    s.add(detail?.cate!!)
                                }
                                if (StringUtils.isNotBlank(detail?.typeName)) {
                                    s.add(detail?.typeName!!)
                                }
                                Text(s.joinToString(separator = " | "))
                            }
                            Text("导演：${detail?.vodDirector ?: "无"}")
                            ExpandedText("演员：${detail?.vodActor ?: "无"}", 2)
                            ExpandedText("简介：${detail?.vodContent?.trim() ?: "无"}", 3)
                            // 线路
                            Spacer(modifier = Modifier.size(20.dp))
                            Text(
                                "线路",
                                fontSize = TextUnit(25F, TextUnitType.Sp),
                                modifier = Modifier.padding(bottom = 5.dp)
                            )
                            if (detail?.vodFlags?.isNotEmpty() == true) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    items(detail?.vodFlags?.toList() ?: listOf()) {
                                        RatioBtn(it?.show ?: "", onClick = {

                                            for (vodFlag in detail?.vodFlags ?: listOf()) {
                                                if (it?.show == vodFlag?.show) {
                                                    it?.activated = true
                                                } else {
                                                    vodFlag?.activated = false
                                                }
                                            }
                                            detail = detail?.copy(
                                                currentFlag = it,
                                                subEpisode = it?.episodes?.getPage(detail!!.currentTabIndex)
                                                    ?.toMutableList()
                                            )
                                        }, selected = it?.activated ?: false)
                                    }
                                }

                                //
                                Spacer(modifier = Modifier.size(20.dp))
                                Row {
                                    if (detail?.currentFlag != null && (detail?.currentFlag?.episodes?.size ?: 0) > 0) {
                                        Text(
                                            "选集",
                                            fontSize = TextUnit(25F, TextUnitType.Sp),
                                            modifier = Modifier.padding(bottom = 5.dp)
                                        )
                                        Spacer(Modifier.size(10.dp))
                                        Text("共${detail?.currentFlag?.episodes?.size}集", textAlign = TextAlign.End)
                                    }
                                }

                                val epSize = detail?.currentFlag?.episodes?.size ?: 0

                                val scrollState = rememberLazyListState(0)
                                val scrollBarAdapter = rememberScrollbarAdapter(scrollState)
                                if (epSize > 15) {
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
                                                        detail = detail?.copy(
                                                            subEpisode = detail?.currentFlag?.episodes?.getPage(detail!!.currentTabIndex)
                                                                ?.toMutableList()
                                                        )
                                                    },
                                                    text = "${i + 1}-${i + 15}"
                                                )
                                            }
                                        }
                                    }
                                    HorizontalScrollbar(
                                        adapter = scrollBarAdapter,
                                        modifier = Modifier.padding(bottom = 5.dp)
                                    )
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
                                                    detail = detail?.copy(
                                                        subEpisode = detail?.currentFlag?.episodes?.getPage(detail!!.currentTabIndex)
                                                            ?.toMutableList()?.toList()?.toMutableList(),
                                                        version = (detail!!.version++)
                                                    )
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
}

private fun closeDetailDialog(onClose: () -> Unit) {
    quickSearchResult.value = listOf()
    detail = null
    SiteViewModel.clearQuickSearch()
    jobList.cancelAll().clear()
    launched = false
    onClose()
}

private fun nextSite(lastVod: Vod?) {
    if (quickSearchResult.value.isEmpty()) return
    val list = quickSearchResult.value.toMutableList()
    if (lastVod != null) list.remove(lastVod)
    quickSearchResult.value = list
    if (quickSearchResult.value.isNotEmpty()) loadDetail(quickSearchResult.value[0])
}

@Volatile
private var launched = false

private fun loadDetail(vod: Vod) {
    val dt = SiteViewModel.detailContent(vod.site?.key!!, vod.vodId)
    if (dt == null || dt.detailIsEmpty()) {
        nextSite(vod)
    } else {
        if (vod.site?.name?.isNotBlank() == true && detail != null && !detail?.site?.name?.equals(vod.site!!.name)!!) SnackBar.postMsg(
            "正在切换站源至 [${vod.site!!.name}]"
        )
        val first = dt.list[0]
        detail = first.copy(
            subEpisode = first.vodFlags.get(0)?.episodes?.getPage(first.currentTabIndex)?.toMutableList()
        )
        detail?.site = vod.site
        launched = false
//        searchScope.cancel("已找到可用站源${detail?.site?.name}")
        supervisor.cancelChildren()
        jobList.cancelAll().clear()
    }
}

val quickSearchResult = mutableStateOf<List<Vod>>(listOf())
private val lock = Any()
private fun quickSearch(vod: Vod) {
    searchScope.launch {
        val quickSearchSites = api?.sites?.filter { it.changeable == 1 }
        quickSearchSites?.forEach {
            val job = launch() {
                withTimeout(2500L) {
                    SiteViewModel.searchContent(it, vod.vodName ?: "", true)
                    log.debug("{}完成搜索", it.name)
                }
            }

            job.invokeOnCompletion {
                quickSearchResult.value = SiteViewModel.quickSearch.get(0).getList()?.toList() ?: listOf()
                log.debug("一个job执行完毕 result size:{}", quickSearchResult.value.size)

                synchronized(lock) {
                    if (quickSearchResult.value.isNotEmpty() && detail == null && !launched) {
                        launched = true
                        loadDetail(quickSearchResult.value[0])
                    }
                }
            }
            jobList.add(job)
        }
        jobList.forEach {
            it.join()
        }
        if (quickSearchResult.value.isEmpty()) {
            detail = vod
            SnackBar.postMsg("暂无线路数据")
        }
    }
}

@Composable
@Preview
fun previewLeftCol() {
    val quickSearch = buildVodList()
    AppTheme(useDarkTheme = true) {
        val quickState = rememberLazyGridState(0)
        Row {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = quickState,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                userScrollEnabled = true
            ) {
                items(quickSearch) {
                    QuickSearchItem(it) {
                        SiteViewModel.viewModelScope.launch {
                            loadDetail(it)
                        }
                    }
                }
            }
            VerticalScrollbar(
                rememberScrollbarAdapter(quickState),
                style = defaultScrollbarStyle().copy(
                    unhoverColor = Color.DarkGray.copy(0.3F),
                    hoverColor = Color.DarkGray
                )
            )
        }
    }
}

private fun buildVodList(): List<Vod> {
    val list = mutableListOf<Vod>()
    for (i in 0 until 30) {
        list.add(Vod(vodId = "$i", vodName = "name$i", vodRemarks = "remark$i"))
    }
    return list
}
