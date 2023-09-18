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

package cn.lanthing.sig.sockets;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

@Component
@Slf4j
public class SocketServer {
    @Autowired
    private SocketConfig config;

    @Autowired
    private NonSslChannelInitializer nonSslChannelInitializer;

//    @Autowired
//    private  SslChannelInitializer sslChannelInitializer;

    private NioEventLoopGroup bossGroup;

    private NioEventLoopGroup childGroup;

    @PostConstruct
    public void start() throws Exception {
        bossGroup = new NioEventLoopGroup();
        childGroup = new NioEventLoopGroup();
        ServerBootstrap sslBoostrap = new ServerBootstrap();
        ServerBootstrap nonSslBoostrap = new ServerBootstrap();
        nonSslBoostrap.group(bossGroup, childGroup)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(config.getIP(), config.getPort()))
                .childHandler(nonSslChannelInitializer);
//        sslBoostrap.group(bossGroup, childGroup)
//                .channel(NioServerSocketChannel.class)
//                .localAddress((new InetSocketAddress(config.getIP(), config.getSslPort())))
//                .childHandler(sslChannelInitializer);

        nonSslBoostrap.bind().sync();
//        sslBoostrap.bind().sync();

        log.info("Socket server initialized");
    }

    @PreDestroy
    public void stop() throws Exception {
        bossGroup.shutdownGracefully().sync();
        childGroup.shutdownGracefully().sync();
    }

    public SocketConfig getConfig() {
        return config;
    }

    public void setConfig(SocketConfig config) {
        this.config = config;
    }

    public NonSslChannelInitializer getNonSslChannelInitializer() {
        return nonSslChannelInitializer;
    }

    public void setNonSslChannelInitializer(NonSslChannelInitializer nonSslChannelInitializer) {
        this.nonSslChannelInitializer = nonSslChannelInitializer;
    }

//    public SslChannelInitializer getSslChannelInitializer() {
//        return sslChannelInitializer;
//    }
//
//    public void setSslChannelInitializer(SslChannelInitializer sslChannelInitializer) {
//        this.sslChannelInitializer = sslChannelInitializer;
//    }
}
