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

package boofcv.alg.disparity.block;

import boofcv.factory.disparity.DisparityError;
import boofcv.struct.image.ImageGray;

/**
 * Very basic algorithm for testing stereo disparity algorithms for correctness and employs a
 * "winner takes all" strategy for selecting the solution. No optimization
 * is done to improve performance and minimize cache misses. The advantage is that it can take in
 * any image type.
 *
 * @author Peter Abeles
 */
public class DisparityBlockMatchNaive<I extends ImageGray<I>>
		extends CommonDisparityBlockMatch<I> {

	public DisparityBlockMatchNaive( DisparityError errorType ) {
		super(errorType);
	}

	/**
	 * Compute SAD (Sum of Absolute Difference) error.
	 *
	 * @param leftX X-axis center left image
	 * @param rightX X-axis center left image
	 * @param centerY Y-axis center for both images
	 * @return Fit score for both regions.
	 */
	@Override
	protected double computeScore( int leftX, int rightX, int centerY ) {
		return computeScoreBlock(leftX, rightX, centerY);
	}
}
