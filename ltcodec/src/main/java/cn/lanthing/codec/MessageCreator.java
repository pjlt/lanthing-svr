package cn.lanthing.codec;

import com.google.protobuf.Message;

public interface MessageCreator {
    Message parseFrom(byte[] bytes);
}
