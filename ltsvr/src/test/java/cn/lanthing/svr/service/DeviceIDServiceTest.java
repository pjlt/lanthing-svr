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

package cn.lanthing.svr.service;

import cn.lanthing.svr.dao.UnusedIDDao;
import cn.lanthing.svr.dao.UsedIDDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
public class DeviceIDServiceTest {

    @Autowired
    private DeviceIDService deviceIDService;

    @Autowired
    private UnusedIDDao unusedIDDao;

    @Autowired
    private UsedIDDao usedIDDao;

    @BeforeEach
    public void setupDB() {
        unusedIDDao.clear();
        usedIDDao.clear();;
    }

    @Test
    public void allocateDeviceID() {
        final int deviceID = 123456;
        unusedIDDao.insert(deviceID);
        var newID = deviceIDService.allocateDeviceID();
        Assertions.assertNotNull(newID);
        Assertions.assertEquals(deviceID, newID.deviceID());
        Assertions.assertNotNull(newID.cookie());
        Assertions.assertNotSame("", newID.cookie());
    }

    @Test
    public void allocateDeviceIDWithEmptyDB() {
        final int deviceID = 654321;
        var newID = deviceIDService.allocateDeviceID();
        Assertions.assertNull(newID);
    }

    @Test
    public void getUsedDeviceIDAfterAllocate() {
        final int deviceID = 123456;
        unusedIDDao.insert(deviceID);
        var newID = deviceIDService.allocateDeviceID();
        Assertions.assertNotNull(newID);
        var usedID = deviceIDService.getUsedDeviceID(newID.deviceID());
        Assertions.assertEquals(newID.deviceID(), usedID.getDeviceID());
        Assertions.assertEquals(newID.cookie(), usedID.getCookie());
    }

    @Test
    public void getInvalidUsedDeviceID() {
        final int deviceID = 8737284;
        var usedID = deviceIDService.getUsedDeviceID(deviceID);
        Assertions.assertNull(usedID);
    }

    @Test
    public void updateCookie() {
        final int deviceID = 5464535;
        unusedIDDao.insert(deviceID);
        var newID = deviceIDService.allocateDeviceID();
        Assertions.assertNotNull(newID);
        var newCookie = UUID.randomUUID().toString();
        deviceIDService.updateCookie(deviceID, newCookie);
        var usedID = deviceIDService.getUsedDeviceID(deviceID);
        Assertions.assertNotSame(newID.cookie(), usedID.getCookie());
        Assertions.assertSame(newCookie, usedID.getCookie());
    }

    @Test
    public void getDeviceIDStatWithEmptyDB() {
        var stat = deviceIDService.getDeviceIDStat();
        Assertions.assertSame(0, stat.usedCount());
        Assertions.assertSame(0, stat.unUsedCount());
    }

    @Test
    public void getDeviceIDStat() {
        final int ID1 = 5464535;
        final int ID2 = 1234567;
        final int ID3 = 9876541;
        unusedIDDao.insert(ID1);
        unusedIDDao.insert(ID2);
        unusedIDDao.insert(ID3);
        deviceIDService.allocateDeviceID();
        var stat = deviceIDService.getDeviceIDStat();
        Assertions.assertSame(1, stat.usedCount());
        Assertions.assertSame(2, stat.unUsedCount());
    }

}
