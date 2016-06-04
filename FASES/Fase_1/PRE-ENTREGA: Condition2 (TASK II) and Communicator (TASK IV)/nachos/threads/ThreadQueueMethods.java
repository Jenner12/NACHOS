package nachos.threads;

import nachos.machine.*;
import java.util.NoSuchElementException;
import java.lang.IndexOutOfBoundsException;

public  class ThreadQueueMethods{

	public static int ThreadQueueIndexOf(ThreadQueue queue, KThread kthread){
		boolean intStatus = Machine.interrupt().disable();
		KThread t;
		int index = -1, cont = 0;
        ThreadQueue aux = ThreadedKernel.scheduler.newThreadQueue(false);
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
	        ThreadQueue aux = ThreadedKernel.scheduler.newThreadQueue(false);
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
        ThreadQueue aux = ThreadedKernel.scheduler.newThreadQueue(false);
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
        ThreadQueue aux = ThreadedKernel.scheduler.newThreadQueue(false);
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
        boolean intStatus = Machine.interrupt().disable();
        KThread res = null;
        if (ThreadQueueIsEmpty(queue))
           throw new NoSuchElementException("Impossible to get the ThreadQueue's fist element, the ThreadQueue was empty!");
        res = queue.nextThread();
        ThreadQueue aux = ThreadedKernel.scheduler.newThreadQueue(false);
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
        ThreadQueue aux = ThreadedKernel.scheduler.newThreadQueue(false);
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
        ThreadQueue aux = ThreadedKernel.scheduler.newThreadQueue(false);
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
        ThreadQueue aux = ThreadedKernel.scheduler.newThreadQueue(false);
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
        boolean intStatus = Machine.interrupt().disable();
        int size = 0;
        KThread t;
        ThreadQueue aux = ThreadedKernel.scheduler.newThreadQueue(false);
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
        return ThreadQueueSize(queue) == 0;
    }
}