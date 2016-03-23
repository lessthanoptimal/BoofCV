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

package boofcv.abst.transform.fft;

import boofcv.alg.transform.fft.DiscreteFourierTransformOps;
import boofcv.alg.transform.fft.GeneralPurposeFFT_F64_2D;
import boofcv.struct.image.GrayF64;
import boofcv.struct.image.InterleavedF64;

/**
 * Wrapper around {@link GeneralPurposeFFT_F64_2D} which implements {@link DiscreteFourierTransform}
 *
 * @author Peter Abeles
 */
public class GeneralFft_to_DiscreteFourierTransform_F64
		implements DiscreteFourierTransform<GrayF64,InterleavedF64>
{
	// previous size of input image
	private int prevWidth = -1;
	private int prevHeight = -1;

	// performs the FFT
	private GeneralPurposeFFT_F64_2D alg;

	// storage for temporary results
	private InterleavedF64 tmp = new InterleavedF64(1,1,2);

	// if true then it can modify the input images
	private boolean modifyInputs = false;

	@Override
	public void forward(GrayF64 image, InterleavedF64 transform ) {
		DiscreteFourierTransformOps.checkImageArguments(image,transform);
		if( image.isSubimage() )
			throw new IllegalArgumentException("Subimages are not supported");

		checkDeclareAlg(image);

		int N = image.width*image.height;
		System.arraycopy(image.data,0,transform.data,0,N);

		// the transform over writes the input data
		alg.realForwardFull(transform.data);
	}

	@Override
	public void inverse(InterleavedF64 transform, GrayF64 image ) {
		DiscreteFourierTransformOps.checkImageArguments(image,transform);
		if( image.isSubimage() )
			throw new IllegalArgumentException("Subimages are not supported");

		checkDeclareAlg(image);

		// If he user lets us, modify the transform
		InterleavedF64 workImage;
		if(modifyInputs) {
			workImage = transform;
		} else {
			tmp.reshape(transform.width,transform.height);
			tmp.setTo(transform);
			workImage = tmp;
		}

		alg.complexInverse(workImage.data, true);

		// copy the real portion.  imaginary should be zeros
		int N = image.width*image.height;
		for( int i = 0; i < N; i++ ) {
			image.data[i] = workImage.data[i*2];
		}
	}

	/**
	 * Declare the algorithm if the image size has changed
	 */
	private void checkDeclareAlg(GrayF64 image) {
		if( prevWidth != image.width || prevHeight != image.height ) {
			prevWidth = image.width;
			prevHeight = image.height;
			alg = new GeneralPurposeFFT_F64_2D(image.height,image.width);
		}
	}

	@Override
	public void setModifyInputs(boolean modify) {
		this.modifyInputs = modify;
	}

	@Override
	public boolean isModifyInputs() {
		return modifyInputs;
	}
}
