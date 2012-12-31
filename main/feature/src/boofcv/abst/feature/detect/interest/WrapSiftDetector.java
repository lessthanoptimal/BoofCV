/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.detect.interest.SiftDetector;
import boofcv.alg.feature.detect.interest.SiftImageScaleSpace;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;

/**
 * Wrapper around {@link SiftDetector} for {@link InterestPointDetector}.
 *
 * @author Peter Abeles
 */
public class WrapSiftDetector implements InterestPointDetector<ImageFloat32> {

	SiftImageScaleSpace ss;
	SiftDetector detector;

	public WrapSiftDetector( SiftDetector detector ,
							 SiftImageScaleSpace ss ) {
		this.detector = detector;
		this.ss = ss;
	}

	@Override
	public void detect(ImageFloat32 input) {

		// compute initial octave's scale-space
		ss.constructPyramid(input);
		ss.computeFeatureIntensity();

		detector.process(ss);
	}

	@Override
	public int getNumberOfFeatures() {
		return detector.getFoundPoints().size();
	}

	@Override
	public Point2D_F64 getLocation(int featureIndex) {
		return detector.getFoundPoints().get(featureIndex);
	}

	@Override
	public double getScale(int featureIndex) {
		return detector.getFoundPoints().get(featureIndex).scale;
	}

	@Override
	public double getOrientation(int featureIndex) {
		return 0;
	}

	@Override
	public boolean hasScale() {
		return true;
	}

	@Override
	public boolean hasOrientation() {
		return false;
	}
}
