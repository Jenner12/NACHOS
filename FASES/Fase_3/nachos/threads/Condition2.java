package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;
/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
        this.conditionLock = conditionLock;
        this.value = 0;
        this.blockedThreadsQueue = ThreadedKernel.scheduler.newThreadQueue(false);
        //this.waitQueue = new LinkedList<ThreadQueue>();
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        Lib.debug(dbgCondition, "Sleep Begin: El lockHolder actual era: " + this.conditionLock.getLockHolder());
        conditionLock.release();
        boolean intStatus = Machine.interrupt().disable(); 
        blockedThreadsQueue.waitForAccess(KThread.currentThread());
        KThread.sleep();
        Machine.interrupt().restore(intStatus);
        conditionLock.acquire();

    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        Lib.debug(dbgCondition, "Wake Begin: El lockHolder actual era: " + this.conditionLock.getLockHolder());
        boolean intStatus = Machine.interrupt().disable();
        KThread kt;
        if (! ThreadQueueMethods.ThreadQueueIsEmpty(this.blockedThreadsQueue)){
            kt = blockedThreadsQueue.nextThread();
            kt.ready();
            Lib.debug(dbgCondition, "Se desperto al thread: " + kt);
        }
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        Lib.debug(dbgCondition, "WakeAll Begin: El lockHolder actual era: " + this.conditionLock.getLockHolder());
        boolean intStatus = Machine.interrupt().disable();
        while (!ThreadQueueMethods.ThreadQueueIsEmpty(this.blockedThreadsQueue))
            wake();
        Machine.interrupt().restore(intStatus);
    }

    public ThreadQueue getBlockedThreadsQueue(){
        return this.blockedThreadsQueue;
    }

    public void setBlockedThreadsQueue(ThreadQueue tq){
        KThread kt = null;
        while ((kt = this.blockedThreadsQueue.nextThread()) != null);
        while ((kt = tq.nextThread()) != null)
            this.blockedThreadsQueue.waitForAccess(kt);
        //this.blockedThreadsQueue = tq;
    }

    private static final char dbgCondition = 'C';

    private int value;
    private Lock conditionLock;
    private ThreadQueue blockedThreadsQueue = ThreadedKernel.scheduler.newThreadQueue(false);
}
