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

package boofcv.factory.transform.ii;

import boofcv.alg.transform.ii.impl.*;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.sparse.SparseScaleGradient;
import boofcv.struct.sparse.SparseScaleSample_F64;


/**
 * Creates various filters for {@link boofcv.alg.transform.ii.IntegralImageOps integral images}.
 *
 * @author Peter Abeles
 */
public class FactorySparseIntegralFilters {

	public static <T extends ImageSingleBand>
	SparseScaleSample_F64<T> sample( int radius , Class<T> imageType ) {
		if( imageType == ImageFloat32.class )
			return (SparseScaleSample_F64<T>)new SparseIntegralSample_F32(radius);
		else if( imageType == ImageSInt32.class )
			return (SparseScaleSample_F64<T>)new SparseIntegralSample_I32(radius);
		else
			throw new IllegalArgumentException("Unsupported image type: "+imageType.getSimpleName());
	}

	public static <T extends ImageSingleBand>
	SparseScaleGradient<T,?> gradient( int radius , Class<T> imageType ) {
		if( imageType == ImageFloat32.class )
			return (SparseScaleGradient<T,?>)new SparseIntegralGradient_NoBorder_F32(radius);
		else if( imageType == ImageSInt32.class )
			return (SparseScaleGradient<T,?>)new SparseIntegralGradient_NoBorder_I32(radius);
		else
			throw new IllegalArgumentException("Unsupported image type: "+imageType.getSimpleName());
	}

	public static <T extends ImageSingleBand>
	SparseScaleGradient<T,?> haar( int radius , Class<T> imageType ) {
		if( imageType == ImageFloat32.class )
			return (SparseScaleGradient<T,?>)new SparseIntegralHaar_NoBorder_F32(radius);
		else if( imageType == ImageSInt32.class )
			return (SparseScaleGradient<T,?>)new SparseIntegralHaar_NoBorder_I32(radius);
		else
			throw new IllegalArgumentException("Unsupported image type: "+imageType.getSimpleName());
	}
}
