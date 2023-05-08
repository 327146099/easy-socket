package com.polaris.socket.core.socket.impl;

import com.polaris.socket.core.socket.ISocket;

public abstract class IdeaSocket implements ISocket {

    private long ideaTime;

    @Override
    public void updateIdeaTime() {
        this.ideaTime = System.currentTimeMillis();
    }

    @Override
    public long getLastIdeaTime() {
        return ideaTime;
    }
}
