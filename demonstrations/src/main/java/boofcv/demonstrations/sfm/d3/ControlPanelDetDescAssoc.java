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

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.describe.*;
import boofcv.abst.feature.detdesc.ConfigCompleteSift;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigSiftDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.associate.ConfigAssociateGreedy;
import boofcv.factory.feature.associate.ConfigAssociateNearestNeighbor;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientation;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.feature.*;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

import javax.swing.*;

/**
 * Contains controls for all the usual detectors, descriptors, and associations. Mostly contains boiler plate
 * and leaves the final visualization step to the implementing class.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public abstract class ControlPanelDetDescAssoc extends StandardAlgConfigPanel  {
	protected static final String[] DETECT_TYPES = {"Fast-Hessian","SIFT","Point"};
	protected static final String[] DESCRIBE_TYPES = {"SURF-Fast","SURF-Stable","SIFT","BRIEF","Template"};
	protected static final String[] ASSOCIATE_TYPES = {"Brute","KD-TREE","Forest"};

	protected int selectedDetector = 0;
	protected int selectedDescriptor = 0;
	protected int selectedAssociate = 0;

	protected JComboBox<String> comboDetect;
	protected JComboBox<String> comboDescribe;
	protected JComboBox<String> comboAssociate;

	// Configurations. Modify these before calling initializeControlsGUI
	public ConfigFastHessian configFastHessian = new ConfigFastHessian();
	public ConfigSiftDetector configSiftDetector = new ConfigSiftDetector();
	public ConfigSiftScaleSpace configSiftScaleSpace = new ConfigSiftScaleSpace();
	public ConfigSurfDescribe.Fast configSurfFast = new ConfigSurfDescribe.Fast();
	public ConfigSurfDescribe.Stability configSurfStability = new ConfigSurfDescribe.Stability();
	public ConfigSiftDescribe configSiftDescribe = new ConfigSiftDescribe();
	public ConfigBrief configBrief = new ConfigBrief(false);
	public ConfigTemplateDescribe configTemplate = new ConfigTemplateDescribe();
	public ConfigAssociateGreedy configAssocGreedy = new ConfigAssociateGreedy();
	public ConfigAssociateNearestNeighbor configAssocNN = new ConfigAssociateNearestNeighbor();

	// Controls for different detectors / descriptors
	public ControlPanelSiftDetector controlDetectSift;
	public ControlPanelFastHessian controlDetectFastHessian;
	public ControlPanelPointDetector controlDetectPoint;
	public ControlPanelSurfDescribe.Speed controlDescSurfFast;
	public ControlPanelSurfDescribe.Stability controlDescSurfStable;
	public ControlPanelDescribeSift controlDescSift;
	public ControlPanelDescribeBrief controlDescBrief;
	public ControlPanelDescribeTemplate controlDescTemplate;
	public ControlPanelAssociateGreedy controlAssocGreedy;
	public ControlPanelAssociateNearestNeighbor controlAssocNN;

	public void initializeControlsGUI() {
		comboDetect    = combo(selectedDetector,DETECT_TYPES);
		comboDescribe  = combo(selectedDescriptor,DESCRIBE_TYPES);
		comboAssociate = combo(selectedAssociate,ASSOCIATE_TYPES);

		controlDetectSift = new ControlPanelSiftDetector(configSiftScaleSpace,configSiftDetector,this::handleControlsUpdated);
		controlDetectFastHessian = new ControlPanelFastHessian(configFastHessian,this::handleControlsUpdated);
		controlDetectPoint = new ControlPanelPointDetector(-1, PointDetectorTypes.SHI_TOMASI,this::handleControlsUpdated);
		controlDescSurfFast = new ControlPanelSurfDescribe.Speed(configSurfFast,this::handleControlsUpdated);
		controlDescSurfStable = new ControlPanelSurfDescribe.Stability(configSurfStability,this::handleControlsUpdated);
		controlDescSift = new ControlPanelDescribeSift(configSiftDescribe,this::handleControlsUpdated);
		controlDescBrief = new ControlPanelDescribeBrief(configBrief,this::handleControlsUpdated);
		controlDescTemplate = new ControlPanelDescribeTemplate(configTemplate,this::handleControlsUpdated);
		controlAssocGreedy = new ControlPanelAssociateGreedy(configAssocGreedy,this::handleControlsUpdated);
		controlAssocNN = new ControlPanelAssociateNearestNeighbor(configAssocNN,this::handleControlsUpdated);

		controlDetectSift.setBorder(BorderFactory.createEmptyBorder());
		controlDetectFastHessian.setBorder(BorderFactory.createEmptyBorder());
		controlDetectPoint.setBorder(BorderFactory.createEmptyBorder());
		controlDescSurfFast.setBorder(BorderFactory.createEmptyBorder());
		controlDescSurfStable.setBorder(BorderFactory.createEmptyBorder());
		controlDescSift.setBorder(BorderFactory.createEmptyBorder());
		controlDescBrief.setBorder(BorderFactory.createEmptyBorder());
		controlDescTemplate.setBorder(BorderFactory.createEmptyBorder());
		controlAssocGreedy.setBorder(BorderFactory.createEmptyBorder());
		controlAssocNN.setBorder(BorderFactory.createEmptyBorder());
	}

	/**
	 * Called when the user modifies a setting in one of the controls. Should be overloaded by
	 * an extending class.
	 */
	protected abstract void handleControlsUpdated();

	public JPanel getDetectorPanel() {
		switch(selectedDetector) {
			case 0: return controlDetectFastHessian;
			case 1: return controlDetectSift;
			case 2: return controlDetectPoint;
			default: throw new IllegalArgumentException("Unknown");
		}
	}

	public JPanel getDescriptorPanel() {
		switch(selectedDescriptor) {
			case 0: return controlDescSurfFast;
			case 1: return controlDescSurfStable;
			case 2: return controlDescSift;
			case 3: return controlDescBrief;
			case 4: return controlDescTemplate;
			default: throw new IllegalArgumentException("Unknown");
		}
	}

	public JPanel getAssociatePanel() {
		System.out.println("Selected associate "+selectedAssociate);
		switch(selectedAssociate) {
			case 0: return controlAssocGreedy;
			case 1:
			case 2: return controlAssocNN;
			default: throw new IllegalArgumentException("Unknown");
		}
	}

	/**
	 * Creates an implementation of {@link DetectDescribePoint}. if possible a specialized implementation is created
	 */
	public <T extends ImageGray<T>, D extends ImageGray<D>>
	DetectDescribePoint<T,?> createDetectDescribe( Class<T> imageType )
	{
		// See if a special combined detector/descriptor is available
		DetectDescribePoint<T,?> output = null;
		if( selectedDetector == 0 ) {
			if( selectedDescriptor == 0 ) {
				output = FactoryDetectDescribe.surfFast(
						configFastHessian, configSurfFast,null,imageType);
			} else if( selectedDescriptor == 1 ) {
				output = FactoryDetectDescribe.surfStable(
						configFastHessian,configSurfStability,null,imageType);
			}
		} else if( selectedDetector == 1 && selectedDescriptor == 2 ) {
			var config = new ConfigCompleteSift();
			config.scaleSpace = controlDetectSift.configSS;
			config.detector = controlDetectSift.configDetector;
			config.describe = controlDescSift.config;
			output = FactoryDetectDescribe.sift(config);
		}

		if( output != null )
			return output;

		// Create detector/descriptor using independent components
		var detector = createDetector(imageType);
		var describe = createDescriptor(imageType);

		OrientationImage<T> orientation = null;

		// only compute orientation if the descriptor will use it
		if( describe.isOriented() ) {
			Class integralType = GIntegralImageOps.getIntegralType(imageType);
			OrientationIntegral orientationII = FactoryOrientationAlgs.sliding_ii(null, integralType);
			orientation = FactoryOrientation.convertImage(orientationII, imageType);
		}
		return FactoryDetectDescribe.fuseTogether(detector,orientation,describe);
	}

	public <T extends ImageGray<T>, D extends ImageGray<D>>
	InterestPointDetector<T> createDetector( Class<T> imageType ) {
		switch(selectedDetector) {
			case 0: return FactoryInterestPoint.fastHessian(controlDetectFastHessian.config);
			case 1: return FactoryInterestPoint.sift(
					controlDetectSift.configSS,controlDetectSift.configDetector,imageType);
			case 2: {
				GeneralFeatureDetector<T, D> alg = controlDetectPoint.create(imageType);
				return FactoryInterestPoint.wrapPoint(alg, controlDetectPoint.pointDetectRadius, imageType, alg.getDerivType());
			}
			default:
				throw new IllegalArgumentException("Unknown detector");
		}
	}

	public <T extends ImageGray<T>, D extends ImageGray<D>>
	DescribeRegionPoint<T,?> createDescriptor(Class<T> imageType ) {
		switch(selectedDescriptor) {
			case 0:
				if( controlDescSurfFast.color ) {
					return (DescribeRegionPoint)FactoryDescribeRegionPoint.surfColorFast(
							controlDescSurfFast.config, ImageType.pl(3, imageType));
				} else {
					return FactoryDescribeRegionPoint.surfFast(controlDescSurfFast.config, imageType);
				}
			case 1:
				if( controlDescSurfStable.color ) {
					return (DescribeRegionPoint)FactoryDescribeRegionPoint.surfColorStable(
							controlDescSurfStable.config, ImageType.pl(3, imageType));
				} else {
					return FactoryDescribeRegionPoint.surfStable(controlDescSurfStable.config, imageType);
				}
			case 2: return FactoryDescribeRegionPoint.sift(
					controlDetectSift.configSS,controlDescSift.config, imageType);
			case 3: return FactoryDescribeRegionPoint.brief(controlDescBrief.config, imageType);
			case 4: return FactoryDescribeRegionPoint.template(controlDescTemplate.config, imageType);
			default:
				throw new IllegalArgumentException("Unknown descriptor");
		}
	}

	public AssociateDescription createAssociate( DescriptorInfo descriptor ) {
		ScoreAssociation scorer = FactoryAssociation.defaultScore(descriptor.getDescriptionType());
		int DOF = descriptor.createDescription().size();

		if( selectedAssociate == 0 ) {
			return FactoryAssociation.greedy(controlAssocGreedy.config,scorer);
		} else {
			if( !TupleDesc_F64.class.isAssignableFrom(descriptor.getDescriptionType())) {
				JOptionPane.showMessageDialog(this, "Requires TupleDesc_F64 description type");
				return null;
			}

			ConfigAssociateNearestNeighbor configNN = controlAssocNN.config;
			switch( selectedAssociate ) {
				case 1: return FactoryAssociation.kdtree(configNN,DOF, 75);
				case 2: return FactoryAssociation.kdRandomForest(
						configNN,DOF, 75, 10, 5, 1233445565);
				default:
					throw new IllegalArgumentException("Unknown association");
			}
		}
	}
}
