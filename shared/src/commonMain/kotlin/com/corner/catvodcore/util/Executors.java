package com.corner.catvodcore.util;

import java.util.concurrent.*;

/**
 * @author heatdesert
 * @date 2023-11-20 22:23
 * @description
 */
public class Executors {

    private Executors(){}

    public static Executor getInstance(){
        return Singleton.executor;
    }

    private static class Singleton{
        private static final Executor executor = new ThreadPoolExecutor(5, 15, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(50));
    }
}
