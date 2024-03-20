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

package cn.lanthing.svr.service;

import cn.lanthing.svr.model.Order;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface OrderService {

    record BasicOrderInfo(
            int id,
            LocalDateTime start,
            LocalDateTime finish,
            String duration,
            String finishReason,
            long fromDeviceID,
            long toDeviceID
    ) {
        public static BasicOrderInfo createFrom(Order order) {
            var finishAt = order.getFinishedAt();
            if (finishAt == null) {
                finishAt = LocalDateTime.now();
            }
            var duration = Duration.between(order.getCreatedAt(), finishAt);
            return new BasicOrderInfo(
                    order.getId(),
                    order.getCreatedAt(),
                    order.getFinishedAt(),
                    String.format("%d:%02d:%02d", duration.toHoursPart(), duration.toMillisPart(), duration.toSecondsPart()),
                    order.getFinishReason(),
                    order.getFromDeviceID(),
                    order.getToDeviceID()
            );
        }
        void f() {
            var d = Duration.between(start, finish);

        }
    }

    record OrderInfo(
            long fromDeviceID,
            long toDeviceID,
            long clientRequestID,
            String signalingAddress,
            int signalingPort,
            String roomID,
            String serviceID,
            String clientID,
            String authToken,
            String p2pUsername,
            String p2pPassword,
            String relayServer,
            List<String> reflexServers){}

    record HistoryOrders(int total, int index, int limit, List<BasicOrderInfo> orders) {}

    OrderInfo newOrder(long fromDeviceID, long toDeviceID, long clientRequestID);

    Order getOrderByControlledDeviceID(long deviceID);

    boolean closeOrderFromControlled(String roomID, long deviceID);

    boolean closeOrderFromControlling(String roomID, long deviceID);

    void controllingDeviceLogout(long deviceID);

    void controlledDeviceLogout(long deviceID);

    HistoryOrders getHistoryOrders(int index, int limit);
}
