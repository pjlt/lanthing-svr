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

package cn.lanthing.sig.handler;

import cn.lanthing.codec.*;
import cn.lanthing.ltproto.ErrorCodeOuterClass;
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
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.Success);
        } else {
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.SignalingPeerNotOnline);
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
