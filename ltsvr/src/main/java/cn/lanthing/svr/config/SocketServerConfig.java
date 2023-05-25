package cn.lanthing.svr.config;

import cn.lanthing.ltsocket.SocketConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SocketServerConfig {

    @Bean
    @ConfigurationProperties("controlled-socket-svr")
    public SocketConfig controlledSocketConfig() {
        return new SocketConfig();
    }

    @Bean
    @ConfigurationProperties("controlling-socket-svr")
    public SocketConfig controllingSocketConfig() {
        return new SocketConfig();
    }
}
