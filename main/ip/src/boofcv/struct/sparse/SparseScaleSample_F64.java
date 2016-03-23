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

package boofcv.struct.sparse;

import boofcv.struct.image.ImageGray;

/**
 * Samples the image using a kernel which can be rescaled
 *
 * @author Peter Abeles
 */
public abstract class SparseScaleSample_F64<T extends ImageGray>
		implements SparseImageSample_F64<T>
{
	protected T input;

	// defines the kernel's bounds
	protected int x0,y0,x1,y1;

	/**
	 * Width of the sampled region in pixels
	 * @param width pixels
	 */
	public abstract void setWidth( double width );

	@Override
	public void setImage(T input ) {
		this.input = input;
	}

	@Override
	public boolean isInBounds( int x , int y ) {
		return( x+x0 >= 0 && y+y0 >= 0 && x+x1 < input.width && y+y1 < input.height );
	}
}
