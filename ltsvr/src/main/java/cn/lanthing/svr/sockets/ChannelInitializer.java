package cn.lanthing.svr.sockets;

import cn.lanthing.ltsocket.MessageDispatcher;
import cn.lanthing.ltsocket.NonSslChannelInitializer;
import cn.lanthing.ltsocket.SecurityConfig;
import cn.lanthing.ltsocket.SslChannelInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChannelInitializer {

    @Autowired
    private MessageDispatcher controllingDispatcher;

    @Autowired
    private MessageDispatcher controlledDispatcher;

    @Autowired
    private SecurityConfig securityConfig;

    @Bean
    public NonSslChannelInitializer controllingNonSslChannelInitializer() throws Exception {
        return new NonSslChannelInitializer(controllingDispatcher);
    }

//    @Bean
//    public SslChannelInitializer controllingSslChannelInitializer() throws Exception {
//        return new SslChannelInitializer(securityConfig, controllingDispatcher);
//    }

    @Bean
    public NonSslChannelInitializer controlledNonSslChannelInitializer() throws Exception {
        return new NonSslChannelInitializer(controlledDispatcher);
    }

//    @Bean
//    public SslChannelInitializer controlledSslChannelInitializer() throws Exception {
//        return new SslChannelInitializer(securityConfig, controlledDispatcher);
//    }

    public void setSecurityConfig(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }
}
