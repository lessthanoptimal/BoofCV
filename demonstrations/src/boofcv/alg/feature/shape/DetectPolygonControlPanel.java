/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.shape;

import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.gui.StandardAlgConfigPanel;

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
public class DetectPolygonControlPanel extends StandardAlgConfigPanel
	implements ActionListener, ChangeListener
{
	DetectBlackPolygonApp owner;

	// selects which image to view
	JComboBox imageView;

	JCheckBox showCorners;
	JCheckBox showLines;
	JCheckBox showContour;

	JComboBox refineChoice;

	int selectedView = 0;
	boolean bShowCorners = true;
	boolean bShowLines = true;
	boolean bShowContour = false;

	ThresholdControlPanel threshold;

	JSpinner spinnerMinSides;
	JSpinner spinnerMaxSides;
	JSpinner spinnerMinEdge;
	JCheckBox setConvex;

	JSpinner spinnerContourSplit;
	JSpinner spinnerContourMinSplit;
	JSpinner spinnerContourIterations;

	JSpinner spinnerLineSamples;
	JSpinner spinnerCornerOffset;
	JSpinner spinnerSampleRadius;
	JSpinner spinnerRefineMaxIterations;
	JSpinner spinnerConvergeTol;
	JSpinner spinnerMaxCornerChange;

	ConfigPolygonDetector config = new ConfigPolygonDetector(3,4,5,6);

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
		refineChoice.addItem("Lines");
		refineChoice.addItem("Corners");
		refineChoice.addItem("None");
		refineChoice.addActionListener(this);
		refineChoice.setMaximumSize(refineChoice.getPreferredSize());

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
		spinnerContourMinSplit = new JSpinner(new SpinnerNumberModel(config.contour2Poly_minimumSplitFraction, 0.0, 1.0, 0.001));
		configureSpinnerFloat(spinnerContourMinSplit);
		spinnerContourSplit.addChangeListener(this);
		spinnerContourIterations = new JSpinner(new SpinnerNumberModel(config.contour2Poly_iterations, 1, 200, 1));
		spinnerContourIterations.setMaximumSize(spinnerContourIterations.getPreferredSize());
		spinnerContourIterations.addChangeListener(this);

		setConvex = new JCheckBox("Convex");
		setConvex.addActionListener(this);
		setConvex.setSelected(config.convex);

		spinnerLineSamples = new JSpinner(new SpinnerNumberModel(config.configRefineLines.lineSamples, 5, 100, 1));
		spinnerLineSamples.setMaximumSize(spinnerLineSamples.getPreferredSize());
		spinnerLineSamples.addChangeListener(this);
		spinnerCornerOffset = new JSpinner(new SpinnerNumberModel(config.configRefineLines.cornerOffset, 0, 10, 1));
		spinnerCornerOffset.setMaximumSize(spinnerCornerOffset.getPreferredSize());
		spinnerCornerOffset.addChangeListener(this);
		spinnerSampleRadius = new JSpinner(new SpinnerNumberModel(config.configRefineLines.sampleRadius, 0, 10, 1));
		spinnerSampleRadius.setMaximumSize(spinnerCornerOffset.getPreferredSize());
		spinnerSampleRadius.addChangeListener(this);
		spinnerRefineMaxIterations = new JSpinner(new SpinnerNumberModel(config.configRefineLines.maxIterations, 0, 200, 1));
		spinnerRefineMaxIterations.setMaximumSize(spinnerRefineMaxIterations.getPreferredSize());
		spinnerRefineMaxIterations.addChangeListener(this);
		spinnerConvergeTol = new JSpinner(new SpinnerNumberModel(config.configRefineLines.convergeTolPixels, 0.0, 2.0, 0.005));
		configureSpinnerFloat(spinnerConvergeTol);
		spinnerMaxCornerChange = new JSpinner(new SpinnerNumberModel(config.configRefineLines.maxCornerChangePixel, 0.0, 50.0, 1.0));
		configureSpinnerFloat(spinnerMaxCornerChange);

		addLabeled(imageView, "View: ", this);
		addAlignLeft(showCorners, this);
		addAlignLeft(showLines, this);
		addAlignLeft(showContour, this);
		add(threshold);
		addLabeled(spinnerMinSides, "Minimum Sides: ", this);
		addLabeled(spinnerMaxSides, "Maximum Sides: ", this);
		addLabeled(spinnerMinEdge, "Edge Intensity: ", this);
		addAlignLeft(setConvex, this);
		addCenterLabel("Contour", this);
		addLabeled(spinnerContourSplit, "Split Fraction: ", this);
		addLabeled(spinnerContourMinSplit, "Min Split: ", this);
		addLabeled(spinnerContourIterations, "Max Iterations: ", this);
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
			int selected = refineChoice.getSelectedIndex();

			if( selected == 0 ) {
				config.refineWithLines = true;
				config.refineWithCorners = false;
			} else if( selected == 1 ){
				config.refineWithLines = false;
				config.refineWithCorners = true;
			} else {
				config.refineWithLines = false;
				config.refineWithCorners = false;
			}
			updateRefineSettings();
			owner.configUpdate();
		} else if( e.getSource() == setConvex ) {
			config.convex = setConvex.isSelected();
			owner.configUpdate();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		int refine = refineChoice.getSelectedIndex();

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
			if( maxSides < minSides) {
				minSides = maxSides;
				spinnerMinSides.setValue(minSides);
			}
			updateSidesInConfig();
		} else if( e.getSource() == spinnerContourSplit ) {
			config.contour2Poly_splitFraction = ((Number) spinnerContourSplit.getValue()).doubleValue();
		} else if( e.getSource() == spinnerContourMinSplit ) {
			config.contour2Poly_minimumSplitFraction = ((Number) spinnerContourMinSplit.getValue()).doubleValue();
		} else if( e.getSource() == spinnerContourIterations ) {
			config.contour2Poly_iterations = ((Number) spinnerContourIterations.getValue()).intValue();
		} else if( e.getSource() == spinnerLineSamples ) {
			if( refine == 0 ) {
				config.configRefineLines.lineSamples = ((Number) spinnerLineSamples.getValue()).intValue();
			} else {
				config.configRefineCorners.lineSamples = ((Number) spinnerLineSamples.getValue()).intValue();
			}
		} else if( e.getSource() == spinnerCornerOffset ) {
			if( refine == 0 ) {
				config.configRefineLines.cornerOffset = ((Number) spinnerCornerOffset.getValue()).intValue();
			} else {
				config.configRefineCorners.cornerOffset = ((Number) spinnerCornerOffset.getValue()).intValue();
			}
		} else if( e.getSource() == spinnerSampleRadius ) {
			if( refine == 0 ) {
				config.configRefineLines.sampleRadius = ((Number) spinnerSampleRadius.getValue()).intValue();
			} else {
				config.configRefineCorners.sampleRadius = ((Number) spinnerSampleRadius.getValue()).intValue();
			}
		} else if( e.getSource() == spinnerRefineMaxIterations) {
			if( refine == 0 ) {
				config.configRefineLines.maxIterations = ((Number) spinnerRefineMaxIterations.getValue()).intValue();
			} else {
				config.configRefineCorners.maxIterations = ((Number) spinnerRefineMaxIterations.getValue()).intValue();
			}
		} else if( e.getSource() == spinnerConvergeTol ) {
			if( refine == 0 ) {
				config.configRefineLines.convergeTolPixels = ((Number) spinnerConvergeTol.getValue()).doubleValue();
			} else {
				config.configRefineCorners.convergeTolPixels = ((Number) spinnerConvergeTol.getValue()).doubleValue();
			}
		} else if( e.getSource() == spinnerMaxCornerChange ) {
			if( refine == 0 ) {
				config.configRefineLines.maxCornerChangePixel = ((Number) spinnerMaxCornerChange.getValue()).doubleValue();
			} else {
				config.configRefineCorners.maxCornerChangePixel = ((Number) spinnerMaxCornerChange.getValue()).doubleValue();
			}
		}
		owner.configUpdate();
	}

	private void updateSidesInConfig() {
		int allowed[] = new int[maxSides-minSides+1];
		for (int i = minSides; i <= maxSides; i++) {
			allowed[i-minSides] = i;
		}
		config.numberOfSides = allowed;
	}

	private void updateRefineSettings() {
		int selected = refineChoice.getSelectedIndex();

		if( selected == 0 ) {
			spinnerLineSamples.setValue(config.configRefineLines.lineSamples);
			spinnerCornerOffset.setValue(config.configRefineLines.cornerOffset);
			spinnerSampleRadius.setValue(config.configRefineLines.sampleRadius);
			spinnerRefineMaxIterations.setValue(config.configRefineLines.maxIterations);
			spinnerConvergeTol.setValue(config.configRefineLines.convergeTolPixels);
			spinnerMaxCornerChange.setValue(config.configRefineLines.maxCornerChangePixel);
		} else if( selected == 1 ){
			spinnerLineSamples.setValue(config.configRefineCorners.lineSamples);
			spinnerCornerOffset.setValue(config.configRefineCorners.cornerOffset);
			spinnerSampleRadius.setValue(config.configRefineCorners.sampleRadius);
			spinnerRefineMaxIterations.setValue(config.configRefineCorners.maxIterations);
			spinnerConvergeTol.setValue(config.configRefineCorners.convergeTolPixels);
			spinnerMaxCornerChange.setValue(config.configRefineCorners.maxCornerChangePixel);
		}
	}

	public ThresholdControlPanel getThreshold() {
		return threshold;
	}

	public ConfigPolygonDetector getConfig() {
		return config;
	}
}
