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

package boofcv.demonstrations.shapes;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Peter Abeles
 */
public class ShapeFitContourPanel extends StandardAlgConfigPanel
	implements ActionListener, ChangeListener
{
	ShapeFitContourApp owner;

	// which type of shape it should fit
	JComboBox algorithmCombo;

	// selects which image to view
	JComboBox imageView;

	JSpinner selectZoom;

	ThresholdControlPanel threshold;

	JSpinner selectMinimumSideFraction;
	JSpinner selectSplitFraction;
	JCheckBox showCorners;
	JCheckBox showContour;

	int selectedAlgorithm = 0;
	int selectedView = 0;
	double zoom = 1;

	double minimumSplitFraction = 0.01;
	double splitFraction = 0.05;

	boolean cornersVisible = false;
	boolean contoursVisible = true;


	public ShapeFitContourPanel( ShapeFitContourApp owner ) {
		this.owner = owner;

		algorithmCombo = new JComboBox();
		algorithmCombo.addItem("Polygon");
		algorithmCombo.addItem("Oval");
		algorithmCombo.addActionListener(this);
		algorithmCombo.setMaximumSize(algorithmCombo.getPreferredSize());

		imageView = new JComboBox();
		imageView.addItem("Input");
		imageView.addItem("Binary");
		imageView.addItem("Black");
		imageView.addActionListener(this);
		imageView.setMaximumSize(imageView.getPreferredSize());

		selectZoom = new JSpinner(new SpinnerNumberModel(1,0.1,50,1));
		selectZoom.addChangeListener(this);
		selectZoom.setMaximumSize(selectZoom.getPreferredSize());

		threshold = new ThresholdControlPanel(owner);

		selectMinimumSideFraction = new JSpinner(new SpinnerNumberModel(minimumSplitFraction,0,0.999,0.0025));
		selectMinimumSideFraction.setEditor(new JSpinner.NumberEditor(selectMinimumSideFraction, "#,####0.0000;(#,####0.0000)"));
		selectMinimumSideFraction.addChangeListener(this);
		selectMinimumSideFraction.setMaximumSize(selectMinimumSideFraction.getPreferredSize());
		selectSplitFraction = new JSpinner(new SpinnerNumberModel(splitFraction,0,1.0,0.01));
		selectSplitFraction.setEditor(new JSpinner.NumberEditor(selectSplitFraction, "#,##0.00;(#,##0.00)"));
//		JComponent editor = selectSplitFraction.getEditor();
//		JFormattedTextField ftf = ((JSpinner.DefaultEditor) editor).getTextField();
//		ftf.setColumns(3);
		showCorners = new JCheckBox("Show Corners");
		showCorners.setSelected(cornersVisible);
		showCorners.addChangeListener(this);
		showContour = new JCheckBox("Show Contours");
		showContour.setSelected(contoursVisible);
		showContour.addChangeListener(this);

		selectSplitFraction.addChangeListener(this);
		selectSplitFraction.setMaximumSize(selectSplitFraction.getPreferredSize());

		addLabeled(algorithmCombo, "Type of Shape", this);
		addSeparator(200);
		addLabeled(imageView, "Background", this);
		addLabeled(selectZoom,"Zoom",this);
		addAlignCenter(threshold,this);
		addLabeled(selectMinimumSideFraction, "Min Side Fraction", this);
		addLabeled(selectSplitFraction, "Split Fraction",this);
		addAlignLeft(showCorners, this);
		addAlignLeft(showContour, this);
		addVerticalGlue(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == algorithmCombo ) {
			selectedAlgorithm = algorithmCombo.getSelectedIndex();
			updateEnabledByAlgorithm();
		} else if( e.getSource() == imageView ) {
			selectedView = imageView.getSelectedIndex();
			owner.viewUpdated();
			return;
		}
		owner.processImage(null,null);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == selectMinimumSideFraction) {
			minimumSplitFraction = (Double) selectMinimumSideFraction.getValue();
		} else if( e.getSource() == selectSplitFraction) {
			splitFraction = (Double) selectSplitFraction.getValue();
		} else if( e.getSource() == selectZoom ) {
			zoom = ((Number) selectZoom.getValue()).doubleValue();
			owner.viewUpdated();
			return;
		} else if( e.getSource() == showContour ) {
			contoursVisible = showContour.isSelected();
			owner.viewUpdated();
			return;
		} else if( e.getSource() == showCorners ) {
			cornersVisible = showCorners.isSelected();
			owner.viewUpdated();
			return;
		}
		owner.processImage(null,null);
	}

	private void updateEnabledByAlgorithm() {
		if( selectedAlgorithm == 0 ) {
			showCorners.setEnabled(true);
			selectSplitFraction.setEnabled(true);
			selectMinimumSideFraction.setEnabled(true);
		} else {
			showCorners.setEnabled(false);
			selectSplitFraction.setEnabled(false);
			selectMinimumSideFraction.setEnabled(false);
		}
	}

	public int getSelectedAlgorithm() {
		return selectedAlgorithm;
	}

	public int getSelectedView() {
		return selectedView;
	}

	public double getZoom() {
		return zoom;
	}

	public double getMinimumSplitFraction() {
		return minimumSplitFraction;
	}

	public double getSplitFraction() {
		return splitFraction;
	}

	public boolean isCornersVisible() {
		return cornersVisible;
	}

	public ThresholdControlPanel getThreshold() {
		return threshold;
	}
}
