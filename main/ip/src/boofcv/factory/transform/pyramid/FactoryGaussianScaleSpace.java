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

package boofcv.factory.transform.pyramid;

import boofcv.alg.transform.pyramid.NoCacheScaleSpace;
import boofcv.core.image.ImageGenerator;
import boofcv.core.image.inst.SingleBandGenerator;
import boofcv.struct.gss.GaussianScaleSpace;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.pyramid.PyramidFloat;

/**
 * Factory which removes some of the drudgery from creating {@link boofcv.struct.gss.GaussianScaleSpace}
 *
 * @author Peter Abeles
 */
public class FactoryGaussianScaleSpace {

	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	GaussianScaleSpace<T,D> nocache( Class<T> imageType  ) {
		if( imageType == ImageFloat32.class ) {
			return (NoCacheScaleSpace<T,D>)nocache_F32();
		} else if( imageType == ImageUInt8.class ) {
			return (NoCacheScaleSpace<T,D>)nocache_U8();
		} else {
			throw new IllegalArgumentException("Doesn't handle "+imageType.getSimpleName()+" yet.");
		}
	}

	public static GaussianScaleSpace<ImageFloat32,ImageFloat32> nocache_F32() {
		ImageGenerator<ImageFloat32> imageGen = new SingleBandGenerator<ImageFloat32>(ImageFloat32.class);
		return new NoCacheScaleSpace<ImageFloat32,ImageFloat32>(imageGen,imageGen);
	}

	public static GaussianScaleSpace<ImageUInt8, ImageSInt16> nocache_U8() {
		ImageGenerator<ImageUInt8> imageGen = new SingleBandGenerator<ImageUInt8>(ImageUInt8.class);
		ImageGenerator<ImageSInt16> derivGen = new SingleBandGenerator<ImageSInt16>(ImageSInt16.class);
		return new NoCacheScaleSpace<ImageUInt8,ImageSInt16>(imageGen,derivGen);
	}

	/**
	 * Constructs an image pyramid which is designed to mimic a {@link GaussianScaleSpace}.  Each layer in the pyramid
	 * should have the equivalent amount of blur that a space-space constructed with the same parameters would have.
	 *
	 * @param scaleSpace The scale of each layer and the desired amount of blur relative to the original image
	 * @param imageType Type of image
	 * @return Image pyramid
	 */
	public static <T extends ImageSingleBand>
	PyramidFloat<T> scaleSpacePyramid( double scaleSpace[], Class<T> imageType ) {

		double[] sigmas = new double[ scaleSpace.length ];

		sigmas[0] = scaleSpace[0];
		for( int i = 1; i < scaleSpace.length; i++ ) {
			// the desired amount of blur
			double c = scaleSpace[i];
			// the effective amount of blur applied to the last level
			double b = scaleSpace[i-1];
			// the amount of additional blur which is needed
			sigmas[i] = Math.sqrt(c*c-b*b);
			// take in account the change in image scale
			sigmas[i] /= scaleSpace[i-1];
		}

		return FactoryPyramid.floatGaussian(scaleSpace,sigmas,imageType);
	}

	public static <T extends ImageSingleBand>
	PyramidFloat<T> scaleSpace( double scaleSpace[], Class<T> imageType ) {

		double[] scaleFactors = new double[ scaleSpace.length ];

		for( int i = 0; i < scaleSpace.length; i++ ) {
			scaleFactors[i] = 1;
		}

		// find the amount of blue that it needs to apply at each layer
		double[] sigmas = new double[ scaleSpace.length ];

		sigmas[0] = scaleSpace[0];
		for( int i = 1; i < scaleSpace.length; i++ ) {
			// the desired amount of blur
			double c = scaleSpace[i];
			// the effective amount of blur applied to the last level
			double b = scaleSpace[i-1];
			// the amount of additional blur which is needed
			sigmas[i] = Math.sqrt(c*c-b*b);
		}

		return FactoryPyramid.floatGaussian(scaleFactors,sigmas,imageType);
	}
}
