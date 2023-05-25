package cn.lanthing.sig.handler;

import cn.lanthing.codec.*;
import cn.lanthing.ltproto.signaling.SignalingMessageAckProto;
import cn.lanthing.ltproto.signaling.SignalingMessageProto;
import cn.lanthing.sig.service.Session;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@MessageType(MessageTypes.kSignalingMessage)
public class HandlerSignalingMessage implements MessageHandler, MessageCreator {

    @Override
    public void handle(Session session, LtMessage message) throws Exception {

        SignalingMessageAckProto.SignalingMessageAck.Builder ack = SignalingMessageAckProto.SignalingMessageAck.newBuilder();
        if (session.relayMessage(message)) {
            ack.setErrCode(SignalingMessageAckProto.SignalingMessageAck.ErrCode.Success);
        } else {
            ack.setErrCode(SignalingMessageAckProto.SignalingMessageAck.ErrCode.NotOnline);
        }
        LtMessage response = new LtMessage(MessageTypes.kSignalingMessageAck, ack.build());
        session.send(response);
    }

    @Override
    public Message parseFrom(byte[] bytes) {
        SignalingMessageProto.SignalingMessage msg = null;
        try {
            msg = SignalingMessageProto.SignalingMessage.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            log.warn("Parse SignalingMessage failed: {}", e.toString());
        }
        return msg;
    }
}
