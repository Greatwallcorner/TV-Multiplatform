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
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.corner.bean.Setting
import com.corner.bean.SettingStore
import com.corner.bean.SettingType
import com.corner.catvodcore.config.api
import com.corner.catvodcore.enum.ConfigType
import com.corner.database.Db
import com.corner.init.initConfig
import com.corner.ui.decompose.component.DefaultSettingComponent
import com.corner.ui.scene.BackRow
import com.corner.ui.scene.Dialog
import com.corner.ui.scene.SnackBar
import kotlinx.coroutines.launch
import org.jsoup.internal.StringUtil
import java.util.*

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
fun SettingScene(component: DefaultSettingComponent, modifier: Modifier, onClickBack: () -> Unit) {
    val model = component.model.subscribeAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var currentChoose by remember { mutableStateOf<Setting?>(null) }
    DisposableEffect("setting") {
        component.sync()
        onDispose {
            SettingStore.write()
        }
    }

    DisposableEffect(model.value.settingList){
        println("settingList 修改")
        onDispose {  }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        Column(modifier.fillMaxSize()) {
            BackRow(Modifier, onClickBack = { onClickBack() }) {
                Text(
                    "设置",
                    style = MaterialTheme.typography.h4,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            LazyColumn {
                items(model.value.settingList) {
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
            component.sync()
        }
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
                SiteViewModel.viewModelScope.launch {
                    when (currentChoose!!.id) {
                        "vod" -> {
                            if (textFieldValue == null || textFieldValue == "") {
                                SnackBar.postMsg("不可为空")
                                return@launch
                            }
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
                            SettingStore.setValue(SettingType.VOD, textFieldValue!!)
                            api?.cfg?.value = Db.Config.find(textFieldValue!!, ConfigType.SITE.ordinal.toLong())
                            initConfig()
                        }

                        "player" -> {
                            SettingStore.setValue(SettingType.PLAYER, textFieldValue!!)
                        }

                        "log" -> {
                            if (textFieldValue == null || textFieldValue == "") {
                                SnackBar.postMsg("不可为空")
                                return@launch
                            }
                            SettingStore.setValue(SettingType.LOG, textFieldValue!!)
                        }
                    }
                }.invokeOnCompletion {
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