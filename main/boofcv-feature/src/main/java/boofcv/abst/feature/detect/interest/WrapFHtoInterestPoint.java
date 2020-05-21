/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.BoofDefaults;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;

import java.util.List;


/**
 * Wrapper around {@link boofcv.alg.feature.detect.interest.FastHessianFeatureDetector} for {@link InterestPointDetector}.
 *
 * @author Peter Abeles
 */
public class WrapFHtoInterestPoint<T extends ImageGray<T>, II extends ImageGray<II>> implements InterestPointDetector<T> {

	// detects the feature's location and scale
	FastHessianFeatureDetector<II> detector;
	List<ScalePoint> location;
	II integral;
	ImageType<T> inputType;

	public WrapFHtoInterestPoint(FastHessianFeatureDetector<II> detector, Class<T> inputType ) {
		this.detector = detector;
		this.inputType = ImageType.single(inputType);
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
	public int getNumberOfSets() {
		return 1;
	}

	@Override
	public int getSet(int index) {
		return 0; // For SURF the "set" isn't found out until it computes the descriptor
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
		return location.get(featureIndex).scale*BoofDefaults.SURF_SCALE_TO_RADIUS;
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

	@Override
	public ImageType<T> getInputType() {
		return inputType;
	}
}
