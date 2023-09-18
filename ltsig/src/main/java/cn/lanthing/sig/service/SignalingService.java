/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2023 Zhennan Tu <zhennan.tu@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
