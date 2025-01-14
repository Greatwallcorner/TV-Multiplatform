package com.github.catvod.net;

import com.github.catvod.utils.Utils;
import okhttp3.*;
import okio.BufferedSource;
import okio.Okio;

import java.io.IOException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class OkhttpInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(getRequest(chain));
        String encoding = response.header("Content-Encoding");
        if (response.body() == null || encoding == null || !encoding.equals("deflate")) return response;
        InflaterInputStream is = new InflaterInputStream(response.body().byteStream(), new Inflater(true));
        return response.newBuilder().headers(response.headers()).body(new ResponseBody() {
            @Override
            public MediaType contentType() {
                return response.body().contentType();
            }

            @Override
            public long contentLength() {
                return response.body().contentLength();
            }

            @Override
            public BufferedSource source() {
                return Okio.buffer(Okio.source(is));
            }
        }).build();
    }

    private Request getRequest(Chain chain) {
        Request request = chain.request();
        if (request.url().host().equals("gitcode.net"))
            return request.newBuilder().addHeader("User-Agent", Utils.CHROME).build();
        return request;
    }
}
