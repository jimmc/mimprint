/* Viewer.java
 *
 * Jim McBeath, September 15, 2001
 */

package net.jimmc.mimprint;

import net.jimmc.swing.CheckBoxMenuAction;
import net.jimmc.swing.GridBagger;
import net.jimmc.swing.JsFrame;
import net.jimmc.swing.MenuAction;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Vector;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

/** The top window class for the mimprint program.
 */
public class Viewer extends JsFrame {
    /** Our application info. */
    private App app;

    /** The list of images. */
    private ImageLister imageLister;

    private JSplitPane splitPane;
    private JPanel imagePane;
    //private CardLayout imagePaneLayout;
    private JTextField statusLine;

    /** Our image display area. */
    private ImageArea imageArea;
    private ImageArea fullImageArea;
    private ImagePage imagePage;
    private JPanel imagePagePanel;

    private ImagePageControls imagePageControls;

    /** Full-screen window. */
    private JFrame fullWindow;
        //Have to use a Frame here rather than a window; when using
        //a window, the keyboard input is directed to the main
        //frame (Viewer), which causes problems.
    /** Alternate screen window. */
    private JWindow altWindow;

    /** The current screen mode. */
    private int screenMode;

    private JMenu layoutMenu;
    private MenuAction printMenuItem;

    //Menu items for screen mode
    private CheckBoxMenuAction cbmaSlideShow;
    private CheckBoxMenuAction cbmaAlternate;
    private CheckBoxMenuAction cbmaFull;
    private CheckBoxMenuAction cbmaPrint;

    private CheckBoxMenuAction cbmaSplitPane;
    private CheckBoxMenuAction cbmaListIncludeInfo;
    private CheckBoxMenuAction cbmaListIncludeImage;
    private CheckBoxMenuAction cbmaShowAreaOutlines;

    /** The screen bounds when in slideshow mode. */
    private Rectangle slideShowBounds;

    /** The latest file opened with the File Open dialog. */
    private File currentOpenFile;

    private File lastSaveLayoutTemplateFile;
    private File lastLoadLayoutTemplateFile;

    /** Create our frame. */
    public Viewer(App app) {
        super();
        setResourceSource(app);
        this.app = app;
        setJMenuBar(createMenuBar());
        initForm();
        setScreenMode(SCREEN_PRINT);
        pack();

        cbmaSplitPane.setState(true);   //start off with horizontal split
        setSplitPaneHorizontal(cbmaSplitPane.getState());
        cbmaListIncludeImage.setState(true);
        setListMode(ImageLister.MODE_INFO);

        imageArea.requestFocus();
        addWindowListener();
        setTitleFileName("");
    }

    protected Component getDialogParent() {
        if (fullWindow!=null)
            return fullWindow;
        else
            return this;
    }

    /** Show status text in our status line. */
    public void showStatus(String s) {
        if (s==null)
            s = "";
        statusLine.setText(s);
    }

    /** Our message display text area uses a big font so we can read it
     * on the TV. */
    protected JTextArea getMessageDisplayTextArea(String msg) {
        JTextArea textArea = super.getMessageDisplayTextArea(msg);
        if (app.useBigFont()) {
            Font bigFont = new Font("Serif",Font.PLAIN,25);
            textArea.setFont(bigFont);
            maxSimpleMessageLength = 0;    //use special dialogs
        }
        return textArea;
    }

    /** Create our menu bar. */
    protected JMenuBar createMenuBar() {
        JMenuBar mb = new JMenuBar();
        mb.add(createFileMenu());
        mb.add(createImageMenu());
        mb.add(layoutMenu=createLayoutMenu());
        layoutMenu.setEnabled(false);
        mb.add(createViewMenu());
        mb.add(createHelpMenu());
        return mb;
    }

    /** Create our File menu. */
    protected JMenu createFileMenu() {
        JMenu m = new JMenu(getResourceString("menu.File.label"));
        MenuAction mi;

        String openLabel = getResourceString("menu.File.Open.label");
        mi = new MenuAction(openLabel) {
            public void action() {
                processFileOpen();
            }
        };
        m.add(mi);

        String printLabel = getResourceString("menu.File.Print.label");
        mi = printMenuItem = new MenuAction(printLabel) {
            public void action() {
                processPrint();
            }
        };
                printMenuItem.setEnabled(false);
                    //only enabled when we are showing the Printable window
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

    /** Create our Image menu. */
    protected JMenu createImageMenu() {
        JMenu m = new JMenu(getResourceString("menu.Image.label"));
        MenuAction mi;
                String label;

        label = getResourceString("menu.Image.PreviousImage.label");
        mi = new MenuAction(label) {
            public void action() {
                moveUp();
            }
        };
        m.add(mi);

        label = getResourceString("menu.Image.NextImage.label");
        mi = new MenuAction(label) {
            public void action() {
                moveDown();
            }
        };
        m.add(mi);

        label = getResourceString("menu.Image.PreviousDirectory.label");
        mi = new MenuAction(label) {
            public void action() {
                moveLeft();
            }
        };
        m.add(mi);

        label = getResourceString("menu.Image.NextDirectory.label");
        mi = new MenuAction(label) {
            public void action() {
                moveRight();
            }
        };
        m.add(mi);

        m.add(new JSeparator());

        label = getResourceString("menu.Image.RotateMenu.label");
        JMenu rotateMenu = new JMenu(label);
        m.add(rotateMenu);

        label = getResourceString("menu.Image.RotateMenu.R90.label");
        mi = new MenuAction(label) {
            public void action() {
                rotateCurrentImage(1);
            }
        };
        rotateMenu.add(mi);

        label = getResourceString("menu.Image.RotateMenu.R180.label");
        mi = new MenuAction(label) {
            public void action() {
                rotateCurrentImage(2);
            }
        };
        rotateMenu.add(mi);

        label = getResourceString("menu.Image.RotateMenu.R270.label");
        mi = new MenuAction(label) {
            public void action() {
                rotateCurrentImage(-1);
            }
        };
        rotateMenu.add(mi);

        label = getResourceString("menu.Image.ShowEditDialog.label");
        mi = new MenuAction(label) {
            public void action() {
                showImageEditDialog();
            }
        };
        m.add(mi);

        label = getResourceString("menu.Image.ShowInfoDialog.label");
        mi = new MenuAction(label) {
            public void action() {
                showImageInfoDialog();
            }
        };
        m.add(mi);

        return m;
    }

    /** Create our Layout menu. */
    protected JMenu createLayoutMenu() {
        JMenu m = new JMenu(getResourceString("menu.Layout.label"));
        MenuAction mi;
        String label;

        label = getResourceString("menu.Layout.SaveTemplateAs.label");
        mi = new MenuAction(label) {
            public void action() {
                saveLayoutTemplateAs();
            }
        };
        m.add(mi);

        label = getResourceString("menu.Layout.LoadTemplate.label");
        mi = new MenuAction(label) {
            public void action() {
                loadLayoutTemplate();
            }
        };
        m.add(mi);

        label = getResourceString("menu.Layout.EditDescription.label");
        mi = new MenuAction(label) {
            public void action() {
                editLayoutDescription();
            }
        };
        m.add(mi);

        return m;
    }

    /** Create our View menu. */
    protected JMenu createViewMenu() {
        JMenu m = new JMenu(getResourceString("menu.View.label"));
        MenuAction mi;
        String label;

        label = getResourceString("menu.View.ScreenModePrint.label");
        cbmaPrint = new CheckBoxMenuAction(label) {
            public void action() {
                setScreenMode(SCREEN_PRINT);
            }
        };
        m.add(cbmaPrint);

        label = getResourceString("menu.View.ScreenModeSlideShow.label");
        cbmaSlideShow = new CheckBoxMenuAction(label) {
            public void action() {
                setScreenMode(SCREEN_SLIDESHOW);
            }
        };
        m.add(cbmaSlideShow);

        label = getResourceString("menu.View.ScreenModeAlternate.label");
        cbmaAlternate = new CheckBoxMenuAction(label) {
            public void action() {
                setScreenMode(SCREEN_ALT);
            }
        };
        m.add(cbmaAlternate);

        label = getResourceString("menu.View.ScreenModeFull.label");
        cbmaFull = new CheckBoxMenuAction(label) {
            public void action() {
                setScreenMode(SCREEN_FULL);
            }
        };
        m.add(cbmaFull);

        cbmaPrint.setState(true); //default view mode is printable

        m.add(new JSeparator());

        label = getResourceString("menu.View.SplitPaneHorizontal.label");
        cbmaSplitPane = new CheckBoxMenuAction(label) {
            public void action() {
                setSplitPaneHorizontal(cbmaSplitPane.getState());
            }
        };
        m.add(cbmaSplitPane);

        label = getResourceString("menu.View.ListIncludeInfo.label");
        cbmaListIncludeInfo = new CheckBoxMenuAction(label) {
            public void action() {
                int listMode = cbmaListIncludeInfo.getState()?
                        ImageLister.MODE_INFO:ImageLister.MODE_NAME;
                setListMode(listMode);
            }
        };
        m.add(cbmaListIncludeInfo);

        label = getResourceString("menu.View.ListIncludeImage.label");
        cbmaListIncludeImage = new CheckBoxMenuAction(label) {
            public void action() {
                int listMode = cbmaListIncludeImage.getState()?
                        ImageLister.MODE_FULL:ImageLister.MODE_NAME;
                setListMode(listMode);
            }
        };
        m.add(cbmaListIncludeImage);

        label = getResourceString("menu.View.ShowAreaOutlines.label");
        cbmaShowAreaOutlines = new CheckBoxMenuAction(label) {
            public void action() {
                if (imagePage==null)
                    return;   //shouldn't get here, but avoid NPE if so
                imagePage.setShowOutlines(
                        cbmaShowAreaOutlines.getState());
                imagePage.repaint();
            }
        };
        m.add(cbmaShowAreaOutlines);
        cbmaShowAreaOutlines.setState(true);
        cbmaShowAreaOutlines.setEnabled(false);
            //enable this when imagePage is created

        m.add(new JSeparator());

        label = getResourceString("menu.View.ShowHelpDialog.label");
        mi = new MenuAction(label) {
            public void action() {
                showHelpDialog();
            }
        };
        m.add(mi);

        return m;
    }

    /** Create the body of our form. */
    protected void initForm() {
        imageLister = new ImageLister(app,this);
        imageArea = new ImageArea(app,this);
        imageLister.setImageWindow(imageArea);
                //There is a bug in Java that causes drag-and-drop not
                //to work correctly in a CardLayout, so we don't use it.
                //Instead, we just add and remove the children as we
                //want to display them.
                //imagePaneLayout = new CardLayout();
                //imagePane = new JPanel(imagePaneLayout);
                imagePane = new JPanel(new BorderLayout());
                imagePane.setMinimumSize(new Dimension(100,100));
                //imagePane.add(imageArea,"slideShow");
                imagePane.add(imageArea,BorderLayout.CENTER);
        splitPane = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,imageLister,imagePane);
        splitPane.setBackground(imageArea.getBackground());

        statusLine = new JTextField();
        statusLine.setEditable(false);
        statusLine.setBackground(Color.lightGray);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(splitPane,BorderLayout.CENTER);
        getContentPane().add(statusLine,BorderLayout.SOUTH);
    }

    /** Get our App. */
    public App getApp() {
        return app;
    }

    /** Toggle the orientation of the top-level split pane. */
    public void setSplitPaneHorizontal(boolean horizontal) {
        int newOrientation = horizontal?
            JSplitPane.HORIZONTAL_SPLIT:JSplitPane.VERTICAL_SPLIT;
        if (newOrientation==splitPane.getOrientation())
            return;         //no change
        splitPane.setOrientation(newOrientation);
        splitPane.setDividerLocation(0.25);
        imageLister.showListOnly(horizontal);
    }

    /** Activate the currently selected item in the list. */
    public void activateSelection() {
        imageLister.activateSelection();
    }

    /** Open the specified target. */
    public void open(String target) {
        currentOpenFile = new File(target);
        imageLister.open(target);
    }

    /** Open the specified target. */
    public void open(File targetFile) {
        currentOpenFile = targetFile;
        imageLister.open(targetFile);
    }

    /** Set our status info. */
    public void setStatus(String status) {
        imageLister.setStatus(status);
    }

    /** Process the File->Open menu command. */
    protected void processFileOpen() {
        String msg = getResourceString("query.FileToOpen");
        File newOpenFile = fileOrDirectoryOpenDialog(msg,currentOpenFile);
        if (newOpenFile==null)
            return;        //nothing specified
        currentOpenFile = newOpenFile;
        open(currentOpenFile);        //open it
    }

    /** Process the File->Print menu command. */
    protected void processPrint() {
        //We should only get here if we are in SCREEN_PRINT mode
        if (screenMode!=SCREEN_PRINT) {
            return;
        }
        imagePage.print();
    }

    /** Closing this form exits the program. */
    protected void processClose() {
        processFileExit();
    }

    //Just exit, don't bother asking
    protected boolean confirmExit() {
        return true;
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

    public final static int SCREEN_SLIDESHOW = 0;
    public final static int SCREEN_FULL = 1;
    public final static int SCREEN_PRINT = 2;
    public final static int SCREEN_ALT = 3;
    /** Set the image area to take up the full screen, or unset.
     * @param mode The screen mode to use, one of the SCREEN_* constants.
     */
    public void setScreenMode(int mode) {
        if (mode==screenMode)
            return;        //already in that mode
        //imageLister.setVisible(!fullImage);
            //Calling setVisible gets rid of the imageLister when we
            //switch to full screen mode, but it doesn't come back
            //when we return to slideShow mode.
        if (mode!=SCREEN_FULL) {
            this.show();
        }
        switch (mode) {
        case SCREEN_SLIDESHOW:
            {
            //Switch back to slideShow bounds
            //setBounds(slideShowBounds.x, slideShowBounds.y,
            //    slideShowBounds.width, slideShowBounds.height);
            //imagePaneLayout.show(imagePane,"slideShow");
            if (imagePagePanel!=null)
                imagePane.remove(imagePagePanel);
            imagePane.add(imageArea);
            imageLister.setImageWindow(imageArea);
            fullImageArea = null;
            imageArea.requestFocus();
            }
            break;
        case SCREEN_FULL:
            {
            Dimension screenSize = getToolkit().getScreenSize();
            fullImageArea = new ImageArea(app,this);
            fullWindow = new JFrame();
            fullWindow.getContentPane().add(fullImageArea);
            fullWindow.setBounds(0,0,screenSize.width,screenSize.height);
            fullWindow.setBackground(fullImageArea.getBackground());
            imageLister.setImageWindow(fullImageArea);
            fullWindow.show();
            fullImageArea.requestFocus();
            this.hide();
            imageArea.showText("see full page window for image");
            }
            break;
        case SCREEN_ALT:
            {
            GraphicsEnvironment ge = GraphicsEnvironment.
                    getLocalGraphicsEnvironment();
            GraphicsDevice[] gs = ge.getScreenDevices();
            Vector configs = new Vector();
            for (int i=0; i<gs.length; i++) {
                GraphicsDevice gd = gs[i];
                GraphicsConfiguration[] gc = gd.getConfigurations();
                for (int j=0; j<gc.length; j++) {
                    configs.addElement(gc[j]);
                }
            }
            if (configs.size()<2) {
                getToolkit().beep();
                return;         //only one display, no mode change
            }
            //TODO - if more than 2 configs, ask which one to use
            //For now, just use the second config

            GraphicsConfiguration gc = 
                    (GraphicsConfiguration)configs.elementAt(1);
            Rectangle altScreenBounds = gc.getBounds();

            fullImageArea = new ImageArea(app,this);
            altWindow = new JWindow();
            altWindow.getContentPane().add(fullImageArea);
            altWindow.setBounds(altScreenBounds);
            altWindow.setBackground(fullImageArea.getBackground());
            imageLister.setImageWindow(fullImageArea);
            altWindow.show();
            fullImageArea.requestFocus();
            imageArea.showText("see alternate screen for image");
            }
            break;
        case SCREEN_PRINT:
            {
            if (imagePage==null) {
                imagePage = new ImagePage(this);
                imagePage.setBackground(Color.gray);
                imagePage.setForeground(Color.black);
                imagePage.setPageColor(Color.white);
                imagePagePanel = new JPanel();
                imagePagePanel.setLayout(new BorderLayout());
                imagePagePanel.add(imagePage,BorderLayout.CENTER);
                imagePageControls = new ImagePageControls(app,imagePage);
                imagePage.setControls(imagePageControls);
                imagePagePanel.add(imagePage,BorderLayout.CENTER);
                imagePagePanel.add(imagePageControls,BorderLayout.NORTH);
                //imagePane.add(imagePagePanel,"print");
                cbmaShowAreaOutlines.setEnabled(true);
            }
            //imagePaneLayout.show(imagePane,"print");
            imagePane.remove(imageArea);
            imagePane.add(imagePagePanel,BorderLayout.CENTER);
            imageLister.setImageWindow(imagePage);
            imagePage.requestFocus();
            imageArea.showText("see image pane for image");
            }
            break;
}
        if (mode!=SCREEN_FULL && fullWindow!=null) {
            fullWindow.hide();
            fullWindow.dispose();
            fullWindow = null;
        }
        if (mode!=SCREEN_ALT && altWindow!=null) {
            altWindow.hide();
            altWindow.dispose();
            altWindow = null;
        }
        screenMode = mode;
        imageLister.displayCurrentSelection();
        try {
            Thread.sleep(200);  //give image updater a bit of time to run
        } catch (InterruptedException ex) {
            //ignore
        }
        if (altWindow!=null)
            altWindow.validate();
        if (fullWindow!=null)
            fullWindow.validate();
        else {
            this.validate();
            this.repaint();
        }

        layoutMenu.setEnabled(mode==SCREEN_PRINT);
        printMenuItem.setEnabled(mode==SCREEN_PRINT);

        cbmaSlideShow.setState(mode==SCREEN_SLIDESHOW);
        cbmaAlternate.setState(mode==SCREEN_ALT);
        cbmaFull.setState(mode==SCREEN_FULL);
        cbmaPrint.setState(mode==SCREEN_PRINT);
    }

    /** Set the specified mode on the lister, and update the states
     * of the related menu items. */
    private void setListMode(int mode) {
        imageLister.setListMode(mode);
        cbmaListIncludeInfo.setState(mode==ImageLister.MODE_INFO);
        cbmaListIncludeImage.setState(mode==ImageLister.MODE_FULL);
    }

    /** Display the specified text, allow the user to edit it.
     * @param title The title of the editing dialog.
     * @param text The text to display and edit.
     * @return The edited string, or null if cancelled.
     */
    protected String editTextDialog(String title, String text) {
        final JTextArea tx = new JTextArea(text);
        JScrollPane scroll = new JScrollPane(tx);
        scroll.setPreferredSize(new Dimension(500,200));
        JOptionPane pane = new JOptionPane(scroll,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);
        JDialog dlg = pane.createDialog(getDialogParent(),title);
        dlg.setResizable(true);
        pane.setInitialValue(null);
        pane.selectInitialValue();
	//We want the text area to have the focus, and it seems there is no
	//easy way to do this.  See Bug 4222534, from whence came this code.
	dlg.addWindowListener(new WindowAdapter() {
	    public void windowActivated(WindowEvent e) {
		SwingUtilities.invokeLater( new Runnable() {  
		    public void run() {
			tx.requestFocus();
		    }
		});
	    }
	});
        dlg.show();    //get user's changes

        Object v = pane.getValue();
        if (!(v instanceof Integer))
            return null;        //CLOSED_OPTION
        int n = ((Integer)v).intValue();
        if (n==JOptionPane.NO_OPTION || n==JOptionPane.CANCEL_OPTION)
            return null;        //canceled
        String newText = tx.getText();
        return newText;
    }

    /** Put up an editing dialog showing the image text. */
    public void showImageEditDialog() {
        String imageText = imageLister.getCurrentImageFileText();
        if (imageText==null) {
            String msg = getResourceString("error.NoImageSelected");
            errorDialog(msg);
            return;
        }
        String title = getResourceFormatted("prompt.TextForImage",
                imageLister.currentImage.path);
        String newImageText = editTextDialog(title,imageText);
        if (newImageText==null)
            return;        //cancelled
        imageLister.setCurrentImageFileText(newImageText);
    }

    /** Put up a dialog showing the image info. */
    public void showImageInfoDialog() {
        String imageInfo = imageLister.getCurrentImageFileInfo();
        if (imageInfo==null) {
            String msg = getResourceString("error.NoImageSelected");
            errorDialog(msg);
            return;
        }
        infoDialog(imageInfo);
    }

    /** Rotate the current image. */
    public void rotateCurrentImage(int quarters) {
        switch (screenMode) {
        case SCREEN_FULL:
        case SCREEN_ALT:
            fullImageArea.rotate(quarters);
            break;
        case SCREEN_PRINT:
            imagePage.rotate(quarters);
            break;
        case SCREEN_SLIDESHOW:
        default:
            imageArea.rotate(quarters);
            break;
        }
    }

    /** Move the active image up to the previous image in the lister. */
    public void moveUp() {
        imageLister.up();
    }

    /** Move the active image down to the next image in the lister. */
    public void moveDown() {
        imageLister.down();
    }

    /** Move the active image left to the previous dir in the lister. */
    public void moveLeft() {
        imageLister.left();
    }

    /** Move the active image right to the next dir in the lister. */
    public void moveRight() {
        imageLister.right();
    }

    /** Save the current layout to a named file. */
    private void saveLayoutTemplateAs() {
        String prompt = getResourceString("prompt.SaveLayoutTemplateAs");
        File f = fileSaveDialog(prompt,lastSaveLayoutTemplateFile);
        if (f==null)
            return;
        lastSaveLayoutTemplateFile = f;
        PrintWriter pw = getPrintWriterFor(f);
        if (pw==null)
            return;         //cancelled
        imagePage.writeLayoutTemplate(pw);
        pw.close();
        String status = getResourceFormatted("status.SavedTemplateToFile",
                f.toString());
        showStatus(status);
    }

    /** Load a layout from a named file. */
    private void loadLayoutTemplate() {
        String prompt = getResourceString("prompt.LoadLayoutTemplate");
        File f = fileOpenDialog(prompt,lastLoadLayoutTemplateFile);
        if (f==null)
            return;
        lastLoadLayoutTemplateFile = f;
        loadLayoutTemplate(f);
    }

    //Load the specified layout template
    public void loadLayoutTemplate(File f) {
        if (imagePage==null) {
            String msg = getResourceString("error.NoPrintablePage");
            showStatus(msg);
            return;
        }
        PageLayout pageLayout = new PageLayout(app);
        pageLayout.loadLayoutTemplate(f);
            //TODO - check return status here rather than using exception?
        imagePage.setPageLayout(pageLayout);
        String status = getResourceFormatted(
                "status.LoadedTemplateFromFile",f.toString());
        showStatus(status);
    }

    //Edit the description of the current page layout
    private void editLayoutDescription() {
        if (imagePage==null) {
            String msg = getResourceString("error.NoPrintablePage");
            showStatus(msg);
            return;
        }
        PageLayout pageLayout = imagePage.getPageLayout();
        String text = pageLayout.getDescription();
        if (text==null)
                text = "";
        String title = getResourceString("query.EditLayout.title");
        String newText = editTextDialog(title,text);
        if (newText==null)
                return;        //cancelled
        pageLayout.setDescription(newText);
    }

    /** Put up a help dialog. */
    public void showHelpDialog() {
        String helpText = app.getResourceString("info.ImageHelp");
        infoDialog(helpText);
    }

    /** Get a string from our resources. */
    public String getResourceString(String name) {
        return app.getResourceString(name);
    }

    /** Get a string from our resources. */
    public String getResourceFormatted(String name, String arg) {
        return app.getResourceFormatted(name, arg);
    }
}

/* end */
