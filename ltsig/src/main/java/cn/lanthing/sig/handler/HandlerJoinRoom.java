package cn.lanthing.sig.handler;

import cn.lanthing.codec.*;
import cn.lanthing.ltproto.signaling.JoinRoomAckProto;
import cn.lanthing.ltproto.signaling.JoinRoomProto;
import cn.lanthing.sig.service.Session;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@MessageType(MessageTypes.kJoinRoom)
public class HandlerJoinRoom implements MessageHandler, MessageCreator {

    @Override
    public void handle(Session session, LtMessage message) throws Exception {
        JoinRoomProto.JoinRoom msg = (JoinRoomProto.JoinRoom) message.protoMsg;
        JoinRoomAckProto.JoinRoomAck.Builder ack = JoinRoomAckProto.JoinRoomAck.newBuilder();
        if (session.joinRoom(msg.getRoomId(), msg.getSessionId())) {
            log.info("Session '{}' join room '{}' success", msg.getSessionId(), msg.getRoomId());
            ack.setErrCode(JoinRoomAckProto.JoinRoomAck.ErrCode.Success);
        } else {
            log.warn("Session '{}' join room '{}' failed", msg.getSessionId(), msg.getRoomId());
            ack.setErrCode(JoinRoomAckProto.JoinRoomAck.ErrCode.Failed);
        }
        LtMessage response = new LtMessage(MessageTypes.kJoinRoomAck, ack.build());
        session.send(response);
    }

    @Override
    public Message parseFrom(byte[] bytes) {
        JoinRoomProto.JoinRoom msg = null;
        try {
            msg = JoinRoomProto.JoinRoom.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            log.warn("Parse JoinRoom failed: {}", e.toString());
        }
        return msg;
    }
}
