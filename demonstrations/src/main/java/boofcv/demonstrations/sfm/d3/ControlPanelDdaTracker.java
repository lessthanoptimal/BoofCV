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
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.tracker.ConfigTrackerDda;
import boofcv.abst.tracker.PointTracker;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import javax.swing.*;

/**
 * Control panel for creating Detect-Describe-Associate style trackers
 *
 * @author Peter Abeles
 */
public class ControlPanelDdaTracker extends StandardAlgConfigPanel {
	private static final String[] DETECT_TYPES = {"SIFT","SURF","Shi-Tomasi","Harris","FAST","Laplace"};
	private static final String[] DESCRIBE_TYPES = {"SIFT","SURF","BRIEF","Pixel","NCC"};
	private static final String[] ASSOCIATE_TYPES = {"Brute","KD-TREE","Forest"};

	int selectedDetect = 1;
	int selectedDescribe = 1;
	int selectedAssociate = 0;

	JComboBox<String> comboDetect    = combo(selectedDetect,DETECT_TYPES);
	JComboBox<String> comboDescribe  = combo(selectedDescribe,DESCRIBE_TYPES);
	JComboBox<String> comboAssociate = combo(selectedAssociate,ASSOCIATE_TYPES);

	Listener listener;
	public ControlPanelDdaTracker(Listener listener) {
		setBorder(BorderFactory.createEmptyBorder());
		this.listener = listener;

		addLabeled(comboDetect,"Detect","Point feature detectors");
		addLabeled(comboDescribe,"Describe","Point feature Descriptors");
		addLabeled(comboAssociate,"Associate","Feature association Approach");
	}

	public <T extends ImageBase<T>>
	PointTracker<T> createTracker(ImageType<T> imageType ) {
		Class inputType = imageType.getImageClass();

		ConfigTrackerDda configDDA = new ConfigTrackerDda();

		DetectDescribePoint detDesc;

		// TODO use enums for and not a string array

		// handle the special case where they match
		if( selectedDetect == selectedDescribe && selectedDetect < 2 ) {
			if( selectedDetect == 0 )
				detDesc = FactoryDetectDescribe.sift(null);
			else if( selectedDetect == 1 )
				detDesc = FactoryDetectDescribe.surfFast(null,null,null,inputType);
			else
				throw new RuntimeException("BUG");
		} else {
			throw new RuntimeException("Not supported");
		}

		ScoreAssociation scorer = FactoryAssociation.defaultScore(detDesc.getDescriptionType());
		AssociateDescription2D associate = new AssociateDescTo2D(
				FactoryAssociation.greedy(scorer,Double.MAX_VALUE,true));

		return FactoryPointTracker.dda(detDesc,associate, configDDA);
	}

	@Override
	public void controlChanged(final Object source) {
		if (source == comboDetect) {

		} else if (source == comboDescribe) {

		} else if (source == comboAssociate) {

		}
		listener.changedPointTrackerDda();
	}

	public interface Listener {
		void changedPointTrackerDda();
	}
}
