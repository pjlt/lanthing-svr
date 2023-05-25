package cn.lanthing.svr.service;

import cn.lanthing.codec.LtMessage;

public interface ControlledSocketService {
    void send(long connectionID, LtMessage ltMessage);
}
