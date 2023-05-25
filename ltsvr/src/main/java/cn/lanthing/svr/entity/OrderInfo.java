package cn.lanthing.svr.entity;


import java.util.List;

public class OrderInfo {
    public long fromDeviceID;
    public long toDeviceID;
    public long clientRequestID;
    public String signalingAddress;
    public int signalingPort;
    public String roomID;
    public String serviceID;
    public String clientID;
    public String authToken;
    public String p2pUsername;
    public String p2pPassword;
    public String relayServer;
    public List<String> reflexServers;
}
