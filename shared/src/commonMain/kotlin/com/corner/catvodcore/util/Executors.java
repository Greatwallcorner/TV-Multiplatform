package com.corner.catvodcore.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Executors {

    private Executors(){}

    public static Executor getInstance(){
        return Singleton.executor;
    }

    private static class Singleton{
        private static final Executor executor = new ThreadPoolExecutor(5, 15, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(50));
    }
}
