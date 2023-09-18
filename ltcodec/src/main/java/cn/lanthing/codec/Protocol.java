/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2023 Zhennan Tu <zhennan.tu@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
