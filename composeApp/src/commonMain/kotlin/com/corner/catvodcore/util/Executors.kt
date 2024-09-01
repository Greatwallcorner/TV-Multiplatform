package com.corner.catvodcore.util

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object Executors {
    val instance: Executor = ThreadPoolExecutor(5, 15, 30, TimeUnit.SECONDS, ArrayBlockingQueue(50))
}
