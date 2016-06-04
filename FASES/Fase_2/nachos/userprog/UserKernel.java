package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.util.LinkedList;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
	   super();
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
    	super.initialize(args);
    	console = new SynchConsole(Machine.console());
    	freePhysicalPages = new LinkedList<>();
        fppLock = new Lock();
    	for (int i = 0; i < Machine.processor().getNumPhysPages(); i++)
            freePhysicalPages.add(new TranslationEntry(0, i, false, false, false, false));
        
        Machine.processor().setExceptionHandler(new Runnable() {
            public void run() { exceptionHandler(); }
            });
    }

    /**
     * Test the console device.
     */	
    public void selfTest() {
	   super.selfTest();
    /*
    	System.out.println("Testing the console device. Typed characters");
    	System.out.println("will be echoed until q is typed.");

    	char c;

    	do {
    	    c = (char) console.readByte(true);
    	    console.writeByte(c);
    	}
    	while (c != 'q');

    	System.out.println("");
    */
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
	if (!(KThread.currentThread() instanceof UThread))
	    return null;
	
	return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
	Lib.assertTrue(KThread.currentThread() instanceof UThread);

	UserProcess process = ((UThread) KThread.currentThread()).process;
	int cause = Machine.processor().readRegister(Processor.regCause);
	process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
    	super.run();

    	UserProcess process = UserProcess.newUserProcess();
    	
    	String shellProgram = Machine.getShellProgramName();
        String[] shellArgs = Machine.getShellArguments();

    	//Lib.assertTrue(process.execute(shellProgram, new String[] { }));
        Lib.assertTrue(process.execute(shellProgram, shellArgs));

    	KThread.currentThread().finish();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }

    public TranslationEntry[] fillPhysicalPages(int cantPages){
        
        TranslationEntry[] filledPages = null;
        fppLock.acquire();
        if (!freePhysicalPages.isEmpty() && cantPages <= freePhysicalPages.size()) {
            filledPages = new TranslationEntry[cantPages]; 
            for (int x = 0; x < cantPages; x++) {
                filledPages[x] = freePhysicalPages.remove(); 
                filledPages[x].valid = true;
            }
        }
        fppLock.release();
        return filledPages;
    } 

    // libera algunas paginas que han sido reservadas por algun proceso anteriormente
    public void freeFilledPhysicalPages(TranslationEntry[] pages){
        fppLock.acquire();
        for (int x = 0; x < pages.length ; x++){
            pages[x].valid = false;
            freePhysicalPages.add(pages[x]);
        }
        fppLock.release();
    }

    public static UserProcess getProcessById(int id){
        for (UserProcess p : procesos){
            if (p.getPid() == id)
                return p;
        }
        return null;
    }

    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;

    private static LinkedList<TranslationEntry> freePhysicalPages;
    private static Lock fppLock;

    public static LinkedList<UserProcess> procesos = new LinkedList<>();
}
