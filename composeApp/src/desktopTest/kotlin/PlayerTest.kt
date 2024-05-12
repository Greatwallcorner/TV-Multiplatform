import kotlinx.coroutines.*


class PlayerTest {
    val playerPath = "\"E:\\Program File\\Tools\\Scoop\\apps\\potplayer\\current\\PotPlayerMini64.exe\""



}

fun main() = runBlocking {
    val supervisor = SupervisorJob()

    val child1 = launch(supervisor) {
        delay(1000)
        println("Child 1 completed")
    }

    val child2 = launch(supervisor) {
        delay(500)
        println("Child 2 completed")
    }

    delay(700)

    // 取消所有子作业
    supervisor.cancelChildren()

    // 等待所有作业完成
    supervisor.join()

    println("All children are completed")
}