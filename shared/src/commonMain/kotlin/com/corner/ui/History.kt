package com.corner.ui

import SiteViewModel
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.corner.catvod.enum.bean.Vod
import com.corner.database.Db
import com.corner.database.History
import com.corner.database.repository.buildVod
import com.corner.database.repository.getSiteKey
import com.corner.ui.decompose.component.DefaultHistoryComponentComponent
import com.corner.ui.scene.BackRow
import com.corner.ui.scene.hideProgress
import com.corner.ui.scene.showProgress
import com.corner.ui.video.DetailDialog
import com.seiko.imageloader.ui.AutoSizeImage
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
fun HistoryItem(
    modifier: Modifier,
    history: History,
    showSite: Boolean,
    onDelete: (String) -> Unit,
    click: (History) -> Unit
) {
    Card(
        modifier = modifier.clickable(enabled = true, onClick = { click(history) }),
        elevation = 8.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        ContextMenuArea(items = {
            listOf(ContextMenuItem("删除") {
                onDelete(history.key)
            })
        }) {
            Box(modifier = modifier) {
                AutoSizeImage(url = history.vodPic!!,
                    modifier = Modifier.height(220.dp),
                    contentDescription = history.vodName,
                    contentScale = ContentScale.Crop,
                    placeholderPainter = { painterResource("empty.png") },
                    errorPainter = { painterResource("empty.png") })
                Text(
                    text = history.vodName!!,
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
                text = if (showSite) history.vodFlag ?: "" else history.vodRemarks!!,
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
fun HistoryScene(component: DefaultHistoryComponentComponent, onClickBack: () -> Unit) {
    val model = component.model.subscribeAsState()
    var chooseHistory by remember { mutableStateOf<History?>(null) }
    var vod by remember { mutableStateOf<Vod?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }
    DisposableEffect(chooseHistory) {
        if (chooseHistory != null) {
            vod = chooseHistory?.buildVod()
            showDetailDialog = true
        }
        onDispose { }
    }
    LaunchedEffect(Unit) {
        showProgress()
        SiteViewModel.viewModelScope.launch {
            try {
                component.fetchHistoryList()
            } finally {
                hideProgress()
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()
        .background(MaterialTheme.colors.background)) {
        Column {
            BackRow(modifier = Modifier.align(Alignment.Start), { onClickBack() }) {
                Row(modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically){
                    Text(
                        "历史记录", style = MaterialTheme.typography.h4,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    IconButton(modifier = Modifier.align(Alignment.CenterVertically).padding(end = 20.dp), onClick = {
                        Db.History.deleteAll()
                        component.fetchHistoryList()
                    }) {
                        Row {
                            Icon(Icons.Default.Delete, "delete all", tint = MaterialTheme.colors.onSurface)
                            Text("清空", color = MaterialTheme.colors.onSurface)
                        }
                    }
                }
            }
            val gridState = remember { LazyGridState() }
            LazyVerticalGrid(
                modifier = Modifier.padding(horizontal = 10.dp),
                columns = GridCells.Adaptive(140.dp),
                state = gridState,
                contentPadding = PaddingValues(5.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                userScrollEnabled = true,
            ) {
                items(model.value.historyList) {
                    HistoryItem(Modifier,
                        it, showSite = false, { key ->
                            Db.History.deleteBatch(listOf(key))
                            component.fetchHistoryList()
                        }) {
                        chooseHistory = it
                        showDetailDialog = true
                    }
                }
            }
            VerticalScrollbar(rememberScrollbarAdapter(gridState))
        }
        if (vod != null) {
            DetailDialog(showDetailDialog, vod, chooseHistory?.getSiteKey() ?: "") {
                showDetailDialog = false
                vod = null
                chooseHistory = null
            }
        }
    }
}