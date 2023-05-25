package cn.lanthing.sig.handler;

import cn.lanthing.codec.LtMessage;
import cn.lanthing.sig.service.Session;

public interface MessageHandler {
    void handle(Session session, LtMessage message) throws Exception;
}
