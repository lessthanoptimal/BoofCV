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

import boofcv.abst.feature.associate.AssociateDescTo2D;
import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.tracker.ConfigTrackerDda;
import boofcv.abst.tracker.PointTracker;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import javax.swing.*;
import java.awt.*;

/**
 * Control panel for creating Detect-Describe-Associate style trackers
 *
 * @author Peter Abeles
 */
public class ControlPanelDdaTracker extends ControlPanelDetDescAssoc {

	private final JPanel controlPanel = new JPanel(new BorderLayout());
	private final Listener listener;

	public ControlPanelDdaTracker(Listener listener) {
		this.listener = listener;

		configFastHessian.maxFeaturesPerScale = 400;
		configAssocGreedy.scoreRatioThreshold = 0.75;
		configPointDetector.general.threshold = 100;
		configPointDetector.general.radius = 4;
		configPointDetector.shiTomasi.radius = 4;

		initializeControlsGUI();

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

		ConfigTrackerDda configDDA = new ConfigTrackerDda();

		DetectDescribePoint detDesc = createDetectDescribe(inputType);
		AssociateDescription2D associate = new AssociateDescTo2D(createAssociate(detDesc));

		return FactoryPointTracker.dda(detDesc,associate, configDDA);
	}

	private void updateActiveControls( int which ) {
		controlPanel.removeAll();
		JPanel inside = switch( which ) {
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
			selectedDetector = comboDetect.getSelectedIndex();
			which = 0;
		} else if (source == comboDescribe) {
			selectedDescriptor = comboDescribe.getSelectedIndex();
			which = 1;
		} else if (source == comboAssociate) {
			selectedAssociate = comboAssociate.getSelectedIndex();
			which = 2;
		}
		updateActiveControls(which);
		listener.changedPointTrackerDda();
	}

	public interface Listener {
		void changedPointTrackerDda();
	}
}
