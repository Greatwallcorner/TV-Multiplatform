package com.corner.init

import com.corner.catvodcore.util.KtorHeaderUrlFetcher
import com.corner.catvodcore.util.Paths
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.defaultImageResultMemoryCache
import okio.Path.Companion.toOkioPath

actual fun initPlatformSpecify() {
}

fun generateImageLoader(): ImageLoader {
    return ImageLoader {
        components {
//            setupDefaultComponents()
            add(KtorHeaderUrlFetcher.CustomUrlFetcher)
        }
        interceptor {
            // cache 100 success image result, without bitmap
            defaultImageResultMemoryCache()
            memoryCacheConfig {
                maxSizeBytes(32 * 1024 * 1024) // 32MB
            }

            diskCacheConfig {
                directory(Paths.picCache().toOkioPath())
                maxSizeBytes(512L * 1024 * 1024) // 512MB
            }
        }
    }
}
