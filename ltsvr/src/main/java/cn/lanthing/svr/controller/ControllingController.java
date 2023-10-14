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

package cn.lanthing.svr.controller;

import cn.lanthing.codec.LtMessage;
import cn.lanthing.ltproto.ErrorCodeOuterClass;
import cn.lanthing.ltproto.LtProto;
import cn.lanthing.ltproto.server.*;
import cn.lanthing.ltsocket.ConnectionEvent;
import cn.lanthing.ltsocket.ConnectionEventType;
import cn.lanthing.ltsocket.MessageController;
import cn.lanthing.ltsocket.MessageMapping;
import cn.lanthing.svr.entity.OrderInfo;
import cn.lanthing.svr.service.*;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@MessageController
@Component
public class ControllingController {
    @Autowired
    private DeviceIDService deviceIDService;

    @Autowired
    private ControllingDeviceService controllingDeviceService;

    @Autowired
    private ControlledDeviceService controlledDeviceService;

    @Autowired
    private ControlledSocketService controlledSocketService;

    @Autowired
    private OrderService orderService;

    @ConnectionEvent(type = ConnectionEventType.Connected)
    public void onConnectionConnected(long connectionID) {
        controllingDeviceService.addSession(connectionID);
    }

    @ConnectionEvent(type = ConnectionEventType.Closed)
    public void onConnectionClosed(long connectionID) {
        Long deviceID = controllingDeviceService.removeSession(connectionID);
        if (deviceID != null) {
            orderService.controllingDeviceLogout(deviceID);
        }
    }

    @ConnectionEvent(type = ConnectionEventType.UnexpectedlyClosed)
    public void onConnectionUnexpectedlyClosed(long connectionID) {
        //
    }

    @MessageMapping(proto = LtProto.AllocateDeviceID)
    public LtMessage handleAllocateDeviceID(long connectionID, AllocateDeviceIDProto.AllocateDeviceID msg) {
        Long newID = deviceIDService.allocateDeviceID();
        var ack = AllocateDeviceIDAckProto.AllocateDeviceIDAck.newBuilder();
        if (newID == null) {
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.AllocateDeviceIDNoAvailableID);
        } else {
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.Success);
            ack.setDeviceId(newID);
        }
        return new LtMessage(LtProto.AllocateDeviceIDAck.ID, ack.build());
    }

    @MessageMapping(proto = LtProto.LoginDevice)
    public LtMessage handleLoginDevice(long connectionID, LoginDeviceProto.LoginDevice msg) {
        log.debug("Handling LoginDevice({}:{})", connectionID, msg.getDeviceId());
        var ack = LoginDeviceAckProto.LoginDeviceAck.newBuilder();
        if (!deviceIDService.isValidDeviceID(msg.getDeviceId())) {
            log.warn("LoginDevice failed: device id({}) not valid", msg.getDeviceId());
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.LoginDeviceInvalidID);
            return new LtMessage(LtProto.LoginDeviceAck.ID, ack.build());
        }

        String sessionID = controllingDeviceService.loginDevice(connectionID, msg.getDeviceId(), msg.getSessionId());
        if (Strings.isNullOrEmpty(sessionID)) {
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.LoginDeviceInvalidStatus);
            log.info("LoginDevice failed({}:{})", connectionID, msg.getDeviceId());
        } else {
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.Success);
            ack.setSessionId(sessionID);
            log.info("LoginDevice success({}:{}:{})", connectionID, msg.getDeviceId(), sessionID);
        }
        return new LtMessage(LtProto.LoginDeviceAck.ID, ack.build());
    }

    @MessageMapping(proto = LtProto.RequestConnection)
    public LtMessage handleRequestConnection(long connectionID, RequestConnectionProto.RequestConnection msg) {
        long peerDeviceID = msg.getDeviceId();
         Long peerConnID = controlledDeviceService.getConnectionIDByDeviceID(peerDeviceID);
        if (peerConnID == null) {
            log.warn("Controlled device({}) not online", peerDeviceID);
            var ack = RequestConnectionAckProto.RequestConnectionAck.newBuilder();
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.RequestConnectionInvalidStatus)
                    .setRequestId(msg.getRequestId());
            return new LtMessage(LtProto.RequestConnectionAck.ID, ack.build());
        }
        Long deviceID = controllingDeviceService.getDeviceIDByConnectionID(connectionID);
        if (deviceID == null) {
            // 可能是这个message处理到一半，在另一个线程处理了断链
            log.error("Get device id by connection id failed!");
            var ack = RequestConnectionAckProto.RequestConnectionAck.newBuilder();
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.RequestConnectionInvalidStatus)
                    .setRequestId(msg.getRequestId());
            return new LtMessage(LtProto.RequestConnectionAck.ID, ack.build());
        }
        OrderInfo orderInfo = orderService.newOrder(deviceID, peerDeviceID, msg.getRequestId());
        if (orderInfo == null) {
            log.warn("RequestConnection({}->{}) failed", connectionID, peerDeviceID);
            var ack = RequestConnectionAckProto.RequestConnectionAck.newBuilder();
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.RequestConnectionCreateOrderFailed)
                    .setRequestId(msg.getRequestId());
            return new LtMessage(LtProto.RequestConnectionAck.ID, ack.build());
        }
        var openConn = OpenConnectionProto.OpenConnection.newBuilder();
        openConn.setSignalingAddr(orderInfo.signalingAddress)
                .setSignalingPort(orderInfo.signalingPort)
                .setRoomId(orderInfo.roomID)
                .setServiceId(orderInfo.serviceID)
                .setAuthToken(orderInfo.authToken)
                .setStreamingParams(msg.getStreamingParams())
                .setAccessToken(msg.getAccessToken())
                .setP2PUsername(orderInfo.p2pUsername)
                .setP2PPassword(orderInfo.p2pPassword)
                .setClientDeviceId(deviceID)
                .setCookie(msg.getCookie());
        if (!CollectionUtils.isEmpty(orderInfo.reflexServers)) {
            openConn.addAllReflexServers(orderInfo.reflexServers);
        }
        if (!Strings.isNullOrEmpty(orderInfo.relayServer)) {
            openConn.addRelayServers(orderInfo.relayServer);
        }
        controlledSocketService.send(peerConnID, new LtMessage(LtProto.OpenConnection.ID, openConn.build()));
        return null;
    }

    @MessageMapping(proto = LtProto.CloseConnection)
    public LtMessage handleCloseConnection(long connectionID, CloseConnectionProto.CloseConnection msg) {
        Long deviceID = controllingDeviceService.getDeviceIDByConnectionID(connectionID);
        if (deviceID == null) {
            log.error("Get device id by connection id failed");
            return null;
        }
        boolean success = orderService.closeOrderFromControlling(msg.getRoomId(), deviceID);
        if (success) {
            log.info("Order with room id({}) closed", msg.getRoomId());
        } else {
            log.warn("Order with room id({}) close failed", msg.getRoomId());
        }
        return null;
    }
}
