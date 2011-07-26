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

package gecv.alg.interpolate.impl;

import gecv.alg.interpolate.InterpolatePixel;
import gecv.struct.convolve.KernelContinuous1D_F32;
import gecv.struct.image.ImageFloat32;

/**
 * Performs interpolation by convolving a continuous-discrete function across the image.  Borders are handled by
 * re-normalizing.
 *
 * @author Peter Abeles
 */
public class ImplInterpolatePixelConvolution_F32 implements InterpolatePixel<ImageFloat32>  {

	// kernel used to perform interpolation
	private KernelContinuous1D_F32 kernel;
	// input image
	private ImageFloat32 image;

	public ImplInterpolatePixelConvolution_F32(KernelContinuous1D_F32 kernel) {
		this.kernel = kernel;
	}

	@Override
	public void setImage(ImageFloat32 image) {
		this.image = image;
	}

	@Override
	public ImageFloat32 getImage() {
		return image;
	}

	@Override
	public float get(float x, float y) {

		int xx = (int)x;
		int yy = (int)y;

		final int radius = kernel.getRadius();
		final int width = kernel.getWidth();

		int x0 = xx - radius;
		int x1 = x0 + width;

		int y0 = yy - radius;
		int y1 = y0 + width;

		if( x0 < 0 ) x0 = 0;
		else if( x1 >= image.width ) x1 = image.width-1;

		if( y0 < 0 ) y0 = 0;
		else if( y1 >= image.height ) y1 = image.height-1;

		float value = 0;
		float weight = 0;
		for( int i = y0; i < y1; i++ ) {
			int indexSrc = image.startIndex + yy*image.stride + x0;
			float weightX = 0;
			float valueX = 0;
			for( int j = x0; j < x1; j++ ) {
				float w = kernel.compute(j-x);
				weightX += w;
				valueX += w * (image.data[ indexSrc++ ]);
			}
			float weightY = kernel.compute(i-y);
			value += weightY*valueX;
			weight +=  weightY*weightX;
		}

		return value/weight;
	}

	@Override
	public float get_unsafe(float x, float y) {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
