/* GridBagger.java
 *
 * Jim McBeath, June 28, 1997
 */

package net.jimmc.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

/** GridBagger is a helper class to simplify using GridBagLayout.
 * It includes a few utility routines to make the code using the
 * GridBagLayout cleaner.
 */

public class GridBagger {
	/** The container to which this GridBagger adds. */
	public Container container;

	/** The GridBagLayout used by this GridBagger. */
	public GridBagLayout gbl;

	/** The GridBagConstraints used by this GridBagger. */
	public GridBagConstraints gbc;

	/** Create a GridBagger for a Container. */
	public GridBagger(Container p) {
		container = p;
		gbl = new GridBagLayout();
		gbc = new GridBagConstraints();
		gbc.insets = new Insets(1,2,1,2);
		container.setLayout(gbl);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
	}

	/** Add a component in the next column of the same row. */
	public void add(Component c) {
		gbl.setConstraints(c,gbc);
		container.add(c);
		gbc.gridx++;	/* advance to the next column */
	}

	/** Add a component in the next column of the same row. */
	public void add(GridBagConstraints gbc2,Component c) {
		gbl.setConstraints(c,gbc2);
		container.add(c);
		gbc2.gridx++;	/* advance to the next column */
	}

	/** Add a component which is the last in the row. */
	public void addLast(Component c) {
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.gridx = GridBagConstraints.RELATIVE;
		add(c);
		gbc.gridwidth = 1;
	}

	/** Advance to the next row. */
	public void nextRow() {
		gbc.gridx = 0;
		gbc.gridy++;
	}
}

/* end */
