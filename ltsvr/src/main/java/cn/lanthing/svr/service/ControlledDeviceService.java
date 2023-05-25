package cn.lanthing.svr.service;

public interface ControlledDeviceService {

    void addSession(long connectionID);

    Long removeSession(long connectionID);

    String loginDevice(long connectionID, long deviceID, boolean allowControl, String sessionID);

    Long getConnectionIDByDeviceID(long deviceID);

    Long getDeviceIDByConnectionID(long connectionID);
}
