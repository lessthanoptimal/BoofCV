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

import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.tracker.ConfigTrackerDda;
import boofcv.abst.tracker.PointTracker;
import boofcv.factory.feature.associate.ConfigAssociate;
import boofcv.factory.feature.associate.ConfigAssociate.AssociationType;
import boofcv.factory.feature.describe.ConfigDescribeRegion;
import boofcv.factory.feature.detdesc.ConfigDetectDescribe;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import javax.swing.*;
import java.awt.*;

/**
 * Control panel for creating Detect-Describe-Associate style trackers
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ControlPanelDdaTracker extends ControlPanelDetDescAssocBase {

	private final JPanel controlPanel = new JPanel(new BorderLayout());
	private final Listener listener;

	public final ConfigTrackerDda configDDA;
	private ControlTracker controlTrackerDDA;

	public ControlPanelDdaTracker(Listener listener) {
		this.listener = listener;

		this.configDDA = new ConfigTrackerDda();

		configDetDesc.detectFastHessian.maxFeaturesPerScale = 400;
		configDetDesc.detectPoint.general.threshold = 100;
		configDetDesc.detectPoint.general.radius = 4;
		configDetDesc.detectPoint.shiTomasi.radius = 4;
		configAssociate.greedy.scoreRatioThreshold = 0.75;
	}

	public ControlPanelDdaTracker(Listener listener,
								  ConfigTrackerDda configTracker,
								  ConfigDetectDescribe detDesc ,
								  ConfigAssociate associate ) {
		this.listener = listener;
		this.configDDA = configTracker;

		configDetDesc = detDesc;
		configAssociate = associate;
	}

	@Override
	public void initializeControlsGUI() {
		super.associateWithPixels = true;
		super.initializeControlsGUI();
		controlTrackerDDA = new ControlTracker();
		controlTrackerDDA.setBorder(BorderFactory.createTitledBorder("Tracker"));
		add(controlTrackerDDA);
		addLabeled(comboDetect,"Detect","Point feature detectors");
		addLabeled(comboDescribe,"Describe","Point feature Descriptors");
		addLabeled(comboAssociate,"Associate","Feature association Approach");
		add(controlPanel);

		updateActiveControls(0);
	}

	@Override
	protected void handleControlsUpdated() {listener.changedPointTrackerDda();}

	public <T extends ImageBase<T>>
	PointTracker<T> createTracker(ImageType<T> imageType ) {
		Class inputType = imageType.getImageClass();

		DetectDescribePoint detDesc = createDetectDescribe(inputType);
		AssociateDescription2D associate = createAssociate2(detDesc);

		return FactoryPointTracker.dda(detDesc,associate, configDDA);
	}

	private void updateActiveControls( int which ) {
		controlPanel.removeAll();
		JPanel inside = switch (which) {
			case 0 -> getDetectorPanel();
			case 1 -> getDescriptorPanel();
			case 2 -> getAssociatePanel();
			default -> null;
		};
		if( inside != null )
			controlPanel.add(BorderLayout.CENTER,inside);
		controlPanel.validate();
		SwingUtilities.invokeLater(this::repaint);
	}

	@Override
	public void controlChanged(final Object source) {
		int which = -1;
		if (source == comboDetect) {
			configDetDesc.typeDetector = ConfigDetectInterestPoint.Type.values()[comboDetect.getSelectedIndex()];
			which = 0;
		} else if (source == comboDescribe) {
			configDetDesc.typeDescribe = ConfigDescribeRegion.Type.values()[comboDescribe.getSelectedIndex()];
			which = 1;
			// since BRIEF is binary only greedy association is supported
			if( configDetDesc.typeDescribe == ConfigDescribeRegion.Type.BRIEF ) {
				configAssociate.type = AssociationType.GREEDY;
				comboAssociate.setEnabled(false);
			} else {
				configAssociate.type = AssociationType.values()[comboAssociate.getSelectedIndex()];
				comboAssociate.setEnabled(true);
			}
		} else if (source == comboAssociate) {
			configAssociate.type = AssociationType.values()[comboAssociate.getSelectedIndex()];
			which = 2;
		}
		updateActiveControls(which);
		listener.changedPointTrackerDda();
	}

	public class ControlTracker extends StandardAlgConfigPanel {
		JSpinner spinnerMaxUnused = spinner(configDDA.maxInactiveTracks,0,5000,10);
		JCheckBox checkUpdate = checkbox("Update Description",configDDA.updateDescription);

		public ControlTracker() {
			addLabeled(spinnerMaxUnused,"Max Unused","Maximum number of unused/not visible tracks kept around");
			addAlignLeft(checkUpdate,"If the description is updated after every frame or not");
		}

		@Override
		public void controlChanged(final Object source) {
			if( source == spinnerMaxUnused ) {
				configDDA.maxInactiveTracks = (Integer)spinnerMaxUnused.getValue();
			} else if( source == checkUpdate ) {
				configDDA.updateDescription = checkUpdate.isSelected();
			} else {
				throw new RuntimeException("BUG");
			}
			listener.changedPointTrackerDda();
		}
	}

	public interface Listener {
		void changedPointTrackerDda();
	}
}
