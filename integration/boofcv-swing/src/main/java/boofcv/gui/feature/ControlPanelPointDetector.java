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

import boofcv.abst.feature.detect.interest.*;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.image.ImageGray;

import javax.swing.*;

/**
 * Control for detecting corners and dots/blobs.
 *
 * @author Peter Abeles
 */
public class ControlPanelPointDetector extends StandardAlgConfigPanel {
	public final ConfigGeneralDetector configGeneral = new ConfigGeneralDetector();
	public final ConfigHarrisCorner configHarris = new ConfigHarrisCorner();
	public final ConfigShiTomasi configShiTomasi = new ConfigShiTomasi();
	public final ConfigFastCorner configFast = new ConfigFastCorner();
	public PointDetectorTypes type;

	private final JComboBox<String> comboType;
	private final ControlPanelExtractor controlExtractor;
	private final JSpinner spinnerMaxFeatures;

	Listener listener;

	public ControlPanelPointDetector( int maxFeatures , PointDetectorTypes type , Listener listener )
	{
		this.configGeneral.maxFeatures = maxFeatures;
		this.type = type;
		this.listener = listener;

		comboType = combo(type.ordinal(),PointDetectorTypes.values());
		controlExtractor = new ControlPanelExtractor(configGeneral,listener::handleChangePointDetector);
		spinnerMaxFeatures = spinner(configGeneral.maxFeatures,-1,9999,50);

		controlExtractor.setBorder(BorderFactory.createEmptyBorder());

		addLabeled(comboType,"Type","Type of corner or blob detector");
		add(controlExtractor);
		addLabeled(spinnerMaxFeatures,"Max Features","Maximum features it will detect. <= 0 for no limit");
	}

	public <T extends ImageGray<T>, D extends ImageGray<D>>
	GeneralFeatureDetector<T,D> create( Class<T> imageType ) {
		Class<D> derivType = GImageDerivativeOps.getDerivativeType(imageType);

		configGeneral.detectMaximums = true;
		configGeneral.detectMinimums = false;
		switch( type ) {
			case FAST:
			case LAPLACIAN: configGeneral.detectMinimums = true; break;
		}

		switch( type ) {
			case HARRIS: return FactoryDetectPoint.createHarris(configGeneral,configHarris,derivType);
			case SHI_TOMASI: return FactoryDetectPoint.createShiTomasi(configGeneral,configShiTomasi,derivType);
			case FAST: return FactoryDetectPoint.createFast(configGeneral, configFast, imageType);
			case KIT_ROS: return FactoryDetectPoint.createKitRos(configGeneral,derivType);
			case MEDIUM: return FactoryDetectPoint.createMedian(configGeneral,imageType);
			case DETERMINANT: return FactoryDetectPoint.createHessianDeriv(HessianBlobIntensity.Type.DETERMINANT,configGeneral,derivType);
			case LAPLACIAN: return FactoryDetectPoint.createHessianDeriv(HessianBlobIntensity.Type.TRACE,configGeneral,derivType);
			default: throw new IllegalArgumentException("Unknown type "+type);
		}
	}

	@Override
	public void controlChanged(final Object source) {
		if (source == comboType) {
			type = PointDetectorTypes.values()[comboType.getSelectedIndex()];
		} else if( source == spinnerMaxFeatures ) {
			configGeneral.maxFeatures = ((Number)spinnerMaxFeatures.getValue()).intValue();
		} else {
			throw new RuntimeException("Unknown source");
		}
		listener.handleChangePointDetector();
	}

	public interface Listener {
		void handleChangePointDetector();
	}
}
