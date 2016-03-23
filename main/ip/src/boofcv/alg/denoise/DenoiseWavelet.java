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

package boofcv.alg.denoise;

import boofcv.struct.image.ImageGray;


/**
 * Interface for algorithms which "denoise" the wavelet transform of an image.  Typically
 * this is done by setting insignificant coefficients to zero.
 *
 * @author Peter Abeles
 */
public interface DenoiseWavelet <T extends ImageGray>  {

	/**
	 * Removes noise from the multi-level wavelet transform.
	 *
	 * @param transform Transform of the original image.
	 * @param numLevels NUmber of levels in the transform.
	 */
	public void denoise( T transform , int numLevels );
}
