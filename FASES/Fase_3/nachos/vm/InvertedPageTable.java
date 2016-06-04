package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.util.HashMap;

public class InvertedPageTable{

	public InvertedPageTable(){
		this.hash = new HashMap<>();
		this.lock = new Lock();
	}

	
	public TranslationEntry getTranslation(int pid, int vpn){
		//this.lock.acquire();
		return this.hash.get(new PageKey(pid,vpn));
		//this.lock.release();
	}

	public void addTranslation(int pid, int vpn, TranslationEntry te){
		this.lock.acquire();
		this.hash.put(new PageKey(pid, vpn), te);
		this.lock.release();
	}	

	public void removeTranslation(int pid, int vpn){
		this.lock.acquire();
		this.hash.remove(new PageKey(pid, vpn));
		this.lock.release();
	}

	public boolean containsKey(int pid, int vpn){
		return this.hash.containsKey(new PageKey(pid, vpn));
	}

	public boolean isEmpty(){
		//this.lock.acquire();
		return this.hash.isEmpty();
		//this.lock.release();
	}

	public void clear(){
		this.lock.acquire();
		this.hash.clear();
		this.lock.release();
	}
	
	private static final char dbgInverted = 'i';

    public static HashMap<PageKey,TranslationEntry> hash;
    public static Lock lock;
}