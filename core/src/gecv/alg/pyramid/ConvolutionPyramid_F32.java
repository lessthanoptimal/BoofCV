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

package gecv.alg.pyramid;

import gecv.alg.filter.convolve.ConvolveDownNormalized;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.image.ImageFloat32;

/**
 * Implementation of {@link gecv.alg.pyramid.ConvolutionPyramid} for {@link ImageFloat32}.
 *
 * @author Peter Abeles
 */
public class ConvolutionPyramid_F32 extends ConvolutionPyramid<ImageFloat32> {
	// convolution kernel used to blur the input image before down sampling
	private Kernel1D_F32 kernel;
	// stores the results from the first convolution
	private ImageFloat32 temp;

	public ConvolutionPyramid_F32(Kernel1D_F32 kernel) {
		this.kernel = kernel;

		// make sure it has been normalized to one
		float error = Math.abs(kernel.computeSum()-1);
		if( error > 1e-2 )
			throw new IllegalArgumentException("Kernel must be normalized to one");
	}

	@Override
	public void _update(ImageFloat32 original) {
		if( temp == null )
			// declare it to be hte latest image that it might need to be, resize below
			temp = new ImageFloat32(original.width/2,original.height);

		if (pyramid.scale[0] == 1) {
			if (pyramid.saveOriginalReference) {
				pyramid.layers[0] = original;
			} else {
				pyramid.layers[0].setTo(original);
			}
		} else {
			int skip = pyramid.scale[0];
			temp.reshape(original.width/skip,original.height);
			ConvolveDownNormalized.horizontal(kernel,original,temp,skip);
			ConvolveDownNormalized.vertical(kernel,temp,pyramid.layers[0],skip);
		}

		for (int index = 1; index < pyramid.layers.length; index++) {
			int skip = pyramid.scale[index];
			ImageFloat32 prev = pyramid.layers[index-1];
			temp.reshape(prev.width/skip,prev.height);
			ConvolveDownNormalized.horizontal(kernel,prev,temp,skip);
			ConvolveDownNormalized.vertical(kernel,temp,pyramid.layers[index],skip);
		}
	}
}
