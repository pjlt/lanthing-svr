package cn.lanthing.codec;

import com.google.protobuf.Message;

public class LtMessage {
    public long type;
    public Message protoMsg;

    public LtMessage() {
    }

    public LtMessage(long type, Message message) {
        this.type = type;
        this.protoMsg = message;
    }
}
