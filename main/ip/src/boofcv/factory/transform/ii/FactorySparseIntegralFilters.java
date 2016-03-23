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

package boofcv.factory.transform.ii;

import boofcv.alg.transform.ii.impl.*;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.sparse.SparseScaleGradient;
import boofcv.struct.sparse.SparseScaleSample_F64;


/**
 * Creates various filters for {@link boofcv.alg.transform.ii.IntegralImageOps integral images}.
 *
 * @author Peter Abeles
 */
public class FactorySparseIntegralFilters {

	public static <T extends ImageGray>
	SparseScaleSample_F64<T> sample( Class<T> imageType ) {
		if( imageType == GrayF32.class )
			return (SparseScaleSample_F64<T>)new SparseIntegralSample_F32();
		else if( imageType == GrayS32.class )
			return (SparseScaleSample_F64<T>)new SparseIntegralSample_I32();
		else
			throw new IllegalArgumentException("Unsupported image type: "+imageType.getSimpleName());
	}

	public static <T extends ImageGray>
	SparseScaleGradient<T,?> gradient( Class<T> imageType ) {
		if( imageType == GrayF32.class )
			return (SparseScaleGradient<T,?>)new SparseIntegralGradient_NoBorder_F32();
		else if( imageType == GrayS32.class )
			return (SparseScaleGradient<T,?>)new SparseIntegralGradient_NoBorder_I32();
		else
			throw new IllegalArgumentException("Unsupported image type: "+imageType.getSimpleName());
	}

	public static <T extends ImageGray>
	SparseScaleGradient<T,?> haar( Class<T> imageType ) {
		if( imageType == GrayF32.class )
			return (SparseScaleGradient<T,?>)new SparseIntegralHaar_NoBorder_F32();
		else if( imageType == GrayS32.class )
			return (SparseScaleGradient<T,?>)new SparseIntegralHaar_NoBorder_I32();
		else
			throw new IllegalArgumentException("Unsupported image type: "+imageType.getSimpleName());
	}
}
