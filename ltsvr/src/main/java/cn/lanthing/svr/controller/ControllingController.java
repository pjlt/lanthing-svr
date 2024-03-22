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
import cn.lanthing.svr.model.UsedID;
import cn.lanthing.svr.service.*;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

@Slf4j
@MessageController
@Component
public class ControllingController {
    @Autowired
    private DeviceIDService deviceIDService;

    @Autowired
    private ControllingSessionService controllingSessionService;

    @Autowired
    private ControlledSessionService controlledSessionService;

    @Autowired
    private ControlledSocketService controlledSocketService;

    @Autowired
    private ControllingSocketService controllingSocketService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private VersionService versionService;

    @ConnectionEvent(type = ConnectionEventType.Connected)
    public void onConnectionConnected(long connectionID) {
        log.info("Accepted new connection({})", connectionID);
        controllingSessionService.addSession(connectionID);
    }

    @ConnectionEvent(type = ConnectionEventType.Closed)
    public void onConnectionClosed(long connectionID) {
        Long deviceID = controllingSessionService.removeSession(connectionID);
        if (deviceID != null) {
            log.info("Device(connectionID:{}, deviceID:{}) connection closed", connectionID, deviceID);
            orderService.controllingDeviceLogout(deviceID);
        } else {
            log.info("Device(connectionID:{}) connection closed failed", connectionID);
        }
    }

    @ConnectionEvent(type = ConnectionEventType.UnexpectedlyClosed)
    public void onConnectionUnexpectedlyClosed(long connectionID) {
        Long deviceID = controllingSessionService.removeSession(connectionID);
        if (deviceID != null) {
            log.info("Device(connectionID:{}, deviceID:{}) connection unexpectedly closed", connectionID, deviceID);
            orderService.controllingDeviceLogout(deviceID);
        } else {
            log.info("Device(connectionID:{}) connection unexpectedly closed failed", connectionID);
        }
    }

    @MessageMapping(proto = LtProto.AllocateDeviceID)
    public LtMessage handleAllocateDeviceID(long connectionID, AllocateDeviceIDProto.AllocateDeviceID msg) {
        log.info("Handle AllocateDeviceID for connection({})", connectionID);
        var newID = deviceIDService.allocateDeviceID();
        var ack = AllocateDeviceIDAckProto.AllocateDeviceIDAck.newBuilder();
        if (newID == null) {
            log.error("Allocate new deviceID for connection({}) failed", connectionID);
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.AllocateDeviceIDNoAvailableID);
        } else {
            log.info("Allocated new deviceID({}) for connection({})", newID.deviceID(), connectionID);
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.Success)
                    .setDeviceId(newID.deviceID())
                    .setCookie(newID.cookie());
        }
        return new LtMessage(LtProto.AllocateDeviceIDAck.ID, ack.build());
    }

    @MessageMapping(proto = LtProto.LoginDevice)
    public LtMessage handleLoginDevice(long connectionID, LoginDeviceProto.LoginDevice msg) {
        //注意与ControlledController的区别
        log.info("Handle LoginDevice(connectionID:{}, deviceID:{})", connectionID, msg.getDeviceId());
        var ack = LoginDeviceAckProto.LoginDeviceAck.newBuilder();
        UsedID usedID = deviceIDService.getUsedDeviceID(msg.getDeviceId());
        if (usedID == null) {
            // 不认识该ID，为客户端分配新ID
            log.warn("LoginDevice(connectionID:{}, deviceID:{}) failed: DeviceID invalid, try allocate new deviceID", connectionID, msg.getDeviceId());
            var newID = deviceIDService.allocateDeviceID();
            if (newID == null) {
                log.error("Allocate new deviceID for connection(connectionID:{}, old deviceID:{}) failed", connectionID, msg.getDeviceId());
                ack.setErrCode(ErrorCodeOuterClass.ErrorCode.LoginDeviceInvalidID);
                return new LtMessage(LtProto.LoginUserAck.ID, ack.build());
            }
            log.info("Allocated new deviceID({}) for connection(connectionID:{}, old deviceID:{}", newID.deviceID(), connectionID, msg.getDeviceId());
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.LoginDeviceInvalidID)
                    .setNewDeviceId(newID.deviceID())
                    .setNewCookie(newID.cookie());
            return new LtMessage(LtProto.LoginDeviceAck.ID, ack.build());
        }
        if (!msg.getCookie().isEmpty()) {
            if (!msg.getCookie().equals(usedID.getCookie())) {
                // cookie不对，分配新ID
                log.warn("LoginDevice(connectionID:{}, deviceID:{}) with invalid cookie, try allocate new deviceID", connectionID, msg.getDeviceId());
                var newID = deviceIDService.allocateDeviceID();
                if (newID == null) {
                    log.error("Allocate new deviceID for connection(connectionID:{}, old deviceID:{}) failed", connectionID, msg.getDeviceId());
                    ack.setErrCode(ErrorCodeOuterClass.ErrorCode.LoginDeviceInvalidCookie);
                    return new LtMessage(LtProto.LoginUserAck.ID, ack.build());
                }
                ack.setErrCode(ErrorCodeOuterClass.ErrorCode.LoginDeviceInvalidCookie)
                        .setNewDeviceId(newID.deviceID())
                        .setNewCookie(newID.cookie());
                return new LtMessage(LtProto.LoginDeviceAck.ID, ack.build());
            }
        } else {
            // 发上来的cookie为空，为了兼容以前的客户端，暂时不处理，等旧版本客户端都没了就当作错误处理
            ack.setNewCookie(usedID.getCookie());
        }

        var expiredAt = new Date((usedID.getUpdatedAt().toEpochSecond(ZoneOffset.UTC) + 60 * 60 * 24 * 7) * 1000);
        var now = new Date();
        if (expiredAt.before(now)) {
            // cookie是对的，但是过期了，则更新cookie
            usedID.setCookie(UUID.randomUUID().toString());
            deviceIDService.updateCookie(usedID.getDeviceID(), usedID.getCookie());
            ack.setNewCookie(usedID.getCookie());
        }
        // 走到这里，说明id和cookie都对了
        int versionNum = msg.getVersionMajor() * 1_000_000 + msg.getVersionMinor() * 1_000 + msg.getVersionPatch();
        boolean success = controllingSessionService.loginDevice(connectionID, msg.getDeviceId(), versionNum, msg.getOsType().toString());
        if (!success) {
            // 失败暂时有两种可能
            // 1. 代码bug
            // 2. 有同id登录了
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.LoginDeviceInvalidStatus);
            log.info("LoginDevice(connectionID:{}, deviceID:{}) failed", connectionID, msg.getDeviceId());
        } else {
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.Success);
            log.info("LoginDevice(connectionID:{}, deviceID:{}) success)", connectionID, msg.getDeviceId());
            VersionService.Version version = versionService.getNewVersionPC(msg.getVersionMajor(), msg.getVersionMinor(), msg.getVersionPatch());
            if (version != null) {
                // version != null说明有新版本
                var newVer = NewVersionProto.NewVersion.newBuilder().
                        setMajor(version.major())
                        .setMinor(version.minor())
                        .setPatch(version.patch())
                        .setForce(version.force())
                        .setTimestamp(version.timestamp())
                        .setUrl(version.url())
                        .addAllFeatures(version.features())
                        .addAllBugfix(version.bugfix());
                controllingSocketService.send(connectionID, new LtMessage(LtProto.NewVersion.ID, newVer.build()));
            }
        }
        return new LtMessage(LtProto.LoginDeviceAck.ID, ack.build());
    }

    @MessageMapping(proto = LtProto.RequestConnection)
    public LtMessage handleRequestConnection(long connectionID, RequestConnectionProto.RequestConnection msg) {
        long peerDeviceID = msg.getDeviceId();
        var controlledSession = controlledSessionService.getSessionByDeviceID(peerDeviceID);
        if (controlledSession == null) {
            log.warn("Handle RequestConnection(connectionID:{}, toDeviceID:{}), but peer not online", connectionID, peerDeviceID);
            var ack = RequestConnectionAckProto.RequestConnectionAck.newBuilder();
            ack.setDeviceId(peerDeviceID);
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.RequestConnectionPeerNotOnline)
                    .setRequestId(msg.getRequestId());
            return new LtMessage(LtProto.RequestConnectionAck.ID, ack.build());
        } else {
            log.info("Handle RequestConnection(connectionID:{}, peer connectionID:{}, toDeviceID:{}", connectionID, controlledSession.connectionID(), peerDeviceID);
        }
        var controllingSession = controllingSessionService.getSessionByConnectionID(connectionID);
        if (controllingSession == null || controllingSession.deviceID() == 0) {
            // 可能是这个message处理到一半，在另一个线程处理了断链
            log.error("RequestConnection(connectionID:{}, toDeviceID:{}) get controlling session by connectionID failed", connectionID, peerDeviceID);
            var ack = RequestConnectionAckProto.RequestConnectionAck.newBuilder();
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.RequestConnectionInvalidStatus)
                    .setRequestId(msg.getRequestId());
            return new LtMessage(LtProto.RequestConnectionAck.ID, ack.build());
        }
        OrderService.OrderInfo orderInfo = orderService.newOrder(controllingSession.deviceID(), peerDeviceID, msg.getRequestId());
        if (orderInfo == null) {
            log.warn("RequestConnection(connectionID:{}, fromDeviceID:{}, toDeviceID:{}) create new order failed", connectionID, controllingSession.deviceID(), peerDeviceID);
            var ack = RequestConnectionAckProto.RequestConnectionAck.newBuilder();
            ack.setDeviceId(peerDeviceID);
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.RequestConnectionCreateOrderFailed)
                    .setRequestId(msg.getRequestId());
            return new LtMessage(LtProto.RequestConnectionAck.ID, ack.build());
        }
        var openConn = OpenConnectionProto.OpenConnection.newBuilder();
        openConn.setSignalingAddr(orderInfo.signalingAddress())
                .setSignalingPort(orderInfo.signalingPort())
                .setRoomId(orderInfo.roomID())
                .setServiceId(orderInfo.serviceID())
                .setAuthToken(orderInfo.authToken())
                .setStreamingParams(msg.getStreamingParams())
                .setAccessToken(msg.getAccessToken())
                .setP2PUsername(orderInfo.p2pUsername())
                .setP2PPassword(orderInfo.p2pPassword())
                .setClientDeviceId(controllingSession.deviceID())
                .setCookie(msg.getCookie())
                .setClientVersion(msg.getClientVersion())
                .setRequiredVersion(msg.getRequiredVersion())
                .setTransportType(msg.getTransportType());
        if (!CollectionUtils.isEmpty(orderInfo.reflexServers())) {
            openConn.addAllReflexServers(orderInfo.reflexServers());
        }
        if (!Strings.isNullOrEmpty(orderInfo.relayServer())) {
            openConn.addRelayServers(orderInfo.relayServer());
        }
        controlledSocketService.send(controlledSession.connectionID(), new LtMessage(LtProto.OpenConnection.ID, openConn.build()));
        return null;
    }

    @MessageMapping(proto = LtProto.CloseConnection)
    public LtMessage handleCloseConnection(long connectionID, CloseConnectionProto.CloseConnection msg) {
        var session = controllingSessionService.getSessionByConnectionID(connectionID);
        if (session == null || session.deviceID() == 0) {
            log.error("CloseConnection(connectionID:{}, roomID:{}) get session by connectionID failed", connectionID, msg.getRoomId());
            return null;
        }
        boolean success = orderService.closeOrderFromControlling(msg.getRoomId());
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
