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
 * High level interface for applying the forward and inverse Discrete Fourier Transform to an image.  Images of any
 * size can be processed by this interface.  Images can typically be processed must faster when their size is a power
 * of two, see Fast Fourier Transform. The size of the input image can also be changed between called.
 *
 * @author Peter Abeles
 */
public interface DiscreteFourierTransform<T extends ImageBase> {

	/**
	 * Applies forward transform to the input image.
	 *
	 * @param image (Input) Image.  Not modified.
	 * @param transform (Ouptut) Fourier transform.  Modified.
	 */
	public void forward( T image , T transform );

	/**
	 * Applies the inverse transform to a fourier transformed image to recover the original image
	 * @param transform (Input) Fourier transform.  Not modified.
	 * @param image (Output) Input.  Modified.
	 */
	public void inverse( T transform , T image  );
}
