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

package boofcv.alg.feature.detect.extract;

import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;


/**
 * All pixels which have an intensity above the specified threshold are considered to be features.  This will tend
 * to produce poor results because clusters are values will be above threshold.
 *
 * @author Peter Abeles
 */
public class ThresholdCornerExtractor {
	private float thresh;

	public ThresholdCornerExtractor( float thresh ) {
		this.thresh = thresh;
	}

	public ThresholdCornerExtractor() {
	}

	/**
	 * Selects pixels as corners which are above the threshold.
	 */
	public void process(GrayF32 intensity, QueueCorner corners ) {
		corners.reset();

		float data[] = intensity.data;

		for( int y = 0; y < intensity.height; y++ ) {
			int startIndex = intensity.startIndex + y*intensity.stride;
			int endIndex = startIndex + intensity.width;

			for( int index = startIndex; index < endIndex; index++ ) {
				if( data[index] > thresh ) {
					int x = index-startIndex;
					corners.add(x,y);
				}
			}
		}
	}

	public float getThreshold() {
		return thresh;
	}

	public void setThreshold(float threshold) {
		this.thresh = threshold;
	}
}
