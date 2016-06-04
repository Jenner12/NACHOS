package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.util.LinkedList;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
    /**
     * Allocate a new VM kernel.
     */
    public VMKernel() {
	   super();
       pageSize = Processor.pageSize;
    }

    /**
     * Initialize this kernel.
     */
    public void initialize(String[] args){
        super.initialize(args);
        // Inicializar invertedPageTable, TLB (que utiliza LRU) y swapfile
        invertedPageTable = new InvertedPageTable();
        initLRUTLB();
        this.swapFile = new SwapFile();
    }

    /**
     * Test this kernel.
     */	
    public void selfTest() {
	   super.selfTest();
    }

    /**
     * Start running user programs.
     */
    public void run() {
	   super.correr();
        VMProcess process = VMProcess.newVMProcess();
        
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
        swapFile.terminate();
        super.terminate();
    }

    public static VMProcess getProcessById(int id){
        for (VMProcess p : procesos){
            if (p.getPid() == id)
                return p;
        }
        return null;
    }


    private void initLRUTLB(){
        lruTLB = new TranslationEntry[tlbSize];
        for (int i = 0; i < tlbSize; i++){
            lruTLB[i] = Machine.processor().readTLBEntry(i);
        }
    }

    private void updateProcessorTLB(){
        for (int i = 0; i < tlbSize; i++)
            Machine.processor().writeTLBEntry(i, lruTLB[i]);
    }

    private int lruTLBContains(TranslationEntry e){
        for (int i = 0; i < tlbSize; i++)
            if (lruTLB[i].ppn == e.ppn && lruTLB[i].vpn == e.vpn) return i;
        return -1;
    }

    public void addTranslationLRUTLB(TranslationEntry nueva){

        int pos = lruTLBContains(nueva);
        if (pos == -1)
            pos++;
        for (int i = pos; i < tlbSize-1; i++){
            lruTLB[i] = new TranslationEntry(lruTLB[i+1]);
        }
        lruTLB[tlbSize-1] = new TranslationEntry(nueva);
        updateProcessorTLB();
        printTLB();
    }

    public void printTLB(){
        Lib.debug('q', "[VMKernel:printTLB]: BEGIN...");
        for (int i = 0; i < tlbSize; i++)
            Lib.debug('q', lruTLB[i].toString());
        Lib.debug('q', "[VMKernel:printTLB]: ENDED!");
    }


    public void invalidateTLBEntries(){
        Lib.debug(dbgVM, "[VMKernel:invalidateTLBEntries]: Invalidando traducciones en TLB!");
        for(int i=0; i < tlbSize; i++)
            lruTLB[i].valid = false;
        updateProcessorTLB();
    }

    public void addPageTableEntriesToIPT(int pid, TranslationEntry[] pageTable){
        for (TranslationEntry t : pageTable){
            this.invertedPageTable.addTranslation(pid, t.vpn, t);
        }
    }

    public void processPageFault(int pid, int vpn){
        int position = swapFile.getPagePosition(pid, vpn);
        byte buffer[] = new byte[pageSize];
        swapFile.readPage(position, buffer, 0, buffer.length);
        //System.arraycopy(buffer, 0, Machine.processor().getMemory(), 
        //    freePage.ppn * pageSize, buffer.length);
        //pageTable[vpn] = invertedPageTable.getTranslation(pid, vpn);
    }


    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;
    private static final char dbgVM = 'v';

    public static LinkedList<VMProcess> procesos = new LinkedList<>();
    
    public static final int tlbSize = Machine.processor().getTLBSize();
    public static int pageSize;
    public static InvertedPageTable invertedPageTable;
    public static TranslationEntry[] lruTLB; 
    public SwapFile swapFile;
}
