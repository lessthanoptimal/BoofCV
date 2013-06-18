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

package boofcv.misc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageFloat64;
import boofcv.struct.image.ImageInteger;
import boofcv.struct.image.ImageSingleBand;

import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.core.DefaultConverterLookup;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Miscellaneous functions which have no better place to go.
 *
 * @author Peter Abeles
 */
public class BoofMiscOps {

	public static void saveXML( Object o , String fileName ) {
		BoofcvClassLoader loader = new BoofcvClassLoader();
		XStreamAppletVersion xstream = new XStreamAppletVersion(new PureJavaReflectionProvider(),new DomDriver(),loader,null,new DefaultConverterLookup(), null);
//		XStream xstream = new XStream(new PureJavaReflectionProvider(),new DomDriver(),loader,null,new DefaultConverterLookup(), null);
//		xstream.registerConverter(new JavaBeanConverter(xstream.getMapper()));

		try {
			xstream.toXML(o,new FileOutputStream(fileName));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static <T> T loadXML( String fileName ) {

		BoofcvClassLoader loader = new BoofcvClassLoader();
		XStreamAppletVersion xstream = new XStreamAppletVersion(new PureJavaReflectionProvider(),new DomDriver(),loader,null,new DefaultConverterLookup(), null);
//		xstream.registerConverter(new JavaBeanConverter(xstream.getMapper()));
		try {
			return (T)xstream.fromXML(new FileReader(fileName));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T loadXML( Reader r ) {
		BoofcvClassLoader loader = new BoofcvClassLoader();
		XStreamAppletVersion xstream = new XStreamAppletVersion(new PureJavaReflectionProvider(),new DomDriver(),loader,null,new DefaultConverterLookup(), null);
//		xstream.registerConverter(new JavaBeanConverter(xstream.getMapper()));
		return (T)xstream.fromXML(r);
	}

	public static String toString( Reader r ) {
		char buff[] = new char[1024];

		StringBuilder string = new StringBuilder();
		try {
			while(true) {
				int size = r.read(buff);
			    if( size < 0 )
					break;
				string.append(buff, 0, size);
			}
			return string.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
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

	/**
	 * Returns true if the point is contained inside the image and 'radius' away from the image border.
	 *
	 * @param b Image
	 * @param x x-coordinate of point
	 * @param y y-coordinate of point
	 * @param radius How many pixels away from the border it needs to be to be considered inside
	 * @return true if the point is inside and false if it is outside
	 */
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

	/**
	 * Returns true if the point is contained inside the image and 'radius' away from the image border.
	 *
	 * @param b Image
	 * @param x x-coordinate of point
	 * @param y y-coordinate of point
	 * @param radius How many pixels away from the border it needs to be to be considered inside
	 * @return true if the point is inside and false if it is outside
	 */
	public static boolean checkInside(ImageBase b, float x , float y , float radius ) {
		if( x-radius < 0 )
			return false;
		if( x+radius > b.width-1 )
			return false;

		if( y-radius < 0 )
			return false;
		if( y+radius > b.height-1 )
			return false;
		return true;
	}

	/**
	 * Returns true if the point is contained inside the image and 'radius' away from the image border.
	 *
	 * @param b Image
	 * @param x x-coordinate of point
	 * @param y y-coordinate of point
	 * @param radius How many pixels away from the border it needs to be to be considered inside
	 * @return true if the point is inside and false if it is outside
	 */
	public static boolean checkInside(ImageBase b, double x , double y , double radius ) {
		if( x-radius < 0 )
			return false;
		if( x+radius > b.width-1 )
			return false;

		if( y-radius < 0 )
			return false;
		if( y+radius > b.height-1 )
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

	/**
	 * Invokes wait until the elapsed time has passed.  In the thread is interrupted, the interrupt is ignored.
	 * @param milli Length of desired pause in milliseconds.
	 */
	public static void pause(long milli) {
		final Thread t = Thread.currentThread();
		long start = System.currentTimeMillis();
		while( System.currentTimeMillis() - start < milli ) {
			synchronized( t )  {
				try {
					long target = milli - (System.currentTimeMillis() - start);
					if( target > 0 )
						t.wait(target);
				} catch (InterruptedException ignore) {
				}
			}
		}
	}

	public static void print(ImageSingleBand a) {

		if( a.getTypeInfo().isInteger() ) {
			print((ImageInteger)a);
		} else if( a instanceof ImageFloat32) {
			print((ImageFloat32)a);
		} else {
			print((ImageFloat64)a);
		}
	}

	public static void print(ImageFloat64 a) {
		for (int y = 0; y < a.height; y++) {
			for (int x = 0; x < a.width; x++) {
				System.out.printf("%6.2f ", a.get(x, y));
			}
			System.out.println();
		}
		System.out.println();
	}

	public static void print(ImageFloat32 a) {
		for (int y = 0; y < a.height; y++) {
			for (int x = 0; x < a.width; x++) {
				System.out.printf("%6.2f ", a.get(x, y));
			}
			System.out.println();
		}
		System.out.println();
	}

	public static void print(ImageInteger a) {
		for (int y = 0; y < a.height; y++) {
			for (int x = 0; x < a.width; x++) {
				System.out.printf("%4d ", a.get(x, y));
			}
			System.out.println();
		}
		System.out.println();
	}

	/**
	 * Loads a list of files with the specified prefix.
	 *
	 * @param directory Directory it looks inside of
	 * @param prefix Prefix that the file must have
	 * @return List of files that are in the directory and match the prefix.
	 */
	public static List<String> directoryList( String directory , String prefix ) {
		List<String> ret = new ArrayList<String>();

		File d = new File(directory);

		if( !d.isDirectory() )
			throw new IllegalArgumentException("Must specify an directory");

		File files[] = d.listFiles();

		for( File f : files ) {
			if( f.isDirectory() || f.isHidden() )
				continue;

			if( f.getName().contains(prefix )) {
				ret.add(f.getAbsolutePath());
			}
		}

		return ret;
	}
}
