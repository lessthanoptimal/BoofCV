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

package gecv.factory.transform.pyramid;

import gecv.alg.interpolate.InterpolatePixel;
import gecv.alg.transform.pyramid.PyramidUpdateGaussianScale;
import gecv.alg.transform.pyramid.PyramidUpdateIntegerDown;
import gecv.factory.filter.kernel.FactoryKernelGaussian;
import gecv.factory.interpolate.FactoryInterpolation;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.image.ImageBase;
import gecv.struct.pyramid.PyramidUpdaterDiscrete;
import gecv.struct.pyramid.PyramidUpdaterFloat;


/**
 * Factory for creating classes related to image pyramids.
 *
 * @author Peter Abeles
 */
public class FactoryPyramid {

	/**
	 * Creates an updater for discrete pyramids where a Gaussian is convolved across the input
	 * prior to sub-sampling.
	 *
	 * @param imageType Type of input image.
	 * @param sigma Gaussian sigma.  If < 0 then a sigma is selected using the radius.
	 * @param radius Radius of the Gaussian kernel.  If < 0 then the radius is selected using sigma.
	 * @return PyramidUpdaterDiscrete
	 */
	public static <T extends ImageBase>
	PyramidUpdaterDiscrete<T> discreteGaussian( Class<T> imageType , double sigma , int radius ) {
		return new PyramidUpdateIntegerDown<T>(FactoryKernelGaussian.gaussian(Kernel1D_F32.class,sigma,radius),imageType);
	}

	/**
	 * Creates an updater for float pyramids where each layer is blurred using a Gaussian with the specified
	 * sigma.  Bilinear interpolation is used when sub-sampling.
	 *
	 * @param imageType Type of image in the pyramid.
	 * @param sigmas Gaussian blur magnitude for each layer.
	 * @return PyramidUpdaterFloat
	 */
	public static <T extends ImageBase>
	PyramidUpdaterFloat<T> floatGaussian( Class<T> imageType , double ...sigmas ) {

		InterpolatePixel<T> interp = FactoryInterpolation.bilinearPixel(imageType);

		return new PyramidUpdateGaussianScale<T>(interp,sigmas);
	}
}
