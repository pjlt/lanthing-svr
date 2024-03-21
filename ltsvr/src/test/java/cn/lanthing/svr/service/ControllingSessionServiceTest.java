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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ControllingSessionServiceTest {

    @Autowired
    private ControllingSessionService controllingSessionService;

    @BeforeEach
    public void setupEach() {
        controllingSessionService.clearForTest();
    }

    @Test
    public void addRemoveSession() {
        Long devID0 = controllingSessionService.removeSession(1);
        Assertions.assertNull(devID0);
        controllingSessionService.addSession(1);
        Long devID1 = controllingSessionService.removeSession(1);
        Assertions.assertNotNull(devID1);
        Assertions.assertEquals(0, devID1);
        Long devID2 = controllingSessionService.removeSession(1);
        Assertions.assertNull(devID2);
        Long devID3 = controllingSessionService.removeSession(2);
        Assertions.assertNull(devID3);
        //
        controllingSessionService.addSession(2);
        controllingSessionService.loginDevice(2, 666, 123, "Windows");
        Long devID = controllingSessionService.removeSession(2);
        Assertions.assertEquals(666, devID);
    }

    @Test
    public void loginDevice() {
        boolean success = controllingSessionService.loginDevice(1, 2, 123, "Linux");
        Assertions.assertFalse(success);
        controllingSessionService.addSession(2);
        success = controllingSessionService.loginDevice(2, 3, 123, "Linux");
        Assertions.assertTrue(success);
    }

    @Test
    public void loginDeviceWithSameConnID() {
        controllingSessionService.addSession(3);
        controllingSessionService.loginDevice(3, 4, 345, "Windows");
        boolean success = controllingSessionService.loginDevice(3, 5, 555, "Linux");
        Assertions.assertFalse(success);
    }

    @Test
    public void loginDeviceWithSameDeviceID() {
        controllingSessionService.addSession(4);
        controllingSessionService.loginDevice(4, 666, 777, "macOS");
        boolean success = controllingSessionService.loginDevice(5, 666, 888, "iOS");
        Assertions.assertFalse(success);
    }

    @Test
    public void getSessionByConnectionID() {
        controllingSessionService.addSession(5);
        controllingSessionService.addSession(6);
        var session = controllingSessionService.getSessionByConnectionID(6);
        Assertions.assertEquals(6, session.connectionID());
        Assertions.assertEquals(0, session.deviceID());
        Assertions.assertEquals("", session.os());
        controllingSessionService.loginDevice(5, 456, 678, "iOS");
        controllingSessionService.loginDevice(6, 345, 678, "Android");
        var session2 = controllingSessionService.getSessionByConnectionID(6);
        Assertions.assertEquals(345, session2.deviceID());
        Assertions.assertEquals(678, session2.version());
        Assertions.assertEquals("Android", session2.os());
        var session3 = controllingSessionService.getSessionByConnectionID(5);
        Assertions.assertEquals(456, session3.deviceID());
        Assertions.assertEquals(678, session3.version());
        Assertions.assertEquals("iOS", session3.os());
    }

    @Test
    public void getConnectionIDByDeviceID() {
        controllingSessionService.addSession(7);
        controllingSessionService.addSession(8);
        controllingSessionService.getConnectionIDByDeviceID(777);
        controllingSessionService.getConnectionIDByDeviceID(888);
        controllingSessionService.loginDevice(7, 777, 543, "Windows");
        controllingSessionService.loginDevice(8, 888, 543, "Linux");
        var conn1 = controllingSessionService.getConnectionIDByDeviceID(777);
        var conn2 = controllingSessionService.getConnectionIDByDeviceID(888);
        Assertions.assertEquals(7, conn1);
        Assertions.assertEquals(8, conn2);
    }

    @Test
    public void getSessionCount() {
        int count1 = controllingSessionService.getSessionCount();
        Assertions.assertEquals(0, count1);
        controllingSessionService.addSession(0);
        int count2 = controllingSessionService.getSessionCount();
        Assertions.assertEquals(1, count2);
        controllingSessionService.loginDevice(0, 111, 123, "xxx");
        int count3 = controllingSessionService.getSessionCount();
        Assertions.assertEquals(1, count3);
        controllingSessionService.addSession(1);
        int count4 = controllingSessionService.getSessionCount();
        Assertions.assertEquals(2, count4);
    }
}
