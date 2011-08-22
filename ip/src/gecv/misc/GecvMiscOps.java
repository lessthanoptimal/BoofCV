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

package gecv.misc;

import gecv.struct.ImageRectangle;
import gecv.struct.image.ImageBase;

/**
 * @author Peter Abeles
 */
public class GecvMiscOps {

	public static int countNotZero( int a[] , int size ) {
		int ret = 0;
		for( int i = 0; i < size; i++ ) {
			if( a[i] != 0 )
				ret++;
		}
		return ret;
	}

	public static double[] convertTo_F64( int a[] ) {
		double[] ret = new double[ a.length ];
		for( int i = 0; i < a.length; i++ ) {
			ret[i] = (int)a[i];
		}
		return ret;
	}

	/**
	 * Bounds the provided rectangle to be inside the image.  It is assumed that
	 * at least part of the rectangle is inside of the image so all possibilities
	 * do not need to be checked.
	 *
	 * @param b An image.
	 * @param r Rectangle
	 */
	public static void boundRectangleInside( ImageBase b , ImageRectangle r )
	{
		if( r.x0 < 0 )
			r.x0 = 0;
		else if( r.x1 > b.width )
			r.x1 = b.width;

		if( r.y0 < 0 )
			r.y0 = 0;
		else if( r.y1 > b.height )
			r.y1 = b.height;
	}
}
