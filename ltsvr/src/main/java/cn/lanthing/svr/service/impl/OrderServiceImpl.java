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
import cn.lanthing.svr.dao.CurrentOrderDao;
import cn.lanthing.svr.dao.OrderDao;
import cn.lanthing.svr.model.CurrentOrder;
import cn.lanthing.svr.model.Order;
import cn.lanthing.svr.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private SignalingConfig signalingConfig;

    @Autowired
    private ReflexRelayConfig reflexRelayConfig;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private CurrentOrderDao currentOrderDao;

    @Override
    public OrderInfo newOrder(long fromDeviceID, long toDeviceID, long clientRequestID) {
        String relayServer = "";
        List<String> reflexServers = new ArrayList<>();
        if (!CollectionUtils.isEmpty(reflexRelayConfig.getRelays())) {
            relayServer = reflexRelayConfig.getRelays().get(0);
        }
        if (!CollectionUtils.isEmpty(reflexRelayConfig.getReflexes())){
            reflexServers = reflexRelayConfig.getReflexes();
        }
        OrderInfo orderInfo = new OrderInfo(
                fromDeviceID,
                toDeviceID,
                clientRequestID,
                signalingConfig.getIP(),
                signalingConfig.getPort(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                RandomStringUtils.randomAlphanumeric(6),
                RandomStringUtils.randomAlphanumeric(20),
                relayServer,
                reflexServers);
        boolean success = currentOrderDao.insertOrder(orderInfo);
        if (!success) {
            log.error("Insert new OrderStatus(fromDeviceID:{}, toDeviceID:{}) failed, maybe there is another order with same toDeviceID", orderInfo.fromDeviceID(), orderInfo.toDeviceID());
            return null;
        }
        success = orderDao.insertOrder(orderInfo);
        if (!success) {
            log.error("Insert new Order(fromDeviceID:{}, toDeviceID:{}) failed", orderInfo.fromDeviceID(), orderInfo.toDeviceID());
            currentOrderDao.deleteOrder(orderInfo.roomID());
            return null;
        }
        log.info("Insert new Order(fromDeviceID:{}, toDeviceID:{}, roomID:{}) success", orderInfo.fromDeviceID(), orderInfo.toDeviceID(), orderInfo.roomID());
        return orderInfo;
    }

    @Override
    public Order getOrderByControlledDeviceID(long deviceID) {
        var orderStatus = currentOrderDao.queryOrderByToDeviceID((int) deviceID);
        if (orderStatus == null) {
            log.error("Query OrderStatus by toDeviceID({}) failed", deviceID);
            return null;
        }
        var order = orderDao.queryOrderByRoomID(orderStatus.getRoomID());
        if (order == null) {
            log.error("Query Order by toDeviceID({}) -> roomID({}) -> Order failed", deviceID, orderStatus.getRoomID());
        }
        return order;
    }

    @Override
    public boolean closeOrderFromControlled(String roomID) {
        final String reason = "controlled_close";
        currentOrderDao.deleteOrder(roomID);
        boolean success = orderDao.markOrderFinishedWithReason(roomID, reason);
        log.info("MarkOrderFinishedWithReason({}, reason:{}) {}", roomID, reason, success ? "success" : "failed");
        return success;
    }

    @Override
    public boolean closeOrderFromControlling(String roomID) {
        final String reason = "controlling_close";
        currentOrderDao.deleteOrder(roomID);
        boolean success = orderDao.markOrderFinishedWithReason(roomID, reason);
        log.info("MarkOrderFinishedWithReason({}, reason:{}) {}", roomID, reason, success ? "success" : "failed");
        return success;
    }

    @Override
    public void controlledDeviceLogout(long deviceID) {
        final String reason = "controlled_logout";
        var orderStatus = currentOrderDao.queryOrderByToDeviceID((int)deviceID);
        if (orderStatus == null) {
            log.error("ControlledDeviceLogout: Query OrderStatus by toDeviceID({}) failed", deviceID);
            return;
        }
        currentOrderDao.deleteOrder(orderStatus.getRoomID());
        boolean success = orderDao.markOrderFinishedWithReason(orderStatus.getRoomID(), reason);
        log.info("MarkOrderFinishedWithReason({}, reason:{}) {}", orderStatus.getRoomID(), reason, success ? "success" : "failed");
    }

    @Override
    public void controllingDeviceLogout(long deviceID) {
        final String reason = "controlling_logout";
        List<CurrentOrder> statusList = currentOrderDao.queryOrderByFromDeviceID((int)deviceID);
        if (statusList.isEmpty()) {
            log.error("ControllingDeviceLogout: Query List<OrderStatus> by fromDeviceID({}) failed", deviceID);
            return;
        }
        for (var status : statusList) {
            currentOrderDao.deleteOrder(status.getRoomID());
            boolean success = orderDao.markOrderFinishedWithReason(status.getRoomID(), reason);
            log.info("MarkOrderFinishedWithReason({}, reason:{}) {}", status.getRoomID(), reason, success ? "success" : "failed");
        }
    }

    @Override
    public HistoryOrders getHistoryOrders(int index, int limit) {
        var orders = orderDao.queryHistoryOrders(index, limit);
        var total = orderDao.countOrder();
        List<BasicOrderInfo> basicOrders = new ArrayList<>();
        for (var order : orders) {
            basicOrders.add(BasicOrderInfo.createFrom(order));
        }
        return new HistoryOrders(total, index, limit, basicOrders);
    }
}
