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
