/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity.sgm;

import boofcv.alg.transform.pyramid.PyramidDiscreteNN2;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;

import java.util.Random;

/**
 * Estimates stereo disparity using Semi Global Matching and Hierarchical Mutual Information Cost. A pyramidal
 * approach is used to estimate MI. it works by down sampling the stereo image
 *
 * @author Peter Abeles
 */
public class SgmStereoDisparityHmi {

	Random rand = new Random(234);

	StereoMutualInformation stereoMI;
	SgmDisparityCost<GrayU8> sgmCost;

	SgmCostAggregation aggregation = new SgmCostAggregation();
	SgmDisparitySelector selector;

	Planar<GrayU16> costYXD = new Planar<>(GrayU16.class,1,1,1);

	PyramidDiscreteNN2<GrayU8> pyrLeft = new PyramidDiscreteNN2<>(ImageType.single(GrayU8.class));
	PyramidDiscreteNN2<GrayU8> pyrRight = new PyramidDiscreteNN2<>(ImageType.single(GrayU8.class));

	int disparityMin;
	int disparityRange;

	public SgmStereoDisparityHmi( int minWidth,
								  StereoMutualInformation stereoMI ,
								  SgmDisparitySelector selector ) {
		this.stereoMI = stereoMI;
		this.selector = selector;
		pyrLeft.setMinWidth(minWidth);
		pyrRight.setMinWidth(minWidth);

		sgmCost = new SgmMutualInformation(stereoMI);
	}

	public void process( GrayU8 left , GrayU8 right , GrayU8 disparity ) {
		// Create image pyramid
		pyrLeft.process(left);
		pyrRight.process(right);

		// randomly initialize MI
		stereoMI.randomHistogram(rand,SgmDisparityCost.MAX_COST);

		int invalid = disparityRange;

		// Process from low to high resolution. The only disparity esitmate which is
		// saved is the full resolution one. All prior estimates are done to estimate the mutual information
		for (int level = pyrLeft.getNumLevels()-2; level >= 0; level-- ) {
			GrayU8 levelLeft = pyrLeft.get(level);
			GrayU8 levelRight = pyrRight.get(level);

			sgmCost.process(levelLeft,levelRight, disparityMin, disparityRange,costYXD);
			aggregation.process(costYXD);
			selector.select(costYXD,disparityMin,invalid,disparity);

			if( level > 0 ) {
				// Update the mututal information
				stereoMI.process(levelLeft, levelRight, disparityMin, disparity, invalid);
				stereoMI.precomputeScaledCost(SgmDisparityCost.MAX_COST);
			}
		}
	}
}
