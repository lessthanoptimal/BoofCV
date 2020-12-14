/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.errors.BoofCheckFailure;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.*;
import georegression.struct.GeoTuple;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.*;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;
import pabeles.concurrency.ConcurrencyOps;
import pabeles.concurrency.GrowArray;

import java.io.*;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Miscellaneous functions which have no better place to go.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("rawtypes")
public class BoofMiscOps {

	/**
	 * Computes the elapsed time in calling the provided function in nano seconds
	 */
	public static long timeNano( BoofLambdas.ProcessCall process ) {
		long time0 = System.nanoTime();
		process.process();
		long time1 = System.nanoTime();
		return time1-time0;
	}

	public static void profile( BoofLambdas.ProcessCall process, String description ) {
		long nano = timeNano(process);
		System.out.println("Elapsed: "+(nano*1e-6)+" (ms) "+description);
	}

	public static String timeStr() {
		return Instant.now()
				.atOffset(ZoneOffset.UTC)
				.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
				.replace("T", " ");
	}

	public static String timeStr( long systemTimeMS ) {
		return Instant.ofEpochMilli(systemTimeMS)
				.atOffset(ZoneOffset.UTC)
				.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
				.replace("T", " ");
	}

	public static float bound( float value, float min, float max ) {
		if (value <= min)
			return min;
		if (value >= max)
			return max;
		return value;
	}

	public static double bound( double value, double min, double max ) {
		if (value <= min)
			return min;
		if (value >= max)
			return max;
		return value;
	}

	public static void offsetPixels( final List<Point2D_F64> list, final double dx, final double dy ) {
		for (int i = 0; i < list.size(); i++) {
			Point2D_F64 p = list.get(i);
			p.x += dx;
			p.y += dy;
		}
	}

	public static String[] toStringArray( List<File> files ) {
		String[] output = new String[files.size()];
		for (int i = 0; i < files.size(); i++) {
			output[i] = files.get(i).getPath();
		}
		return output;
	}

	/**
	 * Copies all of src into dst by appended it onto it
	 */
	public static <Point extends GeoTuple<Point>>
	void copyAll( FastAccess<Point> src, DogArray<Point> dst ) {
		dst.reserve(dst.size + src.size);
		for (int i = 0; i < src.size; i++) {
			dst.grow().setTo(src.get(i));
		}
	}

	public static List<File> toFileList( String[] files ) {
		List<File> output = new ArrayList<>();
		for (String s : files) { // lint:forbidden ignore_line
			output.add(new File(s));
		}
		return output;
	}

	// Remove when Minimum Java version is 11
	public static <T> List<T> asList( T... objects ) {
		List<T> list = new ArrayList<>();
		for (int i = 0; i < objects.length; i++) {
			list.add(objects[i]);
		}
		return list;
	}

	public static List<File> toFileList( List<String> files ) {
		List<File> output = new ArrayList<>();
		for (String s : files) { // lint:forbidden ignore_line
			output.add(new File(s));
		}
		return output;
	}

	public static int bitsToWords( int bits, int wordBits ) {
		return (bits/wordBits) + (bits%wordBits == 0 ? 0 : 1);
	}

	public static int numBands( ImageBase img ) {
		if (img instanceof ImageMultiBand) {
			return ((ImageMultiBand)img).getNumBands();
		} else {
			return 1;
		}
	}

	public static String milliToHuman( long milliseconds ) {
		long second = (milliseconds/1000)%60;
		long minute = (milliseconds/(1000*60))%60;
		long hour = (milliseconds/(1000*60*60))%24;
		long days = milliseconds/(1000*60*60*24);

		return String.format("%03d:%02d:%02d:%02d (days:hrs:min:sec)", days, hour, minute, second);
	}

	public static double diffRatio( double a, double b ) {
		return Math.abs(a - b)/Math.max(a, b);
	}

	/**
	 * Returns the number of digits in a number. E.g. 345 = 3, -345 = 4, 0 = 1
	 */
	public static int numDigits( int number ) {
		if (number == 0)
			return 1;
		int adjustment = 0;
		if (number < 0) {
			adjustment = 1;
			number = -number;
		}
		return adjustment + (int)Math.log10(number) + 1;
	}

	public static double pow2( double v ) {
		return v*v;
	}

	public static float pow2( float v ) {
		return v*v;
	}

	public static void sortFileNames( List<String> images ) {
		images.sort(new CompareStringNames());
	}

	public static void sortFilesByName( List<File> images ) {
		images.sort(Comparator.comparing(File::getName));
	}

	public static void printMethodInfo( Method target, PrintStream out ) {
		Class[] types = target.getParameterTypes();
		out.println("Method: " + target.getName() + " param.length = " + types.length);
		out.print("    { ");
		for (int i = 0; i < types.length; i++) {
			out.print(types[i].getSimpleName() + " ");
		}
		out.println("}");
	}

	private static class CompareStringNames implements Comparator<String> {

		@Override
		public int compare( String a, String b ) {
			if (a.length() < b.length()) {
				return -1;
			} else if (a.length() > b.length()) {
				return 1;
			} else {
				return a.compareTo(b);
			}
		}
	}

	public static String toString( Reader r ) {
		char[] buff = new char[1024];

		StringBuilder string = new StringBuilder();
		try {
			while (true) {
				int size = r.read(buff);
				if (size < 0)
					break;
				string.append(buff, 0, size);
			}
			return string.toString();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static int countNotZero( int[] a, int size ) {
		int ret = 0;
		for (int i = 0; i < size; i++) {
			if (a[i] != 0)
				ret++;
		}
		return ret;
	}

	public static double[] convertTo_F64( int[] a ) {
		double[] ret = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			ret[i] = a[i];
		}
		return ret;
	}

	public static float[] convertTo_F32( double[] a, float[] ret ) {
		if (ret == null)
			ret = new float[a.length];
		for (int i = 0; i < a.length; i++) {
			ret[i] = (float)a[i];
		}
		return ret;
	}

	public static int[] convertTo_I32( double[] a, int[] ret ) {
		if (ret == null)
			ret = new int[a.length];
		for (int i = 0; i < a.length; i++) {
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
	public static void boundRectangleInside( ImageBase b, ImageRectangle r ) {
		if (r.x0 < 0)
			r.x0 = 0;
		if (r.x1 > b.width)
			r.x1 = b.width;

		if (r.y0 < 0)
			r.y0 = 0;
		if (r.y1 > b.height)
			r.y1 = b.height;
	}

	public static boolean isInside( ImageBase b, ImageRectangle r ) {
		if (r.x0 < 0)
			return false;
		if (r.x1 > b.width)
			return false;

		if (r.y0 < 0)
			return false;
		return r.y1 <= b.height;
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
	public static boolean isInside( ImageBase b, int x, int y, int radius ) {
		if (x - radius < 0)
			return false;
		if (x + radius >= b.width)
			return false;

		if (y - radius < 0)
			return false;
		return y + radius < b.height;
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
	public static boolean isInside( ImageBase b, float x, float y, float radius ) {
		if (x - radius < 0)
			return false;
		if (x + radius > b.width - 1)
			return false;

		if (y - radius < 0)
			return false;
		return !(y + radius > b.height - 1);
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
	public static boolean isInside( ImageBase b, double x, double y, double radius ) {
		if (x - radius < 0)
			return false;
		if (x + radius > b.width - 1)
			return false;

		if (y - radius < 0)
			return false;
		return !(y + radius > b.height - 1);
	}

	public static boolean isInside( ImageBase b, int x, int y, int radiusWidth, int radiusHeight ) {
		if (x - radiusWidth < 0)
			return false;
		if (x + radiusWidth >= b.width)
			return false;

		if (y - radiusHeight < 0)
			return false;
		return y + radiusHeight < b.height;
	}

	public static boolean isInside( ImageBase b, int c_x, int c_y, int radius, double theta ) {
		int r = radius;
		float c = (float)Math.cos(theta);
		float s = (float)Math.sin(theta);

		// make sure the region is inside the image
		if (!checkInBounds(b, c_x, c_y, -r, -r, c, s))
			return false;
		else if (!checkInBounds(b, c_x, c_y, -r, r, c, s))
			return false;
		else if (!checkInBounds(b, c_x, c_y, r, r, c, s))
			return false;
		else return checkInBounds(b, c_x, c_y, r, -r, c, s);
	}

	private static boolean checkInBounds( ImageBase b, int c_x, int c_y, int dx, int dy, float c, float s ) {
		float x = c_x + c*dx - s*dy;
		float y = c_y + s*dx + c*dy;

		return b.isInBounds((int)x, (int)y);
	}

	public static boolean isInside( ImageBase b, float x, float y ) {
		return x >= 0 && x < b.width && y >= 0 && y < b.height;
	}

	public static boolean isInside( ImageBase b, double x, double y ) {
		return x >= 0 && x < b.width && y >= 0 && y < b.height;
	}

	public static boolean isInside( int width, int height, float x, float y ) {
		return x >= 0 && x < width && y >= 0 && y < height;
	}

	public static boolean isInside( int width, int height, double x, double y ) {
		return x >= 0 && x < width && y >= 0 && y < height;
	}

	/**
	 * Invokes wait until the elapsed time has passed.  In the thread is interrupted, the interrupt is ignored.
	 *
	 * @param milli Length of desired pause in milliseconds.
	 */
	@SuppressWarnings({"EmptyCatch"})
	public static void pause( long milli ) {
		final Thread t = Thread.currentThread();
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < milli) {
			synchronized (t) {
				try {
					long target = milli - (System.currentTimeMillis() - start);
					if (target > 0)
						t.wait(target);
				} catch (InterruptedException ignore) {
				}
			}
		}
	}

	public static void print( ImageGray a ) {

		if (a.getDataType().isInteger()) {
			print((GrayI)a);
		} else if (a instanceof GrayF32) {
			print((GrayF32)a);
		} else {
			print((GrayF64)a);
		}
	}

	public static void print( GrayF64 a ) {
		for (int y = 0; y < a.height; y++) {
			for (int x = 0; x < a.width; x++) {
				System.out.printf("%6.2f ", a.get(x, y));
			}
			System.out.println();
		}
		System.out.println();
	}

	public static void print( GrayF32 a ) {
		for (int y = 0; y < a.height; y++) {
			for (int x = 0; x < a.width; x++) {
				System.out.printf("%6.2f ", a.get(x, y));
			}
			System.out.println();
		}
		System.out.println();
	}

	public static void print( InterleavedF32 a ) {
		for (int y = 0; y < a.height; y++) {
			for (int x = 0; x < a.width; x++) {
				System.out.print("|");
				for (int band = 0; band < a.numBands; band++) {
					System.out.printf(" %6.2f", a.getBand(x, y, band));
				}
				System.out.print(" |");
			}
			System.out.println();
		}
		System.out.println();
	}

	public static void print( GrayI a ) {
		for (int y = 0; y < a.height; y++) {
			for (int x = 0; x < a.width; x++) {
				System.out.printf("%4d ", a.get(x, y));
			}
			System.out.println();
		}
		System.out.println();
	}

	public static int[] convertArray( double[] input, int[] output ) {
		if (output == null)
			output = new int[input.length];

		for (int i = 0; i < input.length; i++) {
			output[i] = (int)input[i];
		}

		return output;
	}

	public static long[] convertArray( double[] input, long[] output ) {
		if (output == null)
			output = new long[input.length];

		for (int i = 0; i < input.length; i++) {
			output[i] = (long)input[i];
		}

		return output;
	}

	public static float[] convertArray( double[] input, float[] output ) {
		if (output == null)
			output = new float[input.length];

		for (int i = 0; i < input.length; i++) {
			output[i] = (float)input[i];
		}

		return output;
	}

	public static double[] convertArray( float[] input, double[] output ) {
		if (output == null)
			output = new double[input.length];

		for (int i = 0; i < input.length; i++) {
			output[i] = input[i];
		}

		return output;
	}

	public static int[] convertArray( float[] input, int[] output ) {
		if (output == null)
			output = new int[input.length];

		for (int i = 0; i < input.length; i++) {
			output[i] = (int)input[i];
		}

		return output;
	}

	public static float[] convertArray( int[] input, float[] output ) {
		if (output == null)
			output = new float[input.length];

		for (int i = 0; i < input.length; i++) {
			output[i] = input[i];
		}

		return output;
	}

	public static void sleep( long milli ) {
		try {
			Thread.sleep(milli);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void checkTrue( boolean result ) {
		checkTrue(result, "Assert failed.");
	}

	public static void checkTrue( boolean result, String message ) {
		if (!result)
			throw new BoofCheckFailure(message);
	}

	public static void checkEq( int valA, int valB ) {
		checkEq(valA, valB, "not equals");
	}

	public static void checkEq( int valA, int valB, String message ) {
		if (valA != valB)
			throw new BoofCheckFailure(valA + " != " + valB + " " + message);
	}

	public static <T> void forIdx( List<T> list, BoofLambdas.ProcessIndex<T> func ) {
		for (int i = 0; i < list.size(); i++) {
			func.process(i, list.get(i));
		}
	}

	/**
	 * Extracts elements from a list and returns a list
	 */
	public static <In, Out> List<Out> collectList( List<In> list, BoofLambdas.Extract<In, Out> func ) {
		List<Out> out = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			In input = list.get(i);
			out.add(func.process(input));
		}
		return out;
	}

	/** Returns the last element in the list. Does not check if the list is empty */
	public static <T>T tail( List<T> list ) {
		return list.get(list.size()-1);
	}

	/** Returns and removes the last element in the list. Does not check if the list is empty */
	public static <T>T removeTail( List<T> list ) {
		return list.remove(list.size()-1);
	}

	/**
	 * Finds the row at the specified column with the maximum absolute value
	 *
	 * @param A (input) matrix
	 * @param column column to inspect
	 * @return The row index
	 */
	public static int columnMaxAbsRow( DMatrixRMaj A, int column ) {
		int selected = -1;
		double largestValue = -1;
		for (int row = 0; row < A.numRows; row++) {
			double v = Math.abs(A.unsafe_get(row, column));
			if (v > largestValue) {
				largestValue = v;
				selected = row;
			}
		}
		return selected;
	}

	public static <T> List<T> createListFilled( int total, BoofLambdas.Factory<T> factory ) {
		List<T> out = new ArrayList<>();
		for (int i = 0; i < total; i++) {
			out.add(factory.newInstance());
		}
		return out;
	}

	public static <V> V getOrThrow( Map map, Object key ) throws IOException {
		V value = (V)map.get(key);
		if (value == null)
			throw new IOException("Key not found in map. key=" + key);
		return value;
	}

	/**
	 * Checks to see if the list contains duplicate items
	 */
	public static <T> boolean containsDuplicates( List<T> list ) {
		Set<T> set = new HashSet<>();
		for (int i = 0; i < list.size(); i++) {
			T o = list.get(i);
			if (!set.add(o))
				return true;
		}
		return false;
	}

	public static boolean[] checkDeclare( @Nullable DogArray_B queue, int length, boolean zero ) {
		if (queue==null)
			queue = new DogArray_B(length);
		queue.resize(length);
		if (zero)
			queue.fill(false);
		return queue.data;
	}

	public static byte[] checkDeclare( @Nullable DogArray_I8 queue, int length, boolean zero ) {
		if (queue==null)
			queue = new DogArray_I8(length);
		queue.resize(length);
		if( zero )
			queue.fill((byte)0);
		return queue.data;
	}

	public static int[] checkDeclare( @Nullable DogArray_I32 queue, int length, boolean zero ) {
		if (queue==null)
			queue = new DogArray_I32(length);
		queue.resize(length);
		if( zero )
			queue.fill(0);
		return queue.data;
	}

	public static float[] checkDeclare( @Nullable DogArray_F32 queue, int length, boolean zero ) {
		if (queue==null)
			queue = new DogArray_F32(length);
		queue.resize(length);
		if( zero )
			queue.fill(0.0f);
		return queue.data;
	}

	public static double[] checkDeclare( @Nullable DogArray_F64 queue, int length, boolean zero ) {
		if (queue==null)
			queue = new DogArray_F64(length);
		queue.resize(length);
		if( zero )
			queue.fill(0.0);
		return queue.data;
	}

	public static<T> GrowArray<T> checkDeclare( @Nullable GrowArray<T> growable,
												ConcurrencyOps.NewInstance<T> factory ) {
		growable = growable == null ? new GrowArray<>(factory) : growable;
		growable.reset();
		return growable;
	}

}
