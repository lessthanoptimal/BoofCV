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

package boofcv.alg.feature.disparity.impl;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageSingleBand;

import static org.junit.Assert.assertEquals;

/**
 * Provides a serise of simple tests that check basic functionality at computing image disparity
 *
 * @author Peter Abeles
 */
public abstract class BasicDisparityTests<T extends ImageSingleBand, D extends  ImageSingleBand> {
	T left;
	T right;

	// image size
	int w = 50;
	int h = 60;

	int maxDisparity = 40;

	public BasicDisparityTests( Class<T> imageType ) {
		left = GeneralizedImageOps.createSingleBand(imageType,w,h);
		right = GeneralizedImageOps.createSingleBand(imageType,w,h);

		initialize(maxDisparity);
	}

	public abstract void initialize( int maxDisparity );

	public abstract int getBorderX();

	public abstract int getBorderY();

	public abstract D computeDisparity( T left , T right );

	/**
	 * Set the intensity values to have a gradient and see if it generates the correct
	 * solution
	 */
	public void checkGradient() {

		int disparity = 5;

		for( int y = 0; y < h; y++ ) {
			for( int x = 0; x < w; x++ ) {
				GeneralizedImageOps.set(left,x,y,10+x+y);
				GeneralizedImageOps.set(right,x,y,10+x+disparity+y);
			}
		}

		D output = computeDisparity(left,right);

		int borderX = getBorderX();
		int borderY = getBorderY();

		for( int y = borderY; y < h-borderY; y++ ) {
			for( int x = borderX+disparity; x < w-borderX; x++ ) {
				double found = GeneralizedImageOps.get(output,x,y);
				assertEquals("x = "+x+" y = "+y,disparity,found,1e-8);
			}
		}
	}

}
