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

    @Autowired(required = false)
    private SslChannelInitializer controllingSslChannelInitializer;

    @Autowired
    private NonSslChannelInitializer controlledNonSslChannelInitializer;

    @Autowired(required = false)
    private SslChannelInitializer controlledSslChannelInitializer;

    private SocketServer controlledSocketServer;

    private SocketServer controllingSocketServer;

    @PostConstruct
    public void init() throws Exception {
        controlledSocketServer = new SocketServer(controlledSocketConfig, controlledNonSslChannelInitializer, controlledSslChannelInitializer);
        controllingSocketServer = new SocketServer(controllingSocketConfig, controllingNonSslChannelInitializer, controllingSslChannelInitializer);
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
