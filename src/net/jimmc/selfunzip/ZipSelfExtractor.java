// From http://www.javaworld.com/javaworld/javatips/jw-javatip120.html

/* ZipSelfExtractor.java */
/* Author: Z.S. Jin
   Updates: John D. Mitchell, Jim McBeath */

package net.jimmc.selfunzip;

import net.jimmc.jshortcut.JShellLink;

import java.awt.event.ActionEvent;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;
import javax.swing.*;

/* Self-extractor for installing Java applications.
 * This class can be added to a jar file to turn it into a self-extracting
 * jar file.  If the jar file is invoked in the standard way,
 * using the command "java -jar file.jar" or double-clicking on it,
 * it opens a dialog allowing the user to control install options,
 * then installs the application.
 * <p>
 * On Windows systems, the user has the option of creating desktop and
 * menu shortcuts.  This is done by using the JShellLink class from
 * the JShortcut package, which must thus be available.
 */
public class ZipSelfExtractor extends JFrame
{
    private final String appname = "mimprint";  //name as used in files
    private final String appnameCap = "Mimprint";  //name as shown to user
    private final String appAbbrev = "JV";      //two-char abbrev as short name
    private boolean isServer = false;

    private String myClassName;
    private String jsClassName;
    static String MANIFEST = "META-INF/MANIFEST.MF";
    private File tmpDllFile;
    JTextField installDirField;
    private boolean addShortcutP;
    private boolean addMenuP;
    private boolean addClientP;
    private boolean autoStartP;
    private boolean showStatsP;
    private boolean debug;	//set with -Dmimprint.install.debug

    public static void main(String[] args)
    {
	ZipSelfExtractor zse = new ZipSelfExtractor();
	boolean ok = zse.doMain(args);
	System.exit(ok?0:1);
    }

    /** Do everything except exit.
     * @return True if all was OK, false if there was an error.
     */
    public boolean doMain(String[] args) {
    	debug = (System.getProperty(appname+".install.debug")!=null);
    	if (System.getProperty(appname+".install.pause")!=null) {
	    System.out.print(
		    "Installer paused for debug, press Enter to continue: ");
	    try { System.in.read(); } catch (Exception ex) {}
	}
	String jarFileName = getJarFileName();
	return extract(jarFileName);
    }

    ZipSelfExtractor()
    {
    }

    private String getJarFileName()
    {
	myClassName = this.getClass().getName().replace('.','/') + ".class";
	URL urlJar =
		this.getClass().getClassLoader().getSystemResource(myClassName);
	String urlEncStr = urlJar.toString();
	//The urlEncStr contains encodings like "%20" for space,
	//we need to convert it back to regular characters.
	String urlStr = URLDecoder.decode(urlEncStr);
	int from = "jar:file:".length();
	int to = urlStr.indexOf("!/");
	String jarFileName = urlStr.substring(from, to);
	if (debug) {
	    System.out.println("myClassName="+myClassName);
	    System.out.println("urlEncStr="+urlEncStr);
	    System.out.println("urlStr="+urlStr);
	    System.out.println("jarFileName="+jarFileName);
	}
	return jarFileName;
    }

    //True if we are running on Windows
    private boolean isWindows() {
    	return (File.separatorChar=='\\' ||
		(System.getProperty(appname+".install.forceWindows")!=null));
    }

    //Extract the program name and the version number from the file name
    private String getProgramAndVersion(String filename) {
	if (debug) {
	    System.out.println("programAndVersion from "+filename);
	}
        int xx = filename.lastIndexOf('.');
	if (xx>0) {
	    String ext = filename.substring(xx+1).toLowerCase();
	    if (ext.equals("jar"))
		filename = filename.substring(0,xx);
	}
    	int dx = filename.indexOf('-');
	if (dx<0) {
	    dx = filename.indexOf('_');	//on CD, we use _ instead of -
	    if (dx>0 && Character.isDigit(filename.charAt(dx-1))) {
		dx--;	//point to last digit before underscore
		while (dx>0 && Character.isDigit(filename.charAt(dx-1))) {
	            dx--;	//include all digits just before underscore
	    	}
		dx--;	//point to the char just before the first digit
	    }
	}
	if (dx<0) {
	    if (debug) {
		System.out.println("Can't figure out program and version");
		System.out.println("programAndVersion="+filename);
	    }
	    return filename;	//can't figure it out
	}
	String ver = filename.substring(dx+1).replace('_','.');
	char ch = filename.charAt(dx);
	if (ch!='_' && ch!='-')
	    dx++;
	String prog = filename.substring(0,dx);
	if (prog.equalsIgnoreCase(appname) || prog.equalsIgnoreCase(appAbbrev))
	    prog = appnameCap;
	String programAndVersion = prog+" "+ver;
	if (debug) {
	    System.out.println("programAndVersion="+programAndVersion);
	}
	return programAndVersion;
    }

    //Extract the version number suffix from the file name.
    //The string includes underscore rather than dot.
    private String getVersionSuffix(String filename) {
	if (debug) {
	    System.out.println("getVersionSuffix from "+filename);
	}
	int sepx = filename.lastIndexOf(File.separatorChar);
	if (sepx>0)
	    filename = filename.substring(sepx+1);
	int vx = filename.indexOf("-");
	if (vx<0)
	    vx = filename.indexOf("_");
	    if (vx>0 && Character.isDigit(filename.charAt(vx-1))) {
		vx--;	//point to last digit before underscore
		while (vx>0 && Character.isDigit(filename.charAt(vx-1))) {
	            vx--;	//include all digits just before underscore
	    	}
		vx--;	//point to the char just before the first digit
	    }
	if (vx<0) {
	    if (debug) {
	        System.out.println("Can't figure version from "+filename);
	    	System.out.println("versionSuffix=X_X_X");
	    }
	    return "X_X_X";		//can't figure it out
	}
	String version = filename.substring(vx+1);
	vx = version.lastIndexOf(".");
	if (vx>0)
	    version= version.substring(0,vx);
	if (debug) {
	    System.out.println("versionSuffix="+version);
	}
	return version;
    }

    //Get the directory in which to unpack our file
    //@param filename The name of the JAR file from which we are unpacking.
    File getInstallDir(String filename) {
	String programAndVersion = getProgramAndVersion(filename);
	String installDirectory;
	if (isWindows()) {
	    installDirectory = JShellLink.getDirectory("program_files")+
	    			"\\"+appnameCap;
	} else {
	    installDirectory = System.getProperty("user.home")+
		File.separator+appname;
	}

	String versionSuffix = getVersionSuffix(filename);
	String msg = "This installer will create the directory\n"+
		"   "+appname+"-"+versionSuffix+"\n"+
		"within the Install Directory you select.\n ";

	//Create a panel with all of the install options
	Box panel = Box.createVerticalBox();
		//We want everything in this box to be left-justified.
		//It should work for us to call panel.setAlignmentX(0f),
		//but it doesn't.  Instead, we use hBox (below) which puts
		//in some "glue" to make things be left-justified.
	JTextArea introLabel = new JTextArea(msg);
	introLabel.setEditable(false);
	introLabel.setBackground(panel.getBackground());
	panel.add(introLabel);

	Box installDirBox = Box.createHorizontalBox();
	installDirBox.add(new JLabel("Install Directory: "));
	installDirField = new JTextField(30);
	installDirField.setText(installDirectory);
	installDirBox.add(installDirField);
	JButton browseButton = new JButton("Browse...");
	browseButton.addActionListener(new AbstractAction() {
	    public void actionPerformed(ActionEvent ev) {
	        String newInstallDir = browseForInstallDirectory(
			installDirField.getText());
		if (newInstallDir!=null) {
		    installDirField.setText(newInstallDir);
		}
	    }
	});
	installDirBox.add(browseButton);
	panel.add(installDirBox);

	JCheckBox addShortcutField = null;
	JCheckBox addMenuField = null;
	JCheckBox addClientField = null;
	if (isWindows()) {
	    String shortcutPrompt = "Add "+appnameCap+" shortcut on desktop";
	    addShortcutField = new JCheckBox(shortcutPrompt,true);
	    panel.add(hBox(addShortcutField));

	    String menuPrompt = "Add "+appnameCap+" menu to Start Menu";
	    addMenuField = new JCheckBox(menuPrompt,true);
	    panel.add(hBox(addMenuField));

            if (isServer) {
                String clientPrompt = "Add "+appnameCap+" client/server shortcuts/menus";
                addClientField = new JCheckBox(clientPrompt,true);
                panel.add(hBox(addClientField));
            }
	}

	//For almost everyone, showing the completion screen is just clutter.
	//Make it available for debugging if they run with -DUNJAR_SHOW_STATS
	JCheckBox showStatsField = null;
	if (System.getProperty("UNJAR_SHOW_STATS")!=null) {
	    String statsPrompt = "Show extraction statistics on completion";
	    showStatsField = new JCheckBox(statsPrompt,false);
	    panel.add(hBox(showStatsField));
	}

	String autoStartPrompt = "Auto-start "+appnameCap+" after install";
	JCheckBox autoStartField = null;
	autoStartField = new JCheckBox(autoStartPrompt,true);
	panel.add(hBox(autoStartField));

	//Show the user the query panel so he can select options
	String title = "Installing "+programAndVersion;
	Object[] options = { "Install", "Cancel" };
	int answer = JOptionPane.showOptionDialog(this,panel,title,
	    JOptionPane.DEFAULT_OPTION,
	    JOptionPane.QUESTION_MESSAGE, null,
	    options,options[0]);
	if (answer!=0) {
	    return null;		//cancelled
	}

	//Get the values from the dialog box
	installDirectory = installDirField.getText();
	if (addShortcutField!=null)
	    addShortcutP = addShortcutField.isSelected();
	if (addMenuField!=null)
	    addMenuP = addMenuField.isSelected();
	if (addClientField!=null)
	    addClientP = addClientField.isSelected();
	if (autoStartField!=null)
	    autoStartP = autoStartField.isSelected();
	if (showStatsField!=null)
	    showStatsP = showStatsField.isSelected();

	File installDirFile = new File(installDirectory);
	if (!installDirFile.exists()) {
	    if (!installDirFile.mkdirs()) {
		msg = "Unable to create directory\n"+installDirectory;
		title="Error Creating Directory";
		JOptionPane.showMessageDialog(this,msg,title,
			JOptionPane.ERROR_MESSAGE);
		return null;	//TBD - should let user try again
	    }
	}
	return installDirFile;
    }

    //Create a Box with space on the right, to make the contained component
    //be left-justified
    private Box hBox(JComponent comp) {
        Box b = Box.createHorizontalBox();
	b.add(comp);
	b.add(Box.createGlue());
	return b;
    }


    //TBD - call this when the user presses the Browse button, then put
    //the result back into the main dialog
    private String browseForInstallDirectory(String defaultInstallDir) {
	JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File(defaultInstallDir));
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        fc.setDialogTitle(
		"Select destination directory for extracting "+appnameCap);
        fc.setMultiSelectionEnabled(false);

	fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (fc.showDialog(ZipSelfExtractor.this, "Select")
	    != JFileChooser.APPROVE_OPTION)
        {
            return null;	//user cancelled
        }

        return fc.getSelectedFile().toString();
    }

    /** Set up the native library we use during installation.
     * This involves the following steps:
     * <ol>
     * <li>Get the tmp dir location from property java.io.tmpdir
     * <li>Extract jshortcut.dll into that directory
     * <li>Set JSHORTCUT_HOME to that directory
     * <li>Instantiate a JShellLink to load the library
     * </ol>
     */
    void setupNativeLibrary(String filename, ZipFile zf)
    		throws IOException, FileNotFoundException {
        String tmpDir = System.getProperty("java.io.tmpdir");
	if (tmpDir==null) {
	    System.out.println("No java.io.tmpdir set");
	    //TBD - error, no tmp directory available
	}

	//Open the entry for our native library
	String versionSuffix = getVersionSuffix(filename);
	String dllName = appname+"-"+versionSuffix+"/jshortcut.dll";
	ZipEntry ze = zf.getEntry(dllName);
	if (ze==null) {
	    String eMsg = "Can't find entry in jar file for "+dllName;
	    throw new RuntimeException(eMsg);
	}
	InputStream in = zf.getInputStream(ze);
	tmpDllFile = new File(tmpDir, "jshortcut.dll");
	FileOutputStream out = new FileOutputStream(tmpDllFile);

	//Copy the DLL from the zip file to the tmp dir
        byte[] buf = new byte[1024];
	while (true)
	{
	    int nRead = in.read(buf, 0, buf.length);
	    if (nRead <= 0)
		break;
	    out.write(buf, 0, nRead);
	}
	out.close();
	//We don't care about the date on the file

        System.setProperty("JSHORTCUT_HOME",tmpDir);
	new JShellLink();	//force the native library to load now
    }

    /** Extract from the specified file.
     * @return True if all was OK, false if there was a problem.
     */
    public boolean extract(String zipfile)
    {
	if (debug) {
	    System.out.println("Trying to extract file "+zipfile);
	}
	File currentArchive = new File(zipfile);

        byte[] buf = new byte[1024];
        SimpleDateFormat formatter =
		new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault());

        ProgressMonitor pm = null;

        boolean overwrite = false;
	boolean stopped = false;

	ZipFile zf = null;
	FileOutputStream out = null;
	InputStream in = null;

        try
        {
	        zf = new ZipFile(currentArchive);

		if (isWindows()) {
		    //extract and set up the native library for installation
		    setupNativeLibrary(zipfile,zf);
		}
		jsClassName = "net/jimmc/jshortcut/JShellName.class";

		String programAndVersion =
			getProgramAndVersion(currentArchive.getName());
		File outputDir = getInstallDir(currentArchive.getName());
		if (outputDir==null)
			return true;		//cancelled

		int size = zf.size();
		int extracted = 0;
		int skipped = 0;
		int fullExtractionCount = size;

		String spaces =
		        "                                        "+
			"                                        ";
		pm = new ProgressMonitor(getParent(),
			"Extracting files...",
			"starting"+spaces,0, size-4);
		pm.setMillisToDecideToPopup(0);
		pm.setMillisToPopup(0);

		Enumeration entries = zf.entries();

		for (int i=0; i<size; i++)
		{
		    ZipEntry entry = (ZipEntry) entries.nextElement();
		    if(entry.isDirectory()) {
			fullExtractionCount--;
			continue;
		    }

		    String pathname = entry.getName();
		    if (pathname.startsWith("net/jimmc/jshortcut") ||
		            pathname.startsWith("net/jimmc/selfunzip") ||
			    pathname.toUpperCase().equals(MANIFEST)) {
			fullExtractionCount--;
			continue;
		    }

                    pm.setProgress(i);
                    pm.setNote(pathname);
		    if(pm.isCanceled()) {
			stopped = true;
			break;
		    }

                    in = zf.getInputStream(entry);

                    File outFile = new File(outputDir, pathname);
		    Date archiveTime = new Date(entry.getTime());

                    if(overwrite==false)
                    {
                        if(outFile.exists())
                        {
                            Object[] options =
			    	{"Yes", "Yes To All", "No", "Cancel"};
                            Date existTime = new Date(outFile.lastModified());
                            Long archiveLen = new Long(entry.getSize());

                            String msg = "File name conflict: "
				+ "There is already a file with "
				+ "that name on the disk!\n"
				+ "\nFile name: " + outFile.getName()
				+ "\nDestination: " + outFile.getPath()
				+ "\nExisting file: "
				+ formatter.format(existTime) + ",  "
				+ outFile.length() + "Bytes"
                                + "\nFile in archive:"
				+ formatter.format(archiveTime) + ",  "
				+ archiveLen + "Bytes"
				+"\n\nWould you like to overwrite the file?";

                            int result = JOptionPane.showOptionDialog(
			        ZipSelfExtractor.this,
				msg, "Warning", JOptionPane.DEFAULT_OPTION,
				JOptionPane.WARNING_MESSAGE, null,
				options,options[0]);

			    if (result==3) {
			    	//Stop the install
				stopped = true;
				break;
			    }

                            if(result == 2) // No, skip this file
                            {
			        skipped++;
                                continue;
                            }
                            else if( result == 1) //YesToAll
                            {
                                overwrite = true;
                            }
                        }
                    }

                    File parent = new File(outFile.getParent());
                    if (parent != null && !parent.exists())
                    {
                        parent.mkdirs();
                    }

                    out = new FileOutputStream(outFile);

                    while (true)
                    {
                        int nRead = in.read(buf, 0, buf.length);
                        if (nRead <= 0)
                            break;
                        out.write(buf, 0, nRead);
                    }
		    extracted ++;

                    out.close();
		    outFile.setLastModified(archiveTime.getTime());
                }

                pm.close();
                zf.close();

		if (tmpDllFile!=null) {
		    tmpDllFile.delete();	//delete tmp file
		}

                //getToolkit().beep();

		String stoppedMsg = "";
		if (stopped) {
		    stoppedMsg = "Stopped, extraction incomplete.\n \n";
		}

		String skippedMsg = "";
		if (skipped>0) {
		    skippedMsg = " and skipped "+skipped+" file"+
		    	((skipped>1)?"s":"");
		}

		String outOfMsg = "";
		if (extracted+skipped<fullExtractionCount) {
		    outOfMsg = " out of "+fullExtractionCount;
		}

		String currentArchiveName = currentArchive.getName();
		String version = getVersionSuffix(currentArchiveName);
		String targetDir = outputDir.getPath()+File.separator+
			appname+"-"+version;
		String jarFileName = targetDir+File.separator+appname+".jar";
		String title = "Installed "+programAndVersion;
		String msg = stoppedMsg +
		     "Extracted " + extracted +
		     " file" + ((extracted != 1) ? "s": "") +
		     skippedMsg + outOfMsg +
		     " from the\n" +
		     zipfile + "\narchive into the\n" +
		     targetDir +
		     "\ndirectory.";
		File jarFile = new File(jarFileName);
		if (jarFile.exists()) {
		    msg += "\n\n"+
			 "The "+appnameCap+" executable JAR file is at\n"+
			 jarFileName;
		}

		//If the extraction was incomplete or if the user asked
		//for stats, show them here.
		if (showStatsP || stopped) {
		    JOptionPane.showMessageDialog
			(ZipSelfExtractor.this,msg,title,
			 JOptionPane.INFORMATION_MESSAGE);
		}

		//Add shortcut and menu on Windows
		if (isWindows() && !stopped && jarFile.exists()) {
		    //Location of client/server batch scripts
		    String serverBatName = targetDir+File.separator+
			    "server.bat";
		    String clientBatName = targetDir+File.separator+
			    "client.bat";
		    String shutdownBatName = targetDir+File.separator+
			    "shutdown.bat";
		    if (addShortcutP) {
			String desktopDir = JShellLink.getDirectory("desktop");
		        JShellLink link = new JShellLink();
			link.setFolder(desktopDir);
			link.setName(appnameCap);
			link.setDescription(programAndVersion);
			link.setPath(jarFileName);
			link.save();	//create the shortcut
			if (addClientP) {
			    link = new JShellLink();
			    link.setFolder(desktopDir);
			    link.setName(appnameCap+" Server");
			    link.setDescription(programAndVersion+" Server");
			    link.setPath(serverBatName);
			    link.save();

			    link = new JShellLink();
			    link.setFolder(desktopDir);
			    link.setName(appnameCap+" Client");
			    link.setDescription(programAndVersion+" Client");
			    link.setPath(clientBatName);
			    link.save();

			    link = new JShellLink();
			    link.setFolder(desktopDir);
			    link.setName(appnameCap+" Shutdown");
			    link.setDescription(programAndVersion+" Shutdown");
			    link.setPath(shutdownBatName);
			    link.save();
			}
		    }
		    if (addMenuP) {
			String programs = JShellLink.getDirectory("programs");
			String jmenu = programs+"\\"+appnameCap;
			File pf = new File(jmenu);
			if (!pf.exists()) {
			    if (!pf.mkdir()) {
				throw new RuntimeException("Can't create menu");
			    }
			}
			JShellLink link = new JShellLink();
			link.setFolder(jmenu);
			link.setName(appnameCap);
			link.setDescription(programAndVersion);
			link.setPath(jarFileName);
			link.save();	//create the menu item
			if (addClientP) {
			    link = new JShellLink();
			    link.setFolder(jmenu);
			    link.setName(appnameCap+" Server");
			    link.setDescription(programAndVersion+" Server");
			    link.setPath(serverBatName);
			    link.save();

			    link = new JShellLink();
			    link.setFolder(jmenu);
			    link.setName(appnameCap+" Client");
			    link.setDescription(programAndVersion+" Client");
			    link.setPath(clientBatName);
			    link.save();

			    link = new JShellLink();
			    link.setFolder(jmenu);
			    link.setName(appnameCap+" Shutdown");
			    link.setDescription(programAndVersion+" Shutdown");
			    link.setPath(shutdownBatName);
			    link.save();
			}
		    }
		}

		//Start app if all conditions are met
		if (autoStartP && !stopped && jarFile.exists()) {
		    pm = new ProgressMonitor(getParent(),
			"Starting "+appnameCap+", please wait...",
			" ",0,100);
		    pm.setMillisToDecideToPopup(0);
		    pm.setMillisToPopup(0);
                    pm.setProgress(1);
                    pm.setNote(" ");
		    startJar(jarFileName);
		    try {
		        Thread.sleep(5*1000);	//sleep a few seconds
				//On Windows, when the installer exits,
				//the window from which it was invoked
				//(e.g. DOS window or File Manager)
				//is raised, hiding the app startup
				//screen.  This is annoying.  Keep the
				//sleep time here low so that on a slow
				//machine we will be gone before the
				//app startup screen appears, and
				//thus we won't have covered it up as
				//the slow machine plods along.
		    } catch (Exception ex) {}
		    pm.close();
		}
	}
	catch (Exception e)
	{
	    if (System.getProperty(appname+".install.debug")!=null) {
	        e.printStackTrace(System.out);
	    } else {
	        System.out.println(e);
	    }
	    if(zf!=null) { try { zf.close(); } catch(IOException ioe) {;} }
	    if(out!=null) { try {out.close();} catch(IOException ioe) {;} }
	    if(in!=null) { try { in.close(); } catch(IOException ioe) {;} }
	    return false;	//got an error
	}
	return true;		//no errors
    }

    public void startJar(String jar) {
	String cmd;
	Runtime rt = Runtime.getRuntime();
	if (isWindows())
	    cmd = "\""+getJavawProg()+"\" -jar \""+jar+"\"";
	else
	    cmd = getJavaProg()+" -jar "+jar;
	try {
	    rt.exec(cmd);
	} catch (IOException ex) {
	    JOptionPane.showMessageDialog(this,"Can't run "+cmd,
	    	"Error Running Java",JOptionPane.ERROR_MESSAGE);
	}
	//Don't wait for it to finish, we just quietly exit now
    }

    /** Get the path to the java program. */
    public String getJavaProg() {
	String sep = File.separator;
	return System.getProperty("java.home")+sep+"bin"+sep+"java";
    }

    /** Get the path to the javaw program. */
    public String getJavawProg() {
	String sep = File.separator;
	return System.getProperty("java.home")+sep+"bin"+sep+"javaw";
    }
}
