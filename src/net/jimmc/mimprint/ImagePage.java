/* ImagePage.java
 *
 * Jim McBeath, October 7, 2005
 */

package net.jimmc.jiviewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.awt.Toolkit;
import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import javax.swing.JComponent;

/** A page to display a collection of images in different
 * areas on the page.
 */
public class ImagePage extends JComponent 
        implements ImageWindow, Printable {

    private Viewer viewer;
    private ImagePageControls controls;

    private PageLayout pageLayout;      //our layout

    private ImagePageArea currentArea;  //the active area
    private AreaLayout highlightedArea;   //the highlight area for drop
    private ImagePageArea dragArea;          //source area for drag

    private Color pageColor;    //color of the "paper"
    private boolean knownKeyPress;
    private String imageInfoText;

    //drag-and-drop stuff
    private DragSource dragSource;
    private DragGestureListener dgListener;
    private DragSourceListener dsListener;
    private DropTarget dropTarget;
    private DropTargetListener dtListener;
    private int dropActions = DnDConstants.ACTION_COPY_OR_MOVE;

    private Cursor busyCursor;
    private Cursor invisibleCursor;
    private boolean cursorBusy;
    private boolean cursorVisible;

    private boolean showOutlines;

    /** Create an ImagePage with no images or layout. */
    public ImagePage(Viewer viewer) {
        super();
        this.viewer = viewer;
        pageLayout = new PageLayout(viewer.getApp());
        setBackground(Color.gray);      //neutral background
        setForeground(Color.black);     //black text
        setPageColor(Color.white);      //white "paper"
        setPreferredSize(new Dimension(425,550));
        addKeyListener(new ImagePageKeyListener());
        addMouseListener(new ImagePageMouseListener());
        addMouseMotionListener(new ImagePageMouseMotionListener());
        addComponentListener(new ImagePageComponentListener());
        initCursors();
        setDefaultLayout();
        setupDrag();
        setShowOutlines(true);
    }

    private void initCursors() {
        Toolkit toolkit = getToolkit();
        Image cursorImage = toolkit.createImage(new byte[0]);
        invisibleCursor = toolkit.createCustomCursor(
                cursorImage,new Point(0,0),"invisible");
        busyCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    }

    public void setPageLayout(PageLayout pageLayout) {
        this.pageLayout = pageLayout;
        setHighlightedArea(null);
        currentArea = null;
        repaint();
        controls.updateAllAreasList();
        controls.selectArea(new Point(0,0));    //reset page control bar
    }

    public PageLayout getPageLayout() {
        return pageLayout;
    }

    public void setControls(ImagePageControls controls) {
        this.controls = controls;
    }

    public void writeLayoutTemplate(PrintWriter pw) {
        pageLayout.writeLayoutTemplate(pw);
    }

    /** Set the page units.
     * @param unit One of PageLayout.UNIT_CM or PageLayout.UNIT_INCH.
     */
    public void setPageUnit(int unit) {
        pageLayout.setPageUnit(unit);
    }

    /** Get the current page units, either PageLayout.UNIT_CM or PageLayout.UNIT_INCH. */
    public int getPageUnit() {
        return pageLayout.getPageUnit();
    }

    public void setPageWidth(int width) {
        pageLayout.setPageWidth(width);
    }

    public int getPageWidth() {
        return pageLayout.getPageWidth();
    }

    public void setPageHeight(int height) {
        pageLayout.setPageHeight(height);
    }

    public int getPageHeight() {
        return pageLayout.getPageHeight();
    }

    protected void setAreaLayout(AreaLayout areaLayout) {
        pageLayout.setAreaLayout(areaLayout);
    }

    protected AreaLayout getAreaLayout() {
        return pageLayout.getAreaLayout();
    }

    public void setShowOutlines(boolean show) {
        this.showOutlines = show;
    }

    public boolean isShowOutlines() {
        return showOutlines;
    }

 //Drag-and-drop stuff
    private void setupDrag() {
        //enabled dragging from this component
        dragSource = DragSource.getDefaultDragSource();
        dgListener = new ImagePageDragGestureListener();
        dsListener = new ImagePageDragSourceListener();
        dragSource.createDefaultDragGestureRecognizer(
            this,DnDConstants.ACTION_COPY_OR_MOVE, dgListener);

        //enable dropping into this component
        dtListener = new DTListener();
        dropTarget = new DropTarget(this,dropActions,
                dtListener, true);
    }

  class ImagePageDragGestureListener implements DragGestureListener {
  //The DragGestureListener interface
    public void dragGestureRecognized(DragGestureEvent ev) {
        Point p = ev.getDragOrigin();
        ImagePageArea a = windowToImageArea(p);
        dragArea = a;
        if (a==null)
            return;     //not in an area, can't start a drag here
        String path = a.getImagePath();
        if (path==null)
            return;     //no image in this area, can't start a drag
        Image image = null;     //image to drag
        Image fullImage;
        if (DragSource.isDragImageSupported())
            fullImage = a.getImage();
        else
            fullImage = null;   //image dragging not supported
        Point offset = null;
        if (fullImage!=null) {
            ImageUtil imageUtil = viewer.getApp().getImageUtil();
            image = imageUtil.createTransparentIconImage(fullImage,path);
            imageUtil.loadCompleteImage(image);
            int width = image.getWidth(null);
            int height = image.getHeight(null);
            offset = new Point(-width/2, -height/2);
        }
        try {
            Transferable transferable = new StringSelection(path);
            if (image!=null) {
                ev.startDrag(DragSource.DefaultCopyNoDrop,
                        image, offset,
                        transferable, dsListener);
            } else {
                ev.startDrag(DragSource.DefaultCopyNoDrop,
                        transferable, dsListener);
            }
            //dragSource.startDrag(ev, DragSource.DefaultCopyNoDrop,
                    //transferable, dsListener);
        } catch (InvalidDnDOperationException ex) {
            System.err.println(ex);     //TODO - better error handling
        }
    }
  //End DragGestureListener interface
  }

    private void setDragCursor(DragSourceDragEvent ev) {
        DragSourceContext ctx = ev.getDragSourceContext();
        int action = ev.getDropAction();
        ctx.setCursor(null);
        if ((action & DnDConstants.ACTION_COPY)!=0) {
            //System.out.println("cursor Copy");
            ctx.setCursor(DragSource.DefaultCopyDrop);
            //TODO - we are getting here, but the cursor does not change.
        } else if ((action & DnDConstants.ACTION_MOVE)!=0) {
            //System.out.println("cursor Move");
            ctx.setCursor(DragSource.DefaultMoveDrop);
        } else {
            //System.out.println("cursor NoCopy");
            ctx.setCursor(DragSource.DefaultCopyNoDrop);
            //TODO - or MoveNoDrop?
        }
    }

  class ImagePageDragSourceListener implements DragSourceListener {
  //The DragSourceListener interface
    public void dragEnter(DragSourceDragEvent ev) {
        //System.out.println("dragEnter");
        setDragCursor(ev);
    }
    public void dragOver(DragSourceDragEvent ev) {
        //System.out.println("dragOver");
        setDragCursor(ev);
    }
    public void dragExit(DragSourceEvent ev) {
        //System.out.println("DragSourceListener dragExit");
    }
    public void dragDropEnd(DragSourceDropEvent ev) {
        dragArea = null;
        setHighlightedArea(null);
        if (!ev.getDropSuccess()) {
            System.out.println("DragSource DragDropEnd drop failed");
            return;
        }
        int dropAction = ev.getDropAction();
        if (dropAction==DnDConstants.ACTION_COPY)
            System.out.println("DragSource DragDropEnd Copy");
        else if (dropAction==DnDConstants.ACTION_MOVE)
            System.out.println("DragSource DragDropEnd Move");
        else
            System.out.println("DragDropEnd no action");
        //TODO - need to do anything here to handle the drag end?
    }
    public void dropActionChanged(DragSourceDragEvent ev) { }
  //End DragSourceListener interface
  }

    class DTListener implements DropTargetListener {
        private void checkDrop(DropTargetDragEvent ev) {
            //printFlavors(ev);       //for debug
            ImagePageArea a = getDropArea(ev);
            if (a==null || a==dragArea) {
                //No drop in source area or outside any area
                setHighlightedArea(null);
                //System.out.println("reject drag");
                ev.rejectDrag();
                return;
            }
            setHighlightedArea(a);
            //System.out.println("accept drag");
            ev.acceptDrag(ImagePage.this.dropActions);
        }
      //The DropTargetListener interface
        public void dragEnter(DropTargetDragEvent ev) {
            checkDrop(ev);
        }
        public void dragOver(DropTargetDragEvent ev) {
            checkDrop(ev);
        }
        public void dropActionChanged(DropTargetDragEvent ev) {
            checkDrop(ev);
        }
        public void dragExit(DropTargetEvent ev) {
            setHighlightedArea(null);
            //System.out.println("DropTargetListener dragExit");
        }

        public void drop(DropTargetDropEvent ev) {
            System.out.println("dropped");
            DataFlavor[] flavors = getDropFlavors();
            DataFlavor chosenFlavor = null;
            for (int i=0; i<flavors.length; i++) {
                if (ev.isDataFlavorSupported(flavors[i])) {
                    chosenFlavor = flavors[i];
                    break;
                }
            }
            if (chosenFlavor==null) {
                ev.dropComplete(false);
                return;       //no support for any flavors
            }

            int sourceActions = ev.getSourceActions();
            if ((sourceActions & ImagePage.this.dropActions)==0) {
                ev.dropComplete(false);
                return;       //no actions available
            }

            ImagePageArea dropArea = windowToImageArea(ev.getLocation());
            if (dropArea==null) {
                ev.dropComplete(false); //bad area
                return;       //no actions available
            }

            Object data = null;
            try {
                ev.acceptDrop(ImagePage.this.dropActions);
                data = ev.getTransferable().getTransferData(chosenFlavor);
                if (data==null) {
                    throw new IllegalArgumentException("No drop data");
                }
            } catch (Exception ex) {
                System.out.println("Exception accepting drop: "+ex);
                ev.dropComplete(false);
                return;
            }

            if (data instanceof String) {
                String s = (String)data;
                if (dropFileName(s,dropArea)) {
                    ev.dropComplete(true);
                    return;     //success
                }
            } else if (data instanceof List) {
                List fList = (List)data;        //list of file names
                if (fList.size()>=1) {
                    Object list0 = fList.get(0);
                    if (list0 instanceof String) {
                        if (dropFileName((String)list0,dropArea)) {
                            ev.dropComplete(true);
                            return;
                        }
                    } else if (list0 instanceof File) {
                        File f = (File)list0;
                        if (dropFileName(f.toString(),dropArea)) {
                            ev.dropComplete(true);
                            return;
                        }
                    } else {
                        System.out.println("List item 0 is not a string");
                        System.out.println("List item 0 class is "+list0.getClass().getName());
                        System.out.println("List item 0 data is "+list0.toString());
                    }
                } else {
                    System.out.println("List is empty");
                }
            } else {
                //can't deal with this yet
                System.out.println("drop data class is "+data.getClass().getName());
                System.out.println("drop data is "+data.toString());
            }
            //not processed
            System.out.println("Rejecting drop");
            ev.dropComplete(false);
        }
      //End DropTargetListener interface
    }

    /** Drop a file into the currently selected area.
     * @param s The full path to the image file.
     * @return True if the file exists, false if not.
     */
    private boolean dropFileName(String s, ImagePageArea dropArea) {
        System.out.println("Got drop data: "+s);
	if (s.startsWith("file://"))
	    s = s.substring("file://".length()); //convert URL to file path
	while ((s.endsWith("\n"))||s.endsWith("\r"))
	    s = s.substring(0,s.length()-1); //drop trailing newline
        File f = new File(s);
        if (!f.exists()) {
            System.out.println("No such file '"+s+"'");
            return false;
        }
        if (f.isDirectory()) {
            System.out.println(s+" is a directory");
            return false;
        }
        ImageBundle b = new ImageBundle(viewer.getApp(),ImagePage.this,f,-1);
        currentArea = dropArea;
        showImage(b,null);
        System.out.println("Accepted drop of file "+f);
        //TODO - add call to viewer.setStatus, but need to figure out when
        //to clear the status first
        return true;
    }

    /** Get the flavors we support for drop. */
    private DataFlavor[] getDropFlavors() {
        DataFlavor[] flavors = {
            DataFlavor.stringFlavor,
            DataFlavor.plainTextFlavor,
            DataFlavor.javaFileListFlavor
        };
        return flavors;
    }

    private ImagePageArea getDropArea(DropTargetDragEvent ev) {
        DataFlavor[] flavors = getDropFlavors();
        DataFlavor chosenFlavor = null;
        for (int i=0; i<flavors.length; i++) {
            if (ev.isDataFlavorSupported(flavors[i])) {
                chosenFlavor = flavors[i];
                break;
            }
        }
        if (chosenFlavor==null) {
            System.out.println("No supported flavor");
            return null;       //no support for any flavors
        }

        int sourceActions = ev.getSourceActions();
        if ((sourceActions & ImagePage.this.dropActions)==0) {
            System.out.println("Action not supported "+sourceActions);
            return null;       //no actions available
        }

        Point p = ev.getLocation();
        ImagePageArea a = windowToImageArea(p);
        //System.out.println("area "+a);
        return a;               //null if no area
    }

    private void printFlavors(DropTargetDragEvent ev) {
        DataFlavor[] flavors = ev.getCurrentDataFlavors();
        for (int i=0; i<flavors.length; i++) {
            DataFlavor flavor = flavors[i];
            System.out.println("drop flavor "+flavor);
        }
    }

  //End Drag-and-drop stuff

    public Component getComponent() {
        return this;
    }

    public void setPageColor(Color pageColor) {
        if (pageColor==null)
            pageColor = Color.white;
        this.pageColor = pageColor;
    }

    public Color getPageColor() {
        return pageColor;
    }

    /** Set up the default area layout. */
    private void setDefaultLayout() {
        pageLayout.setDefaultLayout();

        currentArea = null;  //start with nothing selected
        highlightedArea = null;        //nothing highlighted
    }

    //Set the currently highlighted area (the drop target)
    protected void setHighlightedArea(AreaLayout a) {
        if (a==highlightedArea)
            return;             //already set
        highlightedArea = a;
        repaint();
    }

    private void repaintCurrentImage() {
        if (currentArea==null)
            return;
        //repaint(currentArea.getBounds());
            //TODO - need to scale and translate area bounds
            // into current page coordinates
        repaint();
    }

    public void showImage(ImageBundle imageBundle, String imageInfo) {
        if (currentArea==null)
            //currentArea = (first area);
            return;     //TODO - select first area
        imageInfoText = imageInfo;
        currentArea.setImage(imageBundle);
        repaintCurrentImage();
    }

    public void showText(String text) {
        //TODO - display the text
    }

    //Override from JComponent
    public boolean isFocusTraversable() {
        return true;            //allow keyboard input
    }

    /** Select the image area at the specified location. */
    public void selectArea(Point windowPoint) {
        if (controls!=null)
            controls.selectArea(windowToUser(windowPoint));
        ImagePageArea a = windowToImageArea(windowPoint);
        if (a==null)
            return;     //not in an area
        currentArea = a;
        repaint();
    }

    /** Return the area containing the window point, or null if not in an area. */
    public ImagePageArea windowToImageArea(Point windowPoint) {
        Point userPoint = windowToUser(windowPoint);
        AreaLayout aa = getAreaLayout();
        while (true) {  //exit loop via break
            //Follow the tree down as far as we can
            AreaLayout bb = aa.getArea(userPoint);
            if (bb==null)
                break;
            aa = bb;
        }
        if (aa instanceof ImagePageArea)
            return (ImagePageArea)aa;
        return null;    //no an image area at that point
    }

    /** Rotate the current image. */
    public void rotate(int quarters) {
        if (currentArea==null)
            return;
        currentArea.rotate(quarters);
        repaintCurrentImage();
    }

    /** Paint all of our images. */
    public void paint(Graphics g) {
        paint(g,getWidth(),getHeight(),showOutlines);
    }

    private void paint(Graphics g, int devWidth, int devHeight, boolean drawOutlines) {
        Graphics2D g2 = (Graphics2D)g;
        g2.setColor(getBackground());
        g2.fillRect(0,0,devWidth,devHeight); //clear to background
        scaleAndTranslate(g2,getPageWidth(),getPageHeight(),devWidth,devHeight);
            //scale and translate the page to fit the component size
        g2.setColor(pageColor);
        g2.fillRect(0,0,getPageWidth(),getPageHeight());
        g2.setColor(getForeground());

        getAreaLayout().paint(g2,currentArea,highlightedArea,drawOutlines);
    }

    /** Given an area of specified size in user space, scale it to fit into
     * the given window space, and translate it to center it top/bottom or
     * left/right for whichever dimension is smaller.
     */
    protected static void scaleAndTranslate(Graphics2D g2, int userWidth, int userHeight,
                int windowWidth, int windowHeight) {
        double xscale = ((double)windowWidth)/((double)userWidth);
        double yscale = ((double)windowHeight)/((double)userHeight);
        double scale = (xscale<yscale)?xscale:yscale;
        if (xscale<yscale)
            g2.translate(0,(yscale-xscale)*userHeight/2);
        else
            g2.translate((xscale-yscale)*userWidth/2,0);
        g2.scale(scale,scale);
    }

    /** Transform a point in window coordinates to a point in user space. */
    private Point windowToUser(Point p) {
        Point userP = new Point(p);
        double xscale = ((double)getWidth())/((double)getPageWidth());
        double yscale = ((double)getHeight())/((double)getPageHeight());
        double scale = (xscale<yscale)?xscale:yscale;
        if (xscale<yscale)
            userP.y -= (yscale-xscale)*getPageHeight()/2.0;
        else
            userP.x -= (xscale-yscale)*getPageWidth()/2.0;
        userP.x *= 1.0/scale;
        userP.y *= 1.0/scale;
        return userP;
    }

    /** Set the cursor to a busy cursor. */
    public void setCursorBusy(boolean busy) {
        cursorBusy = busy;
        if (busy) {
                setCursor(busyCursor);
        } else {
                setCursorVisible(cursorVisible);
        }
    }

    /** Make the cursor visible or invisible.
     * If busy-cursor has been set, cursor is always visible.
     */
    public void setCursorVisible(boolean visible) {
        cursorVisible = visible;
        if (cursorBusy)
            return;		//busy takes priority over invisible
        if (visible)
            setCursor(null);
        else
            setCursor(invisibleCursor);
    }

    protected String formatPageValue(int n) {
        return pageLayout.formatPageValue(n);
    }

    /** Print this page of images. */
    public void print() {
	PrinterJob pJob = PrinterJob.getPrinterJob();
	PageFormat pageFormat = pJob.defaultPage();
	//pageFormat = pJob.validatePage(pageFormat);
	Paper oldPaper = pageFormat.getPaper();
        //Check the size of the printer paper to see if it matches the
        //size of the image page.
        double paperScale;      //dimensions of Paper object are in points
        if (getPageUnit()==PageLayout.UNIT_INCH)
            paperScale = 72.0;          //points per inch
        else
            paperScale = 72.0/2.54;     //points per cm
        double paperWidth = oldPaper.getWidth();
        double paperHeight = oldPaper.getHeight();
        double paperPageWidth = paperWidth*PageLayout.UNIT_MULTIPLIER/paperScale;
        double paperPageHeight = paperHeight*PageLayout.UNIT_MULTIPLIER/paperScale;
        double widthDiff = Math.abs(getPageWidth() - paperPageWidth);
        double heightDiff = Math.abs(getPageHeight() - paperPageHeight);
        if (widthDiff>0.5/PageLayout.UNIT_MULTIPLIER || heightDiff>0.5/PageLayout.UNIT_MULTIPLIER) {
            //Ask user if he wants to continue, and whether he wants to
            //scale the output to fill the paper, or print at the same scale
            //as if the right paper was there.
            String introStr =getResourceString("prompt.Print.PageSizeMismatch");
            Object[] pageSizeArgs = {
                new Double(((double)getPageWidth())/PageLayout.UNIT_MULTIPLIER),
                new Double(((double)getPageHeight())/PageLayout.UNIT_MULTIPLIER),
                ((getPageUnit()==PageLayout.UNIT_INCH)?"in":"cm")
            };
            String pageSizeStr = getResourceFormatted(
                    "prompt.Print.PageSizeMismatch.PageSize",pageSizeArgs);
            Object[] paperSizeArgs = {
                new Double(((double)paperPageWidth)/PageLayout.UNIT_MULTIPLIER),
                new Double(((double)paperPageHeight)/PageLayout.UNIT_MULTIPLIER),
                ((getPageUnit()==PageLayout.UNIT_INCH)?"in":"cm")
            };
            String paperSizeStr = getResourceFormatted(
                    "prompt.Print.PageSizeMismatch.PaperSize",paperSizeArgs);
            String prompt = introStr+"\n"+pageSizeStr+"\n"+paperSizeStr;
            //TODO i18n these remaining strings in the print dialog
            String yesString = "Scale images to fiil paper";
            String noString = "Print at specified page size";
            String cancelString = "Cancel";
            int response = viewer.yncDialog(prompt,yesString,noString,cancelString);
            switch (response) {
            case 0:     //yes, scale to fill paper
                break;          //leave paper size alone
            case 1:     //no, use original specified size
                paperWidth *= getPageWidth()/paperPageWidth;
                paperHeight *= getPageHeight()/paperPageHeight;
                break;
            case 2:     //cancel
            default:
                return;         //cancelled
            }
            try { Thread.sleep(10); }catch(InterruptedException ex){}
                //Curious bug on MacOSX: after going through the above
                //dialog the first time and selecting cancel, the next
                //time through when selecting Yes or No, the following
                //print dialog just blinks up on the screen momentarily
                //and self-cancels.  Adding this brief sleep seems to
                //cure the problem.
        }
	Paper newPaper = new PaperNoMargin();
	newPaper.setSize(paperWidth,paperHeight);
	pageFormat.setPaper(newPaper);
	pJob.setPrintable(this,pageFormat);
	if (!pJob.printDialog())
	    return;		//cancelled
	try {
	    pJob.print();
	} catch (PrinterException ex) {
	    throw new RuntimeException(ex);
	}
    }

    /** Get a string from our resources. */
    public String getResourceString(String name) {
            return viewer.getApp().getResourceString(name);
    }

    /** Get a string from our resources. */
    public String getResourceFormatted(String name, Object[] args) {
            return viewer.getApp().getResourceFormatted(name, args);
    }

  //The Printable interface
    public int print(Graphics graphics, PageFormat pageFormat,
			int pageIndex) {
	if (pageIndex!=0)
	    return NO_SUCH_PAGE;
        Paper paper = pageFormat.getPaper();
        int paperWidth = (int)paper.getWidth();
        int paperHeight = (int)paper.getHeight();
	paint(graphics,paperWidth,paperHeight,false);
	return PAGE_EXISTS;
    }
  //End Printable interface

  class ImagePageKeyListener implements KeyListener {
  //The KeyListener interface
    public void keyPressed(KeyEvent ev) {
        setCursorVisible(false);	//turn off cursor on any key
        int keyCode = ev.getKeyCode();
        knownKeyPress = true;	//assume we know it
        switch (keyCode) {
        case KeyEvent.VK_LEFT:
            setCursorVisible(true);
            viewer.moveLeft();
            setCursorVisible(false);
            break;
        case KeyEvent.VK_RIGHT:
            setCursorVisible(true);
            viewer.moveRight();
            setCursorVisible(false);
            break;
        case KeyEvent.VK_DOWN:
            setCursorVisible(true);
            viewer.moveDown();
            setCursorVisible(false);
            break;
        case KeyEvent.VK_UP:
            setCursorVisible(true);
            viewer.moveUp();
            setCursorVisible(false);
            break;
        case KeyEvent.VK_ESCAPE:
            viewer.setScreenMode(Viewer.SCREEN_NORMAL);	//back to normal size
            break;
        case KeyEvent.VK_ENTER:
            viewer.activateSelection();
            break;
        default:	//ignore
            knownKeyPress = false;
            break;
        }
    }
    public void keyReleased(KeyEvent ev) {
        int keyCode = ev.getKeyCode();
    }
    public void keyTyped(KeyEvent ev) {
            char ch = ev.getKeyChar();
            switch (ch) {
            case ' ':   //activate selection
                    viewer.activateSelection();
                    break;
            case 'a':	//alternate-screen
                    viewer.setScreenMode(Viewer.SCREEN_ALT);
                    break;
            case 'f':	//full-screen
                    viewer.setScreenMode(Viewer.SCREEN_FULL);
                    break;
            case 'L'-0100:	//control-L, refresh
                    //showCurrentImage();               //TODO
                    break;
            case 'e':
                    setCursorVisible(true);	//turn on cursor
                    viewer.showImageEditDialog();
                    setCursorVisible(false);	//turn cursor back off
                    break;
            case 'i':
                    setCursorVisible(true);	//turn on cursor
                    if (imageInfoText==null) {
                            imageInfoText =
                                getResourceString("query.Info.NoDescription");
                    }
                    viewer.infoDialog(imageInfoText);
                    setCursorVisible(false);	//turn cursor back off
                    break;
            case 'o':	//file-open dialog
                    setCursorVisible(true);	//turn on cursor
                    viewer.processFileOpen();
                    setCursorVisible(false);	//turn cursor back off
                    break;
            case 'p':	//the print screen
                    viewer.setScreenMode(Viewer.SCREEN_PRINT);
                    break;
            case 'r':	//rotate CCW
                    viewer.rotateCurrentImage(1);
                    break;
            case 'R':	//rotate CW
                    viewer.rotateCurrentImage(-1);
                    break;
            case 'R'-0100:	//control-R, rotate 180
                    viewer.rotateCurrentImage(2);
                    break;
            case 'x':	//exit
                    setCursorVisible(true);	//turn on cursor
                    viewer.processClose();
                    setCursorVisible(false);	//turn cursor back off
                    break;
            case '?':
                    setCursorVisible(true);	//turn on cursor
                    viewer.showHelpDialog();
                    setCursorVisible(false);	//turn cursor back off
                    break;
            case 0177:                  //delete
            case 8:                     //backspace
                    if (currentArea!=null) {
                        //clear image from current area
                        currentArea.setImage(null);
                        repaintCurrentImage();
                    }
                    break;
            default:		//unknown key
                    if (!knownKeyPress) {
                        System.out.println("Unknown key "+ch+" ("+((int)ch)+")");
                        getToolkit().beep();
                    }
                    break;
            }
    }
  //End KeyListener interface
  }

  class ImagePageMouseListener implements MouseListener {
  //The MouseListener interface
    public void mouseClicked(MouseEvent ev) {}
    public void mouseEntered(MouseEvent ev) {}
    public void mouseExited(MouseEvent ev) {}
    public void mousePressed(MouseEvent ev) {
            requestFocus();
            selectArea(new Point(ev.getX(),ev.getY()));
    }
    public void mouseReleased(MouseEvent ev) {}
  //End MouseListener interface
  }

  class ImagePageMouseMotionListener implements MouseMotionListener {
  //The MouseMotionListener interface
    public void mouseDragged(MouseEvent ev){
            setCursorVisible(true); //turn cursor back on
    }
    public void mouseMoved(MouseEvent ev){
            setCursorVisible(true); //turn cursor back on
    }
  //End MouseMotionListener interface
  }

  class ImagePageComponentListener implements ComponentListener {
  //The ComponentListener interface
    public void componentHidden(ComponentEvent ev){}
    public void componentMoved(ComponentEvent ev){}
    public void componentResized(ComponentEvent ev){
        //app.debugMsg("componentResized");
        //repaint();
    }
    public void componentShown(ComponentEvent ev){}
  //End ComponentListener interface
  }

  class PaperNoMargin extends Paper {
        //Force our paper's imageable area to the full size
        public double getImageableHeight() {
            return getHeight();
        }
        public double getImageableWidth() {
            return getWidth();
        }
        public double getImageableX() {
            return 0.0;
        }
        public double getImageableY() {
            return 0.0;
        }
    }
}
