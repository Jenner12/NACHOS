package nachos.threads;

import nachos.machine.*;
import java.util.NoSuchElementException;
import java.util.LinkedList;
import java.lang.IndexOutOfBoundsException;
import nachos.threads.PriorityScheduler.ThreadState;
import nachos.threads.PriorityScheduler.PriorityQueue;

public  class ThreadQueueMethods{

    private static boolean transfer = ThreadedKernel.schedulerType == 'c';

    public static void PriorityQueueSort(PriorityQueue queue){
        Lib.debug('t', "PriorityQueueSort()");
        LinkedList<ThreadState> states = queue.getThreadsStatesList();
        int size = states.size();
        if (size == 0)
            return;
        quickSort(states, 0, size-1);
        LinkedList<KThread> sortedThreads = new LinkedList<KThread>();
        for (ThreadState state : states){
            sortedThreads.add(state.getThread());
        }
        queue.setWaitQueue(sortedThreads);
    }
    private static int partition(LinkedList<ThreadState> v, int p, int r){
        ThreadState x = v.get(r);
        int i = p - 1;
        ThreadState aux;
        for (int j = p; j < r; j++){
            if (v.get(j).getEffectivePriority() > x.getEffectivePriority()){
                i++;
                aux = v.get(i);
                v.set(i, v.get(j));
                v.set(j, aux);
            }
            else if (v.get(j).getEffectivePriority() == x.getEffectivePriority()){
                if (v.get(j).getWaitingTime() <= x.getWaitingTime()){
                    i++;
                aux = v.get(i);
                v.set(i, v.get(j));
                v.set(j, aux);
                }
            }
        }
        aux = v.get(r);
        v.set(r, v.get(++i));
        v.set(i, aux);
        return i;
    }

    public static void quickSort(LinkedList<ThreadState> v, int p, int r){
            if (p < r){
                int q = partition(v, p, r);
                quickSort(v, p, q-1);
                quickSort(v, q+1, r);
            }
        }

	public static int ThreadQueueIndexOf(ThreadQueue queue, KThread kthread){
		boolean intStatus = Machine.interrupt().disable();
		KThread t;
		int index = -1, cont = 0;
        ThreadQueue aux = ThreadedKernel.scheduler.newThreadQueue(transfer);
        while ((t = queue.nextThread()) != null){
            aux.waitForAccess(t);
            if (t.compareTo(kthread) == 0  && index == -1)
            	index = cont;
            cont++;
        }
        while ((t = aux.nextThread()) != null)
            queue.waitForAccess(t);   
		Machine.interrupt().restore(intStatus);
		return index;
	}
	public static boolean ThreadQueueContains(ThreadQueue queue, KThread kthread){
		return ThreadQueueIndexOf(queue, kthread) != -1;
	}


	public static void ThreadQueueAdd(ThreadQueue queue, KThread kthread, int index){
        boolean intStatus = Machine.interrupt().disable();
        if (index < 0 || index >= ThreadQueueSize(queue))
            throw new IndexOutOfBoundsException("Impossible to add the element to the ThreadQueue, index " + index + " is out of range for this ThreadQueue!");
        if (index == 0)
        	ThreadQueueAddFirst(queue, kthread);
        else if (index == ThreadQueueSize(queue))
        	queue.waitForAccess(kthread);
        else{
	        KThread t;
	        int cont = 0;
	        ThreadQueue aux = ThreadedKernel.scheduler.newThreadQueue(transfer);
	        while ((t = queue.nextThread()) != null){
	            if (cont++ == index)
	            	aux.waitForAccess(kthread);
	            aux.waitForAccess(t);
	        }
	        while ((t = aux.nextThread()) != null)
	            queue.waitForAccess(t);   
	    }
        Machine.interrupt().restore(intStatus);
    }

    public static void ThreadQueueAddFirst(ThreadQueue queue, KThread kthread){
        boolean intStatus = Machine.interrupt().disable();
        KThread t = null;
        ThreadQueue aux = ThreadedKernel.scheduler.newThreadQueue(transfer);
        while ((t = queue.nextThread()) != null)
            aux.waitForAccess(t);
        queue.waitForAccess(kthread);
        while ((t = aux.nextThread()) != null)
            queue.waitForAccess(t);
        Machine.interrupt().restore(intStatus);
    }

    public static KThread ThreadQueueGet(ThreadQueue queue, int index){
        boolean intStatus = Machine.interrupt().disable();
        KThread res = null;
        if (ThreadQueueIsEmpty(queue))
            throw new IndexOutOfBoundsException("Impossible to get the ThreadQueue's element, the ThreadQueue was empty!");
        if (index < 0 || index >= ThreadQueueSize(queue))
            throw new IndexOutOfBoundsException("Impossible to get the ThreadQueue's element, index " + index + " is out of range for this ThreadQueue!");
        KThread t;
        int cont = 0;
        ThreadQueue aux = ThreadedKernel.scheduler.newThreadQueue(transfer);
        while ((t = queue.nextThread()) != null){
            aux.waitForAccess(t);
            if (cont++ == index)
                res = t;
        }
        while ((t = aux.nextThread()) != null)
            queue.waitForAccess(t);   
        Machine.interrupt().restore(intStatus);
        return res;
    }

    public static KThread ThreadQueueGetFirst(ThreadQueue queue){
        Lib.debug('t', "ThreadQueueGetFirst()");
        boolean intStatus = Machine.interrupt().disable();
        KThread res = null;
        if (ThreadQueueIsEmpty(queue))
           throw new NoSuchElementException("Impossible to get the ThreadQueue's fist element, the ThreadQueue was empty!");
        res = queue.nextThread();
        ThreadQueue aux = ThreadedKernel.scheduler.newThreadQueue(transfer);
        KThread t;
        while ((t = queue.nextThread()) != null)
            aux.waitForAccess(t);
        queue.waitForAccess(res);
        while ((t = aux.nextThread()) != null)
            queue.waitForAccess(t);   
        Machine.interrupt().restore(intStatus);
        return res;
    }


    public static KThread ThreadQueueGetLast(ThreadQueue queue){
        boolean intStatus = Machine.interrupt().disable();
        KThread t, res = null;
        if (ThreadQueueIsEmpty(queue))
            throw new NoSuchElementException("Impossible to get the ThreadQueue's last element, the ThreadQueue was empty");
        ThreadQueue aux = ThreadedKernel.scheduler.newThreadQueue(transfer);
        while ((t = queue.nextThread()) != null){
            aux.waitForAccess(t);
            res = t;
        }
        while ((t = aux.nextThread()) != null)
            queue.waitForAccess(t);           
        Machine.interrupt().restore(intStatus);
        return res;
    }

    public static KThread ThreadQueueRemove(ThreadQueue queue, int index){
        boolean intStatus = Machine.interrupt().disable();
        KThread t, res = null;
        if (ThreadQueueIsEmpty(queue))
            throw new IndexOutOfBoundsException("Impossible to remove the ThreadQueue's element, the ThreadQueue was empty!");
        if (index < 0 || index >= ThreadQueueSize(queue))
            throw new IndexOutOfBoundsException("Impossible to remove the ThreadQueue's element, index " + index + " is out of range for this ThreadQueue!");
        int cont = 0;
        ThreadQueue aux = ThreadedKernel.scheduler.newThreadQueue(transfer);
        while ((t = queue.nextThread()) != null){
            if (cont++ == index)
                res = t;
            else
                aux.waitForAccess(t);
        }
        while ((t = aux.nextThread()) != null)
            queue.waitForAccess(t);        
        Machine.interrupt().restore(intStatus);
        return res;
    }

    public static KThread ThreadQueueRemoveLast(ThreadQueue queue){
        boolean intStatus = Machine.interrupt().disable();
        KThread t, res = null;
        if (ThreadQueueIsEmpty(queue))
            throw new NoSuchElementException("Impossible to remove the ThreadQueue's fist element, the ThreadQueue was empty!");

        int cont = 0, size = ThreadQueueSize(queue);
        ThreadQueue aux = ThreadedKernel.scheduler.newThreadQueue(transfer);
        while ((t = queue.nextThread()) != null){
            if (++cont == size)
                res = t;
            else
                aux.waitForAccess(t);
        }
        while ((t = aux.nextThread()) != null)
            queue.waitForAccess(t);
    
        Machine.interrupt().restore(intStatus);
        return res;
    }

    public static int ThreadQueueSize(ThreadQueue queue){
        Lib.debug('t', "ThreadQueueSize()");
        boolean intStatus = Machine.interrupt().disable();
        int size = 0;
        KThread t;
        ThreadQueue aux = ThreadedKernel.scheduler.newThreadQueue(transfer);
        while ((t = queue.nextThread()) != null){
            aux.waitForAccess(t);
            size++;
        }
        while ((t = aux.nextThread()) != null)
            queue.waitForAccess(t);
        Machine.interrupt().restore(intStatus);
        return size;
    }
    public static boolean ThreadQueueIsEmpty(ThreadQueue queue){
        //return ThreadQueueSize(queue) == 0;
        //boolean intStatus = Machine.interrupt().disable();
        Lib.debug('t', "ThreadQueueIsEmpty()");
        int size = 0;
        KThread t;
        ThreadQueue aux = ThreadedKernel.scheduler.newThreadQueue(transfer);
        while ((t = queue.nextThread()) != null){
            aux.waitForAccess(t);
            size++;
        }
        while ((t = aux.nextThread()) != null)
            queue.waitForAccess(t);
        //Machine.interrupt().restore(intStatus);
        return size == 0;
    }
}