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

import boofcv.struct.ImageRectangle;


/**
 * Convolution kernel for an integral image.  Note that the bounds in the specified
 * rectangle are inclusive.  Normally the upper bounds are exclusive.
 *
 * @author Peter Abeles
 */
public class IntegralKernel {
	public ImageRectangle[] blocks;
	public int scales[];

	public IntegralKernel( int numBlocks ) {
		this.blocks = new ImageRectangle[numBlocks];
		this.scales = new int[ numBlocks ];

		for( int i = 0; i < numBlocks; i++ ) {
			blocks[i] = new ImageRectangle();
		}
	}

	public IntegralKernel() {
	}

	public int getNumBlocks() {
		return blocks.length;
	}

	public IntegralKernel copy() {
		IntegralKernel ret = new IntegralKernel( blocks.length );
		for( int i = 0; i < blocks.length; i++ ) {
			this.blocks[i] = new ImageRectangle(blocks[i]);
			this.scales[i] = scales[i];
		}

		return ret;
	}
}
