/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.interest;

import boofcv.alg.feature.detect.intensity.FastCornerDetector;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageGray;

/**
 * @author Peter Abeles
 */
public class WrapFastToPointDetector<T extends ImageGray<T>>
	implements PointDetector<T>
{
	FastCornerDetector<T> detector;

	public WrapFastToPointDetector(FastCornerDetector<T> detector ) {
		this.detector = detector;
	}

	@Override
	public void process(T image) {
		detector.process(image);
	}

	@Override
	public int totalSets() {
		return 2;
	}

	@Override
	public QueueCorner getPointSet(int which) {
		if( which == 0 )
			return detector.getCornersLow();
		else if( which == 1 ) {
			return detector.getCornersHigh();
		} else {
			throw new IllegalArgumentException("Invalid set request");
		}
	}

	public FastCornerDetector<T> getDetector() {
		return detector;
	}
}
