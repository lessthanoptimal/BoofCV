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

package gecv.alg.feature.detect.interest;

import gecv.abst.detect.extract.FeatureExtractor;
import gecv.abst.detect.intensity.GeneralFeatureIntensity;
import gecv.abst.detect.intensity.WrapperLaplacianBlobIntensity;
import gecv.alg.feature.detect.intensity.HessianBlobIntensity;
import gecv.factory.feature.detect.extract.FactoryFeatureFromIntensity;
import gecv.struct.image.ImageFloat32;

/**
 * @author Peter Abeles
 */
public abstract class GenericFeatureScaleDetector extends GenericFeatureDetector {

	int r = 2;

	private GeneralFeatureDetector<ImageFloat32,ImageFloat32> createBlobDetector( int maxFeatures) {
		FeatureExtractor extractor = FactoryFeatureFromIntensity.create(r,1,0,false,false,false);
		GeneralFeatureIntensity<ImageFloat32, ImageFloat32> intensity =
				new WrapperLaplacianBlobIntensity<ImageFloat32,ImageFloat32>(HessianBlobIntensity.Type.DETERMINANT,ImageFloat32.class);
		return new GeneralFeatureDetector<ImageFloat32,ImageFloat32>(intensity,extractor,maxFeatures);
	}

	@Override
	protected Object createDetector( int maxFeatures )
	{
		return createDetector(createBlobDetector(maxFeatures));
	}

	protected abstract Object createDetector( GeneralFeatureDetector<ImageFloat32, ImageFloat32> detector);


}
