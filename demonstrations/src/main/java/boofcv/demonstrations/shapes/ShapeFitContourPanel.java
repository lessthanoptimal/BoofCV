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

import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Controls for {@link ShapeFitContourApp}
 *
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

	JSpinner selectMinimumSplitPixels;
	JSpinner selectCornerPenalty;
	JCheckBox showCorners;
	JCheckBox showContour;

	int selectedAlgorithm = 0;
	int selectedView = 0;
	double zoom = 1;

	int minimumSplitPixels = 10;
	double cornerPenalty = 0.25;

	boolean cornersVisible = false;
	boolean contoursVisible = true;


	public ShapeFitContourPanel( ShapeFitContourApp owner ) {
		this.owner = owner;

		algorithmCombo = new JComboBox();
		algorithmCombo.addItem("Polyline");
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

		selectMinimumSplitPixels = spinner(minimumSplitPixels,1,1000,5);
		selectCornerPenalty = new JSpinner(new SpinnerNumberModel(cornerPenalty,0,10.0,0.01));
		selectCornerPenalty.setEditor(new JSpinner.NumberEditor(selectCornerPenalty, "#,##0.00;(#,##0.00)"));
		selectCornerPenalty.addChangeListener(this);
		selectCornerPenalty.setMaximumSize(selectCornerPenalty.getPreferredSize());

//		JComponent editor = selectSplitFraction.getEditor();
//		JFormattedTextField ftf = ((JSpinner.DefaultEditor) editor).getTextField();
//		ftf.setColumns(3);
		showCorners = new JCheckBox("Show Corners");
		showCorners.setSelected(cornersVisible);
		showCorners.addChangeListener(this);
		showContour = new JCheckBox("Show Contours");
		showContour.setSelected(contoursVisible);
		showContour.addChangeListener(this);



		addLabeled(algorithmCombo, "Shape");
		addSeparator(200);
		addLabeled(imageView, "Background");
		addLabeled(selectZoom,"Zoom");
		addAlignCenter(threshold);
		addLabeled(selectMinimumSplitPixels, "Min Split Pixels");
		addLabeled(selectCornerPenalty, "Corner Penalty");
		addAlignLeft(showCorners);
		addAlignLeft(showContour);
		addVerticalGlue();
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
		owner.processImage(0,0 , null, null);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == selectMinimumSplitPixels) {
			minimumSplitPixels = (Integer) selectMinimumSplitPixels.getValue();
		} else if( e.getSource() == selectCornerPenalty) {
			cornerPenalty = (Double) selectCornerPenalty.getValue();
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
		owner.processImage(0,0 , null, null);
	}

	private void updateEnabledByAlgorithm() {
		if( selectedAlgorithm == 0 ) {
			showCorners.setEnabled(true);
			selectCornerPenalty.setEnabled(true);
			selectMinimumSplitPixels.setEnabled(true);
		} else {
			showCorners.setEnabled(false);
			selectCornerPenalty.setEnabled(false);
			selectMinimumSplitPixels.setEnabled(false);
		}
	}

	public void setZoom(double _zoom) {
		if( this.zoom != _zoom ) {
			this.zoom = _zoom;
			BoofSwingUtil.invokeNowOrLater(()->selectZoom.setValue(_zoom));
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

	public int getMinimumSplitPixels() {
		return minimumSplitPixels;
	}

	public double getCornerPenalty() {
		return cornerPenalty;
	}

	public boolean isCornersVisible() {
		return cornersVisible;
	}

	public ThresholdControlPanel getThreshold() {
		return threshold;
	}
}
