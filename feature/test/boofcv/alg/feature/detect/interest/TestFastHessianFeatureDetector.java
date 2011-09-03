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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.alg.transform.ii.IntegralImageOps;
import boofcv.factory.feature.detect.extract.FactoryFeatureFromIntensity;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_I32;

import java.util.List;


/**
 * @author Peter Abeles
 */
public class TestFastHessianFeatureDetector extends GenericFeatureDetector {

	public TestFastHessianFeatureDetector() {
		this.scaleTolerance = 0.3;
	}

	@Override
	protected Object createDetector( int maxFeatures ) {
		FeatureExtractor extractor = FactoryFeatureFromIntensity.create(2,1,5,false,false,false);
		return new FastHessianFeatureDetector(extractor,maxFeatures,9,4,4);
	}

	@SuppressWarnings({"unchecked"})
	@Override
	protected List<Point2D_I32> detectFeature(ImageFloat32 input, double[] scales, Object detector) {
		FastHessianFeatureDetector<ImageFloat32> alg = (FastHessianFeatureDetector<ImageFloat32>)detector;
		ImageFloat32 integral = IntegralImageOps.transform(input,null);
		alg.detect(integral);

		return (List)alg.getFoundPoints();
	}
}
