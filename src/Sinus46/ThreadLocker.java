package Sinus46;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadLocker {

    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    public void pause(){
        lock.lock();
        try {
            condition.await();
        } catch (InterruptedException ignored) {
        } finally {
            lock.unlock();
        }
    }
    public void resume(){
        lock.lock();
        try {
            condition.signal();
        }finally {
            lock.unlock();
        }
    }
}
