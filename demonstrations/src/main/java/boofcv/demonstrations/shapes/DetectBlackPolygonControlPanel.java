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

import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.ConfigRefinePolygonLineToImage;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.ConfigLength;
import boofcv.struct.ConnectRule;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

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

	public ThresholdControlPanel threshold;

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
		this(owner,new ConfigPolygonDetector(3,6));
	}

	public DetectBlackPolygonControlPanel(ShapeGuiListener owner, ConfigPolygonDetector config) {
		setBorder(BorderFactory.createEmptyBorder());
		this.owner = owner;
		this.config = config;
		config.detector.minimumContour = ConfigLength.fixed(20);
		minSides = config.detector.minimumSides;
		maxSides = config.detector.maximumSides;
		refineGray = config.refineGray != null ? config.refineGray : new ConfigRefinePolygonLineToImage();
		spinnerContourConnect = spinner(config.detector.contourRule.ordinal(), ConnectRule.values());

		threshold = new ThresholdControlPanel(owner);

		spinnerMinContourSize = new JSpinner(new SpinnerNumberModel(config.detector.minimumContour.length,
				5,10000,2));
		spinnerMinContourSize.setMaximumSize(spinnerMinContourSize.getPreferredSize());
		spinnerMinContourSize.addChangeListener(this);

		spinnerMinSides = spinner(minSides, 3, 20, 1);
		spinnerMaxSides = spinner(maxSides, 3, 20, 1);
		spinnerMinSides = new JSpinner(new SpinnerNumberModel(minSides, 3, 20, 1));
		spinnerMinSides.setMaximumSize(spinnerMinSides.getPreferredSize());
		spinnerMinSides.addChangeListener(this);
		spinnerMaxSides = new JSpinner(new SpinnerNumberModel(maxSides, 3, 20, 1));
		spinnerMaxSides.setMaximumSize(spinnerMaxSides.getPreferredSize());
		spinnerMaxSides.addChangeListener(this);
		JPanel sidesPanel = new JPanel();
		sidesPanel.setLayout(new BoxLayout(sidesPanel,BoxLayout.X_AXIS));
		sidesPanel.add( new JLabel("Sides:"));
		sidesPanel.add(spinnerMinSides);
		sidesPanel.add(Box.createRigidArea(new Dimension(10,10)));
		sidesPanel.add( new JLabel("to"));
		sidesPanel.add(Box.createRigidArea(new Dimension(10,10)));
		sidesPanel.add(spinnerMaxSides);

		spinnerMinEdgeD = spinner(config.detector.minimumEdgeIntensity, 0.0,255.0,1.0);
		spinnerMinEdgeR = spinner(config.minimumRefineEdgeIntensity, 0.0,255.0,1.0);

		polylinePanel = new PolylineControlPanel(owner,config.detector.contourToPoly);

		setBorder = new JCheckBox("Image Border");
		setBorder.addActionListener(this);
		setBorder.setSelected(config.detector.canTouchBorder);

		setRefineContour = new JCheckBox("Refine Contour");
		setRefineContour.setSelected(config.refineContour);
		setRefineContour.addActionListener(this);
		setRefineGray = new JCheckBox("Refine Gray");
		setRefineGray.setSelected(config.refineGray != null);
		setRefineGray.addActionListener(this);
		setRemoveBias = new JCheckBox("Remove Bias");
		setRemoveBias.setSelected(config.adjustForThresholdBias);
		setRemoveBias.addActionListener(this);
		spinnerLineSamples = spinner(refineGray.lineSamples, 5, 100, 1);
		spinnerCornerOffset = spinner(refineGray.cornerOffset, 0, 10, 1);
		spinnerSampleRadius = spinner(refineGray.sampleRadius, 1, 10, 1);
		spinnerRefineMaxIterations = spinner(refineGray.maxIterations, 1, 200, 1);
		spinnerConvergeTol = spinner(refineGray.convergeTolPixels, 0.0, 2.0, 0.005);
		configureSpinnerFloat(spinnerConvergeTol);
		spinnerMaxCornerChange = spinner(refineGray.maxCornerChangePixel, 0.0, 50.0, 1.0);
		configureSpinnerFloat(spinnerMaxCornerChange);

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
		tabbedPane.addTab("Threshold", threshold);
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

	private void configureSpinnerFloat( JSpinner spinner ) {
		JSpinner.NumberEditor editor = (JSpinner.NumberEditor)spinner.getEditor();
		DecimalFormat format = editor.getFormat();
		format.setMinimumFractionDigits(3);
		format.setMinimumIntegerDigits(1);
		editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
		Dimension d = spinner.getPreferredSize();
		d.width = 60;
		spinner.setPreferredSize(d);
		spinner.addChangeListener(this);
		spinner.setMaximumSize(d);
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

	public ThresholdControlPanel getThreshold() {
		return threshold;
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
