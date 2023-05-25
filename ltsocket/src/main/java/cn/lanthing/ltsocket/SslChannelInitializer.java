package cn.lanthing.ltsocket;

import cn.lanthing.codec.LtCodec;
import cn.lanthing.codec.Protocol;
import cn.lanthing.ltproto.LtProto;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SslChannelInitializer extends ChannelInitializer<Channel> {

    private SslContext sslContext;

    private MessageDispatcher messageDispatcher;

    private SecurityConfig securityConfig;

    public SslChannelInitializer(SecurityConfig securityConfig, MessageDispatcher messageDispatcher) throws Exception {
        this.securityConfig = securityConfig;
        this.messageDispatcher = messageDispatcher;
        init();
    }

    private void init() throws Exception {
        Path certPath = Paths.get(securityConfig.getCertsFolder(), securityConfig.getCertChainFile());
        Path keyPath = Paths.get(securityConfig.getCertsFolder(), securityConfig.getPrivateKeyFile());
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

    public MessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }

    public void setMessageDispatcher(MessageDispatcher messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
    }

    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    public void setSecurityConfig(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

}
