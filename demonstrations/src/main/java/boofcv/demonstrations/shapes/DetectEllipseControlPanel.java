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

package boofcv.demonstrations.shapes;

import boofcv.factory.shape.ConfigEllipseDetector;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Configures ellipse detectors
 *
 * @author Peter Abeles
 */
public class DetectEllipseControlPanel extends DetectBlackShapePanel
	implements ActionListener, ChangeListener
{
	DetectBlackEllipseApp owner;

	// selects which image to view
	JComboBox imageView;

	JCheckBox showEllipses;
	JCheckBox showContour;

	boolean bShowShapes = true;
	boolean bShowContour = false;

	ThresholdControlPanel threshold;

	JSpinner spinnerMaxContourSize;
	JSpinner spinnerMinContourSize;
	JSpinner spinnerMinMinorAxisSize;

	JSpinner spinnerConvergeTol;
	JSpinner spinnerMaxIterations;
	JSpinner spinnerNumSample;
	JSpinner spinnerRadiusSample;

	JSpinner spinnerMinEdge;

	ConfigEllipseDetector config = new ConfigEllipseDetector();

	public DetectEllipseControlPanel(DetectBlackEllipseApp owner) {
		this.owner = owner;

		imageView = new JComboBox();
		imageView.addItem("Input");
		imageView.addItem("Binary");
		imageView.addItem("Black");
		imageView.addActionListener(this);
		imageView.setMaximumSize(imageView.getPreferredSize());

		selectZoom = new JSpinner(new SpinnerNumberModel(1,MIN_ZOOM,MAX_ZOOM,1));
		selectZoom.addChangeListener(this);
		selectZoom.setMaximumSize(selectZoom.getPreferredSize());

		showEllipses = new JCheckBox("Ellipses");
		showEllipses.setSelected(bShowShapes);
		showEllipses.addActionListener(this);
		showContour = new JCheckBox("Contour");
		showContour.addActionListener(this);
		showContour.setSelected(bShowContour);

		threshold = new ThresholdControlPanel(owner);


		spinnerMinContourSize = spinner(config.minimumContour,0,50000,20);
		spinnerMaxContourSize = spinner(config.maximumContour,0,50000,20);
		spinnerMinMinorAxisSize = spinner(config.minimumMinorAxis,0,1000.0,1.0);

		spinnerMinEdge = spinner(config.minimumEdgeIntensity,0.0,255.0,1.0);

		spinnerConvergeTol = spinner(config.convergenceTol,0.0,1.0,0.001);
		configureSpinnerFloat(spinnerConvergeTol, 1, 3);
		spinnerMaxIterations = spinner(config.maxIterations,0,200,2);
		spinnerNumSample = spinner(config.numSampleContour,1,200,2);
		spinnerRadiusSample = spinner(config.refineRadialSamples, 1, 5, 1);

		addLabeled(processingTimeLabel,"Time (ms)");
		addLabeled(imageSizeLabel,"Size");
		addLabeled(imageView, "View: ");
		addLabeled(selectZoom,"Zoom");
		addAlignLeft(showEllipses);
		addAlignLeft(showContour);
		add(threshold);
		addCenterLabel("Contour");
		addLabeled(spinnerMinContourSize, "Min Contour Size: ");
		addLabeled(spinnerMaxContourSize, "Max Contour Size: ");
		addLabeled(spinnerMinMinorAxisSize, "Min Minor Axis: ");
		addLabeled(spinnerMinEdge, "Edge Intensity: ");
		addCenterLabel("Refinement");
		addLabeled(spinnerConvergeTol, "Converge Tol: ");
		addLabeled(spinnerMaxIterations, "Max Iterations: ");
		addLabeled(spinnerNumSample, "Contour Samples: ");
		addLabeled(spinnerRadiusSample, "Radius Samples: ");
		addVerticalGlue();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == imageView ) {
			selectedView = imageView.getSelectedIndex();
			owner.viewUpdated();
		} else if( e.getSource() == showEllipses) {
			bShowShapes = showEllipses.isSelected();
			owner.viewUpdated();
		} else if( e.getSource() == showContour ) {
			bShowContour = showContour.isSelected();
			owner.viewUpdated();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {

		if( e.getSource() == spinnerMinEdge) {
			config.minimumEdgeIntensity = ((Number) spinnerMinEdge.getValue()).doubleValue();
		} else if( e.getSource() == selectZoom ) {
			zoom = ((Number) selectZoom.getValue()).doubleValue();
			owner.viewUpdated();
			return;
		} else if( e.getSource() == spinnerMinContourSize) {
			config.minimumContour = ((Number) spinnerMinContourSize.getValue()).intValue();
		} else if( e.getSource() == spinnerMaxContourSize) {
			config.maximumContour = ((Number) spinnerMaxContourSize.getValue()).intValue();
		} else if( e.getSource() == spinnerMinMinorAxisSize) {
			config.minimumMinorAxis = ((Number) spinnerMinMinorAxisSize.getValue()).doubleValue();
		} else if( e.getSource() == spinnerConvergeTol) {
			config.convergenceTol = ((Number) spinnerConvergeTol.getValue()).doubleValue();
		} else if( e.getSource() == spinnerMaxIterations) {
			config.maxIterations = ((Number) spinnerMaxIterations.getValue()).intValue();
		} else if( e.getSource() == spinnerNumSample) {
			config.numSampleContour = ((Number) spinnerNumSample.getValue()).intValue();
		} else if( e.getSource() == spinnerRadiusSample) {
			config.refineRadialSamples = ((Number) spinnerRadiusSample.getValue()).intValue();
		} else {
			throw new RuntimeException("Unknown source! BuG");
		}

		owner.configUpdate();
	}

	public ThresholdControlPanel getThreshold() {
		return threshold;
	}

	public ConfigEllipseDetector getConfigEllipse() {
		return config;
	}
}
