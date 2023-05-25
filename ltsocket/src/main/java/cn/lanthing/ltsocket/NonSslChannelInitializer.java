package cn.lanthing.ltsocket;

import cn.lanthing.codec.LtCodec;
import cn.lanthing.codec.Protocol;
import cn.lanthing.ltproto.LtProto;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

import java.util.ArrayList;
import java.util.List;

public class NonSslChannelInitializer extends ChannelInitializer<Channel> {

    private MessageDispatcher messageDispatcher;

    public NonSslChannelInitializer(MessageDispatcher messageDispatcher) throws Exception {
        this.messageDispatcher = messageDispatcher;
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
    protected void initChannel(Channel ch) {
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

}
