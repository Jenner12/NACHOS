package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler(){
    	
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

    public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
			       
		return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
			       
		return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());
			       
		Lib.assertTrue(priority >= priorityMinimum &&
			   priority <= priorityMaximum);
		
		getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();
			       
		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
		    return false;

		setPriority(thread, priority+1);

		Machine.interrupt().restore(intStatus);
		return true;
    }

    public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();
			       
		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
		    return false;

		setPriority(thread, priority-1);

		Machine.interrupt().restore(intStatus);
		return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
		    thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
		
		PriorityQueue(boolean transferPriority) {
		    this.transferPriority = transferPriority;
		    this.waitQueue = new LinkedList<KThread>();
		}

		public void waitForAccess(KThread thread) {
		    Lib.assertTrue(Machine.interrupt().disabled());
		    getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
		    Lib.assertTrue(Machine.interrupt().disabled());
		    getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
		    Lib.assertTrue(Machine.interrupt().disabled());
		    // implement me

		    if (this.waitQueue.isEmpty())
		    	return null;
		    //ThreadQueueMethods.PriorityQueueSort(this);
		    //System.out.println("Aplicando nextThread()");

		    Lib.debug('p', "Cola de espera ANTES del ordenamiento!");
		    print();
		    ThreadQueueMethods.PriorityQueueSort(this);
		    //System.out.println("La cola ya esta ordenada");
			Lib.debug('p', "Cola de espera DESPUES del ordenamiento!");
			print();
			//KThread thread = ThreadQueueMethods.ThreadQueueGetFirst(this);
			KThread thread = this.waitQueue.getFirst();
			KThread joinedT = thread.getThreadJoinedTo();
		    //System.out.println("La cola ya esta ordenada222");
		    if (joinedT != null ){  //&& false){
			    while (thread == this.waitQueue.getFirst()){
			    	ThreadState threadS = getThreadState(thread), joinedTS = getThreadState(joinedT);
			    	int effPriorJoinedT = joinedTS.getEffectivePriority(),
			    		effPriorThread = threadS.getEffectivePriority();
			    	long waitTimeJoinedT = joinedTS.getWaitingTime(),
			    		 waitTimeThread = threadS.getWaitingTime();
			    	if (effPriorJoinedT < effPriorThread || 
			    	    (effPriorJoinedT == effPriorThread && waitTimeJoinedT < waitTimeThread)){
			    		threadS.donatePriority();
			    		joinedTS.recievePriority();
		   				ThreadQueueMethods.PriorityQueueSort(this);
		   				thread = this.waitQueue.getFirst();
		   				Lib.debug('p', "thread " + thread + " dono prioridad! -- EffPriority: " + threadS.getEffectivePriority());
		   				Lib.debug('p', "thread " + joinedT + " recibio prioridad! -- EffPriority: " + joinedTS.getEffectivePriority());
		   				//System.out.println("thread " + thread + " dono prioridad! -- EffPriority: " + threadS.getEffectivePriority());
		   				//System.out.println("thread " + joinedT + " recibio prioridad! -- EffPriority: " + joinedTS.getEffectivePriority());
			    	}
			    	else break;
		   		}
		    }
		    else if (this.lockHolder != null && false){
		    	while (thread == this.waitQueue.getFirst()){
			    	ThreadState threadS = getThreadState(thread), holderTS = getThreadState(this.lockHolder);
			    	int effPriorHolderT = holderTS.getEffectivePriority(),
			    		effPriorThread = threadS.getEffectivePriority();
			    	long waitTimeHolderT = holderTS.getWaitingTime(),
			    		 waitTimeThread = threadS.getWaitingTime();
			    	if (effPriorHolderT < effPriorThread || 
			    	    (effPriorHolderT == effPriorThread && waitTimeHolderT < waitTimeThread)){
			    		threadS.donatePriority();
			    		holderTS.recievePriority();
		   				ThreadQueueMethods.PriorityQueueSort(this);
		   				thread = this.waitQueue.getFirst();
		   				Lib.debug('p', "thread " + thread + " dono prioridad! -- EffPriority: " + threadS.getEffectivePriority());
		   				Lib.debug('p', "thread " + this.lockHolder + " recibio prioridad! -- EffPriority: " + holderTS.getEffectivePriority());
		   				//System.out.println("thread " + thread + " dono prioridad! -- EffPriority: " + threadS.getEffectivePriority());
		   				//System.out.println("thread " + this.lockHolder + " recibio prioridad! -- EffPriority: " + holderTS.getEffectivePriority());
			    	}
			    	else break;
		   		}
		    }
		    
		    Lib.debug('p', "Sacando al thread " + thread);

		    //return ThreadQueueMethods.ThreadQueueGetFirst(this);
		    return this.waitQueue.removeFirst();
		    //return thread;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return	the next thread that <tt>nextThread()</tt> would
		 *		return.
		 */
		protected ThreadState pickNextThread() {
		    // implement me
		    ThreadQueueMethods.PriorityQueueSort(this);
		    return getThreadState(ThreadQueueMethods.ThreadQueueGetFirst(this));
		}
		
		public void print() {
		    Lib.assertTrue(Machine.interrupt().disabled());
		    // implement me (if you want)
		    //Lib.debug('p', "Imprimiendo cola!");
	    //DESCOMENTAME
	    //for (Iterator i=waitQueue.iterator(); i.hasNext(); )
		//	System.out.print((KThread) i.next() + " ");
		//
		// BORRAME
		for (ThreadState ts : this.getThreadsStatesList())
			Lib.debug('p', "Thread: " + ts.getThread() + " - EffPriority: " + ts.getEffectivePriority());
		}

		public void addThread(KThread toAdd){
			this.waitQueue.add(toAdd);
		}

		public LinkedList<ThreadState> getThreadsStatesList(){
			LinkedList<ThreadState> list = new LinkedList<ThreadState>();
			for (KThread kt : this.waitQueue){
				list.add(getThreadState(kt));
			}
			return list;
		}

		public void setWaitQueue(LinkedList<KThread> list){
			this.waitQueue = list;
		}

		public void setLockHolder(KThread newHolder){
			this.lockHolder = newHolder;
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;

		// List of KThreads inside this queue
		private LinkedList<KThread> waitQueue;
		private KThread lockHolder;
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param	thread	the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
		    this.thread = thread;
		    this.donatedPriority = this.recievedPriority = 0;
		    this.waitingTime = 0;
		    corr++;
		    //Lib.debug('p', "El thread es: " + thread);
		    //Lib.debug('p', "El correlativo es: " + corr);
		    /*
		    switch(corr){
		    	case 2:
		    		setPriority(4);
		    		break;
		    	case 3:
		    		setPriority(2);
		    		break;
		    	case 4:
		    		setPriority(7);
		    		break;

		    }
		    */
		}

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return	the priority of the associated thread.
		 */
		public int getPriority() {
		    return this.priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return	the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
		    // implement me
		    return this.priority + this.recievedPriority - this.donatedPriority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param	priority	the new priority.
		 */
		public void setPriority(int priority) {
		    if (this.priority == priority)
			return;
		    
		    this.priority = priority;
		    
		    // implement me
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the
		 * resource guarded by <tt>waitQueue</tt>. This method is only called
		 * if the associated thread cannot immediately obtain access.
		 *
		 * @param	waitQueue	the queue that the associated thread is
		 *				now waiting on.
		 *
		 * @see	nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
		    // implement me
		    waitQueue.addThread(this.thread);
		    this.waitingTime = Machine.timer().getTime();
		    this.waitingForQueue = waitQueue;
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see	nachos.threads.ThreadQueue#acquire
		 * @see	nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
		    // implement me
		    waitQueue.setLockHolder(this.thread);
		}	

		public long getWaitingTime(){
			return this.waitingTime;
		}

		public KThread getThread(){
			return this.thread;
		}
		public void donatePriority(){
			this.donatedPriority++;
		}
		public void recievePriority(){
			this.recievedPriority++;
		}

		/** The thread with which this object is associated. */	   
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;


		// The ThreadQueue it is waiting for
		protected PriorityQueue waitingForQueue;
		protected long waitingTime;
		protected int donatedPriority, recievedPriority;
	}
	public static int corr = 0;
}