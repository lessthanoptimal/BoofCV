/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.struct.BoofDefaults;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F64;

import java.util.List;


/**
 * Wrapper around {@link boofcv.alg.feature.detect.interest.FastHessianFeatureDetector} for {@link InterestPointDetector}.
 *
 * @author Peter Abeles
 */
public class WrapFHtoInterestPoint<T extends ImageBase> implements InterestPointDetector<T> {

	FastHessianFeatureDetector<T> detector;
	List<ScalePoint> location;
	T integral;

	public WrapFHtoInterestPoint(FastHessianFeatureDetector<T> detector) {
		this.detector = detector;
	}

	@Override
	public void detect(T input) {
		if( integral != null ) {
			integral.reshape(input.width,input.height);
		}

		integral = GIntegralImageOps.transform(input,integral);

		detector.detect(integral);

		location = detector.getFoundPoints();
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
	public double getScale(int featureIndex) {
		return location.get(featureIndex).scale;
	}

	@Override
	public double getOrientation(int featureIndex) {
		return 0;
	}

	@Override
	public double getCanonicalRadius() {
		return BoofDefaults.SCALE_SPACE_CANONICAL_RADIUS;
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
