package com.corner.ui

import SiteViewModel
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import ch.qos.logback.classic.Level
import com.corner.bean.Setting
import com.corner.catvodcore.config.api
import com.corner.catvodcore.enum.ConfigType
import com.corner.catvodcore.util.Jsons
import com.corner.catvodcore.util.Paths
import com.corner.database.Db
import com.corner.init.initConfig
import com.corner.ui.scene.BackRow
import com.corner.ui.scene.Dialog
import com.github.sardine.model.Set
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import org.jsoup.internal.StringUtil
import java.nio.file.Files
import java.util.*
import kotlin.io.path.exists

@Composable
fun SettingItem(modifier: Modifier, label: String, value: String?, onClick: () -> Unit) {
    Row(
        modifier
            .clickable {
                onClick()
            }.shadow(3.dp)
            .background(MaterialTheme.colors.background, shape = RoundedCornerShape(4))
            .padding(start = 20.dp, end = 20.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 15.dp),
            color = MaterialTheme.colors.onBackground
        )
        Text(
            text = if (StringUtil.isBlank(value)) "无" else value ?: "",
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 15.dp)
                .weight(0.5f),
            color = MaterialTheme.colors.onBackground
        )
    }
}

@Composable
fun SettingItem2(modifier: Modifier, label: String, content: @Composable () -> Unit) {
    Row(
        modifier
            .shadow(3.dp)
            .background(MaterialTheme.colors.background, shape = RoundedCornerShape(4))
            .padding(start = 20.dp, end = 20.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 15.dp),
            color = MaterialTheme.colors.onBackground
        )
        content()
    }
}

private val defaultList = listOf(
    Setting("vod", "点播", ""),
    Setting("player", "外部播放器", ""),
    Setting("log", "日志级别", Level.INFO.levelStr)
)
//private var settingListData = mutableStateOf<MutableList<Setting>>(mutableListOf())
private var settingListData = mutableListOf<Setting>()

fun getSettingItem(s: String): String {
    return settingListData.find { it.id == s }?.value ?: ""
}

@Composable
fun SettingScene(modifier: Modifier, onClickBack: () -> Unit) {
    var showEditDialog by remember { mutableStateOf(false) }
    var currentChoose by remember { mutableStateOf<Setting?>(null) }
//    val settingList = rememberUpdatedState{ settingListData }
    var settingList = remember { mutableStateOf<List<Setting>>(listOf()) }
    DisposableEffect("setting") {
        initSetting()
        settingList.value = settingListData.toList()
        onDispose {
            Files.write(Paths.setting(), Jsons.encodeToString(settingListData.toList()).toByteArray())
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        Column(modifier.fillMaxSize()) {
            BackRow(modifier, onClickBack = { onClickBack() }) {
                Text(
                    "设置",
                    style = MaterialTheme.typography.h4,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            LazyColumn {
                items(settingList.value) {
                    SettingItem(
                        modifier, it.label, it.value
                    ) {
                        showEditDialog = true
                        currentChoose = it
                    }
                }
            }
        }
        DialogEdit(showEditDialog, onClose = { showEditDialog = false }, currentChoose = currentChoose) {
//            settingListData = settingListData.toList().toMutableList()
            settingList.value = settingListData.toList().toMutableList().toList()
        }
    }

}

fun initSetting() {
    val file = Paths.setting()
    if (file.exists() && settingListData.size == 0) {
        settingListData.addAll(Jsons.decodeFromString<List<Setting>>(Files.readString(file)))
        if (settingListData.size != defaultList.size) {
            defaultList.forEach { setting ->
                if (settingListData.find { setting.id == it.id } == null) {
                    settingListData.add(setting)
                }
            }
        }
    }
    if (settingListData.size == 0) {
        settingListData.addAll(defaultList)
        Files.write(file, Jsons.encodeToString(settingListData.toList()).toByteArray())
    }
}

@Composable
fun DialogEdit(
    showEditDialog: Boolean,
    onClose: () -> Unit,
    currentChoose: Setting?,
    onValueChange: (v: String) -> Unit
) {
    var textFieldValue by remember { mutableStateOf(currentChoose?.value) }
    val focusRequester = remember { FocusRequester() }
    if (showEditDialog) {
        LaunchedEffect(Unit) {
            textFieldValue = currentChoose?.value
            focusRequester.requestFocus()
        }
    }
    Dialog(
        modifier = Modifier.fillMaxWidth(0.5f),
        showEditDialog,
        onClose = {
            onClose()
        }
    ) {
        Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 25.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(currentChoose?.label ?: "", color = MaterialTheme.colors.onBackground)
                Spacer(modifier = Modifier.size(35.dp))
                TextField(
                    modifier = Modifier.fillMaxWidth(0.8f).focusRequester(focusRequester),
                    enabled = true,
                    value = textFieldValue ?: "",
                    onValueChange = { text -> textFieldValue = text },
                    interactionSource = MutableInteractionSource(),
                    maxLines = 2
                )
            }
            Button(modifier = Modifier.align(Alignment.End), onClick = {
                if (textFieldValue == null || textFieldValue == "") {
                    return@Button
                }
                SiteViewModel.viewModelScope.launch {
                    when (currentChoose!!.id) {
                        "vod" -> {
                            val config = Db.Config.find(textFieldValue!!, ConfigType.SITE.ordinal.toLong())
                            if (config == null) {
                                Db.Config.save(
                                    type = ConfigType.SITE.ordinal.toLong(),
                                    time = Date(),
                                    url = textFieldValue
                                )
                            } else {
                                Db.Config.updateUrl(config.id, textFieldValue as String)
                            }
                            settingListData.find { it.id == "vod" }?.value = textFieldValue
                            api?.cfg?.value = Db.Config.find(textFieldValue!!, ConfigType.SITE.ordinal.toLong())
                            initConfig()
                        }

                        "player" -> {
                            val find = settingListData.find { it.id == "player" }
                            find?.value = textFieldValue
                        }

                        "log" -> {
                            val find = settingListData.find { it.id == "log" }
                            find?.value = textFieldValue
                        }
                    }
                }.invokeOnCompletion {
                    Files.write(Paths.setting(), Jsons.encodeToString(settingListData.toList()).toByteArray())
                    onValueChange(textFieldValue ?: "")
                    onClose()
                }
            }) {
                Text("确认")
            }
        }
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