import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.test.Test

class scopeTest{
    @Test
    fun main(): Unit {
        val scope = CoroutineScope(Job())

        // 启动一个协程
        val job = scope.launch {
            delay(1000)
            println("Coroutine finished")
        }

        // 检查作用域中是否有协程在执行
        val isActive = scope.coroutineContext[Job]?.isActive ?: false
        if (isActive) {
            println("There are active coroutines in the scope")
        } else {
            println("There are no active coroutines in the scope")
        }

        // 取消协程
        job.cancel()

        // 再次检查作用域中是否有协程在执行
        val isStillActive = scope.coroutineContext[Job]?.isActive ?: false
        if (isStillActive) {
            println("There are still active coroutines in the scope")
        } else {
            println("There are no active coroutines in the scope")
        }
    }
}
