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

package boofcv.alg.disparity.sgm;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.disparity.sgm.cost.SgmCostFromBlocks;
import boofcv.alg.disparity.sgm.cost.SgmMutualInformation_U8;
import boofcv.alg.disparity.sgm.cost.StereoMutualInformation;
import boofcv.alg.transform.pyramid.PyramidDiscreteNN2;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;

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
 * @author Peter Abeles
 * @see StereoMutualInformation
 * @see SgmCostAggregation
 * @see SgmDisparitySelector
 *
 * <p>[1] Hirschmuller, Heiko. "Stereo processing by semiglobal matching and mutual information."
 * IEEE Transactions on pattern analysis and machine intelligence 30.2 (2007): 328-341.</p>
 */
@SuppressWarnings("WeakerAccess")
public class SgmStereoDisparityHmi extends SgmStereoDisparityError<GrayU8> {

	protected Random rand = new Random(234);

	// Mutual information based cost
	protected StereoMutualInformation stereoMI;

	// Number of extra iterations to perform when at the highest resolution level in the pyramid
	protected int extraIterations = 0;

	protected PyramidDiscreteNN2<GrayU8> pyrLeft = new PyramidDiscreteNN2<>(ImageType.single(GrayU8.class));
	protected PyramidDiscreteNN2<GrayU8> pyrRight = new PyramidDiscreteNN2<>(ImageType.single(GrayU8.class));

	/**
	 * Provides configurations and internal implementations of different components
	 *
	 * @param configPyr Specifies number of layers in the pyramid
	 * @param stereoMI Computes mutual information from a stereo pair with known disparity
	 * @param selector Selects the best disparity given the cost
	 */
	public SgmStereoDisparityHmi( ConfigDiscreteLevels configPyr,
								  StereoMutualInformation stereoMI,
								  SgmDisparitySelector selector ) {
		super(new SgmMutualInformation_U8(stereoMI), selector);
		this.stereoMI = stereoMI;
		pyrLeft.getConfigLayers().setTo(configPyr);
		pyrRight.getConfigLayers().setTo(configPyr);
	}

	public SgmStereoDisparityHmi( ConfigDiscreteLevels configPyr,
								  StereoMutualInformation stereoMI,
								  SgmDisparitySelector selector,
								  SgmCostFromBlocks<GrayU8> blockScore ) {
		super(blockScore, selector);
		this.stereoMI = stereoMI;
		pyrLeft.getConfigLayers().setTo(configPyr);
		pyrRight.getConfigLayers().setTo(configPyr);
	}

	/**
	 * Computes disparity using pyramidal mutual information.
	 *
	 * @param left (Input) left rectified stereo image
	 * @param right (Input) right rectified stereo image
	 */
	@Override
	public void process( GrayU8 left, GrayU8 right ) {
		InputSanityCheck.checkSameShape(left, right);
		disparity.reshape(left);

		helper.configure(left.width, disparityMin, disparityRange);

		// Create image pyramid
		pyrLeft.process(left);
		pyrRight.process(right);

		// Initialize MI by assuming pixels with a similar value are similar
		// NOTE: Scaling by average pixel intensity seems to be a wash. helps/hurts equally.
		stereoMI.diagonalHistogram(1.0, SgmDisparityCost.MAX_COST);
		// Random is the suggested approach but only works on simplistic scenes
//		stereoMI.randomHistogram(rand,SgmDisparityCost.MAX_COST);

		// Process from low to high resolution. The only disparity estimate which is
		// saved is the full resolution one. All prior estimates are done to estimate the mutual information
		int tol_R_to_L = selector.getRightToLeftTolerance();
		double textureThreshold = selector.getTextureThreshold();
		for (int level = pyrLeft.getLevelsCount() - 1; level >= 0; level--) {
			int scale = 1 << level;
			// reduce the minimum disparity for this scale otherwise it might not consider any matches at this scale
			int levelDisparityMin = (int)Math.round(disparityMin/(double)scale);
			int levelDisparityRange = (int)Math.ceil(disparityRange/(double)scale);

			GrayU8 levelLeft = pyrLeft.get(level);
			GrayU8 levelRight = pyrRight.get(level);

			if (level > 0) {
				// Turning off these validation checks when learning seems to result in a better model
				// This was shown in Middlebury where the number of bad or invalid pixels decreased significantly overall
				selector.setTextureThreshold(-1);
				selector.setRightToLeftTolerance(-1);
			} else {
				selector.setTextureThreshold(textureThreshold);
				selector.setRightToLeftTolerance(tol_R_to_L);
			}
			sgmCost.configure(levelDisparityMin, levelDisparityRange);
			aggregation.configure(levelDisparityMin);
			sgmCost.process(levelLeft, levelRight, costYXD);
			aggregation.process(costYXD);
			selector.setDisparityMin(levelDisparityMin); // todo move to function below
			selector.select(costYXD, aggregation.getAggregated(), disparity);

			if (level > 0) {
				int invalid = selector.getInvalidDisparity();
				// Update the mutual information model using the latest disparity estimate
				stereoMI.process(levelLeft, levelRight, levelDisparityMin, disparity, invalid);
				stereoMI.precomputeScaledCost(SgmDisparityCost.MAX_COST);
			}
		}
		for (int i = 0; i < extraIterations; i++) {
			stereoMI.process(left, right, disparityMin, disparity, selector.getInvalidDisparity());
			stereoMI.precomputeScaledCost(SgmDisparityCost.MAX_COST);
			sgmCost.configure(disparityMin, disparityRange);
			aggregation.configure(disparityMin);
			sgmCost.process(left, right, costYXD);
			aggregation.process(costYXD);
			selector.setDisparityMin(disparityMin);
			selector.select(costYXD, aggregation.getAggregated(), disparity);
		}
	}

	/**
	 * Computes disparity from an initial disparity estimate. A pyramid is not required because of the
	 * initial disparity estimate. This function can be called iteratively to improve the estimate.
	 *
	 * @param left (Input) left rectified stereo image
	 * @param right (Input) right rectified stereo image
	 * @param disparityEst (Input) Initial disparity estimate used to compute MI
	 */
	public void process( GrayU8 left, GrayU8 right, GrayU8 disparityEst, int invalid ) {
		InputSanityCheck.checkSameShape(left, right, disparityEst);

		// Compute mutual information model given the initial disparity estimate
		stereoMI.process(left, right, disparityMin, disparityEst, invalid);
		stereoMI.precomputeScaledCost(SgmDisparityCost.MAX_COST);

		super.process(left, right);
	}

	public StereoMutualInformation getStereoMI() {
		return stereoMI;
	}

	public PyramidDiscreteNN2<GrayU8> getPyrLeft() {
		return pyrLeft;
	}

	public PyramidDiscreteNN2<GrayU8> getPyrRight() {
		return pyrRight;
	}

	public int getExtraIterations() {
		return extraIterations;
	}

	public void setExtraIterations( int extraIterations ) {
		this.extraIterations = extraIterations;
	}
}
