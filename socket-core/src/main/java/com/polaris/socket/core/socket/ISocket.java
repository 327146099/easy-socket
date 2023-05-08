package com.polaris.socket.core.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ISocket {

    void updateIdeaTime();

    long getLastIdeaTime();

    OutputStream getOutputStream() throws IOException;

    InputStream getInputStream() throws IOException;

    boolean isConnected();

    boolean isClosed();

    void close() throws IOException;
}
