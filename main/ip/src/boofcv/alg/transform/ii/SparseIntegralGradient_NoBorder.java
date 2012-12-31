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

package boofcv.alg.transform.ii;

import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.sparse.GradientValue;
import boofcv.struct.sparse.SparseScaleGradient;


/**
 * Computes the gradient from an integral image.  Does not check for border conditions.
 * Much faster than generalized algorithms that can handle image borders, but is unsafe and
 * bounds must be checked before use.
 *
 * @author Peter Abeles
 */
public abstract class SparseIntegralGradient_NoBorder <T extends ImageSingleBand, G extends GradientValue>
		extends SparseScaleGradient<T, G>
{
	// the radius at a scale of one
	protected int baseR;
	
	// radius of the kernel
	protected int r;
	// width of the kernel
	protected int w;

	public SparseIntegralGradient_NoBorder(int baseRadius) {
		baseR = baseRadius;
		setScale(1);
	}

	@Override
	public void setScale(double scale) {
		r = (int)(baseR*scale+0.5);
		if( r < 0 )
			r = 1;
		w = r*2+1;
	}
}
