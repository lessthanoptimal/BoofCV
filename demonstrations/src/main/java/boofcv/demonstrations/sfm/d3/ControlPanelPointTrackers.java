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
	public static final String[] FAMILIES = {"KLT","DDA","Hybrid"};
	int selectedFamily = 0;

	JComboBox<String> cFamily = combo(selectedFamily, FAMILIES);
	JPanel mainPanel = new JPanel(new BorderLayout());

	ControlPanelPointTrackerKlt controlKlt;
	ControlPanelDdaTracker controlDda;
	ControlPanelHybridTracker controlHybrid;

	// The previously set component in mainPanel
	JComponent previous;

	Listener listener;

	public ControlPanelPointTrackers( Listener listener ,
									  ControlPanelPointTrackerKlt klt,
									  ControlPanelDdaTracker dda,
									  ControlPanelHybridTracker hybrid ) {
		setBorder(BorderFactory.createEmptyBorder());
		this.listener = listener;

		controlKlt = klt;
		controlDda = dda;
		controlHybrid = hybrid;

		int selected = selectedFamily;
		selectedFamily = -1; // so that it will update
		changeFamily(selected);

		addLabeled(cFamily,"Family","Which high level point tracker type");
		add(mainPanel);
	}

	public ControlPanelPointTrackers( Listener listener ) {
		this(listener,new ControlPanelPointTrackerKlt(listener::changePointTracker),
				new ControlPanelDdaTracker(listener::changePointTracker),
				new ControlPanelHybridTracker(listener::changePointTracker));
	}

	public <T extends ImageBase<T>>
	PointTracker<T> createTracker( ImageType<T> imageType ) {
		return switch (selectedFamily) {
			case 0 -> controlKlt.createTracker(imageType);
			case 1 -> controlDda.createTracker(imageType);
			case 2 -> controlHybrid.createTracker(imageType);
			default -> throw new RuntimeException("Not yet supported");
		};
	}

	private void changeFamily( int which ) {
		if( which == selectedFamily )
			return;
		if( previous != null )
			mainPanel.remove(previous);
		switch (which) {
			case 0 -> previous = controlKlt;
			case 1 -> previous = controlDda;
			case 2 -> previous = controlHybrid;
			default -> throw new RuntimeException("BUG");
		}
		selectedFamily = which;
		mainPanel.add(BorderLayout.CENTER,previous);
		mainPanel.repaint();
	}

	@Override
	public void controlChanged(final Object source) {
		if( source == cFamily ) {
			changeFamily(cFamily.getSelectedIndex());
		}
		listener.changePointTracker();
	}

	public interface Listener {
		void changePointTracker();
	}
}
