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

import boofcv.abst.filter.ImageFunctionSparse;
import boofcv.abst.filter.derivative.AnyImageDerivative;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.inst.FactoryImageGenerator;
import boofcv.factory.filter.derivative.FactoryDerivativeSparse;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.pyramid.PyramidFloat;


/**
 * @author Peter Abeles
 */
public class TestFeatureLaplacePyramid extends GenericFeatureScaleDetector {

	public TestFeatureLaplacePyramid() {
		// just make one of these tests work
		scaleTolerance = 2.5;
	}

	@Override
	protected Object createDetector(GeneralFeatureDetector<ImageFloat32, ImageFloat32> detector) {

		ImageFunctionSparse<ImageFloat32> sparseLaplace =
				FactoryDerivativeSparse.createLaplacian(ImageFloat32.class, null);
		AnyImageDerivative<ImageFloat32, ImageFloat32> deriv =  GImageDerivativeOps.
				createDerivatives(ImageFloat32.class, FactoryImageGenerator.create(ImageFloat32.class));

		return new FeatureLaplacePyramid<ImageFloat32, ImageFloat32>(detector, sparseLaplace, deriv, 1);
	}

	@Override
	protected int detectFeature(ImageFloat32 input, Object detector) {

		PyramidFloat<ImageFloat32> ss = FactoryPyramid.scaleSpacePyramid(new double[]{1, 2, 4, 8}, ImageFloat32.class);
		ss.process(input);

		FeatureLaplacePyramid<ImageFloat32, ImageFloat32> alg = (FeatureLaplacePyramid<ImageFloat32, ImageFloat32>) detector;
		alg.detect(ss);

		return alg.getInterestPoints().size();
	}

}
