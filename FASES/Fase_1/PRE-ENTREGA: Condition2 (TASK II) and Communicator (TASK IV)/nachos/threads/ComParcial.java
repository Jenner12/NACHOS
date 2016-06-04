package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    
    private boolean inboxFull = false;
    private int MESG;

    /**
     * Allocate a new communicator.
     */
    public Communicator(){
        this.lock = new Lock();
        this.atomicityCondition = new Condition2(this.lock);
        this.speakerCalls = this.listenerCalls = 0;
        this.message = 0;
    }

    int cantSpeaked = 0, cantListened = 0;
    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word)
    {
        lock.acquire();
        Lib.debug('c', "*Transmitiendo '" + word + "'");
        ++speakerCalls;
        while((listenerCalls == 0) || inboxFull)
            atomicityCondition.sleep();        
        inboxFull = true;   // Going to be placing a Messgae
        MESG = word;
        atomicityCondition.wake();  //Wake any(the) sleeping listners
        --speakerCalls;
        this.lock.release();
    }
    
    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transfer 2red.                                                                                           
     */   
    public int listen()
    {
        lock.acquire();
        Lib.debug('c', "*Escuchando...");
        atomicityCondition.wakeAll();
        ++listenerCalls;
        while(!inboxFull) 
            atomicityCondition.sleep();      // No message has been spoken, listners wait.
        --listenerCalls;
        inboxFull = false;
        int word = MESG;        //Recieve the message and update the inbox
        atomicityCondition.wakeAll();    //Remind any other speakers to check if there are additional listeners
        lock.release();
        return word;
    }

    // IMPLEMENTACION NUESTRA
    /**
     * Gets the last word that has been transfered in this Communicator.
     *
     * @return  the last integer transfered.
     */
    public int getMessage(){
        return this.message;
    }

    private Lock lock;
    private Condition2 atomicityCondition;
    private byte speakerCalls, listenerCalls;
    private int message;
}
