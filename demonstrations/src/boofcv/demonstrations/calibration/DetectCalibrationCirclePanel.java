/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

	double circleRadius;
	double circleSpacing;

	boolean showGraphs;

	public DetectCalibrationCirclePanel(int gridRows, int gridColumns, double radius , double spacing,
										boolean showGraphs ) {
		super(gridRows, gridColumns, true, false);

		this.showGraphs = showGraphs;
		this.circleRadius = radius;
		this.circleSpacing = spacing;

		this.showClusters = new JCheckBox("Show Clusters");
		this.showClusters.setSelected(doShowClusters);
		this.showClusters.addItemListener(this);
		this.showClusters.setMaximumSize(this.showClusters.getPreferredSize());
		selectDiameter = spinner(radius*2,0.0,1000.0,1.0);
		selectSpacing = spinner(spacing,0.0,1000.0,1.0);

		addComponents(true);
	}

	@Override
	protected void addComponents( boolean hasManualMode ) {
		addLabeled(successIndicator, "Found:", this);
		addSeparator(100);
		addLabeled(viewSelector, "View ", this);
		addSeparator(100);
		addLabeled(selectRows, "Rows", this);
		addLabeled(selectColumns, "Cols", this);
		addLabeled(selectDiameter, "Diameter", this);
		addLabeled(selectSpacing, "Spacing", this);
		if( hasManualMode )
			addAlignLeft(manualThreshold,this);
		addLabeled(thresholdSpinner, "Threshold", this);
		addSeparator(100);
		addLabeled(selectZoom, "Zoom ", this);
		addAlignLeft(showPoints, this);
		addAlignLeft(showNumbers,this);
		addAlignLeft(showClusters,this);
		if( showGraphs )
			addAlignLeft(showGraph,this);
		addAlignLeft(showGrids,this);
		addAlignLeft(showOrder,this);
		addAlignLeft(showShapes, this);
		addAlignLeft(showContour, this);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if( e.getSource() == showClusters ) {
			doShowClusters = showClusters.isSelected();
			listener.calibEventGUI();
		} else {
			super.itemStateChanged(e);
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == selectDiameter) {
			circleRadius = ((Number) selectDiameter.getValue()).doubleValue()/2.0;
			listener.calibEventDetectorModified();
		}  else if( e.getSource() == selectSpacing) {
			circleSpacing = ((Number) selectSpacing.getValue()).doubleValue();
			listener.calibEventDetectorModified();
		} else {
			super.stateChanged(e);
		}
	}

	public double getCircleRadius() {
		return circleRadius;
	}

	public double getCircleSpacing() {
		return circleSpacing;
	}
}
