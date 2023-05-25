package cn.lanthing.svr.entity;


import lombok.ToString;

@ToString
public class UnusedIDEntity {
    private int id;
    private long deviceID;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(long deviceID) {
        this.deviceID = deviceID;
    }
}
