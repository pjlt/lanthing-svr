package cn.lanthing.svr.sockets;

import cn.lanthing.ltsocket.NonSslChannelInitializer;
import cn.lanthing.ltsocket.SocketConfig;
import cn.lanthing.ltsocket.SocketServer;
import cn.lanthing.ltsocket.SslChannelInitializer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Server {
    @Autowired
    private SocketConfig controlledSocketConfig;

    @Autowired
    private SocketConfig controllingSocketConfig;

    @Autowired
    private NonSslChannelInitializer controllingNonSslChannelInitializer;

//    @Autowired
//    private SslChannelInitializer controllingSslChannelInitializer;

    @Autowired
    private NonSslChannelInitializer controlledNonSslChannelInitializer;

//    @Autowired
//    private SslChannelInitializer controlledSslChannelInitializer;

    private SocketServer controlledSocketServer;

    private SocketServer controllingSocketServer;

    @PostConstruct
    public void init() throws Exception {
        controlledSocketServer = new SocketServer(controlledSocketConfig, controlledNonSslChannelInitializer, null); //controlledSslChannelInitializer);
        controllingSocketServer = new SocketServer(controllingSocketConfig, controllingNonSslChannelInitializer, null); //controllingSslChannelInitializer);
    }

    @PreDestroy
    public void uninit() throws Exception {
        if (controlledSocketServer != null) {
            controlledSocketServer.stop();
        }
        if (controllingSocketServer != null) {
            controllingSocketServer.stop();
        }
    }

}
