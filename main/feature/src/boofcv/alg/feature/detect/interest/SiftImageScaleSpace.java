/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.interest;

import boofcv.alg.filter.convolve.ConvolveNormalized;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.image.ImageFloat32;

/**
 * @author Peter Abeles
 */
public class SiftImageScaleSpace {

	ImageFloat32 dog[];
	ImageFloat32 scale[];
	float sigma;

	Kernel1D_F32 kernel;

	ImageFloat32 storage;
	int octave;

	public SiftImageScaleSpace( int numScales , float sigma , int radius )
	{
		if( numScales < 3 )
			throw new IllegalArgumentException("A minimum of 3 scales are required");

		this.sigma = sigma;

		scale = new ImageFloat32[numScales];
		dog = new ImageFloat32[numScales-1];
		for( int i = 0; i < numScales; i++ ) {
			scale[i] = new ImageFloat32(1,1);
		}
		for( int i = 0; i < numScales-1; i++ ) {
			dog[i] = new ImageFloat32(1,1);
		}
		storage = new ImageFloat32(1,1);

		kernel = FactoryKernelGaussian.gaussian(Kernel1D_F32.class, sigma, radius);
	}

	public void process( ImageFloat32 input ) {
		octave = 1;

		for( int i = 0; i < scale.length; i++ ) {
			scale[i].reshape(input.width,input.height);
		}
		for( int i = 0; i < scale.length; i++ ) {
			dog[i].reshape(input.width, input.height);
		}
		storage.reshape(input.width,input.height);

		ConvolveNormalized.horizontal(kernel, input, storage);
		ConvolveNormalized.vertical(kernel,storage,scale[1]);

		for( int i = 1; i < scale.length; i++ ) {
			ConvolveNormalized.horizontal(kernel, scale[i-1], storage);
			ConvolveNormalized.vertical(kernel,storage,scale[i]);
		}
	}

	public void computeFeatureIntensity() {
		for( int i = 1; i < scale.length; i++ ) {
//			PixelMath.plus();
		}
	}

	public void computeNextOctave() {
		octave++;


	}

}
