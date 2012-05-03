/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity;

import boofcv.struct.image.ImageSingleBand;

/**
 * <p>
 * Selects the best disparity using a winner takes all strategy.  Then optionally can employ several different
 * techniques to filter out bad disparity values.  This is a base class which allows the output image type
 * to be specified by a child.
 * </p>
 *
 * <p>
 * Filters:<br>
 * <b>MaxError</b> is the largest error value the selected region can have.<br>
 * <b>right To Left</b> validates the disparity by seeing if the matched region on the right has the same region on
 * the left as its optimal solution, within tolerance.<br>
 * <b>texture</b> Tolerance for how similar the best region is to the second best. Lower values indicate greater
 * tolerance.  Reject if textureTol <= (C2-C1)/C1, where C2 = second best region score and C1 = best region score
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class SelectRectStandard <Array , T extends ImageSingleBand>
		implements DisparitySelect<Array,T>
{
	// output containing disparity
	protected T imageDisparity;
	// maximum disparity being checked
	protected int maxDisparity;
	// max allowed disparity at the current pixel
	protected int localMax;
	// radius and width of the region being compared
	protected int radiusX;
	protected int regionWidth;

	// maximum allowed error
	protected int maxError;
	// tolerance for right to left validation. if < 0 then it's disabled
	protected int rightToLeftTolerance;

	/**
	 * Configures tolerances
	 *
	 * @param maxError The maximum allowed error.  Note this is sum error and not per pixel error.
	 *                    Try (region width*height)*30.
	 * @param rightToLeftTolerance Tolerance for how difference the left to right associated values can be.  Try 6
	 * @param texture Tolerance for how similar optimal region is to other region.  Closer to zero is more tolerant.
	 *                Try 0.1
	 */
	public SelectRectStandard(int maxError, int rightToLeftTolerance, double texture) {
		this.maxError = maxError <= 0 ? Integer.MAX_VALUE : maxError;
		this.rightToLeftTolerance = rightToLeftTolerance;
		setTexture(texture);
	}

	public abstract void setTexture( double threshold );

	@Override
	public void configure(T imageDisparity, int maxDisparity , int radiusX ) {
		this.imageDisparity = imageDisparity;
		this.maxDisparity = maxDisparity;
		this.radiusX = radiusX;

		regionWidth = radiusX*2+1;
	}

	/**
	 * Sets the output to the specified disparity value.
	 *
	 * @param index Image pixel that is being set
	 * @param disparityValue disparity value
	 */
	protected abstract void setDisparity( int index , int disparityValue );

	/**
	 * Returns the maximum allowed disparity for a particular column in left to right direction,
	 * as limited by the image border.
	 */
	protected int maxDisparityAtColumnL2R( int col) {
		return 1+col-Math.max(0,col-maxDisparity+1);
	}
}
