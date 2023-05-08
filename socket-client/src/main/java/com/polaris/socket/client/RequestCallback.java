package com.polaris.socket.client;

public interface RequestCallback<T> {

    void success(T response);

    void fail(RuntimeException e);

}
