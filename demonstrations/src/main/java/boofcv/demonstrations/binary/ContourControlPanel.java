/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.binary;

import boofcv.demonstrations.shapes.ThresholdControlPanel;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.ConnectRule;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Peter Abeles
 */
public class ContourControlPanel extends StandardAlgConfigPanel
	implements ActionListener, ChangeListener
{

	VisualizeBinaryContourApp<?> owner;

	ThresholdControlPanel threshold;

	// selects which image to view
	JComboBox imageView;
	// connection rule
	JComboBox connectCombo;

	JSpinner selectZoom;

	private ConnectRule connectRule = ConnectRule.FOUR;
	public int selectedView;
	public double zoom = 1.0;

	public ContourControlPanel(VisualizeBinaryContourApp<?> owner) {
		this.owner = owner;

		imageView = new JComboBox();
		imageView.addItem("Input");
		imageView.addItem("Binary");
		imageView.addItem("Black");
		imageView.addActionListener(this);
		imageView.setMaximumSize(imageView.getPreferredSize());

		connectCombo = new JComboBox();
		connectCombo.addItem("4-Connect");
		connectCombo.addItem("8-Connect");
		connectCombo.addActionListener(this);
		connectCombo.setMaximumSize(connectCombo.getPreferredSize());

		selectZoom = new JSpinner(new SpinnerNumberModel(zoom,0.1,50,1));
		selectZoom.addChangeListener(this);
		selectZoom.setMaximumSize(selectZoom.getPreferredSize());

		threshold = new ThresholdControlPanel(owner);

		addLabeled(imageView,"View");
		addLabeled(selectZoom,"Zoom");
		addLabeled(connectCombo, "Connect");
		add(threshold);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == connectCombo ) {
			connectRule = ConnectRule.values()[connectCombo.getSelectedIndex()];
			owner.contourAlgUpdated();
		} else if( e.getSource() == imageView ) {
			selectedView = imageView.getSelectedIndex();
			owner.viewUpdated();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == selectZoom) {
			zoom = ((Number) selectZoom.getValue()).doubleValue();
			owner.viewUpdated();
		}
	}

	public ThresholdControlPanel getThreshold() {
		return threshold;
	}

	public ConnectRule getConnectRule() {
		return connectRule;
	}

	public void setZoom(double zoom) {
		selectZoom.setValue(zoom);
	}
}
