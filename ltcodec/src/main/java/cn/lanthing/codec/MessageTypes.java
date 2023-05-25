package cn.lanthing.codec;

public class MessageTypes {
    public static final long kFirstProtocol = 0;

    public static final long kLoginDevice = 1001;
    public static final long kLoginDeviceAck = 1002;
    public static final long kLoginUser = 1003;
    public static final long kLoginUserAck = 1004;
    public static final long kAllocateDeviceID = 1005;
    public static final long kAllocateDeviceIDAck = 1006;

    public static final long kSignalingMessage = 2001;
    public static final long kSignalingMessageAck = 2002;
    public static final long kJoinRoom = 2003;
    public static final long kJoinRoomAck = 2004;

    public static final long kRequestConnection = 3001;
    public static final long kRequestConnectionAck = 3002;
    public static final long kOpenConnection = 3003;
    public static final long kOpenConnectionAck = 3004;
    public static final long kCloseConnection = 3005;

    public static final long kLastProtocol = 0xffffffffL;
}
