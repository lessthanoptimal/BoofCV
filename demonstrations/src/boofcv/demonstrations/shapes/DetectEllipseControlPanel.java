/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
public class DetectEllipseControlPanel extends StandardAlgConfigPanel
	implements ActionListener, ChangeListener
{
	DetectBlackEllipseApp owner;

	// selects which image to view
	JComboBox imageView;

	JSpinner selectZoom;

	JCheckBox showEllipses;
	JCheckBox showContour;

	int selectedView = 0;
	boolean bShowEllipses = true;
	boolean bShowContour = false;

	ThresholdControlPanel threshold;

	JSpinner spinnerMaxContourSize;
	JSpinner spinnerMinContourSize;

	JSpinner spinnerConvergeTol;
	JSpinner spinnerMaxIterations;
	JSpinner spinnerNumSample;
	JSpinner spinnerRadiusSample;

	JSpinner spinnerMinEdge;

	ConfigEllipseDetector config = new ConfigEllipseDetector();

	double zoom = 1;

	public DetectEllipseControlPanel(DetectBlackEllipseApp owner) {
		this.owner = owner;

		imageView = new JComboBox();
		imageView.addItem("Input");
		imageView.addItem("Binary");
		imageView.addItem("Black");
		imageView.addActionListener(this);
		imageView.setMaximumSize(imageView.getPreferredSize());

		selectZoom = new JSpinner(new SpinnerNumberModel(1,0.1,50,1));
		selectZoom.addChangeListener(this);
		selectZoom.setMaximumSize(selectZoom.getPreferredSize());

		showEllipses = new JCheckBox("Ellipses");
		showEllipses.setSelected(bShowEllipses);
		showEllipses.addActionListener(this);
		showContour = new JCheckBox("Contour");
		showContour.addActionListener(this);
		showContour.setSelected(bShowContour);

		threshold = new ThresholdControlPanel(owner);


		spinnerMinContourSize = new JSpinner(new SpinnerNumberModel(config.minimumContour,0,50000,20));
		spinnerMinContourSize.addChangeListener(this);
		spinnerMinContourSize.setMaximumSize(spinnerMinContourSize.getPreferredSize());
		spinnerMaxContourSize = new JSpinner(new SpinnerNumberModel(config.maximumContour,0,50000,20));
		spinnerMaxContourSize.addChangeListener(this);
		spinnerMaxContourSize.setMaximumSize(spinnerMaxContourSize.getPreferredSize());

		spinnerMinEdge = new JSpinner(new SpinnerNumberModel(config.minimumEdgeIntensity,0.0,255.0,1.0));
		spinnerMinEdge.setMaximumSize(spinnerMinEdge.getPreferredSize());
		spinnerMinEdge.addChangeListener(this);

		spinnerConvergeTol = new JSpinner(new SpinnerNumberModel(config.convergenceTol,0.0,1.0,0.001));
		configureSpinnerFloat(spinnerConvergeTol);
		spinnerMaxIterations = new JSpinner(new SpinnerNumberModel(config.maxIterations,0,200,2));
		spinnerMaxIterations.addChangeListener(this);
		spinnerMaxIterations.setMaximumSize(spinnerMaxIterations.getPreferredSize());
		spinnerNumSample = new JSpinner(new SpinnerNumberModel(config.numSampleContour,1,200,2));
		spinnerNumSample.addChangeListener(this);
		spinnerNumSample.setMaximumSize(spinnerNumSample.getPreferredSize());
		spinnerRadiusSample = new JSpinner(new SpinnerNumberModel(config.refineRadialSamples, 1, 5, 1));
		spinnerRadiusSample.addChangeListener(this);
		spinnerRadiusSample.setMaximumSize(spinnerRadiusSample.getPreferredSize());

		addLabeled(imageView, "View: ", this);
		addLabeled(selectZoom,"Zoom",this);
		addAlignLeft(showEllipses, this);
		addAlignLeft(showContour, this);
		add(threshold);
		addCenterLabel("Contour", this);
		addLabeled(spinnerMinContourSize, "Min Contour Size: ", this);
		addLabeled(spinnerMaxContourSize, "Max Contour Size: ", this);
		addLabeled(spinnerMinEdge, "Edge Intensity: ", this);
		addCenterLabel("Refinement", this);
		addLabeled(spinnerConvergeTol, "Converge Tol: ", this);
		addLabeled(spinnerMaxIterations, "Max Iterations: ", this);
		addLabeled(spinnerNumSample, "Contour Samples: ", this);
		addLabeled(spinnerRadiusSample, "Radius Samples: ", this);
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
		} else if( e.getSource() == showEllipses) {
			bShowEllipses = showEllipses.isSelected();
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
