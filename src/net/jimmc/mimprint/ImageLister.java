/* ImageLister.java
 *
 * Jim McBeath, September 18, 2001
 */

package jimmc.jiviewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/** Maintains a list of images and associated information.
 */
public class ImageLister extends JPanel implements ListSelectionListener {
	/** Our App. */
	protected App app;

	/** Our parent window. */
	protected Viewer viewer;

	/** The image area which displays the image. */
	protected ImageArea imageArea;

	/** Our list. */
	protected JList list;

	/** The current directory in which we are displaying files. */
	protected File targetDirectory;

	/** The file names we are displaying, within the targetDirectory. */
	protected String[] fileNames;

	/** Create a new list. */
	public ImageLister(App app, Viewer viewer) {
		super();
		this.app = app;
		this.viewer = viewer;
		list = new JList();
		list.addListSelectionListener(this);
		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setPreferredSize(new Dimension(600,100));
		setLayout(new BorderLayout());
		add(scrollPane);
	}

	/** Set the ImageArea. */
	public void setImageArea(ImageArea imageArea) {
		this.imageArea = imageArea;
	}

	/** Open the specified target.
	 * @param target The file or directory to open.
	 * @see #open(File)
	 */
	public void open(String target) {
		open(new File(target));
	}

	/** Open the specified target.
	 * If it is a directory, list all of the image files in the directory.
	 * If it is a file, list all of the image files in the containing
	 * directory, and selected the given file.
	 * @param targetFile The file or directory to open.
	 */
	public void open(File targetFile) {
		if (!targetFile.exists()) {
			Object[] eArgs = { targetFile.getName() };
			String msg = app.getResourceFormattedString(
					"error.NoSuchFile",eArgs);
			viewer.errorDialog(msg);
			return;
		}
		if (targetFile.isDirectory()) {
			//It's a directory, use it
			targetDirectory = targetFile;
		} else {
			//It's not a directory, get the directory from it
			targetDirectory = targetFile.getParentFile();
		}
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return isImageFileName(name);
			}
		};
		fileNames = targetDirectory.list(filter);
		Arrays.sort(fileNames,new ImageFileNameComparator());
		//TBD - look up file dates, sizes, and associated text
		list.setListData(fileNames);
	}

	/** True if the file name is for an image file that we recognize. */
	public boolean isImageFileName(String name) {
		int dotPos = name.lastIndexOf('.');
		if (dotPos<0)
			return false;	//no extension
		String extension = name.substring(dotPos+1).toLowerCase();
		if (extension.equals("gif") ||
		    extension.equals("jpg") ||
		    extension.equals("jpeg"))
			return true;
		return false;
	}

	/** Here when the list selection changes. */
	public void valueChanged(ListSelectionEvent ev) {
		displayCurrentSelection();
	}

	/** Show the currently selected file. */
	public void displayCurrentSelection() {
		int sel = list.getSelectedIndex();
		File file;
		String path;
		if (sel<0) {
			//Nothing selected
			file = null;
			path = null;
		} else {
			file = new File(targetDirectory,fileNames[sel]);
			path = file.getAbsolutePath();
		}
		if (imageArea!=null) {
			imageArea.showFile(file);
			viewer.setTitleFileName(path);
		}
	}

	/** Move the selection up one item and show that file. */
	public void up() {
		move(-1);
	}

	/** Move the selection down one item and show that file. */
	public void down() {
		move(1);
	}

	/** Move the selection by the specified amount and show that file. */
	public void move(int inc) {
		int sel = list.getSelectedIndex();
		sel += inc;
		int maxIndex = list.getModel().getSize();
		if (sel<0 || sel>=maxIndex) {
			//New selection value is out of range, ignore it
			getToolkit().beep();
			return;
		}
		list.setSelectedIndex(sel);
		list.ensureIndexIsVisible(sel);
		displayCurrentSelection();
	}
}

/* end */
