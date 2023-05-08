package com.polaris.socket.client;

public interface SocketMsgHandler {

    void handle(SocketClient client, String callbackId, String body);

}
