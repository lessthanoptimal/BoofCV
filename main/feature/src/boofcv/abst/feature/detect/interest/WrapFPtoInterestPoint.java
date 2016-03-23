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

package boofcv.abst.feature.detect.interest;

import boofcv.alg.feature.detect.interest.FeaturePyramid;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.PyramidFloat;
import georegression.struct.point.Point2D_F64;

import java.util.List;


/**
 * Wrapper around {@link boofcv.alg.feature.detect.interest.FeaturePyramid} for {@link boofcv.abst.feature.detect.interest.InterestPointDetector}.
 *
 * @author Peter Abeles
 */
public class WrapFPtoInterestPoint<T extends ImageGray, D extends ImageGray> implements InterestPointDetector<T>{

	FeaturePyramid<T,D> detector;
	List<ScalePoint> location;
	PyramidFloat<T> ss;

	public WrapFPtoInterestPoint(FeaturePyramid<T,D> detector,
								 PyramidFloat<T> ss ) {
		this.detector = detector;
		this.ss = ss;
	}

	@Override
	public void detect(T input) {
		ss.process(input);

		detector.detect(ss);

		location = detector.getInterestPoints();
	}

	@Override
	public int getNumberOfFeatures() {
		return location.size();
	}

	@Override
	public Point2D_F64 getLocation(int featureIndex) {
		return location.get(featureIndex);
	}

	@Override
	public double getRadius(int featureIndex) {
		return location.get(featureIndex).scale;
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
