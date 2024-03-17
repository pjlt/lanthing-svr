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

package cn.lanthing.ltsocket;

import cn.lanthing.codec.LtMessage;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;


@Slf4j
public class MessageDispatcher {
    record GeneralHandler(Method method, Object object) {}

    private final ExecutorService executorService = Executors.newWorkStealingPool();

    private final ConcurrentMap<Long, Connection> connections = new ConcurrentHashMap<>();

    private final Map<Long, GeneralHandler> messageHandlers = new HashMap<>();

    private final Map<ConnectionEventType, GeneralHandler> sessionEventHandlers = new HashMap<>();

    public MessageDispatcher(Class<?> controllerClass, ApplicationContext applicationContext) throws Exception {
        init(controllerClass, applicationContext);
}
private void init(Class<?> controller, ApplicationContext applicationContext) throws Exception {
    Object controllerObject;
    try {
        controllerObject = applicationContext.getBean(controller);
    } catch (BeansException be) {
        return;
    }
    var methods = controller.getMethods();
    for (var method : methods) {
        var annotations = method.getAnnotations();
        for (var annotation : annotations) {
            if (MessageMapping.class.isAssignableFrom(annotation.getClass())) {
                if (!method.getReturnType().isAssignableFrom(LtMessage.class)) {
                    throw new Exception("Wrong usage of @MessageMapping: return type isn't LtMessage");
                }
                var paramsType = method.getParameterTypes();
                if (paramsType.length < 2) {
                    throw new Exception("Wrong usage of @MessageMapping");
                }
                MessageMapping messageMapping = method.getAnnotation(MessageMapping.class);
                if (paramsType[0].getName().equals("long") && Message.class.isAssignableFrom(paramsType[1])) {
                    messageHandlers.put(messageMapping.proto().ID, new GeneralHandler(method, controllerObject));
                    log.info("Mapping message({}) to handler {}", messageMapping.proto().ID, method.getName());
                } else {
                    throw new Exception("Wrong usage of @MessageMapping");
                }
            } else if (ConnectionEvent.class.isAssignableFrom(annotation.getClass())) {
                ConnectionEvent connectionEvent = method.getAnnotation(ConnectionEvent.class);
                var paramsType = method.getParameterTypes();
                if (paramsType.length != 1) {
                    throw new Exception("Wrong usage of @SessionEvent");
                }
                if (paramsType[0].getName().equals("long")) {
                    boolean added = true;
                    switch (connectionEvent.type()) {
                        case Closed:
                            if (sessionEventHandlers.containsKey(ConnectionEventType.Closed)) {
                                throw new Exception("Duplicated handler " + ConnectionEventType.Closed);
                            }
                            sessionEventHandlers.put(ConnectionEventType.Closed, new GeneralHandler(method, controllerObject));
                            break;
                        case UnexpectedlyClosed:
                            if (sessionEventHandlers.containsKey(ConnectionEventType.UnexpectedlyClosed)) {
                                throw new Exception("Duplicated handler " + ConnectionEventType.Closed);
                            }
                            sessionEventHandlers.put(ConnectionEventType.UnexpectedlyClosed, new GeneralHandler(method, controllerObject));
                            break;
                        case Connected:
                            if (sessionEventHandlers.containsKey(ConnectionEventType.Connected)) {
                                throw new Exception("Duplicated handler " + ConnectionEventType.Connected);
                            }
                            sessionEventHandlers.put(ConnectionEventType.Connected, new GeneralHandler(method, controllerObject));
                            break;
                        default:
                            added = false;
                            break;
                    }
                    if (added) {
                        log.info("Mapping connection event({}) to handler {}", connectionEvent.type(), method.getName());
                    }
                } else {
                    throw new Exception("Wrong usage of @SessionEvent");
                }
            }
        }
    }
}

    public Callable<Void> generateDispatchTask(Connection connection, LtMessage ltMessage) {
        Callable<Void> handlerTask = generateHandlerTask(connection, ltMessage);
        if (handlerTask == null) {
            return null;
        }
        return () -> {
            handlerTask.call();
            connection.takeOnePendingTask();
            return null;
        };
    }

    public void submitDispatchTask(Callable<Void> task) {
        executorService.submit(task);
    }

    public void onConnectionActive(Connection connection) {
        log.debug("Connection {} accepted", connection.ID);
        connections.put(connection.ID, connection);
        var handler = sessionEventHandlers.get(ConnectionEventType.Connected);
        if (handler != null) {
            submitDispatchTask(()->{
                try {
                    handler.method.invoke(handler.object, connection.ID);
                }  catch (Exception e) {
                    log.warn("Handle Connection Connected error: {}", e.toString());
                }
                return null;
            });
        }
    }

    public void onConnectionClosed(Connection connection) {
        log.debug("Connection {} closed", connection.ID);
        connections.remove(connection.ID);
        var handler = sessionEventHandlers.get(ConnectionEventType.Closed);
        if (handler != null) {
            submitDispatchTask(()->{
                try {
                    handler.method.invoke(handler.object, connection.ID);
                } catch (Exception e) {
                    log.warn("Handle Connection NormalClosed error: {}", e.toString());
                }
                return null;
            });
        }
    }

    public void onConnectionUnexpectedClosed(Connection connection) {
        log.debug("Connection {} close unexpectedly", connection.ID);
        connections.remove(connection.ID);
        var handler = sessionEventHandlers.get(ConnectionEventType.UnexpectedlyClosed);
        if (handler != null) {
            submitDispatchTask(()->{
                try {
                    handler.method.invoke(handler.object, connection.ID);
                }  catch (Exception e) {
                    log.warn("Handle Connection UnexpectedClosed error: {}", e.toString());
                }
                return null;
            });
        }
    }

    private Callable<Void> generateHandlerTask(Connection connection, LtMessage ltMessage) {
        var handler = messageHandlers.get(ltMessage.type);
        if (handler == null) {
            log.warn("Unknown message type({})", ltMessage.type);
            return null;
        }
        return () -> {
            var response = (LtMessage)handler.method.invoke(handler.object, connection.ID, ltMessage.protoMsg);
            if (response != null) {
                connection.send(response);
            }
            return null;
        };
    }

    public void send(long connectionID, LtMessage ltMessage) {
        if (ltMessage == null) {
            return;
        }
        Connection conn = connections.get(connectionID);
        if (conn == null) {
            return;
        }
        conn.submitTaskToExecutor(() -> {
            conn.send(ltMessage);
            return null;
        });
    }
}
