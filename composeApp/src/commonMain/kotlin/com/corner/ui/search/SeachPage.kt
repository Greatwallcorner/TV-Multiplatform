package com.corner.ui.search

import AppTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.corner.bean.HotData
import com.corner.ui.decompose.component.DefaultSearchPageComponent
import com.corner.ui.scene.ControlBar
import com.corner.ui.scene.ExpandedText

/**
 *历史记录
 *热门推荐
 */
@Composable
fun SearchPage(component: DefaultSearchPageComponent, onClickBack: () -> Unit, onSearch: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val model = component.model.subscribeAsState()

    LaunchedEffect("focus") {
        focusRequester.requestFocus()
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.align(alignment = Alignment.TopStart)) {
            // TopBar
            ControlBar(leading = {
                Row(modifier = Modifier.align(Alignment.Start)/*.background(MaterialTheme.colors.background)*/.height(80.dp)) {
                    IconButton(
                        modifier = Modifier.align(Alignment.CenterVertically)
                            .padding(start = 20.dp, end = 20.dp),
                        onClick = { onClickBack() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "back to video home",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    SearchBar(Modifier, focusRequester, "", onSearch, false)
                }
            }, center = {})
            Column(Modifier.fillMaxSize()) {
                if (model.value.historyList.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxHeight(0.4f)) {
                        HistoryPanel(Modifier.padding(15.dp), model.value.historyList) {
                            onSearch(it)
                        }
                    }
                }
                if (model.value.hotList.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxHeight()) {
                        HotPanel(Modifier.padding(horizontal = 15.dp), model.value.hotList) {
                            onSearch(it.title)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun HotPanel(modifier: Modifier, hots: List<HotData>, onClick: (HotData) -> Unit) {
    val hotList by rememberUpdatedState(hots)
    Column(modifier = modifier) {
        Text(
            "热搜",
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 15.dp),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium
        )
        LazyHorizontalStaggeredGrid(
            rows = StaggeredGridCells.Adaptive(80.dp),
            state = rememberLazyStaggeredGridState(), contentPadding = PaddingValues(10.dp),
            horizontalItemSpacing = 8.dp,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
            userScrollEnabled = true
        ) {
            items(hotList.toList(), key = { i -> i.title }) {
                HotItem(Modifier.wrapContentHeight(), it) {
                    onClick(it)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HotItem(modifier: Modifier, hotData: HotData, onClick: (HotData) -> Unit) {
    TooltipArea(tooltip = {
        Surface(modifier = Modifier.border(width = 2.dp, Color.Gray.copy(blue = 0.6f), shape = RoundedCornerShape(5.dp))) {
            Column(
                modifier = Modifier.fillMaxWidth(0.5f)
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(5.dp))
                    .shadow(5.dp, RoundedCornerShape(5.dp), clip = true, ambientColor = Color.Gray)
                    .clip(RoundedCornerShape(5.dp))
                    .padding(15.dp)
            ) {
                if(hotData.comment.isNotEmpty()){
                    Text(hotData.comment, style = MaterialTheme.typography.headlineMedium)
                }
                Text(
                    modifier = Modifier.padding(top = 10.dp),
                    text = hotData.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }) {
        Surface(
            modifier
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(15.dp))
                .border(width = 1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(15.dp))
                .clickable(enabled = true) { onClick(hotData) }
                .clip(RoundedCornerShape(15.dp)),
        ) {
            Column(
                modifier = modifier.padding(10.dp).wrapContentWidth(Alignment.CenterHorizontally).wrapContentHeight()
            ) {
                Text(hotData.title, style = MaterialTheme.typography.headlineMedium.copy(fontSize = TextUnit(15f, TextUnitType.Sp)))
                ExpandedText(hotData.upinfo.trim(), 1, MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Preview
@Composable
fun previewHotItem() {
    AppTheme {
        val hot = HotData("阿凡达", "潘多拉", "更新到第二季", "hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhfffffff")
        HotItem(Modifier, hot) {}
    }
}

@Preview
@Composable
fun previewHotPanel() {
    AppTheme(useDarkTheme = true) {
        val hot = HotData("阿凡达", "潘多拉", "更新到第二季", "hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhfffffff")
        val hots = mutableListOf<HotData>()
        for (i in 0 until 10) {
            hots.add(hot.copy(title = hot.title + i))
        }
        HotPanel(Modifier, hots) {}
    }
}

@Composable
fun HistoryItem(modifier: Modifier, str: String, onClick: (String) -> Unit) {
    Surface(
        modifier
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(15.dp))
            .border(width = 1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(15.dp))
            .clickable(enabled = true) { onClick(str) }
            .clip(RoundedCornerShape(15.dp))
    ) {
        Text(str, color = MaterialTheme.colorScheme.onBackground, modifier = modifier.padding(10.dp))
    }
}

@Composable
fun HistoryPanel(modifier: Modifier, histories: Set<String>, onClick: (String) -> Unit) {
    val list by rememberUpdatedState(histories)
    Column(modifier = modifier) {
        Text(
            "搜索历史",
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 15.dp),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium
        )
        Box(){
            val state = rememberLazyStaggeredGridState()
            LazyHorizontalStaggeredGrid(
                rows = StaggeredGridCells.Adaptive(55.dp),
                state = state, contentPadding = PaddingValues(10.dp),
                horizontalItemSpacing = 8.dp,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxWidth(),
                userScrollEnabled = true,
            ) {
                items(list.toList()) {
                    HistoryItem(Modifier.wrapContentHeight(), it) {
                        onClick(it)
                    }
                }
            }
        }
    }
}