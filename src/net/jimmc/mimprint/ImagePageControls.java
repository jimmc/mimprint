/* ImagePageControls.java
 *
 * Jim McBeath, October 25, 2005
 */

package jimmc.jiviewer;

import jimmc.swing.ComboBoxAction;
import jimmc.swing.JsTextField;

import java.awt.FlowLayout;
import java.awt.Point;
import java.util.Vector;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ImagePageControls extends JPanel {
    private App app;
    private ImagePage imagePage;

    private ComboBoxAction areaChoiceField;
    private JLabel widthLabel;
    private JsTextField widthField;
    private JLabel heightLabel;
    private JsTextField heightField;
    private JLabel unitsLabel;
    private ComboBoxAction unitsField;
    private JLabel rowCountLabel;
    private JsTextField rowCountField;
    private JLabel columnCountLabel;
    private JsTextField columnCountField;
    private JLabel splitOrientationLabel;
    private ComboBoxAction splitOrientationField;
    private JLabel splitPercentLabel;
    private JsTextField splitPercentField;
    private JLabel marginsLabel;
    private JsTextField marginsField;
    private JLabel spacingLabel;
    private JsTextField spacingField;
    private JLabel layoutLabel;
    private ComboBoxAction layoutField;

    private AreaLayout[] selectedAreas;

    public ImagePageControls(App app, ImagePage imagePage) {
        this.app = app;
        this.imagePage = imagePage;
        initLayout();
    }

    private void initLayout() {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        addFields();
        selectedAreas = new AreaLayout[0]; //no areas selected
        updateAreaChoiceField(false);
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
            //These match the values of UNIT_CM and UNIT_INCH in ImagePage.
        unitsField.setItems(initialUnitsItems);
        add(unitsField);

        //Set up the Grid fields
        rowCountLabel = makeLabel("Rows");
        add(rowCountLabel);
        rowCountField = new JsTextField(2) {
            public void action() {
                setRowColumnCount();
            }
        };
        add(rowCountField);

        columnCountLabel = makeLabel("Columns");
        add(columnCountLabel);
        columnCountField = new JsTextField(2) {
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
        splitPercentField = new JsTextField(4) {
            public void action() {
                String s = getText();
                int n = Integer.parseInt(s);
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

    /** Here when the user clicks in the ImagePage window to select an area.
     * @param point A point in our coordinate system.
     */
    public void selectArea(Point point) {
        System.out.println("ImagePageControl.selectArea("+point+")");
        AreaLayout aa = imagePage.getAreaLayout();
        if (!aa.hit(point)) {
            System.out.println("Not in top-level areaLayout");
            selectedAreas = new AreaLayout[0]; //no areas selected
            updateAreaChoiceField(false);
            return;
        }
        Vector v = new Vector();
        do {
            System.out.println("Add area "+aa);
            v.addElement(aa);
            aa = aa.getArea(point);
        } while (aa!=null);
        System.out.println("Area list size: "+v.size());
        selectedAreas = new AreaLayout[v.size()];
        v.copyInto(selectedAreas);
        updateAreaChoiceField(false);
    }

    //After updating the selectedAreas array, update the areaChoiceField
    //to match it and set the selected item to be the last one
    //in the list.
    private void updateAreaChoiceField(boolean keepSelection) {
        int oldSelectionIndex = areaChoiceField.getSelectedIndex();
        int numChoices = 1+selectedAreas.length;
        String[] areaChoiceStrs = new String[numChoices];
        areaChoiceStrs[0] = "1. Page";     //TODO i18n
        for (int i=0; i<selectedAreas.length; i++) {
            areaChoiceStrs[i+1] = Integer.toString(i+2)+". "+
                    getAreaTypeString(i+1);
                    //TODO - add some info such as bounding box?
        }
        areaChoiceField.setItems(areaChoiceStrs);
        int newSelectionIndex;
        if (keepSelection)
            newSelectionIndex = oldSelectionIndex;
        else
            newSelectionIndex = areaChoiceStrs.length - 1;
        areaChoiceField.setSelectedIndex(newSelectionIndex);
                //select the last (or same) item in the list,
                //this also calls the action method which
                //calls areaSelected(int).
    }

    //Here when the user selected the units for the page.
    private void unitsSelected(int index) {
        System.out.println("Setting units to "+index);
        imagePage.setPageUnit(index);
    }

    //Here when the user selects an item from the area choice list,
    //or when we call setSelectedIndex on it.
    private void areaSelected(int index) {
        System.out.println("selection: "+index);
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
            widthField.setText(Integer.toString(imagePage.getPageWidth()));
            heightField.setText(Integer.toString(imagePage.getPageHeight()));
            unitsField.setSelectedIndex(imagePage.getPageUnit());
        } else {
            AreaLayout area = selectedAreas[index-1];
            marginsField.setText(Integer.toString(area.getMargin()));
            spacingField.setText(Integer.toString(area.getSpacing()));
            switch (areaType) {
            case AREA_IMAGE:
                layoutField.setSelectedIndex(0);        //TODO define constant
                break;
            case AREA_GRID:
                AreaGridLayout gridArea = (AreaGridLayout)area;
                layoutField.setSelectedIndex(1);        //TODO define constant
                rowCountField.setText(
                        Integer.toString(gridArea.getRowCount()));
                columnCountField.setText(
                        Integer.toString(gridArea.getColumnCount()));
                break;
            case AREA_SPLIT:
                AreaSplitLayout splitArea = (AreaSplitLayout)area;
                layoutField.setSelectedIndex(2);        //TODO define constant
                splitOrientationField.setSelectedIndex(
                        splitArea.getOrientation());
                splitPercentField.setText(
                        Integer.toString(splitArea.getSplitPercentage()));
                break;
            }
        }
    }

    private static final int AREA_PAGE = 0;
    private static final int AREA_IMAGE = 1;
    private static final int AREA_GRID = 2;
    private static final int AREA_SPLIT = 3;
    //Get the type of the area specified by the given index
    private int getAreaType(int index) {
        if (index==0)
            return AREA_PAGE;
        AreaLayout a = selectedAreas[index-1];
        //TODO - the following probably should just be an overridden method
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
        System.out.println("Selected layout: "+layoutTypeIndex);
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
        //If we are replacing a simple image and its spacing
        //was zero, put in something which we think will be
        //a better default value.
        if (newArea.getSpacing()==0 && area instanceof ImagePageArea) {
            if (selectedIndex==1)
                newArea.setSpacing(10*area.getBorderThickness());
            else {
                AreaLayout parentArea = selectedAreas[selectedIndex-2];
                newArea.setSpacing(parentArea.getSpacing());
            }
        }
        newArea.revalidate();   //recalculate bounds etc
        if (selectedIndex==1) {
            //top level area, contained in page
            imagePage.setAreaLayout(newArea);
        } else {
            AreaLayout parentArea = selectedAreas[selectedIndex-2];
            parentArea.replaceArea(area,newArea);
        }
        selectedAreas[selectedIndex-1] = newArea;
        updateAreaChoiceField(true);    //fix label in area choice list
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
        return selectedAreas[index-1];
    }

    /** Throw an exception if the Page is not currently selected. */
    private void assertPageSelected() {
        int index = areaChoiceField.getSelectedIndex();
        if (index!=0)
            throw new RuntimeException("Page not selected");
    }

    private void setPageWidth(String s) {
        assertPageSelected();
        int width = Integer.parseInt(s);
        imagePage.setPageWidth(width);
        imagePage.repaint();
    }

    private void setPageHeight(String s) {
        assertPageSelected();
        int height = Integer.parseInt(s);
        imagePage.setPageHeight(height);
        imagePage.repaint();
    }

    private void setMargins(String marginStr) {
        int margin = Integer.parseInt(marginStr);
            //TODO - allow specifying 4 margins separated by commas?
        AreaLayout a = getSelectedArea();
        a.setMargin(margin);
        a.revalidate();
        imagePage.repaint();
    }

    private void setSpacing(String spacingStr) {
        int spacing = Integer.parseInt(spacingStr);
            //TODO - allow specifying two spacings separated by commas
        AreaLayout a = getSelectedArea();
        a.setSpacing(spacing);
        a.revalidate();
        imagePage.repaint();
    }

    /** Read the row and column values from the text fields
     * and set those numbers on the grid layout. */
    private void setRowColumnCount() {
        String s = rowCountField.getText();
        int rowCount = Integer.parseInt(s);
        s = columnCountField.getText();
        int columnCount = Integer.parseInt(s);
        AreaLayout a = getSelectedArea();
        if (a instanceof AreaGridLayout) {
            ((AreaGridLayout)a).setRowColumnCounts(rowCount,columnCount);
            a.revalidate();
            imagePage.repaint();
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
