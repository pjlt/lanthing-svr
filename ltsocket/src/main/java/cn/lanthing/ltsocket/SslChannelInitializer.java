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

package cn.lanthing.ltsocket;

import cn.lanthing.codec.LtCodec;
import cn.lanthing.codec.Protocol;
import cn.lanthing.ltproto.LtProto;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import lombok.Getter;
import lombok.Setter;

import javax.net.ssl.SSLEngine;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SslChannelInitializer extends ChannelInitializer<Channel> {

    private SslContext sslContext;

    private MessageDispatcher messageDispatcher;

    private SocketConfig socketConfig;

    public SslChannelInitializer(SocketConfig socketConfig, MessageDispatcher messageDispatcher) throws Exception {
        this.socketConfig = socketConfig;
        this.messageDispatcher = messageDispatcher;
        init();
    }

    private void init() throws Exception {
        Path certPath = Paths.get(socketConfig.getCertsFolder(), socketConfig.getCertChainFile());
        Path keyPath = Paths.get(socketConfig.getCertsFolder(), socketConfig.getPrivateKeyFile());
        sslContext = SslContextBuilder.forServer(certPath.toFile(), keyPath.toFile()).build();
        List<LtCodec.MsgType> msgTypes = new ArrayList<>();
        for (var msgType : LtProto.values()) {
            msgTypes.add(new LtCodec.MsgType(msgType.ID, msgType.className));
        }
        LtCodec.initialize(msgTypes);
    }


    @Override
    protected void initChannel(Channel ch) {
        SSLEngine sslEngine = sslContext.newEngine(ch.alloc());
        sslEngine.setUseClientMode(false);
        SslHandler sslHandler = new SslHandler(sslEngine);
        ch.pipeline().addFirst("ssl", sslHandler);
        ch.pipeline().addLast("protocol", new Protocol());
        ch.pipeline().addLast("message", new LtCodec());
        Connection connection = new Connection(messageDispatcher);
        ch.pipeline().addLast("connection", connection);
    }

}
