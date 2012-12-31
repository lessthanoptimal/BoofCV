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

package boofcv.struct.convolve;


/**
 * This is a kernel in a 2D convolution.  The convolution is performed by
 * convolving this kernel across a 2D array/image.  The kernel is square and has
 * the specified width.  To promote reuse of data structures the width of the kernel can be changed.
 * All elements in this kernel are floating point numbers.
 *
 * @author Peter Abeles
 */
public class Kernel2D_F64 extends Kernel2D {

	public double data[];

	/**
	 * Creates a new kernel whose initial values are specified by data and width.  The length
	 * of its internal data will be width*width.  Data must be at least as long as width*width.
	 *
	 * @param width The kernels width.  Must be odd.
	 * @param data  The value of the kernel. Not modified.  Reference is not saved.
	 */
	public Kernel2D_F64(int width, double ...data) {
		if (width % 2 == 0 && width <= 0)
			throw new IllegalArgumentException("invalid width");

		this.width = width;

		this.data = new double[width * width];
		System.arraycopy(data, 0, this.data, 0, this.data.length);
	}

	/**
	 * Create a kernel whose elements are all equal to zero.
	 *
	 * @param width How wide the kernel is.  Must be odd.
	 */
	public Kernel2D_F64(int width) {
		if (width % 2 == 0 && width <= 0)
			throw new IllegalArgumentException("invalid width");

		data = new double[width * width];
		this.width = width;
	}

	protected Kernel2D_F64() {
	}

	/**
	 * Creates a kernel whose elements are the specified data array and has
	 * the specified width.
	 *
	 * @param data  The array who will be the kernel's data.  Reference is saved.
	 * @param width The kernel's width.
	 * @return A new kernel.
	 */
	public static Kernel2D_F64 wrap(double data[], int width) {
		if (width % 2 == 0 && width <= 0 && width * width > data.length)
			throw new IllegalArgumentException("invalid width");

		Kernel2D_F64 ret = new Kernel2D_F64();
		ret.data = data;
		ret.width = width;

		return ret;
	}

	public double get(int x, int y) {
		return data[y * width + x];
	}

	public void set( int x , int y , double value ) {
		data[y * width + x] = value;
	}

	@Override
	public boolean isInteger() {
		return false;
	}

	public double[] getData() {
		return data;
	}

	public void print() {
		for( int i = 0; i < width; i++ ) {
			for( int j = 0; j < width; j++ ) {
				System.out.printf("%5.2f ",data[i*width+j]);
			}
			System.out.println();
		}
		System.out.println();
	}
}