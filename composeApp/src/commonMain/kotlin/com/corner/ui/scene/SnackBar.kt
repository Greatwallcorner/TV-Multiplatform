import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

object SnackBar {
    private val msgQueue = ConcurrentLinkedQueue<Message>()
    private val msgList = List(4) { MutableStateFlow<Message?>(null) }
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val activeMessages = AtomicInteger(0)

    private val animationMutex = List(4) { Mutex() }

    private const val BURST_THRESHOLD = 3
    private const val MAX_QUEUE_SIZE = 10

    data class Message(
        val content: String,
        val priority: Int = 0,
        val type: MessageType = MessageType.INFO,
        val timestamp: Long = System.currentTimeMillis()
    )

    enum class MessageType {
        INFO, SUCCESS, WARNING, ERROR
    }

    init {
        startMessageProcessors()
    }

    private fun startMessageProcessors() {
        msgList.forEachIndexed { index, stateFlow ->
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

    fun postMsg(msg: String, priority: Int = 0, type: MessageType = MessageType.INFO) {
        val newMessage = Message(msg, priority, type)

        // 智能合并突发消息
        if (shouldMergeMessages()) {
            val merged = Message("已处理 ${msgQueue.size + 1} 条消息", priority, MessageType.INFO)
            msgQueue.clear()
            msgQueue.add(merged)
        } else {
            // 按优先级排序插入，高优先级在前
            val tempList = msgQueue.toMutableList()
            tempList.add(newMessage)
            tempList.sortByDescending { it.priority }

            msgQueue.clear()
            tempList.forEach { msgQueue.add(it) }
        }

        // 限制队列长度
        if (msgQueue.size > MAX_QUEUE_SIZE) {
            repeat(msgQueue.size - MAX_QUEUE_SIZE) { msgQueue.poll() }
        }
    }

    @Composable
    fun SnackBarItem(index: Int) {
        val message = msgList[index].collectAsState()
        var visible by remember { mutableStateOf(false) }
        var currentMessage by remember { mutableStateOf<Message?>(null) }

        LaunchedEffect(message.value) {
            if (message.value != null) {
                currentMessage = message.value
                visible = true
            } else {
                // 先触发退出动画
                visible = false
                // 等待动画完成后再清除消息
                delay(600) // 等待动画完成
                currentMessage = null
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(durationMillis = 400)
            ),
            exit = slideOutVertically(
                targetOffsetY = {  fullHeight -> fullHeight * 2 },
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing) // 延长渐隐时间
            ) + fadeOut(
                animationSpec = tween(durationMillis = 400) // 更平滑的渐隐
            )
        ) {
            currentMessage?.let { msg ->
                // 根据优先级调整显示位置
                val bottomPadding = if (msg.priority > 0) {
                    8.dp + (index * 2).dp // 高优先级消息更靠上
                } else {
                    16.dp + (index * 4).dp // 普通优先级消息正常间距
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = bottomPadding,
                            start = 16.dp,
                            end = 16.dp
                        )
                        .animateContentSize(), // 添加内容大小动画
                    contentAlignment = Alignment.TopCenter
                ) {
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 420.dp)
                            .shadow(
                                elevation = if (msg.priority > 0) 12.dp else 8.dp, // 高优先级阴影更强
                                shape = RoundedCornerShape(12.dp),
                                ambientColor = Color.Black.copy(alpha = 0.1f),
                                spotColor = Color.Black.copy(alpha = 0.2f)
                            )
                            .animateContentSize(), // 添加阴影大小动画
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
                            // 优先级指示器
                            if (msg.priority > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(
                                            color = Color.Red,
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Icon(
                                imageVector = getMessageIcon(msg.type),
                                contentDescription = null,
                                tint = getMessageIconColor(msg.type),
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = msg.content,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (msg.priority > 0) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                ),
                                color = getMessageTextColor(msg.type),
                                modifier = Modifier.weight(1f),
                                maxLines = 3,
                                textAlign = TextAlign.Start
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    msgList[index].value = null
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
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                msgList.forEachIndexed { index, _ ->
                    SnackBarItem(index)
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
