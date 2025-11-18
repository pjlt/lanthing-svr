package cn.lanthing.codec;

import com.google.protobuf.Message;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.List;

@Slf4j
public class LtWsCodec extends MessageToMessageCodec<WebSocketFrame, LtMessage> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, LtMessage ltMessage, List<Object> list) throws Exception {
        log.debug("LtWsCodec encode");
        var payload = Unpooled.buffer(ltMessage.protoMsg.getSerializedSize()+4);
        payload.writeIntLE((int)ltMessage.type);
        payload.writeBytes(ltMessage.protoMsg.toByteArray());
        BinaryWebSocketFrame binaryFrame = new BinaryWebSocketFrame(payload);
        list.add(binaryFrame);
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, WebSocketFrame webSocketFrame, List<Object> list) throws Exception {
        if (webSocketFrame instanceof BinaryWebSocketFrame) {
            try {
                var binaryFrame = (BinaryWebSocketFrame) webSocketFrame;
                LtMessage message = new LtMessage();
                message.type = binaryFrame.content().readUnsignedIntLE();
                Method createMethod = LtCodec.createMethods.get(message.type);
                if (createMethod == null) {
                    log.warn("Unknown message type: {}", message.type);
                    return;
                }
                byte[] bytes = new byte[binaryFrame.content().readableBytes()];
                binaryFrame.content().readBytes(bytes);
                message.protoMsg = (Message) createMethod.invoke(null, bytes);
                if (message.protoMsg != null) {
                    list.add(message);
                }
            } catch (Exception e) {
                log.error("{}", e.toString());
            } finally {
                // release ??
            }
        } else if (webSocketFrame instanceof TextWebSocketFrame) {
            var textFrame = (TextWebSocketFrame)webSocketFrame;
            log.debug("Received websocket text message: {}", textFrame.text());
        } else {
            log.warn("Unexpected websocket message");
        }
    }
}
