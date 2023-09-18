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

import com.google.protobuf.Message;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.ByteProcessor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

@Slf4j
public class LtCodec extends MessageToMessageCodec<NetPacket, LtMessage> {

    public static final class MsgType {
        public final long ID;
        public final String className;

        public MsgType(long id, String name) {
            ID = id;
            className = name;
        }
    }

    private final static HashMap<Long, Method> createMethods = new HashMap<>();

    private final static Random random = new Random();

    private static boolean initialized = false;

    public static synchronized void initialize(List<MsgType> msgTypes) throws Exception {
        if (initialized) {
            return;
        }
        initialized = true;
        for (var msgType : msgTypes) {
            createMethods.put(msgType.ID, Class.forName(msgType.className).getDeclaredMethod("parseFrom", byte[].class));
        }
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, LtMessage ltMessage, List<Object> list) {
        NetPacket netPacket = new NetPacket();
        netPacket.magic = NetPacket.kMagicV1;
        netPacket.xorKey = (byte)random.nextInt();
        if (netPacket.xorKey == 0) {
            netPacket.xorKey = 837;
        }
        netPacket.xorKey = 0;
        netPacket.payloadSize = ltMessage.protoMsg.getSerializedSize() + 4;
        netPacket.payload = Unpooled.buffer((int)netPacket.payloadSize);
        netPacket.payload.writeIntLE((int)ltMessage.type);
        netPacket.payload.writeBytes(ltMessage.protoMsg.toByteArray());
//        netPacket.payload.forEachByte(new ByteProcessor() {
//            private int i = 0;
//            @Override
//            public boolean process(byte b) throws Exception {
//                netPacket.payload.setByte(i, b ^ netPacket.xorKey);
//                i++;
//                return true;
//            }
//        });
        netPacket.checksum = 0;
        list.add(netPacket);
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, NetPacket netPacket, List<Object> list) throws Exception {
        try {
            LtMessage message = new LtMessage();
            if (netPacket.xorKey != 0) {
                netPacket.payload.forEachByte(new ByteProcessor() {
                    private int i = 0;

                    @Override
                    public boolean process(byte b) {
                        netPacket.payload.setByte(i, b ^ netPacket.xorKey);
                        i++;
                        return true;
                    }
                });
            }

            message.type = netPacket.payload.readUnsignedIntLE();
            Method createMethod = LtCodec.createMethods.get(message.type);
            if (createMethod == null) {
                log.warn("Unknown message type: {}", message.type);
                return;
            }
            byte[] bytes = new byte[netPacket.payload.readableBytes()];
            netPacket.payload.readBytes(bytes);
            message.protoMsg = (Message) createMethod.invoke(null, bytes);
            if (message.protoMsg != null) {
                list.add(message);
            }
        } catch (Exception e) {
            log.error("{}", e.toString());
        } finally {
            netPacket.payload.release();
        }

    }
}
