/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2023 Zhennan Tu <zhennan.tu@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package cn.lanthing.sig.controller;

import cn.lanthing.codec.LtMessage;
import cn.lanthing.ltproto.ErrorCodeOuterClass;
import cn.lanthing.ltproto.LtProto;
import cn.lanthing.ltproto.common.KeepAliveAckProto;
import cn.lanthing.ltproto.common.KeepAliveProto;
import cn.lanthing.ltproto.signaling.JoinRoomAckProto;
import cn.lanthing.ltproto.signaling.JoinRoomProto;
import cn.lanthing.ltproto.signaling.SignalingMessageAckProto;
import cn.lanthing.ltproto.signaling.SignalingMessageProto;
import cn.lanthing.ltsocket.ConnectionEvent;
import cn.lanthing.ltsocket.ConnectionEventType;
import cn.lanthing.ltsocket.MessageController;
import cn.lanthing.ltsocket.MessageMapping;
import cn.lanthing.sig.entity.Session;
import cn.lanthing.sig.service.RoomService;
import cn.lanthing.sig.service.SocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@MessageController
@Component
public class SignalingController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private SocketService socketService;

    @ConnectionEvent(type = ConnectionEventType.Connected)
    public void onConnectionConnected(long connectionID) {
    }

    @ConnectionEvent(type = ConnectionEventType.Closed)
    public void onConnectionClosed(long connectionID) {
        roomService.leaveRoom(connectionID);
        log.info("Connection {} leave", connectionID);
    }

    @ConnectionEvent(type = ConnectionEventType.UnexpectedlyClosed)
    public void onConnectionUnexpectedlyClosed(long connectionID) {
        //
    }

    @MessageMapping(proto = LtProto.JoinRoom)
    public LtMessage handleJoinRoom(long connectionID, JoinRoomProto.JoinRoom msg) {
        boolean success = roomService.joinRoom(connectionID, msg.getRoomId(), msg.getSessionId());
        var ack = JoinRoomAckProto.JoinRoomAck.newBuilder();
        if (success) {
            log.info("Connection {} with session id {} join room {} success", connectionID, msg.getSessionId(), msg.getRoomId());
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.Success);
        } else {
            log.error("Connection {} with session id {} join room {} failed", connectionID, msg.getSessionId(), msg.getRoomId());
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.JoinRoomFailed);
        }
        return new LtMessage(LtProto.JoinRoomAck.ID, ack.build());
    }

    @MessageMapping(proto=LtProto.SignalingMessage)
    public LtMessage handleSignalingMessage(long connectionID, SignalingMessageProto.SignalingMessage msg) {
        var ack = SignalingMessageAckProto.SignalingMessageAck.newBuilder();
        Session peer = roomService.getPeer(connectionID);
        if (peer == null) {
            log.error("Connection {} getPeer failed", connectionID);
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.SignalingPeerNotOnline);
            return new LtMessage(LtProto.SignalingMessageAck.ID, ack.build());
        }
        ack.setErrCode(ErrorCodeOuterClass.ErrorCode.Success);
        socketService.send(peer.getConnectionID(), new LtMessage(LtProto.SignalingMessage.ID, msg));
        return new LtMessage(LtProto.SignalingMessageAck.ID, ack.build());
    }

    @MessageMapping(proto = LtProto.KeepAlive)
    public LtMessage handleKeepAlive(long connectionID, KeepAliveProto.KeepAlive msg) {
        var ack  = KeepAliveAckProto.KeepAliveAck.newBuilder();
        return new LtMessage(LtProto.KeepAliveAck.ID, ack.build());
    }
}
