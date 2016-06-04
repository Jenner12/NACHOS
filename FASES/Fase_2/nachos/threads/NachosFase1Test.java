package nachos.threads;
import nachos.machine.*;
import nachos.threads.PriorityScheduler.ThreadState;

public class NachosFase1Test{
	private static class Join_And_WaitUntil_Test implements Runnable {
        private int num;
        private boolean dormir;
        private boolean shared;
        static int correlativo = 0;
        static KThread compartido;

        public Join_And_WaitUntil_Test(int n, boolean dormir, boolean shared){
            this.num = n;
            this.dormir = dormir;
            this.shared = shared;
            correlativo++;
            ///*
            if (correlativo==1 && this.shared){
                compartido = new KThread(new Join_And_WaitUntil_Test(100, dormir, this.shared)).setName("Shared");
                compartido.fork();
            }
            //*/
        }
        public void run() {
            ///*
            if (this.num == 6){
                KThread kt2 = new KThread(new Join_And_WaitUntil_Test(num-5, dormir, this.shared)).setName("KThread2");
                kt2.fork();
                compartido.join();
                kt2.join();
            }
            if (this.num == 1) {
                compartido.join();
                
            }
            
            
            System.out.println("Desplegando numeros (del " + (num) + " al " + (num+4) + ") cada 15000000 ticks");
            for (int x = num; x < num+5; x++){
                if (dormir)
                    KThread.waitUntilSeconds(1.3);
                //System.out.println("El tiempo al despertarlo fue: " + Machine.timer().getTime());
                System.out.println(x);
            }
            KThread.currentThread().yield();
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
            System.out.println("\n            *** Comenzando CommunicatorTest ***");
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
            KThread.currentThread().yield();
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
            KThread.currentThread().yield();
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
            KThread.currentThread().yield();
            //System.out.println(this.nombre + " Termino ejecucion");
        }
    }

    public static void testTask1AndTask3(){
    	///* //   PRUEBA DE JOIN WAITUNTIL
        KThread kt1 = new KThread(new Join_And_WaitUntil_Test(6, true, true)).setName("kt1");
        kt1.fork();
        kt1.join();
        //*/ //   FIN PRUEBA DE JOIN WAITUNTIL
    }

    public static void testTask2AndTask4(){
    	///* // PRUEBA LISTEN Y SPEAK (COMMUNICATOR Y CONDITION2)
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
        KThread kt1 = new KThread(new CommunicatorTest(3, 3, 3, 3)).setName("CommunicatorTest");
        kt1.fork();
        kt1.join();
        
        //*/ // FIN PRUEBA LISTEN Y SPEAK
    }

    public static void testTask5(){
    	///*  // PRUEBA DE PRIORITY SCHEDULER
        //((ThreadState) currentThread.schedulingState).setPriority(7);
        KThread kt1 = new KThread(new Join_And_WaitUntil_Test(30, false, false)).setName("kt1");
        KThread kt2 = new KThread(new Join_And_WaitUntil_Test(20, false, false)).setName("kt2");
        KThread kt3 = new KThread(new Join_And_WaitUntil_Test(25, false, false)).setName("kt3");
        kt1.fork();
        kt2.fork();
        kt3.fork();


        //for (int x = 50; x < 54; x++)
        //    System.out.println(x);
        
        ((ThreadState) kt1.schedulingState).setPriority(2);
        ((ThreadState) kt2.schedulingState).setPriority(6);
        ((ThreadState) kt3.schedulingState).setPriority(4);
        kt1.join();
        kt2.join();
        kt3.join();
        //*/  // FIN PRUEBA DE PRIORITY SCHEDULER
    }

    public static void testTask6(){
    	// PRUEBA DE CLASE BOAT
        Boat.selfTest(4, 3);
    }

    public static void testAllTasks(){
    	System.out.println("\n ---- TESTEANDO JOIN Y WAITUNTIL -----");
    	testTask1AndTask3();
    	System.out.println(" ---- TESTEANDO LISTEN,SPEAK Y CONDITION2 -----");
    	testTask2AndTask4();
    	System.out.println(" ---- TESTEANDO PRIORITY SCHEDULER -----");
    	testTask5();
    	System.out.println(" ---- TESTEANDO BOTES -----");
    	testTask6();
    }

    public static long waitingSecondsToWaitingTicks(double seconds){
        return (long) seconds*12000000;
    }

    public static void waitUntilSeconds(double seconds){
        ThreadedKernel.alarm.waitUntil(waitingSecondsToWaitingTicks(seconds));
    }

    public static class Maquina{
        static final int maximo = 10;
        private int cant;
        public Maquina(){
        this.lock = new Lock();
        this.cant = 0;
        this.ingresoCondition = new Condition2(this.lock);
        this.extraccionCondition = new Condition2(this.lock);
        this.extraccionInProgress = ingresoInProgress = false;
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
    public void ingresar(){
        lock.acquire();
        // Si aún no se ha llamado a ningún listener que reciba la transferencia, o bien,
        // si ya hay un speaker y un listener que estan llevando a cabo una transferencia,
        // entonces dormir el thread actual que se encuentra intentando transmitir...
        while (extraccionInProgress || ingresoInProgress || this.cant == maximo){
            ingresoCondition.sleep();
        }
        this.ingresoInProgress = true; // Indicar que la transferencia se está llevando a cabo
        this.cant++;        // Se ha llamado el método speak, incrementar speakerCalls
        ingresoCondition.wakeAll();             // Se despierta el thread listener que recibirá la transferencia
        extraccionCondition.wakeAll();
        this.ingresoInProgress = false; // Indicar que la transferencia se está llevando a cabo
        lock.release();
    }
    
    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return  the integer transfer 2red.                                                                                           
     */   
    public void extraer() {
        lock.acquire();

        // Si aún no se ha llamado a ningún listener que reciba la transferencia, o bien,
        // si ya hay un speaker y un listener que estan llevando a cabo una transferencia,
        // entonces dormir el thread actual que se encuentra intentando transmitir...
        while (extraccionInProgress || ingresoInProgress || this.cant == 0){
            extraccionCondition.sleep();
        }
        this.extraccionInProgress = true; // Indicar que la transferencia se está llevando a cabo
        this.cant--;        // Se ha llamado el método speak, incrementar speakerCalls
        ingresoCondition.wakeAll();             // Se despierta el thread listener que recibirá la transferencia
        extraccionCondition.wakeAll();
        this.extraccionInProgress = false; // Indicar que la transferencia se está llevando a cabo
        lock.release();
    }

    private static final char dbgCommunicator = 'C';

    private Lock lock;
    private Condition2 ingresoCondition, extraccionCondition;
    private boolean ingresoInProgress, extraccionInProgress;
    }

    public static class MaquinaTest implements Runnable{
        private Maquina com;
        private int cantSpeakers, cantListeners, cantLlamadas, secuencia;
        public MaquinaTest(int secuencia, int cantSpeakers, int cantListeners, int cantLlamadas){
            this.com = new Maquina();
            this.cantSpeakers = cantSpeakers;
            this.cantListeners = cantListeners;
            this.cantLlamadas = cantLlamadas;
            this.secuencia = secuencia;
        }
        public void run(){
            System.out.println("\n            *** Comenzando CommunicatorTest ***");
            System.out.println("\n-> Se utilizaran " + cantSpeakers + " threads como speakers, llamando cada uno al metodo"+
                                    " speak hasta " + cantLlamadas + " veces");
            System.out.println("-> Se utilizaran " + cantListeners + " threads como listeners, llamando cada uno al metodo"+
                                    " listen hasta " + cantLlamadas + " veces\n");
            KThread[] emisores = new KThread[cantSpeakers];
            KThread[] receptores = new KThread[cantListeners];
            int contS = 0, contL = 0;
            System.out.println("\n        *** CommunicatorTest Termino Ejecucion ***\n");
            KThread.currentThread().yield();
        }
    }
    public static class Proveedor implements Runnable{
        private Maquina com;
        private String nombre;
        private int llamadas;
        public Proveedor(Maquina c, int llamadas, String nombre){
            this.com = c;
            this.nombre = nombre;
            this.llamadas = llamadas;
        }
        public void run(){
            java.util.Random r = new java.util.Random();
            int msg = 0;
            for (int x = 0; x < this.llamadas; x++){
                //waitUntilSeconds(r.nextInt(2)+1);
                //System.out.println("# " + this.nombre + " esta por enviar el mensaje: " + msg);
                this.com.ingresar();
            }
            KThread.currentThread().yield();
            //System.out.println(this.nombre + " Termino ejecucion");
        }
    }
    public static class Consumidor implements Runnable{
        private Maquina com;
        private String nombre;
        private int llamadas;
        public Consumidor(Maquina c, int llamadas, String nombre){
            this.com = c;
            this.nombre = nombre;
            this.llamadas = llamadas;
        }
        public void run(){
            java.util.Random r = new java.util.Random();
            for (int x = 0; x < this.llamadas; x++){
                //waitUntilSeconds(r.nextInt(2)+1);
                this.com.extraer();
            }
            KThread.currentThread().yield();
            //System.out.println(this.nombre + " Termino ejecucion");
        }
    }
}