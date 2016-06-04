package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.util.HashMap;

public class SwapFile{

	private static final String SWAP_FILE_NAME = "nachos.swp";
	public static final int PAGES_CAPACITY = 64;


	public SwapFile(){
		Lib.debug(dbgSwap, "[SwapFile.Constructor]: Creando swap file de nombre \"" + SWAP_FILE_NAME + "\" con "
							+ " capacidad para almacenar " + PAGES_CAPACITY + " paginas.");
		this.swapFile = ThreadedKernel.fileSystem.open(SWAP_FILE_NAME, true);
		byte relleno[] = new byte[PAGES_CAPACITY * Processor.pageSize];
		java.util.Arrays.fill(relleno, (byte)0);
		this.swapFile.write(relleno, 0, relleno.length);
		
		swapPages = new HashMap<>();
		pagesPositions = new HashMap<>();
	}

	public boolean pageInSwap(int pid, int vpn){
		PageKey key = new PageKey(pid, vpn);
		return this.swapPages.containsKey(key);
	}

	public int getPagePosition(int pid, int vpn){
		if (pageInSwap(pid, vpn)){
			Lib.debug(dbgSwap, "[SwapFile.getPagePosition]: Dicha pagina no se encuentra en el swap file!");
			return -1;
		}
		PageKey key = new PageKey(pid, vpn);
		return this.pagesPositions.get(key).intValue();
	}

	public void terminate(){
		Lib.debug(dbgSwap, "[SwapFile.terminate]: Cerrando y eliminando swap file \"" + SWAP_FILE_NAME+"\" !");
		swapFile.close();
        VMKernel.fileSystem.remove(swapFile.getName());
	}

	public void readPage(int pagePosition, byte[] buffer, int bufferStart, int length){
		this.swapLock.acquire();
		this.swapFile.read(Processor.pageSize*pagePosition, buffer, bufferStart, length);
		this.swapLock.release();
	}

	public void writePage(int pagePosition, byte[] buffer, int bufferStart, int length){
		this.swapLock.acquire();
		this.swapFile.write(Processor.pageSize*pagePosition, buffer, bufferStart, length);
		this.swapLock.release();
	}

	private static final char dbgSwap = 'S';

	private OpenFile swapFile;
	private Lock swapLock;
	public HashMap<PageKey, Integer> pagesPositions;
	public HashMap<PageKey, TranslationEntry> swapPages;
}