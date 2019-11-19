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

import boofcv.alg.InputSanityCheck;
import boofcv.alg.transform.pyramid.PyramidDiscreteNN2;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;

import java.util.Random;

/**
 * <p>Estimates stereo disparity using Semi Global Matching and Hierarchical Mutual Information Cost. This is intended
 * to be a faithful reproduction of the algorithm described in [1] with no significant deviations.</p>
 *
 * <p>SGM works by computing the path cost along  (2,4,8, or 16) directions for each pixel. These costs
 * are summed up and the path which minimizes the total cost is selected. By considering multiple paths along
 * different directions it avoids the pitfalls of earlier approaches, such as dynamic programming along the x-axis
 * only. It is also by far faster than globally optimal approaches but slower then approaches such as block matching</p>
 *
 * <p>MI cost requires prior information to compute. If the disparity is known then MI can be found. However,
 * we want to find the disparity using MI. To get around this problem a pyramidal approach is used to estimate MI.
 * The MI cost is initially assigned random values. This is
 * then used to estimate the disparity at the lowest resolution image in the pyramid. The found disparity is
 * used to update the MI estimate. This process is repeated until it reaches the highest resolution layer. By
 * this point it should have a good model for MI.</p>
 *
 * @see StereoMutualInformation
 * @see SgmCostAggregation
 * @see SgmDisparitySelector
 *
 * <p>[1] Hirschmuller, Heiko. "Stereo processing by semiglobal matching and mutual information."
 * IEEE Transactions on pattern analysis and machine intelligence 30.2 (2007): 328-341.</p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings("WeakerAccess")
public class SgmStereoDisparityHmi {

	protected Random rand = new Random(234);

	// Mutual information based cost
	protected StereoMutualInformation stereoMI;
	protected SgmDisparityCost<GrayU8> sgmCost;

	protected SgmCostAggregation aggregation = new SgmCostAggregation();
	protected SgmDisparitySelector selector;

	protected Planar<GrayU16> costYXD = new Planar<>(GrayU16.class,1,1,1);

	protected PyramidDiscreteNN2<GrayU8> pyrLeft = new PyramidDiscreteNN2<>(ImageType.single(GrayU8.class));
	protected PyramidDiscreteNN2<GrayU8> pyrRight = new PyramidDiscreteNN2<>(ImageType.single(GrayU8.class));

	// Storage for found disparity
	protected GrayU8 disparity = new GrayU8(1,1);

	/**
	 * Provides configurations and internal implementations of different components
	 *
	 * @param minWidth Minimum width in the image pyramid. Used to dynamically create the pyramid
	 * @param stereoMI Computes mutual information from a stereo pair with known disparity
	 * @param selector Selects the best disparity given the cost
	 */
	public SgmStereoDisparityHmi( int minWidth,
								  StereoMutualInformation stereoMI ,
								  SgmDisparitySelector selector ) {
		this.stereoMI = stereoMI;
		this.selector = selector;
		pyrLeft.setMinWidth(minWidth);
		pyrRight.setMinWidth(minWidth);

		sgmCost = new SgmMutualInformation(stereoMI);
	}

	/**
	 * Computes disparity using pyramidal mutual information
	 *
	 * @param left
	 * @param right
	 * @param disparityMin
	 * @param disparityRange
	 */
	public void process( GrayU8 left , GrayU8 right , int disparityMin , int disparityRange ) {
		InputSanityCheck.checkSameShape(left,right);
		disparity.reshape(left);

		// Create image pyramid
		pyrLeft.process(left);
		pyrRight.process(right);

		// randomly initialize MI
		stereoMI.randomHistogram(rand,SgmDisparityCost.MAX_COST);

		int invalid = disparityRange; // TODO make accessible as a getInvalid()

		// Process from low to high resolution. The only disparity esitmate which is
		// saved is the full resolution one. All prior estimates are done to estimate the mutual information
		for (int level = pyrLeft.getLevelsCount()-1; level >= 0; level-- ) {
			GrayU8 levelLeft = pyrLeft.get(level);
			GrayU8 levelRight = pyrRight.get(level);

			sgmCost.process(levelLeft,levelRight, disparityMin, disparityRange,costYXD);
			aggregation.process(costYXD);
			selector.select(costYXD,disparityMin,invalid,disparity);

			if( level > 0 ) {
				// Update the mututal information model using the latest disparity estimate
				stereoMI.process(levelLeft, levelRight, disparityMin, disparity, invalid);
				stereoMI.precomputeScaledCost(SgmDisparityCost.MAX_COST);
			}
		}

		// TODO optional estimate of disparity to sub-pixel accuracy
	}

	// TODO provide a function where an initial disparity estimate can be provided

	public GrayU8 getDisparity() {
		return disparity;
	}

	public StereoMutualInformation getStereoMI() {
		return stereoMI;
	}

	public SgmDisparityCost<GrayU8> getSgmCost() {
		return sgmCost;
	}

	public SgmCostAggregation getAggregation() {
		return aggregation;
	}

	public Planar<GrayU16> getCostYXD() {
		return costYXD;
	}

	public PyramidDiscreteNN2<GrayU8> getPyrLeft() {
		return pyrLeft;
	}

	public PyramidDiscreteNN2<GrayU8> getPyrRight() {
		return pyrRight;
	}
}
