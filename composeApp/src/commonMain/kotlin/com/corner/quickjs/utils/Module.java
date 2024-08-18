package com.corner.quickjs.utils;

import cn.hutool.core.net.url.UrlPath;
import com.corner.catvodcore.util.Asset;
import com.corner.catvodcore.util.Http;
import com.corner.catvodcore.util.Paths;
import com.corner.catvodcore.util.Utils;
import com.google.common.net.HttpHeaders;
import okhttp3.Headers;
import okhttp3.Response;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Module {

    private final ConcurrentHashMap<String, String> cache;

    private static class Loader {
        static volatile Module INSTANCE = new Module();
    }

    public static Module get() {
        return Loader.INSTANCE;
    }

    public Module() {
        this.cache = new ConcurrentHashMap<>();
    }

    public String fetch(String name) {
        if (cache.contains(name)) return cache.get(name);
        if (name.startsWith("http")) cache.put(name, request(name));
        if (name.startsWith("assets")) cache.put(name, Asset.read(name));
        if (name.startsWith("lib/")) cache.put(name, Asset.read("js/" + name));
        return cache.get(name);
    }

    private String request(String url) {
        try {
            UrlPath uri = UrlPath.of(url, Charset.defaultCharset());
            List<String> segments = uri.getSegments();
            String last = segments.get(segments.size() - 1);
            File file = Paths.js(last);
            if (file.exists()) return Paths.read(file);
            Response response = Http.Companion.newCall(url, Headers.of(HttpHeaders.USER_AGENT, "Mozilla/5.0")).execute();
            if (response.code() != 200) return "";
            byte[] data = response.body().bytes();
            boolean cache = !"127.0.0.1".equals(URI.create(url).getHost());
            if (cache) new Thread(() -> Paths.write(file, data)).start();
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public byte[] bb(String content) {
        byte[] bytes = Utils.INSTANCE.decode(content.substring(4));
        byte[] newBytes = new byte[bytes.length - 4];
        newBytes[0] = 1;
        System.arraycopy(bytes, 5, newBytes, 1, bytes.length - 5);
        return newBytes;
    }
}
