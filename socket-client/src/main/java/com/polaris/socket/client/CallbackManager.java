package com.polaris.socket.client;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CallbackManager<T> {
    public static final String THREAD_NAME_PREFIX = "Callback-Handler-";
    public static final long DEFAULT_TIMEOUT = 30;
    public static final TimeUnit DEFAULT_TIME_UINIT = TimeUnit.SECONDS;

    private final ThreadFactory threadFactory = new ClientThreadFactory(THREAD_NAME_PREFIX);
    private final ConcurrentHashMap<String, ArrayBlockingQueue<T>> callbackBindMap = new ConcurrentHashMap<>();
    private long timeout = DEFAULT_TIMEOUT;
    private TimeUnit unit = DEFAULT_TIME_UINIT;

    public CallbackManager() {
    }

    public CallbackManager(long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
    }

    public <T> void register(String reqId, RequestCallback callback) {
        ArrayBlockingQueue sq = new ArrayBlockingQueue<>(1);

        if (this.callbackBindMap.putIfAbsent(reqId, sq) != null) {
            throw new IllegalArgumentException("不能重复注册请求！");
        }

        CallbackTask<T> task = new CallbackTask<>(this.callbackBindMap, reqId, callback, this.timeout, this.unit);
        Thread taskThread = this.threadFactory.newThread(task);
        taskThread.start();
    }

    public void trigger(String reqId, T response) {
        ArrayBlockingQueue<T> sq = this.callbackBindMap.get(reqId);

        if (sq != null) {
            if(response!=null){
                sq.offer(response);
            }
            log.debug("触发请求回调，响应：{}", response);
        } else {
            log.debug("没有找到注册回调，忽略响应：{}", response);
        }
    }

    public void clear() {
        this.callbackBindMap.clear();
    }


    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    public void setUnit(TimeUnit unit) {
        this.unit = unit;
    }
}
