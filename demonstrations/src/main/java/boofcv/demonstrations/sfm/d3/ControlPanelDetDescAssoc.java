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
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.describe.DescriptorInfo;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.factory.feature.associate.ConfigAssociate;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.ConfigDescribeRegionPoint;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detdesc.ConfigDetectDescribe;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.feature.*;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageGray;

import javax.swing.*;

/**
 * Contains controls for all the usual detectors, descriptors, and associations. Mostly contains boiler plate
 * and leaves the final visualization step to the implementing class.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public abstract class ControlPanelDetDescAssoc extends StandardAlgConfigPanel {
	protected JComboBox<String> comboDetect;
	protected JComboBox<String> comboDescribe;
	protected JComboBox<String> comboAssociate;

	// Configurations. Modify these before calling initializeControlsGUI
	public ConfigDetectDescribe configDetDesc = new ConfigDetectDescribe();
	public ConfigAssociate configAssociate = new ConfigAssociate();


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

	public ControlPanelDetDescAssoc() {
	}

	public ControlPanelDetDescAssoc(ConfigDetectDescribe configDetDesc,
									ConfigAssociate configAssociate)
	{
		this.configDetDesc = configDetDesc;
		this.configAssociate = configAssociate;
	}

	public void initializeControlsGUI() {
		comboDetect    = combo(configDetDesc.typeDetector.ordinal(),ConfigDetectInterestPoint.DetectorType.values() );
		comboDescribe  = combo(configDetDesc.typeDescribe.ordinal(),ConfigDescribeRegionPoint.DescriptorType.values() );
		comboAssociate = combo(configAssociate.type.ordinal(),ConfigAssociate.AssociationType.values());

		controlDetectSift = new ControlPanelSiftDetector(configDetDesc.scaleSpaceSift, configDetDesc.detectSift,this::handleControlsUpdated);
		controlDetectFastHessian = new ControlPanelFastHessian(configDetDesc.detectFastHessian,this::handleControlsUpdated);
		controlDetectPoint = new ControlPanelPointDetector(configDetDesc.detectPoint,this::handleControlsUpdated);
		controlDescSurfFast = new ControlPanelSurfDescribe.Speed(configDetDesc.describeSurfFast,this::handleControlsUpdated);
		controlDescSurfStable = new ControlPanelSurfDescribe.Stability(configDetDesc.describeSurfStability,this::handleControlsUpdated);
		controlDescSift = new ControlPanelDescribeSift(configDetDesc.describeSift,this::handleControlsUpdated);
		controlDescBrief = new ControlPanelDescribeBrief(configDetDesc.describeBrief,this::handleControlsUpdated);
		controlDescTemplate = new ControlPanelDescribeTemplate(configDetDesc.describeTemplate,this::handleControlsUpdated);
		controlAssocGreedy = new ControlPanelAssociateGreedy(configAssociate.greedy,this::handleControlsUpdated);
		controlAssocNN = new ControlPanelAssociateNearestNeighbor(configAssociate.nearestNeighbor,this::handleControlsUpdated);

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
		switch(configDetDesc.typeDetector) {
			case FAST_HESSIAN: return controlDetectFastHessian;
			case SIFT: return controlDetectSift;
			case POINT: return controlDetectPoint;
			default: throw new IllegalArgumentException("Unknown");
		}
	}

	public JPanel getDescriptorPanel() {
		switch(configDetDesc.typeDescribe) {
			case SURF_FAST: return controlDescSurfFast;
			case SURF_STABLE: return controlDescSurfStable;
			case SIFT: return controlDescSift;
			case BRIEF: return controlDescBrief;
			case TEMPLATE: return controlDescTemplate;
			default: throw new IllegalArgumentException("Unknown");
		}
	}

	public JPanel getAssociatePanel() {
		switch(configAssociate.type) {
			case GREEDY: return controlAssocGreedy;
			case KD_TREE:
			case RANDOM_FOREST: return controlAssocNN;
			default: throw new IllegalArgumentException("Unknown");
		}
	}

	/**
	 * Creates an implementation of {@link DetectDescribePoint}. if possible a specialized implementation is created
	 */
	public <T extends ImageGray<T>, D extends ImageGray<D>>
	DetectDescribePoint<T,?> createDetectDescribe( Class<T> imageType )
	{
		return FactoryDetectDescribe.generic(configDetDesc,imageType);
	}

	public <T extends ImageGray<T>, D extends ImageGray<D>>
	InterestPointDetector<T> createDetector( Class<T> imageType ) {
		ConfigDetectInterestPoint c = new ConfigDetectInterestPoint();
		c.type = configDetDesc.typeDetector;
		c.fastHessian = configDetDesc.detectFastHessian;
		c.point = configDetDesc.detectPoint;
		c.scaleSpaceSift = configDetDesc.scaleSpaceSift;
		c.sift = configDetDesc.detectSift;

		return FactoryInterestPoint.generic(c,imageType, null);
	}

	public <T extends ImageGray<T>, D extends ImageGray<D>>
	DescribeRegionPoint<T,?> createDescriptor(Class<T> imageType ) {
		ConfigDescribeRegionPoint c = new ConfigDescribeRegionPoint();
		c.type = configDetDesc.typeDescribe;
		c.brief = configDetDesc.describeBrief;
		c.surfFast = configDetDesc.describeSurfFast;
		c.surfStability = configDetDesc.describeSurfStability;
		c.scaleSpaceSift = configDetDesc.scaleSpaceSift;
		c.template = configDetDesc.describeTemplate;

		return FactoryDescribeRegionPoint.generic(c,imageType);
	}

	public AssociateDescription createAssociate( DescriptorInfo descriptor ) {

		if( configAssociate.type != ConfigAssociate.AssociationType.GREEDY ) {
			if (!TupleDesc_F64.class.isAssignableFrom(descriptor.getDescriptionType())) {
				JOptionPane.showMessageDialog(this, "Requires TupleDesc_F64 description type");
				// not really sure what to do here. I'll just force it to be greedy to avoid a crash
				configAssociate.type = ConfigAssociate.AssociationType.GREEDY;
			}
		}

		return FactoryAssociation.generic(configAssociate,descriptor);
	}
}
