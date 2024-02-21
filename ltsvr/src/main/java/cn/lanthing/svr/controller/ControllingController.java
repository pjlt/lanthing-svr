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
import cn.lanthing.svr.entity.OrderInfo;
import cn.lanthing.svr.entity.UsedIDEntity;
import cn.lanthing.svr.entity.Version;
import cn.lanthing.svr.service.*;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.UUID;

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
    private ControllingSocketService controllingSocketService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private VersionService versionService;

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
        UsedIDEntity idEntity = deviceIDService.allocateDeviceID();
        var ack = AllocateDeviceIDAckProto.AllocateDeviceIDAck.newBuilder();
        if (idEntity == null) {
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.AllocateDeviceIDNoAvailableID);
        } else {
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.Success)
                    .setDeviceId(idEntity.getDeviceID())
                    .setCookie(idEntity.getCookie());
        }
        return new LtMessage(LtProto.AllocateDeviceIDAck.ID, ack.build());
    }

    @MessageMapping(proto = LtProto.LoginDevice)
    public LtMessage handleLoginDevice(long connectionID, LoginDeviceProto.LoginDevice msg) {
        log.debug("Handling LoginDevice({}:{})", connectionID, msg.getDeviceId());
        var ack = LoginDeviceAckProto.LoginDeviceAck.newBuilder();
        UsedIDEntity idEntity = deviceIDService.getUsedDeviceID(msg.getDeviceId());
        if (idEntity == null) {
            // 不认识该ID，为客户端分配新ID
            log.warn("LoginDevice failed: device id({}) not valid", msg.getDeviceId());
            idEntity = deviceIDService.allocateDeviceID();
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.LoginDeviceInvalidID)
                    .setNewDeviceId(idEntity.getDeviceID())
                    .setNewCookie(idEntity.getCookie());
            return new LtMessage(LtProto.LoginDeviceAck.ID, ack.build());
        }
        if (!msg.getCookie().isEmpty()) {
           if (!msg.getCookie().equals(idEntity.getCookie())) {
               // cookie不对，分配新ID
               log.warn("LoginDevice failed: device id({}) invalid cookie", msg.getDeviceId());
               idEntity = deviceIDService.allocateDeviceID();
               ack.setErrCode(ErrorCodeOuterClass.ErrorCode.LoginDeviceInvalidCookie)
                       .setNewDeviceId(idEntity.getDeviceID())
                       .setNewCookie(idEntity.getCookie());
               return new LtMessage(LtProto.LoginDeviceAck.ID, ack.build());
           }
        } else {
            // 发上来的cookie为空，为了兼容以前的客户端，暂时不处理，等旧版本客户端都没了就当作错误处理
            ack.setNewCookie(idEntity.getCookie());
        }
        var expiredAt = new Date(idEntity.getUpdatedAt().getTime() + 1000L * 60 * 60 * 24 * 7);
        var now = new Date();
        if (expiredAt.before(now)) {
            idEntity.setCookie(UUID.randomUUID().toString());
            deviceIDService.updateCookie(idEntity.getDeviceID(), idEntity.getCookie());
            ack.setNewCookie(idEntity.getCookie());
        }
        // 走到这里，说明id和cookie都对了
        int versionNum = msg.getVersionMajor() * 1_000_000 + msg.getVersionMinor() * 1_000 + msg.getVersionPatch();
        boolean success = controllingDeviceService.loginDevice(connectionID, msg.getDeviceId(), versionNum);
        if (!success) {
            // 失败暂时有两种可能
            // 1. 代码bug
            // 2. 有同id登录了
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.LoginDeviceInvalidStatus);
            log.info("LoginDevice failed({}:{})", connectionID, msg.getDeviceId());
        } else {
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.Success);
            log.info("LoginDevice success({}:{})", connectionID, msg.getDeviceId());
            Version version = versionService.getNewVersionPC(msg.getVersionMajor(), msg.getVersionMinor(), msg.getVersionPatch());
            if (version != null) {
                // version != null说明有新版本
                var newVer = NewVersionProto.NewVersion.newBuilder().
                        setMajor(version.getMajor())
                        .setMinor(version.getMinor())
                        .setPatch(version.getPatch())
                        .setForce(version.isForce())
                        .setTimestamp(version.getTimestamp())
                        .setUrl(version.getUrl())
                        .addAllFeatures(version.getFeatures())
                        .addAllBugfix(version.getBugfix());
                controllingSocketService.send(connectionID, new LtMessage(LtProto.NewVersion.ID, newVer.build()));
            }
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
            ack.setDeviceId(peerDeviceID);
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.RequestConnectionPeerNotOnline)
                    .setRequestId(msg.getRequestId());
            return new LtMessage(LtProto.RequestConnectionAck.ID, ack.build());
        }
        var controlledSession = controlledDeviceService.getSessionByConnectionID(peerConnID);
        if (controlledSession == null || controlledSession.deviceID == 0) {
            log.warn("Controlled device({}) not login", peerDeviceID);
            var ack = RequestConnectionAckProto.RequestConnectionAck.newBuilder();
            ack.setDeviceId(peerDeviceID);
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.RequestConnectionPeerNotOnline)
                    .setRequestId(msg.getRequestId());
            return new LtMessage(LtProto.RequestConnectionAck.ID, ack.build());
        }
        var controllingSession = controllingDeviceService.getSessionByConnectionID(connectionID);
        if (controllingSession == null || controllingSession.deviceID == 0) {
            // 可能是这个message处理到一半，在另一个线程处理了断链
            log.error("Get device id by connection id failed!");
            var ack = RequestConnectionAckProto.RequestConnectionAck.newBuilder();
            ack.setErrCode(ErrorCodeOuterClass.ErrorCode.RequestConnectionInvalidStatus)
                    .setRequestId(msg.getRequestId());
            return new LtMessage(LtProto.RequestConnectionAck.ID, ack.build());
        }
        boolean newSignaling = controllingSession.version >= 2000 && controlledSession.version >= 2000;
        OrderInfo orderInfo = orderService.newOrder(controllingSession.deviceID, peerDeviceID, msg.getRequestId(), newSignaling);
        if (orderInfo == null) {
            log.warn("RequestConnection({}->{}) failed", connectionID, peerDeviceID);
            var ack = RequestConnectionAckProto.RequestConnectionAck.newBuilder();
            ack.setDeviceId(peerDeviceID);
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
                .setClientDeviceId(controllingSession.deviceID)
                .setCookie(msg.getCookie())
                .setClientVersion(msg.getClientVersion())
                .setRequiredVersion(msg.getRequiredVersion())
                .setTransportType(msg.getTransportType());
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
        var session = controllingDeviceService.getSessionByConnectionID(connectionID);
        if (session == null || session.deviceID == 0) {
            log.error("Get device id by connection id failed");
            return null;
        }
        boolean success = orderService.closeOrderFromControlling(msg.getRoomId(), session.deviceID);
        if (success) {
            log.info("Order with room id({}) closed", msg.getRoomId());
        } else {
            log.warn("Order with room id({}) close failed", msg.getRoomId());
        }
        return null;
    }

    @MessageMapping(proto = LtProto.KeepAlive)
    public LtMessage handleKeepAlive(long connectionID, KeepAliveProto.KeepAlive msg) {
        var ack  = KeepAliveAckProto.KeepAliveAck.newBuilder();
        return new LtMessage(LtProto.KeepAliveAck.ID, ack.build());
    }
}
