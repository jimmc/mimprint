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
import net.jimmc.util.Publisher

import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Insets
import java.awt.Point
import javax.accessibility.Accessible
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.plaf.basic.ComboPopup
import javax.swing.SpinnerNumberModel

import scala.collection.mutable.ArrayBuffer

class AreaPageControls(val frame:SFrame,
        val multi:PlayViewMulti, val areaPage:AreaPage)
        extends JPanel {

    private var updatingSelected = false

    case class AreaChange(index:Int, area:AreaLayout, areaType:AreaType)
    private val areaChangePublisher = new Object with Publisher[AreaChange]
	//Scalac complains if we just do "new Publisher[AreaType]"

    private var areaChoiceField:SComboBox = null
    private var areaChoicePopupList:JList = null
    private var lastAreaSelectedIndex:Int = -1

    private var pageNumberPanel:JPanel = null
    private var pageNumberField:STextField = null
    private var pageOfField:SLabel = null

    private var allAreas:Array[AreaLayout] = null

    setLayout(new FlowLayout(FlowLayout.LEFT))
    addFields()
    updateAllAreasList()
    updateAreaChoiceField(None)

    /** Create and add all of our fields. */
    private def addFields() {

        //Set up the Page fields
        pageNumberPanel = makePageNumberPanel()
        //pageNumberPanel.setVisible(false)
        add(pageNumberPanel)

        areaChoiceField = new SComboBox(frame)(
                (cb)=>areaSelected(cb.getSelectedIndex()))
        add(areaChoiceField)
        addAreaValueChangedListener()
        val prefix = "NotImplementedYet"     //TODO

	def areaTypeVisibility(c:Component, f:(AreaType)=>Boolean) = {
	    areaChangePublisher.subscribe(e=>c.setVisible(f(e.areaType)))
	}

        val widthLabel = makeLabel("Width")
        add(widthLabel)
	areaTypeVisibility(widthLabel,_==AreaTypePage)

        val widthField = new STextField(frame, prefix+".width",4)(
                (cb)=>setPageWidth(cb.getText()))
        add(widthField)
	areaTypeVisibility(widthField,_==AreaTypePage)
	areaChangePublisher.subscribe(e=>if (e.index==0)
            widthField.setText(formatPageValue(areaPage.pageWidth)))

        val heightLabel = makeLabel("Height")
        add(heightLabel)
	areaTypeVisibility(heightLabel,_==AreaTypePage)

        val heightField = new STextField(frame, prefix+".height",4)(
                (cb)=>setPageHeight(cb.getText()))
        add(heightField)
	areaTypeVisibility(heightField,_==AreaTypePage)
	areaChangePublisher.subscribe(e=>if (e.index==0)
            heightField.setText(formatPageValue(areaPage.pageHeight)))

        //unitsLabel = makeLabel("Units")
        //add(unitsLabel)
        val unitsField = new SComboBox(frame)(
                (cb)=>unitsSelected(cb.getSelectedIndex()))
        val initialUnitsItems = Array("cm", "in")
            //These match the values of PageValue.UNIT_CM and
            //PageValue.UNIT_INCH in AreaPage.
        unitsField.setItems(initialUnitsItems.asInstanceOf[Array[Any]])
        add(unitsField)
	areaTypeVisibility(unitsField,_==AreaTypePage)
	areaChangePublisher.subscribe(e=>if (e.index==0)
            unitsField.setSelectedIndex(areaPage.pageUnit))

        //Set up the Grid fields
        val rowCountLabel = makeLabel("Rows")
        add(rowCountLabel)
	areaTypeVisibility(rowCountLabel,_==AreaTypeGrid)

	var rowCountField:SSpinner = null
	var columnCountField:SSpinner = null
	def setRowOrColumnCount() {
	    setRowAndColumnCount(rowCountField, columnCountField)
	}

        val rowCountModel = new SpinnerNumberModel(1,1,99,1)
        rowCountField = new SSpinner(frame,prefix+".rowCount",rowCountModel)(
                (cb)=>setRowOrColumnCount())
        add(rowCountField)
	areaTypeVisibility(rowCountField,_==AreaTypeGrid)

        val columnCountLabel = makeLabel("Columns")
        add(columnCountLabel)
	areaTypeVisibility(columnCountLabel,_==AreaTypeGrid)

        val columnCountModel = new SpinnerNumberModel(1,1,99,1)
        columnCountField = new SSpinner(frame,prefix+".columnCount",
                columnCountModel)((cb)=>setRowOrColumnCount())
        add(columnCountField)
	areaTypeVisibility(columnCountField,_==AreaTypeGrid)

	areaChangePublisher.subscribe(e=>if (e.index>0) {
            e.area match {
            case gridArea:AreaGridLayout =>
                rowCountField.setValue(gridArea.rowCount)
                columnCountField.setValue(gridArea.columnCount)
	    case _ =>
            }
	})

        //Set up the Split fields
        val splitOrientationLabel = makeLabel("SplitOrientation")
        add(splitOrientationLabel)
	areaTypeVisibility(splitOrientationLabel,_==AreaTypeSplit)

        val splitOrientationField = new SComboBox(frame)(
            (cb)=>splitOrientationSelected(cb.getSelectedIndex()))
        val initialOrientationItems = Array(
          getResourceString("toolbar.Layout.SplitOrientation.Vertical.label"),
          getResourceString("toolbar.Layout.SplitOrientation.Horizontal.label")
        )
            //These values match the VERTICAL and HORIZONTAL values
            //in AreaSplitLayout.
        splitOrientationField.setItems(
                initialOrientationItems.asInstanceOf[Array[Any]])
        add(splitOrientationField);
	areaTypeVisibility(splitOrientationField,_==AreaTypeSplit)

        val splitPercentLabel = makeLabel("SplitPercent")
        add(splitPercentLabel)
	areaTypeVisibility(splitPercentLabel,_==AreaTypeSplit)

        val percentModel = new SpinnerNumberModel(5,1,99,5)
        val splitPercentField = new SSpinner(frame,prefix+".splitPercent",
                percentModel)((cb)=>{
                    val n:Int = cb.getValue().asInstanceOf[Int]
                    setSplitPercentage(n)
                })
        add(splitPercentField)
	areaTypeVisibility(splitPercentField,_==AreaTypeSplit)

	areaChangePublisher.subscribe(e=>if (e.index>0) {
            e.area match {
            case splitArea:AreaSplitLayout =>
                splitOrientationField.setSelectedIndex(
                        splitArea.getOrientation())
                splitPercentField.setValue(splitArea.getSplitPercentage())
	    case _ =>
            }
	})

        val marginsLabel = makeLabel("Margins")
        add(marginsLabel)
	areaTypeVisibility(marginsLabel,_!=AreaTypePage)

        val marginsField = new STextField(frame,prefix+".margins",4)(
                (cb)=>setMargins(cb.getText()))
        add(marginsField)
	areaTypeVisibility(marginsField,_!=AreaTypePage)
	areaChangePublisher.subscribe(e=>if (e.index>0)
            marginsField.setText(formatPageValue(e.area.getMargins())))

        val spacingLabel = makeLabel("Spacing")
        add(spacingLabel)
	areaTypeVisibility(spacingLabel,e=>(e==AreaTypeGrid || e==AreaTypeSplit))
        val spacingField = new STextField(frame,prefix+".spacing",4)(
                (cb)=>setSpacing(cb.getText()))
        add(spacingField)
	areaTypeVisibility(spacingField,e=>(e==AreaTypeGrid || e==AreaTypeSplit))
	areaChangePublisher.subscribe(e=>if (e.index>0)
            spacingField.setText(formatPageValue(e.area.getSpacing())))

        //Add the layout choice
        val layoutLabel = makeLabel("Layout")
        add(layoutLabel)
	areaTypeVisibility(layoutLabel,_!=AreaTypePage)
        val layoutField = new SComboBox(frame)(
                (cb)=>layoutSelected(cb.getSelectedIndex()))
        val initialLayoutItems = Array(
            getResourceString("toolbar.Layout.Layout.Image.label"),
            getResourceString("toolbar.Layout.Layout.Grid.label"),
            getResourceString("toolbar.Layout.Layout.Split.label")
        )
                //TODO define constants
        layoutField.setItems(initialLayoutItems.asInstanceOf[Array[Any]])
        add(layoutField)
	areaTypeVisibility(layoutField,_!=AreaTypePage)
	areaChangePublisher.subscribe(e=>if (e.index>0) {
            e.area match {
            case imageArea:AreaImageLayout =>
                layoutField.setSelectedIndex(0)        //TODO define constant
            case gridArea:AreaGridLayout =>
                layoutField.setSelectedIndex(1)        //TODO define constant
            case splitArea:AreaSplitLayout =>
                layoutField.setSelectedIndex(2)        //TODO define constant
	    case _ =>
            }
	})
    }

    private def makePageNumberPanel():JPanel = {
        val p = new JPanel()

        p.add(new SLabel(frame,"toolbar.Layout.PageNumber.Label"))
        pageNumberField = new STextField(frame,
                "toolbar.Layout.PageNumber.Number",2)(
            (cb)=>setPageNumber(cb.getText)
        )
        pageNumberField.setText("1")
        p.add(pageNumberField)
        pageOfField = new SLabel(frame,"toolbar.Layout.PageNumber.Of")
        p.add(pageOfField)

        p
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
        val aa = new ArrayBuffer[AreaLayout]()
        aa += a       //put the top item into the list
        a.retrieveAreaList(aa)     //put all children into the list
        allAreas = aa.toArray
        areaPage.fixImageIndexes()
    }

    /** Here when the user clicks in the AreaPage window to select an area.
     * @param point A point in our coordinate system.
     */
    def selectArea(point:Point) {
//println("AreaPageControl.selectArea("+point+")")
        updateAreaChoiceField(areaPage.areaLayout.getAreaLeaf(point))
    }

    //After updating the allAreas array, update the areaChoiceField
    //to match it and set the selected item to the specified area.
    private def updateAreaChoiceField(selectedArea:Option[AreaLayout]) {
        val numChoices = 1+allAreas.length
        val areaChoiceStrs = new Array[Any](numChoices)
        areaChoiceStrs(0) = "0. "+AreaTypePage.toString
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
        val idx = allAreas.findIndexOf(_ == selectedArea.getOrElse(null))
            //if selectedArea is null, findIndexOf will return -1, so
            //when we use setSelectedIndex(idx+1) we get (0), the Page item
        areaChoiceField.setSelectedIndex(idx+1)
            //this also calls the action method which calls areaSelected(int)
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
        val areaType = getAreaType(index)

        updatingSelected = true;

	val area:AreaLayout = if (index>0) allAreas(index-1) else null
	areaChangePublisher.publish(AreaChange(index,area,areaType))

	areaPage.highlightedArea = area

        updatingSelected = false
        areaPage.repaint()
    }

    def setPageCount(n:Int) {
        val s = getResourceFormatted("toolbar.Layout.PageNumber.Of.format",n)
        pageOfField.setText(s)
        pageNumberPanel.setVisible(n>1)
    }

    //s is a 1-based number
    private def setPageNumber(s:String):Unit =
        setPageNumber(Integer.parseInt(s) - 1)

    //n is a 0-based number
    private def setPageNumber(n:Int) {
        multi.setPageNumber(n)
    }
    protected[mimprint] def setPageNumberDisplay(n:Int) =
        pageNumberField.setText((n+1).toString)

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
    private def getAreaType(index:Int):AreaType = {
        if (index==0)
            return AreaTypePage
        val a:AreaLayout = allAreas(index-1)
        //TODO - make an AreaLayout method to get type int
        a match {
            case aa:AreaImageLayout => AreaTypeImage
            case aa:AreaGridLayout => AreaTypeGrid
            case aa:AreaSplitLayout => AreaTypeSplit
            case _ => throw new IllegalArgumentException(
                    "unknown instance type "+a);
        }
    }

    private def getAreaTypeString(index:Int):String = {
        val areaType = getAreaType(index)
	areaType.toString
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
        case AreaTypeImage =>
            new AreaImageLayout(0,0,0,0)
        case AreaTypeGrid =>
            val newGridArea = new AreaGridLayout()
            newGridArea.setRowColumnCounts(2,2)
            newGridArea
        case AreaTypeSplit =>
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
        updateAreaChoiceField(Some(newArea))    //fix area choice list
        areaSelected(areaChoiceField.getSelectedIndex())
                //fix area property fields
        multi.refreshAreas()
    }

    private def layoutIndexToAreaType(index:Int):AreaType = {
        val types = Array( AreaTypeImage, AreaTypeGrid, AreaTypeSplit )
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
        val w = (width*PageValue.UNIT_MULTIPLIER).asInstanceOf[Int]
        areaPage.pageWidth = w
        areaPage.repaint()
    }

    private def setPageHeight(s:String) {
        assertPageSelected()
        val height = java.lang.Double.parseDouble(s)
        val h = (height*PageValue.UNIT_MULTIPLIER).asInstanceOf[Int]
        areaPage.pageHeight = h
        areaPage.repaint()
    }

    private def setMargins(marginStr:String) {
        val a = getSelectedArea()
        val marginStrs = marginStr.split(",")
        var d = java.lang.Double.parseDouble(marginStrs(0))
        var m = (d*PageValue.UNIT_MULTIPLIER).asInstanceOf[Int]
        a.setMargins(m)
        if (marginStrs.length>1) {
            val margins0:Insets = a.getMargins()
            val margins = new Insets(margins0.top, margins0.left,
                    margins0.bottom, margins0.right);
            d = java.lang.Double.parseDouble(marginStrs(1));
            m = (d*PageValue.UNIT_MULTIPLIER).asInstanceOf[Int]
            margins.right = m
            if (marginStrs.length>2) {
                d = java.lang.Double.parseDouble(marginStrs(2))
                m = (d*PageValue.UNIT_MULTIPLIER).asInstanceOf[Int]
            }
            margins.top = m
            if (marginStrs.length>3) {
                d = java.lang.Double.parseDouble(marginStrs(3))
                m = (d*PageValue.UNIT_MULTIPLIER).asInstanceOf[Int]
            }
            margins.bottom = m
            a.setMargins(margins)
        }
        a.revalidate()
        areaPage.refresh()
    }

    private def setSpacing(spacingStr:String) {
        val a = getSelectedArea()
        val spacingStrs = spacingStr.split(",")
        var d = java.lang.Double.parseDouble(spacingStrs(0))
        var m = (d*PageValue.UNIT_MULTIPLIER).asInstanceOf[Int]
        a.setSpacing(m)
        if (spacingStrs.length>1) {
            var spacing:Dimension = a.getSpacing()
            spacing = new Dimension(spacing)
            d = java.lang.Double.parseDouble(spacingStrs(1))
            m = (d*PageValue.UNIT_MULTIPLIER).asInstanceOf[Int]
            spacing.height = m
            a.setSpacing(spacing)
        }
        a.revalidate()
        areaPage.refresh()
    }

    /** Read the row and column values from the text fields
     * and set those numbers on the grid layout. */
    private def setRowAndColumnCount(
	    rowCountField:SSpinner, columnCountField:SSpinner) {
        if (updatingSelected)
            return
        val rowCount = rowCountField.getValue().asInstanceOf[Int]
        val columnCount = columnCountField.getValue().asInstanceOf[Int]
        getSelectedArea() match {
        case a:AreaGridLayout =>
            a.setRowColumnCounts(rowCount,columnCount)
            a.revalidate()
            areaPage.refresh()
            //When number of rows or columns changes, that changes our
            //list of areas.
            updateAllAreasList()
            updateAreaChoiceField(Some(a))
        }
    }

    private def splitOrientationSelected(index:Int) {
        getSelectedArea() match {
        case a:AreaSplitLayout =>
            a.setOrientation(index)
            a.revalidate()
            areaPage.refresh()
        }
    }

    private def setSplitPercentage(splitPercent:Int) {
        getSelectedArea() match {
        case a:AreaSplitLayout =>
            a.setSplitPercentage(splitPercent)
            a.revalidate()
            areaPage.refresh()
        }
    }

    /** Get a string from our resources. */
    def getResourceString(name:String):String =
        frame.getResourceString(name)

    /** Get a string from our resources. */
    def getResourceFormatted(name:String, arg:Any):String =
        frame.getResourceFormatted(name, arg)

    /** Get a string from our resources. */
    def getResourceFormatted(name:String, args:Array[Any]):String =
        frame.getResourceFormatted(name, args)


    sealed abstract class AreaType {
	val resName:String
	override def toString() = {
	    getResourceString("layout.areaType."+resName)
	}
    }
    case object AreaTypePage extends AreaType  { val resName = "Page" }
    case object AreaTypeImage extends AreaType { val resName = "Image" }
    case object AreaTypeGrid extends AreaType  { val resName = "Grid" }
    case object AreaTypeSplit extends AreaType { val resName = "Split" }
}
