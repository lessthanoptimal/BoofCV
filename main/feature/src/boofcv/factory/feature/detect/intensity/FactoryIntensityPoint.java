/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.feature.detect.intensity;

import boofcv.abst.feature.detect.intensity.*;
import boofcv.abst.filter.blur.MedianImageFilter;
import boofcv.alg.feature.detect.intensity.FastCornerIntensity;
import boofcv.alg.feature.detect.intensity.HarrisCornerIntensity;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.alg.feature.detect.intensity.KltCornerIntensity;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.image.ImageSingleBand;

/**
 * Provides intensity feature intensity algorithms which conform to the {@link GeneralFeatureIntensity} interface.
 *
 * @see FactoryIntensityPointAlg
 * @see boofcv.factory.feature.detect.extract.FactoryFeatureExtractor
 *
 * @author Peter Abeles
 */
public class FactoryIntensityPoint {

	/**
	 * Feature intensity for Fast corner detector.  See {@link FastCornerIntensity} for more details.
	 *
	 * @param pixelTol How different pixels need to be to be considered part of a corner. Image dependent.  Try 20 to start.
	 * @param minCont Minimum number of continue pixels in a circle for it ot be a corner.  11 or 12 are good numbers.
	 * @param imageType Type of input image it is computed form.
	 * @param <I> Input image type.
	 * @param <D> Derivative type.
	 * @return Fast feature intensity
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureIntensity<I,D>  fast( int pixelTol, int minCont, Class<I> imageType ) {
		FastCornerIntensity<I> alg =  FactoryIntensityPointAlg.fast12(pixelTol, minCont, imageType);
		return new WrapperFastCornerIntensity<I, D>(alg);
	}

	/**
	 * Feature intensity for Harris corner detector.  See {@link HarrisCornerIntensity} for more details.
	 *
	 * @param windowRadius Size of the feature it is detects,
	 * @param kappa Tuning parameter, typically a small number around 0.04
	 * @param weighted Is the gradient weighted using a Gaussian distribution?  Weighted is much slower than unweighted.
	 * @param derivType Image derivative type it is computed from.  @return Harris corner
	 * @param <I> Input image type.
	 * @param <D> Derivative type.
	 * @return Harris feature intensity
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureIntensity<I,D>  harris(int windowRadius, float kappa,
										 boolean weighted, Class<D> derivType)
	{
		HarrisCornerIntensity<D> alg =  FactoryIntensityPointAlg.harris(windowRadius, kappa, weighted, derivType);
		return new WrapperGradientCornerIntensity<I, D>(alg);
	}

	/**
	 * Feature intensity for KLT corner detector.  See {@link KltCornerIntensity} for more details.
	 *
	 * @param windowRadius Size of the feature it detects,
	 * @param weighted Should the it be weighted by a Gaussian kernel?  Unweighted is much faster.
	 * @param derivType Image derivative type it is computed from.	 * @param derivType Image derivative type it is computed from.  @return Harris corner
	 * @param <I> Input image type.
	 * @param <D> Derivative type.
	 * @return KLT feature intensity
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureIntensity<I,D>  klt(int windowRadius, boolean weighted, Class<D> derivType) {
		KltCornerIntensity<D> alg =  FactoryIntensityPointAlg.klt(windowRadius, weighted, derivType);
		return new WrapperGradientCornerIntensity<I, D>(alg);
	}

	/**
	 * Feature intensity for Kitchen and Rosenfeld corner detector.  See {@link boofcv.alg.feature.detect.intensity.KitRosCornerIntensity} for more details.
	 *
	 * @param derivType Image derivative type it is computed from.
	 * @param <I> Input image type.
	 * @param <D> Derivative type.
	 * @return Kitchen and Rosenfeld feature intensity
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureIntensity<I,D>  kitros( Class<D> derivType ) {
		return new WrapperKitRosCornerIntensity<I, D>(derivType);
	}

	/**
	 * Feature intensity for median corner detector.
	 *
	 * @param radius Size of the feature it detects,
	 * @param <I> Input image type.
	 * @return Median feature intensity
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureIntensity<I,D>  median( int radius , Class<I> imageType ) {
		MedianImageFilter<I> filter = FactoryBlurFilter.median(imageType,radius);
		return new WrapperMedianCornerIntensity<I, D>(filter,imageType);
	}

	/**
	 * Feature intensity for median corner detector. See {@Link HessianBlobIntensity} for more details.
	 *
	 * @param type Type of Hessian
	 * @param <I> Input image type.
	 * @param <D> Derivative type.
	 * @return Median feature intensity
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureIntensity<I,D>  laplacian( HessianBlobIntensity.Type type , Class<D> derivType) {
		return new WrapperLaplacianBlobIntensity<I, D>(type,derivType);
	}
}
