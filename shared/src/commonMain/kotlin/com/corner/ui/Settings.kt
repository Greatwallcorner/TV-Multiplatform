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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
import com.corner.ui.scene.HoverableText
import com.corner.ui.scene.SnackBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jsoup.internal.StringUtil
import java.awt.Desktop
import java.net.URI
import java.util.*

@Composable
fun SettingItem(modifier: Modifier, label: String, value: String?, onClick: () -> Unit) {
    Row(
        modifier
            .clickable {
                onClick()
            }.shadow(3.dp)
            .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(4.dp))
            .padding(start = 20.dp, end = 20.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 15.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (StringUtil.isBlank(value)) "无" else value ?: "",
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 15.dp)
                .weight(0.5f),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun SettingScene(component: DefaultSettingComponent, onClickBack: () -> Unit) {
    val model = component.model.subscribeAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var currentChoose by remember { mutableStateOf<Setting?>(null) }
    DisposableEffect("setting") {
        component.sync()
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
            BackRow(Modifier, onClickBack = { onClickBack() }) {
                Text(
                    "设置",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
//                IconButton(modifier = Modifier.align(Alignment.End), onClick = {showAboutDialog = true}){ Icon(Icons.Default.Info, "About", tint = MaterialTheme.colorScheme.onSecondary) }
            }
            LazyColumn {
                items(model.value.settingList) {
                    SettingItem(
                        Modifier, it.label, it.value
                    ) {
                        showEditDialog = true
                        currentChoose = it
                    }
                }
            }
        }
        Surface(Modifier.align(Alignment.BottomCenter).padding(bottom = 15.dp)) {
            HoverableText("关于", style = MaterialTheme.typography.displayMedium) {
                showAboutDialog = true
            }
        }
        AboutDialog(Modifier.fillMaxSize(0.4f), showAboutDialog) { showAboutDialog = false }
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
                Text(currentChoose?.label ?: ""/*, color = MaterialTheme.colors.onBackground*/)
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
                            SnackBar.postMsg("重启生效")
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
fun AboutDialog(modifier: Modifier, showAboutDialog: Boolean, onClose: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    Dialog(modifier, showAboutDialog,
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
                    enter = fadeIn() + slideIn(animationSpec = tween(800), initialOffset = {i -> IntOffset(0, -20) })
                ) {
                    Column {
                        Image(
                            painter = painterResource("avatar.png"),
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
                OutlinedButton(
                    onClick = { openBrowser("https://t.me/tv_multiplatform") },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("TG讨论群")
                }
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(vertical = 15.dp)) {
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