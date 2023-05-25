package cn.lanthing.sig.sockets;

import cn.lanthing.codec.LtCodec;
import cn.lanthing.codec.Protocol;
import cn.lanthing.ltproto.LtProto;
import cn.lanthing.sig.service.Session;
import cn.lanthing.sig.service.SignalingService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class NonSslChannelInitializer extends ChannelInitializer<Channel> {

    @Autowired
    private SignalingService signalingService;

    @PostConstruct
    public void init() throws Exception {
        List<LtCodec.MsgType> msgTypes = new ArrayList<>();
        for (var msgType : LtProto.values()) {
            msgTypes.add(new LtCodec.MsgType(msgType.ID, msgType.className));
        }
        LtCodec.initialize(msgTypes);
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ch.pipeline().addLast("protocol", new Protocol());
        ch.pipeline().addLast("message", new LtCodec());
        Session session = new Session(signalingService);
        ch.pipeline().addLast(session);
    }
}
