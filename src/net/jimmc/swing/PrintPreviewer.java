/* PrintPreviewer.java
 *
 * Jim McBeath, May 4, 2002
 */

package net.jimmc.swing;

import net.jimmc.util.MoreException;
import net.jimmc.util.ResourceSource;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.awt.Shape;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/** Provide a preview of a Printable.
 */
public class PrintPreviewer extends JsFrame {
	/** Size of our gray margin in pixels. */
	public final static int MARGIN = 15;

	/** Our Printable. */
	protected Printable printable;

	/** Our page format. */
	protected PageFormat page;

	/** True to force use of the whole page, with no margins. */
	protected boolean forceZeroMargins;

	/** The scroll pane containing the page. */
	JScrollPane scroll;

	/** Where we display a page. */
	JPanel pageImager;

	/** The current page. */
	int pageIndex;

	/** Page number display. */
	JLabel pageLabel;

	/** The Previous button. */
	ButtonAction previousButton;

	/** The Next button. */
	ButtonAction nextButton;

	/** The Preview Scale choice. */
	ComboBoxAction scaleChoice;

	/** Status of the print of the last page. */
	int pageStatus;

	/** The total number of pages. */
	int pageCount;

	/** Create a previewer for the specified Printable and PageFormat. */
	public PrintPreviewer(ResourceSource res, Printable printable,
			PageFormat pageFormat) {
		super();
		setResourceSource(res);
		this.printable = printable;
		setJMenuBar(createMenuBar());
		if (pageFormat==null)
			pageFormat = PrinterJob.getPrinterJob().defaultPage();
		initForm();
		setPageFormat(pageFormat);
		pack();
		addWindowListener();
		setTitle(res.getResourceString("PrintPreview.title"));
		pageCount = getPageCount(printable);
		setPageLabel();
	}

	/** Create a previewer for the specified Printable. */
	public PrintPreviewer(ResourceSource res, Printable printable) {
		this(res,printable,null);
	}

	/** Set the zero-margins flag. */
	public void setZeroMargins(boolean f) {
		forceZeroMargins = f;
	}

	/** Get the zero-margins flag. */
	public boolean isZeroMargins() {
		return forceZeroMargins;
	}

	/** Create the body of our form. */
	protected void initForm() {
		pageImager = new JPanel() {
			public void paint(Graphics g) {
				paintPage(g);
			}
		};
		scroll = new JScrollPane(pageImager);

		Container contents = getContentPane();
		contents.setLayout(new BorderLayout());
		scroll.setPreferredSize(new Dimension(660,500));
		contents.add(scroll,BorderLayout.CENTER);
		contents.add(createToolBar(),BorderLayout.NORTH);
	}

	/** Create our tool bar. */
	protected Component createToolBar() {
		JPanel bar = new JPanel();
		bar.setLayout(new BoxLayout(bar,BoxLayout.X_AXIS));
		bar.setBorder(BorderFactory.createRaisedBevelBorder());
		bar.setAlignmentX(0.0f);	//left justify
		bar.setMaximumSize(
			new Dimension(Short.MAX_VALUE,Short.MAX_VALUE));

		pageLabel = new JLabel();
		bar.add(pageLabel);
		previousButton = new ButtonAction(
				    res,"PrintPreview.button.Previous",null) {
			public void action() {
				previousPage();
			}
		};
		bar.add(previousButton);

		nextButton = new ButtonAction(
				res,"PrintPreview.button.Next",null) {
			public void action() {
				nextPage();
			}
		};
		bar.add(nextButton);

		bar.add(new ButtonAction(
				res,"PrintPreview.button.Print",null) {
			public void action() {
				printPrintable();
			}
		});

		scaleChoice = new ComboBoxAction(res) {
			public void action() {
				setPageImagerSize();
				displayPage();	//repaint at new scale
			}
		};
		Object[] scaleDisplays = {
			"50%", "75%", "100%", "150%", "200%"
		};
		scaleChoice.setItems(scaleDisplays);
		scaleChoice.setValue("100%");
		bar.add(scaleChoice);

		return bar;
	}

	/** Set the page format for the page. */
	public void setPageFormat(PageFormat page) {
		this.page = page;
		setPageImagerSize();
	}

	/** Set the preferred size for our page imager based on the size
	 * of the page and the current scale factor.
	 */
	protected void setPageImagerSize() {
		if (page==null)
			return;		//ignore call during initialization
		double previewScale = getPreviewScale();
		//Get the page size plus a margin all around */
		int w = (int)(page.getWidth()*previewScale)+MARGIN*2;
		int h = (int)(page.getHeight()*previewScale)+MARGIN*2;
		pageImager.setPreferredSize(new Dimension(w,h));
		scroll.revalidate();
	}

	/** Display the previous page. */
	protected void previousPage() {
		if (pageIndex<=0) {
			if (pageCount==0)
				return;		//can't back up
			pageIndex = pageCount-1;
		} else
			pageIndex--;
		displayPage();
	}

	/** Display the next page. */
	protected void nextPage() {
		if (pageStatus==Printable.NO_SUCH_PAGE ||
		    (pageCount>0 && pageIndex>=pageCount-1))
			pageIndex = 0;	//wrap back to page 0
		else
			pageIndex++;
		displayPage();
	}

	/** Display the current page, after updating pageIndex */
	protected void displayPage() {
		setPageLabel();
		pageImager.repaint();
	}

	/** Set the value of the page label. */
	protected void setPageLabel() {
		String pageCountStr =
			(pageCount==0)?"?":Integer.toString(pageCount);
		Object[] pageArgs = { new Integer(pageIndex+1), pageCountStr };
		String pageStr = res.getResourceFormatted(
			"PrintPreview.label.PageNumber",pageArgs);
		pageLabel.setText(pageStr);
		if (pageCount>0) {
			//We know how many pages there are, so enable or
			//disable the Previous and Next buttons when we are
			//at the beginning or end
			previousButton.setEnabled(pageIndex>0);
			nextButton.setEnabled(pageIndex<pageCount-1);
		}
	}


	/** Get the number of pages in a Printable. */
	protected int getPageCount(Printable prt) {
		BufferedImage buf = new BufferedImage(1,1,
			BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = buf.createGraphics();
		int n;
		int status = Printable.PAGE_EXISTS;
		try {
			for (n=0; status==Printable.PAGE_EXISTS; n++) {
				status = prt.print(g2,page,n);
			}
		} catch (PrinterException ex) {
			return 0;	//unknown
		}
		return n-1;
	}

	/** Get the user-selected scale for previewing. */
	protected double getPreviewScale() {
		String s = (String)scaleChoice.getValue();
		s = s.substring(0,s.length()-1);	//drop trailing % char
		int percent = Integer.parseInt(s);
		return ((double)percent)/100.0;
	}

	/** Paint our page image.
	 * @param g Graphics context for pageImager
	 */
	protected void paintPage(Graphics g) {
		int pageWidth = (int)(page.getWidth());
		int pageHeight = (int)(page.getHeight());
		int xOff = MARGIN;
		int yOff = MARGIN;
		g.setColor(Color.gray);	//outside the page
		g.fillRect(0,0,pageImager.getWidth(),pageImager.getHeight());
		g.setColor(Color.white);	//the page
		Graphics2D g2 = (Graphics2D)g;
		double previewScale = getPreviewScale();
		g2.scale(previewScale,previewScale);
		g2.fillRect(xOff,yOff,pageWidth,pageHeight);
		Shape clip = new Rectangle2D.Double(
			xOff,yOff,pageWidth,pageHeight);
		g2.clip(clip);	//clip to the page
		g2.translate(xOff,yOff);
		try {
			pageStatus = printable.print(g2,page,pageIndex);
			if (pageStatus==Printable.NO_SUCH_PAGE) {
				pageCount = pageIndex;
			}
		} catch (PrinterException ex) {
			//ignore
		}
	}

	/** Print our Printable. */
	public void printPrintable() {
		PrinterJob pJob = PrinterJob.getPrinterJob();
		PageFormat pageFormat = page;
		//pageFormat = pJob.validatePage(pageFormat);
		//Validating just seems to force our margins to be at least
		//0.5in, which is too large for some of our reports.
		if (isZeroMargins()) {
			Paper oldPaper = pageFormat.getPaper();
			Paper newPaper = new Paper() {
				//Force our paper's imageable area
				//to the full size of the paper (no margins)
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
			newPaper.setSize(
				oldPaper.getWidth(),oldPaper.getHeight());
			pageFormat.setPaper(newPaper);
		}
		pJob.setPrintable(printable,pageFormat);
		if (!pJob.printDialog())
			return;		//cancelled
		try {
			pJob.print();
		} catch (PrinterException ex) {
			throw new MoreException(ex);
		}
	}
}

/* end */
