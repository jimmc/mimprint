/* LabelPrinter.java
 *
 * Jim McBeath, April 30, 2002
 */

package net.jimmc.swing;

import net.jimmc.util.MoreException;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.Vector;
import javax.swing.JEditorPane;
import javax.swing.text.html.HTMLEditorKit;

/** Layout and print labels.
 */
public class LabelPrinter implements Printable {
	/** Number of labels across the page. */
	int xLabelCount;

	/** Number of labels down the page. */
	int yLabelCount;

	/** Left and right margin in points. */
	int xMargin;

	/** Top and bottom margin in points. */
	int yMargin;

	/** Horizontal spacing between labels in points. */
	int xSpacing;

	/** Vertical spacing between labels in points. */
	int ySpacing;

	/** Our pages. A list of LabelPage objects. */
	Vector pages;

	/** The current page. */
	LabelPage currentPage;

	/** The current label. */
	Label currentLabel;

	/** Create a LabelPrinter. */
	public LabelPrinter() {
		pages = new Vector();
	}

	/** Set the number of labels on each page. */
	public void setLabelsPerPage(int xCount, int yCount) {
		xLabelCount = xCount;
		yLabelCount = yCount;
	}

	/** Set the margins.
	 * @param xMargin The left margin in points.
	 * @param yMargin The top margin in points.
	 */
	public void setMargins(int xMargin, int yMargin) {
		this.xMargin = xMargin;
		this.yMargin = yMargin;
	}

	/** Set the spacing between labels.
	 * @param xSpacing The horizontal space between labels in points.
	 * @param ySpacing The vertical space between labels in points.
	 */
	public void setSpacing(int xSpacing, int ySpacing) {
		this.xSpacing = xSpacing;
		this.ySpacing = ySpacing;
	}

	/** Advance to the next label.
	 * Call this before setting the html text for the first label,
	 * then call it again before setting the html text for each
	 * subsequent label.
	 * @see #setNextLabelHtml
	 */
	public void nextLabel() {
		if (currentPage==null || currentPage.isFull()) {
			currentPage = new LabelPage(this,
				xLabelCount,yLabelCount,xMargin,yMargin,
				xSpacing,ySpacing);
			pages.addElement(currentPage);
		}
		currentLabel = currentPage.nextLabel();
	}

	/** Set the html text for the current label.
	 */
	public void setLabelHtml(String html) {
		currentLabel.setHtml(html);
	}

	/** Advance to the next label and set its html.
	 * Same as calling {@link #nextLabel} followed by {@link #setLabelHtml}.
	 */
	public void setNextLabelHtml(String html) {
		nextLabel();
		setLabelHtml(html);
	}

	/** Print this set of labels. */
	public void print() {
		PrinterJob pJob = PrinterJob.getPrinterJob();
		PageFormat pageFormat = pJob.defaultPage();
		//pageFormat = pJob.validatePage(pageFormat);
		Paper oldPaper = pageFormat.getPaper();
		Paper newPaper = new Paper() {
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
		};
		newPaper.setSize(oldPaper.getWidth(),oldPaper.getHeight());
		pageFormat.setPaper(newPaper);
		pJob.setPrintable(this,pageFormat);
		if (!pJob.printDialog())
			return;		//cancelled
		try {
			pJob.print();
		} catch (PrinterException ex) {
			throw new MoreException(ex);
		}
	}

    //The Printable interface
	public int print(Graphics graphics, PageFormat pageFormat,
						int pageIndex) {
		if (pageIndex<0 || pageIndex>=pages.size())
			return NO_SUCH_PAGE;
		LabelPage page = (LabelPage)pages.elementAt(pageIndex);
		page.print(graphics,pageFormat);
		return PAGE_EXISTS;
	}
    //End Printable interface
}

//One page of labels
class LabelPage {
	LabelPrinter labelPrinter;
	int xCount;
	int yCount;
	int xMargin;
	int yMargin;
	int xSpacing;
	int ySpacing;

	Vector labels;

	/** Create a LabelPage. */
	LabelPage(LabelPrinter labelPrinter, int xCount, int yCount,
			int xMargin, int yMargin,
			int xSpacing, int ySpacing) {
		this.labelPrinter = labelPrinter;
		this.xCount = xCount;
		this.yCount = yCount;
		this.xMargin = xMargin;
		this.yMargin = yMargin;
		this.xSpacing = xSpacing;
		this.ySpacing = ySpacing;
		labels = new Vector();
	}

	/** True if our page of labels is full. */
	boolean isFull() {
		return (labels.size()>=(xCount*yCount));
	}

	/** Advance to the next label. */
	Label nextLabel() {
		Label label = new Label(this);
		labels.addElement(label);
		return label;
	}

	/** Print this page. */
	public void print(Graphics graphics, PageFormat pageFormat) {
		int width = (int)pageFormat.getWidth() - 2*xMargin;
		int height = (int)pageFormat.getHeight() - 2*yMargin;
		int labelWidth = (width+xSpacing)/xCount - xSpacing;
		int labelHeight = (height+ySpacing)/yCount - ySpacing;

		JEditorPane editor = new JEditorPane();
		editor.setEditorKit(new HTMLEditorKit());
		editor.setContentType("text/html");

		for (int i=0; i<labels.size(); i++) {
			Label label = (Label)labels.elementAt(i);
			int x = (i%xCount)*(labelWidth+xSpacing)+xMargin;
			int y = (i/xCount)*(labelHeight+ySpacing)+yMargin;
			label.print(editor,graphics,x,y,labelWidth,labelHeight);
		}
	}
}

class Label {
	LabelPage page;
	String html;

	Label(LabelPage page) {
		this.page = page;
	}

	void setHtml(String html) {
		this.html = html;
	}

	/** Print this label at the specified location. */
	public void print(JEditorPane editor, Graphics graphics,
				int x, int y, int width, int height) {
		if (html==null)
			return;		//blank label
		Graphics g = graphics.create(x,y,width,height);
		editor.setText(html);
		editor.setSize(new Dimension(width,height));
		editor.print(g);
	}
}

/* end */
