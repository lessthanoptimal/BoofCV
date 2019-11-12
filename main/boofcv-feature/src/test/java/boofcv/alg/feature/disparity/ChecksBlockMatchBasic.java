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

package boofcv.alg.feature.disparity;

import boofcv.abst.feature.disparity.StereoDisparity;
import boofcv.alg.feature.disparity.block.DisparityBlockMatchNaive;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.disparity.ConfigureDisparityBM;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofTesting;
import org.junit.jupiter.api.Test;

import java.util.Random;

/**
 * Test the entire block matching pipeline against a naive implementation
 *
 * @author Peter Abeles
 */
public abstract class ChecksBlockMatchBasic<T extends ImageBase<T>> {

	double maxPixel = 255;

	T left,right;
	GrayU8 expected = new GrayU8(1,1);

	ChecksBlockMatchBasic(ImageType<T> imageType ) {
		left = imageType.createImage(1,1);
		right = imageType.createImage(1,1);
	}

	public abstract DisparityBlockMatchNaive<T> createNaive( int blockRadius , int minDisparity , int maxDisparity );

	public abstract StereoDisparity<T,GrayU8> createAlg( int blockRadius , int minDisparity , int maxDisparity );

	@Test
	void compare() {
//		BoofConcurrency.USE_CONCURRENT=false;
		compare(25,20,2,0,10);
		compare(25,20,2,3,10);
		compare(10,15,1,5,6);

//		compare(400,300,2,0,120);
	}

	void compare( int width , int height , int radius , int minDisparity , int maxDisparity ) {
		Random rand = new Random(234);
		expected.reshape(width,height);
		left.reshape(width,height);
		right.reshape(width,height);

		// Create two images with gradient. This should have a clear best fit and not be nearl as sensitive
		// to noise as a random fill that can have multiple very similar solutions
		for (int j = 0; j < height; j++) {
			for (int x = 0; x < width; x++) {
				GeneralizedImageOps.set((ImageGray)left,x,j,x+j);
				GeneralizedImageOps.set((ImageGray)right,x,j,x+j+2);
			}
		}

		DisparityBlockMatchNaive<T> naive = createNaive(radius,minDisparity,maxDisparity);
		StereoDisparity<T, GrayU8> alg = createAlg(radius,minDisparity,maxDisparity);

		naive.process(left,right,expected);
		alg.process(left,right);

		BoofTesting.assertEquals(expected,alg.getDisparity(),1e-4);
	}

	public static ConfigureDisparityBM createConfigBasicBM(int blockRadius, int minDisparity, int maxDisparity) {
		ConfigureDisparityBM config = new ConfigureDisparityBM();
		config.regionRadiusX = config.regionRadiusY = blockRadius;
		config.minDisparity = minDisparity;
		config.maxDisparity = maxDisparity;
		// turn off all validation
		config.texture = 0;
		config.validateRtoL = -1;
		config.subpixel = false;
		config.maxPerPixelError = -1;
		return config;
	}
}
