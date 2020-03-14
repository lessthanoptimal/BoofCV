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

package boofcv.demonstrations.distort;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Panel which lets you select the shape and FOV for a pinhole camera
 *
 * @author Peter Abeles
 */
// TODO add a graphic to control it
	// TODO some how enforce constraints on each angle's range?
public class RotationPanel extends StandardAlgConfigPanel
	implements ChangeListener, ActionListener
{

	JSpinner selectPitch;
	JSpinner selectYaw;
	JSpinner selectRoll;
	JButton home = new JButton("Home");

	double pitch,yaw,roll; // angle in degrees

	Listener listener;


	public RotationPanel(double pitch , double yaw , double roll,
						 Listener listener ) {
		this.pitch = pitch;
		this.yaw = yaw;
		this.roll = roll;
		this.listener = listener;

		selectPitch = new JSpinner(new SpinnerNumberModel(pitch, -90, 90, 2));
		selectPitch.setMaximumSize(selectPitch.getPreferredSize());
		selectPitch.addChangeListener(this);

		selectYaw = new JSpinner(new SpinnerNumberModel(yaw, -180, 180, 2));
		selectYaw.setMaximumSize(selectYaw.getPreferredSize());
		selectYaw.addChangeListener(this);

		selectRoll = new JSpinner(new SpinnerNumberModel(roll, -180, 180, 2));
		selectRoll.setMaximumSize(selectRoll.getPreferredSize());
		selectRoll.addChangeListener(this);

		home.addActionListener(this);

		addLabeled(selectPitch,"Pitch: ");
		addLabeled(selectYaw,  "Yaw: ");
		addLabeled(selectRoll, "Roll: ");
		addAlignCenter(home,this);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( selectYaw == e.getSource() ) {
			yaw = ((Number) selectYaw.getValue()).doubleValue();
		} else if( selectRoll == e.getSource() ) {
			roll = ((Number) selectRoll.getValue()).doubleValue();
		} else if( selectPitch == e.getSource() ) {
			pitch =  ((Number) selectPitch.getValue()).doubleValue();
		} else {
			return;
		}
		listener.updatedOrientation(pitch,yaw,roll);

	}

	public void setOrientation( final double pitch , final double yaw , final double roll ) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				selectPitch.removeChangeListener(RotationPanel.this);
				selectYaw.removeChangeListener(RotationPanel.this);
				selectRoll.removeChangeListener(RotationPanel.this);

				selectPitch.setValue(pitch);
				selectYaw.setValue(yaw);
				selectRoll.setValue(roll);

				selectPitch.addChangeListener(RotationPanel.this);
				selectYaw.addChangeListener(RotationPanel.this);
				selectRoll.addChangeListener(RotationPanel.this);
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == home ) {
			yaw = pitch = roll = 0;
			selectYaw.setValue(0);
			selectRoll.setValue(0);
			selectPitch.setValue(0);
			listener.updatedOrientation(pitch,yaw,roll);
		}
	}

	public interface Listener {
		void updatedOrientation( double pitch , double yaw , double roll );
	}
}
