/* ImageLister.java
 *
 * Jim McBeath, September 18, 2001
 */

package jimmc.jiviewer;

import jimmc.util.FileUtil;
import jimmc.util.MoreException;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.InvalidDnDOperationException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import javax.swing.DefaultListCellRenderer;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;

/** Maintains a list of images and associated information.
 */
public class ImageLister extends JPanel implements ListSelectionListener,
            DragGestureListener {
        private final static int ICON_SIZE = 64;        //TBD - use a preference
                //the size of each icon in the list
        private final static int ICON_LIST_WIDTH=450;
                //width of each list element when showing images and file text

	/** Our App. */
	private App app;

	/** Our parent window. */
	private Viewer viewer;

        private boolean listOnly = true;
        private JSplitPane mainSplitPane;
        private JSplitPane infoSplitPane;
        private JScrollPane listScrollPane;

        private DragSourceListener dsListener;

	/** The image area which displays the image. */
	private ImageWindow imageWindow;

	/** Our list. */
	private JList fileNameList;

        private int listMode;
            public static final int MODE_NAME = 0;
            public static final int MODE_FULL = 1;
            private static final int MODE_MAX = MODE_FULL; //highest legal value

	/** The status area. */
	private JTextArea statusLabel;

	/** The label showing the directory info text. */
	private JTextArea dirTextLabel;

	/** The label showing the file info text. */
	private JTextArea fileTextLabel;

	/** The current directory in which we are displaying files. */
	private File targetDirectory;

	/** The file names we are displaying, within the targetDirectory. */
	private String[] fileNames;
        private FileInfo[] fileInfos;

	/** The currently displayed image. */
	protected ImageBundle currentImage;

	/** The next image in the list. */
	private ImageBundle nextImage;

	/** The previous image in the list. */
	private ImageBundle previousImage;

	/** The image loader thread, used as a lock object for image loader. */
	private Thread imageLoader;

	/** The display updater thread, used as a lock object
	 * for display updater. */
	private Thread displayUpdater;

	/** Create a new list. */
	public ImageLister(App app, Viewer viewer) {
		super();
		this.app = app;
		this.viewer = viewer;

		//statusLabel = new JTextArea("status here");
		//statusLabel.setEditable(false);
                    //status line is now in viewer
		dirTextLabel = new JTextArea("dir info here");
		dirTextLabel.setEditable(false);
		fileTextLabel = new JTextArea("file info here");
		fileTextLabel.setEditable(false);
		infoSplitPane = new JSplitPane(
			JSplitPane.VERTICAL_SPLIT,
			new JScrollPane(dirTextLabel),
			new JScrollPane(fileTextLabel));
		//infoSplitPane.setBackground(Color.black);
		//JSplitPane statusInfoPane = new JSplitPane(
			//JSplitPane.VERTICAL_SPLIT,
			//statusLabel,infoSplitPane);

		fileNameList = new JList();
		fileNameList.addListSelectionListener(this);
		listScrollPane = new JScrollPane(fileNameList);
		listScrollPane.setPreferredSize(new Dimension(600,140));

		mainSplitPane = new JSplitPane(
			JSplitPane.HORIZONTAL_SPLIT,
			listScrollPane,infoSplitPane);
		mainSplitPane.setDividerLocation(200);
                listOnly = false;

		setLayout(new BorderLayout());
		add(mainSplitPane);

		initImageLoader();
		initDisplayUpdater();

                setupDrag();
	}

        //Recognize dragging from the file list
        private void setupDrag() {
            //enable dragging from this list
            DragSource dragSource = DragSource.getDefaultDragSource();
            dragSource.createDefaultDragGestureRecognizer(
                fileNameList,DnDConstants.ACTION_COPY,this);

            dsListener = new DragSourceAdapter() {
                public void dragDropEnd(DragSourceDropEvent ev) {
                    if (imageWindow!=null)
                        imageWindow.requestFocus();
                }
            };
        }

    //The DragGesture interface
        public void dragGestureRecognized(DragGestureEvent ev) {
            int index = fileNameList.getSelectedIndex();
            if (index==-1)
                return;         //no item selected for dragging
            String path = new File(targetDirectory,fileNames[index]).toString();
            try {
                Transferable transferable = new StringSelection(path);
                ev.startDrag(DragSource.DefaultCopyNoDrop,
                        transferable, dsListener);
            } catch (InvalidDnDOperationException ex) {
                System.err.println(ex); //TODO - better error handling
            }
        }
    //End DragGesture interface

        /** Show only the list, not the other status areas. */
        public void showListOnly(boolean t) {
            if (t==listOnly)
                return;
            if (t) {
                //TODO - we should still display the directory info,
                //so "showListOnly" is perhaps not the right thing here
                remove(mainSplitPane);
                add(listScrollPane);
            } else {
                remove(listScrollPane);
                mainSplitPane.setLeftComponent(listScrollPane);
                add(mainSplitPane);
            }
            listOnly = t;
        }

        /** Set the mode for what we display in the image file list. */
        public void setListMode(int mode) {
            if (mode<0 || mode>MODE_MAX)
                throw new IllegalArgumentException("Bad mode");
            if (mode==listMode)
                return;         //no change
            switch (mode) {
            default:
            case MODE_NAME:
                fileNameList.setCellRenderer(new DefaultListCellRenderer());
                fileNameList.setFixedCellWidth(-1);
                fileNameList.setFixedCellHeight(-1);
                break;
            case MODE_FULL:
                fileNameList.setCellRenderer(new FileListRenderer());
                fileNameList.setFixedCellWidth(ICON_LIST_WIDTH);
                fileNameList.setFixedCellHeight(ICON_SIZE);
                    //set fixed cell height and width to prevent the list
                    //from rendering every item immediately
                break;
            }
            listMode = mode;
            //TODO - redisplay the list
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

	/** Set the ImageWindow. */
	public void setImageWindow(ImageWindow imageWindow) {
		this.imageWindow = imageWindow;
                if (currentImage!=null)
                    currentImage.setImageWindow(imageWindow);
                if (nextImage!=null)
                    nextImage.setImageWindow(imageWindow);
                if (previousImage!=null)
                    previousImage.setImageWindow(imageWindow);
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
                imageWindow.advance();
		fileNames = getImageFileNames(targetDirectory);
		Arrays.sort(fileNames,new ImageFileNameComparator());
                fileInfos = new FileInfo[fileNames.length];
                    //Allocate space for the rest of the file info
		if (formerTargetDirectory==null || targetDirectory==null ||
		    !formerTargetDirectory.toString().equals(
				targetDirectory.toString()))
			setDirectoryInfo(targetDirectory);
		fileNameList.setListData(fileNames);
		if (fileNames.length==0) {
			//No files in the list, so don't try to select anything
		} else if (targetFile==null) {
			//No file specified, so select the first file in the dir
			fileNameList.setSelectedIndex(0);
		} else {
			//Find the index of the specified file and select it
			String targetFileName = targetFile.getName();
			int n = Arrays.binarySearch(fileNames,targetFileName);
			if (n<0)
				n = 0;	//if file not found, select first file
			fileNameList.setSelectedIndex(n);
		}
	}

	/** Display new directory info. */
	protected void setDirectoryInfo(File dir) {
		String dirText = "Directory: "+dir.toString();
		try {
			File summaryFile = new File(dir,"summary.txt");
			String dirSummary = FileUtil.readFile(summaryFile);
			if (dirSummary!=null) {
				if (dirSummary.endsWith("\n")) {
					dirSummary = dirSummary.substring(0,
						dirSummary.length()-1);
				}
				dirText += "\nSummary: "+dirSummary;
			}
		} catch (Exception ex) {
			//on error, ignore summary
		}
		dirTextLabel.setText(dirText);
	}

	/** Get the text associated with a file.
	 * @return The info about the image
	 */
	protected String getFileTextInfo(String path, int index) {
		if (path==null) {
			return null;	//no file, so no info
		}
		File f = new File(path);

		//Start the with file name
		String fileTextInfo = "File: "+f.getName(); //TBD i18n

		//Add (N of M)
		int imageCount = fileNameList.getModel().getSize();
		int thisIndex = index+1;
		fileTextInfo += "; "+thisIndex+" of "+imageCount;  //TBD i18n

		//Add file size and date/time    TBD i18n
		long fileSize = f.length();
		String fileSizeStr;
		if (fileSize>1024*1024*10)	//>10M
			fileSizeStr = ""+(fileSize/(1024*1024))+"M";
		else if (fileSize>1024*10)	//>10K
			fileSizeStr = ""+(fileSize/1024)+"K";
		else
			fileSizeStr = ""+fileSize+"B";
		fileTextInfo += "; "+fileSizeStr;
		long modTimeMillis = f.lastModified();
		Date modDate = new Date(modTimeMillis);
		SimpleDateFormat dFmt =
			(SimpleDateFormat)DateFormat.getDateTimeInstance();
		String tzPath = getTimeZoneFileNameForImage(path);
		File tzFile = new File(tzPath);
		if (tzFile.exists()) {
		    try {
		    	//What a hack... the SimpleDateFormat code doesn't
			//do the right time-zone calculations, it uses
			//TimeZone.getRawOffset, which just gets the first
			//offset in the timezone.  We need it to get the
			//offset for the specified time.
			TimeZone tz = new ZoneInfo(tzFile);
			int zOff = tz.getOffset(modTimeMillis);
			SimpleTimeZone stz =	
				new SimpleTimeZone(zOff,tz.getID());
			dFmt.setTimeZone(stz);
			dFmt.applyPattern(dFmt.toPattern()+" zzzz");
		    } catch (IOException ex) {
System.out.println("IOException reading ZoneInfo: "+ex.getMessage());
		    	//do nothing to change timezone or format
		    }
		}
		String dateStr = dFmt.format(modDate);
		fileTextInfo += "; "+dateStr;

		//Add file info text
		String fileText = getFileText(path);
		if (fileText!=null) {
			if (fileText.endsWith("\n")) {
				fileText = fileText.substring(
						0,fileText.length()-1);
			}
			fileTextInfo += "\n"+fileText;
		}
		return fileTextInfo;
	}

	protected void setFileText(String info) {
		//Display the text for the image
		if (info==null)
			info = "";
		fileTextLabel.setText(info);
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
		//statusLabel.setText(status);
                viewer.showStatus(status);
	}

	/** Get the text for the specified file. */
	protected String getFileText(String path) {
		try {
			String textPath = getTextFileNameForImage(path);
			File f = new File(textPath);
			String text = FileUtil.readFile(f);
                        return text;
                } catch (FileNotFoundException ex) {
                        return null;    //OK if the file is not there
		} catch (Exception ex) {
                        System.out.println("Exception reading file "+path+": "+ex.getMessage());
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

	/** Get the name of the timezone file for the image.
	 * @param path The path to the image file.
	 * @return The timezone file name, or null if we can't figure it out.
	 */
	protected String getTimeZoneFileNameForImage(String path) {
		int sl = path.lastIndexOf(File.separator);
		if (sl<0)
			path = "."+File.separator;
		else
			path = path.substring(0,sl+1);
		String tzPath = path+"TZ";
		return tzPath;
	}

    //The ListSelectionListener interface
	/** Here when the list selection changes. */
	public void valueChanged(ListSelectionEvent ev) {
            displayCurrentSelection();
            if (imageWindow!=null)
                imageWindow.requestFocus();
	}
    //End ListSelectionListener interface

	/** Show the currently selected file. */
	public void displayCurrentSelection() {
            //If we are displaying an ImagePage, don't make the
            //selection change the window; user can drag an image
            //from the list into the ImagePage window.
            if (imageWindow instanceof ImagePage)
                return;
            if (imageWindow==null)
                return;
            synchronized (displayUpdater) {
                displayUpdater.notifyAll();  //wake up updater
            }
	}

	/** Set up our images.
	 * @return The path to the current image.
	 */
	protected void setupCurrentImage() {
		int newSelection = fileNameList.getSelectedIndex();
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
                        if (imageWindow!=null)
                            imageWindow.showText("No file");
			currentImage = null;
			return;		//nothing there
		}
		currentImage = new ImageBundle(app,imageWindow,file,newSelection);
	}

	/** Set up the next and previous images. */
	protected void setupAdjacentImages() {
		if (!app.useLookAhead())
			return;		//lookahead disabled
		int currentSelection = (currentImage==null)?
					-1:currentImage.getListIndex();
		int maxIndex = fileNameList.getModel().getSize();
		if (nextImage==null && currentSelection+1<maxIndex) {
			File file = new File(targetDirectory,
					fileNames[currentSelection+1]);
			if (file!=null) {
				nextImage = new ImageBundle(app,imageWindow,
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
				previousImage = new ImageBundle(app,imageWindow,
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
		setFileText(null);	//clear image text while changing
		if (currentImage==null) {
			path = null;
                        if (imageWindow!=null)
                            imageWindow.showText("No image");
		} else {
			path = currentImage.getPath();
                        int index = currentImage.getListIndex();
			String imageTextInfo = getFileTextInfo(path,index);
			setFileText(imageTextInfo);
                        if (imageWindow!=null)
                            imageWindow.showImage(currentImage,imageTextInfo);
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

        /** Move to previous directory */
        public void left() {
                File newDir = getPreviousDirectory(targetDirectory);
                if (newDir==null) {
                        String eMsg = "No previous directory";
                        viewer.errorDialog(eMsg);
                        return;
                }
                File lastFile = getLastFileInDir(newDir);
                if (lastFile!=null)
                        open(lastFile);
                else
                        open(newDir);	//TBD - skip back farther?
        }

        /** Move to next directory */
        public void right() {
                File newDir = getNextDirectory(targetDirectory);
                if (newDir==null) {
                        String eMsg = "No next directory";
                        viewer.errorDialog(eMsg);
                        return;
                }
                open(newDir);	//TBD - skip forward farther if no imgs?
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
		int sel = fileNameList.getSelectedIndex();
		sel += inc;
		int maxIndex = fileNameList.getModel().getSize();
		if (sel<0) {
			String prompt = "At beginning; move to previous dir?";
				//TBD i18n this section
			if (!viewer.confirmDialog(prompt))
				return;		//cancelled
                        //User is trying to move off the beginning of the list,
                        //see about moving back to the previous directory
                        left();
			return;
		}
		if (sel>=maxIndex) {
			String prompt = "At end; move to next dir?";
				//TBD i18n this section
			if (!viewer.confirmDialog(prompt))
				return;		//cancelled
			//User is trying to move off the end of the list,
			//see about moving forward to the next directory
                        right();
			return;
		}
                //imageWindow.advance();
		fileNameList.setSelectedIndex(sel);
		fileNameList.ensureIndexIsVisible(sel);
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
				setCursorBusy(true);
				app.debugMsg("imageLoader load next image");
				nextImage.loadTransformedImage();
				app.debugMsg("imageLoader done next image");
				setCursorBusy(false);
				setStatus("");
			}
			if (previousImage!=null) {
				setStatus("Loading previous image");
				setCursorBusy(true);
				app.debugMsg("imageLoader load prev image");
				previousImage.loadTransformedImage();
				app.debugMsg("imageLoader done prev image");
				setCursorBusy(false);
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
			//Check to see if the list selection changed while
			//we were busy updating.  If so, don't do the wait.
			int newSelection = fileNameList.getSelectedIndex();
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

        //Get the FileInfo for the specified file in the fileNames list
        private FileInfo getFileInfo(int index) {
            FileInfo fileInfo = fileInfos[index];
            if (fileInfo!=null)
                return fileInfo;        //already loaded
            fileInfo = new FileInfo();
            fileInfo.dir = targetDirectory;
            fileInfo.name = fileNames[index];
            fileInfo.text = getFileTextInfo(fileInfo.getPath(),index);
            fileInfo.icon = getFileIcon(fileInfo.getPath());
            fileInfos[index] = fileInfo;
            return fileInfo;
        }

        private ImageIcon getFileIcon(String filename) {
            Toolkit toolkit = getToolkit();
            Image fullImage = toolkit.createImage(filename);
            Image scaledImage = ImageBundle.createScaledImage(fullImage,0,ICON_SIZE,ICON_SIZE);
            return new ImageIcon(scaledImage);
        }
        //final static ImageIcon imgIcon = new ImageIcon("/Users/jmcbeath/home/scclogo.gif");

        class FileInfo {
            File dir;           //the directory containing the file
            String name;        //name of the file within the directory
            String text;        //text for the image, from getFileTextInfo()
            ImageIcon icon;     //icon for the image, or generic icon for other file types

            public String getPath() {
                return new File(dir,name).toString();
            }
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
                int dirIndex=0;
                if (siblings!=null) {
                    Arrays.sort(siblings);
                    String dirName = dir.getName();
                    dirIndex = Arrays.binarySearch(siblings,dirName);
                    if (dirIndex<0) {
                            String msg = "Can't find dir "+dirName+
                                    " in parent list";
                            throw new RuntimeException(msg);
                    }
                }
		int newDirIndex = dirIndex + move;
		while (siblings==null || newDirIndex<0 || newDirIndex>=siblings.length) {
			//We are at the end/start of our sibling directories,
			//so recurse up the directory tree and move the
			//parent to the next directory.
			parentDir = getRelativeDirectory(parentDir,move);
			if (parentDir==null)
				return null;
			siblings = parentDir.list();
			if (siblings==null || siblings.length==0)
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
		if (names==null || names.length==0)
			return null;
		return new File(dir,names[names.length-1]);
	}

        private void setCursorBusy(boolean busy) {
            if (imageWindow==null)
                return;
            imageWindow.setCursorBusy(busy);
        }

        //class FileListRenderer extends JLabel implements ListCellRenderer {
        class FileListRenderer extends DefaultListCellRenderer {
            public Component getListCellRendererComponent(JList list,
                    Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel cell = (JLabel)super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                FileInfo fileInfo = getFileInfo(index);
                String fileInfoText = fileInfo.text;
                String labelText;
                if (fileInfoText==null) {
                    labelText = fileInfo.name;
                } else {
                    fileInfoText = fileInfoText.replaceAll("\\n","<br>");
                    labelText = "<html>"+fileInfoText+"</html>";
                        //label doesn't normally do newlines, so we use html and
                        //<br> tags instead.
                }
                cell.setText(labelText);
                //cell.setVerticalAlignment(TOP);      //put text at top left
                //cell.setHorizontalAlignment(LEFT);
                cell.setIcon(fileInfo.icon);
                /* the rest is handled by superclass...
                if (isSelected) {
                    cell.setBackground(list.getSelectionBackground());
                    cell.setForeground(list.getSelectionForeground());
                } else {
                    cell.setBackground(list.getBackground());
                    cell.setForeground(list.getForeground());
                }
                cell.setEnabled(list.isEnabled());
                cell.setFont(list.getFont());
                */
                return cell;
            }
        }
}

/* end */
