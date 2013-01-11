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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.alg.transform.ii.IntegralImageOps;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.struct.image.ImageFloat32;


/**
 * @author Peter Abeles
 */
public class TestFastHessianFeatureDetector extends GenericFeatureDetector {

	public TestFastHessianFeatureDetector() {
		this.scaleTolerance = 0.3;
	}

	@Override
	protected Object createDetector( int maxFeatures ) {
		NonMaxSuppression extractor = FactoryFeatureExtractor.nonmax(new ConfigExtract(1, 1, 5, true));
		return new FastHessianFeatureDetector(extractor,maxFeatures, 1, 9,4,4);
	}

	@SuppressWarnings({"unchecked"})
	@Override
	protected int detectFeature(ImageFloat32 input, Object detector) {
		FastHessianFeatureDetector<ImageFloat32> alg = (FastHessianFeatureDetector<ImageFloat32>)detector;
		ImageFloat32 integral = IntegralImageOps.transform(input,null);
		alg.detect(integral);

		return alg.getFoundPoints().size();
	}
}
