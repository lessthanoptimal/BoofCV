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

import boofcv.abst.feature.describe.ConfigTemplateDescribe;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;

import javax.swing.*;
import java.awt.*;

/**
 * Control panel for creating Detect-Describe-Associate style trackers
 *
 * @author Peter Abeles
 */
public class ControlPanelHybridTracker extends ControlPanelDetDescAssoc {
	Listener listener;

	int selectedSelection = 0;
	JComboBox<String> spinnerSelection = combo(selectedSelection,"KLT","Detect","Describe","Associate");

	ControlPanelPointTrackerKlt controlKlt;
	ConfigPKlt configKlt = new ConfigPKlt();

	// Container that specific controls are inserted into
	private JPanel controlPanel = new JPanel(new BorderLayout());
	private JPanel ddaPanel = new JPanel();

	public ControlPanelHybridTracker(Listener listener) {
		this.listener = listener;

		ddaPanel.setLayout(new BoxLayout(ddaPanel,BoxLayout.Y_AXIS));

		// Customize the tracker
		configKlt.toleranceFB = 3;
		configKlt.pruneClose = true;
		configKlt.templateRadius = 3;
		configKlt.pyramidLevels = ConfigDiscreteLevels.levels(4);
		configAssocGreedy.scoreRatioThreshold = 0.75;
		configPointDetector.general.threshold = 100;
		configPointDetector.general.radius = 4;
		configPointDetector.shiTomasi.radius = 4;

		selectedDetector = 2; // point
		selectedDescriptor = 4; // template
		configTemplate.type = ConfigTemplateDescribe.Type.NCC;

		initializeControlsGUI();
		updateActiveControls(selectedSelection);

		addLabeled(spinnerSelection,"Component","Select a component of the tracker to modify");
		add(controlPanel);
	}

	@Override
	public void initializeControlsGUI() {
		super.initializeControlsGUI();
		controlKlt = new ControlPanelPointTrackerKlt(()->listener.changedHybridTracker(),null,configKlt);
		controlKlt.setBorder(BorderFactory.createEmptyBorder());
	}

	private void updateActiveControls( int which ) {
		this.selectedSelection = which;
		controlPanel.removeAll();
		JPanel inside;
		if( which == 0 ) {
			inside = controlKlt;
		} else {
			inside = ddaPanel;
			ddaPanel.removeAll();
			switch ( which ) {
				case 1: ddaPanel.add(comboDetect); ddaPanel.add(getDetectorPanel()); break;
				case 2: ddaPanel.add(comboDescribe); ddaPanel.add(getDescriptorPanel()); break;
				case 3: ddaPanel.add(comboAssociate); ddaPanel.add(getAssociatePanel()); break;
			}
			ddaPanel.validate();
		}
		if( inside != null ) {
			controlPanel.add(BorderLayout.CENTER, inside);
		}
		controlPanel.validate();
		SwingUtilities.invokeLater(this::repaint);
	}

	public <T extends ImageBase<T>>
	PointTracker<T> createTracker(ImageType<T> imageType ) {
		Class inputType = imageType.getImageClass();

		DetectDescribePoint detDesc = createDetectDescribe(inputType);

		PointTracker<T> tracker = FactoryPointTracker.combined(detDesc,createAssociate(detDesc),
				configKlt,50,imageType.getImageClass());
		return tracker;
	}

	@Override
	protected void handleControlsUpdated() {
		listener.changedHybridTracker();
	}

	@Override
	public void controlChanged(final Object source) {
		System.out.println("Control changed");
		if( source == comboDetect ) {
			selectedDetector = comboDetect.getSelectedIndex();
		} else if( source == comboDescribe ) {
			selectedDescriptor = comboDescribe.getSelectedIndex();
		} else if( source == comboAssociate ) {
			selectedAssociate = comboAssociate.getSelectedIndex();
		}
		updateActiveControls(spinnerSelection.getSelectedIndex());
		listener.changedHybridTracker();
	}

	public interface Listener {
		void changedHybridTracker();
	}
}
