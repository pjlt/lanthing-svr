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

package cn.lanthing.svr.controller;

import cn.lanthing.svr.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
public class ManagerController {

    public record JsonResult<T>(
            int code,
            String msg,
            T data
    ) {}

    public record OnlineDevices(int controlling, int controlled, int total) {}

    public record Devices(int used, int unused, int total, OnlineDevices online){}

    @Autowired
    private DeviceIDService deviceIDService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ControlledSessionService controlledSessionService;

    @Autowired
    private ControllingSessionService controllingSessionService;

    @Autowired
    private OnlineStatisticService onlineStatisticService;

    @GetMapping("/mgr/devices")
    public JsonResult<Devices> devices() {
        var stat = deviceIDService.getDeviceIDStat();
        var controlledCount = controlledSessionService.getSessionCount();
        var controllingCount = controllingSessionService.getSessionCount();
        return new JsonResult<>(
                0, "ok", new Devices(stat.usedCount(), stat.unUsedCount(), stat.usedCount() + stat.unUsedCount(),
                        new OnlineDevices(controllingCount, controlledCount, controlledCount + controllingCount)));
    }

    @GetMapping("/mgr/devices/online")
    public JsonResult<List<OnlineStatisticService.OnlineHistory>> onlineHistory(@RequestParam("index") int index , @RequestParam("limit") int limit) {
        limit = Math.min(144, limit);
        return new JsonResult<>(0, "ok", onlineStatisticService.query(index, limit));
    }

    @GetMapping("/mgr/orders")
    public JsonResult<OrderService.HistoryOrders> orders(@RequestParam("index") int index , @RequestParam("limit") int limit) {
        final int kMaxOrdersPerQuery = 50;
        limit = Math.min(limit, kMaxOrdersPerQuery);
        return new JsonResult<>(0, "ok", orderService.getHistoryOrders(index, limit));
    }
}
