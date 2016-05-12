package edu.maranatha.pbol.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackgroundExecutor {

    //This executor is suitable for applications that launch many short-lived tasks.
    private static ExecutorService backgroundEx = Executors.newCachedThreadPool(); //UI thread shouldn't do math

    //pool with fixed number of "parallel" thread
    //private static int NTHREAD = Runtime.getRuntime().availableProcessors();
    //private static ExecutorService backgroundEx = Executors.newFixedThreadPool(NTHREAD); //UI thread shouldn't do math
    public BackgroundExecutor() {
    }

    public static ExecutorService get() {
        return backgroundEx;
    }
}
