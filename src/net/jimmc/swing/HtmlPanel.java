/* HtmlPanel.java
 *
 * Jim McBeath, October 12, 2001
 */

package net.jimmc.swing;

import net.jimmc.util.LimitedList;
import net.jimmc.util.MoreException;
import net.jimmc.util.ResourceSource;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

/** A panel to display HTML text. */
public class HtmlPanel extends JScrollPane
		implements HyperlinkListener, Printable {
	/** Special tags in the HTML file start with this prefix. */
	public static final String PRINT_PREFIX = "<!--PRINT ";
	public static final String PRINT_MARGINS_KEY = "<!--PRINT MARGINS";
	public static final String PRINT_PAGEBREAK_KEY = "<!--PRINT PAGEBREAK";
	public static final String PRINT_CLASS_PAGEBREAK_KEY =
		"class=\"PRINT PAGEBREAK\"";
	public static final String PRINT_SCALE_KEY = "<!--PRINT SCALE";
	public static final String PRINT_ZEROMARGIN_KEY =
			"<!--PRINT ZEROMARGIN";

	/** Initial history size. */
	public static final int INITIAL_HISTORY_SIZE = 100;

	/** Our resource source. */
	protected ResourceSource res;

	/** The editor panel. */
	protected JEditorPane editor;

	/** A set of editor panels used for printing when there are
	 * explicit page breaks.
	 */
	protected JEditorPane[] pageEds;

	/** Map to use when we have explicit page breaks.
	 * When we have explicit page breaks, it's possible that the
	 * text between any two page breaks takes more than one page.
	 * If so, we can't just count the page breaks to get to the
	 * requested page.  So we build a mapping that gives us the
	 * number of page breaks we need to skip and the index of
	 * the page within that page break section.
	 */
	int[][] pageBreakMap;	//array of int[2]

	/** Our history. */
	protected LimitedList history;

	/** The index of the currently displayed item. */
	protected int historyPosition;

	/** The location of the currently displayed page, or null
	 * if we are displaying an internal string. */
	protected String currentUrl;

	/** The ComponentPrintDialog we use. */
	protected ComponentPrintDialog printDialog;

	/** Create the Html panel. */
	public HtmlPanel(ResourceSource res) {
		this.res = res;
		editor = new JEditorPane();
		setViewportView(editor);
		editor.setContentType("text/html");
		editor.setEditable(false);
		editor.addHyperlinkListener(this);
		history = new LimitedList(INITIAL_HISTORY_SIZE);
		historyPosition = -1;
	}

	/** Set the background color. */
	public void setBackground(Color color) {
		super.setBackground(color);
		if (editor!=null)
			editor.setBackground(color);
	}

	/** Get the location of the currently displayed page, or null if
	 * the page is internally generated (was set by a call to
	 * {@link #showHtml}).
	 */
	public String getUrl() {
		return currentUrl;
	}

	/** Get the contents of the currently displayed page.
	 */
	public String getText() {
		return editor.getText();
	}

	/** Show the contents of a URL. */
	public void showUrl(String url) {
		showUrlNoHistory(url);
		addToHistory(url);
	}

	/** Show the contents of a URL, but don't add it to the history.
	 */
	public void showUrlNoHistory(String url) {
		pageEds = null;
		if (url.startsWith("html:")) {
			showHtmlNoHistory(url.substring(5));
			currentUrl = url;
			return;
		}
		Document doc = editor.getDocument();
		try {
			editor.setPage(url);
		} catch (IOException ex) {
			editor.setDocument(doc);	//reset to the old page
			String msg = res.getResourceFormatted(
				"error.CantOpenUrl",url);
			showHtmlNoHistory(msg);
		}
		currentUrl = url;
	}

	/** Show an html string. */
	public void showHtml(String html) {
		showHtmlNoHistory(html);
		addToHistory("html:"+html);
	}

	/** Show an html string, but don't add it to the history.
	 */
	public void showHtmlNoHistory(String html) {
		pageEds = null;
		//When we call editor.setText after editor.setPage, and then
		//go back and call setPage on the same URL, it doesn't show
		//us the URL. Apparently it thinks it is still showing that
		//URL.  So we clear out the document here to make it know.
		editor.setEditorKit(new HTMLEditorKit());
		editor.setContentType("text/html");
		editor.setText(html);
		currentUrl = null;
	}

	/** Add an item to the history list. */
	public void addToHistory(String item) {
		if (historyPosition<history.size()-1) {
			//Discard all items past the current position
			history.truncate(historyPosition+1);
		}
		history.addLimited(item);
		historyPosition = history.size()-1;
		fireHistoryChangeEvent();
	}

	/** Back up the position by one and show the resulting item.
	 * @return True if we were able to back up; false if we are
	 *         already at the beginning of the list.
	 */
	public boolean showBack() {
		if (!hasBack())
			return false;	//already at the start of the list
		historyPosition--;
		showUrlNoHistory((String)(history.get(historyPosition)));
		fireHistoryChangeEvent();	//position changed
		return true;
	}

	/** Move the position forward by one and show the resulting item.
	 * @return True if we were able to go forward; false if we are
	 *         already at the end of the list.
	 */
	public boolean showForward() {
		if (!hasForward())
			return false;	//already at the end of the list
		historyPosition++;
		showUrlNoHistory((String)(history.get(historyPosition)));
		fireHistoryChangeEvent();	//position changed
		return true;
	}

	/** True if a call to showBack will work. */
	public boolean hasBack() {
		return (historyPosition>0);
	}

	/** True if a call to showForward will work. */
	public boolean hasForward() {
		return (historyPosition<history.size()-1);
	}

	/** Here when the history changes.
	 * Eventually, this method could have a listener list that it invokes,
	 * but for now we just let users subclass this class and override
	 * this method.
	 */
	public void fireHistoryChangeEvent() {
		//do nothing
	}

	/** Print our contents.
	 * @param parent The controlling frame.
	 * @return true if printed, false if cancelled
	 */
	public boolean print(JsFrame parent) {
		//If our HTML text contains printer controls, then we use
		//our own print method to print it; otherwise, use the
		//generic print stuff in ComponentPrintDialog to print
		//our JEditorPane.
		JComponent p;
		String text = getText();
		if (text.indexOf(PRINT_PREFIX)>0 ||
		    text.indexOf(PRINT_CLASS_PAGEBREAK_KEY)>0)
			p = this;
		else
			p = editor;
		printDialog = new ComponentPrintDialog(parent,res,p);

		//Check for margins
		int marginKeyStart;
		if (text.indexOf(PRINT_ZEROMARGIN_KEY)>0)
			printDialog.setZeroMargins(true);
		else if ((marginKeyStart=text.indexOf(PRINT_MARGINS_KEY))>0) {
			int marginStart =
				marginKeyStart+PRINT_MARGINS_KEY.length();
			int marginEnd = text.indexOf("-->",marginKeyStart);
			String marginStr= text.substring(marginStart,marginEnd);
			double margin = Double.parseDouble(marginStr);
			printDialog.setMargin(margin);
		}

		if (!printDialog.printDialog())
			return false;		//cancelled
		try {
			printDialog.print();
		} catch (PrinterException ex) {
			throw new MoreException(ex);
		}
		return true;		//printed it
	}

	/** Here when the cursor enters a hyperlink */
	public void linkEntered(URL url) {
		//do nothing; let subclass override
	}

	/** Here when the cursor exits a hyperlink */
	public void linkExited(URL url) {
		//do nothing; let subclass override
	}

	/** Get the Nth page of the specified text.
	 * @param text The full html text.
	 * @param pageIndex The 0-based page index to retrieve.
	 * @return The specified page, with proper html tags around it,
	 *         or null if no such page.
	 */
	protected String getPageText(String text, int pageIndex) {
		//Find the starting point
		int textStart = 0;
		for (int i=0; i<pageIndex; i++) {
			//Find the next instance of either kind of pagebreak
			int cTextStart = text.indexOf(PRINT_CLASS_PAGEBREAK_KEY,
							textStart);
			textStart = text.indexOf(PRINT_PAGEBREAK_KEY,textStart);
			if (textStart<0 && cTextStart<0)
				return null;
			if (cTextStart>=0 &&
			    (textStart<0 || cTextStart<textStart)) {
				//Skip the element containing the class
				//pagebreak key
				textStart = text.indexOf('>',cTextStart)+1;
			} else {
				//Skip the entire line containing the
				//comment pagebreak key
				textStart = text.indexOf('\n',textStart);
			}
			if (textStart<=0)
				return null;
		}

		StringBuffer sb = new StringBuffer();
		if (textStart>0) {
			//If we are not at the first page, add enough HTML
			//to begin a valid HTML document
			sb.append("<html><body>\n");
		}

		//Find the ending point
		int textEnd = text.indexOf(PRINT_PAGEBREAK_KEY,textStart+2);
		int cTextEnd = text.indexOf(PRINT_CLASS_PAGEBREAK_KEY,
						textStart+2);
		if (textEnd<0 && cTextEnd<0) {
			//must be the last page, use the rest of the text
			sb.append(text.substring(textStart));
		} else {
			if (cTextEnd>=0 && (textEnd<0 || cTextEnd<textEnd)) {
				textEnd = text.lastIndexOf('<',cTextEnd);
			}
			sb.append(text.substring(textStart,textEnd));
			//Add HTML to end a valid HTML document
			sb.append("</body></html>\n");
		}
		String pageText = sb.toString();
		return pageText;
	}

	/** Set the scaling for the page. */
	protected double getPageScale(String text) {
		//See if there is a scale factor
		int scaleKeyStart = text.indexOf(PRINT_SCALE_KEY);
		double scale = 72.0/100.0;
			//default scale to go from browser's standard 100dpi
			//to printer points, 72 per inch.
		if (scaleKeyStart>0) {
			int scaleStart =
				scaleKeyStart+PRINT_SCALE_KEY.length()+1;
			int scaleEnd = text.indexOf("-->",scaleKeyStart);
			String scaleStr = text.substring(scaleStart,scaleEnd);
			scale = Double.parseDouble(scaleStr);
		}
		return scale;
	 }

	/** Given the complete text, split it up into pages at the page
	 * breaks and create a list of editor components for each page.
	 * @param text The entire HTML text.
	 * @param pageFormat The page to which we are printing.
	 */
	protected JEditorPane[] createPageEds(String text,
				PageFormat pageFormat) {
		Vector v = new Vector();
		int pageIndex = 0;
		String pageText;
		while ((pageText=getPageText(text,pageIndex))!=null) {
			JEditorPane p = createPageEd(pageText,pageFormat);
			v.addElement(p);
			pageIndex++;
		}
		JEditorPane[] pa = new JEditorPane[v.size()];
		v.copyInto(pa);
		return pa;
	}

	/** Create the editor pane for a page.
	 * @param text The HTML text to put on the page.
	 * @param pageFormat The page to which we are printing.
	 */
	protected JEditorPane createPageEd(String text, PageFormat pageFormat) {
		double scale = getPageScale(text);
		JEditorPane p = new JEditorPane();
		p.setEditorKit(new HTMLEditorKit());
		p.setContentType("text/html");
		int width = (int)pageFormat.getImageableWidth();
		int height = (int)pageFormat.getImageableHeight();
		int pWidth = (int)(width/scale);
		int pHeight = (int)(height/scale);
		p.setSize(pWidth,pHeight);
		p.setText(text);
		//Now set the height to the preferred height so that all
		//of the text fits.
		Dimension pSize = p.getPreferredSize();
		p.setSize(pWidth,(int)pSize.getHeight());
		return p;
	}

	/** Given the list of editor panes for the page-break pages,
	 * generate the page break map to convert
	 * from a page index to a page-break index and a page index within
	 * that page break.
	 */
	protected int[][] createPageBreakMap(JEditorPane[] pageEds,
				PageFormat pageFormat) {
		Vector v = new Vector();
		for (int px=0; px<pageEds.length; px++) {
			int pageCount = printDialog.getPageCount(
						pageEds[px],pageFormat);
			for (int p=0; p<pageCount; p++) {
				int[] pMap = new int[2];
				pMap[0] = px;	//page break index
				pMap[1] = p;	//page within page break
				v.addElement(pMap);
			}
		}
		int[][] pbMap = new int[v.size()][];
		v.copyInto(pbMap);
		return pbMap;
	}

    //The HyperlinkListener interface
	/** Handle a hyperlink event. */
	public void hyperlinkUpdate(HyperlinkEvent ev) {
		HyperlinkEvent.EventType evType = ev.getEventType();
		if (evType==HyperlinkEvent.EventType.ACTIVATED) {
			showUrl(ev.getURL().toString());
		} else if (evType==HyperlinkEvent.EventType.ENTERED) {
			linkEntered(ev.getURL());
		} else if (evType==HyperlinkEvent.EventType.EXITED) {
			linkExited(ev.getURL());
		}
		//do nothing if we don't understand it
	}
    //End HyperlinkListener interface

    //The Printable interface
    	public int print(Graphics graphics, PageFormat pageFormat,
						int pageIndex) {
		if (pageEds==null) {
			//Create our list of pages the first time through
			String text = getText();
			boolean hasPageBreaks =
				(text.indexOf(PRINT_CLASS_PAGEBREAK_KEY)>=0 ||
				 text.indexOf(PRINT_PAGEBREAK_KEY)>=0);
			if (hasPageBreaks) {
				pageEds = createPageEds(text,pageFormat);
				pageBreakMap =
					createPageBreakMap(pageEds,pageFormat);
			} else {
				pageEds = new JEditorPane[1];
				pageEds[0] = createPageEd(text,pageFormat);
				pageBreakMap = null;	//no page breaks
			}
		}

		//Figure out which page-break to use, and which page within
		//that page break
		JEditorPane pageEd;
		if (pageBreakMap==null) {
			pageEd = pageEds[0];
			//leave pageIndex as is
		} else {
			if (pageIndex>pageBreakMap.length-1)
				return NO_SUCH_PAGE;
			int pageBreakIndex = pageBreakMap[pageIndex][0];
			pageEd = pageEds[pageBreakIndex];
			pageIndex = pageBreakMap[pageIndex][1];
				//the page within this page-break
		}

		//Use the print dialog printer to print the selected page
		return printDialog.printTarget(pageEd,graphics,
				pageFormat,pageIndex);
	}
    //End Printable interface
}

/* end */
