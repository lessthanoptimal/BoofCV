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

package gecv.alg.filter.blur;

import gecv.alg.filter.blur.impl.MedianHistogramInner_I8;
import gecv.alg.filter.blur.impl.MedianSortNaive_F32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageUInt8;

/**
 * Factory for creating median filters.  It automatically selects the best general purpose filter for the
 * specific radius.
 *
 * @author Peter Abeles
 */
public class MedianFilterFactory {

	public static MedianImageFilter<ImageUInt8> create_I8( int radius ) {
		// todo process outer part of the image
		return new MedianHistogramInner_I8(radius);
	}

	public static MedianImageFilter<ImageFloat32> create_F32( int radius ) {
		return new MedianSortNaive_F32(radius);
	}
}
