package cn.lanthing.utils;

import java.util.concurrent.locks.ReentrantLock;

public class AutoReentrantLock extends ReentrantLock {
    public AutoLock lockAsResource() {
        lock();
        return this::unlock;
    }
}
