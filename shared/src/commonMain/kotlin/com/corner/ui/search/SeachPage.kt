package com.corner.ui.search

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
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
import com.corner.bean.HotData
import com.corner.bean.SettingStore.getHistoryList
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.AppTheme
import com.corner.ui.scene.ExpandedText

/**
 *历史记录
 *热门推荐
 */
@Composable
fun SearchPage(onClickBack: () -> Unit, onSearch: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect("focus") {
        focusRequester.requestFocus()
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        Column(modifier = Modifier.align(alignment = Alignment.TopStart)) {
            // TopBar
            Row(modifier = Modifier.align(Alignment.Start).background(MaterialTheme.colors.background).height(80.dp)) {
                IconButton(
                    modifier = Modifier.align(Alignment.CenterVertically)
                        .padding(start = 20.dp, end = 20.dp),
                    onClick = { onClickBack() }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = "back to video home",
                        tint = MaterialTheme.colors.onBackground
                    )
                }
                SearchBar(Modifier.align(Alignment.CenterVertically), focusRequester, "", onSearch, false)
            }
            Column(Modifier.fillMaxSize()) {
                if (getHistoryList().isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxHeight(0.3f)) {
                        HistoryPanel(Modifier.padding(15.dp), getHistoryList()) {
                            onSearch(it)
                        }
                    }
                }
                if (GlobalModel.hotList.value.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxHeight()) {
                        HotPanel(Modifier.padding(horizontal = 15.dp), GlobalModel.hotList.value) {
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
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.h6
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
                    .background(MaterialTheme.colors.surface, shape = RoundedCornerShape(5.dp))
                    .shadow(5.dp, RoundedCornerShape(5.dp), clip = true, ambientColor = Color.Gray)
                    .clip(RoundedCornerShape(5.dp))
                    .padding(15.dp)
            ) {
                if(hotData.comment.isNotEmpty()){
                    Text(hotData.comment, style = MaterialTheme.typography.h6)
                }
                Text(
                    modifier = Modifier.padding(top = 10.dp),
                    text = hotData.description,
                    style = MaterialTheme.typography.body2
                )
            }
        }
    }) {
        Surface(
            modifier
                .background(MaterialTheme.colors.surface, shape = RoundedCornerShape(15.dp))
                .border(width = 1.dp, MaterialTheme.colors.secondaryVariant, RoundedCornerShape(15.dp))
                .clickable(enabled = true) { onClick(hotData) }
                .clip(RoundedCornerShape(15.dp)),
        ) {
            Column(
                modifier = modifier.padding(10.dp).wrapContentWidth(Alignment.CenterHorizontally).wrapContentHeight()
            ) {
                Text(hotData.title, style = MaterialTheme.typography.h6.copy(fontSize = TextUnit(15f, TextUnitType.Sp)))
                ExpandedText(hotData.upinfo.trim(), 1, MaterialTheme.typography.caption)
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
        modifier.background(MaterialTheme.colors.surface, shape = RoundedCornerShape(15.dp))
            .border(width = 1.dp, MaterialTheme.colors.secondaryVariant, RoundedCornerShape(15.dp))
            .clickable(enabled = true) { onClick(str) }
            .clip(RoundedCornerShape(15.dp))
    ) {
        Text(str, color = MaterialTheme.colors.onBackground, modifier = modifier.padding(10.dp))
    }
}

@Composable
fun HistoryPanel(modifier: Modifier, histories: Set<String>, onClick: (String) -> Unit) {
    val list by rememberUpdatedState(histories)
    Column(modifier = modifier) {
        Text(
            "搜索历史",
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 15.dp),
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.h6
        )
//        LazyHorizontalGrid(rows = GridCells.)
        LazyHorizontalStaggeredGrid(
            rows = StaggeredGridCells.Adaptive(55.dp),
            state = rememberLazyStaggeredGridState(), contentPadding = PaddingValues(10.dp),
            horizontalItemSpacing = 8.dp,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = true
        ) {
            items(list.toList()) {
                HistoryItem(Modifier.wrapContentHeight(), it) {
                    onClick(it)
                }
            }
        }
    }
}