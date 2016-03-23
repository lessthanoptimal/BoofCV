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

package boofcv.abst.transform.wavelet;

import boofcv.core.image.border.BorderType;
import boofcv.struct.image.ImageGray;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlCoef;


/**
 * <p>
 * Easy to use interface for performing a multilevel wavelet transformations.  Internally it handles
 * all buffer maintenance and type conversion.  To create a new instance of this interface use
 * {@link boofcv.factory.transform.wavelet.FactoryWaveletTransform}.
 * </p>
 * 
 * @see boofcv.alg.transform.wavelet.WaveletTransformOps
 *
 * @author Peter Abeles
 */
public interface WaveletTransform
		<O extends ImageGray, T extends ImageGray, C extends WlCoef >
{
	/**
	 * Computes the wavelet transform of the input image.  If no output/transform image is provided a new image is
	 * created and returned.
	 *
	 * @param original Original unmodified image. Not modified.
	 * @param transformed Where the computed transform is stored.  If null a new image is created. Modified.
	 * @return Wavelet transform.
	 */
	public T transform( O original , T transformed );

	/**
	 * Applies the inverse wavelet transform to the specified image.
	 *
	 * @param transformed Wavelet transform of the image. Not modified.
	 * @param original Reconstructed image from transform. Modified.
	 */
	public void invert( T transformed , O original );

	/**
	 * Number of levels in the wavelet transform.
	 *
	 * @return number of levels.
	 */
	public int getLevels();

	/**
	 * Returns how the borders are handled.
	 *
	 * @return Type of border used.
	 */
	public BorderType getBorderType();

	/**
	 * Description of the wavelet.
	 *
	 * @return wavelet description.
	 */
	public WaveletDescription<C> getDescription();

	public Class<O> getOriginalType();
}
