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

package cn.lanthing.sig.service;

import cn.lanthing.codec.LtMessage;
import cn.lanthing.utils.AutoLock;
import cn.lanthing.utils.AutoReentrantLock;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Paths;
import java.util.HashMap;

@Slf4j
public class RoomManager {

    private final AutoReentrantLock lock = new AutoReentrantLock();

    private final HashMap<String, Room> rooms = new HashMap<>();

    public boolean joinRoom(Session session) {
        if (session.getRoomID().equalsIgnoreCase("uninitialized")) {
            log.error("Join room with uninitialized-session");
            return false;
        }
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            Room room = this.rooms.get(session.getRoomID());
            if (room == null) {
                Room newRoom = createNewRoom(session);
                this.rooms.put(session.getRoomID(), newRoom);
            } else {
                boolean success = room.join(session);
                if (!success) {
                    return false;
                }
            }
        }
        return true;
    }

    private Room createNewRoom(Session session) {
        return new Room(session);
    }

    public void leaveRoom(Session session) {
        Room room = null;
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            room = this.rooms.get(session.getRoomID());
            if (room == null) {
                return;
            }
        }
        room.leave(session);
    }

    public boolean relayMessage(Session fromSession, LtMessage message) {
        Room room = null;
        try (AutoLock lockguard = this.lock.lockAsResource()) {
            room = this.rooms.get(fromSession.getRoomID());
            if (room == null) {
                log.error("Room '{}' for session '{}' does not exists", fromSession.getRoomID(), fromSession.getSessionID());
                return false;
            }
        }
        return room.relayMessage(fromSession, message);
    }
}
