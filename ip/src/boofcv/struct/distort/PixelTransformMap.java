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

package boofcv.struct.distort;

import jgrl.struct.point.Point2D_F32;


/**
 * Precomputed distortion mapping where the location of each pixel is precomputed.
 *
 * @author Peter Abeles
 */
public class PixelTransformMap extends PixelTransform {

	int width;
	int height;
	Point2D_F32 map[];

	public PixelTransformMap( int width , int height ) {
		this.width = width;
		this.height = height;

		map = new Point2D_F32[width*height];
		for( int i = 0; i < map.length; i++ ) {
			map[i] = new Point2D_F32();
		}
	}

	/**
	 * Sets the map using another {@link PixelTransform} to compute the distortion.
	 *
	 * @param distortion Distortion which is being precomputed.
	 */
	public void set( PixelTransform distortion ) {
		int index = 0;
		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				distortion.compute(x,y);
				map[index++].set(distortion.distX,distortion.distY);
			}
		}
	}

	/**
	 * Sets the transform for a specific pixel.
	 *
	 * @param x Original pixel x-coordinate.
	 * @param y Original pixel y-coordinate.
	 * @param distX Distorted pixel x-coordinate.
	 * @param distY Distorted pixel y-coordinate.
	 */
	public void set( int x , int y , float distX , float distY ) {
		if( x < 0 || x >= width )
			throw new IllegalArgumentException("x is out of bounds");
		if( y < 0 || y >= height )
			throw new IllegalArgumentException("y is out of bounds");

		map[y*width + x].set(distX,distY);
	}

	@Override
	public void compute(int x, int y) {
		Point2D_F32 p = map[y*width+x];
		distX = p.x;
		distY = p.y;
	}

}
