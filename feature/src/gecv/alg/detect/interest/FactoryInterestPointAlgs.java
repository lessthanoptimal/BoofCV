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

package gecv.alg.detect.interest;

import gecv.abst.detect.corner.GeneralCornerDetector;
import gecv.abst.detect.corner.GeneralCornerIntensity;
import gecv.abst.detect.corner.WrapperGradientCornerIntensity;
import gecv.abst.detect.extract.CornerExtractor;
import gecv.abst.detect.extract.FactoryFeatureFromIntensity;
import gecv.abst.filter.ImageFunctionSparse;
import gecv.abst.filter.derivative.FactoryDerivativeSparse;
import gecv.alg.detect.corner.FactoryCornerIntensity;
import gecv.alg.detect.corner.GradientCornerIntensity;
import gecv.struct.image.ImageBase;

/**
 * @author Peter Abeles
 */
public class FactoryInterestPointAlgs {

	/**
	 * Creates a {@link gecv.alg.detect.interest.CornerLaplaceScaleSpace} which is uses the Harris corner detector.
	 *
	 * @param featureRadius Size of the feature used to detect the corners.
	 * @param cornerThreshold Minimum corner intensity required
	 * @param maxFeatures Max number of features that can be found.
	 * @param imageType Type of input image.
	 * @param derivType Image derivative type.
	 * @return CornerLaplaceScaleSpace
	 */
	public static <T extends ImageBase, D extends ImageBase>
	CornerLaplaceScaleSpace<T,D> harrisLaplace( int featureRadius ,
												float cornerThreshold ,
												int maxFeatures ,
												Class<T> imageType ,
												Class<D> derivType)
	{
		CornerExtractor extractor = FactoryFeatureFromIntensity.create(featureRadius,cornerThreshold,false,false,false);
		GradientCornerIntensity<D> harris = FactoryCornerIntensity.createHarris(derivType,featureRadius,0.04f);
		GeneralCornerIntensity<T, D> intensity = new WrapperGradientCornerIntensity<T,D>(harris);
		GeneralCornerDetector<T,D> detector = new GeneralCornerDetector<T,D>(intensity,extractor,maxFeatures);

		ImageFunctionSparse<T> sparseLaplace = FactoryDerivativeSparse.createLaplacian(imageType,null);

		return new CornerLaplaceScaleSpace<T,D>(detector,sparseLaplace);
	}
}
