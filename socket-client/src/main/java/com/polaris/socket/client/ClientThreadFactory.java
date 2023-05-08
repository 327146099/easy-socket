package com.polaris.socket.client;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientThreadFactory implements ThreadFactory {
    private static final AtomicInteger totalTfNum = new AtomicInteger(0);
    private int tfID;
    private String threadNamePrefix;
    private AtomicInteger totalThNum = new AtomicInteger(0);

    public ClientThreadFactory(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
        this.tfID = totalTfNum.incrementAndGet();
    }

    @Override
    public Thread newThread(Runnable r) {
        int tid;
        if (totalThNum.compareAndSet(1000, 1)) {
            tid = 1;
        } else {
            tid = totalThNum.incrementAndGet();
        }

        String tname = new StringBuilder(this.threadNamePrefix)
                .append(this.tfID).append("-").append(tid).toString();
        Thread thread = new Thread(r, tname);
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY);
        return thread;
    }
}
