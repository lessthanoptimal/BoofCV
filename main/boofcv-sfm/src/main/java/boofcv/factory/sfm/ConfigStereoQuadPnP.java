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

package boofcv.factory.sfm;

import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.factory.feature.associate.ConfigAssociate;
import boofcv.factory.feature.associate.ConfigAssociateGreedy;
import boofcv.factory.feature.describe.ConfigDescribeRegion;
import boofcv.factory.feature.detdesc.ConfigDetectDescribe;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.feature.detect.selector.SelectLimitTypes;
import boofcv.factory.geo.ConfigBundleAdjustment;
import boofcv.factory.geo.ConfigRansac;
import boofcv.factory.geo.EnumPNP;
import boofcv.misc.ConfigConverge;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.abst.sfm.d3.WrapVisOdomDualTrackPnP}.
 *
 * @author Peter Abeles
 */
public class ConfigStereoQuadPnP implements Configuration {

	/** Configuration for Bundle Adjustment */
	public ConfigBundleAdjustment bundle = new ConfigBundleAdjustment();
	/** Convergence criteria for bundle adjustment. Set max iterations to &le; 0 to disable */
	public ConfigConverge bundleConverge = new ConfigConverge(1e-3, 1e-3, 1);

	/** Which PNP solution to use */
	public EnumPNP pnp = EnumPNP.P3P_GRUNERT;

	/** Configuration for RANSAC. Used to robustly estimate frame-to-frame motion */
	public ConfigRansac ransac = new ConfigRansac(500, 1.5);
	/** Number of iterations to perform when refining the initial frame-to-frame motion estimate. Disable &le; 0 */
	public int refineIterations = 50;

	/** Which feature detector / descriptor should it use */
	public ConfigDetectDescribe detectDescribe = new ConfigDetectDescribe();

	/** Association approach for matching frames across time steps */
	public ConfigAssociate associateF2F = new ConfigAssociate();
	/** Association approach for matching stereo pairs */
	public ConfigAssociateGreedy associateL2R = new ConfigAssociateGreedy(false, 1.0, -1);

	/** Tolerance for matching stereo features along epipolar line in Pixels */
	public double epipolarTol = 3.0;

	{
		detectDescribe.typeDescribe = ConfigDescribeRegion.Type.BRIEF;
		detectDescribe.describeBrief.fixed = true;

		detectDescribe.typeDetector = ConfigDetectInterestPoint.Type.FAST_HESSIAN;
		detectDescribe.detectFastHessian.extract.radius = 2;
		detectDescribe.detectFastHessian.maxFeaturesPerScale = 300;
		detectDescribe.detectFastHessian.numberOfOctaves = 4;

		// while not active, let's give it a reasonable configuration for a point detector
		detectDescribe.detectPoint.type = PointDetectorTypes.SHI_TOMASI;
		detectDescribe.detectPoint.scaleRadius = 11;
		detectDescribe.detectPoint.shiTomasi.radius = 3;
		detectDescribe.detectPoint.general.threshold = 1.0f;
		detectDescribe.detectPoint.general.radius = 4;
		detectDescribe.detectPoint.general.maxFeatures = 1000;
		detectDescribe.detectPoint.general.selector.type = SelectLimitTypes.SELECT_N;

		associateF2F.type = ConfigAssociate.AssociationType.GREEDY;
	}

	public ConfigStereoQuadPnP setTo( ConfigStereoQuadPnP src ) {
		this.bundle.setTo(src.bundle);
		this.bundleConverge.setTo(src.bundleConverge);
		this.pnp = src.pnp;
		this.ransac.setTo(src.ransac);
		this.refineIterations = src.refineIterations;
		this.detectDescribe.setTo(src.detectDescribe);
		this.associateF2F.setTo(src.associateF2F);
		this.associateL2R.setTo(src.associateL2R);
		this.epipolarTol = src.epipolarTol;
		return this;
	}

	@Override
	public void checkValidity() {
		bundleConverge.checkValidity();
		detectDescribe.checkValidity();
		associateF2F.checkValidity();
		associateL2R.checkValidity();
	}
}
