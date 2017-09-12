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

package boofcv.demonstrations.fiducial;

import boofcv.demonstrations.shapes.DetectBlackShapePanel;
import boofcv.demonstrations.shapes.ThresholdControlPanel;
import boofcv.factory.fiducial.ConfigQrCode;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.factory.shape.ConfigRefinePolygonLineToImage;

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
public class DetectQrCodeControlPanel extends DetectBlackShapePanel
		implements ActionListener, ChangeListener
{
	DetectQrCodeApp owner;

	// selects which image to view
	JComboBox imageView;

	JButton bRunAgain = new JButton("Run Again");

	JCheckBox showSquares;
	JCheckBox showPositionPattern;
	JCheckBox showContour;

	boolean bShowSquares = true;
	boolean bShowPositionPattern = true; // show position patterns
	boolean bShowContour = false;

	boolean bRefineGray = true;
	ConfigRefinePolygonLineToImage refineGray = new ConfigRefinePolygonLineToImage();

	ThresholdControlPanel threshold;

	JSpinner spinnerMinContourSize;
	JSpinner spinnerMinEdgeD; // threshold for detect
	JSpinner spinnerMinEdgeR; // threshold for refine
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

	ConfigQrCode config = new ConfigQrCode();

	public DetectQrCodeControlPanel(DetectQrCodeApp owner) {
		this.owner = owner;

		bRunAgain.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				DetectQrCodeControlPanel.this.owner.reprocessImageOnly();
			}
		});

		imageView = new JComboBox();
		imageView.addItem("Input");
		imageView.addItem("Binary");
		imageView.addItem("Black");
		imageView.addActionListener(this);
		imageView.setMaximumSize(imageView.getPreferredSize());

		selectZoom = new JSpinner(new SpinnerNumberModel(1,MIN_ZOOM,MAX_ZOOM,1));
		selectZoom.addChangeListener(this);
		selectZoom.setMaximumSize(selectZoom.getPreferredSize());

		showSquares = new JCheckBox("Squares");
		showSquares.addActionListener(this);
		showSquares.setSelected(bShowSquares);
		showPositionPattern = new JCheckBox("Position Pattern");
		showPositionPattern.setSelected(bShowPositionPattern);
		showPositionPattern.addActionListener(this);
		showContour = new JCheckBox("Contour");
		showContour.addActionListener(this);
		showContour.setSelected(bShowContour);

		threshold = new ThresholdControlPanel(owner, ThresholdType.LOCAL_BLOCK_OTSU);
		threshold.setRadius(40);

		spinnerMinContourSize = new JSpinner(new SpinnerNumberModel(config.polygon.detector.minimumContour.pixels,
				5,10000,2));
		spinnerMinContourSize.setMaximumSize(spinnerMinContourSize.getPreferredSize());
		spinnerMinContourSize.addChangeListener(this);

		spinnerMinEdgeD = new JSpinner(new SpinnerNumberModel(config.polygon.detector.minimumEdgeIntensity,
				0.0,255.0,1.0));
		spinnerMinEdgeD.setMaximumSize(spinnerMinEdgeD.getPreferredSize());
		spinnerMinEdgeD.addChangeListener(this);
		spinnerMinEdgeR = new JSpinner(new SpinnerNumberModel(config.polygon.minimumRefineEdgeIntensity,
				0.0,255.0,1.0));
		spinnerMinEdgeR.setMaximumSize(spinnerMinEdgeR.getPreferredSize());
		spinnerMinEdgeR.addChangeListener(this);

		spinnerContourSplit = new JSpinner(new SpinnerNumberModel(config.polygon.detector.contourToPoly.splitFraction,
				0.0,1.0,0.01));
		configureSpinnerFloat(spinnerContourSplit);
		spinnerContourMinSplit = new JSpinner(new SpinnerNumberModel(config.polygon.detector.contourToPoly.minimumSideFraction,
				0.0, 1.0, 0.001));
		configureSpinnerFloat(spinnerContourMinSplit);
		spinnerContourIterations = new JSpinner(new SpinnerNumberModel(config.polygon.detector.contourToPoly.iterations,
				1, 200, 1));
		spinnerContourIterations.setMaximumSize(spinnerContourIterations.getPreferredSize());
		spinnerContourIterations.addChangeListener(this);
		spinnerSplitPenalty = new JSpinner(new SpinnerNumberModel(config.polygon.detector.splitPenalty, 0.0, 100.0, 1.0));
		configureSpinnerFloat(spinnerSplitPenalty);

		setBorder = new JCheckBox("Image Border");
		setBorder.addActionListener(this);
		setBorder.setSelected(config.polygon.detector.canTouchBorder);

		setRefineContour = new JCheckBox("Refine Contour");
		setRefineContour.addActionListener(this);
		setRefineContour.setSelected(config.polygon.refineContour);
		setRefineGray = new JCheckBox("Refine Gray");
		setRefineGray.addActionListener(this);
		setRefineGray.setSelected(config.polygon.refineGray != null);
		setRemoveBias = new JCheckBox("Remove Bias");
		setRemoveBias.addActionListener(this);
		setRemoveBias.setSelected(config.polygon.adjustForThresholdBias);
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
		add(bRunAgain);
		addLabeled(imageView, "View: ", this);
		addLabeled(selectZoom,"Zoom",this);
		addAlignLeft(showSquares, this);
		addAlignLeft(showPositionPattern, this);
		addAlignLeft(showContour, this);
		add(threshold);
		addLabeled(spinnerMinContourSize, "Min Contour Size: ", this);
		addLabeled(spinnerMinEdgeD, "Edge Intensity D: ", this);
		addLabeled(spinnerMinEdgeR, "Edge Intensity R: ", this);
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
		} else if( e.getSource() == showSquares ) {
			bShowSquares = showSquares.isSelected();
			owner.viewUpdated();
		} else if( e.getSource() == showPositionPattern ) {
			bShowPositionPattern = showPositionPattern.isSelected();
			owner.viewUpdated();
		} else if( e.getSource() == showContour ) {
			bShowContour = showContour.isSelected();
			owner.viewUpdated();
		} else if( e.getSource() == setBorder ) {
			config.polygon.detector.canTouchBorder = setBorder.isSelected();
			owner.configUpdate();
		} else if( e.getSource() == setRefineContour ) {
			config.polygon.refineContour = setRefineContour.isSelected();
			owner.configUpdate();
		} else if( e.getSource() == setRefineGray ) {
			bRefineGray = setRefineGray.isSelected();
			owner.configUpdate();
		} else if( e.getSource() == setRemoveBias ) {
			config.polygon.adjustForThresholdBias = setRemoveBias.isSelected();
			owner.configUpdate();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == spinnerMinEdgeD) {
			config.polygon.detector.minimumEdgeIntensity = ((Number) spinnerMinEdgeD.getValue()).doubleValue();
		} else if( e.getSource() == spinnerMinEdgeR) {
			config.polygon.minimumRefineEdgeIntensity = ((Number) spinnerMinEdgeR.getValue()).doubleValue();
		} else if( e.getSource() == selectZoom ) {
			zoom = ((Number) selectZoom.getValue()).doubleValue();
			owner.viewUpdated();
			return;
		} else if( e.getSource() == spinnerMinContourSize ) {
			config.polygon.detector.minimumContour.pixels = ((Number) spinnerMinContourSize.getValue()).intValue();
		} else if( e.getSource() == spinnerContourSplit ) {
			config.polygon.detector.contourToPoly.splitFraction = ((Number) spinnerContourSplit.getValue()).doubleValue();
		} else if( e.getSource() == spinnerContourMinSplit ) {
			config.polygon.detector.contourToPoly.minimumSideFraction = ((Number) spinnerContourMinSplit.getValue()).doubleValue();
		} else if( e.getSource() == spinnerContourIterations ) {
			config.polygon.detector.contourToPoly.iterations = ((Number) spinnerContourIterations.getValue()).intValue();
		} else if( e.getSource() == spinnerSplitPenalty ) {
			config.polygon.detector.splitPenalty = ((Number) spinnerSplitPenalty.getValue()).doubleValue();
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
		}
		owner.configUpdate();
	}

	public ThresholdControlPanel getThreshold() {
		return threshold;
	}

	public ConfigQrCode getConfigPolygon() {
		return config;
	}
}
