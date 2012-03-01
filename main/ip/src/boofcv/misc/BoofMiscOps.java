/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.misc;

import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageBase;

/**
 * @author Peter Abeles
 */
public class BoofMiscOps {

	public static int countNotZero( int a[] , int size ) {
		int ret = 0;
		for( int i = 0; i < size; i++ ) {
			if( a[i] != 0 )
				ret++;
		}
		return ret;
	}

	public static void zero( byte a[] , int size ) {
		for( int i = 0; i < size; i++ ) {
			a[i] = 0;
		}
	}

	public static void zero( short a[] , int size ) {
		for( int i = 0; i < size; i++ ) {
			a[i] = 0;
		}
	}

	public static void zero( int a[] , int size ) {
		for( int i = 0; i < size; i++ ) {
			a[i] = 0;
		}
	}

	public static void zero( float a[] , int size ) {
		for( int i = 0; i < size; i++ ) {
			a[i] = 0;
		}
	}

	public static void zero( double a[] , int size ) {
		for( int i = 0; i < size; i++ ) {
			a[i] = 0;
		}
	}

	public static double[] convertTo_F64( int a[] ) {
		double[] ret = new double[ a.length ];
		for( int i = 0; i < a.length; i++ ) {
			ret[i] = (int)a[i];
		}
		return ret;
	}

	public static float[] convertTo_F32( double a[] , float[] ret) {
		if( ret == null )
			ret = new float[ a.length ];
		for( int i = 0; i < a.length; i++ ) {
			ret[i] = (float)a[i];
		}
		return ret;
	}

	public static int[] convertTo_I32( double a[] , int[] ret) {
		if( ret == null )
			ret = new int[ a.length ];
		for( int i = 0; i < a.length; i++ ) {
			ret[i] = (int)a[i];
		}
		return ret;
	}


	/**
	 * Bounds the provided rectangle to be inside the image.  
	 *
	 * @param b An image.
	 * @param r Rectangle
	 */
	public static void boundRectangleInside( ImageBase b , ImageRectangle r )
	{
		if( r.x0 < 0 )
			r.x0 = 0;
		if( r.x1 > b.width )
			r.x1 = b.width;

		if( r.y0 < 0 )
			r.y0 = 0;
		if( r.y1 > b.height )
			r.y1 = b.height;
	}

	public static boolean checkInside(ImageBase b, ImageRectangle r) {
		if( r.x0 < 0 )
			return false;
		if( r.x1 > b.width )
			return false;

		if( r.y0 < 0 )
			return false;
		if( r.y1 > b.height )
			return false;
		return true;
	}

	public static boolean checkInside(ImageBase b, int x , int y , int radius ) {
		if( x-radius < 0 )
			return false;
		if( x+radius >= b.width )
			return false;

		if( y-radius < 0 )
			return false;
		if( y+radius >= b.height )
			return false;
		return true;
	}

	public static boolean checkInside(ImageBase b, int x , int y , int radiusWidth , int radiusHeight ) {
		if( x-radiusWidth < 0 )
			return false;
		if( x+radiusWidth >= b.width )
			return false;

		if( y-radiusHeight < 0 )
			return false;
		if( y+radiusHeight >= b.height )
			return false;
		return true;
	}

	public static boolean checkInside(ImageBase b, int c_x , int c_y , int radius , double theta ) {
		int r = radius;
		float c = (float)Math.cos(theta);
		float s = (float)Math.sin(theta);

		// make sure the region is inside the image
		if( !checkInBounds(b,c_x,c_y,-r,-r,c,s))
			return false;
		else if( !checkInBounds(b,c_x,c_y,-r,r,c,s))
			return false;
		else if( !checkInBounds(b,c_x,c_y,r,r,c,s))
			return false;
		else if( !checkInBounds(b,c_x,c_y,r,-r,c,s))
			return false;

		return true;
	}

	private static boolean checkInBounds( ImageBase b , int c_x , int c_y , int dx , int dy , float c , float s )
	{
		float x = c_x + c*dx - s*dy;
		float y = c_y + s*dx + c*dy;

		return b.isInBounds((int) x, (int) y);
	}

	public static void pause(int milli) {
		Thread t = Thread.currentThread();
		synchronized( t )  {
			try {
				t.wait(milli);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
