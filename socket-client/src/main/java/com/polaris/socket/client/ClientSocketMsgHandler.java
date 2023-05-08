package com.polaris.socket.client;

public class ClientSocketMsgHandler implements SocketMsgHandler {

    @Override
    public void handle(SocketClient client, String callbackId, String body) {
        if (body == null) {
            body = "";
        }
        client.triggerCallback(callbackId, body);
    }

}
