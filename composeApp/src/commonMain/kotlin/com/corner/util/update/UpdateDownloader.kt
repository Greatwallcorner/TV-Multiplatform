package com.corner.util.update

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class UpdateDownloader {
    companion object {
        private val log = LoggerFactory.getLogger(UpdateDownloader::class.java)

        fun downloadUpdate(
            url: String,
            destination: File,
            client: HttpClient = com.corner.util.network.KtorClient.client
        ): Flow<DownloadProgress> = flow {
            emit(DownloadProgress.Starting)

            try {
                val response: HttpResponse = client.get(url)
                val contentLength = response.headers["Content-Length"]?.toLongOrNull() ?: 0L

                val channel: ByteReadChannel = response.body()
                val tempFile = File(destination.parent, "${destination.name}.tmp")

                // 移除 channel.use，让响应处理器管理资源
                Files.newOutputStream(tempFile.toPath()).use { output ->
                    val buffer = ByteArray(8192)
                    var totalBytesRead = 0L
                    var bytesRead: Int

                    while (!channel.isClosedForRead) {
                        bytesRead = channel.readAvailable(buffer)
                        if (bytesRead > 0) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            if (contentLength > 0) {
                                val progress = (totalBytesRead * 100 / contentLength).toInt()
                                emit(DownloadProgress.Downloading(progress, totalBytesRead, contentLength))
                            } else {
                                emit(DownloadProgress.Downloading(0, totalBytesRead, contentLength))
                            }
                        } else if (bytesRead == -1) {
                            break
                        }
                    }
                }

                Files.move(
                    tempFile.toPath(),
                    destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )

                emit(DownloadProgress.Completed(destination))
            } catch (e: Exception) {
                log.error("Download failed", e)
                emit(DownloadProgress.Failed(e.message ?: "Unknown error"))
            }
        }.flowOn(Dispatchers.IO)

        suspend fun downloadUpdateSync(
            url: String,
            destination: File,
            client: HttpClient = com.corner.util.network.KtorClient.client
        ): Result<File> = withContext(Dispatchers.IO) {
            try {
                val response: HttpResponse = client.get(url)
                val tempFile = File(destination.parent, "${destination.name}.tmp")

                val channel: ByteReadChannel = response.body()
                Files.newOutputStream(tempFile.toPath()).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (!channel.isClosedForRead) {
                        bytesRead = channel.readAvailable(buffer)
                        if (bytesRead > 0) {
                            output.write(buffer, 0, bytesRead)
                        } else if (bytesRead == -1) {
                            break
                        }
                    }
                }

                Files.move(
                    tempFile.toPath(),
                    destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )

                Result.success(destination)
            } catch (e: Exception) {
                log.error("Download failed", e)
                Result.failure(e)
            }
        }
    }
}

sealed class DownloadProgress {
    object Starting : DownloadProgress()
    data class Downloading(
        val progress: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : DownloadProgress()
    data class Completed(val file: File) : DownloadProgress()
    data class Failed(val error: String) : DownloadProgress()
}
