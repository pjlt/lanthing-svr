package cn.lanthing.sig.service;

import cn.lanthing.codec.LtMessage;
import cn.lanthing.codec.MessageType;
import cn.lanthing.sig.handler.MessageHandler;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Set;

@Component
@Slf4j
public class SignalingService {


    private final RoomManager roomManager = new RoomManager();

    private final HashMap<Long, MessageHandler> handlers = new HashMap<>();


    @PostConstruct
    public void init() {
        registerHandlers();
    }

    private void registerHandlers() {
        Reflections reflections = new Reflections(new ConfigurationBuilder().forPackages("cn.lanthing.sig.handler"));
        Set<Class<? extends MessageHandler>> handlerClasses = reflections.getSubTypesOf(MessageHandler.class);
        for (Class<? extends MessageHandler> obj : handlerClasses) {
            this.registerHandler(obj);
        }
    }

    private void registerHandler(Class<? extends MessageHandler> handlerClass) {
        try {
            MessageType type = handlerClass.getAnnotation(MessageType.class);
            if (type == null || type.value() <= 0) {
                return;
            }
            MessageHandler handler = handlerClass.getDeclaredConstructor().newInstance();
            this.handlers.put(type.value(), handler);
            log.info("Registered handler '{}' for type '{}'", handlerClass.getName(), type.value());
        } catch (Exception e) {
            log.error("Register handler failed: {}", e.toString());
        }
    }

    public void handle(Session session, LtMessage message) {
        MessageHandler handler = this.handlers.get(message.type);
        if (handler == null) {
            log.warn("Received unknown message type {}", message.type);
            return;
        }
        try {
            handler.handle(session, message);
        } catch (Exception e) {
            log.error("Handle message exception: {}", e.toString());
        }
    }

    public boolean joinRoom(Session session) {
        return this.roomManager.joinRoom(session);
    }

    public void leaveRoom(Session session) {
        this.roomManager.leaveRoom(session);
    }

    public boolean relayMessage(Session fromSession, LtMessage message) {
        return this.roomManager.relayMessage(fromSession, message);
    }
}
