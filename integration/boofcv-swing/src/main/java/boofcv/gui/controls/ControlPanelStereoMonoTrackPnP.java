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

package boofcv.gui.controls;

import boofcv.abst.sfm.d3.StereoVisualOdometry;
import boofcv.factory.sfm.ConfigStereoMonoTrackPnP;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.struct.image.ImageGray;

import javax.swing.*;
import java.awt.*;

/**
 * Control panel for {@link ConfigStereoMonoTrackPnP}
 *
 * @author Peter Abeles
 */
public class ControlPanelStereoMonoTrackPnP extends JPanel {

	ControlPanelVisOdomTrackPnP controlPnpDepth;
	ControlPanelPointTrackers controlTrackers;
	ControlPanelDisparitySparse controlDisparity;

	public ControlPanelStereoMonoTrackPnP( ConfigStereoMonoTrackPnP config, Listener listener ) {
		setLayout(new BorderLayout());
		controlPnpDepth = new ControlPanelVisOdomTrackPnP(listener::changedStereoMonoTrackPnP, config.scene);
		controlTrackers = new ControlPanelPointTrackers(listener::changedStereoMonoTrackPnP, config.tracker);
		controlDisparity = new ControlPanelDisparitySparse(listener::changedStereoMonoTrackPnP, config.disparity);

		var panelAlgControls = new JPanel(new BorderLayout());
		var tuningTabs = new JTabbedPane();
		tuningTabs.addTab("VO", panelAlgControls);
		tuningTabs.addTab("Tracker", controlTrackers);
		tuningTabs.addTab("Stereo", controlDisparity);

		panelAlgControls.add(BorderLayout.CENTER, controlPnpDepth);

		var panelTuning = new JPanel();
		panelTuning.setLayout(new BoxLayout(panelTuning, BoxLayout.Y_AXIS));
		panelTuning.add(tuningTabs);

		add(BorderLayout.CENTER, tuningTabs);
	}

	public ConfigStereoMonoTrackPnP createConfiguration() {
		var config = new ConfigStereoMonoTrackPnP();
		config.tracker.setTo(controlTrackers.createConfiguration());
		config.scene.setTo(controlPnpDepth.config);
		config.disparity.setTo(controlDisparity.config);
		return config;
	}

	public <T extends ImageGray<T>>
	StereoVisualOdometry<T> createVisOdom( Class<T> imageType ) {
		return FactoryVisualOdometry.stereoMonoPnP(createConfiguration(), imageType);
	}

	public interface Listener {
		void changedStereoMonoTrackPnP();
	}
}
