package com.corner.quickjs.utils;

import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class JSUtil {

    public static JSArray toArray(QuickJSContext ctx, List<String> items) {
        JSArray array = ctx.createNewJSArray();
        if (items == null || items.isEmpty()) return array;
        for (int i = 0; i < items.size(); i++) array.set(items.get(i), i);
        return array;
    }

    public static JSArray toArray(QuickJSContext ctx, byte[] bytes) {
        JSArray array = ctx.createNewJSArray();
        if (bytes == null || bytes.length == 0) return array;
        for (int i = 0; i < bytes.length; i++) array.set((int) bytes[i], i);
        return array;
    }

    public static JSObject toObj(QuickJSContext ctx, Map<String, String> map) {
        JSObject obj = ctx.createNewJSObject();
        if (map == null || map.isEmpty()) return obj;
        for (String s : map.keySet()) obj.setProperty(s, map.get(s));
        return obj;
    }

    public static String decodeTo(String charset, JSArray buffer) throws CharacterCodingException {
        byte[] bytes = new byte[buffer.length()];
        for (int i = 0; i < buffer.length(); i++) bytes[i] = (byte) (int) buffer.get(i);
        return Charset.forName(charset).newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
    }
}
