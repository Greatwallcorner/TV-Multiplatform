package com.corner.ui

import SiteViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.WindowScope
import com.corner.bean.*
import com.corner.bean.enums.PlayerType
import com.corner.catvodcore.config.ApiConfig
import com.corner.catvodcore.enum.ConfigType
import com.corner.catvodcore.util.Paths
import com.corner.catvodcore.viewmodel.GlobalAppState.hideProgress
import com.corner.catvodcore.viewmodel.GlobalAppState.showProgress
import com.corner.database.Db
import com.corner.database.entity.Config
import com.corner.init.Init.Companion.initConfig
import com.corner.ui.nav.vm.SettingViewModel
import com.corner.ui.player.vlcj.VlcJInit
import com.corner.ui.scene.*
import com.corner.util.getSetting
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import lumentv_compose.composeapp.generated.resources.Res
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.net.URI
import androidx.compose.runtime.collectAsState
import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.util.M3U8FilterConfig
import lumentv_compose.composeapp.generated.resources.LumenTV_icon_svg
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("SettingsScreen")

@Composable
fun WindowScope.SettingScene(vm: SettingViewModel, config: M3U8FilterConfig, onClickBack: () -> Unit) {
    val model = vm.state.collectAsState()
    var showAboutDialog by remember { mutableStateOf(false) }

    // 收集全局主题状态
    val isDarkTheme by GlobalAppState.isDarkTheme.collectAsState()
    val config = remember { mutableStateOf(SettingStore.getM3U8FilterConfig()) }
    val isAdFilterEnabled by remember { mutableStateOf(SettingStore.isAdFilterEnabled()) }
    var adFilterChecked by remember { mutableStateOf(isAdFilterEnabled) }
    var showRestartDialog by remember { mutableStateOf(false) }
    DisposableEffect("setting") {
        vm.sync()
        onDispose {
            SettingStore.write()
        }
    }

    DisposableEffect(model.value.settingList) {
        log.info("settingList 修改")
        onDispose { }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
            // 顶部应用栏 - 更现代化的设计
            WindowDraggableArea {
                ControlBar(
                    leading = {
                        BackRow(modifier = Modifier.align(Alignment.Start), { onClickBack() }) {
                            Row(
                                modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "设置",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.15.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        // 数据目录按钮 - 修正版本
                        FilledTonalButton(
                            onClick = { Desktop.getDesktop().open(Paths.userDataRoot()) },
                            modifier = Modifier.padding(end = 16.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            elevation = ButtonDefaults.filledTonalButtonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "数据目录",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "数据目录",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                )
            }
        }
        // 设置内容区域 - 使用卡片布局
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp, top = 80.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 广告过滤设置项
            item {
                SettingCard(
                    title = "广告过滤设置(实验性)",
                    icon = Icons.Default.Block
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (adFilterChecked) "广告过滤：开启" else "广告过滤：关闭",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = adFilterChecked,
                            onCheckedChange = {
                                adFilterChecked = it
                                SettingStore.setAdFilterEnabled(it)
                                vm.sync()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    // 仅在广告过滤开启时显示配置项
                    if (adFilterChecked) {
                        var tsNameLenExtend by remember { mutableStateOf(config.value.tsNameLenExtend.toFloat()) }
                        var theExtinfBenchmarkN by remember { mutableStateOf(config.value.theExtinfBenchmarkN.toFloat()) }
                        var violentFilterModeFlag by remember { mutableStateOf(config.value.violentFilterModeFlag) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Text("TS 前缀长度容错值")
                            Slider(
                                value = tsNameLenExtend,
                                onValueChange = {
                                    tsNameLenExtend = it
                                    config.value.tsNameLenExtend = it.toInt()
                                    SettingStore.setM3U8FilterConfig(config.value)
                                    showRestartDialog = true
                                },
                                valueRange = 1f..5f,
                                steps = 4
                            )

                            Text("EXTINF 基准值")
                            Slider(
                                value = theExtinfBenchmarkN,
                                onValueChange = {
                                    theExtinfBenchmarkN = it
                                    config.value.theExtinfBenchmarkN = it.toInt()
                                    SettingStore.setM3U8FilterConfig(config.value)
                                    showRestartDialog = true
                                },
                                valueRange = 1f..10f,
                                steps = 9
                            )

                            Text("启用暴力拆解模式")
                            Switch(
                                checked = violentFilterModeFlag,
                                onCheckedChange = {
                                    violentFilterModeFlag = it
                                    config.value.violentFilterModeFlag = it
                                    SettingStore.setM3U8FilterConfig(config.value)
                                    showRestartDialog = true
                                }
                            )
                        }
                    }
                }
            }
            // 新增主题切换卡片
            item {
                SettingCard(
                    title = "主题设置",
                    icon = Icons.Default.Palette
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isDarkTheme) "当前主题：暗色模式" else "当前主题：亮色模式",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = {
                                GlobalAppState.isDarkTheme.value = it
                                try {
                                    // 保存新的主题状态到 SettingStore
                                    SettingStore.setValue(SettingType.THEME, if (it) "dark" else "light")
                                } catch (e: Exception) {
                                    // 打印错误日志，方便排查问题
                                    e.printStackTrace()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            item {
                SettingCard(title = "点播源配置", icon = Icons.Default.LiveTv) {
                    val focusRequester = remember { FocusRequester() }
                    val isExpand = remember { mutableStateOf(false) }
                    val setting = derivedStateOf { model.value.settingList.getSetting(SettingType.VOD) }
                    val vodConfigList = derivedStateOf { model.value.dbConfigList }

                    // 本地状态管理
                    var textValue by remember { mutableStateOf(setting.value?.value ?: "") }

                    // 同步外部状态变化
                    LaunchedEffect(setting.value?.value) {
                        setting.value?.value?.let {
                            if (textValue != it) textValue = it
                        }
                    }

                    // 初始焦点设置
                    LaunchedEffect(isExpand.value) {
                        if (isExpand.value) {
                            vm.getConfigAll()
                            focusRequester.requestFocus()
                            delay(100) // 稍延迟确保布局稳定
                        }
                    }

                    SettingItemTemplate("地址") {
                        Box(Modifier.fillMaxSize()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                //输入框
                                OutlinedTextField(
                                    value = textValue,
                                    onValueChange = { newValue ->
                                        textValue = newValue
                                        SettingStore.setValue(SettingType.VOD, newValue)
                                        vm.sync()
                                    },
                                    label = { Text("输入点播源地址") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .focusRequester(focusRequester)
                                        .weight(1f)
                                        .onFocusEvent { isExpand.value = it.isFocused },
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Done,
                                        keyboardType = KeyboardType.Uri
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = {
                                        Row {
                                            // 清空按钮
                                            if (textValue.isNotEmpty()) {
                                                IconButton(
                                                    onClick = {
                                                        textValue = ""
                                                        SettingStore.setValue(SettingType.VOD, "")
                                                        vm.sync()
                                                    }
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        "清空",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                            // 粘贴按钮
                                            IconButton(
                                                onClick = {
                                                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                                                    try {
                                                        val text = clipboard.getData(DataFlavor.stringFlavor) as? String
                                                        text?.let {
                                                            textValue = it
                                                            SettingStore.setValue(SettingType.VOD, it)
                                                            vm.sync()
                                                        }
                                                    } catch (e: Exception) {
                                                        log.error("粘贴失败: ${e.message}")
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.ContentPaste,
                                                    "粘贴",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                )

                                // 确定按钮
                                Button(
                                    onClick = { setConfig(textValue) },
                                    modifier = Modifier.height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("确定")
                                }
                            }
                            DropdownMenu(
                                isExpand.value,
                                { isExpand.value = false },
                                modifier = Modifier.fillMaxWidth(0.8f),
                                properties = PopupProperties(focusable = false)
                            ) {
                                vodConfigList.value.forEach {
                                    DropdownMenuItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = { Text(it.url ?: "") },
                                        onClick = {
                                            setConfig(it.url)
                                            isExpand.value = false
                                        }, trailingIcon = {
                                            IconButton(onClick = {
                                                vm.deleteHistoryById(it)
                                            }) {
                                                Icon(Icons.Default.Close, "delete the config")
                                            }
                                        })
                                }
                            }
                        }
                    }
                }
            }

            // 日志级别设置项
            item {
                SettingCard(
                    title = "日志级别",
                    icon = Icons.AutoMirrored.Filled.ListAlt
                ) {
                    val current = derivedStateOf {
                        model.value.settingList.getSetting(SettingType.LOG)?.value ?: logLevel[0]
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        logLevel.forEach { level ->
                            FilterChip(
                                selected = level == current.value,
                                onClick = {
                                    SettingStore.setValue(SettingType.LOG, level)
                                    vm.sync()
                                    SnackBar.postMsg("重启生效", type = SnackBar.MessageType.INFO)
                                },
                                label = { Text(level) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }

            // 播放器设置项
            item {
                SettingCard(
                    title = "播放器设置",
                    icon = Icons.Default.PlayCircle
                ) {
                    val playerSetting = derivedStateOf {
                        val arr = model.value.settingList.getSetting(SettingType.PLAYER)
                            ?.value?.getPlayerSetting()?.toMutableList()
                            ?: mutableListOf(PlayerType.Innie.id, "")

                        if (listOf("true", "false").contains(arr[0])) {
                            if (arr[0].toBoolean()) {
                                arr[0] = PlayerType.Innie.id
                            } else {
                                arr[1] = PlayerType.Outie.id
                            }
                            SettingStore.setValue(SettingType.PLAYER, "${arr.first()}#${arr[1]}")
                        }
                        arr
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 播放器类型选择
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlayerType.entries.filter { it.id != PlayerType.Web.id }.forEach { type ->
                                AssistChip(
                                    onClick = {
                                        SettingStore.setValue(
                                            SettingType.PLAYER,
                                            "${type.id}#${playerSetting.value[1]}"
                                        )
                                        when (type.id) {
                                            PlayerType.Innie.id -> SnackBar.postMsg("使用内置播放器", type = SnackBar.MessageType.INFO)
                                            PlayerType.Outie.id -> SnackBar.postMsg("使用外部播放器 请配置播放器路径", type = SnackBar.MessageType.INFO)
                                            PlayerType.Web.id -> SnackBar.postMsg("使用浏览器播放器", type = SnackBar.MessageType.INFO)
                                        }
                                        vm.sync()
                                    },
                                    label = { Text(type.display) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (playerSetting.value.first() == type.id) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        labelColor = if (playerSetting.value.first() == type.id) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        // 播放器路径输入
                        OutlinedTextField(
                            value = playerSetting.value[1],
                            onValueChange = {
                                SettingStore.setValue(SettingType.PLAYER, "${playerSetting.value.first()}#$it")
                                SiteViewModel.viewModelScope.launch {
                                    if (playerSetting.value.first() == PlayerType.Innie.id) {
                                        if (File(it).exists()) {
                                            VlcJInit.init(true)
                                        }
                                    }
                                }
                                vm.sync()
                            },
                            label = { Text("播放器路径") },
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // 代理设置项
            item {
                SettingCard(
                    title = "代理设置",
                    icon = Icons.Default.Security
                ) {
                    val proxySetting = derivedStateOf {
                        model.value.settingList.getSetting(SettingType.PROXY)
                            ?.value?.parseAsSettingEnable()
                            ?: SettingEnable.Default()
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Switch(
                            checked = proxySetting.value.isEnabled,
                            onCheckedChange = {
                                SettingStore.setValue(SettingType.PROXY, "$it#${proxySetting.value.value}")
                                vm.sync()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )

                        OutlinedTextField(
                            value = proxySetting.value.value,
                            onValueChange = {
                                SettingStore.setValue(SettingType.PROXY, "${proxySetting.value.isEnabled}#$it")
                                vm.sync()
                            },
                            label = { Text("代理地址") },
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                            enabled = proxySetting.value.isEnabled,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // 重置按钮
            item {
                Button(
                    onClick = {
                        SettingStore.reset()
                        vm.sync()
                        SnackBar.postMsg("重置设置 重启生效", type = SnackBar.MessageType.INFO)
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("重置所有设置", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        // 关于按钮 - 悬浮在右下角
        FloatingActionButton(
            onClick = { showAboutDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ) {
            Icon(Icons.Default.Info, "关于")
        }
    }
    if (showAboutDialog) {
        AboutDialog(
            modifier = Modifier
                .fillMaxWidth(0.5f)  // 改为宽度比例
                .fillMaxHeight(0.8f), // 改为高度比例
            showDialog = showAboutDialog,
            onDismiss = { showAboutDialog = false },
            contentPadding = PaddingValues(16.dp)  // 可调整内边距
        )
    }

    // 显示重启提示弹窗
    if (showRestartDialog) {
        Dialog(
            onDismissRequest = { showRestartDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "重启应用后生效",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(
                        onClick = { showRestartDialog = false }
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

// 设置项卡片组件
@Composable
fun SettingCard(
    title: String,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Divider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 1.dp
            )

            content()
        }
    }
}

fun SettingStore.getPlayerSetting(): List<Any> {
    val settingItem = getSettingItem(SettingType.PLAYER.id)
    val split = settingItem.split("#")
    return if (split.size == 1) listOf(false, settingItem) else listOf(split[0].toBoolean(), split[1])
}

@Composable
fun SettingItemTemplate(title: String, content: @Composable () -> Unit) {
    Row(
        Modifier
//            .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(4.dp))
            .padding(start = 20.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 15.dp).align(Alignment.CenterVertically),
            color = MaterialTheme.colorScheme.onBackground
        )
        content()
    }
}

private val logLevel = listOf("INFO", "DEBUG")

enum class SideButtonType {
    LEFT, MID, RIGHT
}

@Composable
fun SideButton(
    choosen: Boolean,
    buttonColors: ButtonColors = ButtonDefaults.buttonColors()
        .copy(disabledContainerColor = MaterialTheme.colorScheme.background),
    type: SideButtonType = SideButtonType.MID,
    text: String,
    onClick: (String) -> Unit
) {
    val textColor = if (choosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
    Text(text = text, modifier = Modifier.clickable { onClick(text) }
        .defaultMinSize(50.dp)
        .drawWithCache {
//            val width = size.width * 1.1f
//            val height = size.height * 1.1f
            val width = size.width + 5
            val height = size.height + 5
            val color = if (choosen) buttonColors.containerColor else buttonColors.disabledContainerColor

            onDrawBehind {
                val rectOffset = when (type) {
                    SideButtonType.LEFT -> Offset(height / 2, 0f)
                    SideButtonType.MID -> Offset.Zero
                    SideButtonType.RIGHT -> Offset.Zero
                }
                if (type == SideButtonType.LEFT) {
                    drawCircle(
                        color = color,
                        radius = height / 2,
                        center = Offset(height / 2, height / 2),
                        style = Fill
                    )
                }
                val rectSize = Size(width - height / 2, height)
                drawRect(
                    color = color,
                    topLeft = rectOffset,
                    size = rectSize,
                    style = Fill,
                )
                if (type == SideButtonType.RIGHT) {
                    drawCircle(
                        color = color,
                        radius = height / 2,
                        center = Offset(size.width - height / 2, height / 2),
                        style = Fill
                    )
                }
            }
        }, textAlign = TextAlign.Center, color = textColor)
}

fun setConfig(textFieldValue: String?) {
    showProgress()
    SiteViewModel.viewModelScope.launch {
        if (textFieldValue == null || textFieldValue == "") {
            SnackBar.postMsg("不可为空", type = SnackBar.MessageType.ERROR)
            return@launch
        }
        SettingStore.setValue(SettingType.VOD, textFieldValue)
        val config = Db.Config.find(textFieldValue, ConfigType.SITE.ordinal.toLong()).firstOrNull()
        if (config == null) {
            Db.Config.save(
                Config(
                    type = ConfigType.SITE.ordinal.toLong(),
                    url = textFieldValue
                )
            )
        } else {
            Db.Config.updateUrl(config.id, textFieldValue)
        }

        ApiConfig.api.cfg = Db.Config.find(textFieldValue, ConfigType.SITE.ordinal.toLong()).firstOrNull()
        initConfig()
    }.invokeOnCompletion {
        hideProgress()
    }
}

@Composable
fun AboutDialog(
    modifier: Modifier = Modifier,
    showDialog: Boolean,
    onDismiss: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(24.dp)
) {
    var visible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .heightIn(min = 400.dp, max = 600.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)  // 添加垂直滚动支持
                    .padding(contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with avatar
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically(
                        initialOffsetY = { -40 },
                        animationSpec = tween(500)
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(Res.drawable.LumenTV_icon_svg),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "LumenTV Compose",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            "1.0.4",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                // Author section - 保持水平滚动
                AboutSection(
                    title = "开发团队",
                    items = listOf(
                        AboutItem(
                            name = "Clevebitr",
                            role = "该版本开发者",
                            link = "https://github.com/clevebitr",
                            icon = Icons.Default.Person
                        ),
                        AboutItem(
                            name = "Greatwallcorner",
                            role = "主要开发者",
                            link = "https://github.com/Greatwallcorner",
                            icon = Icons.Default.Person
                        ),
                        AboutItem(
                            name = "贡献者",
                            role = "开源社区",
                            link = "https://github.com/Greatwallcorner/TV-Multiplatform/graphs/contributors",
                            icon = Icons.Default.Group
                        )
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Links section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = { openBrowser("https://t.me/tv_multiplatform") },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                            Text("加入原项目Telegram群组")
                        }
                    }
                    FilledTonalButton(
                        onClick = { openBrowser("https://github.com/Clevebitr/TV-Multiplatform") },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null)
                            Text("查看该版本源代码")
                        }
                    }

                    FilledTonalButton(
                        onClick = { openBrowser("https://github.com/Greatwallcorner/TV-Multiplatform") },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null)
                            Text("查看原项目源代码")
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(showDialog) {
        if (showDialog) {
            delay(100)
            visible = true
        } else {
            visible = false
        }
    }
}

// New data class for author information
data class AboutItem(
    val name: String,
    val role: String,
    val link: String,
    val icon: ImageVector
)

// New composable for author section
@Composable
fun AboutSection(
    title: String,
    items: List<AboutItem>
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 垂直按钮列表
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                ElevatedButton(
                    onClick = { openBrowser(item.link) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 左侧头像和文本
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = item.role,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // 右侧链接图标
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "打开链接",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

//@Preview
//@Composable
//fun previewAboutDialog() {
//    AppTheme {
//        AboutDialog(Modifier, true) {}
//    }
//}

fun openBrowser(url: String) {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(URI(url))
    }
}

//@Composable
//fun AboutItem(title: String, modifier: Modifier, content: @Composable (Modifier) -> Unit) {
//    Row(modifier.padding(vertical = 5.dp, horizontal = 15.dp)) {
//        Text(
//            title,
//            style = MaterialTheme.typography.titleMedium.copy(
//                color = MaterialTheme.colorScheme.onSurface,
//                fontWeight = FontWeight.Bold
//            ),
//            modifier = Modifier.align(Alignment.CenterVertically).padding(end = 5.dp)
//        )
//        content(Modifier.align(Alignment.CenterVertically))
//    }
//}

//@Composable
//@Preview
//fun SettingItem() {
//    AppTheme(useDarkTheme = false) {
//        SettingItem(
//            Modifier,
//            "点播", "PeopleInSpaceTheme"
//        ) {
//
//        }
//    }
//}