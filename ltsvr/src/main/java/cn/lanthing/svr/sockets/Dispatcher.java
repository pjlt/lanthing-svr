package cn.lanthing.svr.sockets;

import cn.lanthing.ltsocket.MessageDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Dispatcher {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public MessageDispatcher controllingDispatcher() throws Exception {
        return new MessageDispatcher("cn.lanthing.svr.controller.ControllingController", applicationContext);
    }

    @Bean
    public MessageDispatcher controlledDispatcher() throws Exception {
        return new MessageDispatcher("cn.lanthing.svr.controller.ControlledController", applicationContext);
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
