package com.corner.ui.player

import com.seiko.imageloader.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import org.slf4j.LoggerFactory
import java.util.concurrent.LinkedBlockingDeque

private val log = LoggerFactory.getLogger("BitmapPool")

class BitmapPool(private var maxPoolSize: Int = 3) {
    private val pool = LinkedBlockingDeque<Bitmap>()
    @Volatile private var createdCount = 0

    fun setMaxSize(size: Int) {
        maxPoolSize = size
        log.info("BitmapPool 最大容量更新为: $maxPoolSize")
    }

    fun acquire(width: Int, height: Int): Bitmap {
        pool.iterator().let { iterator ->
            while (iterator.hasNext()) {
                val bitmap = iterator.next()
                if (bitmap.width == width && bitmap.height == height) {
                    iterator.remove()
                    return bitmap
                }
            }
        }

        synchronized(this) {
            createdCount++
            return Bitmap().apply {
                allocPixels(ImageInfo.makeN32(width, height, ColorAlphaType.PREMUL))
            }
        }
    }

    fun release(bitmap: Bitmap) {
        if (!bitmap.isClosed && pool.size < maxPoolSize) {
            if (!pool.offerFirst(bitmap)) {
                bitmap.close()
            }
        } else {
            bitmap.close()
        }
    }

    fun clear() {
        pool.forEach { it.close() }
        pool.clear()
    }
}
