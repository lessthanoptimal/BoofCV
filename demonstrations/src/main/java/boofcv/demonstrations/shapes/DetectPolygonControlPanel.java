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
import boofcv.struct.ConfigLength;
import boofcv.struct.ConnectRule;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * @author Peter Abeles
 */
public class DetectPolygonControlPanel extends DetectBlackShapePanel
		implements ActionListener, ChangeListener
{
	ShapeGuiListener owner;

	// selects which image to view
	JComboBox imageView;

	JCheckBox showCorners;
	JCheckBox showLines;
	JCheckBox showContour;
	JSpinner spinnerContourConnect;

	ConnectRule connectRule = ConnectRule.FOUR;
	boolean bShowCorners = true;
	boolean bShowLines = true;
	boolean bShowContour = false;

	boolean bRefineGray = true;
	ConfigRefinePolygonLineToImage refineGray = new ConfigRefinePolygonLineToImage();

	ThresholdControlPanel threshold;

	JSpinner spinnerMinContourSize;
	JSpinner spinnerMinSides;
	JSpinner spinnerMaxSides;
	JSpinner spinnerMinEdgeD; // threshold for detect
	JSpinner spinnerMinEdgeR; // threshold for refine
	JCheckBox setConvex;
	JCheckBox setBorder;

	JSpinner spinnerContourSplit;
	JSpinner spinnerContourMinSplit;
	JSpinner spinnerContourIterations;
	JSpinner spinnerSplitPenalty;


	JCheckBox setRefineContour;
	JCheckBox setRefineGray;
	JCheckBox setRemoveBias;
	JSpinner spinnerLineSamples;
	JSpinner spinnerCornerOffset;
	JSpinner spinnerSampleRadius;
	JSpinner spinnerRefineMaxIterations;
	JSpinner spinnerConvergeTol;
	JSpinner spinnerMaxCornerChange;

	ConfigPolygonDetector config = new ConfigPolygonDetector(3,6);

	int minSides = 3;
	int maxSides = 6;

	{
		config.detector.minimumContour = ConfigLength.fixed(20);
	}

	public DetectPolygonControlPanel(ShapeGuiListener owner) {
		this.owner = owner;

		imageView = new JComboBox();
		imageView.addItem("Input");
		imageView.addItem("Binary");
		imageView.addItem("Black");
		imageView.addActionListener(this);
		imageView.setMaximumSize(imageView.getPreferredSize());

		spinnerContourConnect = spinner(connectRule.ordinal(), ConnectRule.values());

		selectZoom = new JSpinner(new SpinnerNumberModel(1,MIN_ZOOM,MAX_ZOOM,1));
		selectZoom.addChangeListener(this);
		selectZoom.setMaximumSize(selectZoom.getPreferredSize());

		showCorners = new JCheckBox("Corners");
		showCorners.addActionListener(this);
		showCorners.setSelected(bShowCorners);
		showLines = new JCheckBox("Lines");
		showLines.setSelected(bShowLines);
		showLines.addActionListener(this);
		showContour = new JCheckBox("Contour");
		showContour.addActionListener(this);
		showContour.setSelected(bShowContour);

		threshold = new ThresholdControlPanel(owner);

		spinnerMinContourSize = new JSpinner(new SpinnerNumberModel(config.detector.minimumContour.length,
				5,10000,2));
		spinnerMinContourSize.setMaximumSize(spinnerMinContourSize.getPreferredSize());
		spinnerMinContourSize.addChangeListener(this);
		spinnerMinSides = new JSpinner(new SpinnerNumberModel(minSides, 3, 20, 1));
		spinnerMinSides.setMaximumSize(spinnerMinSides.getPreferredSize());
		spinnerMinSides.addChangeListener(this);
		spinnerMaxSides = new JSpinner(new SpinnerNumberModel(maxSides, 3, 20, 1));
		spinnerMaxSides.setMaximumSize(spinnerMaxSides.getPreferredSize());
		spinnerMaxSides.addChangeListener(this);

		spinnerMinEdgeD = new JSpinner(new SpinnerNumberModel(config.detector.minimumEdgeIntensity,
				0.0,255.0,1.0));
		spinnerMinEdgeD.setMaximumSize(spinnerMinEdgeD.getPreferredSize());
		spinnerMinEdgeD.addChangeListener(this);
		spinnerMinEdgeR = new JSpinner(new SpinnerNumberModel(config.minimumRefineEdgeIntensity,
				0.0,255.0,1.0));
		spinnerMinEdgeR.setMaximumSize(spinnerMinEdgeR.getPreferredSize());
		spinnerMinEdgeR.addChangeListener(this);

		spinnerContourSplit = new JSpinner(new SpinnerNumberModel(config.detector.contourToPoly.splitFraction,
				0.0,1.0,0.01));
		configureSpinnerFloat(spinnerContourSplit);
		spinnerContourMinSplit = new JSpinner(new SpinnerNumberModel(config.detector.contourToPoly.minimumSide.fraction,
				0.0, 1.0, 0.001));
		configureSpinnerFloat(spinnerContourMinSplit);
		spinnerContourIterations = new JSpinner(new SpinnerNumberModel(config.detector.contourToPoly.iterations,
				1, 200, 1));
		spinnerContourIterations.setMaximumSize(spinnerContourIterations.getPreferredSize());
		spinnerContourIterations.addChangeListener(this);
		spinnerSplitPenalty = new JSpinner(new SpinnerNumberModel(config.detector.contourToPoly.pruneSplitPenalty, 0.0, 100.0, 1.0));
		configureSpinnerFloat(spinnerSplitPenalty);

		setConvex = new JCheckBox("Convex");
		setConvex.addActionListener(this);
		setConvex.setSelected(config.detector.convex);
		setBorder = new JCheckBox("Image Border");
		setBorder.addActionListener(this);
		setBorder.setSelected(config.detector.canTouchBorder);

		setRefineContour = new JCheckBox("Refine Contour");
		setRefineContour.addActionListener(this);
		setRefineContour.setSelected(config.refineContour);
		setRefineGray = new JCheckBox("Refine Gray");
		setRefineGray.addActionListener(this);
		setRefineGray.setSelected(config.refineGray != null);
		setRemoveBias = new JCheckBox("Remove Bias");
		setRemoveBias.addActionListener(this);
		setRemoveBias.setSelected(config.adjustForThresholdBias);
		spinnerLineSamples = new JSpinner(new SpinnerNumberModel(refineGray.lineSamples, 5, 100, 1));
		spinnerLineSamples.setMaximumSize(spinnerLineSamples.getPreferredSize());
		spinnerLineSamples.addChangeListener(this);
		spinnerCornerOffset = new JSpinner(new SpinnerNumberModel(refineGray.cornerOffset, 0, 10, 1));
		spinnerCornerOffset.setMaximumSize(spinnerCornerOffset.getPreferredSize());
		spinnerCornerOffset.addChangeListener(this);
		spinnerSampleRadius = new JSpinner(new SpinnerNumberModel(refineGray.sampleRadius, 1, 10, 1));
		spinnerSampleRadius.setMaximumSize(spinnerCornerOffset.getPreferredSize());
		spinnerSampleRadius.addChangeListener(this);
		spinnerRefineMaxIterations = new JSpinner(new SpinnerNumberModel(refineGray.maxIterations, 1, 200, 1));
		spinnerRefineMaxIterations.setMaximumSize(spinnerRefineMaxIterations.getPreferredSize());
		spinnerRefineMaxIterations.addChangeListener(this);
		spinnerConvergeTol = new JSpinner(new SpinnerNumberModel(refineGray.convergeTolPixels, 0.0, 2.0, 0.005));
		configureSpinnerFloat(spinnerConvergeTol);
		spinnerMaxCornerChange = new JSpinner(new SpinnerNumberModel(refineGray.maxCornerChangePixel, 0.0, 50.0, 1.0));
		configureSpinnerFloat(spinnerMaxCornerChange);

		addLabeled(processingTimeLabel,"Time (ms)", this);
		addLabeled(imageSizeLabel,"Size", this);
		addLabeled(imageView, "View: ", this);
		addLabeled(selectZoom,"Zoom",this);
		addAlignLeft(showCorners, this);
		addAlignLeft(showLines, this);
		addAlignLeft(showContour, this);
		add(threshold);
		addLabeled(spinnerContourConnect,"Contour Connect: ",this);
		addLabeled(spinnerMinContourSize, "Min Contour Size: ", this);
		addLabeled(spinnerMinSides, "Minimum Sides: ", this);
		addLabeled(spinnerMaxSides, "Maximum Sides: ", this);
		addLabeled(spinnerMinEdgeD, "Edge Intensity D: ", this);
		addLabeled(spinnerMinEdgeR, "Edge Intensity R: ", this);
		addAlignLeft(setConvex, this);
		addAlignLeft(setBorder, this);
		addCenterLabel("Contour", this);
		addLabeled(spinnerContourSplit, "Split Fraction: ", this);
		addLabeled(spinnerContourMinSplit, "Min Split: ", this);
		addLabeled(spinnerContourIterations, "Max Iterations: ", this);
		addLabeled(spinnerSplitPenalty, "Split Penalty: ", this);
		addCenterLabel("Refinement", this);
		addAlignLeft(setRemoveBias, this);
		addAlignLeft(setRefineContour, this);
		addAlignLeft(setRefineGray, this);
		addLabeled(spinnerLineSamples, "Line Samples: ", this);
		addLabeled(spinnerCornerOffset, "Corner Offset: ", this);
		addLabeled(spinnerSampleRadius, "Sample Radius: ", this);
		addLabeled(spinnerRefineMaxIterations, "Iterations: ", this);
		addLabeled(spinnerConvergeTol, "Converge Tol Pixels: ", this);
		addLabeled(spinnerMaxCornerChange, "Max Corner Change: ", this);
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
		if( e.getSource() == imageView ) {
			selectedView = imageView.getSelectedIndex();
			owner.viewUpdated();
		} else if( e.getSource() == showCorners ) {
			bShowCorners = showCorners.isSelected();
			owner.viewUpdated();
		} else if( e.getSource() == showLines ) {
			bShowLines = showLines.isSelected();
			owner.viewUpdated();
		} else if( e.getSource() == showContour ) {
			bShowContour = showContour.isSelected();
			owner.viewUpdated();
		} else if( e.getSource() == setConvex ) {
			config.detector.convex = setConvex.isSelected();
			owner.configUpdate();
		} else if( e.getSource() == setBorder ) {
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
		} else if( e.getSource() == spinnerMinEdgeR) {
			config.minimumRefineEdgeIntensity = ((Number) spinnerMinEdgeR.getValue()).doubleValue();
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
		} else if( e.getSource() == selectZoom ) {
			zoom = ((Number) selectZoom.getValue()).doubleValue();
			owner.viewUpdated();
			return;
		} else if( e.getSource() == spinnerMinContourSize ) {
			config.detector.minimumContour.length = ((Number) spinnerMinContourSize.getValue()).intValue();
		} else if( e.getSource() == spinnerContourSplit ) {
			config.detector.contourToPoly.splitFraction = ((Number) spinnerContourSplit.getValue()).doubleValue();
		} else if( e.getSource() == spinnerContourMinSplit ) {
			config.detector.contourToPoly.minimumSide.fraction = ((Number) spinnerContourMinSplit.getValue()).doubleValue();
		} else if( e.getSource() == spinnerContourIterations ) {
			config.detector.contourToPoly.iterations = ((Number) spinnerContourIterations.getValue()).intValue();
		} else if( e.getSource() == spinnerSplitPenalty ) {
			config.detector.contourToPoly.pruneSplitPenalty = ((Number) spinnerSplitPenalty.getValue()).doubleValue();
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
			connectRule = (ConnectRule)spinnerContourConnect.getValue();
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
		return config;
	}
}
