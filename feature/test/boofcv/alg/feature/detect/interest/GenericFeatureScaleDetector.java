/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package boofcv.alg.feature.detect.interest;

import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.factory.feature.detect.interest.FactoryBlobDetector;
import boofcv.struct.image.ImageFloat32;

/**
 * @author Peter Abeles
 */
public abstract class GenericFeatureScaleDetector extends GenericFeatureDetector {

	int r = 2;

	private GeneralFeatureDetector<ImageFloat32,ImageFloat32> createBlobDetector( int maxFeatures) {
		return FactoryBlobDetector.createLaplace(r,1,maxFeatures,ImageFloat32.class,HessianBlobIntensity.Type.DETERMINANT);
	}

	@Override
	protected Object createDetector( int maxFeatures )
	{
		return createDetector(createBlobDetector(maxFeatures));
	}

	protected abstract Object createDetector( GeneralFeatureDetector<ImageFloat32, ImageFloat32> detector);
}
