package com.corner.ui

import java.awt.Dimension
import java.awt.Point
import java.awt.Toolkit

/**
@author heatdesert
@date 2023-12-30 16:56
@description
 */
object SwingUtil {
    /**
     * 屏幕像素密度
     */
    val resolution: Int = Toolkit.getDefaultToolkit().screenResolution
    val screenSize: Dimension = Toolkit.getDefaultToolkit().screenSize

    private val lenToEdge = dp2px(35)

    /**
     * windows 底栏高度
     */
    private val BottomBarHeight = dp2px(78)


    fun dp2px(dp: Int): Int {
        return dp * resolution / 160
    }

    /**
     * 右下
     */
    fun screenRightDown(winWidth: Int, winHeight: Int): Point {
        val y = screenSize.getHeight() - winHeight - lenToEdge - BottomBarHeight
        val x = screenSize.getWidth() - winWidth - lenToEdge
        return Point(x.toInt(), y.toInt())
    }

    fun screenRightCenter(winWidth: Int, winHeight: Int): Point {
        val x = screenSize.getWidth() - winWidth - lenToEdge
        val y = (screenSize.getHeight() - BottomBarHeight) / 2 - winHeight.toDouble() / 2
        return Point(x.toInt(), y.toInt())
    }


    /**
     * 中央
     * (len/2)-(winLen/2)
     */
    fun getCenter(winWidth: Int, winHeight: Int): Point {
        val x = ((screenSize.getWidth() - BottomBarHeight) / 2) - winWidth.toDouble() / 2
        val y = ((screenSize.getHeight() - BottomBarHeight) / 2) - winHeight.toDouble() / 2
        return Point(x.toInt(), y.toInt())
    }
}