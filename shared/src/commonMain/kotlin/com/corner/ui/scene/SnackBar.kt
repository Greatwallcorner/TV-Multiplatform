package com.corner.ui.scene

import AppTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.ConcurrentLinkedQueue

object SnackBar {

    private val msgQueue = ConcurrentLinkedQueue<String>()
    private val msgList = MutableList(2) { MutableStateFlow("") }

    private val CoroutineScope = CoroutineScope(Dispatchers.Default)

    private var isRunningSemaphore = Semaphore(2)

    init {
        CoroutineScope.launch {
            delay(100)
            msgCheck()
        }
    }

    private fun msgCheck() {
        for (i in 0 until msgList.size) {
            CoroutineScope.launch {
                isRunningSemaphore.acquire()
                while (true) {
                    if (msgList[i].value.isBlank()) {
                        val poll = msgQueue.poll() ?: break
                        msgList[i].value = poll
                        // 显示三秒
                        delay(3000)
                        msgList[i].value = ""
                        // 等待消失
                        delay(200)
                    }
                }
                isRunningSemaphore.release()
            }
        }
    }

    @Composable
    fun SnackBarItem(index: Int) {
        val msg = msgList[index].collectAsState()
        LaunchedEffect(msg) {
            if (msg.value.isNotBlank()) {
                delay(3000)
                msgList[index].value = ""
                delay(50)
                msgList[index].value = msgQueue.poll() ?: ""
            }
        }
        AnimatedVisibility(
            visible = msg.value.trim().isNotBlank(),
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> -fullHeight },
                animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing)
            ),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight -> -fullHeight },
                animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing)
            )
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Snackbar(
                    modifier = Modifier.padding(top = 5.dp, start = 10.dp, end = 10.dp)
                        .height(60.dp)
                        .width((35 * msg.value.length).dp)
                        .align(Alignment.Center),
                    shape = RoundedCornerShape(25),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    action = {
                        Box(modifier = Modifier.fillMaxHeight()) {
                            Icon(
                                imageVector = Icons.Outlined.Close, contentDescription = "Close the SnackBar",
                                modifier = Modifier.clickable(onClick = { msgList[index].value = "" })
                                    .align(Alignment.Center)
                            )
                        }
                    }
                ) {

                    Text(msg.value, modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center)
                }
            }
        }
    }

    @Composable
    fun SnackBarList() {
        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.aligned(Alignment.Bottom)
        ) {
            repeat(msgList.size) {
                SnackBarItem(it)
            }
        }
    }

    fun postMsg(msg: String) {
        if (msgQueue.size > 6) {
            msgQueue.poll()
        }
        msgQueue.add(msg)
        msgCheck()
    }

}

@Composable
@Preview
fun previewSnackBar() {
    AppTheme {
        SnackBar.postMsg("TEST")
        SnackBar.SnackBarItem(0)
    }
}