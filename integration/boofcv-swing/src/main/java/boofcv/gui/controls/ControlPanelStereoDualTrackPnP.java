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
import boofcv.abst.tracker.PointTracker;
import boofcv.factory.feature.describe.ConfigDescribeRegion;
import boofcv.factory.sfm.ConfigStereoDualTrackPnP;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Control panel for {@link ConfigStereoDualTrackPnP}.
 *
 * @author Peter Abeles
 */
public class ControlPanelStereoDualTrackPnP extends JTabbedPane {

	ControlPanelVisOdomTrackPnP controlPnpDepth;
	ControlPanelPointTrackers controlTrackers;
	StereoControls controlStereo;

	final Listener listener;
	public final ConfigStereoDualTrackPnP config;

	public ControlPanelStereoDualTrackPnP( ConfigStereoDualTrackPnP config, Listener listener ) {
		setBorder(BorderFactory.createEmptyBorder());
		this.listener = listener;
		this.config = config;

		controlPnpDepth = new ControlPanelVisOdomTrackPnP(listener::changedStereoDualTrackPnP, config.scene);
		controlTrackers = new ControlPanelPointTrackers(listener::changedStereoDualTrackPnP, config.tracker);
		controlStereo = new StereoControls();

		var panelAlgControls = new JPanel(new BorderLayout());
		panelAlgControls.add(BorderLayout.CENTER, controlPnpDepth);

		addTab("VO", panelAlgControls);
		addTab("Tracker", controlTrackers);
		addTab("Stereo", controlStereo);
	}

	public <T extends ImageGray<T>>
	StereoVisualOdometry<T> createVisOdom( Class<T> imageType ) {

		PointTracker<T> trackerLeft = controlTrackers.createTracker(ImageType.single(imageType));
		PointTracker<T> trackerRight = controlTrackers.createTracker(ImageType.single(imageType));

		return FactoryVisualOdometry.stereoDualTrackerPnP(controlPnpDepth.config,
				trackerLeft, trackerRight, config, imageType);
	}

	public class StereoControls extends StandardAlgConfigPanel {
		JSpinner spinnerScaleRadius;
		JSpinner spinnerEpipolarTol;
		DescribeControl panelDescribe = new DescribeControl();

		public StereoControls() {
			setBorder(BorderFactory.createEmptyBorder());
			spinnerScaleRadius = spinner(config.stereoRadius, 0.0, 500.0, 1.0);
			spinnerEpipolarTol = spinner(config.epipolarTol, 0.0, 100.0, 1.0);

			addLabeled(spinnerScaleRadius, "Describe Radius", "Size of region covered by descriptor");
			addLabeled(spinnerEpipolarTol, "Epipolar Tol.", "Maximum distance away from the epipolar line two features can be");
			add(panelDescribe);
		}

		@Override
		public void controlChanged( final Object source ) {
			if (source == spinnerScaleRadius) {
				config.stereoRadius = ((Number)spinnerScaleRadius.getValue()).doubleValue();
			} else if (source == spinnerEpipolarTol) {
				config.epipolarTol = ((Number)spinnerEpipolarTol.getValue()).doubleValue();
			}
			listener.changedStereoDualTrackPnP();
		}
	}

	public class DescribeControl extends ControlPanelDetDescAssocBase {

		JPanel panelDescriptor = new JPanel(new BorderLayout());

		public DescribeControl() {
			configDetDesc.copyRefFrom(config.stereoDescribe);

			panelDescriptor.setBorder(BorderFactory.createTitledBorder("Descriptor"));

			setBorder(BorderFactory.createEmptyBorder());
			initializeControlsGUI();
			addLabeled(comboDescribe, "Type");
			add(panelDescriptor);

			handleDescriptorChanged();
		}

		private void handleDescriptorChanged() {
			config.stereoDescribe.type = configDetDesc.typeDescribe;
			panelDescriptor.removeAll();
			panelDescriptor.add(getDescriptorPanel(), BorderLayout.CENTER);
			panelDescriptor.invalidate();
			ControlPanelStereoDualTrackPnP.this.repaint();
		}

		@Override
		protected void handleControlsUpdated() {
			listener.changedStereoDualTrackPnP();
		}

		@Override
		public void actionPerformed( ActionEvent e ) {
			if (comboDescribe == e.getSource()) {
				configDetDesc.typeDescribe =
						ConfigDescribeRegion.Type.values()[comboDescribe.getSelectedIndex()];
				handleDescriptorChanged();
				listener.changedStereoDualTrackPnP();
			}
		}
	}

	public interface Listener {
		void changedStereoDualTrackPnP();
	}
}
