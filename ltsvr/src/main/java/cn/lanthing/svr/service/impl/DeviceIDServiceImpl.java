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

import cn.lanthing.svr.dao.UnusedIDDao;
import cn.lanthing.svr.dao.UsedIDDao;
import cn.lanthing.svr.model.UnusedID;
import cn.lanthing.svr.model.UsedID;
import cn.lanthing.svr.service.DeviceIDService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class DeviceIDServiceImpl implements DeviceIDService {

    @Autowired
    private UnusedIDDao unusedIDDao;

    @Autowired
    private UsedIDDao usedIDDao;

    @Override
    public DeviceCookiePair allocateDeviceID() {
        long deviceID;
        String cookie = UUID.randomUUID().toString();
        synchronized (this) {
            UnusedID unnUsedID = unusedIDDao.getNextDeviceID();
            if (unnUsedID == null) {
                log.error("Get next unused deviceID failed");
                return null;
            }
            deviceID = unnUsedID.getDeviceID();
            unusedIDDao.deleteDeviceID(deviceID);
            usedIDDao.addDeviceID(deviceID, cookie);
        }
        log.info("Allocated new deviceID '{}'", deviceID);
        return new DeviceCookiePair(deviceID, cookie);
    }

    @Override
    public synchronized UsedID getUsedDeviceID(long deviceID) {
        return usedIDDao.queryByDeviceID(deviceID);
    }

    @Override
    public synchronized void updateCookie(long deviceID, String cookie) {
        usedIDDao.updateCookie(deviceID, cookie);
    }

    @Override
    public synchronized DeviceIDStat getDeviceIDStat() {
        var usedCount = usedIDDao.countID();
        var unUsedCount = unusedIDDao.countID();
        return new DeviceIDStat(usedCount, unUsedCount);
    }

}
