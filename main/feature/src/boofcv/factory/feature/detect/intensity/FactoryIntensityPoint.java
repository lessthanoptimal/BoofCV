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

package boofcv.factory.feature.detect.intensity;

import boofcv.abst.feature.detect.intensity.*;
import boofcv.abst.filter.blur.BlurStorageFilter;
import boofcv.alg.feature.detect.intensity.FastCornerIntensity;
import boofcv.alg.feature.detect.intensity.HarrisCornerIntensity;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.image.ImageGray;

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
	 * @param minCont Minimum number of continue pixels in a circle for it ot be a corner.  Can be 9,10,11 or 12.
	 * @param imageType Type of input image it is computed form.
	 * @param <I> Input image type.
	 * @param <D> Derivative type.
	 * @return Fast feature intensity
	 */
	public static <I extends ImageGray, D extends ImageGray>
	GeneralFeatureIntensity<I,D>  fast( int pixelTol, int minCont, Class<I> imageType ) {
		FastCornerIntensity<I> alg =  FactoryIntensityPointAlg.fast(pixelTol, minCont, imageType);
		return new WrapperFastCornerIntensity<>(alg);
	}

	/**
	 * Feature intensity for Harris corner detector.  See {@link HarrisCornerIntensity} for more details.
	 *
	 * @param windowRadius Size of the feature it is detects, Try 2.
	 * @param kappa Tuning parameter, typically a small number around 0.04
	 * @param weighted Is the gradient weighted using a Gaussian distribution?  Weighted is much slower than unweighted.
	 * @param derivType Image derivative type it is computed from.  @return Harris corner
	 * @param <I> Input image type.
	 * @param <D> Derivative type.
	 * @return Harris feature intensity
	 */
	public static <I extends ImageGray, D extends ImageGray>
	GeneralFeatureIntensity<I,D>  harris(int windowRadius, float kappa,
										 boolean weighted, Class<D> derivType)
	{
		HarrisCornerIntensity<D> alg =  FactoryIntensityPointAlg.harris(windowRadius, kappa, weighted, derivType);
		return new WrapperGradientCornerIntensity<>(alg);
	}

	/**
	 * Feature intensity for KLT corner detector.  See {@link boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity} for more details.
	 *
	 * @param windowRadius Size of the feature it detects, Try 2.
	 * @param weighted Should the it be weighted by a Gaussian kernel?  Unweighted is much faster.
	 * @param derivType Image derivative type it is computed from.	 * @param derivType Image derivative type it is computed from.  @return Harris corner
	 * @param <I> Input image type.
	 * @param <D> Derivative type.
	 * @return KLT feature intensity
	 */
	public static <I extends ImageGray, D extends ImageGray>
	GeneralFeatureIntensity<I,D> shiTomasi(int windowRadius, boolean weighted, Class<D> derivType) {
		ShiTomasiCornerIntensity<D> alg =  FactoryIntensityPointAlg.shiTomasi(windowRadius, weighted, derivType);
		return new WrapperGradientCornerIntensity<>(alg);
	}

	/**
	 * Feature intensity for Kitchen and Rosenfeld corner detector.  See {@link boofcv.alg.feature.detect.intensity.KitRosCornerIntensity} for more details.
	 *
	 * @param derivType Image derivative type it is computed from.
	 * @param <I> Input image type.
	 * @param <D> Derivative type.
	 * @return Kitchen and Rosenfeld feature intensity
	 */
	public static <I extends ImageGray, D extends ImageGray>
	GeneralFeatureIntensity<I,D>  kitros( Class<D> derivType ) {
		return new WrapperKitRosCornerIntensity<>(derivType);
	}

	/**
	 * Feature intensity for median corner detector.
	 *
	 * @param radius Size of the feature it detects,
	 * @param <I> Input image type.
	 * @return Median feature intensity
	 */
	public static <I extends ImageGray, D extends ImageGray>
	GeneralFeatureIntensity<I,D>  median( int radius , Class<I> imageType ) {
		BlurStorageFilter<I> filter = FactoryBlurFilter.median(imageType,radius);
		return new WrapperMedianCornerIntensity<>(filter, imageType);
	}

	/**
	 * Blob detector which uses the image's second order derivatives directly.
	 *
	 * @see HessianBlobIntensity
	 *
	 * @param type Type of Hessian
	 * @param <I> Input image type.
	 * @param <D> Derivative type.
	 * @return Hessian based blob intensity
	 */
	public static <I extends ImageGray, D extends ImageGray>
	GeneralFeatureIntensity<I,D> hessian(HessianBlobIntensity.Type type, Class<D> derivType) {
		return new WrapperHessianBlobIntensity<>(type, derivType);
	}

	/**
	 * Blob detector which uses a 3x3 kernel to approximate the second order derivatives and compute a Laplacian
	 * blob.
	 */
	public static <I extends ImageGray>
	GeneralFeatureIntensity<I,?> laplacian() {
		return new WrapperLaplacianBlobIntensity<>();
	}
}
