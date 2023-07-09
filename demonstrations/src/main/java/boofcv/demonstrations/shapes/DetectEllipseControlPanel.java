/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

import boofcv.factory.shape.ConfigEllipseDetector;
import boofcv.gui.controls.BaseImageControlPanel;
import boofcv.gui.controls.JConfigLength;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionListener;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Configures ellipse detectors
 *
 * @author Peter Abeles
 */
public class DetectEllipseControlPanel extends BaseImageControlPanel implements ActionListener, ChangeListener {
	ConfigEllipseDetector config = new ConfigEllipseDetector();
	DetectBlackEllipseApp owner;

	// selects which image to view
	JComboBox imageView = combo(0, "Input", "Binary", "Black");

	boolean bShowShapes = true;
	boolean bShowContour = false;

	JCheckBox showEllipses = checkbox("Ellipses", bShowShapes);
	JCheckBox showContour = checkbox("Contour", bShowContour);

	ThresholdControlPanel threshold;

	JConfigLength spinnerMinContourSize = configLength(config.minimumContour, -1, 50000);
	JConfigLength spinnerMaxContourSize = configLength(config.maximumContour, -1, 50000);
	JSpinner spinnerMaxError = spinner(config.maxDistanceFromEllipse, 0, 1000.0, 1.0);
	JConfigLength spinnerMinMinorAxisSize = configLength(config.minimumMinorAxis, 0, 1000.0);

	JSpinner spinnerConvergeTol = spinner(config.convergenceTol, 0.0, 1.0, 0.001);
	JSpinner spinnerMaxIterations = spinner(config.maxIterations, 0, 200, 2);
	JSpinner spinnerNumSample = spinner(config.numSampleContour, 1, 200, 2);
	JSpinner spinnerRadiusSample = spinner(config.refineRadialSamples, 1, 5, 1);

	JSpinner spinnerMinEdge = spinner(config.minimumEdgeIntensity, 0.0, 255.0, 1.0);

	public DetectEllipseControlPanel( DetectBlackEllipseApp owner ) {
		this.owner = owner;

		threshold = new ThresholdControlPanel(owner);

		selectZoom = new JSpinner(new SpinnerNumberModel(1, MIN_ZOOM, MAX_ZOOM, 1));
		selectZoom.addChangeListener(this);
		selectZoom.setMaximumSize(selectZoom.getPreferredSize());

		configureSpinnerFloat(spinnerConvergeTol, 1, 3);

		addLabeled(processingTimeLabel, "Time (ms)");
		addLabeled(imageSizeLabel, "Size");
		addLabeled(imageView, "View: ");
		addLabeled(selectZoom, "Zoom");
		addAlignLeft(showEllipses);
		addAlignLeft(showContour);
		add(threshold);
		addCenterLabel("Contour");
		addLabeled(spinnerMinContourSize, "Min Contour Size: ");
		addLabeled(spinnerMaxContourSize, "Max Contour Size: ");
		addLabeled(spinnerMaxError, "Max Error: ");
		addLabeled(spinnerMinMinorAxisSize, "Min Minor Axis: ");
		addLabeled(spinnerMinEdge, "Edge Intensity: ");
		addCenterLabel("Refinement");
		addLabeled(spinnerConvergeTol, "Converge Tol: ");
		addLabeled(spinnerMaxIterations, "Max Iterations: ");
		addLabeled(spinnerNumSample, "Contour Samples: ");
		addLabeled(spinnerRadiusSample, "Radius Samples: ");
		addVerticalGlue();
	}

	@Override public void controlChanged( final Object source ) {
		if (source == imageView) {
			selectedView = imageView.getSelectedIndex();
			owner.viewUpdated();
		} else if (source == showEllipses) {
			bShowShapes = showEllipses.isSelected();
			owner.viewUpdated();
		} else if (source == showContour) {
			bShowContour = showContour.isSelected();
			owner.viewUpdated();
		} else {
			if (source == spinnerMinEdge) {
				config.minimumEdgeIntensity = ((Number)spinnerMinEdge.getValue()).doubleValue();
			} else if (source == selectZoom) {
				zoom = ((Number)selectZoom.getValue()).doubleValue();
				owner.viewUpdated();
				return;
			} else if (source == spinnerMinContourSize) {
				config.minimumContour.setTo(spinnerMinContourSize.getValue());
			} else if (source == spinnerMaxContourSize) {
				config.maximumContour.setTo(spinnerMaxContourSize.getValue());
			} else if (source == spinnerMaxError) {
				config.maxDistanceFromEllipse = ((Number)spinnerMaxError.getValue()).doubleValue();
			} else if (source == spinnerMinMinorAxisSize) {
				config.minimumMinorAxis.setTo(spinnerMinMinorAxisSize.getValue());
			} else if (source == spinnerConvergeTol) {
				config.convergenceTol = ((Number)spinnerConvergeTol.getValue()).doubleValue();
			} else if (source == spinnerMaxIterations) {
				config.maxIterations = ((Number)spinnerMaxIterations.getValue()).intValue();
			} else if (source == spinnerNumSample) {
				config.numSampleContour = ((Number)spinnerNumSample.getValue()).intValue();
			} else if (source == spinnerRadiusSample) {
				config.refineRadialSamples = ((Number)spinnerRadiusSample.getValue()).intValue();
			} else {
				throw new RuntimeException("Unknown source! BuG");
			}

			owner.configUpdate();
		}
	}

	public ThresholdControlPanel getThreshold() {
		return threshold;
	}

	public ConfigEllipseDetector getConfigEllipse() {
		return config;
	}
}
