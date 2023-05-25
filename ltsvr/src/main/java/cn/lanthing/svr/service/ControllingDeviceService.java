package cn.lanthing.svr.service;

public interface ControllingDeviceService {

    void addSession(long connectionID);

    Long removeSession(long connectionID);

    String loginDevice(long connectionID, long deviceID, String sessionID);

    Long getDeviceIDByConnectionID(long connectionID);

    Long getConnectionIDByDeviceID(long deviceID);
}
