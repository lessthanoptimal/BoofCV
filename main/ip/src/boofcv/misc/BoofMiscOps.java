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

package boofcv.misc;

import boofcv.struct.ImageRectangle;
import boofcv.struct.image.*;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Miscellaneous functions which have no better place to go.
 *
 * @author Peter Abeles
 */
public class BoofMiscOps {

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

	public static boolean checkInside( ImageBase b , float x , float y ) {
		return x >= 0 && x <= b.width-1 && y >= 0 && y <= b.height-1;
	}

	public static boolean checkInside( ImageBase b , double x , double y ) {
		return x >= 0 && x <= b.width-1 && y >= 0 && y <= b.height-1;
	}

	public static boolean checkInside( int width , int height , float x , float y ) {
		return x >= 0 && x <= width-1 && y >= 0 && y <= height-1;
	}

	public static boolean checkInside( int width , int height , double x , double y ) {
		return x >= 0 && x <= width-1 && y >= 0 && y <= height-1;
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

	public static void print(ImageGray a) {

		if( a.getDataType().isInteger() ) {
			print((GrayI)a);
		} else if( a instanceof GrayF32) {
			print((GrayF32)a);
		} else {
			print((GrayF64)a);
		}
	}

	public static void print(GrayF64 a) {
		for (int y = 0; y < a.height; y++) {
			for (int x = 0; x < a.width; x++) {
				System.out.printf("%6.2f ", a.get(x, y));
			}
			System.out.println();
		}
		System.out.println();
	}

	public static void print(GrayF32 a) {
		for (int y = 0; y < a.height; y++) {
			for (int x = 0; x < a.width; x++) {
				System.out.printf("%6.2f ", a.get(x, y));
			}
			System.out.println();
		}
		System.out.println();
	}

	public static void print(InterleavedF32 a) {
		for (int y = 0; y < a.height; y++) {
			for (int x = 0; x < a.width; x++) {
				System.out.print("|");
				for( int band = 0; band < a.numBands; band++ ) {
					System.out.printf(" %6.2f", a.getBand(x, y,band));
				}
				System.out.print(" |");

			}
			System.out.println();
		}
		System.out.println();
	}

	public static void print(GrayI a) {
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
		List<String> ret = new ArrayList<>();

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

	/**
	 * <p>
	 * Looks for file names which match the regex in the directory.
	 * </p>
	 * <p>
	 * Example:<br>
	 * BoofMiscOps.findMatches(new File("/path/to/directory"), ".+jpg");
	 * </p>
	 *
	 * @param directory directory
	 * @param regex file name regex
	 * @return array of matching files
	 */
	public static File[] findMatches( File directory , String regex ) {
		final Pattern p = Pattern.compile(regex); // careful: could also throw an exception!
		return directory.listFiles(new FileFilter(){
			@Override
			public boolean accept(File file) {
				return p.matcher(file.getName()).matches();
			}
		});
	}

	/**
	 * Looks up all files which are in the specified directory and match the regex.<br>
	 * Example: path/to/input/image\d*.jpg
	 *
	 * @param pathRegex regex
	 * @return list of matching files
	 */
	public static File[] findMatches( String pathRegex ) {

		File f = new File(pathRegex);
		File directory = f.getParentFile();
		String regex = f.getName();

		final Pattern p = Pattern.compile(regex); // careful: could also throw an exception!
		return directory.listFiles(new FileFilter(){
			@Override
			public boolean accept(File file) {
				return p.matcher(file.getName()).matches();
			}
		});
	}

	public static int[] convertArray( double input[] , int output[] ) {
		if( output == null )
			output = new int[input.length];

		for (int i = 0; i < input.length; i++) {
			output[i] = (int)input[i];
		}

		return output;
	}

	public static long[] convertArray( double input[] , long output[] ) {
		if( output == null )
			output = new long[input.length];

		for (int i = 0; i < input.length; i++) {
			output[i] = (long)input[i];
		}

		return output;
	}

	public static float[] convertArray( double input[] , float output[] ) {
		if( output == null )
			output = new float[input.length];

		for (int i = 0; i < input.length; i++) {
			output[i] = (float)input[i];
		}

		return output;
	}

	public static double[] convertArray( float input[] , double output[] ) {
		if( output == null )
			output = new double[input.length];

		for (int i = 0; i < input.length; i++) {
			output[i] = input[i];
		}

		return output;
	}

	public static int[] convertArray( float input[] , int output[] ) {
		if( output == null )
			output = new int[input.length];

		for (int i = 0; i < input.length; i++) {
			output[i] = (int)input[i];
		}

		return output;
	}

	public static float[] convertArray( int input[] , float output[] ) {
		if( output == null )
			output = new float[input.length];

		for (int i = 0; i < input.length; i++) {
			output[i] = input[i];
		}

		return output;
	}
}
