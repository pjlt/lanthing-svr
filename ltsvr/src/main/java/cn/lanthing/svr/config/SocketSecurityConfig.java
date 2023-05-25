package cn.lanthing.svr.config;

import cn.lanthing.ltsocket.SecurityConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SocketSecurityConfig {

    @Bean
    @ConfigurationProperties("ssl-security")
    public SecurityConfig securityConfig() {
        return new SecurityConfig();
    }
}
