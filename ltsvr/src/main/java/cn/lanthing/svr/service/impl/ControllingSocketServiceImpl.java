package cn.lanthing.svr.service.impl;

import cn.lanthing.codec.LtMessage;
import cn.lanthing.ltsocket.MessageDispatcher;
import cn.lanthing.svr.service.ControllingSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ControllingSocketServiceImpl implements ControllingSocketService {
    @Autowired
    private MessageDispatcher controllingDispatcher;
    @Override
    public void send(long connectionID, LtMessage ltMessage) {
        controllingDispatcher.send(connectionID, ltMessage);
    }
}
