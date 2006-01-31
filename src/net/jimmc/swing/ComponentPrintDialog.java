/* ComponentPrintDialog.java
 *
 * Jim McBeath, December 3, 2001
 */
//PrintDialog method taken from IQvis

package net.jimmc.swing;

import net.jimmc.util.ResourceSource;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterGraphics;
import java.awt.print.PrinterJob;
import javax.swing.border.Border;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.RepaintManager;

public class ComponentPrintDialog implements Printable {
	private static final double TRANSLATION_FUDGE = 1.01;
	private static final double SCALE_FUDGE = 0.98;

	/** Our parent. */
	protected Component parent;

	/** Our resource source. */
	protected ResourceSource res;

	/** The component to print. */
	protected Component target;

	/** Our PrinterJob. */
	protected PrinterJob printerJob;

	/** Our page format. */
	protected PageFormat pageFormat;

	/** Our dialog panel. */
	protected JPanel panel;

	/** True to force use of the whole page, with no margins. */
	protected boolean forceZeroMargins;

	/** If non-negative, the margin to use on all sides of the page. */
	protected double marginInches;

	/** The actual margin we are using, if either forceZeroMargin or
	 * marginInches has been set.
	 */
	protected double actualMarginPixels;

	/** Create our object. */
	public ComponentPrintDialog(Component parent,
				ResourceSource res, Component target) {
		this.parent = parent;
		this.res = res;
		this.target = target;
		panel = buildPanel();
		marginInches = -1.0;
	}

	/** Create our dialog panel. */
	protected JPanel buildPanel() {
		JPanel p = new JPanel();
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		p.setLayout(gbl);

		String emptyMsg = res.getResourceString(
			"ComponentPrintDialog.EmptyPanel");
		JLabel label = new JLabel(emptyMsg);
		p.add(label);

		return p;
	}

	/** Set the zero-margins flag. */
	public void setZeroMargins(boolean f) {
		forceZeroMargins = f;
	}

	/** Get the zero-margins flag. */
	public boolean isZeroMargins() {
		return forceZeroMargins;
	}

	/** Set the margin to use for all side. */
	public void setMargin(double inches) {
		marginInches = inches;
	}

	/** Get the margin that has been set for all sides.
	 * @return The margin as passed to setMargin, or -1 if not set.
	 *         A call to setZeroMargins will override this value.
	 */
	public double getMargin() {
		return marginInches;
	}

	/** Put up the dialog and let the user set his options.
	 * @return The PrinterJob ready for a call to it's print method,
	 *         or null if cancelled.
	 */
	public boolean printDialog() {
		String title = res.getResourceString(
			"ComponentPrintDialog.title");
		Icon icon = null;	//no icon
		Object[] options = {
			res.getResourceString(
				"ComponentPrintDialog.button.PageSetup"),
			res.getResourceString(
				"ComponentPrintDialog.button.Preview"),
			res.getResourceString(
				"ComponentPrintDialog.button.Continue"),
			res.getResourceString(
				"ComponentPrintDialog.button.Cancel")
		};
		Object initialOption = options[1];

		printerJob = PrinterJob.getPrinterJob();
		pageFormat = printerJob.defaultPage();
		//pageFormat = printerJob.validatePage(pageFormat);

		//Force margins to zero if requested
		if (isZeroMargins())
			setZeroMargins(pageFormat);
		else if (marginInches>=0.0)
			setMargins(pageFormat,marginInches);
		//TBD - add a Zero Margins toggle option in the dialog box

		int t = -1;
		while (t!=2) {	//loop until he picks "Continue"
			t = JOptionPane.showOptionDialog(
				parent, panel, title,
				JOptionPane.YES_NO_OPTION,
				JOptionPane.PLAIN_MESSAGE,
				icon,
				options, initialOption);
			switch (t) {
			case 3:
				return false;	//cancelled
			case 1:
				PrintPreviewer pv =
					new PrintPreviewer(res,this,pageFormat);
				pv.show();
				//Assume the user will print from the Previewer,
				//so we don't need to do anything more here.
				return false;
			case 0:
				//Put up page dialog
				PageFormat pf =
					printerJob.pageDialog(pageFormat);
				if (pf!=pageFormat) {
					//pageFormat =printerJob.validatePage(pf);
					pageFormat = pf;
					if (isZeroMargins())
						setZeroMargins(pageFormat);
					else if (marginInches>=0.0)
						setMargins(pageFormat,
								marginInches);
				}
				break;
			default:
				break;	//do nothing
			}
			//if t==2, we will break out of the loop.
			//if t==0 or t==1, we will go back and put up our
			//  dialog box again.
		}

		//When the user pushes the "Continue" button in our dialog,
		//we get to this point in the code, and we put up the
		//standard Print dialog.

		printerJob.setPrintable(this,pageFormat);
		if (!printerJob.printDialog())
			return false;		//cancelled

		return true;			//ready to print
	}

	/** Set the margins of the page to zero. */
	protected void setZeroMargins(PageFormat pageFormat) {
		setMargins(pageFormat,0.0);
	}

	/** Set the margins of the page as specified.
	 * This method creates a Paper object which references the
	 * actualMarginPixels variable in the ComponentPrintDialog object,
	 * so only one PageFormat object at a time can have its margin set
	 * this way.
	 */
	protected void setMargins(PageFormat pageFormat, double margin) {
		actualMarginPixels = margin*72.0;
			//convert from inches to pixels at 72 pixels per inch
		Paper oldPaper = pageFormat.getPaper();
		Paper newPaper = new Paper() {
			//Force our paper's imageable area
			//to the specified margin
			public double getImageableHeight() {
				return getHeight() - 2*actualMarginPixels;
			}
			public double getImageableWidth() {
				return getWidth() - 2*actualMarginPixels;
			}
			public double getImageableX() {
				return actualMarginPixels;
			}
			public double getImageableY() {
				return actualMarginPixels;
			}
		};
		newPaper.setSize(
			oldPaper.getWidth(),oldPaper.getHeight());
		pageFormat.setPaper(newPaper);
	}

	/** Print. */
	public void print() throws PrinterException {
		printerJob.print();
	}

	/** Get the count of pages to be printed for a component.
	 * This value must match the number of pages calculated
	 * in {@link #printTarget}.
	 */
	public int getPageCount(Component target, PageFormat pf) {
		double scale =
			SCALE_FUDGE*pf.getImageableWidth()/target.getWidth();

		//The amount of the target that appears on each page
		double targetPageHeight = pf.getImageableHeight()/scale;

		//The number of pages required to print the whole component
		int pageCount = (int)Math.ceil(
				target.getHeight()/targetPageHeight);

		return pageCount;
	}

	/** Print the specified page from the specified target.
	 * @return PAGE_EXISTS if we printed a page, NO_SUCH_PAGE if not.
	 */
	public int printTarget(Component target, Graphics g, PageFormat pf,
					int pageIndex) {

		//The default foreground and background colors on printing
		//are both white, which makes it pretty hard to see anything.
		//Set the foreground to black.
		g.setColor(Color.black);

		//TBD - allow some kind of control to force use of 1.00 for the
		//two fudge factors in this section, to allow pixel-perfect
		//printing calculations.
		double tf = TRANSLATION_FUDGE;	//translation fudge factor
		double sf = SCALE_FUDGE;	//scale fudge factor
		Graphics2D g2 = (Graphics2D)g;

		//Print only the imageable area, so that our preview looks
		//the same as the printed copy
		g2.setClip((int)pf.getImageableX(),(int)pf.getImageableY(),
		    (int)pf.getImageableWidth(),(int)pf.getImageableHeight());

		//Translate out of the non-imageable area
		g2.translate(tf*pf.getImageableX(),tf*pf.getImageableY());
		//Scale so that the width of the taget component is
		//displayed exactly in the width of the imaging area
		//(plus a small fudge factor to ensure it is all inside
		//the imageable area, otherwise border lines don't all print).
		double scale = sf*pf.getImageableWidth()/target.getWidth();
		g2.scale(scale,scale);

		//The amount of the target that appears on each page
		double targetPageHeight = pf.getImageableHeight()/scale;

		//The number of pages required to print the whole component
		int pageCount = (int)Math.ceil(
					target.getHeight()/targetPageHeight);
		if (pageIndex>=pageCount)
			return NO_SUCH_PAGE;
		//For pages after the first, translate the image so that
		//we print the proper portion of the image.
		g2.translate(0.0,-pageIndex*targetPageHeight);

		//Turn off double-buffering before printing so that we don't
		//just get a scaled bitmap image of the buffer, turn it back
		//on when we are done printing.
		RepaintManager rm = RepaintManager.currentManager(target);
		rm.setDoubleBufferingEnabled(false);

		target.print(g2);

		rm = RepaintManager.currentManager(target);
		rm.setDoubleBufferingEnabled(true);

		return PAGE_EXISTS;
	}

    //The Printable interface
    	/** Print our target component. */
	public int print(Graphics g, PageFormat pf, int pageIndex)
				throws PrinterException {
		//If the target implements Printable, use its print method
		//instead of our own.
		if (target instanceof Printable) {
			return ((Printable)target).print(g,pf,pageIndex);
		}
		return printTarget(target, g, pf, pageIndex);
	}
    //End Printable interface
}

/* end */
