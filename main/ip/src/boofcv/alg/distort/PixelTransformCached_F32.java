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

package boofcv.alg.distort;

import boofcv.struct.distort.PixelTransform2_F32;
import georegression.struct.point.Point2D_F32;

/**
 * Precomputes transformations for each pixel in the image.  Doesn't check bounds and will give an incorrect result
 * or crash if outside pixels are requested.
 *
 * @author Peter Abeles
 */
public class PixelTransformCached_F32 extends PixelTransform2_F32 {

	Point2D_F32 map[];
	int width,height;

	public PixelTransformCached_F32(int width, int height, PixelTransform2_F32 transform ) {
		this.width = width+1; // add one to the width since some stuff checks the outside border
		this.height = height+1;


		map = new Point2D_F32[this.width*this.height];
		int index = 0;
		for (int y = 0; y < this.height; y++) {
			for (int x = 0; x < this.width; x++) {
				transform.compute(x,y);
				map[index++] = new Point2D_F32(transform.distX,transform.distY);
			}
		}
	}

	@Override
	public void compute(int x, int y) {
//		if( x < 0 || y < 0 || x >= width || y >= height )
//			throw new IllegalArgumentException("Out of bounds");

		Point2D_F32 p = map[y*width+x];
		distX = p.x;
		distY = p.y;
	}
}
