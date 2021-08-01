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

package boofcv.demonstrations.calibration;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ItemEvent;

/**
 * Added controls for patterns composed of circles
 *
 * @author Peter Abeles
 */
public class DetectCalibrationCirclePanel extends DetectCalibrationPanel {

	JSpinner selectDiameter;
	JSpinner selectSpacing;

	JCheckBox showClusters;

	double circleDiameter;
	double circleSpacing;

	boolean showGraphs;

	public DetectCalibrationCirclePanel( int gridRows, int gridColumns, double diameter, double spacing,
										 boolean showGraphs ) {
		super(gridRows, gridColumns, false);

		doShowNumbers = false;
		this.showNumbers.setSelected(doShowNumbers);

		this.showGraphs = showGraphs;
		this.circleDiameter = diameter;
		this.circleSpacing = spacing;

		this.showClusters = new JCheckBox("Show Clusters");
		this.showClusters.setSelected(doShowClusters);
		this.showClusters.addItemListener(this);
		this.showClusters.setMaximumSize(this.showClusters.getPreferredSize());
		selectDiameter = spinner(diameter, 0.0, 1000.0, 1.0);
		selectSpacing = spinner(spacing, 0.0, 1000.0, 1.0);

		addComponents();
	}

	@Override
	protected void addComponents() {
		JPanel togglePanel = new JPanel(new GridLayout(0, 2));
		togglePanel.add(showPoints);
		togglePanel.add(showNumbers);
		addAlignLeft(showClusters, this);
		if (showGraphs)
			addAlignLeft(showGraph, this);
		togglePanel.add(showGrids);
		togglePanel.add(showOrder);
		togglePanel.add(showShapes);
		togglePanel.add(showContour);

		togglePanel.setMaximumSize(togglePanel.getMinimumSize());

		addLabeled(labelSpeed, "Time (ms)");
		addLabeled(successIndicator, "Found:");
		add(viewInfo);
		addLabeled(viewSelector, "View ");
		addLabeled(selectRows, "Rows");
		addLabeled(selectColumns, "Cols");
		addLabeled(selectDiameter, "Diameter");
		addLabeled(selectSpacing, "Spacing");
		add(togglePanel);
		add(threshold);
	}

	@Override
	public void itemStateChanged( ItemEvent e ) {
		if (e.getSource() == showClusters) {
			doShowClusters = showClusters.isSelected();
			listener.calibEventGUI();
		} else {
			super.itemStateChanged(e);
		}
	}

	@Override
	public void stateChanged( ChangeEvent e ) {
		if (e.getSource() == selectDiameter) {
			circleDiameter = ((Number)selectDiameter.getValue()).doubleValue();
			listener.calibEventDetectorModified();
		} else if (e.getSource() == selectSpacing) {
			circleSpacing = ((Number)selectSpacing.getValue()).doubleValue();
			listener.calibEventDetectorModified();
		} else {
			super.stateChanged(e);
		}
	}

	public double getCircleDiameter() {
		return circleDiameter;
	}

	public double getCircleSpacing() {
		return circleSpacing;
	}
}
