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

package boofcv.alg.disparity.sgm;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;

public class CommonSgmChecks<T extends ImageGray<T>> extends BoofStandardJUnit {
	protected int width, height;

	ImageType<T> imageType;

	protected T left;
	protected T right;
	protected GrayU8 disparityTruth = new GrayU8(1, 1);

	protected CommonSgmChecks( int width, int height, ImageType<T> imageType ) {
		this.imageType = imageType;
		this.width = width;
		this.height = height;
		left = imageType.createImage(width, height);
		right = imageType.createImage(width, height);
		disparityTruth.reshape(width, height);
	}

	/**
	 * Randomly fills in the left image and copies it by a fixed amount into the right
	 */
	protected void renderStereoRandom( int min, int max, int disparity, int invalid ) {
		GImageMiscOps.fillUniform(left, rand, min, max);
		GImageMiscOps.fill(right, 0);
		GImageMiscOps.copy(disparity, 0, 0, 0, left.width - disparity, left.height, left, right);

		GImageMiscOps.fill(disparityTruth, invalid);
		GImageMiscOps.fillRectangle(disparityTruth, disparity, disparity, 0, left.width - disparity, left.height);
	}

	/**
	 * Renders a stereo pair with a step gradient and a fixed constant disparity
	 */
	protected void renderStereoStep( int d, int invalid ) {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				// Create a step function
				int valueR = y*3 + ((x + d)/6)*4;
				int valueL = y*3 + (x/6)*4;
				GeneralizedImageOps.set(left, x, y, valueL);
				GeneralizedImageOps.set(right, x, y, valueR);
			}
		}
		ImageMiscOps.fill(disparityTruth, d);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < d; x++) {
				disparityTruth.set(x, y, invalid);
			}
		}
	}

	protected void renderStereoGradient( int d, int invalid ) {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				// Create a step function
				int valueR = y + x + d;
				int valueL = y + x;
				GeneralizedImageOps.set(left, x, y, valueL);
				GeneralizedImageOps.set(right, x, y, valueR);
			}
		}
		ImageMiscOps.fill(disparityTruth, d);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < d; x++) {
				disparityTruth.set(x, y, invalid);
			}
		}
	}
}
