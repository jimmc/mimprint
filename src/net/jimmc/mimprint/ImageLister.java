/* ImageLister.java
 *
 * Jim McBeath, September 18, 2001
 */

package jimmc.jiviewer;

import jimmc.util.FileUtil;
import jimmc.util.MoreException;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

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

	/** The status area. */
	protected JTextArea statusLabel;

	/** The label showing the directory info. */
	protected JTextArea dirInfoLabel;

	/** The label showing the file info. */
	protected JTextArea fileInfoLabel;

	/** The current directory in which we are displaying files. */
	protected File targetDirectory;

	/** The file names we are displaying, within the targetDirectory. */
	protected String[] fileNames;

	/** The currently displayed image. */
	protected ImageBundle currentImage;

	/** The next image in the list. */
	protected ImageBundle nextImage;

	/** The previous image in the list. */
	protected ImageBundle previousImage;

	/** The image loader thread, used as a lock object for image loader. */
	protected Thread imageLoader;

	/** The display updater thread, used as a lock object
	 * for display updater. */
	protected Thread displayUpdater;

	/** Create a new list. */
	public ImageLister(App app, Viewer viewer) {
		super();
		this.app = app;
		this.viewer = viewer;

		statusLabel = new JTextArea("status here");
		statusLabel.setEditable(false);
		dirInfoLabel = new JTextArea("dir info here");
		dirInfoLabel.setEditable(false);
		fileInfoLabel = new JTextArea("file info here");
		fileInfoLabel.setEditable(false);
		JSplitPane infoSplitPane = new JSplitPane(
			JSplitPane.VERTICAL_SPLIT,
			new JScrollPane(dirInfoLabel),
			new JScrollPane(fileInfoLabel));
		//infoSplitPane.setBackground(Color.black);
		JSplitPane statusInfoPane = new JSplitPane(
			JSplitPane.VERTICAL_SPLIT,
			statusLabel,infoSplitPane);

		list = new JList();
		list.addListSelectionListener(this);
		JScrollPane listScrollPane = new JScrollPane(list);
		listScrollPane.setPreferredSize(new Dimension(600,140));

		JSplitPane splitPane = new JSplitPane(
			JSplitPane.HORIZONTAL_SPLIT,
			listScrollPane,statusInfoPane);
		splitPane.setDividerLocation(200);

		setLayout(new BorderLayout());
		add(splitPane);

		initImageLoader();
		initDisplayUpdater();
	}

	/** Initialize our image loader thread. */
	protected void initImageLoader() {
		imageLoader = new Thread() {
			public void run() {
				imageLoaderRun();
			}
		};
		imageLoader.setPriority(imageLoader.getPriority()-2);
		imageLoader.start();
		app.debugMsg("image loader thread started");
	}

	/** Initialize a thread to update our display. */
	protected void initDisplayUpdater() {
		displayUpdater = new Thread() {
			public void run() {
				displayUpdaterRun();
			}
		};
		displayUpdater.setPriority(imageLoader.getPriority()-1);
		displayUpdater.start();
		app.debugMsg("display updater thread started");
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
	 * If it is a directory, list all of the image files in the directory
	 * and select the first one.
	 * If it is a file, list all of the image files in the containing
	 * directory, and selected the given file.
	 * @param targetFile The file or directory to open.
	 */
	public void open(File targetFile) {
		if (!targetFile.exists()) {
			Object[] eArgs = { targetFile.getName() };
			String msg = app.getResourceFormatted(
					"error.NoSuchFile",eArgs);
			viewer.errorDialog(msg);
			return;
		}
		try {
			targetFile = new File(targetFile.getCanonicalPath());
			//Clean up the path,
			//to avoid problems when attempting to traverse dirs.
		} catch (IOException ex) {
			throw new MoreException(ex);
		}
		File formerTargetDirectory = targetDirectory;
		if (targetFile.isDirectory()) {
			//It's a directory, use it
			targetDirectory = targetFile;
			targetFile = null;	//get the real file later
		} else {
			//It's not a directory, get the directory from it
			targetDirectory = targetFile.getParentFile();
			if (targetDirectory==null) {
				//No parent, so the file must not name a
				//directory, so the directory must be "."
				targetDirectory = new File(".");
			}
		}
		currentImage = null;
		nextImage = null;
		previousImage = null;
		fileNames = getImageFileNames(targetDirectory);
		Arrays.sort(fileNames,new ImageFileNameComparator());
		//TBD - look up file dates, sizes, and associated text
		if (formerTargetDirectory==null || targetDirectory==null ||
		    !formerTargetDirectory.toString().equals(
				targetDirectory.toString()))
			setDirectoryInfo(targetDirectory);
		list.setListData(fileNames);
		if (fileNames.length==0) {
			//No files in the list, so don't try to select anything
		} else if (targetFile==null) {
			//No file specified, so select the first file in the dir
			list.setSelectedIndex(0);
		} else {
			//Find the index of the specified file and select it
			String targetFileName = targetFile.getName();
			int n = Arrays.binarySearch(fileNames,targetFileName);
			if (n<0)
				n = 0;	//if file not found, select first file
			list.setSelectedIndex(n);
		}
	}

	/** Display new directory info. */
	protected void setDirectoryInfo(File dir) {
		String dirInfo = "Directory: "+dir.toString();
		try {
			File summaryFile = new File(dir,"summary.txt");
			String dirSummary = FileUtil.readFile(summaryFile);
			if (dirSummary!=null) {
				if (dirSummary.endsWith("\n")) {
					dirSummary = dirSummary.substring(0,
						dirSummary.length()-1);
				}
				dirInfo += "\nSummary: "+dirSummary;
			}
		} catch (Exception ex) {
			//on error, ignore summary
		}
		dirInfoLabel.setText(dirInfo);
	}

	/** Get the text associated with a file.
	 * @return The info about the image
	 */
	protected String getFileInfo(String path) {
		if (path==null) {
			return null;	//no file, so no info
		}
		File f = new File(path);

		//Start the with file name
		String fileInfo = "File: "+f.getName(); //TBD i18n

		//Add (N of M)
		int imageCount = list.getModel().getSize();
		int thisIndex = currentImage.getListIndex()+1;
		fileInfo += "; "+thisIndex+" of "+imageCount;  //TBD i18n

		//Add file size and date/time    TBD i18n
		long fileSize = f.length();
		String fileSizeStr;
		if (fileSize>1024*1024*10)	//>10M
			fileSizeStr = ""+(fileSize/(1024*1024))+"M";
		else if (fileSize>1024*10)	//>10K
			fileSizeStr = ""+(fileSize/1024)+"K";
		else
			fileSizeStr = ""+fileSize+"B";
		fileInfo += "; "+fileSizeStr;
		long modTime = f.lastModified();
		fileInfo += "; "+(new Date(modTime)).toString();

		//Add file info text
		String fileText = getFileText(path);
		if (fileText!=null) {
			if (fileText.endsWith("\n")) {
				fileText = fileText.substring(
						0,fileText.length()-1);
			}
			fileInfo += "\n"+fileText;
		}
		return fileInfo;
	}

	protected void setFileInfo(String info) {
		//Display the info
		if (info==null)
			info = "";
		fileInfoLabel.setText(info);
	}

	/** Write new text associated with a file.
	 * @param path The path to the image file.
	 * @param text The text about that image.
	 */
	protected void writeFileText(String path, String text) {
		if (path==null) {
			return;	//no file, so no info
		}
		if (text.length()>0 && !text.endsWith("\n"))
			text = text + "\n";	//terminate with a newline
		try {
			String textPath = getTextFileNameForImage(path);
			File f = new File(textPath);
			FileUtil.writeFile(f,text);
		} catch (Exception ex) {
			throw new RuntimeException(ex);  //TBD more info
		}
		displayCurrentImage();	//refresh image text
	}

	/** Set the contents of the status area. */
	public void setStatus(String status) {
		statusLabel.setText(status);
	}

	/** Get the text for the specified file. */
	protected String getFileText(String path) {
		try {
			String textPath = getTextFileNameForImage(path);
			File f = new File(textPath);
			return FileUtil.readFile(f);
		} catch (Exception ex) {
			return null;	//on any error, ignore the file
		}
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

	/** Get the name of the text file which contains the info
	 * about the specified image file.
	 * @param path The path to the image file.
	 * @return The text file name, or null if we can't figure it out.
	 */
	protected String getTextFileNameForImage(String path) {
		int dot = path.lastIndexOf('.');
		if (dot<0)
			return null;
		String textPath = path.substring(0,dot+1)+"txt";
		return textPath;
	}

    //The ListSelectionListener interface
	/** Here when the list selection changes. */
	public void valueChanged(ListSelectionEvent ev) {
		displayCurrentSelection();
	}
    //End ListSelectionListener interface

	/** Show the currently selected file. */
	public void displayCurrentSelection() {
		if (imageArea==null)
			return;
		synchronized (displayUpdater) {
			displayUpdater.notifyAll();  //wake up updater
		}
	}

	/** Set up our images.
	 * @return The path to the current image.
	 */
	protected void setupCurrentImage() {
		int newSelection = list.getSelectedIndex();
		int currentSelection = (currentImage==null)?
					-1:currentImage.getListIndex();
		if (newSelection==currentSelection)
			return;		//no change, ignore this call

		if (newSelection<0) {
			//Nothing selected
			currentImage = null;
			nextImage = null;
			previousImage = null;
			return;
		}

		//Most common case: user is advancing through the images
		//one at a time.
		if (newSelection == currentSelection+1 && currentSelection>=0) {
			previousImage = currentImage;
			currentImage = nextImage;
			nextImage = null;
			return;
		}

		//Second common case: user is going backwards through the list.
		if (newSelection == currentSelection-1) {
			nextImage = currentImage;
			currentImage = previousImage;
			previousImage = null;
			return;
		}

		//Not an adjacent image
		nextImage = null;
		previousImage = null;

		File file = new File(targetDirectory,fileNames[newSelection]);
		if (file==null) {
			imageArea.showText("No file");
			currentImage = null;
			return;		//nothing there
		}
		currentImage = new ImageBundle(imageArea,file,newSelection);
	}

	/** Set up the next and previous images. */
	protected void setupAdjacentImages() {
		if (!app.useLookAhead())
			return;		//lookahead disabled
		int currentSelection = (currentImage==null)?
					-1:currentImage.getListIndex();
		int maxIndex = list.getModel().getSize();
		if (nextImage==null && currentSelection+1<maxIndex) {
			File file = new File(targetDirectory,
					fileNames[currentSelection+1]);
			if (file!=null) {
				nextImage = new ImageBundle(imageArea,
					file,currentSelection+1);
				synchronized(imageLoader) {
					imageLoader.notifyAll();
						//start imageLoader
				}
			}
		}
		if (previousImage==null && currentSelection-1>=0) {
			File file = new File(targetDirectory,
					fileNames[currentSelection-1]);
			if (file!=null) {
				previousImage = new ImageBundle(imageArea,
					file, currentSelection-1);
				synchronized(imageLoader) {
					imageLoader.notifyAll();
						//start imageLoader
				}
			}
		}
	}

	/** Display the current image. */
	protected void displayCurrentImage() {
		String path;
		setFileInfo(null);	//clear info while changing
		if (currentImage==null) {
			path = null;
			imageArea.showText("No image");
		} else {
			path = currentImage.getPath();
			String imageInfo = getFileInfo(path);
			setFileInfo(imageInfo);
			imageArea.showImage(currentImage,imageInfo);
		}
		viewer.setTitleFileName(path);
	}

	protected String getCurrentImageFileText() {
		String path = currentImage.getPath();
		return getFileText(path);
	}

	protected void setCurrentImageFileText(String imageText) {
		String path = currentImage.getPath();
		writeFileText(path,imageText);
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
		if (sel<0) {
			String prompt = "At beginning; move to previous dir?";
				//TBD i18n this section
			if (!viewer.confirmDialog(prompt))
				return;		//cancelled
			//User is trying to move off the beginning of the list,
			//see about moving back to the previous directory
			File newDir = getPreviousDirectory(targetDirectory);
			if (newDir==null) {
				String eMsg = "No previous directory";
				viewer.errorDialog(eMsg);
				return;
			}
			//TBD - get last file in that directory
			File lastFile = getLastFileInDir(newDir);
			if (lastFile!=null)
				open(lastFile);
			else
				open(newDir);	//TBD - skip back farther?
			return;
		}
		if (sel>=maxIndex) {
			String prompt = "At end; move to next dir?";
				//TBD i18n this section
			if (!viewer.confirmDialog(prompt))
				return;		//cancelled
			//User is trying to move off the end of the list,
			//see about moving forward to the next directory
			File newDir = getNextDirectory(targetDirectory);
			if (newDir==null) {
				String eMsg = "No next directory";
				viewer.errorDialog(eMsg);
				return;
			}
			open(newDir);	//TBD - skip forward farther if no imgs?
			return;
		}
		list.setSelectedIndex(sel);
		list.ensureIndexIsVisible(sel);
		displayCurrentSelection();
	}

	/** The image loader thread, which loads images in the background. */
	public void imageLoaderRun() {
		app.debugMsg("image loader thread running");
		while (true) {
			synchronized(imageLoader) {
				try {
					app.debugMsg(
						"image loader thread waiting");
					imageLoader.wait();
				} catch (InterruptedException ex) {
					//ignore
				}
			}
			try {
				//Do this outside of the sync
				Thread.sleep(100);
			} catch (InterruptedException ex) {
				//ignore
			}
			app.debugMsg("image loader thread awakened");
			if (nextImage!=null) {
				setStatus("Loading next image");
				imageArea.setCursorBusy(true);
				app.debugMsg("imageLoader load next image");
				nextImage.loadTransformedImage();
				app.debugMsg("imageLoader done next image");
				imageArea.setCursorBusy(false);
				setStatus("");
			}
			if (previousImage!=null) {
				setStatus("Loading previous image");
				imageArea.setCursorBusy(true);
				app.debugMsg("imageLoader load prev image");
				previousImage.loadTransformedImage();
				app.debugMsg("imageLoader done prev image");
				imageArea.setCursorBusy(false);
				setStatus("");
			}
		}
	}

	/** The display updater thread, which handles requests to update the
	 * display so that the Event thread will be freed up.
	 */
	public void displayUpdaterRun() {
		app.debugMsg("display updater thread running");
		while (true) {
			//Check to see if the list selection changed will
			//we were busy updating.  If so, don't do the wait.
			int newSelection = list.getSelectedIndex();
			int currentSelection = (currentImage==null)?
						-1:currentImage.getListIndex();
			if (newSelection==currentSelection) {
			    //selection is correct, wait for a notify
			    synchronized(displayUpdater) {
				try {
					app.debugMsg(
					    "display updater thread waiting");
					displayUpdater.wait();
				} catch (InterruptedException ex) {
					//ignore
				}
				app.debugMsg("display updater thread awakened");
			    }
			} else {
				app.debugMsg("display updater thread no wait");
			}
			//Update the display
			setupCurrentImage();
			displayCurrentImage();
			setupAdjacentImages();
		}
	}

	protected String[] getImageFileNames(File dir) {
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return isImageFileName(name);
			}
		};
		return dir.list(filter);
	}

	/** Given a directory, get the next sibling directory. */
	protected File getNextDirectory(File dir) {
		return getRelativeDirectory(dir,1);
	}

	protected File getPreviousDirectory(File dir) {
		return getRelativeDirectory(dir,-1);
	}

	protected File getRelativeDirectory(File dir, int move) {
		File parentDir = dir.getParentFile();
		if (parentDir==null)
			parentDir = new File(".");
		String[] siblings = parentDir.list();
		Arrays.sort(siblings);
		String dirName = dir.getName();
		int dirIndex = Arrays.binarySearch(siblings,dirName);
		if (dirIndex<0) {
			String msg = "Can't find dir "+dirName+
				" in parent list";
			throw new RuntimeException(msg);
		}
		int newDirIndex = dirIndex + move;
		while (newDirIndex<0 || newDirIndex>=siblings.length) {
			//We are at the end/start of our sibling directories,
			//so recurse up the directory tree and move the
			//parent to the next directory.
			parentDir = getRelativeDirectory(parentDir,move);
			if (parentDir==null)
				return null;
			siblings = parentDir.list();
			if (siblings.length==0)
				continue;	//no files, try next dir
			Arrays.sort(siblings);
			if (newDirIndex<0)	//backing up
				newDirIndex = siblings.length-1;
			else
				newDirIndex = 0;
		}
		return new File(parentDir,siblings[newDirIndex]);
	}

	/** Given a directory, get the last image file in that directory. */
	protected File getLastFileInDir(File dir) {
		String[] names = getImageFileNames(dir);
		if (names.length==0)
			return null;
		return new File(dir,names[names.length-1]);
	}
}

/* end */
