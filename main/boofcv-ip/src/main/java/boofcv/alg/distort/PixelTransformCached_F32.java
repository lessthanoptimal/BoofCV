/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.distort.PixelTransform;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.PointToPixelTransform_F32;
import georegression.struct.point.Point2D_F32;
import org.ejml.UtilEjml;

/**
 * Precomputes transformations for each pixel in the image. Doesn't check bounds and will give an incorrect result
 * or crash if outside pixels are requested.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class PixelTransformCached_F32 implements PixelTransform<Point2D_F32> {

	Point2D_F32[] map;
	int width, height;

	boolean ignoreNaN = true;

	public PixelTransformCached_F32( int width, int height, Point2Transform2_F32 transform ) {
		this(width, height, new PointToPixelTransform_F32(transform));
	}

	public PixelTransformCached_F32( int width, int height, PixelTransform<Point2D_F32> transform ) {
		this.width = width + 1; // add one to the width since some stuff checks the outside border
		this.height = height + 1;

		map = new Point2D_F32[this.width*this.height];
		int index = 0;
		for (int y = 0; y < this.height; y++) {
			for (int x = 0; x < this.width; x++) {
				Point2D_F32 p = new Point2D_F32();
				transform.compute(x, y, p);

				// It's not obvious what to do if the pixel is invalid
				// If left as uncountable it can mess up the processing completely later on.
				// Figured a pixel out of the image at -1,-1 might get someone's attention that something is up
				if (!ignoreNaN && (UtilEjml.isUncountable(p.x) || UtilEjml.isUncountable(p.y))) {
					p.setTo(-1, -1);
				}
				map[index++] = p;
			}
		}
	}

	PixelTransformCached_F32() {}

	public Point2D_F32 getPixel( int x, int y ) {
		return map[width*y + x];
	}

	public boolean isIgnoreNaN() {
		return ignoreNaN;
	}

	public void setIgnoreNaN( boolean ignoreNaN ) {
		this.ignoreNaN = ignoreNaN;
	}

	@Override
	public void compute( int x, int y, Point2D_F32 output ) {
//		if( x < 0 || y < 0 || x >= width || y >= height )
//			throw new IllegalArgumentException("Out of bounds");

		output.setTo(map[y*width + x]);
	}

	@Override
	public PixelTransform<Point2D_F32> copyConcurrent() {
		PixelTransformCached_F32 ret = new PixelTransformCached_F32();
		ret.map = new Point2D_F32[this.map.length];
		for (int i = 0; i < ret.map.length; i++) {
			ret.map[i] = this.map[i].copy();
		}

		ret.width = this.width;
		ret.height = this.height;
		ret.ignoreNaN = this.ignoreNaN;

		return ret;
	}
}
