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
