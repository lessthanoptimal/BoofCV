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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.struct.image.GrayU8;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestStereoMutualInformation {
	final int w = 20,h=30;
	final Random rand = new Random(234);

	@Test
	void computeMI() {
		fail("Implement");
	}

	/**
	 * Make sure it properly resets itself
	 */
	@Test
	void multipleCalls() {
		fail("implement");
	}

	@Test
	void computeJointHistogram() {
		GrayU8 left = new GrayU8(w,h);
		GrayU8 right = new GrayU8(w,h);
		GrayU8 disparity = new GrayU8(w,h); // Filled with zero. So each pixel is matched at same coordinate

		ImageMiscOps.fillUniform(left,rand,10,100);
		PixelMath.multiply(left,2.0,right);

		StereoMutualInformation alg = new StereoMutualInformation();
		alg.configureHistogram(250,250);
		alg.computeJointHistogram(left,right,disparity,500);

		// Histogram can only have non-zero values where col=2*row because the right has been scaled by a factor
		// of two
		int nonZero = 0;
		for (int yLeft = 0; yLeft < 251; yLeft++) { // left image is rows and right is columns
			for (int xRight = 0; xRight < 251; xRight++) {
				if( xRight != 2*yLeft || yLeft < 10 || yLeft > 100) {
					assertEquals(0, alg.histJoint.get(xRight, yLeft));
				} else {
					nonZero += alg.histJoint.get(xRight, yLeft);
				}
			}
		}
		// make sure the non-zero elements is more than 0
		assertEquals(w*h,nonZero);
	}

	/**
	 * Make sure invalid pixels are skipped
	 */
	@Test
	void computeJointHistogram_SkipInvalid() {
		GrayU8 left = new GrayU8(w,h);
		GrayU8 right = new GrayU8(w,h);
		GrayU8 disparity = new GrayU8(w,h);

		ImageMiscOps.fillUniform(left,rand,10,100);
		right.setTo(left);

		// set the top half of the image to have invalid disparities
		int invalid = 255;
		ImageMiscOps.fillRectangle(disparity,invalid,0,0,w,h/2);

		StereoMutualInformation alg = new StereoMutualInformation();
		alg.configureHistogram(250,250);
		alg.computeJointHistogram(left,right,disparity,invalid);

		int found = ImageStatistics.sum(alg.histJoint);
		assertEquals(w*h/2,found);
	}

	@Test
	void computeProbabilities() {
		StereoMutualInformation alg = new StereoMutualInformation();
		alg.setEps(0); // zero EPS to make these checks easier
		alg.configureHistogram(250,250);
		// fill just the right column with the same value. This will have a specific structure
		for (int i = 0; i < 251; i++) {
			alg.histJoint.set(250,i,40);
		}
		alg.computeProbabilities();

		for (int i = 0; i < 250; i++) {
			assertEquals(0.0,alg.entropyRight.data[i], UtilEjml.TEST_F32);
			assertEquals(1.0/251,alg.entropyLeft.data[i], UtilEjml.TEST_F32);
		}
		assertEquals(1.0,alg.entropyRight.data[250], UtilEjml.TEST_F32);
		assertEquals(1.0/251,alg.entropyLeft.data[250], UtilEjml.TEST_F32);

		for (int i = 0; i < 251; i++) {
			for (int j = 0; j < 251; j++) {
				if( j < 250 )
					assertEquals(0.0,alg.entropyJoint.get(j,i), UtilEjml.TEST_F32);
				else
					assertEquals(1.0/251.0,alg.entropyJoint.get(j,i), UtilEjml.TEST_F32);
			}
		}
	}

	/**
	 * See if zeros are handled well
	 */
	@Test
	void computeEntropy_Zeros() {
		StereoMutualInformation alg = new StereoMutualInformation();
//		alg.setEps(0); // This will cause the test to fail
		alg.configureHistogram(250,250);

		// only a few non-zero values
		for (int i = 0; i < 251; i++) {
			alg.histJoint.set(250,i,40);
		}
		alg.computeProbabilities();
		alg.computeEntropy();

		// make sure all numbers are valid
		for (int i = 0; i < 251; i++) {
			assertFalse(UtilEjml.isUncountable(alg.entropyLeft.get(i,0)));
			assertFalse(UtilEjml.isUncountable(alg.entropyRight.get(i,0)));

			for (int j = 0; j < 251; j++) {
				assertFalse(UtilEjml.isUncountable(alg.entropyJoint.get(j,i)));
			}
		}
	}

	@Test
	void scalePixelValue() {
		StereoMutualInformation alg = new StereoMutualInformation();

		// test using hand computed values
		alg.configureHistogram(250,250);
		assertEquals(100,alg.scalePixelValue(100));
		assertEquals(250,alg.scalePixelValue(250));

		// 12-bit gray to 8-bit gray
		alg.configureHistogram(4095,255);
		assertEquals(255,alg.scalePixelValue(4095));
		assertEquals(32,alg.scalePixelValue(518));
		assertEquals(68,alg.scalePixelValue(1099));
	}
}