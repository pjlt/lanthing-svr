package cn.lanthing.svr.service;

import cn.lanthing.codec.LtMessage;

public interface ControllingSocketService {
    void send(long connectionID, LtMessage ltMessage);
}
