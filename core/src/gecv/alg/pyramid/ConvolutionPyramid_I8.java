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

import gecv.alg.interpolate.DownSampleConvolve;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.image.ImageInt8;

/**
 * Convolves a re-normalizable blur kernel across the image before down sampling.  This is usefull for creating
 * a Gaussian pyramid as well as other standard pyramids.
 *
 * @author Peter Abeles
 */
public class ConvolutionPyramid_I8 extends ImagePyramid<ImageInt8> {
	// convolution kernel used to blur the input image before down sampling
	private Kernel1D_I32 kernel;
	// storage for computing the down sampled image
	private int storage[];

	public ConvolutionPyramid_I8( int topWidth, int topHeight , boolean saveOriginalReference , Kernel1D_I32 kernel) {
		super(topWidth,topHeight,saveOriginalReference);

		this.kernel = kernel;
		storage = new int[ kernel.width ];
	}

	@Override
	public void _update( ImageInt8 original ) {

		if( scale[0] == 1 ) {
			if( saveOriginalReference ) {
				layers[0] = original;
			}
			else {
				layers[0].setTo(original);
			}
		} else {
			DownSampleConvolve.downSample(kernel,original,layers[0],scale[0],storage);
		}

		for( int index = 1 ; index < layers.length; index++ ) {
			int skip = scale[index];
			DownSampleConvolve.downSample(kernel,layers[index-1],layers[index],skip,storage);
		}
	}

	@Override
	protected ImageInt8 createImage(int width, int height) {
		return new ImageInt8(width,height);
	}

	@Override
	public Class<ImageInt8> getImageType() {
		return ImageInt8.class;
	}
}
