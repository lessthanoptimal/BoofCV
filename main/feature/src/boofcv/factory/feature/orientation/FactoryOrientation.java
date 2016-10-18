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

package boofcv.factory.feature.orientation;

import boofcv.abst.feature.describe.ConfigSiftScaleSpace;
import boofcv.abst.feature.orientation.*;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.interest.SiftScaleSpace;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;

/**
 * Factory for creating implementations of {@link RegionOrientation} that are used to estimate
 * the orientation of a local pixel region..
 *
 * @author Peter Abeles
 */
public class FactoryOrientation {

	/**
	 * Adds wrappers around implementations of {@link RegionOrientation} such that they can be used
	 * as a {@link OrientationImage}.
	 *
	 * @param algorithm Algorithm which takes in a different type of input.
	 * @param imageType Type of input image it will process
	 * @return Wrapped version which can process images as its raw input.
	 */
	public static <T extends ImageGray>
	OrientationImage<T> convertImage( RegionOrientation algorithm , Class<T> imageType ) {
		if( algorithm instanceof OrientationGradient ) {
			Class derivType = ((OrientationGradient) algorithm).getImageType();
			ImageGradient gradient = FactoryDerivative.sobel(imageType,derivType);
			return new OrientationGradientToImage((OrientationGradient)algorithm,gradient,imageType,derivType);
		} else if( algorithm instanceof OrientationIntegral ) {
			Class integralType = GIntegralImageOps.getIntegralType(imageType);
			return new OrientationIntegralToImage((OrientationIntegral)algorithm,imageType,integralType);
		} else {
			throw new IllegalArgumentException("Unknown orientation algorithm type");
		}
	}

	/**
	 * Creates an implementation of the SIFT orientation estimation algorithm
	 *
	 * @param configSS Configuration of the scale-space.  null for default
	 * @param configOri  Orientation configuration. null for default
	 * @param imageType Type of input image
	 * @return SIFT orientation image
	 */
	public static <T extends ImageGray>
	OrientationImage<T> sift(ConfigSiftScaleSpace configSS , ConfigSiftOrientation configOri, Class<T> imageType ) {
		if( configSS == null )
			configSS = new ConfigSiftScaleSpace();
		configSS.checkValidity();

		OrientationHistogramSift<GrayF32> ori = FactoryOrientationAlgs.sift(configOri,GrayF32.class);

		SiftScaleSpace ss = new SiftScaleSpace(
				configSS.firstOctave,configSS.lastOctave,configSS.numScales,configSS.sigma0);
		return new OrientationSiftToImage<>(ori, ss, imageType);
	}
}
