package com.corner.ui

import AppTheme
import SiteViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideIn
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import com.corner.init.initConfig
import com.corner.ui.nav.vm.SettingViewModel
import com.corner.ui.player.vlcj.VlcJInit
import com.corner.ui.scene.*
import com.corner.util.getSetting
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import tv_multiplatform.composeapp.generated.resources.Res
import tv_multiplatform.composeapp.generated.resources.avatar
import java.awt.Desktop
import java.io.File
import java.net.URI

@Composable
fun WindowScope.SettingScene(vm: SettingViewModel, onClickBack: () -> Unit) {
    val model = vm.state.collectAsState()
    var showAboutDialog by remember { mutableStateOf(false) }

    DisposableEffect("setting") {
        vm.sync()
        onDispose {
            SettingStore.write()
        }
    }

    DisposableEffect(model.value.settingList) {
        println("settingList 修改")
        onDispose { }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            WindowDraggableArea {
                ControlBar(leading = {
                    BackRow(Modifier.align(Alignment.Start), onClickBack = { onClickBack() }) {
                        Row(
                            Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "设置",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }, actions = {
                    OutlinedButton(
                        onClick = { Desktop.getDesktop().open(Paths.userDataRoot()) },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text("打开用户数据目录")
                    }
                })
            }
            LazyColumn(contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                item {
                    val focusRequester = remember { FocusRequester() }
                    val isExpand = remember { mutableStateOf(false) }
                    val setting = derivedStateOf { model.value.settingList.getSetting(SettingType.VOD) }
                    val vodConfigList = derivedStateOf { model.value.dbConfigList }
                    LaunchedEffect(isExpand.value) {
                        if (isExpand.value) {
                            vm.getConfigAll()
                            focusRequester.requestFocus()
                        }
                    }
                    val label = derivedStateOf { setting.value?.label ?: "" }
                    val value = derivedStateOf { setting.value?.value ?: "" }
                    SettingItemTemplate(label.value) {
                        Box(Modifier.fillMaxSize()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TextField(
                                    value = value.value,
                                    onValueChange = {
                                        SettingStore.setValue(SettingType.VOD, it)
                                        focusRequester.requestFocus()
                                        vm.sync()
                                    },
                                    maxLines = 1,
                                    enabled = true,
                                    modifier = Modifier.focusRequester(focusRequester)
                                        .fillMaxHeight(0.6f)
                                        .weight(0.9f)
                                        .align(Alignment.CenterVertically)
                                        .clip(RoundedCornerShape(5.dp))
                                        .onFocusEvent {
                                            isExpand.value = it.isFocused
                                        }
                                )
                                Button(
                                    onClick = {
                                        setConfig(setting.value!!.value)
                                    },
                                    modifier = Modifier.weight(0.1f)
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
                item {
                    SettingItemTemplate("日志") {
                        val current =
                            derivedStateOf { model.value.settingList.getSetting(SettingType.LOG)?.value ?: logLevel[0] }
                        SingleChoiceSegmentedButtonRow {
                            logLevel.forEachIndexed { index, label ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = logLevel.size
                                    ),
                                    onClick = {
                                        SettingStore.setValue(SettingType.LOG, label)
                                        vm.sync()
                                        SnackBar.postMsg("重启生效")
                                    },
                                    selected = label == current.value,
                                    label = { Text(label) }
                                )
                            }

                        }
//                        LogButtonList(component) {
//                            SettingStore.setValue(SettingType.LOG, it)
//                            component.sync()
//                            SnackBar.postMsg("重启生效")
//                        }
                    }
                }
                item {
                    SettingItemTemplate("播放器") {
                        val playerSetting = derivedStateOf {
                            val arr = model.value.settingList.getSetting(SettingType.PLAYER)?.value?.getPlayerSetting()?.toMutableList() ?: mutableListOf(PlayerType.Innie.id, "")
//                            val arr = model.value.settingList.getSetting(SettingType.PLAYER)?.value?.split("#")
//                                ?.toMutableList() ?: mutableListOf(PlayerType.Innie.id, "")
                            if (listOf("true", "false").contains(arr[0])) {
                                if (arr[0].toBoolean()) {
                                    arr[0] = PlayerType.Innie.id
                                } else {
                                    arr[1] = PlayerType.Outie.id
                                }
                                SettingStore.setValue(
                                    SettingType.PLAYER,
                                    "${arr.first()}#${arr[1]}"
                                )
                            }
                            arr
                        }
                        SingleChoiceSegmentedButtonRow {
                            PlayerType.entries.filter { i -> i.id != PlayerType.Web.id }.forEach { type ->
                                SegmentedButton(
                                    playerSetting.value.first() == type.id,
                                    onClick = {
                                        SettingStore.setValue(
                                            SettingType.PLAYER,
                                            "${type.id}#${playerSetting.value[1]}"
                                        )
                                        when (type.id) {
                                            PlayerType.Innie.id -> SnackBar.postMsg("使用内置播放器")
                                            PlayerType.Outie.id -> SnackBar.postMsg("使用外部播放器 请配置播放器路径")
                                            PlayerType.Web.id -> SnackBar.postMsg("使用浏览器播放器")
                                        }
                                        vm.sync()
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = type.ordinal,
                                        count = PlayerType.entries.size-1
                                    ),
                                    label = { Text(type.display) },
                                )
                            }


                        }
                        TextField(
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
                            maxLines = 1,
                            enabled = true,
                            modifier = Modifier
                                .fillMaxHeight(0.8f)
                                .fillMaxWidth()
                                .padding(start = 8.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                }
                item {
                    SettingItemTemplate("代理") {
                        val proxySetting = derivedStateOf {
                            model.value.settingList.getSetting(SettingType.PROXY)?.value?.parseAsSettingEnable()
                                ?: SettingEnable.Default()
                        }
                        Row {
                            Switch(proxySetting.value.isEnabled, onCheckedChange = {
                                SettingStore.setValue(SettingType.PROXY, "$it#${proxySetting.value.value}")
                                vm.sync()
                            })
                            Spacer(Modifier.width(5.dp))
                            TextField(proxySetting.value.value, onValueChange = {
                                SettingStore.setValue(SettingType.PROXY, "${proxySetting.value.isEnabled}#$it")
                                vm.sync()
                            }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                item {
                    Box(Modifier.fillMaxSize().padding(top = 10.dp)) {
                        ElevatedButton(
                            onClick = {
                                SettingStore.reset()
                                vm.sync()
                                SnackBar.postMsg("重置设置 重启生效")
                            }, Modifier.fillMaxWidth(0.8f)
                                .align(Alignment.Center)
                        ) {
                            Text("重置")
                        }
                    }
                }
            }
        }
        Surface(Modifier.align(Alignment.BottomCenter).padding(bottom = 15.dp)) {
            HoverableText("关于", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)) {
                showAboutDialog = true
            }
        }
        AboutDialog(Modifier.fillMaxSize(0.4f), showAboutDialog) { showAboutDialog = false }
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

//@Composable
//fun LogButtonList(component: DefaultSettingComponent, onClick: (String) -> Unit) {
//    val model = component.model.subscribeAsState()
//    val current = derivedStateOf { model.value.settingList.getSetting(SettingType.LOG)?.value ?: logLevel[0] }
//    Box(
//        Modifier.padding(horizontal = 10.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Row {
//            logLevel.forEachIndexed { i, t ->
//                when (i) {
//                    0 -> {
//                        SideButton(current.value == t, text = t, type = SideButtonType.LEFT) {
//                            onClick(it)
//                        }
//                    }
//
//                    logLevel.size - 1 -> {
//                        SideButton(current.value == t, text = t, type = SideButtonType.RIGHT) {
//                            onClick(it)
//
//                        }
//                    }
//
//                    else -> {
//                        SideButton(current.value == t, text = t) {
//                            onClick(it)
//                        }
//                    }
//                }
//            }
//        }
//    }
//}

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

@Preview
@Composable
fun previewSideButton() {
    AppTheme {
        Row(Modifier.fillMaxSize()) {
//            LogButtonList(Modifier) {}
//            SideButton(true, text = "test12312", type = SideButtonType.LEFT) {}
//
//            SideButton(false, text = "test1j计划熊㩐动甮的", type = SideButtonType.RIGHT) {}
        }
//        Column(Modifier.fillMaxSize()) {
//            SideButton(Color.Blue, false, "test1") {}
//        }
    }
}

@Preview
@Composable
fun previewLogButtonList() {
    AppTheme {
//        LogButtonList(Modifier) {}
    }
}

fun setConfig(textFieldValue: String?) {
    showProgress()
    SiteViewModel.viewModelScope.launch {
        if (textFieldValue == null || textFieldValue == "") {
            SnackBar.postMsg("不可为空")
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
fun AboutDialog(modifier: Modifier, showAboutDialog: Boolean, onClose: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    Dialog(
        modifier, showAboutDialog,
        onClose = {
            visible = false
            onClose()
        }) {
        LaunchedEffect(Unit) {
            delay(500)
            visible = true
        }
        Box(modifier.padding(20.dp).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    modifier = Modifier.fillMaxWidth(),
                    visible = visible,
                    enter = fadeIn() + slideIn(animationSpec = tween(800), initialOffset = { i -> IntOffset(0, -20) })
                ) {
                    Column {
                        Image(
                            painter = painterResource(Res.drawable.avatar),
                            contentDescription = "avatar",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.padding(8.dp)
                                .size(100.dp)
                                .align(Alignment.CenterHorizontally)
                                .clip(RoundedCornerShape(50))
                        )
                        AboutItem("作者", Modifier.align(Alignment.CenterHorizontally)) {
                            HoverableText("Greatwallcorner") {
                                openBrowser("https://github.com/Greatwallcorner")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.size(25.dp))
            }
            Column(Modifier.align(Alignment.BottomCenter)) {
                OutlinedButton(
                    onClick = { openBrowser("https://t.me/tv_multiplatform") },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("TG讨论群")
                }

                Row(Modifier.align(Alignment.CenterHorizontally).padding(vertical = 15.dp)) {
                    Icon(
                        Icons.Default.Code,
                        "source code",
                        modifier = Modifier.padding(5.dp).align(Alignment.CenterVertically)
                    )
                    HoverableText("源代码") {
                        openBrowser("https://github.com/Greatwallcorner/TV-Multiplatform")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun previewAboutDialog() {
    AppTheme {
        AboutDialog(Modifier, true) {}
    }
}

fun openBrowser(url: String) {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(URI(url))
    }
}

@Composable
fun AboutItem(title: String, modifier: Modifier, content: @Composable (Modifier) -> Unit) {
    Row(modifier.padding(vertical = 5.dp, horizontal = 15.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.align(Alignment.CenterVertically).padding(end = 5.dp)
        )
        content(Modifier.align(Alignment.CenterVertically))
    }
}

@Composable
@Preview
fun SettingItem() {
    AppTheme(useDarkTheme = false) {
//        SettingItem(
//            Modifier,
//            "点播", "PeopleInSpaceTheme"
//        ) {
//
//        }
    }
}