package com.polaris.socket.core.message.impl;

import com.polaris.socket.core.message.base.SuperResponse;
import lombok.Data;

@Data
public class ServerHeartBeat extends SuperResponse {

    private String from;

}
