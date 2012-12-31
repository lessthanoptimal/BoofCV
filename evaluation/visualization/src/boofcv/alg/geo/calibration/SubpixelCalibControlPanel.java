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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Control panel for {@link DebugSquaresSubpixelApp}
 * 
 * @author Peter Abeles
 */
public class SubpixelCalibControlPanel extends StandardAlgConfigPanel
		implements ItemListener, ChangeListener
{
	// toggle what is visible or not
	JCheckBox showPixel;
	JCheckBox showSubpixel;

	// allows the user to change the image zoom
	JSpinner selectZoom;

	Listener listener;
	
	boolean doShowPixel = true;
	boolean doShowSubpixel = true;

	double zoom=1;

	public SubpixelCalibControlPanel( Listener listener ) {
		this.listener = listener;

		selectZoom = new JSpinner(new SpinnerNumberModel(zoom,1,100,1));
		selectZoom.addChangeListener(this);
		selectZoom.setMaximumSize(selectZoom.getPreferredSize());

		showPixel = new JCheckBox("Show Crude");
		showPixel.setSelected(doShowPixel);
		showPixel.addItemListener(this);
		showPixel.setMaximumSize(showPixel.getPreferredSize());

		showSubpixel = new JCheckBox("Show Refined");
		showSubpixel.setSelected(doShowSubpixel);
		showSubpixel.addItemListener(this);
		showSubpixel.setMaximumSize(showSubpixel.getPreferredSize());

		addLabeled(selectZoom, "Zoom:",this);
		addAlignLeft(showPixel, this);
		addAlignLeft(showSubpixel,this);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( listener == null )
			return;
		
		if( e.getSource() == selectZoom ) {
			zoom = ((Number)selectZoom.getValue()).doubleValue();
		}

		listener.updateGUI();
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if( listener == null )
			return;

		if( e.getSource() == showPixel ) {
			doShowPixel = showPixel.isSelected();
		} else if( e.getSource() == showSubpixel ) {
			doShowSubpixel = showSubpixel.isSelected();
		}

		listener.updateGUI();
	}

	public boolean isShowPixel() {
		return doShowPixel;
	}

	public boolean isShowSubpixel() {
		return doShowSubpixel;
	}

	public double getScale() {
		return zoom;
	}

	public static interface Listener
	{
		public void updateGUI();
	}
}
