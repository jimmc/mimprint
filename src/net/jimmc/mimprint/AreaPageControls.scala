/* AreaPageControls.java
 *
 * Jim McBeath, June 16, 2008
 */

package net.jimmc.mimprint

import net.jimmc.swing.SComboBox
import net.jimmc.swing.SFrame
import net.jimmc.swing.SLabel
import net.jimmc.swing.SSpinner
import net.jimmc.swing.STextField

import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Insets
import java.awt.Point
import java.util.Vector
import javax.accessibility.Accessible
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.plaf.basic.ComboPopup
import javax.swing.SpinnerNumberModel

class AreaPageControls(val frame:SFrame,
        val multi:PlayViewMulti, val areaPage:AreaPage)
        extends JPanel {
    import AreaPageControls.AREA_PAGE
    import AreaPageControls.AREA_IMAGE
    import AreaPageControls.AREA_GRID
    import AreaPageControls.AREA_SPLIT

    private var updatingSelected = false

    private var areaChoiceField:SComboBox = null
    private var areaChoicePopupList:JList = null
    private var lastAreaSelectedIndex:Int = -1

    private var widthLabel:SLabel = null
    private var widthField:STextField = null
    private var heightLabel:SLabel = null
    private var heightField:STextField = null
    private var unitsLabel:SLabel = null
    private var unitsField:SComboBox = null
    private var rowCountLabel:SLabel = null
    private var rowCountField:SSpinner = null
    private var columnCountLabel:SLabel = null
    private var columnCountField:SSpinner = null
    private var splitOrientationLabel:SLabel = null
    private var splitOrientationField:SComboBox = null
    private var splitPercentLabel:SLabel = null
    private var splitPercentField:SSpinner = null
    private var marginsLabel:SLabel = null
    private var marginsField:STextField = null
    private var spacingLabel:SLabel = null
    private var spacingField:STextField = null
    private var layoutLabel:SLabel = null
    private var layoutField:SComboBox = null

    private var allAreas:Array[AreaLayout] = null

    setLayout(new FlowLayout(FlowLayout.LEFT))
    addFields()
    updateAllAreasList()
    updateAreaChoiceField(null)

    /** Create and add all of our fields. */
    private def addFields() {
        areaChoiceField = new SComboBox(frame)(
                areaSelected(areaChoiceField.getSelectedIndex()))
        add(areaChoiceField)
        addAreaValueChangedListener()
        val prefix = "NotImplementedYet"     //TODO

        //Set up the Page fields
        widthLabel = makeLabel("Width")
        add(widthLabel)
        widthField = new STextField(frame, prefix+".width",4)(
                setPageWidth(widthField.getText()))
        add(widthField)

        heightLabel = makeLabel("Height")
        add(heightLabel)
        heightField = new STextField(frame, prefix+".height",4)(
                setPageHeight(heightField.getText()))
        add(heightField)

        unitsLabel = makeLabel("Units")
        add(unitsLabel)
        unitsField = new SComboBox(frame)(
                unitsSelected(unitsField.getSelectedIndex()))
        val initialUnitsItems = Array("cm", "in")
            //These match the values of PageLayout.UNIT_CM and
            //PageLayout.UNIT_INCH in AreaPage.
        unitsField.setItems(initialUnitsItems.asInstanceOf[Array[Any]])
        add(unitsField)

        //Set up the Grid fields
        rowCountLabel = makeLabel("Rows")
        add(rowCountLabel)
        val rowCountModel = new SpinnerNumberModel(1,1,99,1)
        rowCountField = new SSpinner(frame,prefix+".rowCount",rowCountModel)(
                setRowColumnCount())
        add(rowCountField)

        columnCountLabel = makeLabel("Columns")
        add(columnCountLabel)
        val columnCountModel = new SpinnerNumberModel(1,1,99,1)
        columnCountField = new SSpinner(frame,prefix+".columnCount",
                columnCountModel)(setRowColumnCount())
        add(columnCountField)

        //Set up the Split fields
        splitOrientationLabel = makeLabel("SplitOrientation")
        add(splitOrientationLabel)
        splitOrientationField = new SComboBox(frame)(
            splitOrientationSelected(splitOrientationField.getSelectedIndex()))
        val initialOrientationItems = Array(
          getResourceString("toolbar.Layout.SplitOrientation.Vertical.label"),
          getResourceString("toolbar.Layout.SplitOrientation.Horizontal.label")
        )
            //These values match the VERTICAL and HORIZONTAL values
            //in AreaSplitLayout.
        splitOrientationField.setItems(
                initialOrientationItems.asInstanceOf[Array[Any]])
        add(splitOrientationField);

        splitPercentLabel = makeLabel("SplitPercent")
        add(splitPercentLabel)
        val percentModel = new SpinnerNumberModel(5,1,99,5)
        splitPercentField = new SSpinner(frame,prefix+".splitPercent",
                percentModel)({
                    val n:Int = splitPercentField.getValue().asInstanceOf[Int]
                    setSplitPercentage(n)
                })
        add(splitPercentField)

        marginsLabel = makeLabel("Margins")
        add(marginsLabel)
        marginsField = new STextField(frame,prefix+".margins",4)(
                setMargins(marginsField.getText()))
        add(marginsField)

        spacingLabel = makeLabel("Spacing")
        add(spacingLabel)
        spacingField = new STextField(frame,prefix+".spacing",4)(
                setSpacing(spacingField.getText()))
        add(spacingField)

        //Add the layout choice
        layoutLabel = makeLabel("Layout")
        add(layoutLabel)
        layoutField = new SComboBox(frame)(
                layoutSelected(layoutField.getSelectedIndex()))
        val initialLayoutItems = Array(
            getResourceString("toolbar.Layout.Layout.Image.label"),
            getResourceString("toolbar.Layout.Layout.Grid.label"),
            getResourceString("toolbar.Layout.Layout.Split.label")
        )
                //TODO define constants
        layoutField.setItems(initialLayoutItems.asInstanceOf[Array[Any]])
        add(layoutField)
    }

    //Make a label, add tooltip if defined, using resources
    private def makeLabel(key:String):SLabel = {
        new SLabel(frame,"toolbar.Layout."+key)
    }

    //Add a listener to the list on the ComboBox so that we can get
    //notified as the user scrolls through the list.
    private def addAreaValueChangedListener() {
        val a:Accessible = areaChoiceField.getUI().getAccessibleChild(
                areaChoiceField,0)
        a match {
            case aa:ComboPopup =>
                areaChoicePopupList = aa.getList()
                areaChoicePopupList.addListSelectionListener(
                        new AreaListSelectionListener())
        }
        //Add a PopupMenuListener so that we know when the popup
        //goes away so that we can reset the highlight when the user
        //cancels the area choice selection.
        areaChoiceField.addPopupMenuListener(new AreaListPopupMenuListener())

        //ItemListener is called under the same conditions as ActionListener,
        //so that one does not help us do our reset when the popup is cancelled.
    }

    class AreaListSelectionListener extends ListSelectionListener {
        //As the user scrolls through the list, highlight the area that
        //corresponds to the selected item in the list.
        def valueChanged(ev:ListSelectionEvent) {
            val index = areaChoicePopupList.getSelectedIndex()
            //val first:Int = ev.getFirstIndex()
            //val last:Int = ev.getLastIndex()
//println("area sel="+index+", first="+first+", last="+last)
            if (index<=0)
                areaPage.highlightedArea = null
            else
                areaPage.highlightedArea = allAreas(index-1)
            areaPage.repaint()
        }
    }

    //The ComboBox implementation is a bit messed up: when you cancel
    //the popup (by pressing escape), it calls this popupMenuCanceled
    //method, but the selectedIndex is still set to the most recently
    //displayed selected item in the popup list, not to what is
    //displayed in the combo box itself.  To get around this, we keep
    //track of the last index actually selected, and we set back to
    //that index on a cancel, which calls our action method that
    //fixes up the highlight.
    class AreaListPopupMenuListener extends javax.swing.event.PopupMenuListener{
        def popupMenuCanceled(ev:javax.swing.event.PopupMenuEvent) {
            areaSelected(lastAreaSelectedIndex)
        }
        def popupMenuWillBecomeInvisible(ev:javax.swing.event.PopupMenuEvent) {}
        def popupMenuWillBecomeVisible(ev:javax.swing.event.PopupMenuEvent) {}
    }

    /** Here when the the page layout changes,
     * to update our list of all areas. */
    def updateAllAreasList() {
        val a = areaPage.areaLayout
        val v = new Vector[AreaLayout]()
        v.addElement(a)       //put the top item into the list
        a.getAreaList(v)     //put all children into the list
        allAreas = new Array[AreaLayout](v.size())
        v.copyInto(allAreas.asInstanceOf[Array[Object]])
    }

    /** Here when the user clicks in the AreaPage window to select an area.
     * @param point A point in our coordinate system.
     */
    def selectArea(point:Point) {
//println("AreaPageControl.selectArea("+point+")")
        var aa = areaPage.areaLayout
        if (!aa.hit(point)) {
//System.out.println("Not in top-level areaLayout");
            updateAreaChoiceField(null)
            return
        }
        var selectedArea:AreaLayout = null
        do {
//System.out.println("Add area "+aa);
            selectedArea = aa
            aa = aa.getArea(point)
        } while (aa!=null)
//System.out.println("Area list size: "+v.size());
        updateAreaChoiceField(selectedArea)
    }

    //After updating the allAreas array, update the areaChoiceField
    //to match it and set the selected item to the specified area.
    private def updateAreaChoiceField(selectedArea:AreaLayout) {
        val numChoices = 1+allAreas.length
        val areaChoiceStrs = new Array[Any](numChoices)
        areaChoiceStrs(0) = "0. "+areaTypeToString(AREA_PAGE)
        for (i <- 0 until allAreas.length) {
            var treeLocation = allAreas(i).getTreeLocation()
            if (treeLocation==null)
                treeLocation = "?";
            areaChoiceStrs(i+1) =
                    Integer.toString(allAreas(i).getTreeDepth()+1)+
                    treeLocation+". "+
                    getAreaTypeString(i+1)
        }
        areaChoiceField.setItems(areaChoiceStrs)
        if (selectedArea==null)
            areaChoiceField.setSelectedIndex(0)  //select the page
        else {
            for (i <- 0 until allAreas.length) {
                if (allAreas(i)==selectedArea) {
                    areaChoiceField.setSelectedIndex(i+1)
                        //select the specified item,
                        //this also calls the action method which
                        //calls areaSelected(int).
                    //break;
                }
            }
        }
    }

    //Here when the user selected the units for the page.
    private def unitsSelected(index:Int) {
//println("Setting units to "+index);
        areaPage.pageUnit = index
    }

    //Here when the user selects an item from the area choice list,
    //or when we call setSelectedIndex on it.
    private def areaSelected(reqIndex:Int) {
//println("selection: "+index)
        lastAreaSelectedIndex = reqIndex
        val index = if (reqIndex<0) 0 else reqIndex
                //by default select the page (0)
        var pageSelected = false
        var gridSelected = false
        var splitSelected = false
        var simpleSelected = false
        var areaType:Int = getAreaType(index)
        areaType match {
            case AREA_PAGE => pageSelected = true
            case AREA_IMAGE => simpleSelected = true
            case AREA_GRID => gridSelected = true
            case AREA_SPLIT => splitSelected = true
            case _ => throw new IllegalArgumentException(
                    "bad area type: "+areaType)
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
            widthField.setText(formatPageValue(areaPage.pageWidth))
            heightField.setText(formatPageValue(areaPage.pageHeight))
            unitsField.setSelectedIndex(areaPage.pageUnit);
            areaPage.highlightedArea = null
        } else {
            val area:AreaLayout = allAreas(index-1)
            areaPage.highlightedArea = area
            marginsField.setText(formatPageValue(area.getMargins()))
            spacingField.setText(formatPageValue(area.getSpacing()))
            areaType match {
            case AREA_IMAGE =>
                layoutField.setSelectedIndex(0)        //TODO define constant
            case AREA_GRID =>
                val gridArea = area.asInstanceOf[AreaGridLayout]
                layoutField.setSelectedIndex(1)        //TODO define constant
                rowCountField.setValue(
                        new java.lang.Integer(gridArea.getRowCount()))
                columnCountField.setValue(
                        new java.lang.Integer(gridArea.getColumnCount()))
            case AREA_SPLIT =>
                val splitArea = area.asInstanceOf[AreaSplitLayout]
                layoutField.setSelectedIndex(2)        //TODO define constant
                splitOrientationField.setSelectedIndex(
                        splitArea.getOrientation())
                splitPercentField.setValue(
                        new java.lang.Integer(splitArea.getSplitPercentage()))
            }
        }
        updatingSelected = false
        areaPage.repaint()
    }

    //Given a page dimension, format it for display to the user
    protected def formatPageValue(n:Int):String = {
        areaPage.formatPageValue(n)
    }

    //Given margins, format for display to the user
    protected def formatPageValue(margins:Insets):String = {
        if (margins.left==margins.right &&
                margins.right==margins.top &&
                margins.top==margins.bottom)
            areaPage.formatPageValue(margins.left)
        else
            areaPage.formatPageValue(margins.left)+","+
                    areaPage.formatPageValue(margins.right)+","+
                    areaPage.formatPageValue(margins.top)+","+
                    areaPage.formatPageValue(margins.bottom)
    }

    //Given spacings, format for display to the user
    protected def formatPageValue(spacing:Dimension):String = {
        if (spacing.width==spacing.height)
            areaPage.formatPageValue(spacing.width);
        else
            areaPage.formatPageValue(spacing.width)+","+
                    areaPage.formatPageValue(spacing.height)
    }

    //Get the type of the area specified by the given index
    private def getAreaType(index:Int):Int = {
        if (index==0)
            return AREA_PAGE
        val a:AreaLayout = allAreas(index-1)
        //TODO - make an AreaLayout method to get type int
        a match {
            case aa:AreaImageLayout => AREA_IMAGE
            case aa:AreaGridLayout => AREA_GRID
            case aa:AreaSplitLayout => AREA_SPLIT
            case _ => throw new IllegalArgumentException(
                    "unknown instance type "+a);
        }
    }

    private def getAreaTypeString(index:Int):String = {
        val areaType = getAreaType(index)
        areaTypeToString(areaType)
    }

    private def areaTypeToString(areaType:Int):String = {
        areaType match {
        case AREA_PAGE => getResourceString("layout.areaType.Page")
        case AREA_IMAGE => getResourceString("layout.areaType.Image")
        case AREA_GRID => getResourceString("layout.areaType.Grid")
        case AREA_SPLIT => getResourceString("layout.areaType.Split")
        case _ => throw new IllegalArgumentException("bad area type: "+areaType)
        }
    }

    private def layoutSelected(layoutTypeIndex:Int) {
//System.out.println("Selected layout: "+layoutTypeIndex);
        val area = getSelectedArea()
        val selectedIndex = areaChoiceField.getSelectedIndex()
        val areaType = getAreaType(selectedIndex)
        val newAreaType = layoutIndexToAreaType(layoutTypeIndex)
        if (newAreaType==areaType)
            return             //no change
        val newArea:AreaLayout = newAreaType match {
        case AREA_IMAGE =>
            new AreaImageLayout(0,0,0,0)
        case AREA_GRID =>
            val newGridArea = new AreaGridLayout()
            newGridArea.setRowColumnCounts(2,2)
            newGridArea
        case AREA_SPLIT =>
            new AreaSplitLayout()
        case _ =>
            throw new RuntimeException("bad newAreaType "+newAreaType)
        }
        //Copy some atttributes from old area to new area
        newArea.setBorderThickness(area.getBorderThickness())
        newArea.setMargins(area.getMargins())
        newArea.setBounds(area.getBounds())
        newArea.setSpacing(area.getSpacing())
        val parentArea = area.getParent()
        //If we are replacing a simple image and its spacing
        //was zero, put in something which we think will be
        //a better default value.
        val newAreaSpacing:Dimension = newArea.getSpacing()
        val zeroSpacing = (newAreaSpacing==null ||
                (newAreaSpacing.width==0 && newAreaSpacing.height==0))
        if (zeroSpacing && area.isInstanceOf[AreaImageLayout]) {
            if (parentArea==null)
                newArea.setSpacing(10*area.getBorderThickness())
            else
                newArea.setSpacing(parentArea.getSpacing())
        }
        newArea.revalidate()   //recalculate bounds etc
        if (parentArea==null) {
            //top level area, contained in page
            areaPage.areaLayout = newArea
        } else {
            parentArea.replaceArea(area,newArea)
        }
        updateAllAreasList()
        updateAreaChoiceField(newArea)    //fix area choice list
        areaSelected(areaChoiceField.getSelectedIndex())
                //fix area property fields
        multi.refreshAreas()
    }

    private def layoutIndexToAreaType(index:Int):Int = {
        val types = Array( AREA_IMAGE, AREA_GRID, AREA_SPLIT )
            //this must match the initialLayoutItems
        return types(index)
    }

    /** Get the currently selected area. */
    private def getSelectedArea():AreaLayout = {
        val index = areaChoiceField.getSelectedIndex()
        if (index<=0)
            throw new RuntimeException("no AreaLayout selected")
        allAreas(index-1)
    }

    /** Throw an exception if the Page is not currently selected. */
    private def assertPageSelected() {
        val index = areaChoiceField.getSelectedIndex()
        if (index!=0)
            throw new RuntimeException("Page not selected")
    }

    private def setPageWidth(s:String) {
        assertPageSelected()
        val width = java.lang.Double.parseDouble(s)
        val w = (width*PageLayout.UNIT_MULTIPLIER).asInstanceOf[Int]
        areaPage.pageWidth = w
        areaPage.repaint()
    }

    private def setPageHeight(s:String) {
        assertPageSelected()
        val height = java.lang.Double.parseDouble(s)
        val h = (height*PageLayout.UNIT_MULTIPLIER).asInstanceOf[Int]
        areaPage.pageHeight = h
        areaPage.repaint()
    }

    private def setMargins(marginStr:String) {
        val a = getSelectedArea()
        val marginStrs = marginStr.split(",")
        var d = java.lang.Double.parseDouble(marginStrs(0))
        var m = (d*PageLayout.UNIT_MULTIPLIER).asInstanceOf[Int]
        a.setMargins(m)
        if (marginStrs.length>1) {
            val margins0:Insets = a.getMargins()
            val margins = new Insets(margins0.top, margins0.left,
                    margins0.bottom, margins0.right);
            d = java.lang.Double.parseDouble(marginStrs(1));
            m = (d*PageLayout.UNIT_MULTIPLIER).asInstanceOf[Int]
            margins.right = m
            if (marginStrs.length>2) {
                d = java.lang.Double.parseDouble(marginStrs(2))
                m = (d*PageLayout.UNIT_MULTIPLIER).asInstanceOf[Int]
            }
            margins.top = m
            if (marginStrs.length>3) {
                d = java.lang.Double.parseDouble(marginStrs(3))
                m = (d*PageLayout.UNIT_MULTIPLIER).asInstanceOf[Int]
            }
            margins.bottom = m
            a.setMargins(margins)
        }
        a.revalidate()
        areaPage.repaint()
    }

    private def setSpacing(spacingStr:String) {
        val a = getSelectedArea()
        val spacingStrs = spacingStr.split(",")
        var d = java.lang.Double.parseDouble(spacingStrs(0))
        var m = (d*PageLayout.UNIT_MULTIPLIER).asInstanceOf[Int]
        a.setSpacing(m)
        if (spacingStrs.length>1) {
            var spacing:Dimension = a.getSpacing()
            spacing = new Dimension(spacing)
            d = java.lang.Double.parseDouble(spacingStrs(1))
            m = (d*PageLayout.UNIT_MULTIPLIER).asInstanceOf[Int]
            spacing.height = m
            a.setSpacing(spacing)
        }
        a.revalidate()
        areaPage.repaint()
    }

    /** Read the row and column values from the text fields
     * and set those numbers on the grid layout. */
    private def setRowColumnCount() {
        if (updatingSelected)
            return
        val rowCount = rowCountField.getValue().asInstanceOf[Int]
        val columnCount = columnCountField.getValue().asInstanceOf[Int]
        getSelectedArea() match {
        case a:AreaGridLayout =>
            a.setRowColumnCounts(rowCount,columnCount)
            a.revalidate()
            areaPage.repaint()
            //When number of rows or columns changes, that changes our
            //list of areas.
            updateAllAreasList()
            updateAreaChoiceField(a)
        }
    }

    private def splitOrientationSelected(index:Int) {
        getSelectedArea() match {
        case a:AreaSplitLayout =>
            a.setOrientation(index)
            a.revalidate()
            areaPage.repaint()
        }
    }

    private def setSplitPercentage(splitPercent:Int) {
        getSelectedArea() match {
        case a:AreaSplitLayout =>
            a.setSplitPercentage(splitPercent)
            a.revalidate()
            areaPage.repaint()
        }
    }

    /** Get a string from our resources. */
    def getResourceString(name:String):String =
        frame.getResourceString(name)

    /** Get a string from our resources. */
    def getResourceFormatted(name:String, args:Array[Any]):String =
        frame.getResourceFormatted(name, args)
}

object AreaPageControls {
    protected val AREA_PAGE = 0
    protected val AREA_IMAGE = 1
    protected val AREA_GRID = 2
    protected val AREA_SPLIT = 3
}
