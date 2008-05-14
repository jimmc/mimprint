package net.jimmc.mimprint;

import net.jimmc.swing.ButtonAction;
import net.jimmc.swing.CheckBoxAction;
import net.jimmc.swing.ComboBoxAction;
import net.jimmc.swing.GridBagger;
import net.jimmc.swing.JsFrame;
import net.jimmc.swing.JsTextField;

import java.io.File;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/** Deal with the commands in the PlayList menu. */
public class PlayListManager {
    private App app;
    private Viewer viewer;
    private ComboBoxAction playListSourceField;
    private JsTextField fileNameField;
    private CheckBoxAction appendField;

    public PlayListManager(App app, Viewer viewer) {
        this.app = app;
        this.viewer = viewer;
    }

    /** Put up a dialog allowing the user to load a file into a playlist. */
    public void load() {
        String title = app.getResourceString("dialog.PlayList.Load.title");
        JComponent dialogPanel = createLoadDialogPanel();
        boolean ok = showOptionDialog(dialogPanel,viewer,title);
        if (!ok)
            return;     //canceled

        int playListIndex = playListSourceField.getSelectedIndex();
        String fileName = fileNameField.getText();
        boolean appendFlag = appendField.isSelected();

        load(playListIndex, fileName, appendFlag);
    }

    /** Put up a dialog allowing the user to save a playlist to a file. */
    public void save() {
        String title = app.getResourceString("dialog.PlayList.Save.title");
        JComponent dialogPanel = createSaveDialogPanel();
        boolean ok = showOptionDialog(dialogPanel,viewer,title);
        if (!ok)
            return;     //canceled

        int playListIndex = playListSourceField.getSelectedIndex();
        String fileName = fileNameField.getText();

        save(playListIndex, fileName);
    }

    private void load(int playListIndex, String fileName, boolean appendFlag) {
        System.out.println("NYI: load PL="+playListIndex+
                " file="+fileName+" append="+appendFlag);
    }

    private void save(int playListIndex, String fileName) {
        if (fileName==null || fileName.equals(""))
            throw new IllegalArgumentException("No filename specified");
        if (playListIndex<0 || playListIndex>2)
            throw new IllegalArgumentException(
                    "Bad PlayList index "+playListIndex);
        File f = new File(fileName);
        if (f.exists()) {
            String prompt = app.getResourceFormatted(
                    "query.Confirm.FileExists.prompt",fileName);
            if (!viewer.confirmDialog(prompt))
                return;         //not confirmed, abort
        }
        switch (playListIndex) {
        case 0:
            viewer.saveMainPlayList(fileName);
            break;
        case 1:
            viewer.savePrintablePlayList(fileName);
            break;
        //case 2 and above are for aux playlists
        default:
            System.out.println("NYI: save PL="+playListIndex+" file="+fileName);
            break;
        }
    }

    private JComponent createLoadDialogPanel() {
        String prefix = "dialog.PlayList.Load.";
        JPanel p = new JPanel();
        GridBagger gb = new GridBagger(p);

        addPlayListRow(gb);
        addFileNameRow(gb,false);

        //Allow user to request that the contents of the file be added to the
        //current playlist, rather than replacing the current contents.
        gb.add(new JLabel(app.getResourceString(prefix+"label.Append")));
        gb.add(appendField=new CheckBoxAction(app,prefix+"field.Append",null));

        return p;
    }

    private JComponent createSaveDialogPanel() {
        String prefix = "dialog.PlayList.Save.";
        JPanel p = new JPanel();
        GridBagger gb = new GridBagger(p);

        addPlayListRow(gb);
        addFileNameRow(gb,false);

        return p;
    }

    private void addPlayListRow(GridBagger gb) {
        String prefix = "dialog.PlayList.LoadSave.";
        //First row allows user to select which PlayList to load to
        gb.add(new JLabel(app.getResourceString(prefix+"label.PlayList")));
        gb.add(playListSourceField=getPlayListChoiceList());
        gb.nextRow();
    }

    private void addFileNameRow(GridBagger gb, final boolean isSave) {
        String prefix = "dialog.PlayList.LoadSave.";
        //Second row allows user to specify the file from which to load
        gb.add(new JLabel(app.getResourceString(prefix+"label.File")));
        gb.add(fileNameField=new JsTextField(40));
        gb.add(new ButtonAction(app,prefix+"button.Browse") {
            public void action() {
                browseAction(isSave);
            }
        });
        gb.nextRow();
    }

    //True if the user pushed the OK button, false otherwise.
    private boolean showOptionDialog(JComponent dialogPanel,
            JsFrame parent, String title) {
        JOptionPane pane = new JOptionPane(dialogPanel,
            JOptionPane.PLAIN_MESSAGE,
            JOptionPane.OK_CANCEL_OPTION);
        JDialog dlg = pane.createDialog(parent,title);
        dlg.setResizable(true);
        pane.setInitialValue(null);
        pane.selectInitialValue();
        /*
        //Use invokeLater to transfor focus to a specific component
        //within our dialog (see bug 4222534).
        dlg.addWIndowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                SwingUtilities.invokeLater(new Runnable()) {
                    public void run() {
                        textArea.requestFocus();
                    }
                }
            }
        });
        */
        dlg.show();             //display dialog and get user's input

        Object v = pane.getValue();
        if (!(v instanceof Integer))
            return false;        //CLOSED_OPTION
        int n = ((Integer)v).intValue();
        if (n==JOptionPane.NO_OPTION | n==JOptionPane.CANCEL_OPTION)
            return false;        //canceled
        return true;            //user pushed OK
    }

    private ComboBoxAction getPlayListChoiceList() {
        ComboBoxAction cb = new ComboBoxAction(app);
        String[] choiceNames = { "Main", "Printable" };
        cb.setItems(choiceNames);
        return cb;
    }

    private void browseAction(boolean isSave) {
        String modeName = isSave?"Save":"Load";
        String title = app.getResourceString(
                "dialog.PlayList.FileBrowser."+modeName+".title");
        String prompt = app.getResourceString(
                "dialog.PlayList.FileBrowser."+modeName+".prompt");
        String defaultLocation = null;  //TODO
        JFileChooser chooser = new JFileChooser(defaultLocation);
        chooser.setDialogTitle(title);
        int result = isSave?
                chooser.showSaveDialog(viewer):chooser.showOpenDialog(viewer);
        if (result!=JFileChooser.APPROVE_OPTION)
            return;             //canceled, no action
        File f = chooser.getSelectedFile();
        if (f==null)
            return;
        fileNameField.setText(f.getPath());
    }
}
