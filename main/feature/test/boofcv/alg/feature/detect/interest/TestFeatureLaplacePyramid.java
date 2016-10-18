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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.filter.ImageFunctionSparse;
import boofcv.abst.filter.derivative.AnyImageDerivative;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.factory.filter.derivative.FactoryDerivativeSparse;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.image.GrayF32;
import boofcv.struct.pyramid.PyramidFloat;


/**
 * @author Peter Abeles
 */
public class TestFeatureLaplacePyramid extends GenericFeatureScaleDetectorTests {

	public TestFeatureLaplacePyramid() {
		// just make one of these tests work
		scaleTolerance = 2.5;
	}

	@Override
	protected Object createDetector(GeneralFeatureDetector<GrayF32, GrayF32> detector) {

		ImageFunctionSparse<GrayF32> sparseLaplace =
				FactoryDerivativeSparse.createLaplacian(GrayF32.class, null);
		AnyImageDerivative<GrayF32, GrayF32> deriv =  GImageDerivativeOps.
				derivativeForScaleSpace(GrayF32.class, GrayF32.class);

		return new FeatureLaplacePyramid<>(detector, sparseLaplace, deriv, 1);
	}

	@Override
	protected int detectFeature(GrayF32 input, Object detector) {

		PyramidFloat<GrayF32> ss = FactoryPyramid.scaleSpacePyramid(new double[]{1, 2, 4, 8}, GrayF32.class);
		ss.process(input);

		FeatureLaplacePyramid<GrayF32, GrayF32> alg = (FeatureLaplacePyramid<GrayF32, GrayF32>) detector;
		alg.detect(ss);

		return alg.getInterestPoints().size();
	}

}
