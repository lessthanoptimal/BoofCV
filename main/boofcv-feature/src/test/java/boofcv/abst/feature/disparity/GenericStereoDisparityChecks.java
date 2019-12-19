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

package boofcv.abst.feature.disparity;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofTesting;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GenericStereoDisparityChecks<Image extends ImageBase<Image>, Disparity extends ImageGray<Disparity>>
{
	Random rand = new Random(234);

	int width = 80;
	int height = 60;

	double minPixelValue;
	double maxPixelValue;
	ImageType<Image> inputType;
	ImageType<Disparity> disparityType;

	Image left,right;

	public GenericStereoDisparityChecks( ImageType<Image> inputType , ImageType<Disparity> disparityType ) {
		this.inputType = inputType;
		this.disparityType = disparityType;

		if( inputType.getDataType().isInteger() ) {
			switch( inputType.getDataType().getNumBits() ) {
				case 8: minPixelValue=0; maxPixelValue=255; break;
				case 16: minPixelValue=0; maxPixelValue=2000; break;
				default: throw new RuntimeException("Unexpected input image");
			}
		} else {
			minPixelValue = -1;
			maxPixelValue =  1;
		}

		left = inputType.createImage(width,height);
		right = inputType.createImage(width,height);

		// Randomly fill images, but the right should have a constant offset of 8
		GImageMiscOps.fillUniform(left,rand,minPixelValue,maxPixelValue);
		GImageMiscOps.copy(8,0,0,0,width-8,height,left,right);
	}

	public abstract StereoDisparity<Image,Disparity> createAlg( int disparityMin , int disparityRange );

	/**
	 * Checks to see if it blows up, sets invalid as invalid, and marks at least some pixels as valid
	 */
	@Test
	void minimalSanityCheck() {
		for( int disparityMin : new int[]{0,1,5}) {
			for( int range : new int[]{1,20}) {
				StereoDisparity<Image,Disparity> alg = createAlg(disparityMin,range);
				alg.process(left,right);
				checkDisparity(disparityMin,range,alg.getDisparity());
			}
		}
	}

	@Test
	void runTwiceSameResult() {
		StereoDisparity<Image,Disparity> alg = createAlg(0,20);
		alg.process(left,right);

		Disparity expected = alg.getDisparity().clone();
		alg.process(left,right);

		BoofTesting.assertEquals(expected, alg.getDisparity(), 1e-4);
	}

	private void checkDisparity( int min , int range , Disparity disparity ) {
		int totalValid = 0;
		int total = height*(width-min);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < min; x++) {
				double v = GeneralizedImageOps.get(disparity,x,y);
				assertEquals(range,v,1e-4);
			}
			for (int x = min; x < width; x++) {
				double v = GeneralizedImageOps.get(disparity,x,y);
				if( v < range ) {
					totalValid++;
				}
			}
		}

		assertTrue(totalValid/(double)total >= 0.4);
	}

}
