package cn.lanthing.svr.service.impl;

import cn.lanthing.codec.LtMessage;
import cn.lanthing.ltsocket.MessageDispatcher;
import cn.lanthing.svr.service.ControlledSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ControlledSocketServiceImpl implements ControlledSocketService {

    @Autowired
    private MessageDispatcher controlledDispatcher;

    @Override
    public void send(long connectionID, LtMessage ltMessage) {
        controlledDispatcher.send(connectionID, ltMessage);
    }
}
