/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.ImageGray;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * Provides a series of simple tests that check basic functionality at computing image disparity
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public abstract class BasicDisparityTests<Image extends ImageGray, Disparity extends ImageGray> {
	Image left;
	Image right;

	// image size
	int w = 50;
	int h = 60;

	int maxDisparity = 40;

	Random rand = new Random();

	public BasicDisparityTests( Class<Image> imageType ) {
		left = GeneralizedImageOps.createSingleBand(imageType,w,h);
		right = GeneralizedImageOps.createSingleBand(imageType,w,h);
	}

	public abstract void initialize( int minDisparity , int maxDisparity );

	public abstract int getBorderX();

	public abstract int getBorderY();

	public abstract Disparity computeDisparity( Image left , Image right );

	public void allChecks() {
		checkGradient();
		checkMinimumDisparity();
	}

	/**
	 * Set the intensity values to have a gradient and see if it generates the correct
	 * solution
	 */
	public void checkGradient() {
		initialize(0,maxDisparity);

		int disparity = 5;

		for( int y = 0; y < h; y++ ) {
			for( int x = 0; x < w; x++ ) {
				GeneralizedImageOps.set(left,x,y,10+x+y);
				GeneralizedImageOps.set(right,x,y,10+x+disparity+y);
			}
		}

		Disparity output = computeDisparity(left,right);

		int borderX = getBorderX();
		int borderY = getBorderY();

		for( int y = borderY; y < h-borderY; y++ ) {
			// borders should be zero since they are not modified
			for( int x = 0; x < borderX; x++ ) {
				double found = GeneralizedImageOps.get(output,x,y);
				assertEquals("x = "+x+" y = "+y,0,found,1e-8);
			}
			for( int x = w-borderX; x < w; x++ ) {
				double found = GeneralizedImageOps.get(output,x,y);
				assertEquals("x = "+x+" y = "+y,0,found,1e-8);
			}

			// check the inside image
			for( int x = borderX+disparity; x < w-borderX; x++ ) {
				double found = GeneralizedImageOps.get(output,x,y);
				assertEquals("x = "+x+" y = "+y,disparity,found,1e-8);
			}
		}
	}

	/**
	 * Set the minimum disparity to a non-zero value and see if it has the expected results
	 */
	public void checkMinimumDisparity() {
		int disparity = 4;
		int minDisparity = disparity+2;
		initialize(disparity+2,maxDisparity);

		for( int y = 0; y < h; y++ ) {
			for( int x = 0; x < w; x++ ) {
				GeneralizedImageOps.set(left,x,y,10+x+y);
				GeneralizedImageOps.set(right,x,y,10+x+disparity+y);
			}
		}

		Disparity output = computeDisparity(left,right);

		int borderX = getBorderX();
		int borderY = getBorderY();

		for( int y = borderY; y < h-borderY; y++ ) {
			// borders should be zero since they are not modified
			for( int x = 0; x < borderX+minDisparity; x++ ) {
				double found = GeneralizedImageOps.get(output,x,y);
				assertEquals("x = "+x+" y = "+y,0,found,1e-8);
			}
			for( int x = w-borderX; x < w; x++ ) {
				double found = GeneralizedImageOps.get(output,x,y);
				assertEquals("x = "+x+" y = "+y,0,found,1e-8);
			}

			// check inside image
			for( int x = borderX+minDisparity; x < w-borderX; x++ ) {
				double found = GeneralizedImageOps.get(output,x,y) + minDisparity;
				// the minimum disparity should  be the closest match
				assertEquals("x = "+x+" y = "+y,minDisparity,found,1e-8);
			}
		}
	}

}
