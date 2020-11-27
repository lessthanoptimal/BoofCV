/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.disparity.sgm.cost;

import boofcv.alg.disparity.sgm.CommonSgmChecks;
import boofcv.alg.disparity.sgm.SgmDisparityCost;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestStereoMutualInformation extends CommonSgmChecks<GrayU8> {

	TestStereoMutualInformation() {
		super(30, 40, ImageType.SB_U8);
	}

	@Test
	void cost() {
		// Create two images that are offset by a constant disparity
		GrayU8 disparity = new GrayU8(width, height);

		// Set the disparity to 10
		ImageMiscOps.fill(disparity, 10);
		ImageMiscOps.fillRectangle(disparity, 255, 0, 0, 10, height);

		// Compute MI with no correlation between left and right images
		ImageMiscOps.fillUniform(left, rand, 125, 150);
		ImageMiscOps.fillUniform(right, rand, 80, 125);

		StereoMutualInformation alg = new StereoMutualInformation();
		alg.process(left, right, 0, disparity, 255);
		float costIncorrect = computeCost(left, right, null, alg);

		// Compute MI with correct disparity
		ImageMiscOps.copy(10, 0, 0, 0, width - 10, height, left, right);
		PixelMath.multiply(right, 0.5, right);
		alg.process(left, right, 0, disparity, 255);
		float costCorrect = computeCost(left, right, disparity, alg);

		// The cost should be much less with correct disparity
		// not sure how to quantiyf much less since positive and negative values are allowed
		assertTrue(costCorrect < costIncorrect);
	}

	@Test
	void costScaled() {
		// Create two images that are offset by a constant disparity
		GrayU8 disparity = new GrayU8(width, height);

		// Set the disparity to 10
		ImageMiscOps.fill(disparity, 10);
		ImageMiscOps.fillRectangle(disparity, 255, 0, 0, 10, height);

		// Compute MI with no correlation between left and right images
		ImageMiscOps.fillUniform(left, rand, 125, 150);
		ImageMiscOps.fillUniform(right, rand, 80, 125);

		StereoMutualInformation alg = new StereoMutualInformation();
		alg.process(left, right, 0, disparity, 255);
		alg.precomputeScaledCost(2047);

		int costIncorrect = computeCostScaled(left, right, null, alg);

		// Compute MI with correct disparity
		ImageMiscOps.copy(10, 0, 0, 0, width - 10, height, left, right);
		PixelMath.multiply(right, 0.5, right);
		alg.process(left, right, 0, disparity, 255);
		alg.precomputeScaledCost(2047);
		int costCorrect = computeCostScaled(left, right, disparity, alg);

		// The cost should be much less with correct disparity
		// Is this enough of a difference?
		assertTrue(costCorrect*1.5 < costIncorrect);
	}

	/**
	 * Given that it's know that the left and right images have the same histograms, see if the same pixel
	 * values have the minimum costs
	 */
	@Test
	void cost_minimum() {
		int invalid = 10;
		int maxValue = 20;

		renderStereoRandom(0, maxValue - 1, 8, invalid);
		StereoMutualInformation alg = new StereoMutualInformation();
		alg.configureHistogram(maxValue);
		alg.configureSmoothing(1);
		alg.process(left, right, 0, disparityTruth, invalid);
		alg.precomputeScaledCost(SgmDisparityCost.MAX_COST);


		// Get a list of values in the left and right image
		// Mutual information isn't designed to handle cases where you attempt to match up with a non-existing pixel
		DogArray_I32 valuesLeft = new DogArray_I32();
		DogArray_I32 valuesRight = new DogArray_I32();

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (disparityTruth.get(x, y) == invalid)
					continue;
				if (!valuesLeft.contains(left.get(x, y)))
					valuesLeft.add(left.get(x, y));
				if (!valuesRight.contains(right.get(x, y)))
					valuesRight.add(right.get(x, y));
			}
		}

		// For every value that appeared in the left image make sure the same value in the right minimizes it
		// dont' consider values not in the left image since those are kinda undefined
		for (int i = 0; i < valuesLeft.size; i++) {
			int bestScore = Integer.MAX_VALUE;
			int bestVR = -1;
			int vl = valuesLeft.get(i);
			for (int j = 0; j < valuesRight.size; j++) {
				int c = alg.costScaled(vl, valuesRight.get(j));
				if (c < bestScore) {
					bestScore = c;
					bestVR = valuesRight.get(j);
				}
			}
//			System.out.println("left="+vl+" right="+bestVR+"  score="+bestScore);
			assertEquals(vl, bestVR);
		}
	}

	private float computeCost( GrayU8 left, GrayU8 right, GrayU8 disparity, StereoMutualInformation alg ) {
		float cost = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int d = disparity != null ? disparity.get(x, y) : 0;
				if (d == 255)
					continue;
				int xx = x - d;
				if (xx >= 0)
					cost += alg.cost(left.get(x, y), right.get(xx, y));
			}
		}
		return cost;
	}

	private int computeCostScaled( GrayU8 left, GrayU8 right, GrayU8 disparity, StereoMutualInformation alg ) {
		int cost = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int d = disparity != null ? disparity.get(x, y) : 0;
				if (d == 255)
					continue;
				int xx = x - d;
				if (xx >= 0)
					cost += alg.costScaled(left.get(x, y), right.get(xx, y));
			}
		}
		return cost;
	}

	/**
	 * Make sure it properly resets itself
	 */
	@Test
	void multipleCalls() {
		// Create arbitrary inputs
		GrayU8 disparity = new GrayU8(width, height);

		ImageMiscOps.fillUniform(left, rand, 10, 220);
		ImageMiscOps.fillUniform(right, rand, 10, 220);
		ImageMiscOps.fillUniform(disparity, rand, 0, 8);
		ImageMiscOps.fillRectangle(disparity, 255, 0, 0, 8, height); // avoid going out of bounds

		StereoMutualInformation alg = new StereoMutualInformation();
		alg.process(left, right, 0, disparity, 255);
		float cost0 = computeCost(left, right, disparity, alg);

		alg.process(left, right, 0, disparity, 255);
		float cost1 = computeCost(left, right, disparity, alg);

		// the cost should be identical
		assertEquals(cost0, cost1);
	}

	@Test
	void computeJointHistogram() {
		GrayU8 disparity = new GrayU8(width, height); // Filled with zero. So each pixel is matched at same coordinate

		ImageMiscOps.fillUniform(left, rand, 10, 100);
		PixelMath.multiply(left, 2.0, right);

		StereoMutualInformation alg = new StereoMutualInformation();
		alg.configureHistogram(251);
		alg.computeJointHistogram(left, right, 0, disparity, 500);

		// Histogram can only have non-zero values where col=2*row because the right has been scaled by a factor
		// of two
		int nonZero = 0;
		for (int yLeft = 0; yLeft < 251; yLeft++) { // left image is rows and right is columns
			for (int xRight = 0; xRight < 251; xRight++) {
				if (xRight != 2*yLeft || yLeft < 10 || yLeft > 100) {
					assertEquals(0, alg.histJoint.get(xRight, yLeft));
				} else {
					nonZero += alg.histJoint.get(xRight, yLeft);
				}
			}
		}
		// make sure the non-zero elements is more than 0
		assertEquals(width*height, nonZero);
	}

	/**
	 * Make sure invalid pixels are skipped
	 */
	@Test
	void computeJointHistogram_SkipInvalid() {
		GrayU8 disparity = new GrayU8(width, height);

		ImageMiscOps.fillUniform(left, rand, 10, 100);
		right.setTo(left);

		// set the top half of the image to have invalid disparities
		int invalid = 255;
		ImageMiscOps.fillRectangle(disparity, invalid, 0, 0, width, height/2);

		StereoMutualInformation alg = new StereoMutualInformation();
		alg.configureHistogram(251);
		alg.computeJointHistogram(left, right, 0, disparity, invalid);

		int found = ImageStatistics.sum(alg.histJoint);
		assertEquals(width*height/2, found);
	}

	@Test
	void computeProbabilities() {
		StereoMutualInformation alg = new StereoMutualInformation();
		alg.configureHistogram(251);
		// fill just the right column with the same value. This will have a specific structure
		for (int i = 0; i < 251; i++) {
			alg.histJoint.set(250, i, 40);
		}
		alg.computeProbabilities();

		for (int i = 0; i < 250; i++) {
			assertEquals(0.0, alg.entropyRight.data[i], UtilEjml.TEST_F32);
			assertEquals(1.0/251, alg.entropyLeft.data[i], UtilEjml.TEST_F32);
		}
		assertEquals(1.0, alg.entropyRight.data[250], UtilEjml.TEST_F32);
		assertEquals(1.0/251, alg.entropyLeft.data[250], UtilEjml.TEST_F32);

		for (int i = 0; i < 251; i++) {
			for (int j = 0; j < 251; j++) {
				if (j < 250)
					assertEquals(0.0, alg.entropyJoint.get(j, i), UtilEjml.TEST_F32);
				else
					assertEquals(1.0/251.0, alg.entropyJoint.get(j, i), UtilEjml.TEST_F32);
			}
		}
	}

	/**
	 * See if zeros are handled well
	 */
	@Test
	void computeEntropy_Zeros() {
		StereoMutualInformation alg = new StereoMutualInformation();
		alg.configureHistogram(251);

		// only a few non-zero values
		for (int i = 0; i < 251; i++) {
			alg.histJoint.set(250, i, 40);
		}
		alg.computeProbabilities();
		alg.computeEntropy();

		// make sure all numbers are valid
		for (int i = 0; i < 251; i++) {
			assertFalse(UtilEjml.isUncountable(alg.entropyLeft.get(i, 0)));
			assertFalse(UtilEjml.isUncountable(alg.entropyRight.get(i, 0)));

			for (int j = 0; j < 251; j++) {
				assertFalse(UtilEjml.isUncountable(alg.entropyJoint.get(j, i)));
			}
		}
	}
}
