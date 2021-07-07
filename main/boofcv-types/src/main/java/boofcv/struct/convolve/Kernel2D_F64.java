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

package boofcv.struct.convolve;

/**
 * This is a kernel in a 2D convolution. The convolution is performed by
 * convolving this kernel across a 2D array/image. The kernel is square and has
 * the specified width. To promote reuse of data structures the width of the kernel can be changed.
 * All elements in this kernel are floating point numbers.
 *
 * <p>
 * WARNING: Do not modify. Automatically generated by {@link boofcv.struct.convolve.GenerateKernel2D}.
 * </p>
 *
 * @author Peter Abeles
 */
public class Kernel2D_F64 extends Kernel2D {

	public double[] data;

	/**
	 * Creates a new kernel whose initial values are specified by 'data' and 'width'. The length
	 * of its internal data will be width*width. Data must be at least as long as width*width.
	 * The offset is automatically set to width/2
	 *
	 * @param width The kernels width. Must be odd.
	 * @param data The value of the kernel. Not modified. Reference is not saved.
	 */
	public Kernel2D_F64( int width, double[] data ) {
		super(width);

		this.data = new double[width*width];
		System.arraycopy(data, 0, this.data, 0, this.data.length);
	}

	/**
	 * Create a kernel with elements initialized to zero with the specified 'width' and offset equal to
	 * width/2
	 *
	 * @param width How wide the kernel is.
	 */
	public Kernel2D_F64( int width ) {
		super(width);

		data = new double[width*width];
	}

	/**
	 * Create a kernel with elements initialized to zero with the specified 'width' and 'offset'.
	 *
	 * @param width How wide the kernel is.
	 */
	public Kernel2D_F64( int width, int offset ) {
		super(width, offset);

		data = new double[width*width];
	}

	protected Kernel2D_F64() {
	}

	/**
	 * Creates a kernel whose elements are the specified data array and has
	 * the specified width.
	 *
	 * @param data The array who will be the kernel's data. Reference is saved.
	 * @param width The kernel's width.
	 * @param offset Kernel origin's offset from element 0.
	 * @return A new kernel.
	 */
	public static Kernel2D_F64 wrap( double[] data, int width, int offset ) {
		if (width%2 == 0 && width <= 0 && width*width > data.length)
			throw new IllegalArgumentException("invalid width");

		Kernel2D_F64 ret = new Kernel2D_F64();
		ret.data = data;
		ret.width = width;
		ret.offset = offset;

		return ret;
	}

	public double get( int x, int y ) {
		return data[y*width + x];
	}

	public void set( int x, int y, double value ) {
		data[y*width + x] = value;
	}

	@Override
	public boolean isInteger() {
		return false;
	}

	public double[] getData() {
		return data;
	}

	public double computeSum() {
		int N = width*width;
		double total = 0;
		for (int i = 0; i < N; i++) {
			total += data[i];
		}
		return total;
	}

	public void print() {
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < width; j++) {
				System.out.printf("%15f ", data[i*width + j]);
			}
			System.out.println();
		}
		System.out.println();
	}

	@Override
	public Kernel2D_F64 copy() {
		Kernel2D_F64 ret = new Kernel2D_F64(width);
		ret.offset = this.offset;
		System.arraycopy(data, 0, ret.data, 0, data.length);
		return ret;
	}

	@Override
	public /**/double /**/getDouble( int x, int y ) {
		return get(x, y);
	}
}
