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

package gecv.alg.wavelet;

import gecv.struct.image.ImageBase;


/**
 * @author Peter Abeles
 */
public interface WaveletImageTransform<T extends ImageBase, D extends WaveletDesc> {

	public void configure( D desc , int levels );

	/**
	 *
	 * @param image Input image. Not modified.
	 * @param transformed Wavelet transformation of input image.
	 */
	public void transform( T image , T transformed );

	public void transform( T image );

	public void inverse( T transformed , T reconstructedImage );

	public void inverse( T transformed );
}
