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

package boofcv.struct;

import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I16;
import georegression.struct.point.Point2D_I32;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;

/**
 * Compact format for storing 2D points as a single integer in an array. Offers much more efficient memory and
 * possibly fewer cache misses.
 *
 * @author Peter Abeles
 */
public class ListIntPoint2D {
	/** Points encoded into a single integer value. y*width + x */
	@Getter DogArray_I32 points = new DogArray_I32();
	int imageWidth;

	/**
	 * Specifies the image's width and height. used for encoding. Also resets the list.
	 */
	public void configure( int width, int height ) {
		this.imageWidth = width;
		points.reset();
	}

	/**
	 * Adds a new point to the list
	 */
	public void add( int x, int y ) {
		points.add(y*imageWidth + x);
	}

	/**
	 * Retrieves a point from the list
	 */
	public void get( int index, Point2D_I16 p ) {
		int v = points.data[index];
		p.x = (short)(v%imageWidth);
		p.y = (short)(v/imageWidth);
	}

	/**
	 * Retrieves a point from the list
	 */
	public void get( int index, Point2D_I32 p ) {
		int v = points.data[index];
		p.x = v%imageWidth;
		p.y = v/imageWidth;
	}

	/**
	 * Retrieves a point from the list
	 */
	public void get( int index, Point2D_F32 p ) {
		int v = points.data[index];
		p.x = (float)(v%imageWidth);
		p.y = (float)(v/imageWidth);
	}

	/**
	 * Retrieves a point from the list
	 */
	public void get( int index, Point2D_F64 p ) {
		int v = points.data[index];
		p.x = (float)(v%imageWidth);
		p.y = (float)(v/imageWidth);
	}

	/**
	 * Returns the coordinate as a {@link Point2D_I32}. not recommended due to the creation of a new point
	 * each call. Provided for backwards compatibility
	 */
	public Point2D_I32 get( int index ) {
		Point2D_I32 p = new Point2D_I32();
		get(index, p);
		return p;
	}

	/**
	 * Copy points from 'this' into 'dst'
	 */
	public void copyInto( DogArray<Point2D_I16> dst ) {
		for (int i = 0; i < points.size; i++) {
			get(i, dst.grow());
		}
	}

	public int size() {
		return points.size;
	}
}
