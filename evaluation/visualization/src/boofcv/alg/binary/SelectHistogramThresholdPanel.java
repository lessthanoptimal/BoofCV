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

package boofcv.alg.binary;

import boofcv.gui.binary.HistogramThresholdPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Creates a control panel for selecting thresholds of pixel image intensities
 *
 * @author Peter Abeles
 */
public class SelectHistogramThresholdPanel extends JPanel implements ChangeListener , ActionListener {
	JSlider thresholdLevel;
	HistogramThresholdPanel histogramPanel;
	JButton toggleButton;

	Listener listener;

	int valueThreshold;
	boolean valueDown;

	public SelectHistogramThresholdPanel( int threshold ,
										  boolean directionDown )
	{
	    this.valueThreshold = threshold;
		this.valueDown = directionDown;

		histogramPanel = new HistogramThresholdPanel(256,256);
		histogramPanel.setPreferredSize(new Dimension(120,60));
		histogramPanel.setMaximumSize(histogramPanel.getPreferredSize());

		thresholdLevel = new JSlider(JSlider.HORIZONTAL,0,255,20);
		thresholdLevel.setMajorTickSpacing(20);
		thresholdLevel.setPaintTicks(true);
		thresholdLevel.addChangeListener(this);
		thresholdLevel.setValue(threshold);

		toggleButton = new JButton();
		toggleButton.setPreferredSize(new Dimension(100,30));
		toggleButton.setMaximumSize(toggleButton.getPreferredSize());
		toggleButton.setMinimumSize(toggleButton.getPreferredSize());
		setToggleText();
		toggleButton.addActionListener(this);

		add(histogramPanel);
		add(Box.createRigidArea(new Dimension(8, 8)));
		add(thresholdLevel);
		add(toggleButton);

	}

	public HistogramThresholdPanel getHistogramPanel() {
		return histogramPanel;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public int getThreshold() {
		return valueThreshold;
	}

	public boolean isDown() {
		return valueDown;
	}

	private void setToggleText() {
		if(valueDown)
			toggleButton.setText("down");
		else
			toggleButton.setText("Up");
	}

	@Override
	public void stateChanged(ChangeEvent e) {

		if( e.getSource() == thresholdLevel )  {
			int oldValue = valueThreshold;
			valueThreshold = ((Number)thresholdLevel.getValue()).intValue();
			if( oldValue == valueThreshold )
				return;
		}

		histogramPanel.setThreshold(valueThreshold,valueDown);
		histogramPanel.repaint();

		if( listener != null )
			listener.histogramThresholdChange();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == toggleButton ) {
			valueDown = !valueDown;
			setToggleText();
		}

		histogramPanel.setThreshold(valueThreshold,valueDown);
		histogramPanel.repaint();

		if( listener != null )
			listener.histogramThresholdChange();
	}

	public void setThreshold(int threshold) {
		valueThreshold = threshold;
		thresholdLevel.setValue(threshold);
		histogramPanel.setThreshold(valueThreshold,valueDown);
	}

	public static interface Listener
	{
		public void histogramThresholdChange();
	}
}
