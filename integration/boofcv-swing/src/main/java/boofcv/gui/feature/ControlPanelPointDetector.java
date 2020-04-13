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

package boofcv.gui.feature;

import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.factory.feature.detect.selector.SelectLimitTypes;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.image.ImageGray;

import javax.swing.*;

/**
 * Control for detecting corners and dots/blobs.
 *
 * @author Peter Abeles
 */
public class ControlPanelPointDetector extends StandardAlgConfigPanel {
	public ConfigPointDetector config;

	private final JComboBox<String> comboType;
	private final JSpinner spinnerRadius;
	private final ControlPanelExtractor controlExtractor;
	private final JSpinner spinnerMaxFeatures;
	private final JComboBox<String> comboSelector;

	Listener listener;

	public ControlPanelPointDetector( ConfigPointDetector config , Listener listener )
	{
		this.config = config;
		this.listener = listener;

		spinnerRadius = spinner(config.scaleRadius,1.0,500.0,1.0);
		comboType = combo(config.type.ordinal(),PointDetectorTypes.values());
		controlExtractor = new ControlPanelExtractor(config.general,listener::handleChangePointDetector);
		spinnerMaxFeatures = spinner(config.general.maxFeatures,-1,9999,50);

		controlExtractor.setBorder(BorderFactory.createEmptyBorder());
		comboSelector = combo(config.general.selector.type.ordinal(), SelectLimitTypes.values());

		addLabeled(comboType,"Type","Type of corner or blob detector");
		addLabeled(spinnerRadius,"Scale/Radius","Specified size given to scale invariant descriptors");
		add(controlExtractor);
		addLabeled(spinnerMaxFeatures,"Max Features","Maximum features it will detect. <= 0 for no limit");
		addLabeled(comboSelector,  "Select",
				"Method used to select points when more have been detected than the maximum allowed");
	}

	public <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T,D> create( Class<T> imageType ) {
		return FactoryDetectPoint.create(config,imageType,null);
	}

	@Override
	public void controlChanged(final Object source) {
		if (source == comboType) {
			config.type = PointDetectorTypes.values()[comboType.getSelectedIndex()];
		} else if( source == spinnerMaxFeatures ) {
			config.general.maxFeatures = ((Number) spinnerMaxFeatures.getValue()).intValue();
		} else if( source == comboSelector) {
			config.general.selector.type = SelectLimitTypes.values()[comboSelector.getSelectedIndex()];
		} else if( source == spinnerRadius ) {
			config.scaleRadius = ((Number) spinnerRadius.getValue()).doubleValue();
		} else {
			throw new RuntimeException("Unknown source");
		}
		listener.handleChangePointDetector();
	}

	public interface Listener {
		void handleChangePointDetector();
	}
}
