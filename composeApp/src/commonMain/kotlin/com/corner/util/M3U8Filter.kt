import com.corner.util.M3U8FilterConfig
import org.slf4j.LoggerFactory

// 基于 ltxlong 的 JavaScript 项目 M3U8-Filter-Ad-Script (MIT License) 重构为 Kotlin
// 原始代码版权信息: Copyright (c) 2024 ltxlong
// 原始许可证: https://opensource.org/licenses/MIT

private val log = LoggerFactory.getLogger("M3U8Filter")

class M3U8Filter(
    config: M3U8FilterConfig = M3U8FilterConfig() // 添加配置参数
) {
    private var tsNameLen = 0 // ts前缀长度
    private var firstExtinfRow = ""
    private var theExtinfJudgeRowN = 0
    private var theSameExtinfNameN = 0
    private var prevTsNameIndex = -1 // 上个ts序列号
    private var firstTsNameIndex = -1 // 首个ts序列号
    private var tsType = 0 // 0：xxxx000数字递增.ts模式0；1：xxxxxxxxxx.ts模式1；2：***.ts模式2-暴力拆解
    private var theExtXMode = 0 // 0：ext_x_discontinuity判断模式0；1：ext_x_discontinuity判断模式1
    private var tsNameLenExtend = config.tsNameLenExtend // 使用配置值
    private var theExtinfBenchmarkN = config.theExtinfBenchmarkN // 使用配置值
    private var violentFilterModeFlag = config.violentFilterModeFlag // 使用配置值
    private var filteredAdCount = 0

    fun getFilteredAdCount(): Int = filteredAdCount

    fun filterLines(lines: List<String>): List<String> {
        val result = mutableListOf<String>()
        filteredAdCount = 0
        if (violentFilterModeFlag) {
            log.info("启用暴力拆解模式")
            tsType = 2 // ts命名模式
        } else {
            log.info("自动判断模式")

            var theNormalIntTsN = 0
            var theDiffIntTsN = 0
            var lastTsNameLen = 0

            // 初始化参数
            for (i in lines.indices) {
                val line = lines[i]

                // 初始化firstExtinfRow
                if (theExtinfJudgeRowN == 0 && line.startsWith("#EXTINF")) {
                    firstExtinfRow = line
                    theExtinfJudgeRowN++
                } else if (theExtinfJudgeRowN == 1 && line.startsWith("#EXTINF")) {
                    if (line != firstExtinfRow) {
                        firstExtinfRow = ""
                    }
                    theExtinfJudgeRowN++
                }

                // 判断ts模式
                val theTsNameLen = line.indexOf(".ts") // ts前缀长度

                if (theTsNameLen > 0) {
                    if (theExtinfJudgeRowN == 1) {
                        tsNameLen = theTsNameLen
                    }

                    lastTsNameLen = theTsNameLen

                    val tsNameIndex = extractNumberBeforeTs(line)
                    if (tsNameIndex == null) {
                        if (theExtinfJudgeRowN == 1) {
                            tsType = 1 // ts命名模式
                        } else if (theExtinfJudgeRowN == 2 && (tsType == 1 || theTsNameLen == tsNameLen)) {
                            tsType = 1 // ts命名模式
                            log.info("识别ts模式1")
                            break
                        } else {
                            theDiffIntTsN++
                        }
                    } else {
                        // 如果序号相隔等于1: 模式0
                        // 如果序号相隔大于1，或其他：模式2（暴力拆解）

                        if (theNormalIntTsN == 0) {
                            // 初始化ts序列号
                            prevTsNameIndex = tsNameIndex
                            firstTsNameIndex = tsNameIndex
                            prevTsNameIndex = firstTsNameIndex - 1
                        }

                        if (theTsNameLen != tsNameLen) {
                            if (theTsNameLen == lastTsNameLen + 1 && tsNameIndex == prevTsNameIndex + 1) {
                                if (theDiffIntTsN > 0) {
                                    if (tsNameIndex == prevTsNameIndex + 1) {
                                        tsType = 0 // ts命名模式
                                        prevTsNameIndex = firstTsNameIndex - 1
                                        log.info("识别ts模式0")
                                        break
                                    } else {
                                        tsType = 2 // ts命名模式
                                        log.info("识别ts模式2")
                                        break
                                    }
                                }
                                theNormalIntTsN++
                                prevTsNameIndex = tsNameIndex
                            } else {
                                theDiffIntTsN++
                            }
                        } else {
                            if (theDiffIntTsN > 0) {
                                if (tsNameIndex == prevTsNameIndex + 1) {
                                    tsType = 0 // ts命名模式
                                    prevTsNameIndex = firstTsNameIndex - 1
                                    log.info("识别ts模式0")
                                    break
                                } else {
                                    tsType = 2 // ts命名模式
                                    log.info("识别ts模式2")
                                    break
                                }
                            }
                            theNormalIntTsN++
                            prevTsNameIndex = tsNameIndex
                        }
                    }
                }

                if (i == lines.size - 1) {
                    // 后缀不是ts，而是jpeg等等，或者以上规则判断不了的，或者没有广告切片的：直接暴力拆解过滤
                    tsType = 2 // ts命名模式
                    log.info("进入暴力拆解模式")
                }
            }
        }

        // 开始遍历过滤
        var i = 0
        while (i < lines.size) {
            val line = lines[i]

            if (tsType == 0) {
                if (line.startsWith("#EXT-X-DISCONTINUITY") && i + 2 < lines.size) {
                    // 检查当前行是否跟 #EXT-X-相关
                    if (i > 0 && lines[i - 1].startsWith("#EXT-X-")) {
                        result.add(line)
                        i++
                        continue
                    } else {
                        val theTsNameLen = lines[i + 2].indexOf(".ts") // ts前缀长度

                        if (theTsNameLen > 0) {
                            // 根据ts名字长度过滤
                            if (theTsNameLen - tsNameLen > tsNameLenExtend) {
                                // 广告过滤
                                if (i + 3 < lines.size && lines[i + 3].startsWith("#EXT-X-DISCONTINUITY")) {
                                    filteredAdCount++
                                    // 打印即将过滤的行
                                    log.info("过滤规则: #EXT-X-DISCONTINUITY-ts文件名长度-, 过滤内容: $line | ${lines[i+1]} | ${lines[i+2]}")
                                    i += 4
                                } else {
                                    filteredAdCount++
                                    // 打印即将过滤的行
                                    log.info("过滤规则: #EXT-X-DISCONTINUITY-ts文件名长度, 过滤内容: $line | ${lines[i+1]} | ${lines[i+2]}")
                                    i += 3
                                }
                                continue
                            } else {
                                tsNameLen = theTsNameLen
                            }

                            // 根据ts序列号过滤
                            val theTsNameIndex = extractNumberBeforeTs(lines[i + 2])

                            if (theTsNameIndex != prevTsNameIndex + 1) {
                                // 广告过滤
                                if (i + 3 < lines.size && lines[i + 3].startsWith("#EXT-X-DISCONTINUITY")) {
                                    filteredAdCount++
                                    // 打印即将过滤的行
                                    log.info("过滤规则: #EXT-X-DISCONTINUITY-ts序列号-, 过滤内容: $line | ${lines[i+1]} | ${lines[i+2]} | ${lines[i+3]}")
                                    i += 4
                                } else {
                                    filteredAdCount++
                                    // 打印即将过滤的行
                                    log.info("过滤规则: #EXT-X-DISCONTINUITY-ts序列号, 过滤内容: $line | ${lines[i+1]} | ${lines[i+2]} | ${lines[i+3]}")
                                    i += 3
                                }
                                continue
                            }
                        }
                    }
                }

                if (line.startsWith("#EXTINF") && i + 1 < lines.size) {
                    val theTsNameLen = lines[i + 1].indexOf(".ts") // ts前缀长度

                    if (theTsNameLen > 0) {
                        // 根据ts名字长度过滤
                        if (theTsNameLen - tsNameLen > tsNameLenExtend) {
                            // 广告过滤
                            if (i + 2 < lines.size && lines[i + 2].startsWith("#EXT-X-DISCONTINUITY")) {
                                filteredAdCount++
                                // 打印即将过滤的行
                                log.info("过滤规则: #EXTINF-ts文件名长度-, 过滤内容: $line | ${lines[i+1]} | ${lines[i+2]}")
                                i += 3
                            } else {
                                filteredAdCount++
                                // 打印即将过滤的行
                                log.info("过滤规则: #EXTINF-ts文件名长度, 过滤内容: $line | ${lines[i+1]}")
                                i += 2
                            }
                            continue
                        } else {
                            tsNameLen = theTsNameLen
                        }

                        // 根据ts序列号过滤
                        val theTsNameIndex = extractNumberBeforeTs(lines[i + 1])

                        if (theTsNameIndex == prevTsNameIndex + 1) {
                            prevTsNameIndex++
                        } else {
                            // 广告过滤
                            if (i + 2 < lines.size && lines[i + 2].startsWith("#EXT-X-DISCONTINUITY")) {
                                filteredAdCount++
                                // 打印即将过滤的行
                                log.info("过滤规则: #EXTINF-ts序列号-, 过滤内容: $line | ${lines[i+1]} | ${lines[i+2]}")
                                i += 3
                            } else {
                                filteredAdCount++
                                // 打印即将过滤的行
                                log.info("过滤规则: #EXTINF-ts序列号, 过滤内容: $line | ${lines[i+1]}")
                                i += 2
                            }
                            continue
                        }
                    }
                }
            } else if (tsType == 1) {
                if (line.startsWith("#EXTINF")) {
                    if (line == firstExtinfRow && theSameExtinfNameN <= theExtinfBenchmarkN && theExtXMode == 0) {
                        theSameExtinfNameN++
                    } else {
                        theExtXMode = 1
                    }

                    if (theSameExtinfNameN > theExtinfBenchmarkN) {
                        theExtXMode = 1
                    }
                }

                if (line.startsWith("#EXT-X-DISCONTINUITY")) {
                    // 检查当前行是否跟 #EXT-X-PLAYLIST-TYPE相关
                    if (i > 0 && lines[i - 1].startsWith("#EXT-X-PLAYLIST-TYPE")) {
                        result.add(line)
                        i++
                        continue
                    } else {
                        // 如果第 i+2 行是 .ts 文件，跳过当前行和接下来的两行
                        if (i + 2 < lines.size && lines[i + 1].startsWith("#EXTINF") && lines[i + 2].indexOf(".ts") > 0) {
                            var theExtXDiscontinuityConditionFlag = false

                            if (theExtXMode == 1) {
                                theExtXDiscontinuityConditionFlag = lines[i + 1] != firstExtinfRow && theSameExtinfNameN > theExtinfBenchmarkN
                            }

                            // 进一步检测第 i+3 行是否也是 #EXT-X-DISCONTINUITY
                            if (i + 3 < lines.size && lines[i + 3].startsWith("#EXT-X-DISCONTINUITY") && theExtXDiscontinuityConditionFlag) {
                                filteredAdCount++
                                // 打印即将过滤的行
                                log.info("过滤规则: #EXT-X-DISCONTINUITY-广告-#EXT-X-DISCONTINUITY过滤, 过滤内容: $line | ${lines[i+1]} | ${lines[i+2]} | ${lines[i+3]}")
                                i += 4 // 跳过当前行和接下来的三行
                            } else {
                                filteredAdCount++
                                // 打印即将过滤的行
                                log.info("过滤规则: #EXT-X-DISCONTINUITY-单个标识过滤, 过滤内容: $line")
                                i++
                            }
                            continue
                        }
                    }
                }
            } else {
                // 暴力拆解
                if (line.startsWith("#EXT-X-DISCONTINUITY")) {
                    // 检查当前行是否跟 #EXT-X-PLAYLIST-TYPE相关
                    if (i > 0 && lines[i - 1].startsWith("#EXT-X-PLAYLIST-TYPE")) {
                        result.add(line)
                        i++
                        continue
                    } else {
                        filteredAdCount++
                        // 打印即将过滤的行
                        log.info("过滤规则: #EXT-X-DISCONTINUITY-单个标识过滤, 过滤内容: $line")
                        i++
                        continue
                    }
                }
            }

            // 保留不需要过滤的行
            result.add(line)
            i++
        }

        return result
    }

    private fun extractNumberBeforeTs(str: String): Int? {
        // 匹配 .ts 前面的数字
        val match = Regex("(\\d+)\\.ts").find(str)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    fun safelyProcessM3u8(url: String, content: String): String {
        return try {
            val lines = content.split("\n")
            val newLines = filterLines(lines)
            newLines.joinToString("\n")
        } catch (e: Exception) {
            println("处理 m3u8 文件时出错: $url, ${e.message}")
            content
        }
    }
}