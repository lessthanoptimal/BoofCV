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

package boofcv.alg.sfm.d2;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Provides info related to image motion estimation and distortion
 *
 * @author Peter Abeles
 */
public class ImageMotionInfoPanel extends StandardAlgConfigPanel implements ItemListener, ActionListener {

	JButton resetButton;
	JCheckBox showView;
	JCheckBox showInliers;
	JCheckBox showAll;
	JTextArea displayFPS;
	JTextArea displayNumKeyFrames;
	JTextArea displayNumTracks;
	JTextArea displayNumInliers;

	boolean setShowView = true;
	boolean setShowInliers = false;
	boolean setShowAll = false;
	boolean shouldReset = false;

	public ImageMotionInfoPanel() {
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

		resetButton = new JButton("Reset");
		resetButton.addActionListener(this);

		showView = new JCheckBox("View");
		showView.addItemListener(this);
		showView.setSelected(setShowView);
		showAll = new JCheckBox("Show All");
		showAll.addItemListener(this);
		showAll.setSelected(setShowAll);
		showInliers = new JCheckBox("Show Inliers");
		showInliers.addItemListener(this);
		showInliers.setSelected(setShowInliers);

		displayFPS = createTextInfo();
		displayNumKeyFrames = createTextInfo();
		displayNumTracks = createTextInfo();
		displayNumInliers = createTextInfo();

		addAlignLeft(resetButton, this);
		addAlignLeft(showView, this);
		addAlignLeft(showAll, this);
		addAlignLeft(showInliers, this);
		addSeparator(200);
		addLabeled(displayFPS,"Algorithm FPS:",this);
		addLabeled(displayNumKeyFrames,"Resets:",this);
		addLabeled(displayNumTracks,"Tracks:",this);
		addLabeled(displayNumInliers,"Inliers:",this);

		setPreferredSize(new Dimension(175,300));
	}

	private JTextArea createTextInfo() {
		JTextArea comp = new JTextArea(1,6);
		comp.setMaximumSize(comp.getPreferredSize());
		comp.setEditable(false);
		return comp;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == resetButton ) {
			shouldReset = true;
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if( e.getItem() == showInliers) {
			setShowInliers = e.getStateChange() != ItemEvent.DESELECTED;
		} else if( e.getItem() == showAll) {
			setShowAll = e.getStateChange() != ItemEvent.DESELECTED;
		} else if( e.getItem() == showView ) {
			setShowView = e.getStateChange() != ItemEvent.DESELECTED;
		}
	}

	public void setFPS( double fps ) {
		displayFPS.setText(String.format("%5.1f", fps));
	}

	public void setKeyFrames(int totalFaults) {
		displayNumKeyFrames.setText(String.format("%5d", totalFaults));
	}

	public void setNumTracks( int totalTracks ) {
		displayNumTracks.setText(String.format("%5d",totalTracks));
	}

	public void setNumInliers(int totalInliers) {
		displayNumInliers.setText(String.format("%5d",totalInliers));
	}

	public boolean getShowInliers() {
		return setShowInliers;
	}

	public boolean getShowAll() {
		return setShowAll;
	}

	public boolean getShowView() {
		return setShowView;
	}

	public boolean resetRequested() {
		if( shouldReset ) {
			shouldReset = false;
			return true;
		}
		return false;
	}
}
