package cn.lanthing.svr.service;

import cn.lanthing.svr.entity.OrderInfo;

public interface OrderService {

    OrderInfo newOrder(long fromDeviceID, long toDeviceID, long clientRequestID);

    OrderInfo getOrderByControlledDeviceID(long deviceID);

    boolean closeOrderFromControlled(String roomID, long deviceID);

    boolean closeOrderFromControlling(String roomID, long deviceID);

    void controllingDeviceLogout(long deviceID);

    void controlledDeviceLogout(long deviceID);
}
