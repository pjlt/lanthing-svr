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
        orderInfo.signalingAddress = signalingConfig.getIP();
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
            controllingDeviceIDToOrderInfoMap.remove(orderInfo.fromDeviceID);
            roomIDToOrderInfoMap.remove(orderInfo.roomID);
        }
    }

    public void setSignalingConfig(SignalingConfig signalingConfig) {
        this.signalingConfig = signalingConfig;
    }

    public void setReflexRelayConfig(ReflexRelayConfig reflexRelayConfig) {
        this.reflexRelayConfig = reflexRelayConfig;
    }
}
