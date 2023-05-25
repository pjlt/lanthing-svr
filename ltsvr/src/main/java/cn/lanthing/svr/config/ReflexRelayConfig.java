package cn.lanthing.svr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties("server-address")
public class ReflexRelayConfig {
    private List<String> reflexes;

    private List<String> relays;

    public List<String> getReflexes() {
        return reflexes;
    }

    public void setReflexes(List<String> reflexes) {
        this.reflexes = reflexes;
    }

    public List<String> getRelays() {
        return relays;
    }

    public void setRelays(List<String> relays) {
        this.relays = relays;
    }
}
