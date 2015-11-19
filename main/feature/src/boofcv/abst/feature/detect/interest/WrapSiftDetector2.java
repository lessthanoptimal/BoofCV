/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.feature.detect.interest.SiftDetector2;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;

/**
 * Wrapper around {@link SiftDetector} for {@link InterestPointDetector}.
 *
 * @author Peter Abeles
 */
public class WrapSiftDetector2 implements InterestPointDetector<ImageFloat32> {


	SiftDetector2 detector;

	public WrapSiftDetector2(SiftDetector2 detector) {
		this.detector = detector;
	}

	@Override
	public void detect(ImageFloat32 input) {

		detector.process(input);
	}

	@Override
	public int getNumberOfFeatures() {
		return detector.getOctaveDetection().size();
	}

	@Override
	public Point2D_F64 getLocation(int featureIndex) {
		return detector.getOctaveDetection().get(featureIndex);
	}

	@Override
	public double getRadius(int featureIndex) {
		return detector.getOctaveDetection().get(featureIndex).scale;
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
