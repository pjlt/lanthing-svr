/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2024 Zhennan Tu <zhennan.tu@gmail.com>
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

package cn.lanthing.svr.service;

import cn.lanthing.svr.dao.CurrentOrderDao;
import cn.lanthing.svr.dao.OrderDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;

@SpringBootTest
public class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private CurrentOrderDao currentOrderDao;

    @BeforeEach
    public void setupDB() {
        orderDao.clear();
        currentOrderDao.clear();
    }

    @Test
    public void newOrder() {
        final int fromDeviceID = 12345678;
        final int toDeviceID = 87654321;
        final int clientReqID = 1;
        var orderInfo = orderService.newOrder(fromDeviceID, toDeviceID, clientReqID);
        Assertions.assertNotNull(orderInfo);
        Assertions.assertEquals(fromDeviceID, orderInfo.fromDeviceID());
        Assertions.assertEquals(toDeviceID, orderInfo.toDeviceID());
        Assertions.assertEquals(clientReqID, orderInfo.clientRequestID());
    }

    @Test
    public void getInvalidOrderByControlledDeviceID() {
        var orderInfo = orderService.getOrderByControlledDeviceID(12345678);
        Assertions.assertNull(orderInfo);
    }

    @Test
    public void getOrderByControlledDeviceID() {
        final int fromDeviceID = 12345678;
        final int toDeviceID = 87654321;
        final int clientReqID = 1;
        var orderInfo = orderService.newOrder(fromDeviceID, toDeviceID, clientReqID);
        Assertions.assertEquals(orderInfo.reflexServers(), Arrays.asList("stun:stun.l.google.com:19302", "stun:stun2.l.google.com:19302", "stun:stun3.l.google.com:19302"));
        var order = orderService.getOrderByControlledDeviceID(orderInfo.toDeviceID());
        Assertions.assertNotNull(order);
        Assertions.assertEquals(orderInfo.fromDeviceID(), order.getFromDeviceID());
        Assertions.assertEquals(orderInfo.toDeviceID(), order.getToDeviceID());
        Assertions.assertEquals(orderInfo.clientRequestID(), order.getClientRequestID());
        Assertions.assertEquals(orderInfo.signalingAddress(), order.getSignalingHost());
        Assertions.assertEquals(orderInfo.signalingPort(), order.getSignalingPort());
        Assertions.assertEquals(orderInfo.roomID(), order.getRoomID());
        Assertions.assertEquals(orderInfo.serviceID(), order.getServiceID());
        Assertions.assertEquals(orderInfo.clientID(), order.getClientID());
        Assertions.assertEquals(orderInfo.authToken(), order.getAuthToken());
        Assertions.assertEquals(orderInfo.p2pUsername(), order.getP2pUser());
        Assertions.assertEquals(orderInfo.p2pPassword(), order.getP2pToken());
        Assertions.assertEquals(orderInfo.relayServer(), order.getRelayServer());
        Assertions.assertEquals("stun:stun.l.google.com:19302,stun:stun2.l.google.com:19302,stun:stun3.l.google.com:19302", order.getReflexServers());
    }

    @Test
    public void closeInvalidOrderFromControlled() {
        boolean success = orderService.closeOrderFromControlled("roomID");
        Assertions.assertFalse(success);
    }

    @Test
    public void closeOrderFromControlled() {
        final int fromDeviceID = 12345678;
        final int toDeviceID = 87654321;
        final int clientReqID = 1;
        var orderInfo = orderService.newOrder(fromDeviceID, toDeviceID, clientReqID);
        boolean success = orderService.closeOrderFromControlled(orderInfo.roomID());
        Assertions.assertTrue(success);
        var order = orderService.getOrderByControlledDeviceID(toDeviceID);
        Assertions.assertNull(order);
        var histories = orderService.getHistoryOrders(0, 20);
        var history = histories.orders().get(0);
        Assertions.assertEquals("controlled_close", history.finishReason());
    }

    @Test
    public void closeOrderFromControlling() {
        final int fromDeviceID = 12345678;
        final int toDeviceID = 87654321;
        final int clientReqID = 1;
        var orderInfo = orderService.newOrder(fromDeviceID, toDeviceID, clientReqID);
        boolean success = orderService.closeOrderFromControlling(orderInfo.roomID());
        Assertions.assertTrue(success);
        var order = orderService.getOrderByControlledDeviceID(toDeviceID);
        Assertions.assertNull(order);
        var histories = orderService.getHistoryOrders(0, 20);
        var history = histories.orders().get(0);
        Assertions.assertEquals("controlling_close", history.finishReason());
    }

    @Test
    public void controllingDeviceLogout() {
        final int fromDeviceID = 12345678;
        final int toDeviceID = 87654321;
        final int clientReqID = 1;
        var orderInfo = orderService.newOrder(fromDeviceID, toDeviceID, clientReqID);
        orderService.controllingDeviceLogout(fromDeviceID);
        var order = orderService.getOrderByControlledDeviceID(toDeviceID);
        Assertions.assertNull(order);
        var histories = orderService.getHistoryOrders(0, 20);
        var history = histories.orders().get(0);
        Assertions.assertEquals("controlling_logout", history.finishReason());
    }

    @Test
    public void controlledDeviceLogout() {
        final int fromDeviceID = 12345678;
        final int toDeviceID = 87654321;
        final int clientReqID = 1;
        var orderInfo = orderService.newOrder(fromDeviceID, toDeviceID, clientReqID);
        orderService.controlledDeviceLogout(toDeviceID);
        var order = orderService.getOrderByControlledDeviceID(toDeviceID);
        Assertions.assertNull(order);
        var histories = orderService.getHistoryOrders(0, 20);
        var history = histories.orders().get(0);
        Assertions.assertEquals("controlled_logout", history.finishReason());
    }

    @Test
    public void getEmptyHistoryOrders() {
        var orders = orderService.getHistoryOrders(0, 20);
        Assertions.assertEquals(0, orders.total());
        Assertions.assertTrue(orders.orders().isEmpty());
    }

    @Test
    public void getHistoryOrders() {
        var orderInfo1 = orderService.newOrder(111, 222, 3);
        var orderInfo2 = orderService.newOrder(444, 555, 6);
        var orderInfo3 = orderService.newOrder(777, 888, 9);
        var orderInfo4 = orderService.newOrder(123, 456, 7);
        orderService.closeOrderFromControlled(orderInfo2.roomID());
        var history = orderService.getHistoryOrders(0, 20);
        Assertions.assertEquals(4, history.total());
        Assertions.assertEquals(4, history.orders().size());
        Assertions.assertNull(history.orders().get(0).finishReason());
        Assertions.assertNotNull(history.orders().get(1).finishReason());
        var history2 = orderService.getHistoryOrders(1, 20);
        Assertions.assertEquals(4, history2.total());
        Assertions.assertEquals(3, history2.orders().size());
        Assertions.assertEquals(444, history2.orders().get(0).fromDeviceID());
        Assertions.assertEquals(888, history2.orders().get(1).toDeviceID());
        var history3 = orderService.getHistoryOrders(1, 2);
        Assertions.assertEquals(2, history3.orders().size());
        Assertions.assertEquals(4, history3.total());
        var history4 = orderService.getHistoryOrders(4, 20);
        Assertions.assertEquals(0, history4.orders().size());
        Assertions.assertEquals(4, history4.total());
    }

}
