package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.LinkedList;
import java.util.HashMap;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {

        this.openFilesTable = new OpenFile[maximumOpenFiles];
        this.openFilesTable[0] = UserKernel.console.openForReading();
        this.openFilesTable[1] = UserKernel.console.openForWriting();

        this.childrenProcesses = new LinkedList<>();
        this.parentProcess = null;
        
        flagsLock.acquire();
        this.pid = ++createdProcesses;
        flagsLock.release();
        this.fileName = null;
        this.exitStatus = null;
        this.thread = null;
        this.lockForParent = new Lock();
        this.waitingParentCondition = new Condition2(lockForParent);

        // La pageTable se llena ahora en método loadSections();
            //for (int i=0; i<numPhysPages; i++)
            //    pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
            //pageTable = ((UserKernel) Kernel.kernel).fillPhysicalPages(numPhysPages);
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return  a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
       return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param   name    the name of the file containing the executable.
     * @param   args    the arguments to pass to the executable.
     * @return  <tt>true</tt> if the program was successfully executed.
     */
    protected boolean execute(String name, String[] args) {
        this.fileName = name;
        if (!load(name, args))
            return false;
        this.thread = new UThread(this);
        this.thread.setName(name);
        this.thread.fork();
        flagsLock.acquire();
        currentlyRunningProcesses++;
        flagsLock.release();
        UserKernel.procesos.add(this);
        return true;
    }

    public boolean doneExecuting(){
        return this.thread.getStatus() == 4;
    }
    public int getPid(){return this.pid;}
    public Integer getExitStatus(){return this.exitStatus;}
    public void setParentProcess(UserProcess parent){this.parentProcess = parent;}
    public UserProcess getParentProcess(){return this.parentProcess;}
    //public LinkedList<UserProcess> getChildrenProcesses(){return this.childrenProcesses;}
    public UThread getUThread(){return this.thread;}
    public void parentGoToBed(UserProcess parent){
        if (parent.getPid() == this.getParentProcess().getPid())
            this.waitingParentCondition.sleep();
        else Lib.debug(dbgProcessSyscalls, "[UserProcess.parentGoToBed]: Error, este proceso no es el padre!");
    }

    public void acquireParentLock(UserProcess parent){
        if (parent.getPid() == this.getParentProcess().getPid())
            this.lockForParent.acquire();
        else Lib.debug(dbgProcessSyscalls, "[UserProcess.acquireParentLock]: Error, este proceso no es el padre!");
    }
    public void releaseParentLock(UserProcess parent){
        if (parent.getPid() == this.getParentProcess().getPid())
            this.lockForParent.release();
        else Lib.debug(dbgProcessSyscalls, "[UserProcess.acquireParentLock]: Error, este proceso no es el padre!");
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
        Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param   vaddr   the starting virtual address of the null-terminated
     *          string.
     * @param   maxLength   the maximum number of characters in the string,
     *              not including the null terminator.
     * @return  the string read, or <tt>null</tt> if no null terminator was
     *      found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
        Lib.assertTrue(maxLength >= 0);

        byte[] bytes = new byte[maxLength+1];

        int bytesRead = readVirtualMemory(vaddr, bytes);

        for (int length=0; length<bytesRead; length++) {
            if (bytes[length] == 0)
            return new String(bytes, 0, length);
        }

        return null;
    }


    protected int processMemoryAccessData(byte[] data, int virtualAddress, int offset, int fullAccessSize, boolean reading){
        int vpn = Processor.pageFromAddress(virtualAddress); // Se obtien la vpn de la dirección virtual
        int physAddOffset = Processor.offsetFromAddress(virtualAddress);
        TranslationEntry translation = pageTable[vpn];
        int memoryStartIndex = Processor.makeAddress(translation.ppn, physAddOffset);
        int dataStartIndex = offset;

        int availableSpace = Processor.pageSize - physAddOffset; // Lo que queda disponible en la pagina
        int accessSize = (fullAccessSize > availableSpace) ? availableSpace : fullAccessSize;
        boolean recursion = (accessSize == fullAccessSize) ? false : true;
        if (!translation.valid){
            Lib.debug(dbgProcessSyscalls, "[processMemoryAccessData]: El TranslationEntry no fue valida");
            return 0;
        }
        else if (reading)
            System.arraycopy(Machine.processor().getMemory(), memoryStartIndex, data, dataStartIndex, accessSize);
        else{
            System.arraycopy(data, dataStartIndex, Machine.processor().getMemory(), memoryStartIndex, accessSize);
            translation.dirty = true;
        }
        translation.used = true;

        return accessSize + ((recursion) ? processMemoryAccessData(data, virtualAddress+accessSize, 
                                            offset + accessSize, fullAccessSize - accessSize, reading) 
                                            : 0);
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param   vaddr   the first byte of virtual memory to read.
     * @param   data    the array where the data will be stored.
     * @return  the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
       return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param   vaddr   the first byte of virtual memory to read.
     * @param   data    the array where the data will be stored.
     * @param   offset  the first byte to write in the array.
     * @param   length  the number of bytes to transfer from virtual memory to
     *          the array.
     * @return  the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
        
        Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
        if (!verifyVirtualAddress(vaddr)){
            Lib.debug(dbgProcessSyscalls, "[readVirtualMemory]: La direccion virtual no es valida!");
            return 0;
        }
        //    NACHOS ORIGINAL /////////////
        /*
        byte[] memory = Machine.processor().getMemory();
        
        // for now, just assume that virtual addresses equal physical addresses
        if (vaddr < 0 || vaddr >= memory.length)
            return 0;

        int amount = Math.min(length, memory.length-vaddr);
        System.arraycopy(memory, vaddr, data, offset, amount);
        return amount;
        */ ////////////////////////////////
        
        /* Pruebas...
         byte[] memory = Machine.processor().getMemory();
        // for now, just assume that virtual addresses equal physical addresses
        if (vaddr < 0 || vaddr >= memory.length)
            return 0;
        int vpn = Processor.pageFromAddress(vaddr); // Se obtien la vpn de la dirección virtual
        int physAddOffset = Processor.offsetFromAddress(vaddr);
        int physicalAddress = Processor.makeAddress(vpn, physAddOffset);
        int amount = Math.min(length, memory.length - physicalAddress);
        System.arraycopy(memory, physicalAddress, data, physicalAddress, amount);
        return amount;
        */
        accesingMemoryLock.acquire();
            int readData = processMemoryAccessData(data, vaddr, offset, length, true);
        accesingMemoryLock.release();
        return readData;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param   vaddr   the first byte of virtual memory to write.
     * @param   data    the array containing the data to transfer.
     * @return  the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
        return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param   vaddr   the first byte of virtual memory to write.
     * @param   data    the array containing the data to transfer.
     * @param   offset  the first byte to transfer from the array.
     * @param   length  the number of bytes to transfer from the array to
     *          virtual memory.
     * @return  the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
        if (!verifyVirtualAddress(vaddr)){
            Lib.debug(dbgProcessSyscalls, "[writeVirtualMemory]: La direccion virtual no es valida!");
            return 0;
        }
        /*
        byte[] memory = Machine.processor().getMemory();
        // for now, just assume that virtual addresses equal physical addresses
        if (vaddr < 0 || vaddr >= memory.length)
            return 0;

        int amount = Math.min(length, memory.length-vaddr);
        System.arraycopy(data, offset, memory, vaddr, amount);
        //return amount;
        */

        /* pruebas
         byte[] memory = Machine.processor().getMemory();
        // for now, just assume that virtual addresses equal physical addresses
        if (vaddr < 0 || vaddr >= memory.length)
            return 0;
        int vpn = Processor.pageFromAddress(vaddr); // Se obtien la vpn de la dirección virtual
        int physAddOffset = Processor.offsetFromAddress(vaddr);
        int physicalAddress = Processor.makeAddress(vpn, physAddOffset);
        int amount = Math.min(length, memory.length - physicalAddress);
        System.arraycopy(data, offset, memory, physicalAddress, amount);
        return amount;
        */
        accesingMemoryLock.acquire();
            int writenData = processMemoryAccessData(data, vaddr, offset, length, false);
        accesingMemoryLock.release();
        return writenData;
        
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param   name    the name of the file containing the executable.
     * @param   args    the arguments to pass to the executable.
     * @return  <tt>true</tt> if the executable was successfully loaded.
     */
    public boolean load(String name, String[] args) {
        Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
        
        if (executable == null) {
            Lib.debug(dbgProcess, "\topen failed");
            return false;
        }

        try {
            coff = new Coff(executable);
        }
        catch (EOFException e) {
            executable.close();
            Lib.debug(dbgProcess, "\tcoff load failed");
            return false;
        }

        // make sure the sections are contiguous and start at page 0
        numPages = 0;
        for (int s=0; s<coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() != numPages) {
                coff.close();
                Lib.debug(dbgProcess, "\tfragmented executable");
                return false;
            }
            numPages += section.getLength();
        }

        // make sure the argv array will fit in one page
        byte[][] argv = new byte[args.length][];
        int argsSize = 0;
        for (int i=0; i<args.length; i++) {
            argv[i] = args[i].getBytes();
            // 4 bytes for argv[] pointer; then string plus one for null byte
            argsSize += 4 + argv[i].length + 1;
        }
        if (argsSize > pageSize) {
            coff.close();
            Lib.debug(dbgProcess, "\targuments too long");
            return false;
        }

        // program counter initially points at the program entry point
        initialPC = coff.getEntryPoint();   

        // next comes the stack; stack pointer initially points to top of it
        numPages += stackPages;
        initialSP = numPages*pageSize;

        // and finally reserve 1 page for arguments
        numPages++;

        // Asignacion de la pageTable:
        pageTable = ((UserKernel) Kernel.kernel).fillPhysicalPages(this, numPages);
        for (int i = 0; i < pageTable.length; i++){
                pageTable[i].vpn = i;
                Lib.debug(dbgLoading, "pageTable["+i+"].vpn = "+i);
        }

        if (!loadSections())
            return false;

        // store arguments in last page
        int entryOffset = (numPages-1)*pageSize;
        int stringOffset = entryOffset + args.length*4;

        this.argc = args.length;
        this.argv = entryOffset;
        
        for (int i=0; i<argv.length; i++) {
            byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
            Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
            entryOffset += 4;
            Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
                   argv[i].length);
            stringOffset += argv[i].length;
            Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
            stringOffset += 1;
        }

        return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return  <tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
    if (numPages > Machine.processor().getNumPhysPages()) {
        coff.close();
        Lib.debug(dbgProcess, "\tinsufficient physical memory");
        return false;
    }

    // load sections
    for (int s=0; s<coff.getNumSections(); s++) {
        CoffSection section = coff.getSection(s);
        boolean translationReadOnly = section.isReadOnly();
        Lib.debug(dbgProcess, "\tinitializing " + section.getName()
              + " section (" + section.getLength() + " pages)");

        for (int i=0; i<section.getLength(); i++) {
            int vpn = section.getFirstVPN()+i;
            // for now, just assume virtual addresses=physical addresses
            // modificado! :)
            //TODO:
            pageTable[vpn].readOnly = translationReadOnly;
            Lib.debug(dbgLoading,"pageTable["+vpn+"].ppn = "+
                pageTable[vpn].ppn + ", vpn = "+pageTable[vpn].vpn);
            section.loadPage(i, pageTable[vpn].ppn);
        }
    }
    
    return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        ((UserKernel) Kernel.kernel).freeFilledPhysicalPages(this, pageTable);
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
        Processor processor = Machine.processor();

        // by default, everything's 0
        for (int i=0; i<processor.numUserRegisters; i++)
            processor.writeRegister(i, 0);

        // initialize PC and SP according
        processor.writeRegister(Processor.regPC, initialPC);
        processor.writeRegister(Processor.regSP, initialSP);

        // initialize the first two argument registers to argc and argv
        processor.writeRegister(Processor.regA0, argc);
        processor.writeRegister(Processor.regA1, argv);
    }

    protected static class RunningFile{

        protected int cantRunning;
        protected boolean toDelete;

        public RunningFile(){
            this.cantRunning = 0;
            this.toDelete = false;
        }

        void increment(){ cantRunning++;  }

        boolean decrement(){
            if(cantRunning <= 0)
                return false;
            cantRunning--;
            return true;
        }

        void unlinkFile(){ toDelete = true;  }

        int getCantRunning(){ return cantRunning;  }

        boolean getUnlinkState(){ return toDelete; }
    }

    protected int getNextAvailableFileDescriptor(){
        for(int i = 0; i < maximumOpenFiles; i++){
            if(openFilesTable[i] == null)
                return i;
        }
        return -1;
    }

    protected boolean verifyFileDescriptor(int fd){
        if(fd < 0 || fd >= maximumOpenFiles 
        || openFilesTable[fd] == null)
            return false;
        return true;
    }


    protected boolean verifyVirtualAddress(int virtualAddress){
        int vpn = Processor.pageFromAddress(virtualAddress);
        return (vpn >= 0 && vpn < numPages) ? true : false;
    }

    /**
     * Handle the halt() system call. 
     */
    protected int handleHalt() {
        if(this.pid != ROOT_PID){
            Lib.debug(dbgProcessSyscalls, "[UserProcess.handleHalt]: El proceso con pid="+this.pid +
                                            " no puede llamar directamente el syscall 'halt()' " 
                                            + ", esto solo lo puede hacer el proceso Root!");
            return -1;
        }
        
        Machine.halt();
        Lib.assertNotReached("Machine.halt() did not halt machine!");
        return 0;
    }

    /**
     * Handle the exit() system call. 
     */
    protected int handleExit(int status) {
        this.lockForParent.acquire();
        Lib.debug(dbgProcessSyscalls, "[UserProcess.handleExit]: Proceso " + this + " llamo al syscal 'exit()' con status = " + status);

        // Dejar huerfanos a todos los child processes
        for (int x = 0; x < this.childrenProcesses.size(); x++)
            this.childrenProcesses.get(x).setParentProcess(null);
        this.childrenProcesses = null;

        // Cerrar todos los file descriptors asociados a este proceso
        for(int x = 0; x < openFilesTable.length; x++){
            if(verifyFileDescriptor(x))
                handleClose(x);
        }

        unloadSections();
        currentlyRunningProcesses--;
        // Si este era el ultimo proceso en ejecución, terminar nachos!
        if (currentlyRunningProcesses < 1)
            Kernel.kernel.terminate();
        // de lo contrario, terminar unicamente este proceso...
        
        if (this.exitStatus != null) 
            this.exitStatus = null;
        else this.exitStatus = new Integer(0);

        this.waitingParentCondition.wakeAll();
        this.lockForParent.release();
        KThread.finish();
        Lib.assertNotReached();
        // inalcanzable...
        return 0xFEA; // lol :P
    }

   /**
     * Handle the exec() system call. 
     */
    protected int handleExec(int file, int argc, int argv) {
        // Se verifica si argc es no negativo
        if (argc < 0){
            Lib.debug(dbgProcessSyscalls, "[UserProcess.handleExec]: argc no debe ser negativo!");
            return -1;
        }
        // Se verifica si los punteros son válidos...
        if (!(verifyVirtualAddress(file) && verifyVirtualAddress(argv))){
            Lib.debug(dbgProcessSyscalls, "[UserProcess.handleExec]: Ambas direcciones (file y argv) deben ser validas!");
            return -1;
        }

        String fileName = readVirtualMemoryString(file, maximumFileNameLength);
        // Se verifica si los punteros son válidos...
        if (fileName == null){
            Lib.debug(dbgProcessSyscalls, "[UserProcess.handleExec]: No se encontro el nombre del archivo!");
            return -1;
        }
        
        // Se verifica que el nombre del archivo termine en ".coff"
        if (fileName.length()<6 || !(fileName.substring(fileName.length()-5, fileName.length()).equals(".coff"))) {
            Lib.debug(dbgProcessSyscalls, "[UserProcess.handleExec]: El formato del archivo debe ser '.coff'!");
            return -1;
        }

        String childArgs[] = new String[argc];
        // Se obtiene los argumentos, que se encuentran en memoria...
        byte args[] = new byte[argc * 4];
        if(argc * 4 != readVirtualMemory(argv, args))
            return -1;

        for(int x = 0; x < argc; x++){
            int argAddress = Lib.bytesToInt(args, x*4);
            if(!verifyVirtualAddress(argAddress)){
                Lib.debug(dbgProcessSyscalls, "[UserProcess.handleExec]: La direccion del argumento #"+(x+1)+" no es valida!");
                return -1;
            }
            childArgs[x] = readVirtualMemoryString(argAddress, maximumFileNameLength);
        }

        // Se crea al child process
        UserProcess childProcess = new UserProcess();
        // Se le indica que el proceso actual es su padre, y es agregado
        // a la lista de hijos del este ultimo...
        childProcess.setParentProcess(this);
        this.childrenProcesses.add(childProcess);
        
        // Finalmente, se manda a ejecutar el nuevo proceso y se retornar su pid.
        childProcess.execute(fileName, childArgs);
        return childProcess.getPid();
    }
    
    /**
     * Handle the join() system call. 
     */
    protected int handleJoin(int processID, int status) {
        if (processID == -1){
            Lib.debug(dbgProcessSyscalls, "[UserProcess.handleJoin]: Proceso " + this + " no puede hacer join, ya que el pid del proceso es -1 (no existe)!");
            return -1;
        }

        UserProcess joining = UserKernel.getProcessById(processID);
        if (joining != null){
            Lib.debug(dbgProcessSyscalls, "[UserProcess.handleJoin]: Proceso "+this+" esta haciendo join al proceso " + joining);
            if (joining.doneExecuting()){
                Lib.debug(dbgProcessSyscalls, "[UserProcess.handleJoin]: Proceso "+this+" no puede hacer join, ya que el proceso " + joining + " ya ha terminado ejecucion!");
                return -1;
            }
        }
        else {
            Lib.debug(dbgProcessSyscalls, "[UserProcess.handleJoin]: Proceso "+this+" no pudo hacer join, ya que no existe algun proceso con pid = " + processID);
            return -1;
        }
    
        // verificar si el processID corresponde a un hijo...
        boolean myChild = false;
        int index = 0;
        for (UserProcess child : this.childrenProcesses){
            if (child.getPid() == processID){
                myChild = true;
                break;
            }
            index++;
        }
        if (!myChild){
            if (joining != null)
                Lib.debug(dbgProcessSyscalls, "[UserProcess.handleJoin]: Proceso " + this + " no pudo hacer join al proceso " + joining + " ya que este no es hijo suyo!");
            else Lib.debug(dbgProcessSyscalls, "[UserProcess.handleJoin]: No se pudo hacer join, el proceso con pid = " + processID + " debe ser un child process del proceso actual!");
            return -1;
        }
        UserProcess childProcessToJoin = this.childrenProcesses.get(index);
        // Mientras el status del hijo no sea "finished", dormir al proceso padre (el actual)
        childProcessToJoin.acquireParentLock(this);
        if (childProcessToJoin.getUThread() != null){
            while (!childProcessToJoin.doneExecuting())
                childProcessToJoin.parentGoToBed(this);
            childProcessToJoin.releaseParentLock(this);
        }
        
        // Si se llegó a este punto, quiere decir que el hijo ha terminado ejecución 
        // y el padre puede continuar ejecución...
        this.childrenProcesses.remove(index);
        Integer childExitStatus = childProcessToJoin.getExitStatus();

        if (childExitStatus != null){
            writeVirtualMemory(status, Lib.bytesFromInt(childExitStatus));
            return 1;
        }
        // Si el exitStatus era nulo, quiere decir que exit para el proceso hijo fue
        // llamado luego de ocurrir una unhandled exception... por lo tanto retornar 0
        return 0;
    }
    protected int handleCreate(int arg) {
        return openOrCreateFile(arg, false);
    }
    protected int handleOpen(int arg) {
        return openOrCreateFile(arg, true);
    }

    protected int openOrCreateFile(int name, boolean open){
        String dbgMethod = "[UserProcess." + ((open) ? "handleOpen]":"handleCreate]");
        String dbgAction = (open) ? "abrir":"crear";
        
        String fileName = readVirtualMemoryString(name, maximumFileNameLength);
        if(fileName == null){
            Lib.debug(dbgProcessSyscalls, dbgMethod+": El nombre del archivo no es valido.");
            return -1;
        }
        
        if (open)
            Lib.debug(dbgProcessSyscalls, dbgMethod + ": Abriendo el archivo " + fileName);
        else Lib.debug(dbgProcessSyscalls, dbgMethod + ": Creando el archivo " + fileName);

        OpenFile file = UserKernel.fileSystem.open(fileName, !open);
        int fileDescriptor = getNextAvailableFileDescriptor();
        if(fileDescriptor == -1){
            Lib.debug(dbgProcessSyscalls, dbgMethod+": No es posible " + dbgAction +" el archivo "
                    + fileName + ", el proceso "+this+" ya ha agotado sus "
                    + maximumOpenFiles + " file descriptors. Debera cerrar alguno antes.");
            return -1;
        }
        if (file == null){
            Lib.debug(dbgProcessSyscalls, dbgMethod+": El FileSystem no pudo crear el archivo "+fileName+".");
            return -1;
        }
        openFilesTable[fileDescriptor] = file;

        RunningFile runFile;
        if(runningFiles.containsKey(fileName)){
            flagsLock.acquire();
            runFile = runningFiles.remove(fileName);
            flagsLock.release();
        }
        else
            runFile = new RunningFile();
    
        if(!runFile.getUnlinkState()){
            runFile.increment();
            flagsLock.acquire();
            runningFiles.put(fileName, runFile);
            flagsLock.release();
        }else{
            Lib.debug(dbgProcessSyscalls, dbgMethod+": El archivo "+fileName+" estaba por ser borrado (unlink).");
            return -1;
        }
        Lib.debug(dbgProcessSyscalls, dbgMethod+": El archivo "+fileName+" fue asociado correctamente al file descriptor '"+fileDescriptor+"'");
        return fileDescriptor;    
    }

    protected int handleRead(int fd, int buff, int cant) {
        if (!verifyFileDescriptor(fd)){
            Lib.debug(dbgProcessSyscalls, "[UserProcess.handleRead]: El file descriptor ('"
                                            +fd+"') del archivo a leer no es valido");
            return -1;
        }
        if (cant <= 0){
            Lib.debug(dbgProcessSyscalls, "[UserProcess.handleRead]: Cantidad de bytes a leer debe ser positiva!");
            return -1;
        }
        
        byte buffer[] = new byte[cant];
        readingLock.acquire();
            int readCant = openFilesTable[fd].read(buffer, 0, cant);
        readingLock.release();

        if(readCant < 0){
            Lib.debug(dbgProcessSyscalls, "[UserProcess.handleRead]: No se pudo leer ningun byte del archivo "+fileName);        
            return -1;
        }
        
        int writeCant = writeVirtualMemory(buff, buffer, 0, readCant);
        if(readCant != writeCant){
            Lib.debug(dbgProcessSyscalls, "[UserProcess.handleRead]: La cantidad de bytes re-escritos en memoria no fue igual que la cantida leida del archivo!");            
            return -1;
        }
        if (fd != 0)
            Lib.debug(dbgProcessSyscalls, "[UserProcess.handleRead]: Se han leido " + writeCant +
                                      " bytes correctamente del archivo "+openFilesTable[fd].getName());        
        return readCant;
    }

    protected int handleWrite(int fd, int buff, int cant) {
        if (!verifyFileDescriptor(fd)){
            Lib.debug(dbgProcessSyscalls, "[UserProcess.handleWrite]: El file descriptor ('"
                                            +fd+"') del archivo a escribir no es valido");
            return -1;
        }
        if (cant < 0){
            Lib.debug(dbgProcessSyscalls, "[UserProcess.handleWrite]: Cantidad de bytes a escribir no puede ser negativa!");
            return -1;
        }
        else if (cant == 0){
            Lib.debug(dbgProcessSyscalls, "[UserProcess.handleWrite]: Cantidad de bytes a escribir es 0, nada por hacer...");
            return 0;
        }

        byte buffer[] = new byte[cant];
        int readCant = readVirtualMemory(buff, buffer);
        if(readCant < 0){
            return -1;
        }
        writingLock.acquire();
            int writeCant = openFilesTable[fd].write(buffer, 0, readCant);
        writingLock.release();
        if (fd != 1)
            Lib.debug(dbgProcessSyscalls, "[UserProcess.handleWrite]: Se han escrito " + writeCant +
                                      " bytes correctamente en el archivo "+openFilesTable[fd].getName());
        return writeCant;
    }

    protected int handleClose(int fd) {
        if (!verifyFileDescriptor(fd)){
            Lib.debug(dbgProcessSyscalls, "[UserProcess.handleClose]: El file descriptor " + fd
                                            + " no esta asociado a ningun archivo");
            return -1;
        }
        String name = openFilesTable[fd].getName();
        boolean unlinking = false;
        if(runningFiles.containsKey(name)){
            RunningFile runFile = runningFiles.get(name);
            runFile.decrement();
            if(runFile.getCantRunning() == 0){
                unlinking = runFile.getUnlinkState();
                flagsLock.acquire();
                runningFiles.remove(name);
                flagsLock.release();
            }
        }
        openFilesTable[fd].close();
        openFilesTable[fd] = null;

        if(unlinking && !UserKernel.fileSystem.remove(name))
            return -1;
        if (fd > 1)
            Lib.debug(dbgProcessSyscalls, "[UserProcess.handleClose]: Se ha cerrado el archivo " + name
                                        + ", el file descriptor " + fd + " se encuentra disponible nuevamente");
        return 0;
    }
    protected int handleUnlink(int name) {
        String fileName = readVirtualMemoryString(name, maximumFileNameLength);
        if(runningFiles.containsKey(fileName)){
            flagsLock.acquire();
            runningFiles.get(fileName).unlinkFile();
            flagsLock.release();
        }
        else if(!UserKernel.fileSystem.remove(fileName))
                return -1;
        Lib.debug(dbgProcessSyscalls, "[UserProcess.handleUnlink]: El archivo "+fileName+ " ha sido eliminado correctamente");

        return 0;
    }

    protected static final int
        syscallHalt = 0,
        syscallExit = 1,
        syscallExec = 2,
        syscallJoin = 3,
        syscallCreate = 4,
        syscallOpen = 5,
        syscallRead = 6,
        syscallWrite = 7,
        syscallClose = 8,
        syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     *                              </tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *                              </tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *                              </tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param   syscall the syscall number.
     * @param   a0  the first syscall argument.
     * @param   a1  the second syscall argument.
     * @param   a2  the third syscall argument.
     * @param   a3  the fourth syscall argument.
     * @return  the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
        //System.out.println("Syscall: " + syscall);
        switch (syscall) {
            case syscallHalt:
                return handleHalt();
            case syscallExit:
                return handleExit(a0);
            case syscallExec:
                return handleExec(a0, a1, a2);
            case syscallJoin:
                return handleJoin(a0, a1);
            case syscallCreate:
                return handleCreate(a0);
            case syscallOpen:
                return handleOpen(a0);
            case syscallRead:
                return handleRead(a0, a1, a2);
            case syscallWrite:
                return handleWrite(a0, a1, a2);
            case syscallClose:
                return handleClose(a0);
            case syscallUnlink:
                return handleUnlink(a0);
            default:
                Lib.debug(dbgProcess, "Unknown syscall " + syscall);
                Lib.assertNotReached("Unknown system call!");
       }
       
       return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param   cause   the user exception that occurred.
     */
    public void handleException(int cause) {
        Processor processor = Machine.processor();
        switch (cause) {
            case Processor.exceptionSyscall:
                int result = handleSyscall(processor.readRegister(Processor.regV0),
                               processor.readRegister(Processor.regA0),
                               processor.readRegister(Processor.regA1),
                               processor.readRegister(Processor.regA2),
                               processor.readRegister(Processor.regA3)
                               );
                processor.writeRegister(Processor.regV0, result);
                processor.advancePC();
                break;                     
                           
            default:
                Lib.debug(dbgProcess, "Unexpected exception: " +
                      Processor.exceptionNames[cause]);
                this.exitStatus = new Integer(-1);
                handleExit(-1);
                Lib.assertNotReached("Unexpected exception");
        }
    }

    public String toString(){
        return "[Nombre:"+this.fileName+" -- Pid:"+this.pid+"]";
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    protected int initialPC, initialSP;
    protected int argc, argv;
    
    public static final char dbgProcess = 'a';
    public static final char dbgLoading = 'l';
    public static final char dbgProcessSyscalls = 's';

    public static final int pageSize = Processor.pageSize;
    public static final int maximumFileNameLength = 256;
    public static final int maximumOpenFiles = 16;
    public static final int ROOT_PID = 1;
    
    public static Lock readingLock = new Lock();
    public static Lock writingLock = new Lock();
    public static Lock accesingMemoryLock = new Lock();
    public static Lock flagsLock = new Lock();

    public static int createdProcesses = 0;
    public static int currentlyRunningProcesses = 0;
    public static HashMap<String, RunningFile> runningFiles
                        = new HashMap<String, RunningFile>();;
    


    public OpenFile[] openFilesTable;
    public LinkedList<UserProcess> childrenProcesses;
    public UserProcess parentProcess;
    public String fileName;
    public int pid;
    public Integer exitStatus;
    public UThread thread;
    public Lock lockForParent;
    public Condition2 waitingParentCondition;
}
