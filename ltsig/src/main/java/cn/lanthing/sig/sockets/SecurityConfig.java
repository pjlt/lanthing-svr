package cn.lanthing.sig.sockets;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("socket-svr.security")
public class SecurityConfig {
    private String certsFolder;

    private String certChainFile;

    private String privateKeyFile;

    public String getCertsFolder() {
        return certsFolder;
    }

    public void setCertsFolder(String certsFolder) {
        this.certsFolder = certsFolder;
    }

    public String getCertChainFile() {
        return certChainFile;
    }

    public void setCertChainFile(String certChainFile) {
        this.certChainFile = certChainFile;
    }

    public String getPrivateKeyFile() {
        return privateKeyFile;
    }

    public void setPrivateKeyFile(String privateKeyFile) {
        this.privateKeyFile = privateKeyFile;
    }
}
