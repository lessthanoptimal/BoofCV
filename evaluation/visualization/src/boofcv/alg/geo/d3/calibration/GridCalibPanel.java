/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.geo.d3.calibration;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Shows calibration grid detector status, configure display, and adjust parameters.
 *
 * @author Peter Abeles
 */
public class GridCalibPanel extends StandardAlgConfigPanel 
		implements ActionListener , ChangeListener
{
	// indicates if a calibration grid was found or not
	JLabel successIndicator;
	
	// selects which image to view
	JComboBox viewSelector;

	// toggle what is visible or not
	JCheckBox showBound;
	JCheckBox showPoints;
	JCheckBox showNumbers;

	// selects threshold to create binary image from
	JSpinner thresholdSpinner;

	boolean doShowBound = true;
	boolean doShowPoints = true;
	boolean doShowNumbers = true;

	Listener listener;
	
	int selectedView = 0;
	int thresholdLevel = 60;

	public GridCalibPanel() {
		viewSelector = new JComboBox();
		viewSelector.addItem("Original");
		viewSelector.addItem("Threshold");
		viewSelector.addItem("Cluster");
		viewSelector.addActionListener(this);
		viewSelector.setMaximumSize(viewSelector.getPreferredSize());

		successIndicator = new JLabel();

		showBound = new JCheckBox("Show Bound");
		showBound.setSelected(doShowBound);
		showBound.addActionListener(this);
		showBound.setMaximumSize(showBound.getPreferredSize());
		
		showPoints = new JCheckBox("Show Points");
		showPoints.setSelected(doShowPoints);
		showPoints.addActionListener(this);
		showPoints.setMaximumSize(showPoints.getPreferredSize());

		showNumbers = new JCheckBox("Show Numbers");
		showNumbers.setSelected(doShowNumbers);
		showNumbers.addActionListener(this);
		showNumbers.setMaximumSize(showNumbers.getPreferredSize());

		thresholdSpinner = new JSpinner(new SpinnerNumberModel(thresholdLevel,0, 255, 20));
		thresholdSpinner.addChangeListener(this);
		thresholdSpinner.setMaximumSize(thresholdSpinner.getPreferredSize());

		addLabeled(successIndicator, "Found:",this);
		addSeparator(100);
		addLabeled(viewSelector,"View ",this);
		addLabeled(thresholdSpinner,"Threshold",this);
		addSeparator(100);
		addAlignLeft(showBound, this);
		addAlignLeft(showPoints,this);
		addAlignLeft(showNumbers,this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( listener == null )
			return;

		if( e.getSource() == viewSelector ) {
			selectedView = viewSelector.getSelectedIndex();
		} else if( e.getSource() == showBound ) {
			doShowBound = showBound.isSelected();
		} else if( e.getSource() == showNumbers ) {
			doShowNumbers = showNumbers.isSelected();
		} else if( e.getSource() == showPoints ) {
			doShowPoints = showPoints.isSelected();
		}

		listener.calibEventGUI();
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( listener == null )
			return;

		if( e.getSource() == thresholdSpinner) {
			thresholdLevel = ((Number) thresholdSpinner.getValue()).intValue();
		}
		
		listener.calibEventProcess();
	}

	public void setSuccessMessage( String message , boolean worked )
	{
		successIndicator.setText(message);
		if( worked )
			successIndicator.setForeground(Color.BLACK);
		else
			successIndicator.setForeground(Color.RED);
	}

	public int getSelectedView() {
		return selectedView;
	}

	public boolean isShowBound() {
		return doShowBound;
	}

	public boolean isShowPoints() {
		return doShowPoints;
	}

	public boolean isShowNumbers() {
		return doShowNumbers;
	}

	public int getThresholdLevel() {
		return thresholdLevel;
	}

	public void setListener( Listener listener) {
		this.listener = listener;
	}

	public static interface Listener
	{
		public void calibEventGUI();
		
		public void calibEventProcess();
	}
}
