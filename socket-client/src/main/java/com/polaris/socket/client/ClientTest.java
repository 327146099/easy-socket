package com.polaris.socket.client;

import com.polaris.socket.core.socket.ISocket;
import com.polaris.socket.core.socket.impl.NetworkSocket;
import com.polaris.socket.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ClientTest {

    public static void main(String[] args) throws InterruptedException {
        SocketClient socketClient = new SocketClient(new ClientSocketMsgHandler(), () -> {
            try {
                Socket socket = new Socket("127.0.0.1", 9998);
                return new NetworkSocket(socket);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, 3, 15);
        socketClient.connect();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                HashMap<String, Object> params = new HashMap<>();
                params.put("a", 1231231);
                params.put("b", 12312222);
                socketClient.callRequest(params, new RequestCallback<String>() {
                    @Override
                    public void success(String response) {
                        System.out.println(JsonUtils.toJson(response));
                    }

                    @Override
                    public void fail(RuntimeException e) {
                        e.printStackTrace();
                    }
                });
            }
        }, 10, 10000);

        TimeUnit.SECONDS.sleep(60);

    }
}
