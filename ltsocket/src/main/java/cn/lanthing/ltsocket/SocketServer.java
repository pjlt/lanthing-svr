package cn.lanthing.ltsocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
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
//        sslBoostrap.group(bossGroup, childGroup)
//                .channel(NioServerSocketChannel.class)
//                .localAddress((new InetSocketAddress(config.getIP(), config.getSslPort())))
//                .childHandler(sslChannelInitializer);

        nonSslBoostrap.bind().sync();
//        sslBoostrap.bind().sync();

        log.info("Socket server initialized");
    }

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

    public SslChannelInitializer getSslChannelInitializer() {
        return sslChannelInitializer;
    }

    public void setSslChannelInitializer(SslChannelInitializer sslChannelInitializer) {
        this.sslChannelInitializer = sslChannelInitializer;
    }
}
