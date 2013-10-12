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

package boofcv.abst.transform.fft;

import boofcv.struct.image.ImageBase;

/**
 * <p>
 * High level interface for applying the forward and inverse Discrete Fourier Transform to an image.  Images of any
 * size can be processed by this interface.  Images can typically be processed must faster when their size is a power
 * of two, see Fast Fourier Transform. The size of the input image can also be changed between called.
 * </p>
 * <p>
 * The Fourier transform of an image contains both real an imaginary components. These are stored in the output
 * image in an interleaved format.  As a result the output image will have twice the width and height as the input
 * image. This format is shown below:
 * <pre>
 * a[i*2*width+2*j] = Re[i][j],
 * a[i*2*width+2*j+1] = Im[i][j], 0&le;i&lt;height, 0&le;j&lt;width,</pre>
 * </p>
 * <p>
 * INPUT MODIFICATION: By default none of the inputs are modified.  However, in some implementations, memory can be
 * saved by allowing inputs to be modified.  To allow the class to modify its inputs use the following function,
 * {@link #setModifyInputs(boolean)}.
 * </p>
 *
 * @author Peter Abeles
 */
public interface DiscreteFourierTransform<I extends ImageBase, T extends ImageBase> {

	/**
	 * Applies forward transform to the input image.
	 *
	 * @param image (Input) Input image.  Default: Not modified.
	 * @param transform (Output) Fourier transform, twice width and same height of input.  Modified.
	 */
	public void forward( I image , T transform );

	/**
	 * Applies the inverse transform to a fourier transformed image to recover the original image
	 * @param transform (Input) Fourier transform. twice width and same height of output.  Default: Not modified.
	 * @param image (Output) reconstructed image.  Modified.
	 */
	public void inverse( T transform , I image );

	/**
	 * This function can toggle the internal implementations ability to modify the input image or input transform.
	 *
	 * @param modify true for the input can be modified and false for it will not be modified.
	 */
	public void setModifyInputs( boolean modify );

	/**
	 * Returns state of forward modification flag
	 * @return true for the input can be modified and false for it will not be modified.
	 */
	public boolean isModifyInputs();
}
