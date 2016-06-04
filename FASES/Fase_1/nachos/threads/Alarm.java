package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
        if (lista.size() > 0){
            int size = lista.size();
            for (int cont = 0; cont < size; cont++){
                long actual = Machine.timer().getTime();
                KthreadsAndTimes tyt = lista.get(cont);
                if (actual >= tyt.time){
                    //System.out.println("Los siguientes threads estan en espera: ");
                    //for (KthreadsAndTimes t: lista){
                    //    System.out.println("Thread " + t.thread.getName());
                    //}
                    Lib.debug('w', "El tiempo del thread " + tyt.thread.getName() + " se ha cumplido, removiendolo de la cola de espera...");
                    boolean intStatus = Machine.interrupt().disable();
                    lista.get(cont).thread.ready();
                    lista.remove(cont);
                    cont--;
                    size--;
                    //System.out.println("La cola de espera se ha actualizado: ");
                    //for (KthreadsAndTimes t: lista){
                    //    System.out.println("Thread " + t.thread.getName());
                    //}
                    Machine.interrupt().restore(intStatus);
                }
                cont++;
            }
        }
        KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
        long wakeTime = Machine.timer().getTime() + x;
        if (wakeTime > Machine.timer().getTime()){
            boolean intStatus = Machine.interrupt().disable();
            Lib.debug('w', "Thread " + KThread.currentThread() + 
                " ha sido agregado a la cola de espera... Estara bloqueado por " + x + " ticks.");
            lista.add(new KthreadsAndTimes(KThread.currentThread(), wakeTime));
            KThread.sleep();
            Machine.interrupt().restore(intStatus);
        }
    }
    private class KthreadsAndTimes{
        KThread thread;
        long time;
        public KthreadsAndTimes(KThread t, long l){
            thread = t;
            time = l;
        }
    }

    private static LinkedList<KthreadsAndTimes> lista = new LinkedList<KthreadsAndTimes>();
}