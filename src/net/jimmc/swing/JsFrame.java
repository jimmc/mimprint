/* JsFrame.java
 *
 * Jim McBeath, June 11, 1998 (from jimmc.roots.RootsFrame)
 */

package net.jimmc.swing;

import net.jimmc.util.ExceptionHandler;
import net.jimmc.util.MoreException;
import net.jimmc.util.ResourceSource;
import net.jimmc.util.StringUtil;
import net.jimmc.util.UserException;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** A general Frame with utility methods.
 */
public class JsFrame extends JFrame implements ExceptionHandler {
	/** Our resource source. */
	protected ResourceSource res;

	protected int maxSimpleMessageLength = 80;
	protected int maxSimpleMessageLineCount = 30;
	protected int maxSimpleMessageLineWidth = 80;

	protected boolean debugUserExceptions;

	/** Create a frame without a name. */
	public JsFrame() {
		super();
	}

	/** Create a frame with a name. */
	public JsFrame(String name) {
		super(name);
	}

        protected Component getDialogParent() {
            return this;
        }

	/** Set our resource source. */
	public void setResourceSource(ResourceSource resourceSource) {
		this.res = resourceSource;
	}

	/** Get our resource source. */
	public ResourceSource getResourceSource() {
		return res;
	}

	/** Set to true to show a Details button on a UserException. */
	public void setDebugUserExceptions(boolean debug) {
		debugUserExceptions = debug;
	}

	/** True means show a Details button on a UserException. */
	public boolean isDebugUserExceptions() {
		return debugUserExceptions;
	}

	/** Add a window listener to close our window. */
	protected void addWindowListener() {
		WindowAdapter a = new WindowAdapter() {
			public void windowClosing(WindowEvent ev) {
				processClose();
			}
		};
		addWindowListener(a);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
	}

	/** Create a default menu bar with a File menu. */
	protected JMenuBar createMenuBar() {
		JMenuBar mb = new JMenuBar();
		mb.add(createFileMenu());
		return mb;
	}

	/** Create the File menu. */
	protected JMenu createFileMenu() {
		JMenu m = new JMenu("File");
		addCloseMenuItem(m);
		return m;
	}

	/** Add the File->Close menu item. */
	protected void addCloseMenuItem(JMenu m) {
		m.add(new MenuAction(res,"menu.File.Close",this) {
			public void action() {
				processClose();
			}
		});
	}

	/** Create the Help menu. */
	protected JMenu createHelpMenu() {
		JMenu m = createHelpMenuOnly();
		addAboutMenuItem(m);
		return m;
	}

	/** Create the Help menu without any entries. */
	protected JMenu createHelpMenuOnly() {
		String helpLabel = res.getResourceString("menu.Help.label");
		return new JMenu(helpLabel);
	}

	/** Add the About entry to the menu. */
	protected void addAboutMenuItem(JMenu menu) {
		MenuAction mi = new MenuAction(res,"menu.Help.About",this) {
			public void action() {
				processHelpAbout();
			}
		};
		menu.add(mi);
	}

	/** Process the Help->About menu command. */
	protected void processHelpAbout() {
		AboutWindow.showAboutWindow(this);
	}

	/** Close this window. */
	protected void processClose() {
		setVisible(false);
		dispose();
	}

	/** Process the File->Exit menu command. */
	protected void processFileExit() {
		if (confirmExit()) {
			System.exit(0);
		}
	}

	/** Ask if he really wants to exit from the program. */
	protected boolean confirmExit() {
		String msg = res.getResourceString("query.Exit.prompt");
		String title = res.getResourceString("query.Exit.title");
		Object msgObj = getMessageDisplay(msg);
		int n = JOptionPane.showConfirmDialog(getDialogParent(),msgObj,title,
				JOptionPane.YES_NO_OPTION);
		return (n==0);
	}

	/* Put up an OK/Cancel dialog.
	 * @param prompt The prompt string to display in the dialog.
	 * @return True if the user selects OK,
	 *         false if the users selectes Cancel.
	 */
	public boolean confirmDialog(String prompt) {
		String title = res.getResourceString("query.Confirm.title");
		Object promptObj = getMessageDisplay(prompt);
		int n = JOptionPane.showConfirmDialog(getDialogParent(),promptObj,title,
				JOptionPane.OK_CANCEL_OPTION);
		return (n==0);
	}

	/** Put up an error dialog. */
	public void errorDialog(String errmsg) {
		String title = res.getResourceString("query.Error.title");
		showMessageDialog(errmsg,title,JOptionPane.ERROR_MESSAGE);
	}

	/** Put up an info dialog. */
	public void infoDialog(String infomsg) {
		String title = res.getResourceString("query.Info.title");
		showMessageDialog(infomsg,title,
			JOptionPane.INFORMATION_MESSAGE);
	}

	protected void showMessageDialog(String msg,String title,int paneType) {
		Object messageDisplay = getMessageDisplay(msg);
		if (messageDisplay instanceof String) {
			JOptionPane.showMessageDialog(getDialogParent(),messageDisplay,title,
				paneType);
		} else {
			//We have a large string which has been wrapped up
			//in a scrolled text area.
			JOptionPane pane = new JOptionPane(messageDisplay,
				paneType);
			JDialog dlg = pane.createDialog(getDialogParent(),title);
			dlg.setResizable(true);
				//Allow this dialog box to be resized so that
				//the user can make it larger to see more of
				//the text at once.
			pane.setInitialValue(null);
			pane.selectInitialValue();
			dlg.show();
		}
	}

	/** Get an object to display a message.
	 * For short messages, the object is the message itself.
	 * For long messages, the object is a JTextArea inside s JScrollPane.
	 */
	protected Object getMessageDisplay(String msg) {
		if (!messageRequiresScrollPane(msg))
			return msg;
		//It's a long string, so rather than putting it up as
		//a single string, we put it up as a text box with
		//wraparound turned on.
		JTextArea tx = getMessageDisplayTextArea(msg);
		tx.setEditable(false);
		tx.setLineWrap(true);
		JScrollPane pane = new JScrollPane(tx);
		pane.setPreferredSize(new Dimension(500,200));
		return pane;
	}

	/** True if the message is large enough to require the use
	 * of a scroll pane in the dialog.
	 */
	protected boolean messageRequiresScrollPane(String msg) {
		//return (msg.length()<=maxSimpleMessageLength);
		int w = StringUtil.getLongestLineWidth(msg);
		int h = StringUtil.getLineCount(msg);
		return (w>maxSimpleMessageLineWidth ||
		        h>maxSimpleMessageLineCount);
	}

	/** Get the JTextArea to use as the message display box. */
	protected JTextArea getMessageDisplayTextArea(String msg) {
		return new JTextArea(msg);
	}

	/** Put up a string dialog. */
	public String stringDialog(String prompt) {
		return JOptionPane.showInputDialog(prompt);
	}

	/** Put up a file open dialog. */
	public File fileOpenDialog(String prompt) {
		return fileOpenDialog(prompt,(String)null);
	}

	/** Put up a file open dialog.
	 * @return null if cancelled.
	 */
	public File fileOpenDialog(String prompt, String dflt) {
		JFileChooser chooser = new JFileChooser(dflt);
		chooser.setDialogTitle(prompt);
		int result = chooser.showOpenDialog(getDialogParent());
		if (result!=JFileChooser.APPROVE_OPTION)
			return null;	//cancelled
		return chooser.getSelectedFile();
	}

	/** Put up a file open dialog.
	 * @return null if cancelled.
	 */
	public File fileOpenDialog(String prompt, File dflt) {
		JFileChooser chooser = new JFileChooser(dflt);
		chooser.setDialogTitle(prompt);
		int result = chooser.showOpenDialog(getDialogParent());
		if (result!=JFileChooser.APPROVE_OPTION)
			return null;	//cancelled
		return chooser.getSelectedFile();
	}

	public File fileOrDirectoryOpenDialog(String prompt, File dflt) {
            return fileOrDirectoryOpenDialog(prompt, dflt, null);
        }

	/** Put up a dialog to open a file or a directory.
	 * @param prompt The title for the dialog.
	 * @param dflt The intially default file or directory.
	 * @param approveLabel The label to use on the Approve button.
	 * @return The selected file or directory, or null if cancelled.
	 */
	public File fileOrDirectoryOpenDialog(String prompt, File dflt,
				String approveLabel) {
		JFileChooser chooser = new JFileChooser();
		//If the specified directory does not exist, JFileChooser
		//defaults to a completely different directory.
		//In an attempt to be slightly more reasonable, we
		//look for the closest parent that does exist.
		while (dflt!=null && !dflt.exists()) {
			dflt = dflt.getParentFile();
		}
		chooser.setCurrentDirectory(dflt);
		chooser.setDialogType(JFileChooser.OPEN_DIALOG);
		chooser.setDialogTitle(prompt);
		chooser.setMultiSelectionEnabled(false);
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

                int result = (approveLabel!=null)?
                        chooser.showDialog(getDialogParent(), approveLabel):
                        chooser.showOpenDialog(getDialogParent());
                if (result != JFileChooser.APPROVE_OPTION) {
			return null;	//user cancelled
		}

		return chooser.getSelectedFile();
	}

	/** Put up a file save dialog. */
	public File fileSaveDialog(String prompt) {
		return fileSaveDialog(prompt,(String)null);
	}

	/** Put up a file save dialog.
	 * @return null if cancelled
	 */
	public File fileSaveDialog(String prompt, String dflt) {
		JFileChooser chooser = new JFileChooser(dflt);
		chooser.setDialogTitle(prompt);
		int result = chooser.showSaveDialog(getDialogParent());
		if (result!=JFileChooser.APPROVE_OPTION)
			return null;	//cancelled
		return chooser.getSelectedFile();
	}

	/** Put up a file save dialog.
	 * @return null if cancelled
	 */
	public File fileSaveDialog(String prompt, File dflt) {
		JFileChooser chooser = new JFileChooser(dflt);
		chooser.setDialogTitle(prompt);
		int result = chooser.showSaveDialog(getDialogParent());
		if (result!=JFileChooser.APPROVE_OPTION)
			return null;	//cancelled
		return chooser.getSelectedFile();
	}

	/** Put up a directory save dialog.
	 * @return null if cancelled
	 */
	public File directorySaveDialog(String prompt, File dflt) {
		JFileChooser chooser = new JFileChooser(dflt);
		chooser.setDialogTitle(prompt);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int result = chooser.showSaveDialog(getDialogParent());
		if (result!=JFileChooser.APPROVE_OPTION)
			return null;	//cancelled
		return chooser.getSelectedFile();
	}

	/** Put up a dialog to select a directory.
	 * @param defaultDir The directory intially displayed in the dialog.
	 * @param title The title for the dialog.
	 * @param approveLabel The label to use on the Approve button.
	 * @return The selected directory, or null if cancelled.
	 */
	public String selectDirectory(String defaultDir,String title,
				String approveLabel) {
		JFileChooser chooser = new JFileChooser();
		File d = (defaultDir!=null)?new File(defaultDir):null;
		//If the specified directory does not exist, JFileChooser
		//defaults to a completely different directory.
		//In an attempt to be slightly more reasonable, we
		//look for the closest parent that does exist.
		while (d!=null && !d.exists()) {
			d = d.getParentFile();
		}
		chooser.setCurrentDirectory(d);
		chooser.setDialogType(JFileChooser.OPEN_DIALOG);
		chooser.setDialogTitle(title);
		chooser.setMultiSelectionEnabled(false);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		if (chooser.showDialog(getDialogParent(), approveLabel)
			    != JFileChooser.APPROVE_OPTION) {
			return null;	//user cancelled
		}

		return chooser.getSelectedFile().toString();
	}

	/** Put up a three-button Yes/No/Cancel dialog.
	 * @return The index of the selected button:
	 *         0 for yes, 1 for no, 2 for cancel.
	 */
	public int yncDialog(String prompt, String yes, String no,
				String cancel) {
		String title = res.getResourceString("query.Confirm.title");
		String[] labels = { yes, no, cancel };
		return multiButtonDialog(prompt,title,labels);
	}

	/** Put up a dialog with multiple buttons.
	 * @param prompt The prompt string.
	 * @param title The title string.
	 * @param labels The labels to use on the buttons.
	 * @return The index number of the selected button.
	 */
	public int multiButtonDialog(String prompt, String title,
			String[] labels) {
		Object promptObj = getMessageDisplay(prompt);
		String dflt = null;
		int t = JOptionPane.showOptionDialog(getDialogParent(),promptObj,title,
			JOptionPane.DEFAULT_OPTION,JOptionPane.QUESTION_MESSAGE,
			null, labels, dflt);
		return t;
	}

	/** A dialog to display an exception. */
	public void exceptionDialog(Throwable ex) {
		String error = ex.getMessage();
		if (error==null || error.trim().length()==0)
			error = ex.getClass().getName();
		if ((ex instanceof UserException) && isDebugUserExceptions()) {
			//On a user exception, no debugging buttons
			//unless the debug flag has been set.
			errorDialog(error);
			return;
		}
		String title = res.getResourceString("query.Error.title");
		String[] labels = {
		    res.getResourceString("query.button.OK.label"),
		    res.getResourceString("query.button.Details.label"),
		};
		switch (multiButtonDialog(error,title,labels)) {
		case 0:		//OK
			break;		//do nothing
		case 1:		//Details
			exceptionDetailsDialog(ex);
			break;
		}
	}

	/** A dialog to display the traceback on an exception. */
	public void exceptionDetailsDialog(Throwable ex) {
		//Print the stack trace to a string
		StringWriter sw = new StringWriter();
		ex.printStackTrace(new PrintWriter(sw));
		String stackTrace = sw.toString();

		String title= res.getResourceString("query.ErrorDetails.title");
		String[] labels = {
		    res.getResourceString("query.button.OK.label"),
		    res.getResourceString("query.button.PrintTraceback.label"),
		    res.getResourceString(
		    	"query.button.SaveTracebackToFile.label")
		};
		switch (multiButtonDialog(stackTrace,title,labels)) {
		case 0:		//OK
			break;		//do nothing
		case 1:		//Print Traceback
			ex.printStackTrace();
			break;
		case 2:		//Save Traceback To File
			String prompt = res.getResourceString(
				"query.SaveTracebackToFile");
			File traceFile = fileSaveDialog(prompt);
			if (traceFile==null)
				break;	//cancelled
			try {
				FileWriter w = new FileWriter(traceFile);
				PrintWriter pw = new PrintWriter(w);
				ex.printStackTrace(pw);
				pw.close();
				w.close();
			} catch (Exception e2) {
				System.out.println("Error writing trace file");
				//ignore errors in this part
			}
			break;
		}
	}

	/* TBD - add saveTextToFile(String text) that asks here for
	 * the output file.
	 */

	/** Get a PrintWriter for the specified file.
	 * If the file aready exists, ask the user for confirmation
	 * before overwriting the file.
	 * @return The opened PrintWriter, or null if the user
	 *         declined to open the file.
	 */
	public PrintWriter getPrintWriterFor(String filename) {
		return getPrintWriterFor(new File(filename));
	}

	/** Get a PrintWriter for the specified file.
	 * If the file aready exists, ask the user for confirmation
	 * before overwriting the file.
	 * @return The opened PrintWriter, or null if the user
	 *         declined to open the file.
	 */
	public PrintWriter getPrintWriterFor(File f) {
		if (f.exists()) {
			String msg = res.getResourceFormatted(
				"query.OverWriteExistingFile",f.toString());
			if (!confirmDialog(msg))
				return null;	//cancelled
		}
		try {
			PrintWriter w = new PrintWriter(new FileWriter(f));
			return w;
		} catch (IOException ex) {
			//TBD - handle this exception?
			throw new MoreException(ex);
		}
	}

	/** Save the specified text string to the specified file.
	 * If the file already exists, asks the user for confirmation
	 * before overwriting it.
	 * @param text The text to write to the file.
	 * @param filename The output file.
	 * @return True if the data was written to the file,
	 *         false if the file already existed and the user
	 *         declined to overwrite it (i.e. cancelled),
	 *         or on error.
	 */
	public boolean saveTextToFile(String text, String filename) {
		PrintWriter w = getPrintWriterFor(filename);
		if (w==null)
			return false;
		w.write(text);
		w.close();
		return true;
	}

    //The ExceptionHandler interface
	public void beforeAction(Object source) {
		//we do nothing here
	}

	public void afterAction(Object source) {
		//we do nothing here
	}

	public void handleException(Object source, Throwable ex) {
		exceptionDialog(ex);	//display it
	}
    //End ExceptionHandler interface
}

/* end */
