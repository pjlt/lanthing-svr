package cn.lanthing.utils;

public interface AutoLock extends AutoCloseable {
    @Override
    void close();
}
