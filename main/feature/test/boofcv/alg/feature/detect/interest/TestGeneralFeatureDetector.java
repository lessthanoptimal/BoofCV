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

import boofcv.abst.feature.detect.interest.GeneralFeatureDetector;
import boofcv.abst.filter.derivative.AnyImageDerivative;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.inst.FactoryImageGenerator;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.struct.image.ImageFloat32;


/**
 * @author Peter Abeles
 */
public class TestGeneralFeatureDetector extends GenericFeatureDetector {

	AnyImageDerivative<ImageFloat32, ImageFloat32> computeDerivative =
			GImageDerivativeOps.createDerivatives(ImageFloat32.class, FactoryImageGenerator.create(ImageFloat32.class));

	@Override
	protected Object createDetector(int maxFeatures) {
//		return FactoryBlobDetector.createLaplace(2,0,maxFeatures,ImageFloat32.class,HessianBlobIntensity.Type.DETERMINANT);
		return FactoryDetectPoint.createHarris(2, false, 0, maxFeatures, ImageFloat32.class);
	}

	@Override
	protected int detectFeature(ImageFloat32 input, Object detector) {
		GeneralFeatureDetector<ImageFloat32, ImageFloat32> d =
				(GeneralFeatureDetector<ImageFloat32, ImageFloat32>) detector;

		computeDerivative.setInput(input);

		ImageFloat32 derivX = computeDerivative.getDerivative(true);
		ImageFloat32 derivY = computeDerivative.getDerivative(false);
		ImageFloat32 derivXX = computeDerivative.getDerivative(true, true);
		ImageFloat32 derivYY = computeDerivative.getDerivative(false, false);
		ImageFloat32 derivXY = computeDerivative.getDerivative(true, false);

		d.process(input, derivX, derivY, derivXX, derivYY, derivXY);

		return d.getFeatures().size;
	}
}
