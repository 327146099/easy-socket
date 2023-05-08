package com.polaris.socket.server;

import com.google.gson.Gson;
import com.polaris.socket.core.message.base.SuperClient;
import com.polaris.socket.core.message.base.SuperResponse;
import com.polaris.socket.core.message.impl.CallbackResponse;
import com.polaris.socket.core.message.impl.DelayResponse;
import com.polaris.socket.core.message.impl.ServerHeartBeat;
import com.polaris.socket.core.protocol.IMessageProtocol;
import com.polaris.socket.server.entity.MessageID;
import com.polaris.socket.server.iowork.IWriter;
import com.polaris.socket.server.listener.MessageListener;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class HandlerIO {

    private final IWriter easyWriter;
    private final IMessageProtocol messageProtocol;

    private final MessageListener messageListener;

    public HandlerIO(IWriter easyWriter, MessageListener messageListener) {
        this.easyWriter = easyWriter;
        messageProtocol = ServerConfig.getInstance().getMessageProtocol();
        this.messageListener = messageListener;
    }

    /**
     * 处理接收的信息
     */
    public void handReceiveMsg(String receiver) {
        try {
            System.out.println("receive message:" + receiver);
            SuperClient clientMsg = new Gson().fromJson(receiver, SuperClient.class);
            //消息ID 用于区分消息类型
            String id = clientMsg.getMsgId();
            String callbackId = clientMsg.getCallbackId(); //回调ID
            SuperResponse superResponse = null;
            switch (id) {
                case MessageID.HEARTBEAT: //心跳包
                    superResponse = new ServerHeartBeat();
                    ((ServerHeartBeat) superResponse).setFrom("server");
                    superResponse.setMsgId(MessageID.HEARTBEAT);
                    break;
                case MessageID.DELAY_MSG: //延时消息
                    superResponse = new DelayResponse();
                    (superResponse).setCallbackId(callbackId);
                    superResponse.setMsgId(MessageID.DELAY_MSG);
                    ((DelayResponse) superResponse).setFrom("server");
                    try {
                        Thread.sleep(1000 * 5);
                    } catch (InterruptedException ignored) {
                    }
                    break;
                case MessageID.CALLBACK_MSG: //回调消息
                    superResponse = new CallbackResponse();
                    (superResponse).setCallbackId(callbackId);
                    superResponse.setMsgId(MessageID.CALLBACK_MSG);
                    ((CallbackResponse) superResponse).setFrom("我来自server");
                    if (messageListener != null) {
                        String result = messageListener.receiveMessage(clientMsg);
                        superResponse.setBody(result);
                    }
                    break;
            }

            if (superResponse == null) {
                return;
            }
            log.info("send message:" + convertObjectToJson(superResponse));
            byte[] bytes = convertObjectToJson(superResponse).getBytes(StandardCharsets.UTF_8);
            // 自定义消息协议
            if (messageProtocol != null) {
                bytes = messageProtocol.pack(bytes);
            }
            easyWriter.offer(bytes);
        } catch (Exception e) {
            System.out.println("可能收到非Json格式的数据");
            e.printStackTrace();
        }
    }


    private String convertObjectToJson(Object object) {
        Gson gson = new Gson();
        return gson.toJson(object);
    }
}
