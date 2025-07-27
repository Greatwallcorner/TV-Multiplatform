package com.corner.util

import org.slf4j.LoggerFactory
import java.util.regex.Pattern

class M3U8Filter {
    private val log = LoggerFactory.getLogger("M3U8FilterAd")
    private var tsNameLen = 0
    private var tsNameLenExtend = 1
    private var firstExtinfRow = ""
    private var theExtinfJudgeRowN = 0
    private var theSameExtinfNameN = 0
    private var theExtinfBenchmarkN = 5
    private var prevTsNameIndex = -1
    private var firstTsNameIndex = -1
    private var tsType = 0
    private var theExtXMode = 0
    private var scriptWhitelistModeFlag = false
    private var theCurrentHostInWhitelistFlag = false
    private var showToastTipFlag = false
    private var violentFilterModeFlag = false
    private var filterLogHtml = ""
    private val whitelist = mutableSetOf<String>() // 白名单

    private fun filterLog(vararg msg: String) {
        val logContent = msg.joinToString("</p><p>")
        log.info("[m3u8_filter_ad] {}", logContent)
        filterLogHtml += "<p><span style=\"font-weight: bold; color: white; background-color: #70b566b0; padding: 2px; border-radius: 2px;\">[m3u8_filter_ad]</span>$logContent</p>"
    }

    private fun isM3U8File(url: String): Boolean {
        return Pattern.compile("\\.m3u8($|\\?)").matcher(url).find()
    }

    private fun extractNumberBeforeTs(str: String): Int? {
        val pattern = Pattern.compile("(\\d+)\\.ts")
        val matcher = pattern.matcher(str)
        return if (matcher.find()) {
            matcher.group(1).toInt()
        } else {
            null
        }
    }

    fun setWhitelist(whitelist: Set<String>) {
        this.whitelist.addAll(whitelist)
    }

    fun enableWhitelistMode(enable: Boolean) {
        this.scriptWhitelistModeFlag = enable
    }

    fun setViolentFilterMode(enable: Boolean) {
        this.violentFilterModeFlag = enable
    }

    fun filterLines(lines: List<String>, currentHost: String): List<String> {
        // 白名单处理
        if (scriptWhitelistModeFlag) {
            theCurrentHostInWhitelistFlag = whitelist.contains(currentHost)
            if (theCurrentHostInWhitelistFlag) {
                filterLog("当前域名 $currentHost 在白名单中，不进行过滤")
                return lines
            }
        }

        val result = mutableListOf<String>()
        if (violentFilterModeFlag) {
            filterLog("----------------------------暴力拆解模式--------------------------")
            tsType = 2
        } else {
            filterLog("----------------------------自动判断模式--------------------------")
            var theNormalIntTsN = 0
            var theDiffIntTsN = 0
            var lastTsNameLen = 0

            // 初始化参数
            for (i in lines.indices) {
                val line = lines[i]

                // 完整的 EXTINF 处理逻辑
                if (line.startsWith("#EXTINF")) {
                    if (theExtinfJudgeRowN == 0) {
                        firstExtinfRow = line
                        theExtinfJudgeRowN++
                    } else if (theExtinfJudgeRowN == 1) {
                        if (line == firstExtinfRow) {
                            theSameExtinfNameN++
                        } else {
                            firstExtinfRow = ""
                        }
                        theExtinfJudgeRowN++
                    } else if (theSameExtinfNameN >= theExtinfBenchmarkN) {
                        // 连续相同的 EXTINF 可能是广告段
                        filterLog("检测到可能的广告段，连续相同的 EXTINF")
                    }
                }

                // 判断 ts 模式
                val theTsNameLen = line.indexOf(".ts")
                if (theTsNameLen > 0) {
                    if (theExtinfJudgeRowN == 1) {
                        tsNameLen = theTsNameLen
                    }
                    lastTsNameLen = theTsNameLen

                    val tsNameIndex = extractNumberBeforeTs(line)
                    if (tsNameIndex == null) {
                        if (theExtinfJudgeRowN == 1) {
                            tsType = 1
                        } else if (theExtinfJudgeRowN == 2 && (tsType == 1 || theTsNameLen == tsNameLen)) {
                            tsType = 1
                            filterLog("----------------------------识别ts模式1---------------------------")
                            break
                        } else {
                            theDiffIntTsN++
                        }
                    } else {
                        if (theNormalIntTsN == 0) {
                            // 初始化 ts 序列号
                            prevTsNameIndex = tsNameIndex
                            firstTsNameIndex = tsNameIndex
                            prevTsNameIndex = firstTsNameIndex - 1
                        }

                        if (theTsNameLen != tsNameLen) {
                            if (theTsNameLen == lastTsNameLen + 1 && tsNameIndex == prevTsNameIndex + 1) {
                                if (theDiffIntTsN > 0) {
                                    if (tsNameIndex == prevTsNameIndex + 1) {
                                        tsType = 0
                                        prevTsNameIndex = firstTsNameIndex - 1
                                        filterLog("----------------------------识别ts模式0---------------------------")
                                        break
                                    } else {
                                        tsType = 2
                                        filterLog("----------------------------识别ts模式2---------------------------")
                                        break
                                    }
                                }
                                theNormalIntTsN++
                                prevTsNameIndex = tsNameIndex
                            }
                        }
                    }
                }
            }
        }

        var inAdSegment = false
        var lastExtinfRow: String? = null

        // 根据识别的 tsType 实现具体的过滤逻辑，同时处理 EXT-X-DISCONTINUITY
        for (i in lines.indices) {
            val line = lines[i]
            if (line.startsWith("#EXT-X-DISCONTINUITY")) {
                // 处理不连续标记，可能是广告段开始
                inAdSegment = true
                filterLog("检测到 EXT-X-DISCONTINUITY，可能进入广告段")
                continue
            }

            if (line.startsWith("#EXTINF")) {
                if (lastExtinfRow != null && line == lastExtinfRow) {
                    // 连续相同的 EXTINF 可能是广告段
                    inAdSegment = true
                    filterLog("检测到连续相同的 EXTINF，可能进入广告段")
                }
                lastExtinfRow = line
            }

            if (line.contains(".ts")) {
                when (tsType) {
                    0 -> {
                        val tsNameIndex = extractNumberBeforeTs(line)
                        if (tsNameIndex == null || !inAdSegment && tsNameIndex == firstTsNameIndex) {
                            result.add(line)
                            firstTsNameIndex++
                        }
                    }
                    1 -> {
                        if (!inAdSegment) {
                            result.add(line)
                        }
                    }
                    2 -> {
                        val adKeywords = listOf("ad", "advertisement")
                        if (adKeywords.none { line.contains(it, ignoreCase = true) } && !inAdSegment) {
                            result.add(line)
                        }
                    }
                }
            } else {
                if (!inAdSegment) {
                    result.add(line)
                }
            }

            // 广告段结束判断，可根据实际情况调整
            if (inAdSegment && line.startsWith("#EXT-X-ENDLIST")) {
                inAdSegment = false
                filterLog("广告段结束")
            }
        }
        return result
    }
}