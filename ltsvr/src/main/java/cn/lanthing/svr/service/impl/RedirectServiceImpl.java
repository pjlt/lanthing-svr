/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2024 Zhennan Tu <zhennan.tu@gmail.com>
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

package cn.lanthing.svr.service.impl;

import cn.lanthing.svr.service.RedirectService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RedirectServiceImpl implements RedirectService {

    @Value("${redirect.controlled.host:#{null}}")
    private String controlledHost;

    @Value("${redirect.controlled.port:0}")
    private int controlledPort;

    @Value("${redirect.controlling.host:#{null}}")
    private String controllingHost;

    @Value("${redirect.controlling.port:0}")
    private int controllingPort;

    @Override
    public RedirectedAddress redirectControlled(long connectionID, int version) {
        if (StringUtils.isEmpty(controlledHost) || controlledPort == 0) {
            return null;
        } else {
            return new RedirectedAddress(controlledHost, controlledPort);
        }
    }

    @Override
    public RedirectedAddress redirectControlling(long connectionID, int version) {
        if (StringUtils.isEmpty(controllingHost) || controllingPort == 0) {
            return null;
        } else {
            return new RedirectedAddress(controllingHost, controllingPort);
        }
    }
}
