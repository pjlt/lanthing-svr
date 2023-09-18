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

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Room {
    public enum Status {
        OneSession,
        TwoSession,
        CloseWait,
    }
    private final List<Session> sessions = new ArrayList<>();

    private final AutoReentrantLock lock = new AutoReentrantLock();

    private Status status;

    private final String roomID;

    public Room(Session session) {
        this.sessions.add(session);
        this.roomID = session.getRoomID();
        this.status = Status.OneSession;
    }

    public boolean join(Session session) {
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            if (this.sessions.size() != 1) {
                log.warn("Join room failed, sessionIDs != 1");
                return false;
            }
            if (this.sessions.get(0).getSessionID().equals(session.getSessionID())) {
                log.warn("Join room failed, session '{}' already in room '{}'", session.getSessionID(), this.roomID);
                return false;
            }
            this.sessions.add(session);
            this.status = Status.TwoSession;
            return true;
        }
    }

    public void leave(Session session) {
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            this.sessions.removeIf((Session s) -> s.getSessionID().equals(session.getSessionID()));
            if (sessions.size() == 0) {
                this.status = Status.CloseWait;
                //TODO: 通知RoomManager删除
            } else {
                this.status = Status.OneSession;
            }
        }
    }

    public boolean relayMessage(Session fromSession, LtMessage message) {
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            for (Session currentSession : this.sessions) {
                if (currentSession.getSessionID().equals(fromSession.getSessionID())) {
                    continue;
                }
                currentSession.send(message);
                return true;
            }
            return false;
        }
    }
}
