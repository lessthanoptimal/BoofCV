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

package boofcv.alg.tracker.tld;

import boofcv.struct.ImageRectangle;
import georegression.struct.shapes.RectangleCorner2D_F64;
import georegression.struct.shapes.RectangleCorner2D_I32;

/**
 * @author Peter Abeles
 */
public class TldHelperFunctions {

	// storage for intermediate results
	private ImageRectangle work = new ImageRectangle();

	/**
	 * Computes the fractional area of intersection between the two regions.
	 *
	 * @return number from 0 to 1.  higher means more intersection
	 */
	public double computeOverlap( ImageRectangle a , ImageRectangle b ) {
		if( !a.intersection(b,work) )
			return 0;

		int areaI = work.area();

		int bottom = a.area() + b.area() - areaI;

		return areaI/ (double)bottom;
	}

	public static void convertRegion(RectangleCorner2D_F64 input, RectangleCorner2D_I32 output) {
		output.x0 = (int)(input.x0+0.5);
		output.x1 = (int)(input.x1+0.5);
		output.y0 = (int)(input.y0+0.5);
		output.y1 = (int)(input.y1+0.5);
	}

	public static void convertRegion(RectangleCorner2D_I32 input, RectangleCorner2D_F64 output) {
		output.x0 = input.x0;
		output.x1 = input.x1;
		output.y0 = input.y0;
		output.y1 = input.y1;
	}
}
