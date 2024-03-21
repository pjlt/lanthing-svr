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

import cn.lanthing.svr.service.ControlledSessionService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ControlledSessionServiceImpl implements ControlledSessionService {

    private enum Status {
        Connected,
        DeviceLogged,
        Disconnected
    }

    private static class SessionInner {

        private final long connectionID;

        private long deviceID = 0;

        private boolean allowControl;

        private Status status;

        private int version = 0;

        private String os = "";

        SessionInner(long connectionID) {
            this.connectionID = connectionID;
        }
    }

    private final Map<Long, SessionInner> connIDToSessionMap = new HashMap<>();

    private final Map<Long, Long> deviceIDToConnIDMap = new HashMap<>();

    //private final AutoReentrantLock lock = new AutoReentrantLock();

    @Override
    public void addSession(long connectionID) {
        var session = new SessionInner(connectionID);
        session.status = Status.Connected;
        synchronized (this) {
            connIDToSessionMap.putIfAbsent(connectionID, session);
        }
    }

    @Override
    public synchronized Long removeSession(long connectionID) {

        var session = connIDToSessionMap.remove(connectionID);
        if (session != null) {
            deviceIDToConnIDMap.remove(session.deviceID);
            return session.deviceID;
        } else {
            return null;
        }

    }

    @Override
    public synchronized boolean loginDevice(long connectionID, long deviceID, boolean allowControl, int version, String os) {
        var session = connIDToSessionMap.get(connectionID);
        if (session == null) {
            return false;
        }
        if (session.status != Status.Connected) {
            //已有设备登录或已断开
            return false;
        }
        session.deviceID = deviceID;
        session.allowControl = allowControl;
        session.status = Status.DeviceLogged;
        session.version = version;
        session.os = os;
        deviceIDToConnIDMap.put(deviceID, connectionID);
        return true;
    }

    @Override
    public synchronized Session getSessionByDeviceID(long deviceID) {
        Long connectionID = deviceIDToConnIDMap.get(deviceID);
        if (connectionID == null) {
            return null;
        }
        var session = connIDToSessionMap.get(connectionID);
        return session == null ? null : new Session(session.connectionID, session.deviceID, session.version, session.os);
    }

    @Override
    public synchronized Session getSessionByConnectionID(long connectionID) {
        var session = connIDToSessionMap.get(connectionID);
        return session == null ? null : new Session(session.connectionID, session.deviceID, session.version, session.os);
    }

    @Override
    public synchronized int getSessionCount() {
        return connIDToSessionMap.size();
    }

    @Override
    public synchronized void clearForTest() {
        connIDToSessionMap.clear();
        deviceIDToConnIDMap.clear();
    }
}
