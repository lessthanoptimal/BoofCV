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

package boofcv.demonstrations.calibration;

import boofcv.demonstrations.shapes.ThresholdControlPanel;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.ViewedImageInfoPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Objects;

/**
 * Shows calibration grid detector status, configure display, and adjust parameters.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DetectCalibrationPanel extends StandardAlgConfigPanel
		implements ChangeListener, ActionListener, ItemListener {
	ViewedImageInfoPanel viewInfo = new ViewedImageInfoPanel();

	// indicates if a calibration grid was found or not
	JLabel successIndicator;

	// How long the algorithm took to run
	JLabel labelSpeed = new JLabel();

	// select the calibration grid's dimensions
	JSpinner selectRows;
	JSpinner selectColumns;

	// selects which image to view
	JComboBox viewSelector;

	// toggle what is visible or not
	JCheckBox showPoints;
	JCheckBox showNumbers;

	JCheckBox showGraph;
	JCheckBox showGrids;
	JCheckBox showShapes;
	JCheckBox showOrder;
	JCheckBox showContour;

	// selects threshold to create binary image from
	@Nullable ThresholdControlPanel threshold;

	boolean doShowPoints = true;
	boolean doShowNumbers = true;
	boolean doShowClusters = false;
	boolean doShowGraph = false;
	boolean doShowOrder = true;
	boolean doShowGrids = false;
	boolean doShowShapes = false;
	boolean doShowContour = false;

	int gridRows;
	int gridColumns;

	Listener listener;

	int selectedView = 0;

	public DetectCalibrationPanel( int gridRows, int gridColumns ) {
		this(gridRows, gridColumns, true);
	}

	public DetectCalibrationPanel( int gridRows, int gridColumns, boolean addComponents ) {
		this.gridRows = gridRows;
		this.gridColumns = gridColumns;

		viewInfo.setListener(zoom -> listener.calibEventGUI());

		viewSelector = new JComboBox();
		viewSelector.addItem("Original");
		viewSelector.addItem("Threshold");
		viewSelector.addItemListener(this);
		viewSelector.setMaximumSize(viewSelector.getPreferredSize());

		successIndicator = new JLabel();

		showPoints = checkbox("Show Points", doShowPoints);
		showNumbers = checkbox("Show Numbers", doShowNumbers);
		showGraph = checkbox("Show Graphs", doShowGraph);
		showGrids = checkbox("Show Grids", doShowGrids);
		showOrder = checkbox("Show Order", doShowOrder);
		showShapes = checkbox("Show Shapes", doShowShapes);
		showContour = checkbox("Show Contour", doShowContour);

		selectRows = new JSpinner(new SpinnerNumberModel(gridRows, 2, 100, 1));
		selectRows.addChangeListener(this);
		selectRows.setMaximumSize(selectRows.getPreferredSize());

		selectColumns = new JSpinner(new SpinnerNumberModel(gridColumns, 2, 100, 1));
		selectColumns.addChangeListener(this);
		selectColumns.setMaximumSize(selectColumns.getPreferredSize());

		threshold = new ThresholdControlPanel(() -> listener.calibEventDetectorModified(), ConfigThreshold.local(ThresholdType.BLOCK_OTSU, 81));
		threshold.addHistogramGraph();

		if (addComponents)
			addComponents();
	}

	protected void addComponents() {
		JPanel togglePanel = new JPanel(new GridLayout(0, 2));
		togglePanel.add(showPoints);
		togglePanel.add(showNumbers);
		togglePanel.add(showGraph);
		togglePanel.add(showGrids);
		togglePanel.add(showOrder);
		togglePanel.add(showShapes);
		togglePanel.add(showContour);

		addLabeled(labelSpeed, "Time (ms)");
		addLabeled(successIndicator, "Found:");
		add(viewInfo);
		addLabeled(viewSelector, "View ");
		addLabeled(selectRows, "Rows");
		addLabeled(selectColumns, "Cols");

		add(togglePanel);
	}

	public void addView( String name ) {
		viewSelector.addItem(name);
	}

	@Override
	public void stateChanged( ChangeEvent e ) {
		if (listener == null)
			return;

		if (e.getSource() == selectRows) {
			gridRows = ((Number)selectRows.getValue()).intValue();
			listener.calibEventDetectorModified();
		} else if (e.getSource() == selectColumns) {
			gridColumns = ((Number)selectColumns.getValue()).intValue();
			listener.calibEventDetectorModified();
		}
	}

	public void setProcessingTime( double milliseconds ) {
		labelSpeed.setText(String.format("%.1f", milliseconds));
	}

	public void setSuccessMessage( String message, boolean worked ) {
		successIndicator.setText(message);
		if (worked)
			successIndicator.setForeground(Color.BLACK);
		else
			successIndicator.setForeground(Color.RED);
	}

	public int getSelectedView() {
		return selectedView;
	}

	public boolean isShowShapes() {
		return doShowShapes;
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

	public boolean isShowGrids() {
		return doShowGrids;
	}

	public boolean isShowOrder() {
		return doShowOrder;
	}

	public void setListener( Listener listener ) {
		this.listener = listener;
	}

	public void setCursor( final double x, final double y ) {
		BoofSwingUtil.invokeNowOrLater(
				() -> viewInfo.setCursor(x, y));
	}

	@Override
	public void itemStateChanged( ItemEvent e ) {
		if (listener == null)
			return;

		if (e.getSource() == viewSelector) {
			selectedView = viewSelector.getSelectedIndex();
			listener.calibEventGUI();
		}
	}

	public int getGridRows() {
		return gridRows;
	}

	public int getGridColumns() {
		return gridColumns;
	}

	public ViewedImageInfoPanel getViewInfo() {
		return viewInfo;
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		if (listener == null)
			return;

		if (e.getSource() == viewSelector) {
			selectedView = viewSelector.getSelectedIndex();
			listener.calibEventGUI();
		} else if (e.getSource() == showShapes) {
			doShowShapes = showShapes.isSelected();
			listener.calibEventGUI();
		} else if (e.getSource() == showNumbers) {
			doShowNumbers = showNumbers.isSelected();
			listener.calibEventGUI();
		} else if (e.getSource() == showPoints) {
			doShowPoints = showPoints.isSelected();
			listener.calibEventGUI();
		} else if (e.getSource() == showGraph) {
			doShowGraph = showGraph.isSelected();
			listener.calibEventGUI();
		} else if (e.getSource() == showGrids) {
			doShowGrids = showGrids.isSelected();
			listener.calibEventGUI();
		} else if (e.getSource() == showOrder) {
			doShowOrder = showOrder.isSelected();
			listener.calibEventGUI();
		} else if (e.getSource() == showContour) {
			doShowContour = showContour.isSelected();
			listener.calibEventGUI();
		}
	}

	public ThresholdControlPanel getThreshold() {
		return Objects.requireNonNull(threshold);
	}

	public interface Listener {
		void calibEventGUI();

		void calibEventDetectorModified();
	}
}
