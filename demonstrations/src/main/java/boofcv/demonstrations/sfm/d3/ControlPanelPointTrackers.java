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

package boofcv.demonstrations.sfm.d3;

import boofcv.abst.tracker.PointTracker;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import javax.swing.*;
import java.awt.*;

/**
 * Control panel for selecting any {@link boofcv.abst.tracker.PointTracker}
 *
 * @author Peter Abeles
 */
public class ControlPanelPointTrackers extends StandardAlgConfigPanel {
	ConfigPointTracker.TrackerType selectedFamily = ConfigPointTracker.TrackerType.KLT;

	JComboBox<String> cFamily = combo(selectedFamily.ordinal(), ConfigPointTracker.TrackerType.values());
	JPanel mainPanel = new JPanel(new BorderLayout());

	ControlPanelPointTrackerKlt controlKlt;
	ControlPanelDdaTracker controlDda;
	ControlPanelHybridTracker controlHybrid;

	// The previously set component in mainPanel
	JComponent previous;

	Listener listener;

	public ControlPanelPointTrackers( Listener listener ,
									  ConfigPointTracker config )
	{
		setBorder(BorderFactory.createEmptyBorder());
		this.listener = listener;

		// TODO pass in copies since each control panel is independent
		controlKlt = config == null ? new ControlPanelPointTrackerKlt(listener::changePointTracker)
				: new ControlPanelPointTrackerKlt(listener::changePointTracker,config.detDesc.detectPoint,config.klt);
		controlDda = config == null ? new ControlPanelDdaTracker(listener::changePointTracker)
				: new ControlPanelDdaTracker(listener::changePointTracker,config.dda,config.detDesc,config.associate);
		controlHybrid = config == null ?  new ControlPanelHybridTracker(listener::changePointTracker)
				: new ControlPanelHybridTracker(listener::changePointTracker);

		controlDda.initializeControlsGUI();
		controlHybrid.initializeControlsGUI();

		ConfigPointTracker.TrackerType selected = selectedFamily;
		selectedFamily = null; // so that it will update
		changeFamily(selected);

		addLabeled(cFamily,"Family","Which high level point tracker type");
		add(mainPanel);
	}


	public <T extends ImageBase<T>>
	PointTracker<T> createTracker( ImageType<T> imageType ) {
		switch( selectedFamily ) {
			case KLT: return controlKlt.createTracker(imageType);
			case DDA: return controlDda.createTracker(imageType);
			case HYBRID: return controlHybrid.createTracker(imageType);
			default: throw new RuntimeException("Not yet supported");
		}
	}

	private void changeFamily( ConfigPointTracker.TrackerType which ) {
		if( which == selectedFamily )
			return;
		if( previous != null )
			mainPanel.remove(previous);
		switch( which ) {
			case KLT: previous = controlKlt; break;
			case DDA: previous = controlDda; break;
			case HYBRID: previous = controlHybrid; break;
			default: throw new RuntimeException("BUG");
		}
		selectedFamily = which;
		mainPanel.add(BorderLayout.CENTER,previous);
		mainPanel.repaint();
	}

	@Override
	public void controlChanged(final Object source) {
		if( source == cFamily ) {
			changeFamily(ConfigPointTracker.TrackerType.values()[cFamily.getSelectedIndex()]);
		}
		listener.changePointTracker();
	}

	public interface Listener {
		void changePointTracker();
	}
}
