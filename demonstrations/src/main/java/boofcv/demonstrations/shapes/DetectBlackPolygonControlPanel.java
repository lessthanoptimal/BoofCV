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

package boofcv.demonstrations.shapes;

import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.ConfigRefinePolygonLineToImage;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.ConnectRule;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Peter Abeles
 */
public class DetectBlackPolygonControlPanel extends StandardAlgConfigPanel
		implements ActionListener, ChangeListener
{
	ShapeGuiListener owner;
	JSpinner spinnerContourConnect;

	public boolean bRefineGray = true;
	public ConfigRefinePolygonLineToImage refineGray;

	public ThresholdControlPanel thresholdPanel;

	JSpinner spinnerMinContourSize;
	JSpinner spinnerMinEdgeD; // threshold for detect
	JSpinner spinnerMinEdgeR; // threshold for refine
	JCheckBox setBorder;

	public PolylineControlPanel polylinePanel;

	JCheckBox setRefineContour;
	JCheckBox setRefineGray;
	JCheckBox setRemoveBias;

	JSpinner spinnerMinSides;
	JSpinner spinnerMaxSides;

	JSpinner spinnerLineSamples;
	JSpinner spinnerCornerOffset;
	JSpinner spinnerSampleRadius;
	JSpinner spinnerRefineMaxIterations;
	JSpinner spinnerConvergeTol;
	JSpinner spinnerMaxCornerChange;

	public ConfigPolygonDetector config;

	public int minSides;
	public int maxSides;

	// todo add back control for min/max sides

	public DetectBlackPolygonControlPanel(ShapeGuiListener owner) {
		this(owner,new ConfigPolygonDetector(3,6),null);
	}

	public DetectBlackPolygonControlPanel(ShapeGuiListener owner,
										  ConfigPolygonDetector configPolygon, ConfigThreshold configThreshold ) {
		setBorder(BorderFactory.createEmptyBorder());
		this.owner = owner;
		this.config = configPolygon;
		configPolygon.detector.maximumSides = Math.min(50,configPolygon.detector.maximumSides);

		minSides = configPolygon.detector.minimumSides;
		maxSides = configPolygon.detector.maximumSides;
		refineGray = configPolygon.refineGray != null ? configPolygon.refineGray : new ConfigRefinePolygonLineToImage();
		spinnerContourConnect = spinner(configPolygon.detector.contourRule.ordinal(), ConnectRule.values());

		thresholdPanel = configThreshold == null ?
				new ThresholdControlPanel(owner) :
				new ThresholdControlPanel(owner,configThreshold);

		if( configPolygon.detector.minimumContour.isFixed()) {
			spinnerMinContourSize = spinner(configPolygon.detector.minimumContour.getLengthI(),5, 10000, 2);
		} else {
			spinnerMinContourSize = spinner(configPolygon.detector.minimumContour.fraction,0, 1.0,0.05,1,2);
		}

		spinnerMinSides = spinner(minSides, 3, 50, 1);
		spinnerMaxSides = spinner(maxSides, 3, 50, 1);
		spinnerMinEdgeD = spinner(configPolygon.detector.minimumEdgeIntensity, 0.0,255.0,1.0);
		spinnerMinEdgeR = spinner(configPolygon.minimumRefineEdgeIntensity, 0.0,255.0,1.0);

		polylinePanel = new PolylineControlPanel(owner,configPolygon.detector.contourToPoly);

		setBorder = new JCheckBox("Image Border");
		setBorder.addActionListener(this);
		setBorder.setSelected(configPolygon.detector.canTouchBorder);

		setRefineContour = checkbox("Refine Contour",configPolygon.refineContour);
		setRefineGray = checkbox("Refine Gray",configPolygon.refineGray != null);
		setRemoveBias = checkbox("Remove Bias",configPolygon.adjustForThresholdBias);
		spinnerLineSamples = spinner(refineGray.lineSamples, 5, 100, 1);
		spinnerCornerOffset = spinner(refineGray.cornerOffset, 0, 10, 1);
		spinnerSampleRadius = spinner(refineGray.sampleRadius, 1, 10, 1);
		spinnerRefineMaxIterations = spinner(refineGray.maxIterations, 1, 200, 1);
		spinnerConvergeTol = spinner(refineGray.convergeTolPixels, 0.0, 2.0, 0.005,1,3);
		spinnerMaxCornerChange = spinner(refineGray.maxCornerChangePixel, 0.0, 50.0, 1.0,1,3);

		StandardAlgConfigPanel refinePanel = new StandardAlgConfigPanel();
		refinePanel.addAlignLeft(setRemoveBias);
		refinePanel.addAlignLeft(setRefineContour);
		refinePanel.addAlignLeft(setRefineGray);
		refinePanel.addLabeled(spinnerLineSamples, "Line Samples: ");
		refinePanel.addLabeled(spinnerCornerOffset, "Corner Offset: ");
		refinePanel.addLabeled(spinnerSampleRadius, "Sample Radius: ");
		refinePanel.addLabeled(spinnerRefineMaxIterations, "Iterations: ");
		refinePanel.addLabeled(spinnerConvergeTol, "Converge Tol Pixels: ");
		refinePanel.addLabeled(spinnerMaxCornerChange, "Max Corner Change: ");

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Threshold", thresholdPanel);
		tabbedPane.addTab("Polyline", polylinePanel);
		tabbedPane.addTab("Refine", refinePanel);

		addLabeled(spinnerContourConnect,"Contour Connect: ");
		addLabeled(spinnerMinContourSize, "Min Contour Size: ");
		addLabeled(spinnerMinEdgeD, "Edge Intensity D: ");
		addLabeled(spinnerMinEdgeR, "Edge Intensity R: ");
		addLabeled(spinnerMinSides,"Min Sides");
		addLabeled(spinnerMaxSides,"Max Sides");

		addAlignLeft(setBorder, this);
		add(tabbedPane);

		addVerticalGlue(this);
	}

	/**
	 * Removes controls related to the number of sides in the polygon. useful when it's not something that
	 * can actually be tuned. ONLY CALL BEFORE DISPLAYED OR INSIDE THE UI THREAD.
	 */
	public void removeControlNumberOfSides() {
		removeChildInsidePanel(this,spinnerMinSides);
		removeChildInsidePanel(this,spinnerMaxSides);
		validate();
		polylinePanel.removeControlNumberOfSides();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == setBorder ) {
			config.detector.canTouchBorder = setBorder.isSelected();
			owner.configUpdate();
		} else if( e.getSource() == setRefineContour ) {
			config.refineContour = setRefineContour.isSelected();
			owner.configUpdate();
		} else if( e.getSource() == setRefineGray ) {
			bRefineGray = setRefineGray.isSelected();
			owner.configUpdate();
		} else if( e.getSource() == setRemoveBias ) {
			config.adjustForThresholdBias = setRemoveBias.isSelected();
			owner.configUpdate();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == spinnerMinEdgeD) {
			config.detector.minimumEdgeIntensity = ((Number) spinnerMinEdgeD.getValue()).doubleValue();
		} else if( e.getSource() == spinnerMinSides ) {
			minSides = ((Number) spinnerMinSides.getValue()).intValue();
			if( minSides > maxSides ) {
				maxSides = minSides;
				spinnerMaxSides.setValue(minSides);
			}
			updateSidesInConfig();
		} else if( e.getSource() == spinnerMaxSides ) {
			maxSides = ((Number) spinnerMaxSides.getValue()).intValue();
			if (maxSides < minSides) {
				minSides = maxSides;
				spinnerMinSides.setValue(minSides);
			}
			updateSidesInConfig();
		} else if( e.getSource() == spinnerMinEdgeR) {
			config.minimumRefineEdgeIntensity = ((Number) spinnerMinEdgeR.getValue()).doubleValue();
		} else if( e.getSource() == spinnerMinContourSize ) {
			if( config.detector.minimumContour.isRelative())
				config.detector.minimumContour.fraction = ((Number) spinnerMinContourSize.getValue()).doubleValue();
			else
				config.detector.minimumContour.length = ((Number) spinnerMinContourSize.getValue()).intValue();
		} else if (e.getSource() == spinnerLineSamples) {
			refineGray.lineSamples = ((Number) spinnerLineSamples.getValue()).intValue();
		} else if (e.getSource() == spinnerCornerOffset) {
			refineGray.cornerOffset = ((Number) spinnerCornerOffset.getValue()).intValue();
		} else if (e.getSource() == spinnerSampleRadius) {
			refineGray.sampleRadius = ((Number) spinnerSampleRadius.getValue()).intValue();
		} else if (e.getSource() == spinnerRefineMaxIterations) {
			refineGray.maxIterations = ((Number) spinnerRefineMaxIterations.getValue()).intValue();
		} else if (e.getSource() == spinnerConvergeTol) {
			refineGray.convergeTolPixels = ((Number) spinnerConvergeTol.getValue()).doubleValue();
		} else if (e.getSource() == spinnerMaxCornerChange) {
			refineGray.maxCornerChangePixel = ((Number) spinnerMaxCornerChange.getValue()).doubleValue();
		} else if( e.getSource() == spinnerContourConnect ) {
			config.detector.contourRule = (ConnectRule)spinnerContourConnect.getValue();
		}
		owner.configUpdate();
	}

	private void updateSidesInConfig() {
		config.detector.minimumSides = minSides;
		config.detector.maximumSides = maxSides;
	}

	public ThresholdControlPanel getThresholdPanel() {
		return thresholdPanel;
	}

	public ConfigPolygonDetector getConfigPolygon() {
		if( polylinePanel.whichAlgorithm == 0 ) {
			config.detector.contourToPoly = polylinePanel.getConfigSplitMerge();
		} else {
			config.detector.contourToPoly = polylinePanel.getConfigSplitMergeOld();
		}
		if( bRefineGray ) {
			config.refineGray = refineGray;
		} else {
			config.refineGray = null;
		}
		return config;
	}
}
