/* Viewer.java
 *
 * Jim McBeath, September 15, 2001
 */

package jimmc.jiviewer;

import jimmc.swing.GridBagger;
import jimmc.swing.JimmcFrame;
import jimmc.swing.MenuAction;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.io.File;
import java.text.MessageFormat;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JSplitPane;

/** The main class for the jiviewer program.
 */
public class Viewer extends JimmcFrame {
	/** Our application info. */
	protected App app;

	/** The list of images. */
	protected ImageLister imageLister;

	/** Create our frame. */
	public Viewer(App app) {
		super();
		this.app = app;
		setJMenuBar(createMenuBar());
		initForm();
		pack();
		addWindowListener();
		setTitleFileName("");
	}

	/** Create our menu bar. */
	protected JMenuBar createMenuBar() {
		JMenuBar mb = new JMenuBar();
		mb.add(createFileMenu());
		mb.add(createHelpMenu());
		return mb;
	}

	/** Create our File menu. */
	protected JMenu createFileMenu() {
		JMenu m = new JMenu("File");
		MenuAction mi;

		String openLabel = getResourceString("menu.File.Open.label");
		mi = new MenuAction(openLabel) {
			public void action() {
				processFileOpen();
			}
		};
		m.add(mi);

		String exitLabel = getResourceString("menu.File.Exit.label");
		mi = new MenuAction(exitLabel) {
			public void action() {
				processFileExit();
			}
		};
		m.add(mi);

		return m;
	}

	/** Create the body of our form. */
	protected void initForm() {
		imageLister = new ImageLister(app,this);
		ImageArea imageArea = new ImageArea(app);
		imageLister.setImageArea(imageArea);
		getContentPane().add(
			new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				imageLister,imageArea));
	}

	/** Get our App. */
	public App getApp() {
		return app;
	}

	/** Open the specified target. */
	public void open(String target) {
		imageLister.open(target);
	}

	/** Open the specified target. */
	public void open(File targetFile) {
		imageLister.open(targetFile);
	}

	/** Process the File->Open menu command. */
	protected void processFileOpen() {
		String msg = getResourceString("query.FileToOpen");
		File targetFile = fileOpenDialog(msg);
		if (targetFile==null)
			return;		//nothing specified
		open(targetFile);		//open it
	}

	/** Closing this form exits the program. */
	protected void processClose() {
		processFileExit();
	}

	/** Set the specified file name into our title line. */
	public void setTitleFileName(String fileName) {
		if (fileName==null)
			fileName = "";
		String title;
		if (fileName.equals(""))
			title = getResourceString("title.nofile");
		else {
			Object[] args = { fileName };
			String fmt = getResourceString("title.fileName");
			title = MessageFormat.format(fmt,args);
		}
		setTitle(title);
	}

	/** Get a string from our resources. */
	public String getResourceString(String name) {
		return app.getResourceString(name);
	}
}

/* end */
