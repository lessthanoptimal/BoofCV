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

import boofcv.demonstrations.shapes.DetectBlackPolygonControlPanel;
import boofcv.demonstrations.shapes.ShapeGuiListener;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.shape.ConfigPolygonDetector;

import javax.swing.event.ChangeListener;
import java.awt.event.ItemListener;

/**
 * Shows calibration grid detector status, configure display, and adjust parameters.
 *
 * @author Peter Abeles
 */
public class DetectCalibrationPolygonPanel extends DetectCalibrationPanel
		implements ChangeListener, ItemListener
{
	// selects threshold to create binary image from
	DetectBlackPolygonControlPanel polygonPanel;

	public DetectCalibrationPolygonPanel(int gridRows, int gridColumns,
										 ConfigPolygonDetector configPolygon , ConfigThreshold configThreshold ) {
		super(gridRows,gridColumns,false);

		polygonPanel = new DetectBlackPolygonControlPanel(new ShapeGuiListener() {
			@Override
			public void configUpdate() {
				listener.calibEventDetectorModified();
			}

			@Override
			public void viewUpdated() {}

			@Override
			public void imageThresholdUpdated() {
				listener.calibEventDetectorModified();
			}
		},configPolygon,configThreshold);
		polygonPanel.removeControlNumberOfSides();

		threshold = null;
		addComponents();
	}

	@Override
	protected void addComponents() {
		addLabeled(successIndicator, "Found");
		add(viewInfo);
		addLabeled(viewSelector, "View ");
		addLabeled(selectRows, "Rows");
		addLabeled(selectColumns, "Cols");
		addAlignLeft(showPoints);
		addAlignLeft(showNumbers);
		addAlignLeft(showGraph);
		addAlignLeft(showGrids);
		addAlignLeft(showOrder);
		addAlignLeft(showShapes);
		addAlignLeft(showContour);
		addAlignLeft(polygonPanel);
	}
}
