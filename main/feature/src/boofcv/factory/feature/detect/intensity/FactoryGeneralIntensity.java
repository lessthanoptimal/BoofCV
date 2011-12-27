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
import boofcv.struct.image.ImageSingleBand;

/**
 * Provides intensity feature intensity algorithms which conform to the {@link GeneralFeatureIntensity} interface.
 *
 * @author Peter Abeles
 */
public class FactoryGeneralIntensity {

	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureIntensity<I,D>  fast( int pixelTol, int minCont, Class<I> imageType ) {
		FastCornerIntensity<I> alg =  FactoryPointIntensityAlg.createFast12(pixelTol,minCont,imageType);
		return new WrapperFastCornerIntensity<I, D>(alg);
	}

	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureIntensity<I,D>  harris( int windowRadius, float kappa , Class<D> derivType ) {
		HarrisCornerIntensity<D> alg =  FactoryPointIntensityAlg.createHarris(windowRadius,kappa, derivType);
		return new WrapperGradientCornerIntensity<I, D>(alg);
	}

	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureIntensity<I,D>  klt( int windowRadius , Class<D> derivType ) {
		KltCornerIntensity<D> alg =  FactoryPointIntensityAlg.createKlt(windowRadius, derivType);
		return new WrapperGradientCornerIntensity<I, D>(alg);
	}

	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureIntensity<I,D>  kitros( Class<D> derivType ) {
		return new WrapperKitRosCornerIntensity<I, D>(derivType);
	}

	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureIntensity<I,D>  median( MedianImageFilter<I> filter , Class<I> imageType ) {
		return new WrapperMedianCornerIntensity<I, D>(filter,imageType);
	}

	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	GeneralFeatureIntensity<I,D>  laplacian( HessianBlobIntensity.Type type , Class<D> derivType) {
		return new WrapperLaplacianBlobIntensity<I, D>(type,derivType);
	}
}
