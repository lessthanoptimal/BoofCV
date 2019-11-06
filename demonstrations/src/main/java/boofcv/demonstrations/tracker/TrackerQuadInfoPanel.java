/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.gui.BoofSwingUtil;
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

	public boolean autoStart = true;
	private double maxFPS = 30;

	protected JTextArea displayFPS = createTextInfo();
	protected JTextArea displayTracking = createTextInfo();
	protected JTextArea imageSizeLabel = createTextInfo();

	JSpinner selectMaxVideoFPS = spinner(maxFPS,0,100,5);
	JCheckBox checkAutoStart = checkbox("Auto Start",autoStart);
	JButton buttonPlay;

	Listener listener;

	public TrackerQuadInfoPanel( Listener listener  ) {
		this.listener = listener;

		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

		buttonPlay = new JButton("Start");
		buttonPlay.addActionListener(this);

		addLabeledV(imageSizeLabel,"Video Size");
		addLabeledV(displayFPS,"Algorithm FPS");
		addLabeledV(selectMaxVideoFPS,"Max Video FPS");
		addLabeledV(displayTracking, "Tracking");
		addSeparator(200);
		add(buttonPlay);
		addAlignCenter(checkAutoStart);
		addAlignCenter(buttonPlay);

		listener.setMaxFPS(maxFPS);
	}

	private JTextArea createTextInfo() {

		JTextArea comp = new JTextArea(1,7);
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
		} else if( e.getSource() == checkAutoStart ) {
			this.autoStart = checkAutoStart.isSelected();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == selectMaxVideoFPS ) {
			maxFPS = ((Number) selectMaxVideoFPS.getValue()).doubleValue();
			listener.setMaxFPS(maxFPS);
		}
	}

	public void setImageSize( final int width , final int height ) {
		BoofSwingUtil.invokeNowOrLater(() -> imageSizeLabel.setText(width+" x "+height));
	}

	public double getMaxFPS() {
		return maxFPS;
	}

	public interface Listener {
		void togglePause();

		void setMaxFPS( double fps );
	}
}
