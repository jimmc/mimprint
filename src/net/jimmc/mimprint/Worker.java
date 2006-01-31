/* Worker.java
 *
 * Jim McBeath, August 17, 2002
 */

package net.jimmc.jiviewer;

import java.util.Vector;

/** A worker thread to execute code on behalf of other threads.
 * In particular, this can be used by code running in the Event thread
 * to start up longer-running tasks so that the Event thread is not blocked.
 */
public class Worker extends Thread {
	/** The queue of tasks to be executed by this worker.
	 */
	protected Vector queue;

	/** Flag when we are waiting for something to be put into the queue.
	 * @see #isBusy
	 */
	protected boolean waiting;

	/** Create a worker thread.
	 * Use Thread.start to start the worker.
	 */
	public Worker() {
		queue = new Vector();
	}

	/** Add a task to the worker's queue, to be run in the worker
	 * thread when it gets to it.
	 * Returns immediately.
	 */
	public synchronized void invoke(WorkerTask task) {
		queue.addElement(task);
		notifyAll();
	}

	/** Get the next task from the queue, or return null if no more. */
	protected synchronized WorkerTask getNextTask() {
		if (queue.size()==0)
			return null;	//no items in queue
		WorkerTask task = (WorkerTask)queue.elementAt(0);
		queue.removeElementAt(0);
		return task;
	}

	/** Wait until the queue is not empty. */
	protected synchronized void waitForQueue() {
		if (queue.size()>0)
			return;		//something there, return right away
		//Nothing in the queue, wait until
		//someone puts something in
		try {
			waiting = true;
			wait();
			waiting = false;
		} catch (InterruptedException ex) {
			//ignore
		}
	}

	/** This method gets executed when the worker starts.
	 * It loops, waiting for and executing entries in the queue.
	 */
	public void run() {
		while (true) {
			WorkerTask task;
			while ((task=getNextTask())!=null) {
				task.run();		//execute the task
			}
			waitForQueue();
		}
	}

	/** True if we are busy doing something. */
	public boolean isBusy() {
		return !waiting;
	}
}

//end
