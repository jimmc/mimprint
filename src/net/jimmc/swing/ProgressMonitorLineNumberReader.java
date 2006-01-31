/* ProgressMonitorLineNumberReader.java
 *
 * Jim McBeath, November 24, 2001
 */

package net.jimmc.swing;

import java.awt.Component;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import javax.swing.ProgressMonitor;

/** Like the standard swing ProgressMonitorInputStream, but works
 * as a LineNumberReader.
 * This works better than using a ProgressMonitorInputStream because
 * we get a progress tick at the per-line level rather than at the
 * block level, which tends to be read in larger chunks.
 * <p>
 * Only the {@link #readLine} method of LineNumberReader is supported.
 */
public class ProgressMonitorLineNumberReader extends LineNumberReader {
	private int size;	//the size of the input stream
	private int location;	//current location in the file
	private ProgressMonitor monitor;

	/** Create the monitor stream.
	 * @param parent The parent of the ProgressMonitor
	 * @param message The message for the Progress Monitor.
	 *        Typically something like "Reading <i>filename</i>..."
	 * @param reader The input stream.
	 * @param size The size of the input stream.
	 */
	public ProgressMonitorLineNumberReader(Component parent,
			Object message, Reader reader, int size) {
		super(reader);
		this.size = size;
		location = 0;
		monitor = new ProgressMonitor(parent,message,null,0,size);
	}

	/** Get the ProgressMonitor we are using.
	 */
	public ProgressMonitor getProgressMonitor() {
		return monitor;
	}

	/** Read the next line of input. */
	public String readLine() throws IOException {
		if (monitor.isCanceled()) {
			throw new IOException("Input cancelled"); //TBD i18n
		}
		String line = super.readLine();
		if (line==null)
			location = size;	//reached the end
		else
			location += line.length();
		monitor.setProgress(location);
		return line;
	}

	/** Close our stream and progress monitor. */
	public void close() throws IOException {
		super.close();
		monitor.close();
	}
}
