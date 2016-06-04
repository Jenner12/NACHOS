package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.util.LinkedList;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public VMProcess() {
        super();
        this.childrenVMProcesses = new LinkedList<>();

        if (kernel == null)
            kernel = (VMKernel) Kernel.kernel;
        //System.out.println("largo: " + pageTable.length);
        //for (int i = 0; i < pageTable.length; i++)
        //    pageTable[i].valid = false;
    }

    public static VMProcess newVMProcess() {
       return (VMProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    public void setParentProcess(VMProcess parent){this.parentProcess = parent;}
    public VMProcess getParentProcess(){return this.parentProcess;}
    //public LinkedList<VMProcess> getChildrenVMProcesses(){return this.childrenVMProcesses;}
    public UThread getUThread(){return this.thread;}
    public void parentGoToBed(VMProcess parent){
        if (parent.getPid() == this.getParentProcess().getPid())
            this.waitingParentCondition.sleep();
        else Lib.debug(dbgProcessSyscalls, "[VMProcess.parentGoToBed]: Error, este proceso no es el padre!");
    }

    public void acquireParentLock(VMProcess parent){
        if (parent.getPid() == this.getParentProcess().getPid())
            this.lockForParent.acquire();
        else Lib.debug(dbgProcessSyscalls, "[VMProcess.acquireParentLock]: Error, este proceso no es el padre!");
    }
    public void releaseParentLock(VMProcess parent){
        if (parent.getPid() == this.getParentProcess().getPid())
            this.lockForParent.release();
        else Lib.debug(dbgProcessSyscalls, "[VMProcess.acquireParentLock]: Error, este proceso no es el padre!");
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
	   super.saveState();
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    @Override
    public void restoreState() {
	   kernel.invalidateTLBEntries();
       updateThisUProcessPageTable();
    }

    @Override
    public boolean execute(String name, String[] args) {
        this.fileName = name;
        if (!super.load(name, args))
            return false;
        this.thread = new UThread(this);
        this.thread.setName(name);
        this.thread.fork();
        flagsLock.acquire();
        currentlyRunningProcesses++;
        flagsLock.release();
        VMKernel.procesos.add(this);
        return true;
    }

    /**
     * Handle the exec() system call. 
     */

    @Override
    protected int handleExec(int file, int argc, int argv) {
        // Se verifica si argc es no negativo
        if (argc < 0){
            Lib.debug(dbgProcessSyscalls, "[VMProcess.handleExec]: argc no debe ser negativo!");
            return -1;
        }
        // Se verifica si los punteros son válidos...
        if (!(verifyVirtualAddress(file) && verifyVirtualAddress(argv))){
            Lib.debug(dbgProcessSyscalls, "[VMProcess.handleExec]: Ambas direcciones (file y argv) deben ser validas!");
            return -1;
        }

        String fileName = readVirtualMemoryString(file, maximumFileNameLength);
        // Se verifica si los punteros son válidos...
        if (fileName == null){
            Lib.debug(dbgProcessSyscalls, "[VMProcess.handleExec]: No se encontro el nombre del archivo!");
            return -1;
        }
        
        // Se verifica que el nombre del archivo termine en ".coff"
        if (fileName.length()<6 || !(fileName.substring(fileName.length()-5, fileName.length()).equals(".coff"))) {
            Lib.debug(dbgProcessSyscalls, "[VMProcess.handleExec]: El formato del archivo debe ser '.coff'!");
            return -1;
        }

        String childArgs[] = new String[argc];
        // Se obtiene los argumentos, que se encuentran en memoria...
        byte args[] = new byte[argc * 4];
        if(argc * 4 != readVirtualMemory(argv, args))
            return -1;

        for(int x = 0; x < argc; x++){
            int argAddress = Lib.bytesToInt(args, x*4);
            if(!super.verifyVirtualAddress(argAddress)){
                Lib.debug(dbgProcessSyscalls, "[VMProcess.handleExec]: La direccion del argumento #"+(x+1)+" no es valida!");
                return -1;
            }
            childArgs[x] = readVirtualMemoryString(argAddress, maximumFileNameLength);
        }

        // Se crea al child process
        VMProcess childProcess = new VMProcess();
        // Se le indica que el proceso actual es su padre, y es agregado
        // a la lista de hijos del este ultimo...
        childProcess.setParentProcess(this);
        this.childrenVMProcesses.add(childProcess);
        
        // Finalmente, se manda a ejecutar el nuevo proceso y se retornar su pid.
        childProcess.execute(fileName, childArgs);
        return childProcess.getPid();
    }

    @Override
    protected int handleJoin(int processID, int status) {
        if (processID == -1){
            Lib.debug(dbgProcessSyscalls, "[VMProcess.handleJoin]: Proceso " + this + " no puede hacer join, ya que el pid del proceso es -1 (no existe)!");
            return -1;
        }

        VMProcess joining = VMKernel.getProcessById(processID);
        if (joining != null){
            Lib.debug(dbgProcessSyscalls, "[VMProcess.handleJoin]: Proceso "+this+" esta haciendo join al proceso " + joining);
            if (joining.doneExecuting()){
                Lib.debug(dbgProcessSyscalls, "[VMProcess.handleJoin]: Proceso "+this+" no puede hacer join, ya que el proceso " + joining + " ya ha terminado ejecucion!");
                return -1;
            }
        }
        else {
            Lib.debug(dbgProcessSyscalls, "[VMProcess.handleJoin]: Proceso "+this+" no pudo hacer join, ya que no existe algun proceso con pid = " + processID);
            return -1;
        }
    
        // verificar si el processID corresponde a un hijo...
        boolean myChild = false;
        int index = 0;
        for (VMProcess child : this.childrenVMProcesses){
            if (child.getPid() == processID){
                myChild = true;
                break;
            }
            index++;
        }
        if (!myChild){
            if (joining != null)
                Lib.debug(dbgProcessSyscalls, "[VMProcess.handleJoin]: Proceso " + this + " no pudo hacer join al proceso " + joining + " ya que este no es hijo suyo!");
            else Lib.debug(dbgProcessSyscalls, "[VMProcess.handleJoin]: No se pudo hacer join, el proceso con pid = " + processID + " debe ser un child process del proceso actual!");
            return -1;
        }
        VMProcess childProcessToJoin = this.childrenVMProcesses.get(index);
        // Mientras el status del hijo no sea "finished", dormir al proceso padre (el actual)
        childProcessToJoin.acquireParentLock(this);
        if (childProcessToJoin.getUThread() != null){
            while (!childProcessToJoin.doneExecuting())
                childProcessToJoin.parentGoToBed(this);
            childProcessToJoin.releaseParentLock(this);
        }
        
        // Si se llegó a este punto, quiere decir que el hijo ha terminado ejecución 
        // y el padre puede continuar ejecución...
        this.childrenVMProcesses.remove(index);
        Integer childExitStatus = childProcessToJoin.getExitStatus();

        if (childExitStatus != null){
            writeVirtualMemory(status, Lib.bytesFromInt(childExitStatus));
            return 1;
        }
        // Si el exitStatus era nulo, quiere decir que exit para el proceso hijo fue
        // llamado luego de ocurrir una unhandled exception... por lo tanto retornar 0
        return 0;
    }


    protected void updateThisUProcessPageTable(){
        for(TranslationEntry trans : pageTable){
            TranslationEntry iptTrans = 
            kernel.invertedPageTable.getTranslation(super.getPid(), trans.vpn);
            if(iptTrans == null || iptTrans.valid == false){
                trans.valid = false;
            }
            else if(iptTrans != null){
                iptTrans.valid = true;
            }
        }
    }

    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     *
     * @return	<tt>true</tt> if successful.
     */
    protected boolean loadSections() {
        //return true;
        boolean res = super.loadSections();
        if (!res){
            Lib.debug(dbgVM, "[VMProcess.loadSections]: Hubo un problema al cargar"+
                                " las secciones del proceso " + this);
        }
        else{
            kernel.addPageTableEntriesToIPT(this.getPid(), this.pageTable);
        }
        return res;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
	   super.unloadSections();
    }    

    public void handleTLBMiss(){
        Lib.debug(dbgCaching, "[VMProcess.handleTLBMiss]: Ocurrio un TLBMiss por el proceso " + this);
        Processor p = Machine.processor();
        Lib.debug(dbgCaching, "[VMProcess.handleTLBMiss][BEGIN]: PC = " + p.readRegister(p.regPC)+
                                " , nextPC = " + p.readRegister(p.regNextPC));
        int virtualAdd = Machine.processor().readRegister(Processor.regBadVAddr);
        int vpn = Processor.pageFromAddress(virtualAdd);
        TranslationEntry trans = kernel.invertedPageTable.getTranslation(this.getPid(), vpn);
        if (trans == null || !trans.valid){
            handlePageFault(vpn);
            //return;
        }
        kernel.addTranslationLRUTLB(trans);
        //p.writeRegister(p.regNextPC, p.readRegister(p.regPC));
        //p.advancePC();
        //Lib.debug(dbgCaching, "[VMProcess.handleTLBMiss][END]: PC = " + p.readRegister(p.regPC)+
        //                        " , nextPC = " + p.readRegister(p.regNextPC));
    }

    public void handlePageFault(int vpn){
        Lib.debug(dbgCaching, "[VMProcess.handlePageFault]: Ocurrio un PageFault en el proceso" + this);
        int pid = this.getPid();
        if (kernel.swapFile.pageInSwap(pid, vpn)){
            kernel.processPageFault(pid, vpn);
            return;
        }
    }

    /**
     * Handle a user exception. Called by
     * <tt>VMKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
    	Processor processor = Machine.processor();

    	switch (cause) {
            case Processor.exceptionTLBMiss:
                handleTLBMiss();
            break;
            default:
    	        super.handleException(cause);
    	    break;
    	}
    }

    public LinkedList<VMProcess> childrenVMProcesses;
    public VMProcess parentProcess;
	
    public static VMKernel kernel = null;

    protected static final int pageSize = Processor.pageSize;
    protected static final char dbgCaching = 'c';
    protected static final char dbgProcess = 'a';
    protected static final char dbgVM = 'v';
}
