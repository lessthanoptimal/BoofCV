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

package boofcv.alg.feature.disparity.block;

import boofcv.struct.image.ImageGray;

/**
 * <p>
 * Selects the disparity with the smallest error, which is known as the winner takes all (WTA) strategy.
 * Optionally several different techniques can be used to filter out bad disparity values.  This is a base class
 * for algorithms which implement this same "standard" algorithm on different data types.
 * </p>
 *
 * <p>
 * Validation Filters:<br>
 * <b>MaxError</b> is the largest error value the selected region can have.<br>
 * <b>right To Left</b> validates the disparity by seeing if the matched region on the right has the same region on
 * the left as its optimal solution, within tolerance.<br>
 * <b>texture</b> Tolerance for how similar the best region is to the second best. Lower values indicate greater
 * tolerance.  Reject if textureTol &le; (C2-C1)/C1, where C2 = second best region score and C1 = best region score
 * </p>
 *
 * <p>
 * This implementation is not based off of any individual paper but ideas commonly expressed in several different
 * sources.  A good study and summary of similar algorithms can be found in:<br>
 * [1] Wannes van der Mark and Dariu M. Gavrila, "Real-Time Dense Stereo for Intelligent Vehicles"
 * IEEE TRANSACTIONS ON INTELLIGENT TRANSPORTATION SYSTEMS, VOL. 7, NO. 1, MARCH 2006
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class SelectDisparityWithChecksWta<Array , DI extends ImageGray<DI>>
		implements DisparitySelect<Array, DI>
{
	// Number of unique values for texture
	public static final int DISCRETIZER = 10000;

	// output containing disparity
	protected DI imageDisparity;
	// minimum and maximum disparity that will be checked
	protected int disparityMin;
	protected int disparityMax;
	protected int disparityRange;
	// value that an invalid pixel will be assigned
	protected int invalidDisparity;
	// max allowed disparity at the current pixel
	protected int localRange;
	// radius and width of the region being compared
	protected int radiusX;
	protected int regionWidth;

	// maximum allowed error
	protected int maxError;
	// tolerance for right to left validation. if < 0 then it's disabled
	protected int rightToLeftTolerance;

	// type of disparity image
	protected Class<DI> disparityType;

	/**
	 * Configures tolerances
	 *
	 * @param maxError The maximum allowed error.  Note this is sum error and not per pixel error.
	 *                 Try (region width*height)*30.
	 * @param rightToLeftTolerance Tolerance for how difference the left to right associated values can be.  Try 6
	 * @param texture Tolerance for how similar optimal region is to other region.  Disable with a value &le; 0.
	 *                Closer to zero is more tolerant. Try 0.1
	 */
	public SelectDisparityWithChecksWta(int maxError, int rightToLeftTolerance, double texture,Class<DI> disparityType) {
		this.maxError = maxError <= 0 ? Integer.MAX_VALUE : maxError;
		this.rightToLeftTolerance = rightToLeftTolerance;
		this.disparityType = disparityType;
		setTexture(texture);
	}

	public abstract void setTexture( double threshold );

	@Override
	public void configure(DI imageDisparity, int disparityMin , int disparityMax , int radiusX ) {
		this.imageDisparity = imageDisparity;
		this.disparityMin = disparityMin;
		this.disparityMax = disparityMax;
		this.radiusX = radiusX;

		disparityRange = disparityMax-disparityMin+1;
		regionWidth = radiusX*2+1;
		invalidDisparity = disparityRange;

		if( invalidDisparity > (int)imageDisparity.getDataType().getMaxValue()-1 )
			throw new IllegalArgumentException("Max range exceeds maximum value in disparity image. v="+invalidDisparity);
	}

	/**
	 * Sets the output to the specified disparity value.
	 *
	 * @param index Image pixel that is being set
	 * @param disparityValue disparity value
	 */
	protected abstract void setDisparity( int index , int disparityValue );

	protected abstract void setDisparityInvalid( int index );

	/**
	 * Returns the maximum allowed disparity for a particular column in left to right direction,
	 * as limited by the image border.
	 */
	protected int disparityMaxAtColumnL2R( int col) {
		return Math.min(col,disparityMax);
	}

	/**
	 * For debugging purposes only
	 */
	public void setLocalDisparityMax(int value) {
		localRange = value;
	}

	@Override
	public Class<DI> getDisparityType() {
		return disparityType;
	}
}
