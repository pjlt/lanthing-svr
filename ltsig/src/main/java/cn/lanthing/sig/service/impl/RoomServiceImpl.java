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

package cn.lanthing.sig.service.impl;

import cn.lanthing.sig.entity.Session;
import cn.lanthing.sig.service.RoomService;
import cn.lanthing.utils.AutoLock;
import cn.lanthing.utils.AutoReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RoomServiceImpl implements RoomService {

    private static class Room {

        private final List<Session> sessions = new ArrayList<>();

        private final String roomID;

        public Room(Session session) {
            this.sessions.add(session);
            this.roomID = session.getRoomID();
        }

        public boolean join(Session session) {
            if (this.sessions.size() != 1) {
                log.warn("Join room failed, sessionIDs != 1");
                return false;
            }
            if (this.sessions.get(0).getSessionID().equals(session.getSessionID())) {
                log.warn("Join room failed, session '{}' already in room '{}'", session.getSessionID(), this.roomID);
                return false;
            }
            this.sessions.add(session);
            return true;
        }

        public void leave(long connectionID) {
            this.sessions.removeIf((Session s) -> s.getConnectionID() == connectionID);
        }

        boolean empty() {
            return sessions.isEmpty();
        }

        Session getPeer(long connectionID) {
            if (sessions.isEmpty() || sessions.size()==1) {
                return null;
            }
            if (sessions.get(0).getConnectionID() == connectionID) {
                return sessions.get(1);
            } else {
                return sessions.get(0);
            }
        }

    }

    private final AutoReentrantLock lock = new AutoReentrantLock();

    private final Map<Long, Room> connID2Room = new HashMap<>();

    private final Map<String, Room> roomID2Room = new HashMap<>();


    @Override
    public boolean joinRoom(long connectionID, String roomID, String sessionID) {
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            var session = new Session();
            session.setRoomID(roomID);
            session.setSessionID(sessionID);
            session.setConnectionID(connectionID);
            var room = roomID2Room.get(roomID);
            if (room == null) {
                room = new Room(session);
                roomID2Room.put(roomID, room);
                connID2Room.put(connectionID, room);
                return true;
            }
            boolean success = room.join(session);
            if (success) {
                connID2Room.put(connectionID, room);
                return true;
            }
            return false;
        }
    }

    @Override
    public void leaveRoom(long connectionID) {
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            var room = connID2Room.get(connectionID);
            if (room == null) {
                return;
            }
            connID2Room.remove(connectionID);
            room.leave(connectionID);
            if (room.empty()) {
                roomID2Room.remove(room.roomID);
            }
        }
    }

    @Override
    public Session getPeer(long connectionID) {
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            var room = connID2Room.get(connectionID);
            if (room == null) {
                return null;
            }
            return room.getPeer(connectionID);
        }
    }
}
