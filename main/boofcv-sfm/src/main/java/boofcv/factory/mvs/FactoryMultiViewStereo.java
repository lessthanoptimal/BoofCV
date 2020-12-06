/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.mvs;

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.alg.mvs.video.SelectFramesForReconstruction3D;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.geo.ConfigLMedS;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating Multi-View Stereo related classes
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class FactoryMultiViewStereo {
	/**
	 * Creates {@link SelectFramesForReconstruction3D} which is used for down sampling frames in image sequences
	 * to select for ones which have significant 3D information
	 *
	 * @param config (Optional) Configuration
	 * @param imageType Type of input image
	 * @return A new instance.
	 */
	public static <T extends ImageGray<T>> SelectFramesForReconstruction3D<T>
	frameSelector3D( @Nullable ConfigSelectFrames3D config, ImageType<T> imageType ) {
		if (config == null)
			config = new ConfigSelectFrames3D();

		// Image tracker currently only supports gray images
		Class<T> grayType = imageType.getImageClass();

		DescribeRegionPoint<T, TupleDesc_F64> describe = FactoryDescribeRegionPoint.generic(config.describe, imageType);

		var alg = new SelectFramesForReconstruction3D<T>(describe);
		alg.setMotionInlier(config.motionInlier);
		alg.setMaxFrameSkip(config.maxFrameSkip);
		alg.setMinimumFeatures(config.minimumFeatures);
		alg.setSignificantFraction(config.significantFraction);

		alg.setTracker(FactoryPointTracker.tracker(config.tracker, grayType, null));
		alg.setAssociate(FactoryAssociation.generic2(config.associate,describe));

		// Hard code this for now
		ConfigLMedS configRobust = new ConfigLMedS();
		configRobust.totalCycles = config.robustIterations;

		alg.setRobust3D(FactoryMultiViewRobust.fundamentalLMedS(null,configRobust));
		alg.setRobustH(FactoryMultiViewRobust.homographyLMedS(null,configRobust));

		// Squared error is used for all the error metrics in robust I *think*,
		// so the square root can be safely applied
		alg.setCompareFit((error3D,errorH,tol)->(1.0-Math.sqrt(error3D)/(0.001+Math.sqrt(errorH)))>=tol);

		return alg;
	}
}
