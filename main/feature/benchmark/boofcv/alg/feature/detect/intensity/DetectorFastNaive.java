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

package boofcv.alg.feature.detect.intensity;

import boofcv.misc.DiscretizedCircle;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayU8;

/**
 * A slow but easy to understand correct implementations.  Has been compared against the
 * original implementation by Edward Rosten.
 *
 * @author Peter Abeles
 */
public class DetectorFastNaive {
	// size of the circle being examined
	private int radius;
	// how many continuous points in a row it needs to detect to be a corner
	private int minCont;
	// difference in pixel intensity
	private int b;

	// list of pixels that might be corners.
	private QueueCorner candidates = new QueueCorner(10);

	public DetectorFastNaive(int radius, int minCont, int b) {
		this.radius = radius;
		this.minCont = minCont;
		this.b = b;
	}

	public void process( GrayU8 image ) {
		candidates.reset();

		// relative offsets of pixel locations in a circle
		int []offsets = DiscretizedCircle.imageOffsets(radius, image.stride);

		for( int y = radius; y < image.height-radius; y++ ) {
			for( int x = radius; x < image.width-radius;x++ ) {
				int index = image.startIndex + y*image.stride + x;

				int centerPixel = image.data[index] & 0xFF;

				int lower = centerPixel - b;
				int upper = centerPixel + b;

				boolean isCorner = false;

				for( int i = 0; i < offsets.length && !isCorner; i++ ) {
					int v = image.data[index+offsets[i]] & 0xFF;
					if( v < lower ) {
						isCorner = true;
						for( int j = 1; j < minCont; j++ ) {
							int a = (i+j) % offsets.length;

							v = image.data[index+offsets[a]] & 0xFF;
							if( v >= lower ) {
								isCorner = false;
								break;
							}
						}
					}
				}

				for( int i = 0; i < offsets.length && !isCorner; i++ ) {
					int v = image.data[index+offsets[i]] & 0xFF;
					if( v > upper ) {
						isCorner = true;
						for( int j = 1; j < minCont; j++ ) {
							int a = (i+j) % offsets.length;

							v = image.data[index+offsets[a]] & 0xFF;
							if( v <= upper ) {
								isCorner = false;
								break;
							}
						}
					}
				}

				if( isCorner ) {
					candidates.grow().set(x,y);
				}
			}
		}
	}

	public QueueCorner getCandidates() {
		return candidates;
	}
}
