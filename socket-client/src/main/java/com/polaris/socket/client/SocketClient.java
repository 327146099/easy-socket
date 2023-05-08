package com.polaris.socket.client;

import com.polaris.socket.core.message.base.SuperClient;
import com.polaris.socket.core.protocol.IMessageProtocol;
import com.polaris.socket.core.protocol.impl.DefaultMessageProtocol;
import com.polaris.socket.core.socket.ISocket;
import com.polaris.socket.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SocketClient {

    //检查连接状态，自动重连周期
    private long checkPeriod = 3;
    //发送心跳周期
    private long heartbeatPeriod = 15;

    private Thread connectionThread = null;
    private Thread receiveThread = null;
    private Thread heartbeatThread = null;

    private ISocket clientSocket = null;

    private final CountDownLatch closeSignal = new CountDownLatch(1);

    private SocketMsgHandler socketMsgHandler;

    private IMessageProtocol iMessageProtocol = new DefaultMessageProtocol();

    private SocketFactory socketFactory;

    private CallbackManager<String> callbackManager;

    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

    public SocketClient(SocketMsgHandler socketMsgHandler, SocketFactory socketFactory, long checkPeriod, long heartbeatPeriod) {
        this.socketMsgHandler = socketMsgHandler;
        this.socketFactory = socketFactory;
        this.callbackManager = new CallbackManager<>();
    }

    public synchronized void connect() {
        if (connectionThread != null) {
            return;
        }

        SocketClient self = this;

        //启动自动重连线程
        connectionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    log.debug("----------------------------------------");
                    log.debug("检查网络连接...");
                    if (clientSocket == null || clientSocket.isClosed()) {
                        log.debug("网络连接未初始化或已关闭。");
                        initConnect();
                    } else if (!clientSocket.isConnected()) {
                        log.debug("网络未连接。");
                        reconnect();
                    } else {
                        log.debug("网络连接正常。");
                    }

                    log.debug("检查网络连接结束。");

                    if (isClientClosed(checkPeriod, TimeUnit.SECONDS)) {
                        return;
                    }
                }
            }
        }, "SOCKET-ClientKeeper");

        connectionThread.setDaemon(true);
        connectionThread.start();

        receiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                log.debug("接收线程启动。");

                while (true) {
                    if (isClientClosed(3L, TimeUnit.SECONDS)) {
                        log.debug("接收线程关闭。");
                        return;
                    }

                    if (clientSocket == null || !clientSocket.isConnected() || clientSocket.isClosed()) {
                        continue;
                    }

                    try (InputStream in = new BufferedInputStream(clientSocket.getInputStream())) {

                        while (true) {
                            int HEADER_PREFETCH_SIZE = iMessageProtocol.getHeaderLength();
                            byte[] prefetch = readNBytes(in, HEADER_PREFETCH_SIZE);

                            if (prefetch.length < HEADER_PREFETCH_SIZE) {
                                //流中断
                                break;
                            }

                            if (isClientClosed(0L, TimeUnit.SECONDS)) {
                                log.debug("接收线程关闭。");
                                return;
                            }

                            int bodyLen = iMessageProtocol.getBodyLength(prefetch, ByteOrder.BIG_ENDIAN);
                            // 读取消息体
                            byte[] msgBody = readNBytes(in, bodyLen);

                            if (msgBody.length < bodyLen) {
                                //流中断
                                break;
                            }
                            SuperClient superClient = JsonUtils.fromJson(new String(msgBody, StandardCharsets.UTF_8), SuperClient.class);
                            String msgId = superClient.getMsgId();
                            switch (msgId) {
                                case "heart_beat":
                                    log.info("接收到心跳报文{}，长度：{}", superClient, msgBody.length);
                                    break;
                                case "biz_msg":
                                    if (socketMsgHandler != null) {
                                        socketMsgHandler.handle(self, superClient.getCallbackId(), superClient.getBody());
                                    }
                                    break;
                                default:
                                    log.debug("接收到未知报文，长度：{}", msgBody.length);
                                    break;
                            }
                        }
                    } catch (IOException e) {
                        log.debug("读取输入流失败！");
                    }
                    //do nothing
                }
            }
        }, "TCP-Receive");

        receiveThread.setDaemon(true);
        receiveThread.start();

        //启动心跳线程
        heartbeatThread = new Thread(() -> {
            log.debug("心跳线程启动。");

            while (true) {
                if (isClientClosed(3L, TimeUnit.SECONDS)) {
                    log.debug("心跳线程关闭。");
                    return;
                }

                if (clientSocket == null || !clientSocket.isConnected() || clientSocket.isClosed()) {
                    continue;
                }

                try {
                    while (true) {
                        SuperClient clientMsg = new SuperClient();
                        clientMsg.setMsgId("heart_beat");
                        sendMsg(clientMsg, "heart_beat", null);

                        if (isClientClosed(heartbeatPeriod, TimeUnit.SECONDS)) {
                            log.debug("心跳线程关闭。");
                            return;
                        }
                    }
                } catch (IOException e) {
                    log.debug("写入输出流失败！");
                } finally {
                    closeSocket();
                }
            }
        }, "TCP-Heartbeat");

        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    public void connectSync() throws InterruptedException {
        this.connect();
        this.closeSignal.await();
    }

    public void triggerCallback(String callbackId, String result) {
        this.callbackManager.trigger(callbackId, result);
    }


    public void callRequest(Object body, RequestCallback<String> callback) {
        try {
            String msgId = UUID.randomUUID().toString();
            this.callbackManager.register(msgId, callback);
            this.sendMsg(body, "biz_msg", msgId);
        } catch (IOException e) {
            callback.fail(new RuntimeException(e));
        } catch (RuntimeException e) {
            callback.fail(e);
        }
    }

    public synchronized void close() {
        closeSignal.countDown();
        this.callbackManager.clear();
        closeSocket();
    }

    public void sendRequest(Object message) throws IOException {
        this.sendMsg(JsonUtils.toJsonBytes(message), "biz_msg", null);
    }

    public void sendRequest(Object message, String callbackId) throws IOException {
        this.sendMsg(JsonUtils.toJsonBytes(message), "biz_msg", callbackId);
    }

    private String sendMsg(Object msgBase, String msgId, String callbackId) throws IOException {
        if (isClientClosed(0L, TimeUnit.SECONDS)) {
            throw new IOException("客户端已关闭。");
        }

        if (this.clientSocket == null || !this.clientSocket.isConnected() || this.clientSocket.isClosed()) {
            throw new IOException("客户端已关闭或者还未连接。");
        }
        if (callbackId == null) {
            callbackId = UUID.randomUUID().toString();
        }
        SuperClient msg = new SuperClient();
        msg.setMsgId(msgId);
        msg.setBody(JsonUtils.toJson(msgBase));
        msg.setCallbackId(callbackId);

        if (iMessageProtocol != null) {
            byte[] bytes = JsonUtils.toJsonBytes(msg);
            byte[] pack = iMessageProtocol.pack(bytes);
            safeWriteSocket(pack);
        } else {
            safeWriteSocket(JsonUtils.toJsonBytes(msg));
        }

        log.debug("发送{}消息结束，ID：{}", JsonUtils.toJson(msgBase), msg.getCallbackId());
        return callbackId;
    }

    private synchronized void reconnect() {
        boolean closed = false;
        try {
            closed = closeSignal.await(0, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //do nothing
        }

        if (closed) {
            return;
        }

        closeSocket();
        doConnect();
    }

    private synchronized void initConnect() {
        boolean closed = false;
        try {
            closed = closeSignal.await(0, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //do nothing
        }

        if (closed) {
            return;
        }

        doConnect();
    }

    private void doConnect() {
        try {
            clientSocket = socketFactory.build();
            log.debug("创建网络连接成功。");
        } catch (Throwable e) {
            log.error("创建网络连接失败！");
        }
    }

    private synchronized void safeWriteSocket(byte[] msgPack) throws IOException {
        OutputStream out = new BufferedOutputStream(this.clientSocket.getOutputStream());
        out.write(msgPack);
        out.flush();
    }

    private byte[] readNBytes(InputStream in, int len) throws IOException {
        if (len < 0) {
            throw new IllegalArgumentException("len < 0");
        }

        List<byte[]> bufs = null;
        byte[] result = null;
        int total = 0;
        int remaining = len;
        int n;
        do {
            byte[] buf = new byte[Math.min(remaining, DEFAULT_BUFFER_SIZE)];
            int nread = 0;

            // read to EOF which may read more or less than buffer size
            while ((n = in.read(buf, nread, Math.min(buf.length - nread, remaining))) > 0) {
                nread += n;
                remaining -= n;
            }

            if (nread > 0) {
                if (MAX_BUFFER_SIZE - total < nread) {
                    throw new OutOfMemoryError("Required array size too large");
                }
                total += nread;
                if (result == null) {
                    result = buf;
                } else {
                    if (bufs == null) {
                        bufs = new ArrayList<>();
                        bufs.add(result);
                    }
                    bufs.add(buf);
                }
            }
            // if the last call to read returned -1 or the number of bytes
            // requested have been read then break
        } while (n >= 0 && remaining > 0);

        if (bufs == null) {
            if (result == null) {
                return new byte[0];
            }
            return result.length == total ? result : Arrays.copyOf(result, total);
        }

        result = new byte[total];
        int offset = 0;
        remaining = total;
        for (byte[] b : bufs) {
            int count = Math.min(b.length, remaining);
            System.arraycopy(b, 0, result, offset, count);
            offset += count;
            remaining -= count;
        }

        return result;
    }

    private void closeSocket() {
        if (this.clientSocket != null && !this.clientSocket.isClosed()) {
            try {
                this.clientSocket.close();
            } catch (IOException e) {
                //do nothing
            }
        }
    }

    private boolean isClientClosed(Long timeout, TimeUnit timeUnit) {
        boolean closed = false;

        try {
            closed = closeSignal.await(timeout, timeUnit);
        } catch (InterruptedException e) {
            //do nothing
        }

        return closed;
    }

    public void setCheckPeriod(long checkPeriod) {
        this.checkPeriod = checkPeriod;
    }

    public void setHeartbeatPeriod(long heartbeatPeriod) {
        this.heartbeatPeriod = heartbeatPeriod;
    }
}
