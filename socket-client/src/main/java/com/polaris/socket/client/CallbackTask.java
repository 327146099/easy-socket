package com.polaris.socket.client;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CallbackTask<T> implements Runnable {
    private final RequestCallback<T> callback;
    private final ConcurrentHashMap<String, ArrayBlockingQueue<T>> bindingMap;
    private String reqId;
    private long timeout;
    private TimeUnit unit;

    public CallbackTask(ConcurrentHashMap<String, ArrayBlockingQueue<T>> bindingMap, String reqId, RequestCallback<T> callback, long timeout, TimeUnit unit) {
        this.bindingMap = bindingMap;
        this.callback = callback;
        this.reqId = reqId;
        this.timeout = timeout;
        this.unit = unit;
    }

    @Override
    public void run() {
        ArrayBlockingQueue<T> sq = this.bindingMap.get(this.reqId);
        T res = null;

        try {
            res = sq.poll(this.timeout, this.unit);
        } catch (InterruptedException e) {
            RuntimeException err = new RuntimeException("线程被中断！", e);
            this.callback.fail(err);
            return;
        } finally {
            this.clearBinding();
        }

        if (res == null) {
            RuntimeException err = new RuntimeException("等待超时！");
            this.callback.fail(err);
            return;
        }

        try {
            T regRes = (T) res;
            this.callback.success(regRes);
        } catch (ClassCastException e) {
            this.callback.fail(e);
        }

    }

    private void clearBinding() {
        this.bindingMap.remove(this.reqId);
    }
}
