package cn.lanthing.codec;

import io.netty.buffer.ByteBuf;

public class NetPacket {
    public static final int kMagicV1 = 0x950414;
    public static final int kHeaderLength = 12;
    int magic = kMagicV1; // 只用uint24
    short xorKey; //只用uint8
    long payloadSize; // 只用 uint32
    long checksum; // 只用uint32
    ByteBuf payload;
}
