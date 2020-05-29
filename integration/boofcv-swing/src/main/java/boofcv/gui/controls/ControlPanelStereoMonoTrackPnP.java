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

package boofcv.gui.controls;

import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.sfm.d3.StereoVisualOdometry;
import boofcv.abst.tracker.PointTracker;
import boofcv.factory.sfm.ConfigStereoMonoTrackPnP;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

import javax.swing.*;
import java.awt.*;

/**
 * @author Peter Abeles
 */
public class ControlPanelStereoMonoTrackPnP extends JPanel {

	ControlPanelVisOdomTrackPnP controlPnpDepth;
	ControlPanelPointTrackers controlTrackers;
	ControlPanelDisparitySparse controlDisparity;

	public ControlPanelStereoMonoTrackPnP(ConfigStereoMonoTrackPnP config, Listener listener ) {
		setLayout(new BorderLayout());
		controlPnpDepth = new ControlPanelVisOdomTrackPnP(listener::changedStereoMonoTrackPnP, config.scene);
		controlTrackers = new ControlPanelPointTrackers(listener::changedStereoMonoTrackPnP,config.tracker);
		controlDisparity = new ControlPanelDisparitySparse(listener::changedStereoMonoTrackPnP, config.disparity);

		var panelAlgControls = new JPanel(new BorderLayout());
		var tuningTabs = new JTabbedPane();
		tuningTabs.addTab("VO",panelAlgControls);
		tuningTabs.addTab("Tracker",controlTrackers);
		tuningTabs.addTab("Stereo",controlDisparity);

		panelAlgControls.add(BorderLayout.CENTER, controlPnpDepth);

		var panelTuning = new JPanel();
		panelTuning.setLayout(new BoxLayout(panelTuning,BoxLayout.Y_AXIS));
		panelTuning.add(tuningTabs);

		add(BorderLayout.CENTER, tuningTabs);
	}

	public <T extends ImageGray<T>>
	StereoVisualOdometry<T> createVisOdom(Class<T> imageType ) {

		PointTracker<T> tracker = controlTrackers.createTracker(ImageType.single(imageType));
		StereoDisparitySparse<T> disparity = controlDisparity.createAlgorithm(imageType);

		return FactoryVisualOdometry.stereoMonoPnP(controlPnpDepth.config,disparity,tracker,imageType);
	}

	public interface Listener {
		void changedStereoMonoTrackPnP();
	}
}
