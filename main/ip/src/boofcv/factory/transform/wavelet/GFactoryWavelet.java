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

import boofcv.core.image.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayI;
import boofcv.struct.image.ImageGray;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlCoef;


/**
 * Creates different wavelet transform by specifying the image type.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class GFactoryWavelet {

	public static <C extends WlCoef, T extends ImageGray>
	WaveletDescription<C> haar( Class<T> imageType )
	{
		if( imageType == GrayF32.class )
			return FactoryWaveletHaar.generate(false,32);
		else if( GrayI.class.isAssignableFrom(imageType) ) {
			return FactoryWaveletHaar.generate(true,32);
		} else {
			return null;
		}
	}

	public static <C extends WlCoef, T extends ImageGray>
	WaveletDescription<C> daubJ( Class<T> imageType , int J )
	{
		if( imageType == GrayF32.class )
			return (WaveletDescription<C>)FactoryWaveletDaub.daubJ_F32(J);
		else {
			return null;
		}
	}

	public static <C extends WlCoef, T extends ImageGray>
	WaveletDescription<C> biorthogoal( Class<T> imageType , int J , BorderType borderType)
	{
		if( imageType == GrayF32.class )
			return (WaveletDescription<C>)FactoryWaveletDaub.biorthogonal_F32(J,borderType);
		else if( GrayI.class.isAssignableFrom(imageType) ) {
			return (WaveletDescription<C>)FactoryWaveletDaub.biorthogonal_I32(J,borderType);
		} else {
			return null;
		}
	}

	public static <C extends WlCoef, T extends ImageGray>
	WaveletDescription<C> coiflet( Class<T> imageType , int J )
	{
		if( imageType == GrayF32.class )
			return (WaveletDescription<C>)FactoryWaveletCoiflet.generate_F32(J);
		else
			return null;
	}
}
