/* IqSplitPane.java
 *
 * Jim McBeath, October 19, 2000
 *
 * Copyright 2000 IqInVision under GPL
 */

package net.jimmc.swing;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JSplitPane;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;

/** An extension to javax.swing.SplitPane that allows you to swap the
 * left/right or top/bottom components by double-clicking on the
 * divider bar.
 * It would be better to add a little button to the divider bar,
 * like the one-touch expandable buttons, but the architecture of
 * swing makes that extremely difficult to do.
 */
public class IqSplitPane extends JSplitPane {
	/** Create a split pane. */
	public IqSplitPane(int orientation) {
		super(orientation);
		setResizeWeight(0.5);
		addSwapper();
	}

	/** Add the swapping funtionality. */
	protected void addSwapper() {
		SplitPaneUI ui = getUI();
		if (ui==null) {
System.out.println("IqSplitPane SplitPaneUI null");
			return;	//hmmm, need to do this later
		}
		if (!(ui instanceof BasicSplitPaneUI)) {
System.out.println("IqSplitPane SplitPaneUI not BasicSplitPaneUI");
			return;
		}
		BasicSplitPaneUI bb = (BasicSplitPaneUI)ui;
		BasicSplitPaneDivider div =
			hackBasicSplitPaneUI.hackGetDivider(bb);
		if (div==null) {
System.out.println("IqSplitPane div==null");
			return;
		}
		div.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent ev) {
				if (ev.getClickCount()==2)
					divDoubleClick();
			}
		});
/* Unfortunately, BasicSplitPaneDivider is derived from java.awt.Container,
 * so does not implement setToolTipText.  Bummer...
		String tip = IqSwingResources.getResourceStringStatic(
				"splitPane.divider.toolTip");
		div.setToolTipText(tip);
 */
	}

	/** Here on a double-click on the mouse. */
	public void divDoubleClick() {
		if (getOrientation()==HORIZONTAL_SPLIT) {
			Component left = getLeftComponent();
			Component right = getRightComponent();
			int leftSize = left.getWidth();
			int rightSize = right.getWidth();
			int loc = getDividerLocation();
			int newLoc = loc+rightSize-leftSize;
			setLeftComponent(null);
			setRightComponent(null);
			setLeftComponent(right);
			setRightComponent(left);
			setDividerLocation(newLoc);
		} else {
			Component top = getTopComponent();
			Component bottom = getBottomComponent();
			int topSize = top.getHeight();
			int bottomSize = bottom.getHeight();
			int loc = getDividerLocation();
			int newLoc = loc+bottomSize-topSize;
			setTopComponent(null);
			setBottomComponent(null);
			setTopComponent(bottom);
			setBottomComponent(top);
			setDividerLocation(newLoc);
		}
	}
}

class hackBasicSplitPaneUI extends BasicSplitPaneUI {
	public static BasicSplitPaneDivider hackGetDivider(BasicSplitPaneUI b) {
		return b.getDivider();
	}
}

/* end */
