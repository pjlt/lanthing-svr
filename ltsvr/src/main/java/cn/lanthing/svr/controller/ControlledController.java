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
import cn.lanthing.ltproto.common.KeepAliveAckProto;
import cn.lanthing.ltproto.common.KeepAliveProto;
import cn.lanthing.ltproto.server.*;
import cn.lanthing.ltsocket.ConnectionEvent;
import cn.lanthing.ltsocket.ConnectionEventType;
import cn.lanthing.ltsocket.MessageController;
import cn.lanthing.ltsocket.MessageMapping;
import cn.lanthing.svr.model.Order;
import cn.lanthing.svr.model.UsedID;
import cn.lanthing.svr.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@MessageController
@Component
public class ControlledController {
    @Autowired
    private DeviceIDService deviceIDService;

    @Autowired
    private ControlledDeviceService controlledDeviceService;

    @Autowired
    private ControllingDeviceService controllingDeviceService;

    @Autowired
    private ControllingSocketService controllingSocketService;

    @Autowired
    private OrderService orderService;

    @ConnectionEvent(type = ConnectionEventType.Connected)
    public void onConnectionConnected(long connectionID) {
        log.info("Accepted new connection({})", connectionID);
        controlledDeviceService.addSession(connectionID);
    }

    @ConnectionEvent(type = ConnectionEventType.Closed)
    public void onConnectionClosed(long connectionID) {
        Long deviceID = controlledDeviceService.removeSession(connectionID);
        if (deviceID != null) {
            log.info("Device(connectionID:{}, deviceID:{}) connection closed", connectionID, deviceID);
            orderService.controlledDeviceLogout(deviceID);
        } else {
            log.info("Device(connectionID:{}) connection closed failed", connectionID);
        }
    }

    @ConnectionEvent(type = ConnectionEventType.UnexpectedlyClosed)
    public void onConnectionUnexpectedlyClosed(long connectionID) {
        Long deviceID = controlledDeviceService.removeSession(connectionID);
        if (deviceID != null) {
            log.info("Device(connectionID:{}, deviceID:{}) connection closed", connectionID, deviceID);
            orderService.controlledDeviceLogout(deviceID);
        } else {
            log.info("Device(connectionID:{}) connection closed failed", connectionID);
        }
    }

    @MessageMapping(proto = LtProto.LoginDevice)
    public LtMessage handleLoginDevice(long connectionID, LoginDeviceProto.LoginDevice msg) {
        //注意与ControllingController的区别
        log.info("Handle LoginDevice(connectionID:{}, deviceID:{})", connectionID, msg.getDeviceId());
        var ack = LoginDeviceAckProto.LoginDeviceAck.newBuilder();
        UsedID usedID = deviceIDService.getUsedDeviceID(msg.getDeviceId());
        if (usedID == null) {
            // 不认识该id，登录失败
            log.warn("LoginDevice(connectionID:{}, deviceID:{}) failed: DeviceID valid", connectionID, msg.getDeviceId());
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.LoginDeviceInvalidID);
            return new LtMessage(LtProto.LoginDeviceAck.ID, ack.build());
        }
        if (msg.getCookie().isEmpty() || !msg.getCookie().equals(usedID.getCookie())) {
            // cookie不对，登录失败
            log.warn("LoginDevice(connectionID:{}, deviceID:{}) with invalid cookie", connectionID, msg.getDeviceId());
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.LoginDeviceInvalidID);
            return new LtMessage(LtProto.LoginDeviceAck.ID, ack.build());
        }

        int versionNum = msg.getVersionMajor() * 1_000_000 + msg.getVersionMinor() * 1_000 + msg.getVersionPatch();
        boolean success = controlledDeviceService.loginDevice(connectionID, msg.getDeviceId(), msg.getAllowControl(), versionNum, msg.getOsType().toString());
        if (!success) {
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.LoginDeviceInvalidStatus);
            log.info("LoginDevice(connectionID:{}, deviceID:{}) failed", connectionID, msg.getDeviceId());
        } else {
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.Success);
            log.info("LoginDevice(connectionID:{}, deviceID:{}) success)", connectionID, msg.getDeviceId());
        }
        return new LtMessage(LtProto.LoginDeviceAck.ID, ack.build());
    }

    @MessageMapping(proto = LtProto.OpenConnectionAck)
    public LtMessage handleOpenConnectionAck(long connectionID, OpenConnectionAckProto.OpenConnectionAck msg) {
        var session = controlledDeviceService.getSessionByConnectionID(connectionID);
        if (session == null || session.deviceID() == 0) {
            log.error("Handle OpenConnectionAck(connectionID:{}) get deviceID by connectionID failed", connectionID);
            return null;
        }
        Order order = orderService.getOrderByControlledDeviceID(session.deviceID());
        if (order == null) {
            log.error("OpenConnectionAck(connectionID:{}, deviceID:{}) get order by deviceID failed", connectionID, session.deviceID());
            return null;
        }
        Long controllingConnectionID = controllingDeviceService.getConnectionIDByDeviceID(order.getFromDeviceID());
        if (controllingConnectionID == null) {
            log.error("OpenConnectionAck(connectionID:{}, deviceID:{}, fromDeviceID:{}) get connectionID by fromDeviceID failed", connectionID, session.deviceID(), order.getFromDeviceID());
            return null;
        }
        log.info("Handle OpenConnectionAck(connectionID:{}, deviceID:{}, fromConnectionID:{}, fromDeviceID:{})", connectionID, session.deviceID(), controllingConnectionID, order.getFromDeviceID());
        var ack = RequestConnectionAckProto.RequestConnectionAck.newBuilder();
        ack.setDeviceId(session.deviceID());
        if (msg.getErrCode() != ErrorCodeOuterClass.ErrorCode.Success) {
            ack.setErrCode(msg.getErrCode()).setRequestId(order.getClientRequestID());
            boolean success = orderService.closeOrderFromControlled(order.getRoomID(), order.getToDeviceID());
            if (success) {
                log.info("OpenConnectionAck(connectionID:{}, deviceID:{}, fromConnectionID:{}, fromDeviceID:{}, roomID:{}) received error code {}, close order success",
                        connectionID, session.deviceID(), controllingConnectionID, order.getFromDeviceID(), order.getRoomID(), msg.getErrCode());
            } else {
                log.warn("OpenConnectionAck(connectionID:{}, deviceID:{}, fromConnectionID:{}, fromDeviceID:{}, roomID:{}) received error code {}, close order failed",
                        connectionID, session.deviceID(), controllingConnectionID, order.getFromDeviceID(), order.getRoomID(), msg.getErrCode());
            }
        } else {
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.Success)
                    .setRequestId(order.getClientRequestID())
                    .setDeviceId(order.getToDeviceID())
                    .setSignalingAddr(order.getSignalingHost())
                    .setSignalingPort(order.getSignalingPort())
                    .setRoomId(order.getRoomID())
                    .setClientId(order.getClientID())
                    .setAuthToken(order.getAuthToken())
                    .setP2PUsername(order.getP2pUser())
                    .setP2PPassword(order.getP2pToken())
                    .setStreamingParams(msg.getStreamingParams())
                    .setTransportType(msg.getTransportType());
            if (!order.getReflexServers().isEmpty()) {
                ack.addAllReflexServers(Arrays.asList(order.getReflexServers().split(",")));
            }
        }
        controllingSocketService.send(controllingConnectionID, new LtMessage(LtProto.RequestConnectionAck.ID, ack.build()));
        return null;
    }

    @MessageMapping(proto = LtProto.CloseConnection)
    public LtMessage handleCloseConnection(long connectionID, CloseConnectionProto.CloseConnection msg) {
        var session = controlledDeviceService.getSessionByConnectionID(connectionID);
        if (session == null || session.deviceID() == 0) {
            log.error("CloseConnection(connectionID:{}, roomID:{}) get session by connectionID failed", connectionID, msg.getRoomId());
            return null;
        }
        boolean success = orderService.closeOrderFromControlled(msg.getRoomId(), session.deviceID());
        if (success) {
            log.info("CloseConnection(connectionID:{}, roomID:{}) close order success", connectionID, msg.getRoomId());
        } else {
            log.warn("CloseConnection(connectionID:{}, roomID:{}) close order failed", connectionID, msg.getRoomId());
        }
        return null;
    }

    @MessageMapping(proto = LtProto.KeepAlive)
    public LtMessage handleKeepAlive(long connectionID, KeepAliveProto.KeepAlive msg) {
        //TODO: 删除超时链接
        var ack = KeepAliveAckProto.KeepAliveAck.newBuilder();
        return new LtMessage(LtProto.KeepAliveAck.ID, ack.build());
    }
}
