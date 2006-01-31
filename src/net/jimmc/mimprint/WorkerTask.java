/* WorkerTask.java
 *
 * Jim McBeath, August 17, 2002
 */

package net.jimmc.jiviewer;

/** A task to be executed by a {@link Worker} thread.
 */
public abstract class WorkerTask {
	/** Data for this task. */
	protected Object data;

	/** Create a task with no data. */
	public WorkerTask() {
	}

	/** Create a task with data.
	 */
	public WorkerTask(Object data) {
		setData(data);
	}

	/** Set the data for this task. */
	public void setData(Object data) {
		this.data = data;
	}

	/** Get the data for this class. */
	public Object getData() {
		return data;
	}

	/** The action for this task.
	 */
	public abstract void run();
}

//end
