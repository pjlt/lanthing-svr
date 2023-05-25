package cn.lanthing.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class Protocol extends ByteToMessageCodec<NetPacket> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, NetPacket netPacket, ByteBuf byteBuf) {
        byteBuf.writeMediumLE(netPacket.magic);
        byteBuf.writeByte(netPacket.xorKey);
        byteBuf.writeIntLE((int)netPacket.payloadSize);
        byteBuf.writeIntLE((int)netPacket.checksum);
        byteBuf.writeBytes(netPacket.payload);
        netPacket.payload.release();
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        NetPacket netPacket = new NetPacket();
        if (byteBuf.readableBytes() < NetPacket.kHeaderLength) {
            return;
        }
        netPacket.magic = byteBuf.getUnsignedMediumLE(0);
        if (netPacket.magic != NetPacket.kMagicV1) {
            //该断链
            throw new Exception("xxxx");
        }
        netPacket.xorKey = byteBuf.getUnsignedByte(3);
        netPacket.payloadSize = byteBuf.getUnsignedIntLE(4);
        netPacket.checksum = byteBuf.getUnsignedIntLE(8);
        if (byteBuf.readableBytes() - NetPacket.kHeaderLength < netPacket.payloadSize) {
            return;
        }
        netPacket.payload = byteBuf.readBytes(NetPacket.kHeaderLength + (int)netPacket.payloadSize);
        netPacket.payload.readerIndex(NetPacket.kHeaderLength);
        byteBuf.discardReadBytes();
        list.add(netPacket);
    }
}
