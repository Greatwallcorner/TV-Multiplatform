package com.corner.ui.player

import com.seiko.imageloader.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import org.slf4j.LoggerFactory
import java.util.concurrent.LinkedBlockingDeque

private val log = LoggerFactory.getLogger("BitmapPool")

class BitmapPool(private var maxPoolSize: Int = 3) {
    private val pool = LinkedBlockingDeque<Bitmap>()

    @Volatile
    private var createdCount = 0

    fun setMaxSize(size: Int) {
        maxPoolSize = size
        log.info("BitmapPool 最大容量更新为: $maxPoolSize")
    }

    fun acquire(width: Int, height: Int): Bitmap {
        synchronized(this) {
            pool.iterator().let { iterator ->
                while (iterator.hasNext()) {
                    val bitmap = iterator.next()
                    // 增加有效性检查
                    if (!bitmap.isClosed && bitmap.width == width && bitmap.height == height) {
                        iterator.remove()
                        return bitmap
                    }
                }
            }

            createdCount++
            return Bitmap().apply {
                try {
                    allocPixels(ImageInfo.makeN32(width, height, ColorAlphaType.PREMUL))
                } catch (e: Exception) {
                    log.error("创建 Bitmap 失败: ${width}x$height", e)
                    throw e
                }
            }
        }
    }

    fun release(bitmap: Bitmap) {
        // 增加有效性检查
        if (bitmap.isClosed) {
            return
        }

        synchronized(this) {
            if (!bitmap.isClosed && pool.size < maxPoolSize) {
                // 检查是否已经存在于池中，避免重复添加
                if (!pool.contains(bitmap)) {
                    if (!pool.offerFirst(bitmap)) {
                        bitmap.close()
                    }
                }
            } else {
                bitmap.close()
            }
        }
    }


    fun clear() {
        pool.forEach { it.close() }
        pool.clear()
    }
}
