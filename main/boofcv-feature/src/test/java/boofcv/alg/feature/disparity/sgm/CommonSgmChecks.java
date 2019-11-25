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
import boofcv.struct.image.GrayU8;

import java.util.Random;

public class CommonSgmChecks {
	Random rand = new Random(234);
	int width,height;

	protected GrayU8 left  = new GrayU8(1,1);
	protected GrayU8 right = new GrayU8(1,1);
	protected GrayU8 disparityTruth = new GrayU8(1,1);

	CommonSgmChecks( int width , int height ) {
		this.width = width;
		this.height = height;
		left.reshape(width,height);
		right.reshape(width,height);
		disparityTruth.reshape(width,height);
	}

	/**
	 * Renders a stereo pair with a step gradient and a fixed constant disparity
	 */
	void renderStereoStep( int d , int invalid ) {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				// Create a step function
				int valueR = y*3 + ((x+d)/6)*4;
				int valueL = y*3 + (x/6)*4;
				right.set(x,y, valueR);
				left.set(x,y, valueL);
			}
		}
		ImageMiscOps.fill(disparityTruth,d);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < d; x++) {
				disparityTruth.set(x,y,invalid);
			}
		}
	}

	void renderStereoGradient( int d , int invalid ) {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				// Create a step function
				int valueR = y + x+d;
				int valueL = y + x;
				right.set(x,y, valueR);
				left.set(x,y, valueL);
			}
		}
		ImageMiscOps.fill(disparityTruth,d);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < d; x++) {
				disparityTruth.set(x,y,invalid);
			}
		}
	}
}
