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

package boofcv.factory.transform.wavelet;

import boofcv.abst.transform.wavelet.WaveletTransform;
import boofcv.abst.transform.wavelet.impl.WaveletTransformFloat32;
import boofcv.abst.transform.wavelet.impl.WaveletTransformInt;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayI;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlCoef;
import boofcv.struct.wavelet.WlCoef_F32;
import boofcv.struct.wavelet.WlCoef_I32;


/**
 * Simplified factory for creating {@link boofcv.abst.transform.wavelet.WaveletTransform}.  Factories are provided
 * for creating the different wavelet descriptions.
 *
 * @author Peter Abeles
 */
public class FactoryWaveletTransform {


	@SuppressWarnings({"unchecked"})
	public static <T extends ImageGray, W extends ImageGray, C extends WlCoef>
	WaveletTransform<T,W,C> create( Class<T> imageType , WaveletDescription<C> waveletDesc , int numLevels ,
									double minPixelValue , double maxPixelValue)
	{
		if( waveletDesc.getForward().getType() == float.class ) {
			return (WaveletTransform<T,W,C>)create_F32((WaveletDescription)waveletDesc,numLevels,
					(float)minPixelValue,(float)maxPixelValue);
		} else if( waveletDesc.getForward().getType() == int.class ) {
			return (WaveletTransform<T,W,C>)create_I((WaveletDescription)waveletDesc,numLevels,
			(int)minPixelValue,(int)maxPixelValue,(Class)imageType);
		} else {
			throw new RuntimeException("Add support for this image type");
		}
	}

	/**
	 * Creates a wavelet transform for images that are derived from {@link GrayI}.
	 *
	 * @param waveletDesc Description of the wavelet.
	 * @param numLevels Number of levels in the multi-level transform.
	 * @param minPixelValue Minimum pixel intensity value
	 * @param maxPixelValue Maximum pixel intensity value
	 * @return The transform class.
	 */
	public static <T extends GrayI>
	WaveletTransform<T, GrayS32,WlCoef_I32>
	create_I( WaveletDescription<WlCoef_I32> waveletDesc ,
			  int numLevels , int minPixelValue , int maxPixelValue ,
			  Class<T> imageType )
	{
		return new WaveletTransformInt<>(waveletDesc, numLevels, minPixelValue, maxPixelValue, imageType);
	}

	/**
	 * Creates a wavelet transform for images that are of type {@link GrayF32}.
	 *
	 * @param waveletDesc Description of the wavelet.
	 * @param numLevels Number of levels in the multi-level transform.
	 * @param minPixelValue Minimum pixel intensity value
	 * @param maxPixelValue Maximum pixel intensity value
	 * @return The transform class.
	 */
	public static
	WaveletTransform<GrayF32, GrayF32,WlCoef_F32>
	create_F32( WaveletDescription<WlCoef_F32> waveletDesc ,
				int numLevels, float minPixelValue , float maxPixelValue )
	{
		return new WaveletTransformFloat32(waveletDesc,numLevels,minPixelValue,maxPixelValue);
	}
}
