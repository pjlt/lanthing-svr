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

import cn.lanthing.svr.service.ControlledDeviceService;
import cn.lanthing.utils.AutoLock;
import cn.lanthing.utils.AutoReentrantLock;
import com.google.common.base.Strings;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ControlledDeviceServiceImpl implements ControlledDeviceService {

    private enum Status {
        Connected,
        DeviceLogged,
        Disconnected
    }

    private static class Session {

        private final long connectionID;

        private long deviceID;

        private boolean allowControl;

        private String sessionID;

        private Status status;

        Session(long connectionID) {
            this.connectionID = connectionID;
        }
    }

    private final Map<Long, Session> connIDToSessionMap = new HashMap<>();

    private final Map<Long, Long> deviceIDToConnIDMap = new HashMap<>();

    private final AutoReentrantLock lock = new AutoReentrantLock();

    @Override
    public void addSession(long connectionID) {
        var session = new Session(connectionID);
        session.status = Status.Connected;
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            connIDToSessionMap.putIfAbsent(connectionID, session);
        }
    }

    @Override
    public Long removeSession(long connectionID) {
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            var session = connIDToSessionMap.remove(connectionID);
            if (session != null) {
                deviceIDToConnIDMap.remove(session.deviceID);
                return session.deviceID;
            } else {
                return null;
            }
        }
    }

    @Override
    public String loginDevice(long connectionID, long deviceID, boolean allowControl, String sessionID) {
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            var session = connIDToSessionMap.get(connectionID);
            if (session == null) {
                return null;
            }
            if (session.status != Status.Connected) {
                //已有设备登录或已断开
                return null;
            }
            if (Strings.isNullOrEmpty(sessionID)) {
                sessionID = UUID.randomUUID().toString();
            }
            session.sessionID = sessionID;
            session.deviceID = deviceID;
            session.allowControl = allowControl;
            session.status = Status.DeviceLogged;
            deviceIDToConnIDMap.put(deviceID, connectionID);
            return sessionID;
        }
    }

    @Override
    public Long getConnectionIDByDeviceID(long deviceID) {
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            return deviceIDToConnIDMap.get(deviceID);
        }
    }

    @Override
    public Long getDeviceIDByConnectionID(long connectionID) {
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            var session = connIDToSessionMap.get(connectionID);
            return session == null ? null : session.deviceID;
        }
    }
}
