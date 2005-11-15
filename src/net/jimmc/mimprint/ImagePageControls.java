/* ImagePageControls.java
 *
 * Jim McBeath, October 25, 2005
 */

package jimmc.jiviewer;

import jimmc.swing.ComboBoxAction;
import jimmc.swing.JsSpinner;
import jimmc.swing.JsTextField;

import java.awt.FlowLayout;
import java.awt.Point;
import java.text.NumberFormat;
import java.util.Vector;
import javax.accessibility.Accessible;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.SpinnerNumberModel;

public class ImagePageControls extends JPanel {
    private App app;
    private ImagePage imagePage;
    private boolean updatingSelected;

    private ComboBoxAction areaChoiceField;
    private JList areaChoicePopupList;
    private JLabel widthLabel;
    private JsTextField widthField;
    private JLabel heightLabel;
    private JsTextField heightField;
    private JLabel unitsLabel;
    private ComboBoxAction unitsField;
    private JLabel rowCountLabel;
    private JsSpinner rowCountField;
    private JLabel columnCountLabel;
    private JsSpinner columnCountField;
    private JLabel splitOrientationLabel;
    private ComboBoxAction splitOrientationField;
    private JLabel splitPercentLabel;
    private JsSpinner splitPercentField;
    private JLabel marginsLabel;
    private JsTextField marginsField;
    private JLabel spacingLabel;
    private JsTextField spacingField;
    private JLabel layoutLabel;
    private ComboBoxAction layoutField;

    private AreaLayout[] allAreas;

    public ImagePageControls(App app, ImagePage imagePage) {
        this.app = app;
        this.imagePage = imagePage;
        initLayout();
    }

    private void initLayout() {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        addFields();
        updateAllAreasList();
        updateAreaChoiceField(null);
    }

    /** Create and add all of our fields. */
    private void addFields() {
        String labelText;

        areaChoiceField = new ComboBoxAction(app) {
            public void action() {
                areaSelected(getSelectedIndex());
            }
        };
        add(areaChoiceField);
        addAreaValueChangedListener();

        //Set up the Page fields
        widthLabel = makeLabel("Width");
        add(widthLabel);
        widthField = new JsTextField(4) {
            public void action() {
                setPageWidth(getText());
            }
        };
        add(widthField);

        heightLabel = makeLabel("Height");
        add(heightLabel);
        heightField = new JsTextField(4) {
            public void action() {
                setPageHeight(getText());
            }
        };
        add(heightField);

        unitsLabel = makeLabel("Units");
        add(unitsLabel);
        unitsField = new ComboBoxAction(app) {
            public void action() {
                unitsSelected(getSelectedIndex());
            }
        };
        Object[] initialUnitsItems = { "cm", "in" };
            //These match the values of PageLayout.UNIT_CM and PageLayout.UNIT_INCH in ImagePage.
        unitsField.setItems(initialUnitsItems);
        add(unitsField);

        //Set up the Grid fields
        rowCountLabel = makeLabel("Rows");
        add(rowCountLabel);
        SpinnerNumberModel rowCountModel = new SpinnerNumberModel(1,1,99,1);
        rowCountField = new JsSpinner(rowCountModel) {
            public void action() {
                setRowColumnCount();
            }
        };
        add(rowCountField);

        columnCountLabel = makeLabel("Columns");
        add(columnCountLabel);
        SpinnerNumberModel columnCountModel = new SpinnerNumberModel(1,1,99,1);
        columnCountField = new JsSpinner(columnCountModel) {
            public void action() {
                setRowColumnCount();
            }
        };
        add(columnCountField);

        //Set up the Split fields
        splitOrientationLabel = makeLabel("SplitOrientation");
        add(splitOrientationLabel);
        splitOrientationField = new ComboBoxAction(app) {
            public void action() {
                splitOrientationSelected(getSelectedIndex());
            }
        };
        Object[] initialOrientationItems = { "V", "H" };     //TODO i18n
            //These values match the VERTICAL and HORIZONTAL values
            //in AreaSplitLayout.
        splitOrientationField.setItems(initialOrientationItems);
        add(splitOrientationField);

        splitPercentLabel = makeLabel("SplitPercent");
        add(splitPercentLabel);
        SpinnerNumberModel percentModel = new SpinnerNumberModel(5,1,99,5);
        splitPercentField = new JsSpinner(percentModel) {
            public void action() {
                int n = ((Integer)getValue()).intValue();
                setSplitPercentage(n);
            }
        };
        add(splitPercentField);

        marginsLabel = makeLabel("Margins");
        add(marginsLabel);
        marginsField = new JsTextField(4) {
            public void action() {
                setMargins(getText());
            }
        };
        add(marginsField);

        spacingLabel = makeLabel("Spacing");
        add(spacingLabel);
        spacingField = new JsTextField(4) {
            public void action() {
                setSpacing(getText());
            }
        };
        add(spacingField);

        //Add the layout choice
        layoutLabel = makeLabel("Layout");
        add(layoutLabel);
        layoutField = new ComboBoxAction(app) {
            public void action() {
                layoutSelected(getSelectedIndex());
            }
        };
        Object[] initialLayoutItems = { "I", "G", "S" };     //TODO i18n
                //TODO define constants
        layoutField.setItems(initialLayoutItems);
        add(layoutField);
    }

    //Make a label, add tooltip if defined, using resources
    private JLabel makeLabel(String key) {
        String labelText = app.getResourceString("toolbar.Layout."+key+".label");
        JLabel label = new JLabel(labelText);
        String toolTipTextKey = "toolbar.Layout."+key+".toolTip";
        String toolTipText = app.getResourceString(toolTipTextKey);
        if (toolTipText!=null && !toolTipText.equals(toolTipTextKey))
            label.setToolTipText(toolTipText);
        return label;
    }

    //Add a listener to the list on the ComboBox so that we can get
    //notified as the user scrolls through the list.
    private void addAreaValueChangedListener() {
        Accessible a = areaChoiceField.getUI().getAccessibleChild(areaChoiceField,0);
        if (a instanceof ComboPopup) {
            areaChoicePopupList = ((ComboPopup)a).getList();
            areaChoicePopupList.addListSelectionListener(new AreaListSelectionListener());
        }
        //TODO also add a PopupMenuListener so that we known when the popup
        //goes away so that we can reset the highlight in the event the user
        //cancels the area choice selection.
    }
    class AreaListSelectionListener implements ListSelectionListener {
        //As the user scrolls through the list, highlight the area that
        //corresponds to the selected item in the list.
        public void valueChanged(ListSelectionEvent ev) {
            int index = areaChoicePopupList.getSelectedIndex();
            //int first = ev.getFirstIndex();
            //int last = ev.getLastIndex();
//System.out.println("area sel="+index+", first="+first+", last="+last);
            if (index<=0)
                imagePage.setHighlightedArea(null);
            else
                imagePage.setHighlightedArea(allAreas[index-1]);
        }
    }

    /** Here the the page layout changes, to update our list of all areas. */
    public void updateAllAreasList() {
        AreaLayout a = imagePage.getAreaLayout();
        Vector v = new Vector();
        v.addElement(a);       //put the top item into the list
        a.getAreaList(v);     //put all children into the list
        allAreas = new AreaLayout[v.size()];
        v.copyInto(allAreas);
    }

    /** Here when the user clicks in the ImagePage window to select an area.
     * @param point A point in our coordinate system.
     */
    public void selectArea(Point point) {
//System.out.println("ImagePageControl.selectArea("+point+")");
        AreaLayout aa = imagePage.getAreaLayout();
        if (!aa.hit(point)) {
//System.out.println("Not in top-level areaLayout");
            updateAreaChoiceField(null);
            return;
        }
        AreaLayout selectedArea;
        do {
//System.out.println("Add area "+aa);
            selectedArea = aa;
            aa = aa.getArea(point);
        } while (aa!=null);
//System.out.println("Area list size: "+v.size());
        updateAreaChoiceField(selectedArea);
    }

    //After updating the allAreas array, update the areaChoiceField
    //to match it and set the selected item to the specified area.
    private void updateAreaChoiceField(AreaLayout selectedArea) {
        int numChoices = 1+allAreas.length;
        String[] areaChoiceStrs = new String[numChoices];
        areaChoiceStrs[0] = "0. Page";     //TODO i18n
        for (int i=0; i<allAreas.length; i++) {
            String treeLocation = allAreas[i].getTreeLocation();
            if (treeLocation==null)
                treeLocation = "?";
            areaChoiceStrs[i+1] =
                    Integer.toString(allAreas[i].getTreeDepth()+1)+
                    treeLocation+". "+
                    getAreaTypeString(i+1);
        }
        areaChoiceField.setItems(areaChoiceStrs);
        if (selectedArea==null)
            areaChoiceField.setSelectedIndex(0);  //select the page
        else {
            for (int i=0; i<allAreas.length; i++) {
                if (allAreas[i]==selectedArea) {
                    areaChoiceField.setSelectedIndex(i+1);
                        //select the specified item,
                        //this also calls the action method which
                        //calls areaSelected(int).
                    break;
                }
            }
        }
    }

    //Here when the user selected the units for the page.
    private void unitsSelected(int index) {
//System.out.println("Setting units to "+index);
        imagePage.setPageUnit(index);
    }

    //Here when the user selects an item from the area choice list,
    //or when we call setSelectedIndex on it.
    private void areaSelected(int index) {
//System.out.println("selection: "+index);
        if (index<0)
            index = 0;  //by default select the page
        boolean pageSelected = false;
        boolean gridSelected = false;
        boolean splitSelected = false;
        boolean simpleSelected = false;
        int areaType = getAreaType(index);
        switch (areaType) {
        case AREA_PAGE:
            pageSelected = true;
            break;
        case AREA_IMAGE:
            simpleSelected = true;
            break;
        case AREA_GRID:
            gridSelected = true;
            break;
        case AREA_SPLIT:
            splitSelected = true;
            break;
        default:
            throw new IllegalArgumentException("bad area type: "+areaType);
        }
        updatingSelected = true;

        //set the visibility on our fields and labels
        widthLabel.setVisible(pageSelected);
        widthField.setVisible(pageSelected);
        heightLabel.setVisible(pageSelected);
        heightField.setVisible(pageSelected);
        unitsLabel.setVisible(false);   //not used
        unitsField.setVisible(pageSelected);
        marginsLabel.setVisible(!pageSelected);
        marginsField.setVisible(!pageSelected);
        rowCountLabel.setVisible(gridSelected);
        rowCountField.setVisible(gridSelected);
        columnCountLabel.setVisible(gridSelected);
        columnCountField.setVisible(gridSelected);
        splitOrientationLabel.setVisible(splitSelected);
        splitOrientationField.setVisible(splitSelected);
        splitPercentLabel.setVisible(splitSelected);
        splitPercentField.setVisible(splitSelected);
        spacingLabel.setVisible(gridSelected||splitSelected);
        spacingField.setVisible(gridSelected||splitSelected);
        layoutLabel.setVisible(!pageSelected);
        layoutField.setVisible(!pageSelected);

        if (index==0) {
            //Page fields
            widthField.setText(formatPageValue(imagePage.getPageWidth()));
            heightField.setText(formatPageValue(imagePage.getPageHeight()));
            unitsField.setSelectedIndex(imagePage.getPageUnit());
            imagePage.setHighlightedArea(null);
        } else {
            AreaLayout area = allAreas[index-1];
            imagePage.setHighlightedArea(area);
            marginsField.setText(formatPageValue(area.getMargin()));
            spacingField.setText(formatPageValue(area.getSpacing()));
            switch (areaType) {
            case AREA_IMAGE:
                layoutField.setSelectedIndex(0);        //TODO define constant
                break;
            case AREA_GRID:
                AreaGridLayout gridArea = (AreaGridLayout)area;
                layoutField.setSelectedIndex(1);        //TODO define constant
                rowCountField.setValue(
                        new Integer(gridArea.getRowCount()));
                columnCountField.setValue(
                        new Integer(gridArea.getColumnCount()));
                break;
            case AREA_SPLIT:
                AreaSplitLayout splitArea = (AreaSplitLayout)area;
                layoutField.setSelectedIndex(2);        //TODO define constant
                splitOrientationField.setSelectedIndex(
                        splitArea.getOrientation());
                splitPercentField.setValue(
                        new Integer(splitArea.getSplitPercentage()));
                break;
            }
        }
        updatingSelected = false;
    }

    //Given a page dimension, format it for display to the user
    protected String formatPageValue(int n) {
        return imagePage.formatPageValue(n);
    }

    private static final int AREA_PAGE = 0;
    private static final int AREA_IMAGE = 1;
    private static final int AREA_GRID = 2;
    private static final int AREA_SPLIT = 3;
    //Get the type of the area specified by the given index
    private int getAreaType(int index) {
        if (index==0)
            return AREA_PAGE;
        AreaLayout a = allAreas[index-1];
        //TODO - make an AreaLayout method to get type int
        if (a instanceof ImagePageArea)
            return AREA_IMAGE;
        if (a instanceof AreaGridLayout)
            return AREA_GRID;
        if (a instanceof AreaSplitLayout)
            return AREA_SPLIT;
        throw new IllegalArgumentException("unknown instance type "+a);
    }

    private String getAreaTypeString(int index) {
        int areaType = getAreaType(index);
        switch (areaType) {
        case AREA_PAGE:
            return "Page";      //TODO i18n
        case AREA_IMAGE:
            return "Image";     //TODO i18n
        case AREA_GRID:
            return "Grid";      //TODO i18n
        case AREA_SPLIT:
            return "Split";      //TODO i18n
        default:
            throw new IllegalArgumentException("bad area type: "+areaType);
        }
    }

    private void layoutSelected(int layoutTypeIndex) {
//System.out.println("Selected layout: "+layoutTypeIndex);
        AreaLayout area = getSelectedArea();
        int selectedIndex = areaChoiceField.getSelectedIndex();
        int areaType = getAreaType(selectedIndex);
        int newAreaType = layoutIndexToAreaType(layoutTypeIndex);
        if (newAreaType==areaType)
            return;             //no change
        AreaLayout newArea;
        switch (newAreaType) {
        case AREA_IMAGE:
            newArea = new ImagePageArea(0,0,0,0);
            break;
        case AREA_GRID:
            AreaGridLayout newGridArea = new AreaGridLayout();
            newGridArea.setRowColumnCounts(2,2);
            newArea = newGridArea;
            break;
        case AREA_SPLIT:
            newArea = new AreaSplitLayout();
            break;
        default:
            throw new RuntimeException("bad newAreaType "+newAreaType);
        }
        //Copy some atttributes from old area to new area
        newArea.setBorderThickness(area.getBorderThickness());
        newArea.setMargin(area.getMargin());
        newArea.setBounds(area.getBounds());
        newArea.setSpacing(area.getSpacing());
        AreaLayout parentArea = area.getParent();
        //If we are replacing a simple image and its spacing
        //was zero, put in something which we think will be
        //a better default value.
        if (newArea.getSpacing()==0 && area instanceof ImagePageArea) {
            if (parentArea==null)
                newArea.setSpacing(10*area.getBorderThickness());
            else
                newArea.setSpacing(parentArea.getSpacing());
        }
        newArea.revalidate();   //recalculate bounds etc
        if (parentArea==null) {
            //top level area, contained in page
            imagePage.setAreaLayout(newArea);
        } else {
            parentArea.replaceArea(area,newArea);
        }
        updateAllAreasList();
        updateAreaChoiceField(newArea);    //fix area choice list
        areaSelected(areaChoiceField.getSelectedIndex()); //fix area property fields
        imagePage.repaint();
    }

    private int layoutIndexToAreaType(int index) {
        int[] types = { AREA_IMAGE, AREA_GRID, AREA_SPLIT };
            //this must match the initialLayoutItems
        return types[index];
    }

    /** Get the currently selected area. */
    private AreaLayout getSelectedArea() {
        int index = areaChoiceField.getSelectedIndex();
        if (index<=0)
            throw new RuntimeException("no AreaLayout selected");
        return allAreas[index-1];
    }

    /** Throw an exception if the Page is not currently selected. */
    private void assertPageSelected() {
        int index = areaChoiceField.getSelectedIndex();
        if (index!=0)
            throw new RuntimeException("Page not selected");
    }

    private void setPageWidth(String s) {
        assertPageSelected();
        double width = Double.parseDouble(s);
        int w = (int)(width*PageLayout.UNIT_MULTIPLIER);
        imagePage.setPageWidth(w);
        imagePage.repaint();
    }

    private void setPageHeight(String s) {
        assertPageSelected();
        double height = Double.parseDouble(s);
        int h = (int)(height*PageLayout.UNIT_MULTIPLIER);
        imagePage.setPageHeight(h);
        imagePage.repaint();
    }

    private void setMargins(String marginStr) {
        double margin = Double.parseDouble(marginStr);
        int d = (int)(margin*PageLayout.UNIT_MULTIPLIER);
            //TODO - allow specifying 4 margins separated by commas?
        AreaLayout a = getSelectedArea();
        a.setMargin(d);
        a.revalidate();
        imagePage.repaint();
    }

    private void setSpacing(String spacingStr) {
        double spacing = Double.parseDouble(spacingStr);
        int sp = (int)(spacing*PageLayout.UNIT_MULTIPLIER);
            //TODO - allow specifying two spacings separated by commas
        AreaLayout a = getSelectedArea();
        a.setSpacing(sp);
        a.revalidate();
        imagePage.repaint();
    }

    /** Read the row and column values from the text fields
     * and set those numbers on the grid layout. */
    private void setRowColumnCount() {
        if (updatingSelected)
            return;
        int rowCount = ((Integer)rowCountField.getValue()).intValue();
        int columnCount = ((Integer)columnCountField.getValue()).intValue();
        AreaLayout a = getSelectedArea();
        if (a instanceof AreaGridLayout) {
            ((AreaGridLayout)a).setRowColumnCounts(rowCount,columnCount);
            a.revalidate();
            imagePage.repaint();
            //When number of rows or columns changes, that changes our
            //list of areas.
            updateAllAreasList();
            updateAreaChoiceField(a);
        }
    }

    private void splitOrientationSelected(int index) {
        AreaLayout a = getSelectedArea();
        if (a instanceof AreaSplitLayout) {
            ((AreaSplitLayout)a).setOrientation(index);
            a.revalidate();
            imagePage.repaint();
        }
    }

    private void setSplitPercentage(int splitPercent) {
        AreaLayout a = getSelectedArea();
        if (a instanceof AreaSplitLayout) {
            ((AreaSplitLayout)a).setSplitPercentage(splitPercent);
            a.revalidate();
            imagePage.repaint();
        }
    }
}
