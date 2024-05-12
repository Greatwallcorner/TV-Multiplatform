package com.corner.ui;

import java.awt.*;

/**
 * @author heatdesert
 * 2023-12-08 22:22
 */
public class Swings {

    /**
     * 屏幕像素密度
     */
    public static final int resolution = Toolkit.getDefaultToolkit().getScreenResolution();
    public static final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    private static final int lenToEdge = dp2px(35);

    /**
     * windows 底栏高度
     */
    private static final int BottomBarHeight = dp2px(78);




    public static int dp2px(int dp) {
        return dp * resolution / 160;
    }

    /**
     * 右下
     */
    public static Point screenRightDown(int winWidth, int winHeight) {
        double y = screenSize.getHeight() - winHeight - lenToEdge - BottomBarHeight;
        double x = screenSize.getWidth() - winWidth - lenToEdge;
        return new Point((int) x, (int) y);
    }

    public static Point screenRightCenter(int winWidth, int winHeight) {
        double x = screenSize.getWidth() - winWidth - lenToEdge;
        double y = (screenSize.getHeight() - BottomBarHeight) / 2 - (double) winHeight / 2;
        return new Point((int) x, (int) y);
    }


    /**
     * 中央
     * (len/2)-(winLen/2)
     */
    public static Point getCenter(int winWidth, int winHeight) {
        double x = ((screenSize.getWidth() - BottomBarHeight) / 2) - (double) winWidth / 2;
        double y = ((screenSize.getHeight() - BottomBarHeight) / 2) - (double) winHeight / 2;
        return new Point((int) x, (int) y);
    }
}


interface WINDOW_LOCATION {
    int CENTER = 0;
    int RIGHT_DOWN = 1;
    int RIGHT_CENTER = 2;
    int RIGHT_UP = 3;
}
