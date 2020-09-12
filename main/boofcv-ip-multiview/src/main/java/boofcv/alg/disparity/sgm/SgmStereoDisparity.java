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

import boofcv.struct.image.*;

/**
 * Base class for SGM stereo implementations. It combines the cost computation, cost aggregation, and disparity
 * selector steps. Sub-pixel can be optionally computed afterwards.
 *
 * <p>NOTE: [1] suggests applying a median filter. This is not done by any of this class' children.</p>
 *
 * <p>[1] Hirschmuller, Heiko. "Stereo processing by semiglobal matching and mutual information."
 * IEEE Transactions on pattern analysis and machine intelligence 30.2 (2007): 328-341.</p>
 *
 * @author Peter Abeles
 */
public abstract class SgmStereoDisparity<T extends ImageBase<T>, C extends ImageBase<C>> {
	// Defines the disparity search range
	protected int disparityMin = 0;     // minimum disparity considered
	protected int disparityRange = 0;   // number of disparity values considered

	// These perform different steps in the SGM algorithm
	protected SgmDisparityCost<C> sgmCost;
	protected SgmCostAggregation aggregation = new SgmCostAggregation();
	protected SgmDisparitySelector selector;
	protected SgmHelper helper = new SgmHelper();

	// Cost tensor. See SgmDisparityCost
	protected Planar<GrayU16> costYXD = new Planar<>(GrayU16.class, 1, 1, 1);

	// Storage for found disparity
	protected GrayU8 disparity = new GrayU8(1, 1);

	protected SgmStereoDisparity( SgmDisparityCost<C> sgmCost, SgmDisparitySelector selector ) {
		this.sgmCost = sgmCost;
		this.selector = selector;
	}

	/**
	 * Computes disparity
	 *
	 * @param left (Input) left rectified stereo image
	 * @param right (Input) right rectified stereo image
	 */
	public abstract void process( T left, T right );

	// TODO remove need to compute U8 first
	public void subpixel( GrayU8 src, GrayF32 dst ) {
		dst.reshape(src);
		Planar<GrayU16> aggregatedYXD = aggregation.getAggregated();

		for (int y = 0; y < aggregatedYXD.getNumBands(); y++) {
			GrayU16 costXD = aggregatedYXD.getBand(y);
			for (int x = 0; x < disparityMin; x++) {
				dst.unsafe_set(x, y, disparityRange); // make as invalid
			}
			for (int x = disparityMin; x < costXD.height; x++) {
				int localMaxRange = helper.localDisparityRangeLeft(x);
				int d = src.unsafe_get(x, y);
				float subpixel;
				if (d > 0 && d < localMaxRange - 1) {
					int adjX = x - disparityMin; // see how cost tensor is defined
					int c0 = costXD.unsafe_get(d - 1, adjX);
					int c1 = costXD.unsafe_get(d, adjX);
					int c2 = costXD.unsafe_get(d + 1, adjX);

					float offset = (float)(c0 - c2)/(float)(2*(c0 - 2*c1 + c2));
					subpixel = d + offset;
				} else {
					subpixel = d;
				}
				dst.unsafe_set(x, y, subpixel);
			}
		}
	}

	public GrayU8 getDisparity() {
		return disparity;
	}

	public SgmDisparityCost<C> getSgmCost() {
		return sgmCost;
	}

	public SgmCostAggregation getAggregation() {
		return aggregation;
	}

	public Planar<GrayU16> getCostYXD() {
		return costYXD;
	}

	public int getInvalidDisparity() {
		return selector.getInvalidDisparity();
	}

	public int getDisparityMin() {
		return disparityMin;
	}

	public void setDisparityMin( int disparityMin ) {
		this.disparityMin = disparityMin;
	}

	public int getDisparityRange() {
		return disparityRange;
	}

	public void setDisparityRange( int disparityRange ) {
		this.disparityRange = disparityRange;
	}

	public SgmDisparitySelector getSelector() {
		return selector;
	}
}
