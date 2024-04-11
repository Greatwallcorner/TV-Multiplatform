package com.corner.util

import io.ktor.util.*
import kotlinx.coroutines.Job
import java.io.*
import java.nio.charset.Charset
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile


object Utils {
    fun <E> MutableList<E>.addIfAbsent(index:Int, element: E){
        if(!this.contains(element)){
            this.add(index, element)
        }
    }

    fun MutableList<Job>.cancelAll():MutableList<Job>{
        this.forEach{it.cancel()}
        return this
    }

    fun StringValues.toSingleValueMap(): Map<String, String> =
        entries().associateByTo(LinkedHashMap(), { it.key }, { it.value.toList()[0] })

    /**
     * 解压文件
     *
     * @param zipFile：需要解压缩的文件
     * @param descDir：解压后的目标目录 以/结尾
     */
    @Throws(IOException::class)
    fun unZipFiles(zipFile: File, descDir: String) {
        val destFile = File(descDir)
        if (!destFile.exists()) {
            destFile.mkdirs()
        }
        // 解决zip文件中有中文目录或者中文文件
        val zip = ZipFile(zipFile, Charset.forName("GBK"))
        val entries: Enumeration<*> = zip.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement() as ZipEntry
            val `in`: InputStream = zip.getInputStream(entry)
            val curEntryName = entry.name
            // 判断文件名路径是否存在文件夹
            val endIndex = curEntryName.lastIndexOf('/')
            // 替换

            val outFile = destFile.resolve(curEntryName)
            if (endIndex != -1) {
                if (!outFile.exists()) {
                    outFile.mkdirs()
                }
            }

            // 判断文件全路径是否为文件夹,如果是上面已经上传,不需要解压
            if (outFile.isDirectory) {
                continue
            }
            val out: OutputStream = FileOutputStream(outFile)
            val buf1 = ByteArray(1024)
            var len: Int
            while ((`in`.read(buf1).also { len = it }) > 0) {
                out.write(buf1, 0, len)
            }
            `in`.close()
            out.close()
        }
    }
}