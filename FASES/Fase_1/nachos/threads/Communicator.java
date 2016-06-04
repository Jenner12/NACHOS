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
    /**
     * Allocate a new communicator.
     */
    public Communicator(){
        this.lock = new Lock();
        this.listenCondition = new Condition2(this.lock);
        this.speakCondition = new Condition2(this.lock);
        this.speakerCalls = this.listenerCalls = 0;
        this.transferenceInProgress = false;
        this.message = 0;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param   word    the integer to transfer.
     */
    public void speak(int word){
        lock.acquire();
        Lib.debug('c', "*Transmitiendo '" + word + "'");
        this.speakerCalls++;        // Se ha llamado el método speak, incrementar speakerCalls

        // Si aún no se ha llamado a ningún listener que reciba la transferencia, o bien,
        // si ya hay un speaker y un listener que estan llevando a cabo una transferencia,
        // entonces dormir el thread actual que se encuentra intentando transmitir...
        while (transferenceInProgress || (this.listenerCalls == 0)){
            if (this.listenerCalls == 0)
                Lib.debug('c', "Durmiendo thread " + KThread.currentThread() + " esperando a que un listener sea llamado...");
            if (transferenceInProgress)
            Lib.debug('c', "Durmiendo thread " + KThread.currentThread() + " esperando a que se termine la transferencia actual...");

            speakCondition.sleep(); // A partir de este punto se detiene la ejecución, hasta que
                                    // un thread listener despierte a todos los speakers en cola
                                    // (de todos estos threads speakers, el primero que el scheduler
                                    // coloque en estado Running será el que haga true la variable
                                    // 'transerenceInProgress', por lo que cuando se ejecuten los 
                                    // siguientes threads recien despertados y se evalue la condicion
                                    // del ciclo while, volverán ha quedar bloqueados (dormidos).
                                    
        }
        this.transferenceInProgress = true; // Indicar que la transferencia se está llevando a cabo
        this.message = word;                // Se envía la palabra, comenzando así la transferencia
        listenCondition.wake();             // Se despierta el thread listener que recibirá la transferencia
        this.speakerCalls--;                // Metodo speak está por terminar, decrementar speakerCalls
        lock.release();                                                                                                                                                                                                                                                        
    }
    
    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return  the integer transfer 2red.                                                                                           
     */   
    public int listen() {
        lock.acquire();
        Lib.debug('c', "*Escuchando...");
        this.listenerCalls++;           // Se ha llamado el métodos listen, incrementar listenerCalls

        if (this.speakerCalls != 0)
            Lib.debug('c', "Listener ha sido llamado, despertando siguiente speaker en cola de espera...");

        speakCondition.wakeAll();       // Despertando a todos los speakers para que alguno (decisión del scheduler)
                                        // comience la transferencia para este thread listener...
        
        // Este thread listener deberá esperar hasta que el 
        // thread speaker correspondiente haya comenzado la transferencia
        while (!transferenceInProgress){
            Lib.debug('c', "Durmiendo thread " + KThread.currentThread() + " esperando a que un speaker haga la transferencia");

            listenCondition.sleep();        // Durmiendo al thread listener...
        }

        int res = this.message;             // Se guarda el mensaje que el thread speaker transmitió
        this.transferenceInProgress = false;// Indicar que la transferencia ya no se encuentra en progreso (ya terminó)
        this.listenerCalls--;               // Métodos listen está por terminar, decrementar listenerCalls
        this.speakCondition.wakeAll();      // Despertar a todos los threads speakers que puedan estar dormidos
                                            // para que puedan continuar transmitiendo
        lock.release();
        return res;                         // Retornar el mensaje guardado anteriormente
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
    private Condition2 listenCondition, speakCondition;
    private boolean transferenceInProgress;
    private byte speakerCalls, listenerCalls;
    private int message;
}
