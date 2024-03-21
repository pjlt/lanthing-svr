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
public class ControlledSessionServiceTest {

    @Autowired
    private ControlledSessionService controlledSessionService;

    @BeforeEach
    public void setupEach() {
        controlledSessionService.clearForTest();
    }

    @Test
    public void addRemoveSession() {
        Long devID0 = controlledSessionService.removeSession(1);
        Assertions.assertNull(devID0);
        controlledSessionService.addSession(1);
        Long devID1 = controlledSessionService.removeSession(1);
        Assertions.assertNotNull(devID1);
        Assertions.assertEquals(0, devID1);
        Long devID2 = controlledSessionService.removeSession(1);
        Assertions.assertNull(devID2);
        Long devID3 = controlledSessionService.removeSession(2);
        Assertions.assertNull(devID3);
        //
        controlledSessionService.addSession(2);
        controlledSessionService.loginDevice(2, 666, true, 123, "Windows");
        Long devID = controlledSessionService.removeSession(2);
        Assertions.assertEquals(666, devID);
    }

    @Test
    public void loginDevice() {
        boolean success = controlledSessionService.loginDevice(1, 2, true, 123, "Linux");
        Assertions.assertFalse(success);
        controlledSessionService.addSession(2);
        success = controlledSessionService.loginDevice(2, 3, true, 123, "Linux");
        Assertions.assertTrue(success);
    }

    @Test
    public void loginDeviceWithSameConnID() {
        controlledSessionService.addSession(3);
        controlledSessionService.loginDevice(3, 4, true, 345, "Windows");
        boolean success = controlledSessionService.loginDevice(3, 5, true, 555, "Linux");
        Assertions.assertFalse(success);
    }

    @Test
    public void loginDeviceWithSameDeviceID() {
        controlledSessionService.addSession(4);
        controlledSessionService.loginDevice(4, 666, true, 777, "macOS");
        boolean success = controlledSessionService.loginDevice(5, 666, true, 888, "iOS");
        Assertions.assertFalse(success);
    }

    @Test
    public void getSessionByDeviceID() {
        var session1 = controlledSessionService.getSessionByDeviceID(0);
        Assertions.assertNull(session1);
        controlledSessionService.addSession(1);
        var session2 = controlledSessionService.getSessionByDeviceID(0);
        Assertions.assertNull(session2);
        controlledSessionService.loginDevice(1, 111, true, 123, "Windows");
        var session3 = controlledSessionService.getSessionByDeviceID(111);
        Assertions.assertNotNull(session3);
        Assertions.assertEquals(1, session3.connectionID());
        Assertions.assertEquals(111, session3.deviceID());
        Assertions.assertEquals(123, session3.version());
        Assertions.assertEquals("Windows", session3.os());
    }

    @Test
    public void getSessionByConnectionID() {
        controlledSessionService.addSession(5);
        controlledSessionService.addSession(6);
        var session = controlledSessionService.getSessionByConnectionID(6);
        Assertions.assertEquals(6, session.connectionID());
        Assertions.assertEquals(0, session.deviceID());
        Assertions.assertEquals("", session.os());
        controlledSessionService.loginDevice(5, 456, true, 678, "iOS");
        controlledSessionService.loginDevice(6, 345, true, 678, "Android");
        var session2 = controlledSessionService.getSessionByConnectionID(6);
        Assertions.assertEquals(345, session2.deviceID());
        Assertions.assertEquals(678, session2.version());
        Assertions.assertEquals("Android", session2.os());
        var session3 = controlledSessionService.getSessionByConnectionID(5);
        Assertions.assertEquals(456, session3.deviceID());
        Assertions.assertEquals(678, session3.version());
        Assertions.assertEquals("iOS", session3.os());
    }

    @Test
    public void getSessionCount() {
        int count1 = controlledSessionService.getSessionCount();
        Assertions.assertEquals(0, count1);
        controlledSessionService.addSession(0);
        int count2 = controlledSessionService.getSessionCount();
        Assertions.assertEquals(1, count2);
        controlledSessionService.loginDevice(0, 111, true, 123, "xxx");
        int count3 = controlledSessionService.getSessionCount();
        Assertions.assertEquals(1, count3);
        controlledSessionService.addSession(1);
        int count4 = controlledSessionService.getSessionCount();
        Assertions.assertEquals(2, count4);
    }

}
