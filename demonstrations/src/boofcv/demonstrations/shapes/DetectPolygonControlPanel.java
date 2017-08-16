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
import boofcv.factory.shape.ConfigRefinePolygonCornersToImage;
import boofcv.factory.shape.ConfigRefinePolygonLineToImage;

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
public class DetectPolygonControlPanel extends DetectBlackShapePanel
	implements ActionListener, ChangeListener
{
	DetectBlackPolygonApp owner;

	// selects which image to view
	JComboBox imageView;

	JCheckBox showCorners;
	JCheckBox showLines;
	JCheckBox showContour;

	JComboBox refineChoice;

	boolean bShowCorners = true;
	boolean bShowLines = true;
	boolean bShowContour = false;

	ThresholdControlPanel threshold;

	JSpinner spinnerMinContourSize;
	JSpinner spinnerMinSides;
	JSpinner spinnerMaxSides;
	JSpinner spinnerMinEdge;
	JCheckBox setConvex;
	JCheckBox setBorder;

	JSpinner spinnerContourSplit;
	JSpinner spinnerContourMinSplit;
	JSpinner spinnerContourIterations;
	JSpinner spinnerSplitPenalty;

	JSpinner spinnerLineSamples;
	JSpinner spinnerCornerOffset;
	JSpinner spinnerSampleRadius;
	JSpinner spinnerRefineMaxIterations;
	JSpinner spinnerConvergeTol;
	JSpinner spinnerMaxCornerChange;

	ConfigPolygonDetector config = new ConfigPolygonDetector(3,6);
	ConfigRefinePolygonLineToImage configLine = new ConfigRefinePolygonLineToImage();
	ConfigRefinePolygonCornersToImage configCorner = new ConfigRefinePolygonCornersToImage();
	PolygonRefineType refineType = PolygonRefineType.LINE;

	int minSides = 3;
	int maxSides = 6;

	public DetectPolygonControlPanel(DetectBlackPolygonApp owner) {
		this.owner = owner;

		imageView = new JComboBox();
		imageView.addItem("Input");
		imageView.addItem("Binary");
		imageView.addItem("Black");
		imageView.addActionListener(this);
		imageView.setMaximumSize(imageView.getPreferredSize());

		selectZoom = new JSpinner(new SpinnerNumberModel(1,minZoom,maxZoom,1));
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

		refineChoice = new JComboBox();
		for( PolygonRefineType n : PolygonRefineType.values() ) {
			refineChoice.addItem(n.name());
		}
		refineChoice.setSelectedIndex(refineType.ordinal());
		refineChoice.addActionListener(this);
		refineChoice.setMaximumSize(refineChoice.getPreferredSize());

		spinnerMinContourSize = new JSpinner(new SpinnerNumberModel(config.minContourImageWidthFraction,0.0,0.2,0.01));
		configureSpinnerFloat(spinnerMinContourSize);
		spinnerMinSides = new JSpinner(new SpinnerNumberModel(minSides, 3, 20, 1));
		spinnerMinSides.setMaximumSize(spinnerMinSides.getPreferredSize());
		spinnerMinSides.addChangeListener(this);
		spinnerMaxSides = new JSpinner(new SpinnerNumberModel(maxSides, 3, 20, 1));
		spinnerMaxSides.setMaximumSize(spinnerMaxSides.getPreferredSize());
		spinnerMaxSides.addChangeListener(this);

		spinnerMinEdge = new JSpinner(new SpinnerNumberModel(config.minimumEdgeIntensity,0.0,255.0,1.0));
		spinnerMinEdge.setMaximumSize(spinnerMinEdge.getPreferredSize());
		spinnerMinEdge.addChangeListener(this);
		spinnerContourSplit = new JSpinner(new SpinnerNumberModel(config.contour2Poly_splitFraction,0.0,1.0,0.01));
		configureSpinnerFloat(spinnerContourSplit);
		spinnerContourMinSplit = new JSpinner(new SpinnerNumberModel(config.contour2Poly_minimumSideFraction, 0.0, 1.0, 0.001));
		configureSpinnerFloat(spinnerContourMinSplit);
		spinnerContourIterations = new JSpinner(new SpinnerNumberModel(config.contour2Poly_iterations, 1, 200, 1));
		spinnerContourIterations.setMaximumSize(spinnerContourIterations.getPreferredSize());
		spinnerContourIterations.addChangeListener(this);
		spinnerSplitPenalty = new JSpinner(new SpinnerNumberModel(config.splitPenalty, 0.0, 100.0, 1.0));
		configureSpinnerFloat(spinnerSplitPenalty);

		setConvex = new JCheckBox("Convex");
		setConvex.addActionListener(this);
		setConvex.setSelected(config.convex);
		setBorder = new JCheckBox("Image Border");
		setBorder.addActionListener(this);
		setBorder.setSelected(config.canTouchBorder);

		spinnerLineSamples = new JSpinner(new SpinnerNumberModel(configLine.lineSamples, 5, 100, 1));
		spinnerLineSamples.setMaximumSize(spinnerLineSamples.getPreferredSize());
		spinnerLineSamples.addChangeListener(this);
		spinnerCornerOffset = new JSpinner(new SpinnerNumberModel(configLine.cornerOffset, 0, 10, 1));
		spinnerCornerOffset.setMaximumSize(spinnerCornerOffset.getPreferredSize());
		spinnerCornerOffset.addChangeListener(this);
		spinnerSampleRadius = new JSpinner(new SpinnerNumberModel(configLine.sampleRadius, 0, 10, 1));
		spinnerSampleRadius.setMaximumSize(spinnerCornerOffset.getPreferredSize());
		spinnerSampleRadius.addChangeListener(this);
		spinnerRefineMaxIterations = new JSpinner(new SpinnerNumberModel(configLine.maxIterations, 0, 200, 1));
		spinnerRefineMaxIterations.setMaximumSize(spinnerRefineMaxIterations.getPreferredSize());
		spinnerRefineMaxIterations.addChangeListener(this);
		spinnerConvergeTol = new JSpinner(new SpinnerNumberModel(configLine.convergeTolPixels, 0.0, 2.0, 0.005));
		configureSpinnerFloat(spinnerConvergeTol);
		spinnerMaxCornerChange = new JSpinner(new SpinnerNumberModel(configLine.maxCornerChangePixel, 0.0, 50.0, 1.0));
		configureSpinnerFloat(spinnerMaxCornerChange);

		addLabeled(imageView, "View: ", this);
		addLabeled(selectZoom,"Zoom",this);
		addAlignLeft(showCorners, this);
		addAlignLeft(showLines, this);
		addAlignLeft(showContour, this);
		add(threshold);
		addLabeled(spinnerMinContourSize, "Min Contour Size: ", this);
		addLabeled(spinnerMinSides, "Minimum Sides: ", this);
		addLabeled(spinnerMaxSides, "Maximum Sides: ", this);
		addLabeled(spinnerMinEdge, "Edge Intensity: ", this);
		addAlignLeft(setConvex, this);
		addAlignLeft(setBorder, this);
		addCenterLabel("Contour", this);
		addLabeled(spinnerContourSplit, "Split Fraction: ", this);
		addLabeled(spinnerContourMinSplit, "Min Split: ", this);
		addLabeled(spinnerContourIterations, "Max Iterations: ", this);
		addLabeled(spinnerSplitPenalty, "Split Penalty: ", this);
		addCenterLabel("Refinement", this);
		addLabeled(refineChoice, "Refine: ", this);
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
		} else if( e.getSource() == refineChoice ) {
			refineType = PolygonRefineType.values()[refineChoice.getSelectedIndex()];

			updateRefineSettings();
			owner.configUpdate();
		} else if( e.getSource() == setConvex ) {
			config.convex = setConvex.isSelected();
			owner.configUpdate();
		} else if( e.getSource() == setBorder ) {
			config.canTouchBorder = setBorder.isSelected();
			owner.configUpdate();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		PolygonRefineType refine = PolygonRefineType.values()[refineChoice.getSelectedIndex()];

		if( e.getSource() == spinnerMinEdge) {
			config.minimumEdgeIntensity = ((Number) spinnerMinEdge.getValue()).doubleValue();
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
				config.minContourImageWidthFraction = ((Number) spinnerMinContourSize.getValue()).doubleValue();
		} else if( e.getSource() == spinnerContourSplit ) {
			config.contour2Poly_splitFraction = ((Number) spinnerContourSplit.getValue()).doubleValue();
		} else if( e.getSource() == spinnerContourMinSplit ) {
			config.contour2Poly_minimumSideFraction = ((Number) spinnerContourMinSplit.getValue()).doubleValue();
		} else if( e.getSource() == spinnerContourIterations ) {
			config.contour2Poly_iterations = ((Number) spinnerContourIterations.getValue()).intValue();
		} else if( e.getSource() == spinnerSplitPenalty ) {
			config.splitPenalty = ((Number) spinnerSplitPenalty.getValue()).doubleValue();
		} else if (e.getSource() == spinnerLineSamples) {
			if (refine == PolygonRefineType.LINE) {
				configLine.lineSamples = ((Number) spinnerLineSamples.getValue()).intValue();
			} else {
				configCorner.lineSamples = ((Number) spinnerLineSamples.getValue()).intValue();
			}
		} else if (e.getSource() == spinnerCornerOffset) {
			if (refine == PolygonRefineType.LINE) {
				configLine.cornerOffset = ((Number) spinnerCornerOffset.getValue()).intValue();
			} else {
				configCorner.cornerOffset = ((Number) spinnerCornerOffset.getValue()).intValue();
			}
		} else if (e.getSource() == spinnerSampleRadius) {
			if (refine == PolygonRefineType.LINE) {
				configLine.sampleRadius = ((Number) spinnerSampleRadius.getValue()).intValue();
			} else {
				configCorner.sampleRadius = ((Number) spinnerSampleRadius.getValue()).intValue();
			}
		} else if (e.getSource() == spinnerRefineMaxIterations) {
			if (refine == PolygonRefineType.LINE) {
				configLine.maxIterations = ((Number) spinnerRefineMaxIterations.getValue()).intValue();
			} else {
				configCorner.maxIterations = ((Number) spinnerRefineMaxIterations.getValue()).intValue();
			}
		} else if (e.getSource() == spinnerConvergeTol) {
			if (refine == PolygonRefineType.LINE) {
				configLine.convergeTolPixels = ((Number) spinnerConvergeTol.getValue()).doubleValue();
			} else {
				configCorner.convergeTolPixels = ((Number) spinnerConvergeTol.getValue()).doubleValue();
			}
		} else if (e.getSource() == spinnerMaxCornerChange) {
			if (refine == PolygonRefineType.LINE) {
				configLine.maxCornerChangePixel = ((Number) spinnerMaxCornerChange.getValue()).doubleValue();
			} else {
				configCorner.maxCornerChangePixel = ((Number) spinnerMaxCornerChange.getValue()).doubleValue();
			}
		}
		owner.configUpdate();
	}

	private void updateSidesInConfig() {
		config.minimumSides = minSides;
		config.maximumSides = maxSides;
	}

	private void updateRefineSettings() {

		spinnerLineSamples.removeChangeListener(this);
		spinnerCornerOffset.removeChangeListener(this);
		spinnerSampleRadius.removeChangeListener(this);
		spinnerRefineMaxIterations.removeChangeListener(this);
		spinnerConvergeTol.removeChangeListener(this);
		spinnerMaxCornerChange.removeChangeListener(this);

		if( refineType == PolygonRefineType.LINE ) {
			spinnerLineSamples.setValue(configLine.lineSamples);
			spinnerCornerOffset.setValue(configLine.cornerOffset);
			spinnerSampleRadius.setValue(configLine.sampleRadius);
			spinnerRefineMaxIterations.setValue(configLine.maxIterations);
			spinnerConvergeTol.setValue(configLine.convergeTolPixels);
			spinnerMaxCornerChange.setValue(configLine.maxCornerChangePixel);
		} else if( refineType == PolygonRefineType.CORNER ){
			spinnerLineSamples.setValue(configCorner.lineSamples);
			spinnerCornerOffset.setValue(configCorner.cornerOffset);
			spinnerSampleRadius.setValue(configCorner.sampleRadius);
			spinnerRefineMaxIterations.setValue(configCorner.maxIterations);
			spinnerConvergeTol.setValue(configCorner.convergeTolPixels);
			spinnerMaxCornerChange.setValue(configCorner.maxCornerChangePixel);
		}

		spinnerLineSamples.addChangeListener(this);
		spinnerCornerOffset.addChangeListener(this);
		spinnerSampleRadius.addChangeListener(this);
		spinnerRefineMaxIterations.addChangeListener(this);
		spinnerConvergeTol.addChangeListener(this);
		spinnerMaxCornerChange.addChangeListener(this);
	}

	public ThresholdControlPanel getThreshold() {
		return threshold;
	}

	public ConfigPolygonDetector getConfigPolygon() {
		return config;
	}

	public ConfigRefinePolygonLineToImage getConfigLine() {
		return configLine;
	}

	public ConfigRefinePolygonCornersToImage getConfigCorner() {
		return configCorner;
	}

	public PolygonRefineType getRefineType() {
		return refineType;
	}
}
