/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.line;

import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

/**
 * Concurrent version of {@link HoughTransformGradient}
 *
 * @author Peter Abeles
 */
public class HoughTransformGradient_MT<D extends ImageGray<D>>
	extends HoughTransformGradient<D>
{

	/**
	 * Specifies parameters of transform.
	 *
	 * @param extractor  Extracts local maxima from transform space.  A set of candidates is provided, but can be ignored.
	 * @param parameters
	 * @param derivType
	 */
	public HoughTransformGradient_MT(NonMaxSuppression extractor, HoughTransformParameters parameters, Class<D> derivType) {
		super(extractor, parameters, derivType);
	}

	@Override
	void transform(GrayU8 binary )
	{
		BoofConcurrency.loopFor(0,binary.height,y->{
			int start = binary.startIndex + y*binary.stride;
			int end = start + binary.width;

			for( int index = start; index < end; index++ ) {
				if( binary.data[index] != 0 ) {
					int x = index-start;
					parameterize(x,y,_derivX.unsafe_getF(x,y),_derivY.unsafe_getF(x,y));
				}
			}
		});
	}

	protected void addParameters( int x , int y , float amount ) {
		if( transform.isInBounds(x,y)) {
			int index = transform.startIndex+y*transform.stride+x;
			// keep track of candidate pixels so that a sparse search can be done
			// to detect lines
			if( transform.data[index] == 0 ) {
				synchronized (candidates) {
					candidates.add(x, y);
				}
			}
			transform.data[index] += amount;
		}
	}
}
