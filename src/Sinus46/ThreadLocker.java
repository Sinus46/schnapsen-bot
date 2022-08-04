package Sinus46;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class ThreadLocker {

    private static final Lock lock = new ReentrantLock();
    private static final Condition condition = lock.newCondition();
    public static void pause(){
        lock.lock();
        try {
            condition.await();
        } catch (InterruptedException ignored) {
        } finally {
            lock.unlock();
        }
    }
    public static void resume(){
        lock.lock();
        try {
            condition.signal();
        }finally {
            lock.unlock();
        }
    }
}
