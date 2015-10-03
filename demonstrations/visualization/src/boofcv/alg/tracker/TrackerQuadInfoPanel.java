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

package boofcv.alg.tracker;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Displays an info bar for {@link VideoTrackerObjectQuadApp} that can control the video's play back and
 * the tracking algorithm.
 *
 * @author Peter Abeles
 */
public class TrackerQuadInfoPanel extends StandardAlgConfigPanel implements ActionListener {

	JTextArea displayFPS;
	JTextArea displayTracking;

	JButton buttonPlay;

	JButton buttonSelect;

	JButton buttonReset;

	Listener listener;

	public TrackerQuadInfoPanel( Listener listener ) {
		this.listener = listener;

		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

		displayFPS = createTextInfo();
		displayTracking = createTextInfo();

		buttonPlay = new JButton("Start");
		buttonPlay.addActionListener(this);

		buttonSelect = new JButton("Select");
		buttonSelect.addActionListener(this);

		buttonReset = new JButton("Reset");
		buttonReset.addActionListener(this);

		addLabeledV(displayFPS,"Algorithm FPS:",this);
		addLabeledV(displayTracking, "Tracking:", this);
		addSeparator(200);
		add(buttonPlay);
		addAlignCenter(buttonPlay, this);
		addAlignCenter(buttonReset, this);
		addSeparator(200);
		addAlignCenter(buttonSelect, this);
	}

	private JTextArea createTextInfo() {
		JTextArea comp = new JTextArea(1,6);
		comp.setMaximumSize(comp.getPreferredSize());
		comp.setEditable(false);
		return comp;
	}

	public void setFPS( double fps ) {
		displayFPS.setText(String.format("%5.1f", fps));
	}

	public void setTracking( String text ) {
		displayTracking.setText(text);
	}

	public void setPlay( boolean playing ) {
		if( playing ) {
			buttonPlay.setText("Play");
		} else {
			buttonPlay.setText("Pause");
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == buttonPlay ) {
			listener.togglePause();
		} else if( e.getSource() == buttonSelect ) {
			listener.selectTarget();
		} else if( e.getSource() == buttonReset ) {
			listener.resetVideo();
		}
	}

	public static interface Listener {
		public void togglePause();

		public void selectTarget();

		public void resetVideo();
	}
}
