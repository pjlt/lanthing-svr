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
