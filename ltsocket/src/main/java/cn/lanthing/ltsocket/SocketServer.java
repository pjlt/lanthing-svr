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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
@Getter
@Setter
public class SocketServer {

    private SocketConfig config;

    private NonSslChannelInitializer nonSslChannelInitializer;

    private SslChannelInitializer sslChannelInitializer;

    private NioEventLoopGroup bossGroup;

    private NioEventLoopGroup childGroup;

    public SocketServer(SocketConfig socketConfig, NonSslChannelInitializer nonSslChannelInitializer, SslChannelInitializer sslChannelInitializer) throws Exception {
        this.config = socketConfig;
        this.nonSslChannelInitializer = nonSslChannelInitializer;
        this.sslChannelInitializer = sslChannelInitializer;
        init();
    }

    public void init() throws Exception {
        bossGroup = new NioEventLoopGroup();
        childGroup = new NioEventLoopGroup();
        ServerBootstrap sslBoostrap = new ServerBootstrap();
        ServerBootstrap nonSslBoostrap = new ServerBootstrap();

        nonSslBoostrap.group(bossGroup, childGroup)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(config.getIP(), config.getPort()))
                .childHandler(nonSslChannelInitializer);
        if (sslChannelInitializer != null) {
            sslBoostrap.group(bossGroup, childGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress((new InetSocketAddress(config.getIP(), config.getSslPort())))
                    .childHandler(sslChannelInitializer);
        }


        nonSslBoostrap.bind().sync();
        if (sslChannelInitializer != null) {
            sslBoostrap.bind().sync();
        }

        log.info("Socket server initialized");
    }

    public void stop() throws Exception {
        bossGroup.shutdownGracefully().sync();
        childGroup.shutdownGracefully().sync();
    }

}
