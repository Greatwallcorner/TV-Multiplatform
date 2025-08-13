package com.corner.ui

import SiteViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.WindowScope
import com.corner.bean.SettingStore
import com.corner.bean.SettingType
import com.corner.bean.enums.PlayerType
import com.corner.bean.getPlayerSetting
import com.corner.catvod.enum.bean.Vod
import com.corner.catvod.enum.bean.Vod.Companion.isEmpty
import com.corner.catvodcore.bean.Episode
import com.corner.catvodcore.util.Utils
import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.catvodcore.viewmodel.GlobalAppState.hideProgress
import com.corner.catvodcore.viewmodel.GlobalAppState.showProgress
import com.corner.ui.nav.data.DetailScreenState
import com.corner.ui.nav.data.DialogState
import com.corner.ui.nav.data.DialogState.isSpecialVideoLink
import com.corner.ui.nav.vm.DetailViewModel
import com.corner.ui.scene.*
import com.corner.ui.video.QuickSearchItem
import com.corner.util.BrowserUtils
import com.corner.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("DetailScreen")

var userTriggered by mutableStateOf(false)
fun onUserSelectEpisode() {
    userTriggered = true
}

@Composable
fun WindowScope.DetailScene(vm: DetailViewModel, onClickBack: () -> Unit) {
    val model by vm.state.collectAsState()  // 自动响应状态更新
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    val detail = model.detail
    val controller = rememberUpdatedState(vm.controller)
    val isFullScreen = GlobalAppState.videoFullScreen.collectAsState()
    val videoHeight = derivedStateOf { if (isFullScreen.value) 1f else 0.6f }
    val videoWidth = derivedStateOf { if (isFullScreen.value) 1f else 0.7f }

    //监听isLoading, 显示加载动画
    LaunchedEffect(model.isLoading) {
        if (model.isLoading) {
            showProgress()
        } else {
            hideProgress()
        }
    }

    // 初始化 BrowserUtils
    LaunchedEffect(Unit) {
        BrowserUtils.init(vm)
    }

    // 监听isFullScreen, 非全屏时请求焦点
    LaunchedEffect(isFullScreen.value) {
        if (!isFullScreen.value) {
            focus.requestFocus()
        }
    }

    var showWebSocketDisconnected by remember { mutableStateOf(false) }
    var localShowPngDialog by remember { mutableStateOf(DialogState.showPngDialog) }
    var localCurrentM3U8Url by remember { mutableStateOf(DialogState.currentM3U8Url) }

// 监听 DialogState 中的状态变化
    LaunchedEffect(DialogState.showPngDialog, DialogState.currentM3U8Url) {
//        log.debug("DialogState.showPngDialog:{}",DialogState.showPngDialog)
        localShowPngDialog = DialogState.showPngDialog
        localCurrentM3U8Url = DialogState.currentM3U8Url
    }

    // 监听 WebSocket 连接状态
    LaunchedEffect(BrowserUtils.webSocketConnectionState) {
        BrowserUtils.webSocketConnectionState.collect { isConnected ->
            showWebSocketDisconnected = !isConnected
        }
    }

    DisposableEffect(Unit) {
        //进入界面时加载数据
        scope.launch {
            vm.load()
        }
        onDispose {
            //重置播放器状态
            vm.clear()
            if (localShowPngDialog) {
                //关闭websocket服务
                BrowserUtils.cleanup()
            }
        }
    }

    val internalPlayer = derivedStateOf {
        val playerSetting =
            SettingStore.getSettingItem(SettingType.PLAYER).getPlayerSetting(detail.site?.playerType)
        playerSetting.first() == PlayerType.Innie.id
    }

    // 提取重复的 UI 组件
    @Composable
    fun NoPlayerContent(message: String, subtitle: String) {
        Box(
            Modifier.fillMaxWidth(videoWidth.value).fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp))
                .focusable() // 确保可获取焦点
                .focusRequester(focus)
        ) {
            emptyShow(
                modifier = Modifier.align(Alignment.Center),
                title = message,
                subtitle = subtitle,
                onRefresh = {
                    scope.launch {
                        vm.load()
                    }
                },
                showRefresh = false
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier) {
            if (!isFullScreen.value) {
                WindowDraggableArea(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    ControlBar(
                        leading = {
                            BackRow(modifier = Modifier.align(Alignment.Start), { onClickBack() }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(0.7f),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ToolTipText(
                                        detail.vodName ?: "",
                                        textStyle = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                }
                            }
                        }, actions = {
                            IconButton(
                                onClick = {
                                    scope.launch {

                                        /*
                                         * 不要使用controller.release()方法，释放资源会为isReleased = true，
                                         * vm.quickSearch()在完成任务时会调用loadDetail函数，
                                         * 加载完成后会调用setDetail函数，最后会调用startPlay()，
                                         * 但是controller.isReleased为true，导致无法播放
                                         * 传入releaseController = false时不释放播放器资源
                                         * */

                                        vm.clear(false)
                                        vm.quickSearch()
                                        SnackBar.postMsg("重新加载", type = SnackBar.MessageType.INFO)
                                    }
                                },
                                enabled = !model.isLoading,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            ) {
                                // 更流畅的动画配置
                                val rotation by animateFloatAsState(
                                    targetValue = if (model.isLoading) 360f else 0f,
                                    animationSpec = if (model.isLoading) {
                                        infiniteRepeatable(
                                            animation = tween(1000, easing = LinearEasing),
                                            repeatMode = RepeatMode.Restart
                                        )
                                    } else {
                                        tween(300, easing = FastOutSlowInEasing)
                                    },
                                    label = "refresh_rotation"
                                )

                                // 颜色过渡动画
                                val iconTint by animateColorAsState(
                                    targetValue = if (!model.isLoading)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    animationSpec = tween(300),
                                    label = "icon_tint"
                                )

                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = "刷新数据",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .rotate(rotation),
                                    tint = iconTint
                                )
                            }
                        })
                }
            }
            val mrl = derivedStateOf { model.currentPlayUrl }
//            log.debug("WebSocket 连接状态：{}",BrowserUtils.webSocketConnectionState.value)
            // 添加顶栏通知
            if (isSpecialVideoLink && showWebSocketDisconnected) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    // 顶栏通知
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(start = 16.dp, end = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "WebSocket连接已断开，请使用Web播放器",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(
                                onClick = { showWebSocketDisconnected = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "关闭通知",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    if (isSpecialVideoLink && showWebSocketDisconnected) {
                        TopEmptyShow(
                            title = "当前播放器无法播放",
                            subtitle = "请使用 Web 播放器；点击选集按钮重新进入浏览器播放，或点击刷新重试",
                            onRefresh = { scope.launch { vm.load() } },
                            modifier = Modifier
                                .height(50.dp)
                                .fillMaxWidth(),
                            showIcon = false,
                            buttonAlignment = ButtonAlignment.RIGHT
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxHeight(videoHeight.value)
                    .padding(start = if (isFullScreen.value) 0.dp else 16.dp),//全屏取消左侧缩进
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (internalPlayer.value) {
                    // 检查用户是否选择在浏览器打开，若选择则不显示对话框
                    if (localShowPngDialog && !DialogState.userChoseOpenInBrowser) {
                        PngFoundDialog(
                            m3u8Url = localCurrentM3U8Url,
                            Text = "在当前播放的m3u8文件中，检测到了特殊链接，是否跳转到浏览器播放？",
                            onDismiss = {
                                localShowPngDialog = false
                                DialogState.dismissPngDialog()
                            },
                            onOpenInBrowser = {
                                // 获取当前选中的剧集
                                val currentEpisode = model.detail.subEpisode.find { it.activated }
                                val episodeName = model.detail.vodName ?: ""
                                val episodeNumber = currentEpisode?.number ?: 0
                                log.debug("Name is {},Number is {}", episodeName, episodeNumber)
                                BrowserUtils.openBrowserWithHtml(localCurrentM3U8Url, episodeName, episodeNumber)
                                localShowPngDialog = false
                                DialogState.dismissPngDialog()
                            },
                            vm
                        )
                        NoPlayerContent(message = "正在 Web 播放器中播放", subtitle = "请使用 Web 播放器")
                    } else if (!DialogState.userChoseOpenInBrowser) {
                        if (!isSpecialVideoLink) {
                            Player(
                                mrl.value,
                                controller.value,
                                Modifier
                                    .fillMaxWidth(videoWidth.value)
                                    .focusable()
                                    .focusRequester(focus),
                                vm,
                                focusRequester = focus
                            )
                        } else {
                            NoPlayerContent(message = "需要使用 Web 播放器播放", subtitle = "请使用 Web 播放器")
                        }
                    } else {
                        NoPlayerContent(message = "正在 Web 播放器中播放", subtitle = "请使用 Web 播放器")
                    }
                } else {
                    NoPlayerContent(message = "正在外部播放器中播放", subtitle = "请使用外部播放器")
                }
                AnimatedVisibility(!isFullScreen.value, modifier = Modifier.fillMaxSize().padding(end = 16.dp)) {
                    val detail = model.detail
                    val hasEpisodes = detail.subEpisode.isNotEmpty()

                    if (hasEpisodes) {
                        EpChooser(
                            vm, Modifier.fillMaxSize().background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(4.dp)
                            ).padding(horizontal = 5.dp)
                        )
                    } else {
                        emptyShow(
                            modifier = Modifier.fillMaxSize(),
                            title = "暂无选集信息",
                            subtitle = "请稍后重试或刷新",
                            showRefresh = false
                        )
                    }
                }
            }
            AnimatedVisibility(!isFullScreen.value) {
                val searchResultList = derivedStateOf { model.quickSearchResult.toList() }

                // 外层容器
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. 固定搜索结果列（30%宽度）
                    if (searchResultList.value.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.3f)
                        ) {
                            quickSearchResult(model, searchResultList, vm)
                        }
                    }
                    // 2. 可滚动内容列（70%宽度）
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()) // 垂直滚动
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(1.dp),  // 统一内边距
                            horizontalArrangement = Arrangement.spacedBy(32.dp)  // 列间距
                        ) {
                            if (model.detail.isEmpty()) {
                                TopEmptyShow(
                                    title = "当前源不可用",
                                    subtitle = "或加载缓慢，请刷新重试",
                                    onRefresh = { scope.launch { vm.load() } },
                                    modifier = Modifier.fillMaxWidth().fillMaxHeight().background(
                                        MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                    buttonAlignment = ButtonAlignment.LEFT
                                )
                            } else {
                                // 左侧视频信息区域 (占50%宽度)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    VodInfo(detail)
                                }

                                // 右侧控制区域 (占50%宽度)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    // 清晰度选择
                                    val urls = rememberUpdatedState(vm.state.value.currentUrl)
                                    val showUrl = derivedStateOf { (urls.value?.values?.size ?: 0) > 1 }
                                    if (showUrl.value) {
                                        Column(
                                            modifier = Modifier
                                                .padding(vertical = 12.dp)
                                                .fillMaxWidth()
                                        ) {
                                            // 标题文本
                                            Text(
                                                text = "画质选择",
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                                            )

                                            // 清晰度选项列表
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                itemsIndexed(urls.value?.values ?: listOf()) { i, item ->
                                                    val isSelected = i == urls.value?.position!!

                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isSelected)
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                        else
                                                            MaterialTheme.colorScheme.surfaceVariant,
                                                        border = BorderStroke(
                                                            width = if (isSelected) 1.5.dp else 1.dp,
                                                            color = if (isSelected)
                                                                MaterialTheme.colorScheme.primary
                                                            else
                                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                                        ),
                                                        onClick = {
                                                            vm.chooseLevel(
                                                                urls.value?.copy(position = i),
                                                                item.v
                                                            )
                                                        },
                                                        modifier = Modifier
                                                            .height(40.dp)
                                                            .widthIn(min = 80.dp)
                                                    ) {
                                                        Box(
                                                            contentAlignment = Alignment.Center,
                                                            modifier = Modifier.padding(horizontal = 16.dp)
                                                        ) {
                                                            Text(
                                                                text = item.n ?: "选项 ${i + 1}",
                                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                                ),
                                                                color = if (isSelected)
                                                                    MaterialTheme.colorScheme.primary
                                                                else
                                                                    MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    // 线路选择
                                    Flags(scope, vm)

                                    // 底部留白
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        //全屏选集弹窗
        if (isFullScreen.value && vm.state.value.showEpChooserDialog) {
            Dialog(
                onDismissRequest = { vm.showEpChooser() }, // 这是点击外部关闭的关键
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnClickOutside = true // 明确启用点击外部关闭
                )
            ) {
                // 1. 透明点击层 - 确保覆盖整个屏幕
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                vm.showEpChooser() // 直接处理点击事件
                            }
                        },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    // 2. 内容区域 - 阻止事件冒泡
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 400.dp)
                            .fillMaxHeight(0.8f)
                            .padding(end = 16.dp)
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    // 空实现，阻止事件冒泡
                                }
                            },
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            bottomStart = 16.dp
                        ),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 8.dp
                    ) {
                        EpChooser(
                            vm,
                            Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

/*
@Composable
private fun QualitySelector(vm: DetailViewModel) {
    val urls = rememberUpdatedState(vm.state.value.currentUrl)
    val showUrl =  derivedStateOf { (urls.value?.values?.size ?: 0) > 1 }

    if (showUrl.value) {

    }
}
*/
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Flags(
    scope: CoroutineScope,
    vm: DetailViewModel,
) {
    val state by vm.state.collectAsState()
    val detail by remember { derivedStateOf { state.detail } }
    val selectedFlagId by remember { derivedStateOf { detail.currentFlag?.flag } }

    Column(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = "线路选择",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                val lazyListState = rememberLazyListState()
                val hasFlags by remember { derivedStateOf { detail.vodFlags.isNotEmpty() } }

                if (hasFlags) {
                    LazyRow(
                        state = lazyListState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(
                            items = detail.vodFlags,
                            key = { it.flag!! }
                        ) { flag ->
                            val isSelected = flag.flag == selectedFlagId
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                ),
                                onClick = {
                                    vm.chooseFlag(state.detail, flag)
                                },
                                modifier = Modifier
                                    .height(40.dp)
                                    .widthIn(min = 88.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = flag.show ?: "线路",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold
                                            else FontWeight.Normal
                                        ),
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    val showScrollbar by remember {
                        derivedStateOf {
                            lazyListState.layoutInfo.totalItemsCount >
                                    lazyListState.layoutInfo.visibleItemsInfo.size
                        }
                    }

                    if (showScrollbar) {
                        HorizontalScrollbar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 12.dp),
                            adapter = rememberScrollbarAdapter(lazyListState),
                            style = ScrollbarStyle(
                                minimalHeight = 4.dp,
                                thickness = 4.dp,
                                shape = RoundedCornerShape(3.dp),
                                hoverDurationMillis = 300,
                                unhoverColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                hoverColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun quickSearchResult(
    detail: DetailScreenState, searchResultList: State<List<Vod>>, component: DetailViewModel
) {
    if (detail.quickSearchResult.isNotEmpty()) {
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
                            log.debug("开始加载新内容...")
                            component.loadDetail(it)
                            log.debug("加载新内容完毕...")
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
    val typography = MaterialTheme.typography
    MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        // 1. 基本信息行（站源/年份/地区/分类）
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            detail?.site?.name?.takeIf { it.isNotBlank() }?.let { siteName ->
                InfoChip("站源: $siteName")
            }

            detail?.vodYear?.takeIf { it.isNotBlank() }?.let { year ->
                InfoChip(year)
            }

            listOfNotNull(
                detail?.vodArea,
                detail?.cate,
                detail?.typeName
            ).filter { it.isNotBlank() }
                .forEach { info ->
                    InfoChip(info)
                }
        }

        // 2. 导演信息 - 只在有数据时显示
        detail?.vodDirector?.takeIf { it.isNotBlank() }?.let { director ->
            LabeledText(
                label = "导演",
                content = director,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        // 3. 演员信息 - 只在有数据时显示
        detail?.vodActor?.takeIf { it.isNotBlank() }?.let { actor ->
            LabeledText(
                label = "演员",
                content = actor,
                modifier = Modifier.padding(top = 8.dp),
                maxLines = 2
            )
        }

        // 4. 简介 - 支持展开/收起
        detail?.vodContent?.takeIf { it.isNotBlank() }?.let { content ->
            ExpandableDescription(
                label = "简介",
                content = content.trim(),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// 新增可展开描述组件
@Composable
private fun ExpandableDescription(
    label: String,
    content: String,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 3
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    Column(modifier) {
        Text(
            text = "$label：",
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            ),
            modifier = Modifier.padding(bottom = 2.dp)
        )

        Box {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLines,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayoutResult ->
                    if (textLayoutResult.hasVisualOverflow) {
                        showButton = true
                    }
                }
            )
        }

        if (showButton) {
            TextButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = if (isExpanded) "收起" else "展开",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


@Composable
private fun InfoChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LabeledText(
    label: String,
    content: String?,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium
) {
    Column(modifier) {
        Text(
            text = "$label：",
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            ),
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = content ?: "无",
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpChooser(vm: DetailViewModel, modifier: Modifier) {
    val model = vm.state.collectAsState()
    val detail = rememberUpdatedState(model.value.detail)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, end = 10.dp, bottom = 8.dp, start = 8.dp)
    ) {
        // 标题行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "选集",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f)
            )

            val show by remember { derivedStateOf { detail.value.currentFlag.episodes.isNotEmpty() } }
            if (show) {
                Text(
                    text = "共${detail.value.currentFlag.episodes.size}集",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )
            }
        }

        // 剧集批次选择（仅当剧集数量>15时显示）
        val epSize by remember { derivedStateOf { detail.value.currentFlag.episodes.size } }
        if (epSize > 15) {
            val scrollState = rememberLazyListState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                LazyRow(
                    state = scrollState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items((0 until epSize step Constants.EpSize).toList()) { i ->
                        RatioBtn(
                            selected = detail.value.currentTabIndex == (i / Constants.EpSize),
                            onClick = { vm.chooseEpBatch(i) },
                            text = buildString {
                                append(i + 1)
                                append("-")
                                append(minOf(i + Constants.EpSize, epSize))
                            },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }

                HorizontalScrollbar(
                    adapter = rememberScrollbarAdapter(scrollState),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter),
                    style = LocalScrollbarStyle.current.copy(
                        unhoverColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        hoverColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
            }
        }
        val uriHandler = LocalUriHandler.current
        val epList by remember { derivedStateOf { detail.value.subEpisode } }
        val gridState = rememberLazyGridState()

        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // 网格列表（占满宽度减去滚动条宽度）
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                state = gridState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp), // 为滚动条预留空间
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                items(epList, key = { it.url + it.number }) { episode ->
                    EpisodeItem(
                        isSelected = episode.url == vm.currentSelectedEpUrl.value,
                        episode = episode,
                        onSelect = {
                            vm.chooseEp(it) { uriHandler.openUri(it) }
                            onUserSelectEpisode()
                            DialogState.resetBrowserChoice()
                        },
                        isLoading = episode.activated && vm.videoLoading.value,
                        modifier = Modifier.fillMaxWidth() // 关键：确保条目填满宽度
                    )
                }
            }

            // 滚动条（固定在右侧）
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(gridState),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(8.dp),
                style = LocalScrollbarStyle.current.copy(
                    unhoverColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    hoverColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpisodeItem(
    isSelected: Boolean, // 由父组件计算选中状态
    episode: Episode,
    onSelect: (Episode) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    TooltipArea(
        tooltip = {
            Surface(
                modifier = Modifier.shadow(8.dp),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Text(
                    text = episode.name,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        delayMillis = 500
    ) {
        RatioBtn(
            text = episode.name,
            onClick = { onSelect(episode) },
            selected = isSelected || episode.activated,
            loading = isLoading,
            tag = {
                if (Utils.isDownloadLink(episode.url)) {
                    Pair(true, "下载")
                } else {
                    Pair(false, "")
                }
            },
            modifier = Modifier.height(48.dp).fillMaxWidth(),
            enableTooltip = false
        )
    }
}
/*
@androidx.compose.desktop.ui.tooling.preview.Preview
@Composable
fun previewEmptyShow() {
    AppTheme {
        emptyShow(onRefresh = { println("ddd") })
    }
}
 */
