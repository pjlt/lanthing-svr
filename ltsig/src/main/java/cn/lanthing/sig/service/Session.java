package cn.lanthing.sig.service;

import cn.lanthing.codec.LtMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Session extends ChannelInboundHandlerAdapter {

    public enum Status {
        Closed,
        Connected,
        Logged
    }

    private Status status = Status.Closed;

    private Channel channel;

    private final SignalingService service;

    private String sessionID = "uninitialized";

    private String roomID = "uninitialized";

    public Session(SignalingService service) {
        this.service = service;
    }

    public String getSessionID() {
        return sessionID;
    }

    public String getRoomID() {
        return roomID;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.channel = ctx.channel();
        this.status = Status.Connected;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.service.leaveRoom(this);
        this.status = Status.Closed;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        LtMessage message = (LtMessage) msg;
        service.handle(this, message);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn("Caught exception session_id:{}", sessionID);
        this.service.leaveRoom(this);
        ctx.close();
    }

    public void send(LtMessage message) {
        channel.writeAndFlush(message);
    }

    public boolean joinRoom(String roomID, String sessionID) {
        this.sessionID = sessionID;
        this.roomID = roomID;
        return this.service.joinRoom(this);
    }

    public boolean relayMessage(LtMessage message) {
        return this.service.relayMessage(this, message);
    }
}
