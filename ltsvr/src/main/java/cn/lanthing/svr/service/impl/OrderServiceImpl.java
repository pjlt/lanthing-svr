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

package cn.lanthing.svr.service.impl;

import cn.lanthing.svr.config.ReflexRelayConfig;
import cn.lanthing.svr.config.SignalingConfig;
import cn.lanthing.svr.entity.OrderInfo;
import cn.lanthing.svr.service.OrderService;
import cn.lanthing.utils.AutoLock;
import cn.lanthing.utils.AutoReentrantLock;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private final Map<Long, OrderInfo> controlledDeviceIDToOrderInfoMap = new HashMap<>();

    private final Map<Long, OrderInfo> controllingDeviceIDToOrderInfoMap = new HashMap<>();

    private final Map<String, OrderInfo> roomIDToOrderInfoMap = new HashMap<>();

    private final AutoReentrantLock lock = new AutoReentrantLock();

    @Autowired
    private SignalingConfig signalingConfig;

    @Autowired
    private ReflexRelayConfig reflexRelayConfig;

    @Override
    public OrderInfo newOrder(long fromDeviceID, long toDeviceID, long clientRequestID) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.clientRequestID = clientRequestID;
        orderInfo.fromDeviceID = fromDeviceID;
        orderInfo.toDeviceID = toDeviceID;
        orderInfo.signalingAddress =  signalingConfig.getIP();
        orderInfo.signalingPort = signalingConfig.getPort();
        orderInfo.roomID = UUID.randomUUID().toString();
        orderInfo.serviceID = UUID.randomUUID().toString();
        orderInfo.clientID = UUID.randomUUID().toString();
        orderInfo.authToken = UUID.randomUUID().toString();
        orderInfo.p2pUsername = RandomStringUtils.randomAlphanumeric(6);
        orderInfo.p2pPassword = RandomStringUtils.randomAlphanumeric(8);
        if (!CollectionUtils.isEmpty(reflexRelayConfig.getRelays())) {
            orderInfo.relayServer = reflexRelayConfig.getRelays().get(0);
        }
        if (!CollectionUtils.isEmpty(reflexRelayConfig.getReflexes())){
            orderInfo.reflexServers = reflexRelayConfig.getReflexes();
        }
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            var previousValue = controlledDeviceIDToOrderInfoMap.putIfAbsent(toDeviceID, orderInfo);
            if (previousValue != null) {
                //已经在被控，打log怕影响到锁了
                return null;
            }
            previousValue = controllingDeviceIDToOrderInfoMap.putIfAbsent(fromDeviceID, orderInfo);
            if (previousValue != null) {
                controlledDeviceIDToOrderInfoMap.remove(toDeviceID);
                return null;
            }
            roomIDToOrderInfoMap.put(orderInfo.roomID, orderInfo);
        }
        return orderInfo;
    }

    @Override
    public OrderInfo getOrderByControlledDeviceID(long deviceID) {
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            return controlledDeviceIDToOrderInfoMap.get(deviceID);
        }
    }

    @Override
    public boolean closeOrderFromControlled(String roomID, long deviceID) {
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            var orderInfo = roomIDToOrderInfoMap.get(roomID);
            if (orderInfo == null) {
                return false;
            }
            if (orderInfo.toDeviceID != deviceID) {
                return false;
            }
            roomIDToOrderInfoMap.remove(roomID);
            controlledDeviceIDToOrderInfoMap.remove(orderInfo.toDeviceID);
            controllingDeviceIDToOrderInfoMap.remove(orderInfo.fromDeviceID);
            return true;
        }
    }

    @Override
    public boolean closeOrderFromControlling(String roomID, long deviceID) {
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            var orderInfo = roomIDToOrderInfoMap.get(roomID);
            if (orderInfo == null) {
                return false;
            }
            if (orderInfo.fromDeviceID != deviceID) {
                return false;
            }
            roomIDToOrderInfoMap.remove(roomID);
            controlledDeviceIDToOrderInfoMap.remove(orderInfo.toDeviceID);
            controllingDeviceIDToOrderInfoMap.remove(orderInfo.fromDeviceID);
            return true;
        }
    }

    @Override
    public void controlledDeviceLogout(long deviceID) {
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            var orderInfo = controlledDeviceIDToOrderInfoMap.remove(deviceID);
            if (orderInfo == null) {
                return;
            }
            controllingDeviceIDToOrderInfoMap.remove(orderInfo.fromDeviceID);
            roomIDToOrderInfoMap.remove(orderInfo.roomID);
        }
    }

    @Override
    public void controllingDeviceLogout(long deviceID) {
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            var orderInfo = controllingDeviceIDToOrderInfoMap.remove(deviceID);
            if (orderInfo == null) {
                return;
            }
            controlledDeviceIDToOrderInfoMap.remove(orderInfo.toDeviceID);
            roomIDToOrderInfoMap.remove(orderInfo.roomID);
        }
    }
}
