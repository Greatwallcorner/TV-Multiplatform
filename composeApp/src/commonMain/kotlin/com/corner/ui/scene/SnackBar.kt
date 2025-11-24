package com.corner.ui.scene

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

object SnackBar {
    private val msgQueue = ConcurrentLinkedQueue<Message>()
    private val msgList = List(4) { MutableStateFlow<Message?>(null) }
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val activeMessages = AtomicInteger(0)
    private const val BURST_THRESHOLD = 3
    private const val MAX_QUEUE_SIZE = 10

    data class Message(
        val content: String,
        val priority: Int = 0,
        val type: MessageType = MessageType.INFO,
        val timestamp: Long = System.currentTimeMillis(),
        val key: String? = null
    )

    enum class MessageType {
        INFO, SUCCESS, WARNING, ERROR
    }

    init {
        startMessageProcessors()
    }

    private fun startMessageProcessors() {
        msgList.forEachIndexed { _, stateFlow ->
            scope.launch {
                while (true) {
                    val message = msgQueue.poll()
                    if (message != null) {
                        stateFlow.value = message
                        activeMessages.incrementAndGet()

                        val displayTime = calculateDisplayTime(message.content)
                        delay(displayTime)

                        stateFlow.value = null
                        activeMessages.decrementAndGet()
                    } else {
                        delay(100)
                    }
                }
            }
        }
    }

    private fun calculateDisplayTime(content: String): Long {
        return (2000L + content.length * 50L).coerceIn(2000L, 5000L)
    }

    private fun shouldMergeMessages(): Boolean {
        val recentMessages = msgQueue.filter {
            System.currentTimeMillis() - it.timestamp < 1000
        }
        return recentMessages.size >= BURST_THRESHOLD
    }

    fun postMsg(msg: String, priority: Int = 0, type: MessageType = MessageType.INFO, key: String? = null) {
        val newMessage = Message(msg, priority, type, key = key)

        if (key != null) {
            msgList.forEachIndexed { _, stateFlow ->
                val currentMsg = stateFlow.value
                if (currentMsg?.key == key) {
                    stateFlow.value = newMessage
                    return
                }
            }

            val tempList = msgQueue.filter { it.key != key }.toMutableList()
            tempList.add(newMessage)
            tempList.sortByDescending { it.priority }

            msgQueue.clear()
            tempList.forEach { msgQueue.add(it) }
        } else {
            if (shouldMergeMessages()) {
                val merged = Message("已处理 ${msgQueue.size + 1} 条消息", priority, MessageType.INFO)
                msgQueue.clear()
                msgQueue.add(merged)
            } else {
                val tempList = msgQueue.toMutableList()
                tempList.add(newMessage)
                tempList.sortByDescending { it.priority }

                msgQueue.clear()
                tempList.forEach { msgQueue.add(it) }
            }
        }

        if (msgQueue.size > MAX_QUEUE_SIZE) {
            repeat(msgQueue.size - MAX_QUEUE_SIZE) { msgQueue.poll() }
        }
    }

    @Composable
    fun SnackBarItem(index: Int) {
        val stateFlow = msgList[index]
        val message by stateFlow.collectAsState()

        var animatedVisibility by remember { mutableStateOf(false) }
        var currentDisplayMessage by remember { mutableStateOf<Message?>(null) }

        LaunchedEffect(message) {
            if (message != null) {
                currentDisplayMessage = message
                animatedVisibility = true
            } else if (currentDisplayMessage != null) {
                animatedVisibility = false
                delay(400) // 等待退出动画完成
                currentDisplayMessage = null
            }
        }

        // 使用 AnimatedVisibility 控制显示
        AnimatedVisibility(
            visible = animatedVisibility,
            enter = slideInVertically(
                initialOffsetY = { it }, // 从底部进入
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it }, // 向底部退出
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
            ) + fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            currentDisplayMessage?.let { msg ->
                // 使用 Box 包裹并居中
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 420.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = getMessageBackgroundColor(msg.type).let {
                            if (msg.priority > 0) it.copy(alpha = 0.95f) else it
                        },
                        tonalElevation = if (msg.priority > 0) 4.dp else 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (msg.priority > 0) {
                                Box(
                                    Modifier
                                        .size(width = 4.dp, height = 20.dp)
                                        .background(Color.Red, RoundedCornerShape(2.dp))
                                )
                                Spacer(Modifier.width(8.dp))
                            }

                            Icon(
                                imageVector = getMessageIcon(msg.type),
                                contentDescription = null,
                                tint = getMessageIconColor(msg.type),
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(Modifier.width(12.dp))

                            Text(
                                text = msg.content,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (msg.priority > 0) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                ),
                                color = getMessageTextColor(msg.type),
                                modifier = Modifier.weight(1f),
                                maxLines = 3
                            )

                            Spacer(Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    animatedVisibility = false
                                    scope.launch {
                                        delay(400)
                                        stateFlow.value = null
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "关闭",
                                    tint = getMessageIconColor(msg.type).copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SnackBarList() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                msgList.forEachIndexed { index, _ ->
                    key(index) {
                        SnackBarItem(index)
                    }
                }
            }
        }
    }


    // 颜色配置函数
    @Composable
    private fun getMessageBackgroundColor(type: MessageType): Color {
        return when (type) {
            MessageType.INFO -> MaterialTheme.colorScheme.primaryContainer
            MessageType.SUCCESS -> MaterialTheme.colorScheme.secondaryContainer
            MessageType.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
            MessageType.ERROR -> MaterialTheme.colorScheme.errorContainer
        }
    }

    @Composable
    private fun getMessageTextColor(type: MessageType): Color {
        return when (type) {
            MessageType.INFO -> MaterialTheme.colorScheme.onPrimaryContainer
            MessageType.SUCCESS -> MaterialTheme.colorScheme.onSecondaryContainer
            MessageType.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
            MessageType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        }
    }

    @Composable
    private fun getMessageIconColor(type: MessageType): Color {
        return when (type) {
            MessageType.INFO -> MaterialTheme.colorScheme.primary
            MessageType.SUCCESS -> MaterialTheme.colorScheme.secondary
            MessageType.WARNING -> MaterialTheme.colorScheme.tertiary
            MessageType.ERROR -> MaterialTheme.colorScheme.error
        }
    }

    private fun getMessageIcon(type: MessageType): ImageVector {
        return when (type) {
            MessageType.INFO -> Icons.Default.Info
            MessageType.SUCCESS -> Icons.Default.CheckCircle
            MessageType.WARNING -> Icons.Default.Warning
            MessageType.ERROR -> Icons.Default.Error
        }
    }
}