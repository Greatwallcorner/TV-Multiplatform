package com.corner.quickjs.utils;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class JSUtil {

    public static NativeArray toArray(Context ctx, ScriptableObject scope, List<String> items) {
        return (NativeArray)ctx.newArray(scope, items.toArray());
    }

    public static Scriptable toArray(Context ctx, ScriptableObject scope, byte[] bytes) {
        Scriptable array = ctx.newArray(scope, 0);
        if (bytes == null || bytes.length == 0) return array;
        for (int i = 0; i < bytes.length; i++) array.put(i, array, (int) bytes[i]);
        return array;
    }

    public static Scriptable toObj(Context ctx,ScriptableObject scope, Map<String, String> map) {
        Scriptable obj = ctx.newObject(scope);
        if (map == null || map.isEmpty()) return obj;
        for (String s : map.keySet()) obj.put(s, obj, map.get(s));
        return obj;
    }

    /**
     *
     * @param charset
     * @param buffer 数组
     * @return
     * @throws CharacterCodingException
     */
    public static String decodeTo(String charset, Scriptable buffer) throws CharacterCodingException {
        int length = (int) ((NativeArray) buffer).getLength();
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) bytes[i] = (byte) (int) buffer.get(i, buffer);
        return Charset.forName(charset).newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
    }
}
