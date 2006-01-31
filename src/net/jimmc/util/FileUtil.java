/* FileUtil.java
 *
 * Jim McBeath, May 10, 2002
 */

package net.jimmc.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/** Utilities for dealing with files. */
public class FileUtil {
    /** Read in a text file, return the contents as a string. */
    public static String readFile(File f) 
	    throws FileNotFoundException, IOException {
	FileReader fr = new FileReader(f);
	BufferedReader br = new BufferedReader(fr);
	String line;
	StringBuffer sb = new StringBuffer();
	while ((line=br.readLine())!=null) {
	    sb.append(line);
	    sb.append("\n");
	}
	br.close();
	return sb.toString();
    }

    /** Write text out to a file.
     */
    public static void writeFile(File f, String contents)
	    throws IOException {
	FileWriter writer = new FileWriter(f);
	writer.write(contents);
	writer.close();
    }

    /** Given a file, rename it by adding .bak to the name.
     * If that backup file already exists, delete it first.
     */
    public static void renameToBak(File f) {
	if (!f.exists())
	    return;		//not there, ignore
	//If we already have an index file, rename it to
	//a backup name.
	File bakFile = new File(f.toString()+".bak");
	bakFile.delete();
	    //If the backup file is there delete it,
	    //ignore error
	if (f.renameTo(bakFile))
	    return;		//OK, done
        /* TBD - use resource code such as the following to generate the
         * error message...
	Object[] args = { f, bakFile };
	String eMsg = res.getResourceFormatted(
	    "FileUtil.error.RenamingFile",
	    args);
        ...*/
        String eMsg = "Can't rename "+f+" to "+bakFile;
	throw new RuntimeException(eMsg);
    }

    /** Get a PrintWriter for the specified file.
     * If the file aready exists, rename it to a .bak file.
     * @return The opened PrintWriter.
     */
    public static PrintWriter bakPrintWriterFor(File f) {
	renameToBak(f);
	try {
	    PrintWriter w = new PrintWriter(new FileWriter(f));
	    return w;
	} catch (IOException ex) {
	    //TBD - handle this exception?
	    throw new MoreException(ex);
	}
    }
}

/* end */
