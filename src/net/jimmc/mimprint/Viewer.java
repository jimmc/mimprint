/* Viewer.java
 *
 * Jim McBeath, September 15, 2001
 */

package jimmc.jiviewer;

import jimmc.swing.GridBagger;
import jimmc.swing.JimmcFrame;
import jimmc.swing.MenuAction;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Point;
import java.awt.Rectangle;
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

	/** Our image display area. */
	protected ImageArea imageArea;

	/** True if we are in full-screen-image mode. */
	protected boolean fullImageP;

	/** The screen bounds when in normal mode. */
	protected Rectangle normalBounds;

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
		imageArea = new ImageArea(app,this);
		imageLister.setImageArea(imageArea);
		imageArea.setImageLister(imageLister);
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

	/** Set the image area to take up the full screen, or unset.
	 * @param fullImage true to make the ImageArea take up the full
	 *        screen, false to go back to the non-full-screen.
	 */
	public void setFullScreen(boolean fullImage) {
		if (fullImage==fullImageP)
			return;		//already in that mode
		if (fullImage) {
			//switching from normal mode to full-image mode
			normalBounds = getBounds();
			Rectangle imageAreaBounds = imageArea.getBounds();
			Point viewerRootLocation = getLocationOnScreen();
			Point imageRootLocation=imageArea.getLocationOnScreen();
			Dimension screenSize = getToolkit().getScreenSize();
			int xoff = imageRootLocation.x - viewerRootLocation.x;
			int yoff = imageRootLocation.y - viewerRootLocation.y;
			int woff = normalBounds.width - imageAreaBounds.width;
			int hoff = normalBounds.height - imageAreaBounds.height;
			int x = -xoff;
			int y = -yoff;
			int w = screenSize.width + woff;
			int h = screenSize.height + hoff;;
			setBounds(x,y,w,h);
		} else {
			//Switch back to normal bounds
			setBounds(normalBounds.x, normalBounds.y,
				normalBounds.width, normalBounds.height);
		}
		fullImageP = fullImage;
		validate();
	}

	/** Get a string from our resources. */
	public String getResourceString(String name) {
		return app.getResourceString(name);
	}
}

/* end */
