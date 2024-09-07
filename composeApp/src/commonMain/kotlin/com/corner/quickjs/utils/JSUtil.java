package com.corner.quickjs.utils;

import com.sun.xml.bind.v2.schemagen.xmlschema.Any;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class JSUtil {

//    public static JSArr toArray(QuickJs quickJs, List<String> items) {
//
//        return (NativeArray)ctx.newArray(scope, items.toArray());
//    }

    public static List<Object> toArray(byte[] bytes) {
        List<Object> list = new java.util.ArrayList<>();
        if (bytes == null) return list;
        for (byte aByte : bytes) list.add((int) aByte);
        return list;
    }

//    public static Scriptable toObj(Context ctx,ScriptableObject scope, Map<String, String> map) {
//        Scriptable obj = ctx.newObject(scope);
//        if (map == null || map.isEmpty()) return obj;
//        for (String s : map.keySet()) obj.put(s, obj, map.get(s));
//        return obj;
//    }

//    /**
//     *
//     * @param charset
//     * @param buffer 数组
//     * @return
//     * @throws CharacterCodingException
//     */
    public static String decodeTo(String charset, ArrayList<Object> buffer) throws CharacterCodingException {
        int length = (int) buffer.size();
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) bytes[i] = (byte) (int) buffer.get(i);
        return Charset.forName(charset).newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
    }
}
