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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
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
import com.corner.catvodcore.util.Http
import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.util.M3U8FilterConfig
import com.github.catvod.bean.Doh
import lumentv_compose.composeapp.generated.resources.LumenTV_icon_svg
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

private val log = LoggerFactory.getLogger("SettingsScreen")

@Composable
fun WindowScope.SettingScene(vm: SettingViewModel, config: M3U8FilterConfig, onClickBack: () -> Unit) {
    val model = vm.state.collectAsState()
    var showAboutDialog by remember { mutableStateOf(false) }
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
            // 顶部应用栏
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
                        // 数据目录按钮
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

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp, top = 80.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 广告过滤设置项
            item {
                SettingCard(
                    title = "广告过滤设置",
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
                        var tsNameLenExtend: Int by remember { mutableStateOf(config.value.tsNameLenExtend) }
                        var theExtinfBenchmarkN: Int by remember { mutableStateOf(config.value.theExtinfBenchmarkN) }
                        var violentFilterModeFlag by remember { mutableStateOf(config.value.violentFilterModeFlag) }

                        // 同步配置变化到本地状态
                        LaunchedEffect(config.value.tsNameLenExtend) {
                            tsNameLenExtend = config.value.tsNameLenExtend
                        }
                        LaunchedEffect(config.value.theExtinfBenchmarkN) {
                            theExtinfBenchmarkN = config.value.theExtinfBenchmarkN
                        }
                        LaunchedEffect(config.value.violentFilterModeFlag) {
                            violentFilterModeFlag = config.value.violentFilterModeFlag
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("TS 前缀长度容错值")
                                Text(
                                    text = "${tsNameLenExtend.toInt()} (默认: 1)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Slider(
                                value = tsNameLenExtend.toFloat(),
                                onValueChange = { newValue ->
                                    // 使用四舍五入获得更准确的整数值
                                    val newInt = newValue.roundToInt()
                                    // 确保值在有效范围内（0到5）
                                    val clampedValue = newInt.coerceIn(0, 5)
                                    tsNameLenExtend = clampedValue
                                    config.value = config.value.copy(tsNameLenExtend = clampedValue)
                                    SettingStore.setM3U8FilterConfig(config.value)
                                    showRestartDialog = true
                                },
                                valueRange = 0f..5f,
                                steps = 4 // 修正为5步，产生0-5共6个离散值
                            )
                            Text(
                                text = "用于匹配TS文件名的前缀长度容错。当TS文件名与预期模式不完全匹配时，允许的前缀长度偏差值。设为0表示严格匹配，增大可提高容错能力。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            // 添加分割线
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("EXTINF 基准值")
                                Text(
                                    text = "${theExtinfBenchmarkN.toInt()} (默认: 5)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Slider(
                                value = theExtinfBenchmarkN.toFloat(),
                                onValueChange = { newValue ->
                                    // 使用四舍五入获得更准确的整数值
                                    val newInt = newValue.roundToInt()
                                    // 确保值在有效范围内
                                    val clampedValue = newInt.coerceIn(1, 10)
                                    theExtinfBenchmarkN = newInt
                                    config.value = config.value.copy(theExtinfBenchmarkN = clampedValue)
                                    SettingStore.setM3U8FilterConfig(config.value)
                                    showRestartDialog = true
                                },
                                valueRange = 1f..10f,
                                steps = 8 // 产生 10 个整数档位：1 到 10
                            )
                            Text(
                                text = "相同描述行阈值：用于判断是否进入广告段。若连续相同的 #EXTINF 行数超过此值，将触发广告过滤逻辑。默认值通常为 3~5。若正常内容被误判为广告，可调大；若广告漏过，可调小。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            // 添加分割线
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("暴力拆解模式")
                                Text(
                                    text = if (violentFilterModeFlag) "开启" else "关闭",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = violentFilterModeFlag,
                                onCheckedChange = {
                                    violentFilterModeFlag = it
                                    config.value.violentFilterModeFlag = it
                                    SettingStore.setM3U8FilterConfig(config.value)
                                    showRestartDialog = true
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                            Text(
                                text = "暴力过滤模式：开启后将直接移除所有 #EXT-X-DISCONTINUITY 行（常用于广告插入点）。适用于复杂广告场景，但可能导致正常内容丢失（如节目切换）。仅在普通模式无法过滤广告时启用。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
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
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                                    label = { Text("输入点播源地址") }, // 保留输入框提示
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
                                    modifier = Modifier.height(60.dp).padding(top = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("确定")
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
                        Text(
                            text = "需要配置点播源才能获取到视频内容\n" +
                                    " \n" +
                                    "格式：\n" +
                                    "file://C:\\\\json\\\\config.json \n" +
                                    "或\n" +
                                    "http://example.com/config.json \n" +
                                    "或\n" +
                                    "{\n" +
                                    "  \"spider\": \"jar路径;md5;校验值\",\n" +
                                    "  \"sites\": [\n" +
                                    "    {\n" +
                                    "      \"key\": \"唯一标识\",\n" +
                                    "      \"name\": \"显示名称\",\n" +
                                    "      \"type\": 3,\n" +
                                    "      \"api\": \"接口类名\",\n" +
                                    "      \"searchable\": 0,\n" +
                                    "      \"changeable\": 0,\n" +
                                    "      \"ext\": {}\n" +
                                    "    }\n" +
                                    "  ]\n" +
                                    "}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
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
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            logLevel.forEach { level ->
                                FilterChip(
                                    selected = level == current.value,
                                    leadingIcon = if (current.value == level) {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.Done,
                                                contentDescription = "Done icon",
                                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    } else {
                                        null
                                    },
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
                        Text(
                            text = "日志级别用于记录应用运行时的信息和错误,默认级别为DEBUG;使用DEBUG级别可能会导致日志文件变大",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
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
                                arr[0] = PlayerType.Outie.id
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
                                            PlayerType.Innie.id -> SnackBar.postMsg(
                                                "使用内置播放器",
                                                type = SnackBar.MessageType.INFO
                                            )

                                            PlayerType.Outie.id -> {
                                                // 检查是否选择了外部播放器但路径为空
                                                if (playerSetting.value[1].isBlank()) {
                                                    SnackBar.postMsg(
                                                        "已切换到外部播放器，请配置播放器路径",
                                                        type = SnackBar.MessageType.WARNING
                                                    )
                                                } else {
                                                    SnackBar.postMsg("使用外部播放器", type = SnackBar.MessageType.INFO)
                                                }
                                            }

                                            PlayerType.Web.id -> SnackBar.postMsg(
                                                "使用浏览器播放器",
                                                type = SnackBar.MessageType.INFO
                                            )
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
                        var isPathValid by remember { mutableStateOf(true) }
                        var showPathWarning by remember { mutableStateOf(false) }
                        // 播放器路径输入
                        OutlinedTextField(
                            value = playerSetting.value[1],
                            onValueChange = {
                                isPathValid = it.isNotBlank()
                                SettingStore.setValue(SettingType.PLAYER, "${playerSetting.value.first()}#$it")
                                SiteViewModel.viewModelScope.launch {
                                    if (playerSetting.value.first() == PlayerType.Innie.id) {
                                        if (File(it).exists()) {
                                            VlcJInit.init(true)
                                        }
                                    }
                                }
                                vm.sync()
                                SnackBar.postMsg("播放器路径更新为：$it", type = SnackBar.MessageType.INFO)
                                // 当用户开始输入路径时，隐藏警告
                                showPathWarning = false
                            },
                            label = { Text("播放器路径") },
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = playerSetting.value.first() != PlayerType.Innie.id,
                            isError = !isPathValid || showPathWarning,
                            supportingText = {
                                if (!isPathValid || showPathWarning) {
                                    if (playerSetting.value.first() == PlayerType.Outie.id && playerSetting.value[1].isBlank()) {
                                        Text("请输入外置播放器路径！", color = MaterialTheme.colorScheme.error)
                                    } else {
                                        Text("请输入外置播放器路径！", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        )

                        // 添加一个按钮用于验证路径
                        if (playerSetting.value.first() == PlayerType.Outie.id) {
                            Button(
                                onClick = {
                                    if (playerSetting.value[1].isBlank()) {
                                        showPathWarning = true
                                        SnackBar.postMsg("请先配置外部播放器路径！", type = SnackBar.MessageType.ERROR)
                                    } else {
                                        // 验证路径是否有效
                                        val file = File(playerSetting.value[1])
                                        if (file.exists() && file.canExecute()) {
                                            SnackBar.postMsg("播放器路径有效", type = SnackBar.MessageType.INFO)
                                        } else {
                                            SnackBar.postMsg(
                                                "播放器路径无效或不可执行",
                                                type = SnackBar.MessageType.ERROR
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = playerSetting.value.first() == PlayerType.Outie.id
                            ) {
                                Text("验证播放器路径")
                            }
                        }
                        Text(
                            text = "播放器可配置为内部播放器、外部播放器;如果选择外部播放器,需要配置外置播放器路径才能播放视频",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
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

            // 爬虫搜索词设置项
            item {
                SettingCard(
                    title = "爬虫搜索词设置",
                    icon = Icons.Default.Search
                ) {
                    val crawlerSearchTerms = remember {
                        mutableStateOf(
                            model.value.settingList.getSetting(SettingType.CRAWLER_SEARCH_TERMS)?.value ?: ""
                        )
                    }

                    OutlinedTextField(
                        value = crawlerSearchTerms.value,
                        onValueChange = { newValue ->
                            crawlerSearchTerms.value = newValue
                            SettingStore.setValue(SettingType.CRAWLER_SEARCH_TERMS, newValue)
                            vm.sync()
                        },
                        label = { Text("搜索模式搜索词") },
                        placeholder = { Text("请输入搜索词") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        text = "用于爬虫可用性功能的搜索模式搜索词，默认为“阿甘正传”",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            item {
                SettingCard(
                    title = "DNS over HTTPS 设置",
                    icon = Icons.Default.Security
                ) {
                    val dohEnabled = remember {
                        mutableStateOf(SettingStore.getSettingItem(SettingType.DOH_ENABLED).toBoolean())
                    }
                    val dohServer = remember {
                        mutableStateOf(SettingStore.getSettingItem(SettingType.DOH_SERVER))
                    }
                    val dohServers = Doh.defaultDoh().filter { it.name != "System" } // 过滤掉System选项

                    Column(modifier = Modifier.fillMaxWidth()) {
                        // DoH 启用开关
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (dohEnabled.value) "DoH：开启" else "DoH：关闭",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = dohEnabled.value,
                                onCheckedChange = { enabled ->
                                    dohEnabled.value = enabled
                                    SettingStore.setValue(SettingType.DOH_ENABLED, enabled.toString())
                                    // 应用DoH设置
                                    applyDohSetting(enabled, dohServer.value)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }

                        // DoH 服务器选择（仅在启用时显示）
                        if (dohEnabled.value) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                                Text(
                                    text = "DoH 服务器",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // 服务器选择按钮
                                dohServers.forEach { server ->
                                    RadioButtonRow(
                                        text = server.name,
                                        selected = dohServer.value == server.name,
                                        onClick = {
                                            dohServer.value = server.name
                                            SettingStore.setValue(SettingType.DOH_SERVER, server.name)
                                            // 应用DoH设置
                                            applyDohSetting(true, server.name)
                                        }
                                    )
                                }
                            }
                        }

                        Text(
                            text = "DNS over HTTPS (DoH) 可以提高DNS查询的安全性和隐私性。开启后，DNS查询将通过HTTPS加密传输。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }

            // 重置按钮
            item {
                var showConfirmDialog by remember { mutableStateOf(false) }

                Button(
                    onClick = {
                        showConfirmDialog = true
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

                if (showConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showConfirmDialog = false },
                        title = { Text("确认重置") },
                        text = { Text("您确定要重置所有设置吗？此操作无法撤销。") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    SettingStore.reset()
                                    vm.sync()
                                    GlobalAppState.isDarkTheme.value =
                                        SettingStore.getSettingItem(SettingType.THEME) == "dark"
                                    SnackBar.postMsg("重置设置,重启生效", type = SnackBar.MessageType.INFO)
                                    showConfirmDialog = false
                                }
                            ) {
                                Text("确认")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showConfirmDialog = false }
                            ) {
                                Text("取消")
                            }
                        }
                    )
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
        SnackBar.postMsg("重启生效", type = SnackBar.MessageType.INFO)
        showRestartDialog = false
    }
}

// 单选按钮行组件
@Composable
fun RadioButtonRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
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
            SnackBar.postMsg("点播源地址不可为空", type = SnackBar.MessageType.ERROR)
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
                            text = "1.0.7",
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
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

fun applyDohSetting(enabled: Boolean, serverName: String) {
    if (enabled) {
        val doh = Doh.defaultDoh().find { it.name == serverName }
        doh?.let {
            Http.setDoh(it) // Apply the DoH setting to Http
            SnackBar.postMsg("已启用DoH: $serverName", type = SnackBar.MessageType.INFO)
        }
    } else {
        // 禁用DoH，重置为系统默认DNS
        resetDohSetting()
        SnackBar.postMsg("已禁用DoH", type = SnackBar.MessageType.INFO)
    }
}

fun resetDohSetting() {
    // 通过反射或添加方法来重置DoH设置
    Http.resetDoh()
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