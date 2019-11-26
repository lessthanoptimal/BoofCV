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
import boofcv.alg.transform.pyramid.ConfigPyramid2;
import boofcv.alg.transform.pyramid.PyramidDiscreteNN2;
import boofcv.struct.image.*;

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
	protected SgmMutualInformation sgmCost;

	protected SgmCostAggregation aggregation = new SgmCostAggregation();
	protected SgmDisparitySelector selector;

	protected Planar<GrayU16> costYXD = new Planar<>(GrayU16.class,1,1,1);

	protected PyramidDiscreteNN2<GrayU8> pyrLeft = new PyramidDiscreteNN2<>(ImageType.single(GrayU8.class));
	protected PyramidDiscreteNN2<GrayU8> pyrRight = new PyramidDiscreteNN2<>(ImageType.single(GrayU8.class));

	// Minimum disparity and number of possible disparity values
	protected int disparityMin = 0;
	protected int disparityRange = 0;

	// Storage for found disparity
	protected GrayU8 disparity = new GrayU8(1,1);

	/**
	 * Provides configurations and internal implementations of different components
	 *
	 * @param configPyr Specifies number of layers in the pyramid
	 * @param stereoMI Computes mutual information from a stereo pair with known disparity
	 * @param selector Selects the best disparity given the cost
	 */
	public SgmStereoDisparityHmi( ConfigPyramid2 configPyr,
								  StereoMutualInformation stereoMI ,
								  SgmDisparitySelector selector ) {
		this.stereoMI = stereoMI;
		this.selector = selector;
		pyrLeft.getConfigLayers().set(configPyr);
		pyrRight.getConfigLayers().set(configPyr);
		sgmCost = new SgmMutualInformation(stereoMI);
	}

	/**
	 * Computes disparity using pyramidal mutual information.
	 *
	 * @param left (Input) left rectified stereo image
	 * @param right (Input) right rectified stereo image
	 */
	public void processHmi(GrayU8 left , GrayU8 right ) {
		InputSanityCheck.checkSameShape(left,right);
		disparity.reshape(left);

		// Create image pyramid
		pyrLeft.process(left);
		pyrRight.process(right);

		// randomly initialize MI
		stereoMI.diagonalHistogram(SgmDisparityCost.MAX_COST);
//		stereoMI.randomHistogram(rand,SgmDisparityCost.MAX_COST);

		// While 'learning' the MI turn off right to left validation. Incorrect matches tend to help more than
		// hurt at this step
//		int rightToLeftTol = selector.getRightToLeftTolerance();

		// Process from low to high resolution. The only disparity esitmate which is
		// saved is the full resolution one. All prior estimates are done to estimate the mutual information
		for (int level = pyrLeft.getLevelsCount()-1; level >= 0; level-- ) {
//			if( level > 0 ) {
//				selector.setRightToLeftTolerance(-1);
//			} else {
//				selector.setRightToLeftTolerance(rightToLeftTol);
//			}
			int scale = 1 << level;
			// reduce the minimum disparity for this scale otherwise it might not consider any matches at this scale
			int levelDisparityMin = (int)Math.round(disparityMin/(double)scale);
			int levelDisparityRange = (int)Math.ceil(disparityRange/(double)scale);

			GrayU8 levelLeft = pyrLeft.get(level);
			GrayU8 levelRight = pyrRight.get(level);

			sgmCost.process(levelLeft,levelRight, levelDisparityMin, levelDisparityRange,costYXD);
			aggregation.process(costYXD,disparityMin);
			selector.setMinDisparity(levelDisparityMin); // todo move to function below
			selector.select(costYXD,aggregation.getAggregated(),disparity);

			if( level > 0 ) {
				int invalid = selector.getInvalidDisparity();
				// Update the mutual information model using the latest disparity estimate
				stereoMI.process(levelLeft, levelRight, levelDisparityMin, disparity, invalid);
				stereoMI.precomputeScaledCost(SgmDisparityCost.MAX_COST);
			}
		}
//		for (int i = 0; i < 2; i++) {
//			stereoMI.process(left, right, disparityMin, disparity, selector.getInvalidDisparity());
//			stereoMI.precomputeScaledCost(SgmDisparityCost.MAX_COST);
//			sgmCost.process(left,right, disparityMin, disparityRange,costYXD);
//			aggregation.process(costYXD);
//			selector.setMinDisparity(disparityMin);
//			selector.select(aggregation.getAggregated(),disparity);
//		}
		// TODO optional estimate of disparity to sub-pixel accuracy
	}

	/**
	 * Computes disparity from an initial disparity estimate. A pyramid is not required because of the
	 * initial disparity estimate. This function can be called iteratively to improve the estimate.
	 *
	 * @param left (Input) left rectified stereo image
	 * @param right (Input) right rectified stereo image
	 * @param disparityEst (Input) Initial disparity estimate used to compute MI
	 */
	public void process( GrayU8 left , GrayU8 right , GrayU8 disparityEst , int invalid) {
		InputSanityCheck.checkSameShape(left,right,disparityEst);
		disparity.reshape(left);

		// Compute mutual information model given the initial disparity estimate
		stereoMI.process(left, right, disparityMin, disparityEst, invalid);
		stereoMI.precomputeScaledCost(SgmDisparityCost.MAX_COST);

		// Compute the cost using mutual information
		sgmCost.process(left,right, disparityMin, disparityRange,costYXD);
		// Aggregate the cost along all the paths
		aggregation.process(costYXD,disparityMin);

		// Select the best disparity for each pixel given the cost
		selector.setMinDisparity(disparityMin); // TODO move to function below
		selector.select(costYXD,aggregation.getAggregated(),disparity);
	}

	// TODO remove need to compute U8 first
	public void subpixel( GrayU8 src , GrayF32 dst ) {
		dst.reshape(src);
		Planar<GrayU16> costYXD = aggregation.getAggregated();

		for (int y = 0; y < costYXD.getNumBands(); y++) {
			GrayU16 costXD = costYXD.getBand(y);
			for (int x = 0; x < costXD.height; x++) {
				int maxLocalDisparity = selector.maxLocalDisparity(x);
				int d = src.unsafe_get(x,y);
				float subpixel;
				if( d > 0 && d < maxLocalDisparity-1) {
					int c0 = costXD.unsafe_get(d-1,x);
					int c1 = costXD.unsafe_get(d  ,x);
					int c2 = costXD.unsafe_get(d+1,x);

					float offset = (float)(c0-c2)/(float)(2*(c0-2*c1+c2));
					subpixel = d + offset;
				} else {
					subpixel = d;
				}
				dst.set(x,y,subpixel);
			}
		}
	}

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

	public int getInvalidDisparity() {
		return selector.getInvalidDisparity();
	}

	public int getDisparityMin() {
		return disparityMin;
	}

	public void setDisparityMin(int disparityMin) {
		this.disparityMin = disparityMin;
	}

	public int getDisparityRange() {
		return disparityRange;
	}

	public void setDisparityRange(int disparityRange) {
		this.disparityRange = disparityRange;
	}

	public SgmDisparitySelector getSelector() {
		return selector;
	}
}
