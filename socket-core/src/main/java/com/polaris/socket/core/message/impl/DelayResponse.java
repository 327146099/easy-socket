package com.polaris.socket.core.message.impl;

import com.polaris.socket.core.message.base.SuperResponse;
import lombok.Data;

@Data
public class DelayResponse extends SuperResponse {

    private String from;

}
