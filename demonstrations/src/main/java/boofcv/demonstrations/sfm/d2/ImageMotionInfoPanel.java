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

package boofcv.demonstrations.sfm.d2;

import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.controls.ControlPanelPointTrackers;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;

/**
 * Provides info related to image motion estimation and distortion
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ImageMotionInfoPanel extends StandardAlgConfigPanel {

	/** User requested that the algorithm's configuration be changed */
	@Nullable AlgorithmListener listenerAlg;

	/** User requested that the visualization is changed */
	@Nullable VisualizationListener listenerVis;

	final ConfigPointTracker configTracker = new ConfigPointTracker();

	int motionModels = 0;

	@Getter boolean showView = true;
	@Getter boolean showInliers = false;
	@Getter boolean showAll = false;
	boolean shouldReset = false;

	JButton resetButton;
	JCheckBox checkShowView = checkbox("Show View", showView);
	JComboBox<String> spinnerModels = combo(0, "Affine", "Homography");
	JCheckBox checkShowInliers = checkbox("Show Inliers", showInliers);
	JCheckBox checkShowAll = checkbox("Show All", showAll);
	JTextArea displayPeriodMS;
	JTextArea displayFrameID;
	JTextArea displayNumKeyFrames;
	JTextArea displayNumTracks;
	JTextArea displayNumInliers;
	ControlPanelPointTrackers panelTrackers;

	public ImageMotionInfoPanel() {}

	public void initializeGui() {
		// Declare the panel here so that the user can do complex modifications to the config first
		panelTrackers = new ControlPanelPointTrackers(() -> {
			if (listenerAlg != null)
				listenerAlg.handleUserChangeAlgorithm();
		}, configTracker);

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

		resetButton = new JButton("Reset");
		resetButton.addActionListener(this);

		displayPeriodMS = createTextInfo();
		displayFrameID = createTextInfo();
		displayNumKeyFrames = createTextInfo();
		displayNumTracks = createTextInfo();
		displayNumInliers = createTextInfo();

		addLabeled(displayPeriodMS, "Period (ms)");
		addLabeled(displayFrameID, "Frame");
		addAlignLeft(checkShowView);
		addLabeled(spinnerModels, "Models");
		addAlignLeft(checkShowAll);
		addAlignLeft(checkShowInliers);
		addSeparator(200);
		addLabeled(displayNumKeyFrames, "Resets:");
		addLabeled(displayNumTracks, "Tracks:");
		addLabeled(displayNumInliers, "Inliers:");
		add(panelTrackers);
		addAlignLeft(resetButton);

		setPreferredSize(new Dimension(200, 650));
		setMaximumSize(getPreferredSize());
	}

	private JTextArea createTextInfo() {
		JTextArea comp = new JTextArea(1, 6);
		comp.setMaximumSize(comp.getPreferredSize());
		comp.setEditable(false);
		return comp;
	}

	@Override public void controlChanged( Object source ) {
		if (source == resetButton) {
			shouldReset = true;
		} else if (source == spinnerModels) {
			motionModels = spinnerModels.getSelectedIndex();
			if (listenerAlg != null)
				listenerAlg.handleUserChangeAlgorithm();
		} else if (source == checkShowInliers) {
			showInliers = checkShowInliers.isSelected();
			if (listenerVis != null)
				listenerVis.handleUserChangeVisualization();
		} else if (source == checkShowAll) {
			showAll = checkShowAll.isSelected();
			if (listenerVis != null)
				listenerVis.handleUserChangeVisualization();
		} else if (source == checkShowView) {
			showView = checkShowView.isSelected();
			if (listenerVis != null)
				listenerVis.handleUserChangeVisualization();
		}
	}

	public void setPeriodMS( double period ) {
		displayPeriodMS.setText(String.format("%5.1f", period));
	}

	public void setFrameID( long frame ) {
		displayFrameID.setText(String.format("%5d", frame));
	}

	public void setKeyFrames( int totalFaults ) {
		displayNumKeyFrames.setText(String.format("%5d", totalFaults));
	}

	public void setNumTracks( int totalTracks ) {
		displayNumTracks.setText(String.format("%5d", totalTracks));
	}

	public void setNumInliers( int totalInliers ) {
		displayNumInliers.setText(String.format("%5d", totalInliers));
	}

	public boolean resetRequested() {
		if (shouldReset) {
			shouldReset = false;
			return true;
		}
		return false;
	}

	public interface AlgorithmListener {
		void handleUserChangeAlgorithm();
	}

	public interface VisualizationListener {
		void handleUserChangeVisualization();
	}
}
