package cn.lanthing.ltsocket;

import cn.lanthing.codec.LtCodec;
import cn.lanthing.codec.LtWsCodec;
import cn.lanthing.ltproto.LtProto;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class WSSslChannelInitializer extends ChannelInitializer<Channel> {
    private SslContext sslContext;

    private MessageDispatcher messageDispatcher;

    private SocketConfig socketConfig;

    private final String path;

    public WSSslChannelInitializer(SocketConfig socketConfig, MessageDispatcher messageDispatcher, String path) throws Exception {
        this.socketConfig = socketConfig;
        this.messageDispatcher = messageDispatcher;
        this.path = path;
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
        ch.pipeline().addLast("http-codec", new HttpServerCodec());
        ch.pipeline().addLast("http-aggr", new HttpObjectAggregator(65535));
        ch.pipeline().addLast("websocket", new WebSocketServerProtocolHandler(path));
        ch.pipeline().addLast("message", new LtWsCodec());
        Connection connection = new Connection(messageDispatcher);
        ch.pipeline().addLast("connection", connection);
    }
}
