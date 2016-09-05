/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.tracker;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Displays an info bar for {@link VideoTrackerObjectQuadApp} that can control the video's play back and
 * the tracking algorithm.
 *
 * @author Peter Abeles
 */
public class TrackerQuadInfoPanel extends StandardAlgConfigPanel implements ActionListener, ChangeListener {

	JTextArea displayFPS;
	JTextArea displayTracking;

	JSpinner selectMaxVideoFPS;

	JButton buttonPlay;


	JButton buttonReplay;

	Listener listener;
	private double maxFPS = 30;

	public TrackerQuadInfoPanel( Listener listener  ) {
		this.listener = listener;

		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

		displayFPS = createTextInfo();
		displayTracking = createTextInfo();

		buttonPlay = new JButton("Start");
		buttonPlay.addActionListener(this);


		buttonReplay = new JButton("Replay");
		buttonReplay.addActionListener(this);

		selectMaxVideoFPS = new JSpinner(new SpinnerNumberModel(maxFPS,0,100,5));
		selectMaxVideoFPS.addChangeListener(this);
		selectMaxVideoFPS.setMaximumSize(selectMaxVideoFPS.getPreferredSize());

		addLabeledV(displayFPS,"Algorithm FPS:",this);
		addLabeledV(selectMaxVideoFPS,"Max Video FPS:",this);
		addLabeledV(displayTracking, "Tracking:", this);
		addSeparator(200);
		add(buttonPlay);
		addAlignCenter(buttonPlay, this);
		addAlignCenter(buttonReplay, this);

		listener.setMaxFPS(maxFPS);
	}

	private JTextArea createTextInfo() {
		JTextArea comp = new JTextArea(1,6);
		comp.setMaximumSize(comp.getPreferredSize());
		comp.setEditable(false);
		return comp;
	}

	public void setFPS( final double fps ) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				displayFPS.setText(String.format("%5.1f", fps));
			}
		});
	}

	public void setTracking( final String text ) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				displayTracking.setText(text);
			}
		});
	}

	public void setPlay( final boolean playing ) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if( playing ) {
					buttonPlay.setText("Playing");
				} else {
					buttonPlay.setText("Paused");
				}
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == buttonPlay ) {
			listener.togglePause();
		} else if( e.getSource() == buttonReplay) {
			listener.replayVideo();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == selectMaxVideoFPS ) {
			maxFPS = ((Number) selectMaxVideoFPS.getValue()).doubleValue();
			listener.setMaxFPS(maxFPS);
		}
	}

	public double getMaxFPS() {
		return maxFPS;
	}

	public static interface Listener {
		public void togglePause();

		public void replayVideo();

		void setMaxFPS( double fps );
	}
}
