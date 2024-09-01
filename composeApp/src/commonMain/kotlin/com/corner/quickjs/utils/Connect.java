package com.corner.quickjs.utils;

import com.corner.catvodcore.util.Http;
import com.corner.catvodcore.util.Json;
import com.corner.catvodcore.util.Utils;
import com.corner.quickjs.bean.Req;
import com.dokar.quickjs.binding.JsObject;
import com.google.common.net.HttpHeaders;
import okhttp3.*;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Connect {

    public static Call to(String url, Req req) {
        OkHttpClient client = Http.Companion.client(req.isRedirect(), req.getTimeout());
        return client.newCall(getRequest(url, req, Headers.of(req.getHeader())));
    }

    public static JsObject success(Req req, Response res) {
        try {
            JsObject object = new JsObject(new HashMap<>());
            JsObject header = new JsObject(new HashMap<>());
            setHeader(res, header);
            object.put("code", res.code());
            object.put("headers", header);
            if (req.getBuffer() == 0) object.put("content", new String(res.body().bytes(), req.getCharset()));
            if (req.getBuffer() == 1) object.put("content", JSUtil.toArray(res.body().bytes()));
            if (req.getBuffer() == 2) object.put("content", Utils.INSTANCE.base64(res.body().bytes()));
            return object;
        } catch (Exception e) {
            return error();
        }
    }

    public static JsObject error() {
        JsObject object = new JsObject(new HashMap<>());
        JsObject jsHeader = new JsObject(new HashMap<>());
        object.put("headers", jsHeader);
        object.put("content", "");
        object.put("code",  "");
        return object;
    }

    private static Request getRequest(String url, Req req, Headers headers) {
        if (req.getMethod().equalsIgnoreCase("post")) {
            return new Request.Builder().url(url).headers(headers).post(getPostBody(req, headers.get(HttpHeaders.CONTENT_TYPE))).build();
        } else if (req.getMethod().equalsIgnoreCase("header")) {
            return new Request.Builder().url(url).headers(headers).head().build();
        } else {
            return new Request.Builder().url(url).headers(headers).get().build();
        }
    }

    private static RequestBody getPostBody(Req req, String contentType) {
        if (req.getData() != null && "json".equals(req.getPostType())) return getJsonBody(req);
        if (req.getData() != null && "form".equals(req.getPostType())) return getFormBody(req);
        if (req.getData() != null && "form-data".equals(req.getPostType())) return getFormDataBody(req);
        if (req.getBody() != null && contentType != null) return RequestBody.create(req.getBody(), MediaType.get(contentType));
        return RequestBody.create(new byte[0]);
    }

    private static RequestBody getJsonBody(Req req) {
        return RequestBody.create(req.getData().toString(), MediaType.get("application/json; charset=utf-8"));
    }

    private static RequestBody getFormBody(Req req) {
        FormBody.Builder builder = new FormBody.Builder();
        Map<String, String> params = Json.toMap(req.getData());
        for (String key : params.keySet()) builder.add(key, params.get(key));
        return builder.build();
    }

    private static RequestBody getFormDataBody(Req req) {
        String boundary = "--dio-boundary-" + new SecureRandom().nextInt(42949) + "" + new SecureRandom().nextInt(67296);
        MultipartBody.Builder builder = new MultipartBody.Builder(boundary).setType(MultipartBody.FORM);
        Map<String, String> params = Json.toMap(req.getData());
        for (String key : params.keySet()) builder.addFormDataPart(key, params.get(key));
        return builder.build();
    }

    private static void setHeader(Response res, JsObject object) {
        for (Map.Entry<String, List<String>> entry : res.headers().toMultimap().entrySet()) {
            if (entry.getValue().size() == 1) object.put(entry.getKey(), entry.getValue().get(0));
            if (entry.getValue().size() >= 2) object.put(entry.getKey(), entry.getValue());
        }
    }
}
