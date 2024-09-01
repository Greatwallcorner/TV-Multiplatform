package com.corner.catvodcore.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;

import java.io.InputStream;
import java.nio.charset.Charset;

public class Asset {

    public static InputStream open(String fileName) {
        try {
            return FileUtil.getInputStream(fileName.replace("assets://", ""));
        } catch (Exception e) {
            return null;
        }
    }

    public static String read(String fileName) {
        try {
            return IoUtil.read(open(fileName), Charset.defaultCharset());
        } catch (Exception e) {
            return "";
        }
    }
}
