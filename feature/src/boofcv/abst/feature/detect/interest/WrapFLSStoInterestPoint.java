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

import boofcv.alg.feature.detect.interest.FeatureLaplaceScaleSpace;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.gss.GaussianScaleSpace;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_I32;

import java.util.List;


/**
 * Wrapper around {@link boofcv.alg.feature.detect.interest.FeatureLaplaceScaleSpace} for {@link boofcv.abst.feature.detect.interest.InterestPointDetector}.
 *
 * @author Peter Abeles
 */
public class WrapFLSStoInterestPoint<T extends ImageBase, D extends ImageBase> implements InterestPointDetector<T>{

	FeatureLaplaceScaleSpace<T,D> detector;
	List<ScalePoint> location;
	GaussianScaleSpace<T,D> ss;

	public WrapFLSStoInterestPoint(FeatureLaplaceScaleSpace<T,D> detector,
								   GaussianScaleSpace<T,D> ss ) {
		this.detector = detector;
		this.ss = ss;
	}

	@Override
	public void detect(T input) {
		ss.setImage(input);

		detector.detect(ss);

		location = detector.getInterestPoints();
	}

	@Override
	public int getNumberOfFeatures() {
		return location.size();
	}

	@Override
	public Point2D_I32 getLocation(int featureIndex) {
		return location.get(featureIndex);
	}

	@Override
	public double getScale(int featureIndex) {
		return location.get(featureIndex).scale;
	}

	@Override
	public double getOrientation(int featureIndex) {
		throw new IllegalArgumentException("Not supported");
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
