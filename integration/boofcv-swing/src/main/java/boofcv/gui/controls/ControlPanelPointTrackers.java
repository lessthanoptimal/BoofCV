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

import boofcv.abst.tracker.PointTracker;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Control panel for selecting any {@link boofcv.abst.tracker.PointTracker}
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ControlPanelPointTrackers extends StandardAlgConfigPanel {
	ConfigPointTracker.TrackerType selectedFamily = ConfigPointTracker.TrackerType.KLT;

	JComboBox<String> cFamily;
	JPanel mainPanel = new JPanel(new BorderLayout());

	ControlPanelPointTrackerKlt controlKlt;
	ControlPanelDdaTracker controlDda;
	ControlPanelHybridTracker controlHybrid;

	// The previously set component in mainPanel
	@Nullable JComponent previous;

	Listener listener;

	public ControlPanelPointTrackers( Listener listener, ConfigPointTracker config ) {
		setBorder(BorderFactory.createEmptyBorder());
		this.listener = listener;

		// TODO pass in copies since each control panel is independent
		controlKlt = config == null ? new ControlPanelPointTrackerKlt(listener::changePointTracker)
				: new ControlPanelPointTrackerKlt(listener::changePointTracker,
				config.detDesc.detectPoint.copy(), config.klt.copy());
		controlDda = config == null ? new ControlPanelDdaTracker(listener::changePointTracker)
				: new ControlPanelDdaTracker(listener::changePointTracker,
				config.dda.copy(), config.detDesc.copy(), config.associate.copy());
		controlHybrid = config == null ? new ControlPanelHybridTracker(listener::changePointTracker)
				: new ControlPanelHybridTracker(listener::changePointTracker, config.hybrid, config.klt.copy(),
				config.detDesc.copy(), config.associate.copy());
		if (config != null)
			selectedFamily = config.typeTracker;

		controlDda.initializeControlsGUI();
		controlHybrid.initializeControlsGUI();

		cFamily = combo(selectedFamily.ordinal(), (Object[])ConfigPointTracker.TrackerType.values());
		ConfigPointTracker.TrackerType selected = selectedFamily;
		changeFamily(selected, true);

		addLabeled(cFamily, "Family", "Which high level point tracker type");
		add(mainPanel);
	}

	public ConfigPointTracker createConfiguration() {
		var config = new ConfigPointTracker();
		config.typeTracker = selectedFamily;

		// only copy configurations that are active
		switch (selectedFamily) {
			case KLT -> {
				config.klt.setTo(controlKlt.configKlt);
				config.detDesc.typeDetector = ConfigDetectInterestPoint.Type.POINT;
				config.detDesc.detectPoint.setTo(Objects.requireNonNull(controlKlt.configDetect));
			}
			case DDA -> {
				config.dda.setTo(controlDda.configDDA);
				config.associate.setTo(controlDda.configAssociate);
				config.detDesc.setTo(controlDda.configDetDesc);
			}
			case HYBRID -> config.setTo(controlHybrid.createConfiguration());
			default -> throw new RuntimeException("Not yet supported");
		}
		return config;
	}

	public <T extends ImageBase<T>>
	PointTracker<T> createTracker( ImageType<T> imageType ) {
		return FactoryPointTracker.tracker(createConfiguration(), imageType.getImageClass(), null);
	}

	private void changeFamily( ConfigPointTracker.TrackerType which, boolean forced ) {
		if (!forced && which == selectedFamily)
			return;
		if (previous != null)
			mainPanel.remove(previous);
		previous = switch (which) {
			case KLT -> controlKlt;
			case DDA -> controlDda;
			case HYBRID -> controlHybrid;
			default -> throw new RuntimeException("BUG");
		};
		selectedFamily = which;
		mainPanel.add(BorderLayout.CENTER, previous);
		mainPanel.validate();
		mainPanel.repaint();
	}

	@Override
	public void controlChanged( final Object source ) {
		if (source == cFamily) {
			changeFamily(ConfigPointTracker.TrackerType.values()[cFamily.getSelectedIndex()], false);
		}
		listener.changePointTracker();
	}

	public interface Listener {
		void changePointTracker();
	}
}
