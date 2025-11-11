package cn.lanthing.ltsocket;

import cn.lanthing.codec.LtCodec;
import cn.lanthing.codec.LtWsCodec;
import cn.lanthing.ltproto.LtProto;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

import java.util.ArrayList;
import java.util.List;

public class WSNonSslChannelInitializer extends ChannelInitializer<Channel> {

    private final MessageDispatcher messageDispatcher;

    private final String path;

    public WSNonSslChannelInitializer(MessageDispatcher messageDispatcher, String path) throws Exception {
        this.messageDispatcher = messageDispatcher;
        this.path = path;
        init();
    }

    public void init() throws Exception {
        List<LtCodec.MsgType> msgTypes = new ArrayList<>();
        for (var msgType : LtProto.values()) {
            msgTypes.add(new LtCodec.MsgType(msgType.ID, msgType.className));
        }
        LtCodec.initialize(msgTypes);
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ch.pipeline().addLast("http-codec", new HttpServerCodec());
        ch.pipeline().addLast("http-aggr", new HttpObjectAggregator(65535));
        ch.pipeline().addLast("websocket", new WebSocketServerProtocolHandler(path));
        ch.pipeline().addLast("message", new LtWsCodec());
        Connection connection = new Connection(messageDispatcher);
        ch.pipeline().addLast("connection", connection);
    }
}
