package cn.lanthing.ltsocket;

import cn.lanthing.codec.LtMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
 class Connection extends ChannelInboundHandlerAdapter {
    public enum Status {
        Closed,
        Connected,
    }

    private Channel channel;

    private final MessageDispatcher messageDispatcher;

    private boolean hasFlyingTask = false;

    private Status status = Status.Closed;

    private static final AtomicLong counter;
    static {
        counter = new AtomicLong(0);
    }

    public final long ID;

    private final Queue<Callable<Void>> pendingTasks = new ArrayDeque<>();

    public Connection(MessageDispatcher dispatcher) {
        ID = counter.incrementAndGet();
        messageDispatcher = dispatcher;
    }

    public void submitTaskToExecutor(Callable<Void> task) {
        channel.eventLoop().submit(task);
    }

    public void send(LtMessage ltMessage) {
        if (!channel.eventLoop().inEventLoop()) {
            channel.eventLoop().submit(() -> {
                channel.writeAndFlush(ltMessage);
            });
        } else {
            channel.writeAndFlush(ltMessage);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        //为什么不在构造函数就把channel保存起来？
        //似乎是因为channel有状态（或者是另一个channel？），在channelActive保存起来的channel，
        //使用channel.write()才能正确传递给我们设置的Protocol和LtCodec
        status = Status.Connected;
        channel = ctx.channel();
        messageDispatcher.onConnectionActive(this);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        status = Status.Closed;
        messageDispatcher.onConnectionClosed(this);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        LtMessage message = (LtMessage) msg;
        Callable<Void> task = messageDispatcher.generateDispatchTask(this, message);
        if (hasFlyingTask) {
            pendingTasks.add(task);
        } else {
            messageDispatcher.submitDispatchTask(task);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        status = Status.Closed;
        ctx.close();
        messageDispatcher.onConnectionUnexpectedClosed(this);
    }

    public void takeOnePendingTask() {
        if (!channel.eventLoop().inEventLoop()) {
            channel.eventLoop().submit((Callable<Void>) () -> {
                takeOnePendingTask();
                return null;
            });
            return;
        }
        Callable<Void> task = pendingTasks.poll();
        if (task == null) {
            hasFlyingTask = false;
            return;
        }
        messageDispatcher.submitDispatchTask(task);
    }
}
