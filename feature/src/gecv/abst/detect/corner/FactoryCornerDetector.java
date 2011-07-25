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

package gecv.abst.detect.corner;

import gecv.abst.detect.extract.CornerExtractor;
import gecv.abst.detect.extract.FactoryFeatureFromIntensity;
import gecv.abst.filter.blur.FactoryBlurFilter;
import gecv.abst.filter.blur.impl.MedianImageFilter;
import gecv.alg.detect.corner.*;
import gecv.struct.image.ImageBase;

/**
 * Factory for creating corner detectors.
 *
 * @author Peter Abeles
 */
public class FactoryCornerDetector {

	public static <T extends ImageBase, D extends ImageBase>
	GeneralCornerDetector<T,D> createHarris( int featureRadius , float cornerThreshold , int maxFeatures , Class<D> derivType )
	{
		GradientCornerIntensity<D> cornerIntensity = FactoryCornerIntensity.createHarris(derivType,featureRadius,0.04f);
		return createGeneral(cornerIntensity,featureRadius,cornerThreshold,maxFeatures);
	}

	public static <T extends ImageBase, D extends ImageBase>
	GeneralCornerDetector<T,D> createKlt( int featureRadius , float cornerThreshold , int maxFeatures , Class<D> derivType )
	{
		GradientCornerIntensity<D> cornerIntensity = FactoryCornerIntensity.createKlt(derivType,featureRadius);
		return createGeneral(cornerIntensity,featureRadius,cornerThreshold,maxFeatures);
	}

	public static <T extends ImageBase, D extends ImageBase>
	GeneralCornerDetector<T,D> createKitRos( int featureRadius , float cornerThreshold , int maxFeatures , Class<D> derivType )
	{
		KitRosCornerIntensity<D> alg = FactoryCornerIntensity.createKitRos(derivType);
		GeneralCornerIntensity<T,D> intensity = new WrapperKitRosCornerIntensity<T,D>(alg);
		return createGeneral(intensity,featureRadius,cornerThreshold,maxFeatures);
	}

	public static <T extends ImageBase, D extends ImageBase>
	GeneralCornerDetector<T,D> createFast( int featureRadius , int pixelTol , int maxFeatures , Class<T> imageType)
	{
		FastCornerIntensity<T> alg = FactoryCornerIntensity.createFast12(imageType,pixelTol,11);
		GeneralCornerIntensity<T,D> intensity = new WrapperFastCornerIntensity<T,D>(alg);
		return createGeneral(intensity,featureRadius,pixelTol,maxFeatures);
	}

	public static <T extends ImageBase, D extends ImageBase>
	GeneralCornerDetector<T,D> createMedian( int featureRadius , int pixelTol , int maxFeatures , Class<T> imageType)
	{
		MedianCornerIntensity<T> alg = FactoryCornerIntensity.createMedian(imageType);
		MedianImageFilter<T> medianFilter = FactoryBlurFilter.median(imageType,featureRadius);
		GeneralCornerIntensity<T,D> intensity = new WrapperMedianCornerIntensity<T,D>(alg,medianFilter);
		return createGeneral(intensity,featureRadius,pixelTol,maxFeatures);
	}

	private static <T extends ImageBase, D extends ImageBase>
	GeneralCornerDetector<T,D> createGeneral( GradientCornerIntensity<D> cornerIntensity ,
											  int featureRadius , float cornerThreshold , int maxFeatures ) {
		GeneralCornerIntensity<T, D> intensity = new WrapperGradientCornerIntensity<T,D>(cornerIntensity);
		return createGeneral(intensity,featureRadius,cornerThreshold,maxFeatures);
	}

	private static <T extends ImageBase, D extends ImageBase>
	GeneralCornerDetector<T,D> createGeneral( GeneralCornerIntensity<T,D> intensity ,
											  int featureRadius , float cornerThreshold , int maxFeatures ) {
		CornerExtractor extractor = FactoryFeatureFromIntensity.create(featureRadius,cornerThreshold,false,false,false);
		return new GeneralCornerDetector<T,D>(intensity,extractor,maxFeatures);
	}
}
