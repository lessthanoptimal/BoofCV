/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Shows calibration grid detector status, configure display, and adjust parameters.
 *
 * @author Peter Abeles
 */
public class GridCalibPanel extends StandardAlgConfigPanel 
		implements  ChangeListener, ItemListener
{
	// indicates if a calibration grid was found or not
	JLabel successIndicator;
	
	// selects which image to view
	JComboBox viewSelector;

	// toggle what is visible or not
	JCheckBox showBound;
	JCheckBox showPoints;
	JCheckBox showNumbers;
	JCheckBox showGraph;

	// allows the user to change the image zoom
	JSpinner selectZoom;

	// selects threshold to create binary image from
	JSpinner thresholdSpinner;
	// should the threshold be manually selected or automatically
	JCheckBox manualThreshold;

	boolean doShowBound = true;
	boolean doShowPoints = true;
	boolean doShowNumbers = true;
	boolean doShowGraph = false;

	double scale = 1;
	boolean isManual = false;

	Listener listener;
	
	int selectedView = 0;
	int thresholdLevel = 60;

	public GridCalibPanel( boolean hasManualMode ) {
		viewSelector = new JComboBox();
		viewSelector.addItem("Original");
		viewSelector.addItem("Threshold");
		viewSelector.addItem("Cluster");
		viewSelector.addItemListener(this);
		viewSelector.setMaximumSize(viewSelector.getPreferredSize());

		selectZoom = new JSpinner(new SpinnerNumberModel(1,0.1,50,1));
		selectZoom.addChangeListener(this);
		selectZoom.setMaximumSize(selectZoom.getPreferredSize());
		
		successIndicator = new JLabel();

		showBound = new JCheckBox("Show Bound");
		showBound.setSelected(doShowBound);
		showBound.addItemListener(this);
		showBound.setMaximumSize(showBound.getPreferredSize());
		
		showPoints = new JCheckBox("Show Points");
		showPoints.setSelected(doShowPoints);
		showPoints.addItemListener(this);
		showPoints.setMaximumSize(showPoints.getPreferredSize());

		showNumbers = new JCheckBox("Show Numbers");
		showNumbers.setSelected(doShowNumbers);
		showNumbers.addItemListener(this);
		showNumbers.setMaximumSize(showNumbers.getPreferredSize());

		showGraph = new JCheckBox("Show Graph");
		showGraph.setSelected(doShowGraph);
		showGraph.addItemListener(this);
		showGraph.setMaximumSize(showGraph.getPreferredSize());

		thresholdSpinner = new JSpinner(new SpinnerNumberModel(thresholdLevel,0, 255, 20));
		thresholdSpinner.addChangeListener(this);
		thresholdSpinner.setMaximumSize(thresholdSpinner.getPreferredSize());

		manualThreshold = new JCheckBox("Manual");
		manualThreshold.setSelected(isManual);
		manualThreshold.addItemListener(this);
		manualThreshold.setMaximumSize(manualThreshold.getPreferredSize());
		
		if( !isManual ) {
			thresholdSpinner.setEnabled(false);
		}

		addLabeled(successIndicator, "Found:", this);
		addSeparator(100);
		addLabeled(viewSelector,"View ",this);
		addSeparator(100);
		if( hasManualMode )
			addAlignLeft(manualThreshold,this);
		addLabeled(thresholdSpinner,"Threshold",this);
		addSeparator(100);
		addLabeled(selectZoom,"Zoom ",this);
		addAlignLeft(showBound, this);
		addAlignLeft(showPoints,this);
		addAlignLeft(showNumbers,this);
		addAlignLeft(showGraph,this);
	}

	public void addView( String name ) {
		viewSelector.addItem(name);
	}
	
	@Override
	public void stateChanged(ChangeEvent e) {
		if( listener == null )
			return;

		if( e.getSource() == thresholdSpinner) {
			thresholdLevel = ((Number) thresholdSpinner.getValue()).intValue();
			// Only need to recompute
			if( isManual )
				listener.calibEventProcess();
		} else if( e.getSource() == selectZoom ) {
			scale = ((Number)selectZoom.getValue()).doubleValue();
			listener.calibEventGUI();
		}
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

	public boolean isShowGraph() {
		return doShowGraph;
	}

	public int getThresholdLevel() {
		return thresholdLevel;
	}

	public boolean isManual() {
		return isManual;
	}

	public void setListener( Listener listener) {
		this.listener = listener;
	}

	public double getScale() {
		return scale;
	}

	public void setThreshold(int threshold) {
		thresholdSpinner.setValue(threshold);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if( listener == null )
			return;

		if( e.getSource() == viewSelector ) {
			selectedView = viewSelector.getSelectedIndex();
			listener.calibEventGUI();
		} else if( e.getSource() == showBound ) {
			doShowBound = showBound.isSelected();
			listener.calibEventGUI();
		} else if( e.getSource() == showNumbers ) {
			doShowNumbers = showNumbers.isSelected();
			listener.calibEventGUI();
		} else if( e.getSource() == showPoints ) {
			doShowPoints = showPoints.isSelected();
			listener.calibEventGUI();
		} else if( e.getSource() == showGraph ) {
			doShowGraph = showGraph.isSelected();
			listener.calibEventGUI();
		} else if( e.getSource() == thresholdSpinner) {
			thresholdLevel = ((Number) thresholdSpinner.getValue()).intValue();
			// Only need to recompute
			if( isManual )
				listener.calibEventProcess();
		} else if( e.getSource() == selectZoom ) {
			scale = ((Number)selectZoom.getValue()).doubleValue();
			listener.calibEventGUI();
		} else if( e.getSource() == manualThreshold) {
			if( isManual != manualThreshold.isSelected() ) {
				isManual = manualThreshold.isSelected();
				thresholdSpinner.setEnabled(isManual);

				listener.calibEventProcess();
			}
		}
	}

	public static interface Listener
	{
		public void calibEventGUI();
		
		public void calibEventProcess();
	}
}
