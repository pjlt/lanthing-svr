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

package cn.lanthing.svr.service.impl;

import cn.lanthing.svr.service.VersionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


import java.io.File;
import java.util.List;

@Service
@Slf4j
public class VersionServiceImpl implements VersionService {

    record VersionFile(List<Version> versions) {}

    private final Object flock = new Object();

    @Value("${version.file}")
    private String versionFileName;

    private VersionFile versionFile;

    public void reloadVersionsFile() {
        ObjectMapper om = new ObjectMapper();
        VersionFile newVersionFile;
        try {
            newVersionFile = om.readValue(new File(versionFileName), VersionFile.class);
        }  catch (Exception e) {
            log.error("Read version from {} failed: {}", versionFileName, e.toString());
            return;
        }
        if (newVersionFile == null) {
            log.error("Read version from {} success with null", versionFileName);
            return;
        }
        if (CollectionUtils.isEmpty(newVersionFile.versions())) {
            log.error("Read version from {} success but 'versions' is empty", versionFileName);
            return;
        }
        synchronized (flock) {
            this.versionFile = newVersionFile;
        }
    }

    @Override
    public Version getNewVersionPC(int clientMajor, int clientMinor, int clientPatch) {
        VersionFile vf;
        synchronized (flock) {
            vf = this.versionFile;
        }
        if (vf == null || CollectionUtils.isEmpty(vf.versions())) {
            return null;
        }
        long clientVersion = clientMajor * 1_000_000L + clientMinor * 1_000L + clientPatch;
        // NOTE: 顺序由提供versions.json的人保证，排最前的就是最新的
        Version newest = vf.versions().get(0);
        long newestVersion = newest.major() * 1_000_000L + newest.minor() * 1_000L + newest.patch();
        if (clientVersion >= newestVersion) {
            return null;
        }
        return newest;
    }

    @Override
    public Version getNewVersionAndroid(int clientMajor, int clientMinor, int clientPatch) {
        return null;
    }

    @Override
    public Version getNewVersionIOS(int clientMajor, int clientMinor, int clientPatch) {
        return null;
    }
}
