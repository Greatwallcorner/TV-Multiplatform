package com.corner.ui

import java.awt.Cursor
import java.awt.Dimension
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage
import javax.swing.SwingUtilities

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

    fun hideCursor(): Cursor {
        val toolkit = Toolkit.getDefaultToolkit()
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        return  toolkit.createCustomCursor(image, Point(0, 0), "InvisibleCursor")
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

    fun <T> runOnUiThread(block: () -> T): T {
        if (SwingUtilities.isEventDispatchThread()) {
            return block()
        }

        var error: Throwable? = null
        var result: T? = null

        SwingUtilities.invokeAndWait {
            try {
                result = block()
            } catch (e: Throwable) {
                error = e
            }
        }

        error?.also { throw it }

        @Suppress("UNCHECKED_CAST")
        return result as T
    }
}