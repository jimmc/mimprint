/* ImageLister.java
 *
 * Jim McBeath, September 18, 2001
 */

package jimmc.jiviewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
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

	/** The currently displayed image. */
	protected Image currentImage;

	/** The current image, scaled. */
	protected Image currentScaledImage;

	/** The next image in the list. */
	protected Image nextImage;

	/** The next image in the list, scaled. */
	protected Image nextScaledImage;

	/** The previous image in the list. */
	protected Image previousImage;

	/** The previous image in the list, scaled. */
	protected Image previousScaledImage;

	/** The path to the current file. */
	protected String currentPath;

	/** The path to the next file in the list. */
	protected String nextPath;

	/** The path to the previous file in the list. */
	protected String previousPath;

	/** The currently selected index. */
	protected int currentSelection;

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
		currentSelection = -1;
		Thread imageLoader = new Thread() {
			public void run() {
				imageLoaderRun();
			}
		};
		imageLoader.setPriority(imageLoader.getPriority()-2);
		imageLoader.start();
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
		if (imageArea==null)
			return;
		setupCurrentImage();
		displayCurrentImage();
		setupNextImage();
	}

	/** Set up our images.
	 * @return The path to the current image.
	 */
	protected void setupCurrentImage() {
		int newSelection = list.getSelectedIndex();
		if (newSelection==currentSelection)
			return;		//no change, ignore this call

		if (newSelection<0) {
			//Nothing selected
			currentImage = null;
			currentScaledImage = null;
			nextImage = null;
			nextScaledImage = null;
			previousImage = null;
			previousScaledImage = null;
			currentPath = null;
			nextPath = null;
			previousPath = null;
			return;
		}

		//Most common case: user is advancing through the images
		//one at a time.
		if (newSelection == currentSelection+1 && currentSelection>=0) {
			currentSelection = newSelection;
			previousImage = currentImage;
			previousScaledImage = currentScaledImage;
			previousPath = currentPath;
			currentImage = nextImage;
			currentScaledImage = nextScaledImage;
			currentPath = nextPath;
			nextImage = null;
			nextScaledImage = null;
			nextPath = null;
			return;
		}

		//Second common case: user is going backwards through the list.
		if (newSelection == currentSelection-1) {
			currentSelection = newSelection;
			nextImage = currentImage;
			nextScaledImage = currentScaledImage;
			nextPath = currentPath;
			currentImage = previousImage;
			currentScaledImage = previousScaledImage;
			currentPath = previousPath;
			previousImage = null;
			previousScaledImage = null;
			previousPath = null;
			return;
		}

		//Not an adjacent image
		nextImage = null;
		nextScaledImage = null;
		nextPath = null;
		previousImage = null;
		previousScaledImage = null;
		previousPath = null;
		currentSelection = newSelection;

		File file = new File(targetDirectory,
					fileNames[currentSelection]);
		if (file==null) {
			imageArea.showText("No file");
			currentImage = null;
			return;		//nothing there
		}
		currentPath = file.getAbsolutePath();
		currentImage = getToolkit().createImage(currentPath);
		currentScaledImage = imageArea.getScaledImage(currentImage);
	}

	/** Set up the next and previous images. */
	protected void setupNextImage() {
		int maxIndex = list.getModel().getSize();
		if (nextImage==null && currentSelection+1<maxIndex) {
			File file = new File(targetDirectory,
					fileNames[currentSelection+1]);
			if (file!=null) {
				nextPath = file.getAbsolutePath();
				nextImage = getToolkit().createImage(nextPath);
				synchronized(this) {
					notifyAll();	//start imageLoader
				}
			}
		}
		if (previousImage==null && currentSelection-1>=0) {
			File file = new File(targetDirectory,
					fileNames[currentSelection-1]);
			if (file!=null) {
				previousPath = file.getAbsolutePath();
				previousImage = getToolkit().createImage(
							previousPath);
				synchronized(this) {
					notifyAll();	//start imageLoader
				}
			}
		}
	}

	/** Display the current image. */
	protected void displayCurrentImage() {
		if (currentScaledImage!=null)
			imageArea.showImage(currentScaledImage);
		else
			imageArea.showImage(currentImage);
		viewer.setTitleFileName(currentPath);
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

	/** The image loader thread, which loads images in the background. */
	public void imageLoaderRun() {
		while (true) {
			synchronized(this) {
				try {
					wait();
					Thread.sleep(100);
				} catch (InterruptedException ex) {
					//ignore
				}
			}
			if (nextImage!=null) {
				imageArea.loadCompleteImage(nextImage);
				Image im = imageArea.getScaledImage(nextImage);
				imageArea.loadCompleteImage(im);
				nextScaledImage = im;
			}
			if (previousImage!=null) {
				imageArea.loadCompleteImage(previousImage);
				Image im = imageArea.getScaledImage(previousImage);
				imageArea.loadCompleteImage(im);
				previousScaledImage = im;
			}
		}
	}
}

/* end */
