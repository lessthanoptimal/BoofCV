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
	private final JSpinner spinnerKernel;
	private final JCheckBox checkWeighted;
	private final JCheckBox checkIntensity;
	private final JSpinner spinnerRadius;
	private final ControlPanelExtractor controlExtractor;
	private final JSpinner spinnerMaxFeatures;
	private final JComboBox<String> comboSelector;

	Listener listener;

	public ControlPanelPointDetector( ConfigPointDetector config , Listener listener )
	{
		this.config = config;
		this.listener = listener;

		spinnerKernel = spinner(1,1,1000,1);
		checkWeighted = checkbox("Weighted",false,"Gaussian weighted or block");
		checkIntensity = checkbox("Non-Max",true,"Use non-max suppression or not");
		spinnerRadius = spinner(config.scaleRadius,1.0,500.0,1.0);
		comboType = combo(config.type.ordinal(),PointDetectorTypes.FIRST_ONLY);
		controlExtractor = new ControlPanelExtractor(config.general,listener::handleChangePointDetector);
		spinnerMaxFeatures = spinner(config.general.maxFeatures,-1,9999,50);

		setKernelSize();
		setWeighted();
		setIntensity();

		controlExtractor.setBorder(BorderFactory.createEmptyBorder());
		comboSelector = combo(config.general.selector.type.ordinal(), SelectLimitTypes.values());

		addLabeled(comboType,"Type","Type of corner or blob detector");
		addLabeled(spinnerKernel,"Kernel","Radius of convolutional kernel");
		add(createHorizontalPanel(checkWeighted,checkIntensity));
		addLabeled(spinnerRadius,"Scale/Radius","Specified size given to scale invariant descriptors");
		add(controlExtractor);
		addLabeled(spinnerMaxFeatures,"Max Features","Maximum features it will detect. <= 0 for no limit");
		addLabeled(comboSelector,  "Select",
				"Method used to select points when more have been detected than the maximum allowed");
	}

	private void setKernelSize() {
		int radius = -1;
		switch( config.type ) {
			case SHI_TOMASI: radius = config.shiTomasi.radius; break;
			case HARRIS: radius = config.harris.radius; break;
		}
		spinnerKernel.removeChangeListener(this);
		if( radius == -1 ) {
			spinnerKernel.setEnabled(false);
		} else {
			spinnerKernel.setEnabled(true);
			spinnerKernel.setValue(radius);
		}
		spinnerKernel.addChangeListener(this);
	}

	private void setWeighted() {
		checkWeighted.removeActionListener(this);
		switch (config.type) {
			case SHI_TOMASI -> {
				checkWeighted.setEnabled(true);
				checkWeighted.setSelected(config.shiTomasi.weighted);
			}
			case HARRIS -> {
				checkWeighted.setEnabled(true);
				checkWeighted.setSelected(config.harris.weighted);
			}
			default -> checkWeighted.setEnabled(false);
		}
		checkWeighted.addActionListener(this);
	}

	private void setIntensity() {
		checkIntensity.removeActionListener(this);
		boolean isFast = config.type == PointDetectorTypes.FAST;
		checkIntensity.setEnabled(isFast);
		if( isFast ) {
			checkIntensity.setSelected(config.fast.nonMax);
		}

		checkIntensity.addActionListener(this);
	}

	public <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T,D> create( Class<T> imageType ) {
		return FactoryDetectPoint.create(config,imageType,null);
	}

	@Override
	public void controlChanged(final Object source) {
		if (source == comboType) {
			config.type = PointDetectorTypes.FIRST_ONLY[comboType.getSelectedIndex()];
			setKernelSize();
			setWeighted();
			setIntensity();
		} else if( source == spinnerMaxFeatures ) {
			config.general.maxFeatures = ((Number) spinnerMaxFeatures.getValue()).intValue();
		} else if( source == comboSelector) {
			config.general.selector.type = SelectLimitTypes.values()[comboSelector.getSelectedIndex()];
		} else if( source == spinnerRadius ) {
			config.scaleRadius = ((Number) spinnerRadius.getValue()).doubleValue();
		} else if( source == spinnerKernel ) {
			switch (config.type) {
				case SHI_TOMASI -> config.shiTomasi.radius = ((Number) spinnerKernel.getValue()).intValue();
				case HARRIS -> config.harris.radius = ((Number) spinnerKernel.getValue()).intValue();
			}
		} else if( source == checkWeighted ) {
			switch (config.type) {
				case SHI_TOMASI -> config.shiTomasi.weighted = checkWeighted.isSelected();
				case HARRIS -> config.harris.weighted = checkWeighted.isSelected();
			}
		} else if( source == checkIntensity ) {
			if( config.type == PointDetectorTypes.FAST ) {
				config.fast.nonMax = checkWeighted.isSelected();
			}
		} else {
			throw new RuntimeException("Unknown source");
		}
		listener.handleChangePointDetector();
	}

	public interface Listener {
		void handleChangePointDetector();
	}
}
