/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.distort.Point2Transform2_F32;
import georegression.struct.point.Point2D_F32;
import org.ejml.UtilEjml;

/**
 * Precomputes transformations for each pixel in the image.  Doesn't check bounds and will give an incorrect result
 * or crash if outside pixels are requested.
 *
 * @author Peter Abeles
 */
public class PixelTransformCached_F32 extends PixelTransform2_F32 {

	Point2D_F32 map[];
	int width,height;

	boolean ignoreNaN = true;

	public PixelTransformCached_F32(int width, int height, Point2Transform2_F32 transform ) {
		this(width,height, new PointToPixelTransform_F32(transform));
	}

	public PixelTransformCached_F32(int width, int height, PixelTransform2_F32 transform ) {
		this.width = width+1; // add one to the width since some stuff checks the outside border
		this.height = height+1;

		map = new Point2D_F32[this.width*this.height];
		int index = 0;
		for (int y = 0; y < this.height; y++) {
			for (int x = 0; x < this.width; x++) {
				transform.compute(x,y);

				// It's not obvious what to do if the pixel is invalid
				// If left as uncountable it can mess up the processing completely later on.
				// Figured a pixel out of the image at -1,-1 might get someone's attention that something is up
				if( !ignoreNaN && (UtilEjml.isUncountable(transform.distX) || UtilEjml.isUncountable(transform.distY)) ) {
					map[index++] = new Point2D_F32(-1,-1);
				} else {
					map[index++] = new Point2D_F32(transform.distX, transform.distY);
				}
			}
		}
	}

	public Point2D_F32 getPixel( int x, int y ) {
		return map[width*y + x];
	}

	public boolean isIgnoreNaN() {
		return ignoreNaN;
	}

	public void setIgnoreNaN(boolean ignoreNaN) {
		this.ignoreNaN = ignoreNaN;
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
