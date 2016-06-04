package nachos.threads;

import nachos.machine.*;

/**
 * A KThread is a thread that can be used to execute Nachos kernel code. Nachos
 * allows multiple threads to run concurrently.
 *
 * To create a new thread of execution, first declare a class that implements
 * the <tt>Runnable</tt> interface. That class then implements the <tt>run</tt>
 * method. An instance of the class can then be allocated, passed as an
 * argument when creating <tt>KThread</tt>, and forked. For example, a thread
 * that computes pi could be written as follows:
 *
 * <p><blockquote><pre>
 * class PiRun implements Runnable {
 *     public void run() {
 *         // compute pi
 *         ...
 *     }
 * }
 * </pre></blockquote>
 * <p>The following code would then create a thread and start it running:
 *
 * <p><blockquote><pre>
 * PiRun p = new PiRun();
 * new KThread(p).fork();
 * </pre></blockquote>
 */
public class KThread {
    /**
     * Get the current thread.
     *
     * @return	the current thread.
     */
    public static KThread currentThread() {
	   Lib.assertTrue(currentThread != null);
	   return currentThread;
    }
    
    /**
     * Allocate a new <tt>KThread</tt>. If this is the first <tt>KThread</tt>,
     * create an idle thread as well.
     */
    public KThread() {
        this.joinedThreadsQueue = ThreadedKernel.scheduler.newThreadQueue(false);
        if (currentThread != null) {
            tcb = new TCB();
    	}	    
    	else {
    	    readyQueue = ThreadedKernel.scheduler.newThreadQueue(false);
    	    readyQueue.acquire(this);	    

    	    currentThread = this;
    	    tcb = TCB.currentTCB();
    	    name = "main";
    	    restoreState();
    	    createIdleThread();
    	}
    }

    /**
     * Allocate a new KThread.
     *
     * @param	target	the object whose <tt>run</tt> method is called.
     */
    public KThread(Runnable target) {
    	this();
    	this.target = target;
    }

    /**
     * Set the target of this thread.
     *
     * @param	target	the object whose <tt>run</tt> method is called.
     * @return	this thread.
     */
    public KThread setTarget(Runnable target) {
    	Lib.assertTrue(status == statusNew);
    	
    	this.target = target;
    	return this;
    }

    /**
     * Set the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @param	name	the name to give to this thread.
     * @return	this thread.
     */
    public KThread setName(String name) {
    	this.name = name;
    	return this;
    }

    /**
     * Get the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @return	the name given to this thread.
     */     
    public String getName() {
	   return name;
    }

    // IMPLEMENTACION NUESTRA
    /**
    * Set the queue of threads joined to this thread
    *
    * @param    queue    the new joined threads queue to set to this thread
    */
    public void setJoinedThreadsQueue(ThreadQueue queue){
        this.joinedThreadsQueue = queue;
    }

    // IMPLEMENTACIÓN NUESTRA
    /**
    * Set the queue of threads joined to this thread
    *
    * @return    queue    the new joined threads queue to set to this thread
    */
    public ThreadQueue getJoinedThreadsQueue(){
        return this.joinedThreadsQueue;
    }

    /**
     * Get the full name of this thread. This includes its name along with its
     * numerical ID. This name is used for debugging purposes only.
     *
     * @return	the full name given to this thread.
     */
    public String toString() {
	   return (name + " (#" + id + ")");
    }

    /**
     * Deterministically and consistently compare this thread to another
     * thread.
     */
    public int compareTo(Object o) {
    	KThread thread = (KThread) o;

    	if (id < thread.id)
    	    return -1;
    	else if (id > thread.id)
    	    return 1;
    	else
    	    return 0;
    }

    /**
     * Causes this thread to begin execution. The result is that two threads
     * are running concurrently: the current thread (which returns from the
     * call to the <tt>fork</tt> method) and the other thread (which executes
     * its target's <tt>run</tt> method).
     */
    public void fork() {
    	Lib.assertTrue(status == statusNew);
    	Lib.assertTrue(target != null);
    	
    	Lib.debug(dbgThread,
    		  "Forking thread: " + toString() + " Runnable: " + target);

    	boolean intStatus = Machine.interrupt().disable();

    	tcb.start(new Runnable() {
    		public void run() {
    		    runThread();
    		}
    	    });

    	ready();
    	
    	Machine.interrupt().restore(intStatus);
    }

    private void runThread() {
    	begin();
    	target.run();
    	finish();
    }

    private void begin() {
    	Lib.debug(dbgThread, "Beginning thread: " + toString());
    	
    	Lib.assertTrue(this == currentThread);

    	restoreState();

    	Machine.interrupt().enable();
    }

    /**
     * Finish the current thread and schedule it to be destroyed when it is
     * safe to do so. This method is automatically called when a thread's
     * <tt>run</tt> method returns, but it may also be called directly.
     *
     * The current thread cannot be immediately destroyed because its stack and
     * other execution state are still in use. Instead, this thread will be
     * destroyed automatically by the next thread to run, when it is safe to
     * delete this thread.
     */
    public static void finish() {
    	Lib.debug(dbgThread, "Finishing thread: " + currentThread.toString());
    	
    	Machine.interrupt().disable();

    	Machine.autoGrader().finishingCurrentThread();

    	Lib.assertTrue(toBeDestroyed == null);
    	toBeDestroyed = currentThread;

        KThread kt;
        while ((kt = currentThread.getJoinedThreadsQueue().nextThread())!=null){
            Lib.debug(dbgJoin, currentThread + " termino de ejecutar... " + kt + " esta listo para continuar ejecucion!");
            kt.ready();
        }

    	currentThread.status = statusFinished;
    	sleep();
    }

    /**
     * Relinquish the CPU if any other thread is ready to run. If so, put the
     * current thread on the ready queue, so that it will eventually be
     * rescheuled.
     *
     * <p>
     * Returns immediately if no other thread is ready to run. Otherwise
     * returns when the current thread is chosen to run again by
     * <tt>readyQueue.nextThread()</tt>.
     *
     * <p>
     * Interrupts are disabled, so that the current thread can atomically add
     * itself to the ready queue and switch to the next thread. On return,
     * restores interrupts to the previous state, in case <tt>yield()</tt> was
     * called with interrupts disabled.
     */
    public static void yield() {
    	Lib.debug(dbgThread, "Yielding thread: " + currentThread.toString());
    	
    	Lib.assertTrue(currentThread.status == statusRunning);
    	
    	boolean intStatus = Machine.interrupt().disable();

    	currentThread.ready();

    	runNextThread();
    	
    	Machine.interrupt().restore(intStatus);
    }

    /**
     * Relinquish the CPU, because the current thread has either finished or it
     * is blocked. This thread must be the current thread.
     *
     * <p>
     * If the current thread is blocked (on a synchronization primitive, i.e.
     * a <tt>Semaphore</tt>, <tt>Lock</tt>, or <tt>Condition</tt>), eventually
     * some thread will wake this thread up, putting it back on the ready queue
     * so that it can be rescheduled. Otherwise, <tt>finish()</tt> should have
     * scheduled this thread to be destroyed by the next thread to run.
     */
    public static void sleep() {
    	Lib.debug(dbgThread, "Sleeping thread: " + currentThread.toString());
    	
    	Lib.assertTrue(Machine.interrupt().disabled());

        
        if (currentThread.status != statusFinished){
            currentThread.status = statusBlocked;
        }

    	runNextThread();
    }

    /**
     * Moves this thread to the ready state and adds this to the scheduler's
     * ready queue.
     */
    public void ready() {
    	Lib.debug(dbgThread, "Ready thread: " + toString());
    	
    	Lib.assertTrue(Machine.interrupt().disabled());
    	Lib.assertTrue(status != statusReady);
    	
    	status = statusReady;
    	if (this != idleThread)
    	    readyQueue.waitForAccess(this);
    	
    	Machine.autoGrader().readyThread(this);
    }

    /**
     * Waits for this thread to finish. If this thread is already finished,
     * return immediately. This method must only be called once; the second
     * call is not guaranteed to return. This thread must not be the current
     * thread.
     */
    public void join() {
    	Lib.debug(dbgJoin, "Joining to thread: " + toString());
    	Lib.assertTrue(this != currentThread);

        if(this.status == statusFinished){
            Lib.debug(dbgJoin, "Thread " + toString() +" has already finished");
            return;
        }
        else{
            boolean intStatus = Machine.interrupt().disable();
            this.joinedThreadsQueue.waitForAccess(currentThread);
            Lib.debug(dbgJoin, currentThread.toString() + " has correctly joined " + this.toString());
            currentThread.sleep();
            Machine.interrupt().restore(intStatus);
        }
    }

    /**
     * Create the idle thread. Whenever there are no threads ready to be run,
     * and <tt>runNextThread()</tt> is called, it will run the idle thread. The
     * idle thread must never block, and it will only be allowed to run when
     * all other threads are blocked.
     *
     * <p>
     * Note that <tt>ready()</tt> never adds the idle thread to the ready set.
     */
    private static void createIdleThread() {
    	Lib.assertTrue(idleThread == null);
    	idleThread = new KThread(new Runnable() {
    	    public void run() { while (true) yield(); }
    	});
    	idleThread.setName("idle");
    	Machine.autoGrader().setIdleThread(idleThread);
    	idleThread.fork();
    }
    
    /**
     * Determine the next thread to run, then dispatch the CPU to the thread
     * using <tt>run()</tt>.
     */
    private static void runNextThread() {
    	KThread nextThread = readyQueue.nextThread();
    	if (nextThread == null)
    	    nextThread = idleThread;
    	nextThread.run();
    }

    /**
     * Dispatch the CPU to this thread. Save the state of the current thread,
     * switch to the new thread by calling <tt>TCB.contextSwitch()</tt>, and
     * load the state of the new thread. The new thread becomes the current
     * thread.
     *
     * <p>
     * If the new thread and the old thread are the same, this method must
     * still call <tt>saveState()</tt>, <tt>contextSwitch()</tt>, and
     * <tt>restoreState()</tt>.
     *
     * <p>
     * The state of the previously running thread must already have been
     * changed from running to blocked or ready (depending on whether the
     * thread is sleeping or yielding).
     *
     * @param	finishing	<tt>true</tt> if the current thread is
     *				finished, and should be destroyed by the new
     *				thread.
     */
    private void run() {
    	Lib.assertTrue(Machine.interrupt().disabled());

    	Machine.yield();

    	currentThread.saveState();

    	Lib.debug(dbgThread, "Switching from: " + currentThread.toString()
    		  + " to: " + toString());

    	currentThread = this;

    	tcb.contextSwitch();

    	currentThread.restoreState();
    }

    /**
     * Prepare this thread to be run. Set <tt>status</tt> to
     * <tt>statusRunning</tt> and check <tt>toBeDestroyed</tt>.
     */
    protected void restoreState() {
    	Lib.debug(dbgThread, "Running thread: " + currentThread.toString());
    	
    	Lib.assertTrue(Machine.interrupt().disabled());
    	Lib.assertTrue(this == currentThread);
    	Lib.assertTrue(tcb == TCB.currentTCB());

    	Machine.autoGrader().runningThread(this);
    	
    	status = statusRunning;

    	if (toBeDestroyed != null) {
    	    toBeDestroyed.tcb.destroy();
    	    toBeDestroyed.tcb = null;
    	    toBeDestroyed = null;
    	}
    }

    /**
     * Prepare this thread to give up the processor. Kernel threads do not
     * need to do anything here.
     */
    protected void saveState() {
    	Lib.assertTrue(Machine.interrupt().disabled());
    	Lib.assertTrue(this == currentThread);
    }

    private static class Join_And_WaitUntil_Test implements Runnable {
        private int num;
        static int correlativo = 0;
        static KThread compartido;
        public Join_And_WaitUntil_Test(int n){
            this.num = n;
            correlativo++;
            if (correlativo==1){
                compartido = new KThread(new Join_And_WaitUntil_Test(100)).setName("Shared");
                compartido.fork();
            }
        }
        public void run() {
            ///*
            if (this.num > 5 && this.num != 100){
                KThread kt2 = new KThread(new Join_And_WaitUntil_Test(num-5)).setName("KThread2");
                kt2.fork();
                compartido.join();
                kt2.join();
            }
            if (this.num == 1) compartido.join();
            
            
            System.out.println("Desplegando numeros (del " + (num) + " al " + (num+4) + ") cada 15000000 ticks");
            for (int x = num; x < num+5; x++){
                waitUntilSeconds(3.2);
                System.out.println(x);
            }
            currentThread.yield();
        }
    }   
        

    public static class CommunicatorTest implements Runnable{
        private Communicator com;
        private int cantSpeakers, cantListeners, cantLlamadas, secuencia;
        public CommunicatorTest(int secuencia, int cantSpeakers, int cantListeners, int cantLlamadas){
            this.com = new Communicator();
            this.cantSpeakers = cantSpeakers;
            this.cantListeners = cantListeners;
            this.cantLlamadas = cantLlamadas;
            this.secuencia = secuencia;
        }
        public void run(){
            System.out.println("\n\n\n            *** Comenzando CommunicatorTest ***");
            System.out.println("\n-> Se utilizaran " + cantSpeakers + " threads como speakers, llamando cada uno al metodo"+
                                    " speak hasta " + cantLlamadas + " veces");
            System.out.println("-> Se utilizaran " + cantListeners + " threads como listeners, llamando cada uno al metodo"+
                                    " listen hasta " + cantLlamadas + " veces\n");
            KThread[] emisores = new KThread[cantSpeakers];
            KThread[] receptores = new KThread[cantListeners];
            int contS = 0, contL = 0;
            while (!(contS == this.cantSpeakers && contL == this.cantListeners)){
                switch(this.secuencia){
                    case 2:
                        if (contL<cantListeners){
                            receptores[contL] = new KThread(new Receptor(this.com, cantLlamadas, "Receptor"+(contL+1))).setName("Receptor"+(contL+1));
                            receptores[contL++].fork();
                        }
                        if (contS<cantSpeakers){
                            emisores[contS] = new KThread(new Emisor(this.com, cantLlamadas, "Emisor"+(contS+1))).setName("Emisor"+(contS+1));
                            emisores[contS++].fork();
                        }
                    break;
                    case 3:
                        if (contS<cantSpeakers){
                            emisores[contS] = new KThread(new Emisor(this.com, cantLlamadas, "Emisor"+(contS+1))).setName("Emisor"+(contS+1));
                            emisores[contS++].fork();
                        }
                        if (contL<cantListeners && contS==cantSpeakers){
                            receptores[contL] = new KThread(new Receptor(this.com, cantLlamadas, "Receptor"+(contL+1))).setName("Receptor"+(contL+1));
                            receptores[contL++].fork();
                        }
                    break;
                    case 4:
                        if (contL<cantListeners){
                            receptores[contL] = new KThread(new Receptor(this.com, cantLlamadas, "Receptor"+(contL+1))).setName("Receptor"+(contL+1));
                            receptores[contL++].fork();
                        }
                        if (contS<cantSpeakers && contL==cantListeners){
                            emisores[contS] = new KThread(new Emisor(this.com, cantLlamadas, "Emisor"+(contS+1))).setName("Emisor"+(contS+1));
                            emisores[contS++].fork();
                        }                        
                    break;
                    default:
                        if (contS<cantSpeakers){
                            emisores[contS] = new KThread(new Emisor(this.com, cantLlamadas, "Emisor"+(contS+1))).setName("Emisor"+(contS+1));
                            emisores[contS++].fork();
                        }
                        if (contL<cantListeners){
                            receptores[contL] = new KThread(new Receptor(this.com, cantLlamadas, "Receptor"+(contL+1))).setName("Receptor"+(contL+1));
                            receptores[contL++].fork();
                        }
                    break;
                }
            }
            contS = contL = 0;
            while (!(contS == this.cantSpeakers && contL == this.cantListeners)){
                switch(this.secuencia){
                    case 2:
                        if (contL<cantListeners)
                            receptores[contL++].join();
                        if (contS<cantSpeakers)
                            emisores[contS++].join();
                    break;
                    case 3: 
                        if (contS<cantSpeakers)
                            emisores[contS++].join();
                        if (contL<cantListeners && contS == cantSpeakers)
                            receptores[contL++].join();
                    break;
                    case 4:
                        if (contL<cantListeners)
                            receptores[contL++].join();
                        if (contS<cantSpeakers && contL == cantListeners)
                            emisores[contS++].join();
                    break;
                    default:
                    if (contS<cantSpeakers)
                        emisores[contS++].join();
                    if (contL<cantListeners)
                        receptores[contL++].join();
                    break;
                }
            }
            System.out.println("\n        *** CommunicatorTest Termino Ejecucion ***\n");
        }
    }
    public static class Emisor implements Runnable{
        private Communicator com;
        private String nombre;
        private int llamadas;
        public Emisor(Communicator c, int llamadas, String nombre){
            this.com = c;
            this.nombre = nombre;
            this.llamadas = llamadas;
        }
        public void run(){
            java.util.Random r = new java.util.Random();
            int msg = 0;
            for (int x = 0; x < this.llamadas; x++){
                //waitUntilSeconds(r.nextInt(2)+1);
                msg = r.nextInt(100)+1;
                //System.out.println("# " + this.nombre + " esta por enviar el mensaje: " + msg);
                this.com.speak(msg);
                System.out.println("# " + this.nombre + " envio el mensaje: " + msg);
            }
            //System.out.println(this.nombre + " Termino ejecucion");
        }
    }
    public static class Receptor implements Runnable{
        private Communicator com;
        private String nombre;
        private int llamadas;
        public Receptor(Communicator c, int llamadas, String nombre){
            this.com = c;
            this.nombre = nombre;
            this.llamadas = llamadas;
        }
        public void run(){
            java.util.Random r = new java.util.Random();
            int msg = 0;
            for (int x = 0; x < this.llamadas; x++){
                //waitUntilSeconds(r.nextInt(2)+1);
                msg = this.com.listen();
                System.out.println("# " + this.nombre + " recibio el mensaje: " + msg);
            }
            //System.out.println(this.nombre + " Termino ejecucion");
        }
    }
    /**
     * Tests whether this module is working.
     */
    public static void selfTest() {
    	Lib.debug(dbgThread, "Enter KThread.selfTest");

        // PARAMETROS DEL CONSTRUCTOR DE LA CLASE CommunicatorTest:

        // PRIMER PARAMETRO: Orden de creación de threads speaker y listeners.
        // 1-> Intercalados creando un speaker primero y luego un listener
        // 2-> Intercalados creando un listener primero y luego un speaker
        // 3-> Primero todos los speakers y luego todos los listeners
        // 4-> Primero todos los listeners y luego todos los speakers

        // SEGUNDO PARAMETRO: Cantidad de threads que realizaran speaks sobre el Communicator
        // TERCER PARAMETRO: Cantidad de threads que realizaran listens sobre el Communicator
        // CUARTO PARAMETRO: Cantidad de veces que cada uno de los threads anteriores
        //                      realizaran speaks/listens sobre el Communicator
        KThread kt1 = new KThread(new CommunicatorTest(1, 3, 3, 4)).setName("CommunicatorTest");
        kt1.fork();
        kt1.join();
        //System.out.println("SelfTest Termino ejecucion");
        //System.out.println("Terminando el programa... ?");
    }
    private static long waitingSecondsToWaitingTicks(double seconds){
        return (long) seconds*12000000;
    }

    private static void waitUntilSeconds(double seconds){
        ThreadedKernel.alarm.waitUntil(waitingSecondsToWaitingTicks(seconds));
    }

    private static final char dbgThread = 't';
    private static final char dbgJoin = 'j';
    /**
     * Additional state used by schedulers.
     *
     * @see	nachos.threads.PriorityScheduler.ThreadState
     */
    public Object schedulingState = null;

    private static final int statusNew = 0;
    private static final int statusReady = 1;
    private static final int statusRunning = 2;
    private static final int statusBlocked = 3;
    private static final int statusFinished = 4;

    /**
     * The status of this thread. A thread can either be new (not yet forked),
     * ready (on the ready queue but not running), running, or blocked (not
     * on the ready queue and not running).
     */
    private int status = statusNew;
    private String name = "(unnamed thread)";
    private Runnable target;
    private TCB tcb;
    private ThreadQueue joinedThreadsQueue;

    /**
     * Unique identifer for this thread. Used to deterministically compare
     * threads.
     */
    private int id = numCreated++;
    /** Number of times the KThread constructor was called. */
    private static int numCreated = 0;

    private static ThreadQueue readyQueue = null;
    private static KThread currentThread = null;
    private static KThread toBeDestroyed = null;
    private static KThread idleThread = null;
}
