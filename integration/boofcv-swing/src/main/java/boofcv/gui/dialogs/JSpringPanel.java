/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.gui.dialogs;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * Panel which uses {@link SpringLayout}. Contain functions that make working with Spring easier.
 *
 * @author Peter Abeles
 */
public class JSpringPanel extends JPanel {

	public SpringLayout layout;

	public JSpringPanel() {
		layout = new SpringLayout();
		setLayout(layout);
	}

	public JPanel createCenterStretched( JComponent left, JComponent right ) {
		JPanel panel = new JPanel();
		SpringLayout layout = new SpringLayout();
		panel.setLayout(layout);
		panel.add(left);
		panel.add(right);

		layout.putConstraint(SpringLayout.NORTH, left, 0, SpringLayout.NORTH, panel);
		layout.putConstraint(SpringLayout.NORTH, right, 0, SpringLayout.NORTH, panel);
		layout.putConstraint(SpringLayout.SOUTH, left, 0, SpringLayout.SOUTH, panel);
		layout.putConstraint(SpringLayout.SOUTH, right, 0, SpringLayout.SOUTH, panel);

		layout.putConstraint(SpringLayout.WEST, left, 0, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.EAST, left, -4, SpringLayout.HORIZONTAL_CENTER, panel);
		layout.putConstraint(SpringLayout.WEST, right, 4, SpringLayout.HORIZONTAL_CENTER, panel);
		layout.putConstraint(SpringLayout.EAST, right, 0, SpringLayout.EAST, panel);

		panel.setPreferredSize(new Dimension(0, 25));

		return panel;
	}

	public static JPanel createLockedSides( JComponent left, JComponent right, int height ) {
		JPanel panel = new JPanel();
		SpringLayout layout = new SpringLayout();
		panel.setLayout(layout);
		panel.add(left);
		panel.add(right);

		layout.putConstraint(SpringLayout.NORTH, left, 0, SpringLayout.NORTH, panel);
		layout.putConstraint(SpringLayout.NORTH, right, 0, SpringLayout.NORTH, panel);
		layout.putConstraint(SpringLayout.SOUTH, left, 0, SpringLayout.SOUTH, panel);
		layout.putConstraint(SpringLayout.SOUTH, right, 0, SpringLayout.SOUTH, panel);

		layout.putConstraint(SpringLayout.WEST, left, 0, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.EAST, right, 0, SpringLayout.EAST, panel);

		panel.setPreferredSize(new Dimension(0, height));

		return panel;
	}

	public void constrainWestNorthEast( JComponent target, @Nullable Component above, int offsetTop, int offsetSides ) {

		add(target);

		if (above == null)
			layout.putConstraint(SpringLayout.NORTH, target, offsetTop, SpringLayout.NORTH, this);
		else
			layout.putConstraint(SpringLayout.NORTH, target, offsetTop, SpringLayout.SOUTH, above);

		layout.putConstraint(SpringLayout.WEST, target, offsetSides, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, target, -offsetSides, SpringLayout.EAST, this);
	}

	public void constrainWestNorthEast( JComponent targetLeft, JComponent targetRight,
										@Nullable Component above,
										int offsetTop, int spaceLR, int offsetSides ) {

		add(targetLeft);
		add(targetRight);

		if (above == null) {
			layout.putConstraint(SpringLayout.NORTH, targetLeft, offsetTop, SpringLayout.NORTH, this);
			layout.putConstraint(SpringLayout.NORTH, targetRight, offsetTop, SpringLayout.NORTH, this);
		} else {
			layout.putConstraint(SpringLayout.NORTH, targetLeft, offsetTop, SpringLayout.SOUTH, above);
			layout.putConstraint(SpringLayout.NORTH, targetRight, offsetTop, SpringLayout.SOUTH, above);
		}

		layout.putConstraint(SpringLayout.WEST, targetLeft, offsetSides, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.WEST, targetRight, spaceLR, SpringLayout.EAST, targetLeft);
		layout.putConstraint(SpringLayout.EAST, targetRight, -offsetSides, SpringLayout.EAST, this);
	}

	public void constrainWestNorth( JComponent targetLeft, JComponent targetRight, @Nullable Component above,
									int offsetTop, int spaceLR, int offsetSides ) {
		add(targetLeft);
		add(targetRight);
		if (above == null) {
			layout.putConstraint(SpringLayout.NORTH, targetLeft, offsetTop, SpringLayout.NORTH, this);
			layout.putConstraint(SpringLayout.NORTH, targetRight, offsetTop, SpringLayout.NORTH, this);
		} else {
			layout.putConstraint(SpringLayout.NORTH, targetLeft, offsetTop, SpringLayout.SOUTH, above);
			layout.putConstraint(SpringLayout.NORTH, targetRight, offsetTop, SpringLayout.SOUTH, above);
		}

		layout.putConstraint(SpringLayout.WEST, targetLeft, offsetSides, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.WEST, targetRight, spaceLR, SpringLayout.EAST, targetLeft);
	}

	public void constrainWestNorth( JComponent target, Component above,
									int offsetTop, int offsetLeft ) {
		add(target);
		if (above == null) {
			layout.putConstraint(SpringLayout.NORTH, target, offsetTop, SpringLayout.NORTH, this);
		} else {
			layout.putConstraint(SpringLayout.NORTH, target, offsetTop, SpringLayout.SOUTH, above);
		}

		layout.putConstraint(SpringLayout.WEST, target, offsetLeft, SpringLayout.WEST, this);
	}

	public void constrainWestSouthEast( JComponent target, @Nullable Component below,
										int offsetBelow, int offsetSides ) {

		add(target);
		if (below == null)
			layout.putConstraint(SpringLayout.SOUTH, target, -offsetBelow, SpringLayout.SOUTH, this);
		else
			layout.putConstraint(SpringLayout.SOUTH, target, -offsetBelow, SpringLayout.NORTH, below);

		layout.putConstraint(SpringLayout.WEST, target, offsetSides, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, target, -offsetSides, SpringLayout.EAST, this);
	}

	public JSpinner spinner( int initial, int minimum, int maximum, int stepSize ) {
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(initial, minimum, maximum, stepSize));
		spinner.setMaximumSize(spinner.getPreferredSize());
		spinner.addChangeListener((ChangeListener)this);
		return spinner;
	}

	public JSpinner spinner( double initial, double minimum, double maximum, double stepSize ) {
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(initial, minimum, maximum, stepSize));
		spinner.setMaximumSize(spinner.getPreferredSize());
		spinner.addChangeListener((ChangeListener)this);
		return spinner;
	}

	public void constrainTopCenter( JComponent target, @Nullable JComponent top, int padTop ) {
		add(target);
		if (top == null) {
			layout.putConstraint(SpringLayout.NORTH, target, padTop, SpringLayout.NORTH, this);
		} else {
			layout.putConstraint(SpringLayout.NORTH, target, padTop, SpringLayout.SOUTH, top);
		}
		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, target, 0, SpringLayout.HORIZONTAL_CENTER, this);
	}

	public void constrainTopStretchH( JComponent target, @Nullable JComponent top, int padTop ) {
		add(target);
		if (top == null) {
			layout.putConstraint(SpringLayout.NORTH, target, padTop, SpringLayout.NORTH, this);
		} else {
			layout.putConstraint(SpringLayout.NORTH, target, padTop, SpringLayout.SOUTH, top);
		}
		layout.putConstraint(SpringLayout.WEST, target, 5, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, target, -5, SpringLayout.EAST, this);
	}

	public void constrainBottomCenter( JComponent target, JComponent bottom, int padV ) {
		add(target);
		constraintSouth(target, null, bottom, padV);
		layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, target, 0, SpringLayout.HORIZONTAL_CENTER, this);
	}

	public void constrainBottomStretchH( JComponent target, JComponent bottom, int padV ) {
		add(target);
		constraintSouth(target, null, bottom, padV);
		layout.putConstraint(SpringLayout.WEST, target, 5, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, target, -5, SpringLayout.EAST, this);
	}

	public void constrainStretchHV( JComponent target, @Nullable JComponent top, JComponent bottom, int padV ) {
		add(target);

		if (top == null) {
			layout.putConstraint(SpringLayout.NORTH, target, padV, SpringLayout.NORTH, this);
		} else {
			layout.putConstraint(SpringLayout.NORTH, target, padV, SpringLayout.SOUTH, top);
		}
		constraintSouth(target, top, bottom, padV);

		layout.putConstraint(SpringLayout.WEST, target, 5, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, target, -5, SpringLayout.EAST, this);
	}

	/**
	 * Constrain it to the top of it's bottom panel and prevent it from getting crushed below it's size
	 */
	public void constraintSouth( JComponent target, @Nullable JComponent top, @Nullable JComponent bottom, int padV ) {
		if (bottom == null) {
			layout.putConstraint(SpringLayout.SOUTH, target, -padV, SpringLayout.SOUTH, this);
		} else {
			Spring a = Spring.sum(Spring.constant(-padV), layout.getConstraint(SpringLayout.NORTH, bottom));
			Spring b;
			if (top == null)
				b = Spring.sum(Spring.height(target), layout.getConstraint(SpringLayout.NORTH, this));
			else
				b = Spring.sum(Spring.height(target), layout.getConstraint(SpringLayout.SOUTH, top));

			layout.getConstraints(target).setConstraint(SpringLayout.SOUTH, Spring.max(a, b));
		}
	}
}
