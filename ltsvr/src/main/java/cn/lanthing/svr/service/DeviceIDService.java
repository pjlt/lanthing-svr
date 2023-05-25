package cn.lanthing.svr.service;

public interface DeviceIDService {
    Long allocateDeviceID();

    boolean isValidDeviceID(long deviceID);

}
