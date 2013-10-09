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

package boofcv.alg.transform.fft;

/**
 * Computes 2D Discrete Fourier Transform (DFT) of complex and real, double
 * precision data. The size of the data can be an arbitrary number. The code originally comes from
 * General Purpose FFT Package written by Takuya Ooura
 * (http://www.kurims.kyoto-u.ac.jp/~ooura/fft.html).  See below for the full history.
 * <p></p>
 * This code has a bit of a history.  Originally from General Purpose FFT.  Which was then ported into
 * JFFTPack written by Baoshe Zhang (http://jfftpack.sourceforge.net/), and then into JTransforms by Piotr Wendykier.
 * The major modification from JTransforms is that the SMP code has been stripped out.  It might be added back in
 * once an SMP strategy has been finalized in BoofCV.
 * <p></p>
 * Code License:  The original license of General Purpose FFT Package is shown below.  This file will fall
 * under the same license:
 * <pre>
 * Copyright Takuya OOURA, 1996-2001
 *
 * You may use, copy, modify and distribute this code for any purpose (include commercial use) and without fee.
 * Please refer to this package when you modify this code.
 * </pre>
 *
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * @author Peter Abeles
 *
 */
public class GeneralPurposeFFT_F64_2D {

	private int rows;

	private int columns;

	private double[] t;

	private GeneralPurposeFFT_F64_1D fftColumns, fftRows;

	private boolean isPowerOfTwo = false;

	// local storage pre-declared
	private double[] temp;
	private double[][] temp2;

	/**
	 * Creates new instance of DoubleFFT_2D.
	 *
	 * @param rows
	 *            number of rows
	 * @param columns
	 *            number of columns
	 */
	public GeneralPurposeFFT_F64_2D(int rows, int columns) {
		if (rows < 1 || columns < 1 ) {
			throw new IllegalArgumentException("rows and columns must be greater than 0");
		}
		this.rows = rows;
		this.columns = columns;

		if (DiscreteFourierTransformOps.isPowerOf2(rows) && DiscreteFourierTransformOps.isPowerOf2(columns)) {
			isPowerOfTwo = true;

			int oldNthreads = 1;
			int nt = 8 * oldNthreads * rows;
			if (2 * columns == 4 * oldNthreads) {
				nt >>= 1;
			} else if (2 * columns < 4 * oldNthreads) {
				nt >>= 2;
			}
			t = new double[nt];
		}

		fftRows = new GeneralPurposeFFT_F64_1D(rows);
		if (rows == columns) {
			fftColumns = fftRows;
		} else {
			fftColumns = new GeneralPurposeFFT_F64_1D(columns);
		}

		temp = new double[2 * rows];
	}

	/**
	 * Computes 2D forward DFT of complex data leaving the result in
	 * <code>a</code>. The data is stored in 1D array in row-major order.
	 * Complex number is stored as two double values in sequence: the real and
	 * imaginary part, i.e. the input array must be of size rows*2*columns. The
	 * physical layout of the input data has to be as follows:<br>
	 *
	 * <pre>
	 * a[k1*2*columns+2*k2] = Re[k1][k2],
	 * a[k1*2*columns+2*k2+1] = Im[k1][k2], 0&lt;=k1&lt;rows, 0&lt;=k2&lt;columns,
	 * </pre>
	 *
	 * @param a
	 *            data to transform
	 */
	public void complexForward(final double[] a) {
		// handle special case
		if( rows == 1 || columns == 1 ) {
			if( rows > 1 )
				fftRows.complexForward(a);
			else
				fftColumns.complexForward(a);
			return;
		}

		if (isPowerOfTwo) {
			int oldn2 = columns;
			columns = 2 * columns;
			for (int r = 0; r < rows; r++) {
				fftColumns.complexForward(a, r * columns);
			}
			cdft2d_sub(-1, a, true);
			columns = oldn2;
		} else {
			final int rowStride = 2 * columns;

			for (int r = 0; r < rows; r++) {
				fftColumns.complexForward(a, r * rowStride);
			}

			for (int c = 0; c < columns; c++) {
				int idx0 = 2 * c;
				for (int r = 0; r < rows; r++) {
					int idx1 = 2 * r;
					int idx2 = r * rowStride + idx0;
					temp[idx1] = a[idx2];
					temp[idx1 + 1] = a[idx2 + 1];
				}
				fftRows.complexForward(temp);
				for (int r = 0; r < rows; r++) {
					int idx1 = 2 * r;
					int idx2 = r * rowStride + idx0;
					a[idx2] = temp[idx1];
					a[idx2 + 1] = temp[idx1 + 1];
				}
			}
		}
	}


	/**
	 * Computes 2D inverse DFT of complex data leaving the result in
	 * <code>a</code>. The data is stored in 1D array in row-major order.
	 * Complex number is stored as two double values in sequence: the real and
	 * imaginary part, i.e. the input array must be of size rows*2*columns. The
	 * physical layout of the input data has to be as follows:<br>
	 *
	 * <pre>
	 * a[k1*2*columns+2*k2] = Re[k1][k2],
	 * a[k1*2*columns+2*k2+1] = Im[k1][k2], 0&lt;=k1&lt;rows, 0&lt;=k2&lt;columns,
	 * </pre>
	 *
	 * @param a
	 *            data to transform
	 * @param scale
	 *            if true then scaling is performed
	 *
	 */
	public void complexInverse(final double[] a, final boolean scale) {
		// handle special case
		if( rows == 1 || columns == 1 ) {
			if( rows > 1 )
				fftRows.complexInverse(a, scale);
			else
				fftColumns.complexInverse(a, scale);
			return;
		}

		if (isPowerOfTwo) {
			int oldn2 = columns;
			columns = 2 * columns;
			for (int r = 0; r < rows; r++) {
				fftColumns.complexInverse(a, r * columns, scale);
			}
			cdft2d_sub(1, a, scale);
			columns = oldn2;
		} else {
			final int rowspan = 2 * columns;
			for (int r = 0; r < rows; r++) {
				fftColumns.complexInverse(a, r * rowspan, scale);
			}

			for (int c = 0; c < columns; c++) {
				int idx1 = 2 * c;
				for (int r = 0; r < rows; r++) {
					int idx2 = 2 * r;
					int idx3 = r * rowspan + idx1;
					temp[idx2] = a[idx3];
					temp[idx2 + 1] = a[idx3 + 1];
				}
				fftRows.complexInverse(temp, scale);
				for (int r = 0; r < rows; r++) {
					int idx2 = 2 * r;
					int idx3 = r * rowspan + idx1;
					a[idx3] = temp[idx2];
					a[idx3 + 1] = temp[idx2 + 1];
				}
			}
		}
	}

	/**
	 * Computes 2D forward DFT of real data leaving the result in <code>a</code>
	 * . This method only works when the sizes of both dimensions are
	 * power-of-two numbers. The physical layout of the output data is as
	 * follows:
	 *
	 * <pre>
	 * a[k1*columns+2*k2] = Re[k1][k2] = Re[rows-k1][columns-k2],
	 * a[k1*columns+2*k2+1] = Im[k1][k2] = -Im[rows-k1][columns-k2],
	 *       0&lt;k1&lt;rows, 0&lt;k2&lt;columns/2,
	 * a[2*k2] = Re[0][k2] = Re[0][columns-k2],
	 * a[2*k2+1] = Im[0][k2] = -Im[0][columns-k2],
	 *       0&lt;k2&lt;columns/2,
	 * a[k1*columns] = Re[k1][0] = Re[rows-k1][0],
	 * a[k1*columns+1] = Im[k1][0] = -Im[rows-k1][0],
	 * a[(rows-k1)*columns+1] = Re[k1][columns/2] = Re[rows-k1][columns/2],
	 * a[(rows-k1)*columns] = -Im[k1][columns/2] = Im[rows-k1][columns/2],
	 *       0&lt;k1&lt;rows/2,
	 * a[0] = Re[0][0],
	 * a[1] = Re[0][columns/2],
	 * a[(rows/2)*columns] = Re[rows/2][0],
	 * a[(rows/2)*columns+1] = Re[rows/2][columns/2]
	 * </pre>
	 *
	 * This method computes only half of the elements of the real transform. The
	 * other half satisfies the symmetry condition. If you want the full real
	 * forward transform, use <code>realForwardFull</code>. To get back the
	 * original data, use <code>realInverse</code> on the output of this method.
	 *
	 * @param a
	 *            data to transform
	 */
	public void realForward(double[] a) {
		// handle special case
		if( rows == 1 || columns == 1 ) {
			if( rows > 1 )
				fftRows.realForward(a);
			else
				fftColumns.realForward(a);
			return;
		}

		if (isPowerOfTwo == false) {
			throw new IllegalArgumentException("rows and columns must be power of two numbers");
		} else {
			for (int r = 0; r < rows; r++) {
				fftColumns.realForward(a, r * columns);
			}
			cdft2d_sub(-1, a, true);
			rdft2d_sub(1, a);
		}
	}

	/**
	 * Computes 2D forward DFT of real data leaving the result in <code>a</code>
	 * . This method computes full real forward transform, i.e. you will get the
	 * same result as from <code>complexForward</code> called with all imaginary
	 * part equal 0. Because the result is stored in <code>a</code>, the input
	 * array must be of size rows*2*columns, with only the first rows*columns
	 * elements filled with real data. To get back the original data, use
	 * <code>complexInverse</code> on the output of this method.
	 *
	 * @param a
	 *            data to transform
	 */
	public void realForwardFull(double[] a) {
		// handle special case
		if( rows == 1 || columns == 1 ) {
			if( rows > 1 )
				fftRows.realForwardFull(a);
			else
				fftColumns.realForwardFull(a);
			return;
		}

		if (isPowerOfTwo) {
			for (int r = 0; r < rows; r++) {
				fftColumns.realForward(a, r * columns);
			}
			cdft2d_sub(-1, a, true);
			rdft2d_sub(1, a);
			fillSymmetric(a);
		} else {
			declareRadixRealData();
			mixedRadixRealForwardFull(a);
		}
	}

	/**
	 * Computes 2D inverse DFT of real data leaving the result in <code>a</code>
	 * . This method only works when the sizes of both dimensions are
	 * power-of-two numbers. The physical layout of the input data has to be as
	 * follows:
	 *
	 * <pre>
	 * a[k1*columns+2*k2] = Re[k1][k2] = Re[rows-k1][columns-k2],
	 * a[k1*columns+2*k2+1] = Im[k1][k2] = -Im[rows-k1][columns-k2],
	 *       0&lt;k1&lt;rows, 0&lt;k2&lt;columns/2,
	 * a[2*k2] = Re[0][k2] = Re[0][columns-k2],
	 * a[2*k2+1] = Im[0][k2] = -Im[0][columns-k2],
	 *       0&lt;k2&lt;columns/2,
	 * a[k1*columns] = Re[k1][0] = Re[rows-k1][0],
	 * a[k1*columns+1] = Im[k1][0] = -Im[rows-k1][0],
	 * a[(rows-k1)*columns+1] = Re[k1][columns/2] = Re[rows-k1][columns/2],
	 * a[(rows-k1)*columns] = -Im[k1][columns/2] = Im[rows-k1][columns/2],
	 *       0&lt;k1&lt;rows/2,
	 * a[0] = Re[0][0],
	 * a[1] = Re[0][columns/2],
	 * a[(rows/2)*columns] = Re[rows/2][0],
	 * a[(rows/2)*columns+1] = Re[rows/2][columns/2]
	 * </pre>
	 *
	 * This method computes only half of the elements of the real transform. The
	 * other half satisfies the symmetry condition. If you want the full real
	 * inverse transform, use <code>realInverseFull</code>.
	 *
	 * @param a
	 *            data to transform
	 *
	 * @param scale
	 *            if true then scaling is performed
	 */
	public void realInverse(double[] a, boolean scale) {
		// handle special case
		if( rows == 1 || columns == 1 ) {
			if( rows > 1 )
				fftRows.realInverse(a, scale);
			else
				fftColumns.realInverse(a, scale);
			return;
		}

		if (isPowerOfTwo == false) {
			throw new IllegalArgumentException("rows and columns must be power of two numbers");
		} else {
			rdft2d_sub(-1, a);
			cdft2d_sub(1, a, scale);
			for (int r = 0; r < rows; r++) {
				fftColumns.realInverse(a, r * columns, scale);
			}
		}
	}

	/**
	 * Computes 2D inverse DFT of real data leaving the result in <code>a</code>
	 * . This method computes full real inverse transform, i.e. you will get the
	 * same result as from <code>complexInverse</code> called with all imaginary
	 * part equal 0. Because the result is stored in <code>a</code>, the input
	 * array must be of size rows*2*columns, with only the first rows*columns
	 * elements filled with real data.
	 *
	 * @param a
	 *            data to transform
	 *
	 * @param scale
	 *            if true then scaling is performed
	 */
	public void realInverseFull(double[] a, boolean scale) {
		// handle special case
		if( rows == 1 || columns == 1 ) {
			if( rows > 1 )
				fftRows.realInverseFull(a, scale);
			else
				fftColumns.realInverseFull(a, scale);
			return;
		}

		if (isPowerOfTwo) {
			for (int r = 0; r < rows; r++) {
				fftColumns.realInverse2(a, r * columns, scale);
			}
			cdft2d_sub(1, a, scale);
			rdft2d_sub(1, a);
			fillSymmetric(a);
		} else {
			declareRadixRealData();
			mixedRadixRealInverseFull(a, scale);
		}
	}

	private void declareRadixRealData() {
		if( temp2 == null ) {
			final int n2d2 = columns / 2 + 1;
			temp2 = new double[n2d2][2 * rows];
		}
	}

	private void mixedRadixRealForwardFull(final double[] a) {
		final int rowStride = 2 * columns;
		final int n2d2 = columns / 2 + 1;
		final double[][] temp = temp2;

		for (int r = 0; r < rows; r++) {
			fftColumns.realForward(a, r * columns);
		}
		for (int r = 0; r < rows; r++) {
			temp[0][r] = a[r * columns]; //first column is always real
		}
		fftRows.realForwardFull(temp[0]);

		for (int c = 1; c < n2d2 - 1; c++) {
			int idx0 = 2 * c;
			for (int r = 0; r < rows; r++) {
				int idx1 = 2 * r;
				int idx2 = r * columns + idx0;
				temp[c][idx1] = a[idx2];
				temp[c][idx1 + 1] = a[idx2 + 1];
			}
			fftRows.complexForward(temp[c]);
		}

		if ((columns % 2) == 0) {
			for (int r = 0; r < rows; r++) {
				temp[n2d2 - 1][r] = a[r * columns + 1];
				//imaginary part = 0;
			}
			fftRows.realForwardFull(temp[n2d2 - 1]);

		} else {
			for (int r = 0; r < rows; r++) {
				int idx1 = 2 * r;
				int idx2 = r * columns;
				int idx3 = n2d2 - 1;
				temp[idx3][idx1] = a[idx2 + 2 * idx3];
				temp[idx3][idx1 + 1] = a[idx2 + 1];
			}
			fftRows.complexForward(temp[n2d2 - 1]);
		}

		for (int r = 0; r < rows; r++) {
			int idx1 = 2 * r;
			for (int c = 0; c < n2d2; c++) {
				int idx0 = 2 * c;
				int idx2 = r * rowStride + idx0;
				a[idx2] = temp[c][idx1];
				a[idx2 + 1] = temp[c][idx1 + 1];
			}
		}

		//fill symmetric
		for (int r = 1; r < rows; r++) {
			int idx5 = r * rowStride;
			int idx6 = (rows - r + 1) * rowStride;
			for (int c = n2d2; c < columns; c++) {
				int idx1 = 2 * c;
				int idx2 = 2 * (columns - c);
				a[idx1] = a[idx2];
				a[idx1 + 1] = -a[idx2 + 1];
				int idx3 = idx5 + idx1;
				int idx4 = idx6 - idx1;
				a[idx3] = a[idx4];
				a[idx3 + 1] = -a[idx4 + 1];
			}
		}
	}

	private void mixedRadixRealInverseFull(final double[] a, final boolean scale) {
		final int rowStride = 2 * columns;
		final int n2d2 = columns / 2 + 1;
		final double[][] temp = temp2;

		for (int r = 0; r < rows; r++) {
			fftColumns.realInverse2(a, r * columns, scale);
		}
		for (int r = 0; r < rows; r++) {
			temp[0][r] = a[r * columns]; //first column is always real
		}
		fftRows.realInverseFull(temp[0], scale);

		for (int c = 1; c < n2d2 - 1; c++) {
			int idx0 = 2 * c;
			for (int r = 0; r < rows; r++) {
				int idx1 = 2 * r;
				int idx2 = r * columns + idx0;
				temp[c][idx1] = a[idx2];
				temp[c][idx1 + 1] = a[idx2 + 1];
			}
			fftRows.complexInverse(temp[c], scale);
		}

		if ((columns % 2) == 0) {
			for (int r = 0; r < rows; r++) {
				temp[n2d2 - 1][r] = a[r * columns + 1];
				//imaginary part = 0;
			}
			fftRows.realInverseFull(temp[n2d2 - 1], scale);

		} else {
			for (int r = 0; r < rows; r++) {
				int idx1 = 2 * r;
				int idx2 = r * columns;
				int idx3 = n2d2 - 1;
				temp[idx3][idx1] = a[idx2 + 2 * idx3];
				temp[idx3][idx1 + 1] = a[idx2 + 1];
			}
			fftRows.complexInverse(temp[n2d2 - 1], scale);
		}

		for (int r = 0; r < rows; r++) {
			int idx1 = 2 * r;
			for (int c = 0; c < n2d2; c++) {
				int idx0 = 2 * c;
				int idx2 = r * rowStride + idx0;
				a[idx2] = temp[c][idx1];
				a[idx2 + 1] = temp[c][idx1 + 1];
			}
		}

		//fill symmetric
		for (int r = 1; r < rows; r++) {
			int idx5 = r * rowStride;
			int idx6 = (rows - r + 1) * rowStride;
			for (int c = n2d2; c < columns; c++) {
				int idx1 = 2 * c;
				int idx2 = 2 * (columns - c);
				a[idx1] = a[idx2];
				a[idx1 + 1] = -a[idx2 + 1];
				int idx3 = idx5 + idx1;
				int idx4 = idx6 - idx1;
				a[idx3] = a[idx4];
				a[idx3 + 1] = -a[idx4 + 1];
			}
		}
	}

	private void rdft2d_sub(int isgn, double[] a) {
		int n1h, j;
		double xi;
		int idx1, idx2;

		n1h = rows >> 1;
		if (isgn < 0) {
			for (int i = 1; i < n1h; i++) {
				j = rows - i;
				idx1 = i * columns;
				idx2 = j * columns;
				xi = a[idx1] - a[idx2];
				a[idx1] += a[idx2];
				a[idx2] = xi;
				xi = a[idx2 + 1] - a[idx1 + 1];
				a[idx1 + 1] += a[idx2 + 1];
				a[idx2 + 1] = xi;
			}
		} else {
			for (int i = 1; i < n1h; i++) {
				j = rows - i;
				idx1 = i * columns;
				idx2 = j * columns;
				a[idx2] = 0.5f * (a[idx1] - a[idx2]);
				a[idx1] -= a[idx2];
				a[idx2 + 1] = 0.5f * (a[idx1 + 1] + a[idx2 + 1]);
				a[idx1 + 1] -= a[idx2 + 1];
			}
		}
	}

	private void cdft2d_sub(int isgn, double[] a, boolean scale) {
		int idx1, idx2, idx3, idx4, idx5;
		if (isgn == -1) {
			if (columns > 4) {
				for (int c = 0; c < columns; c += 8) {
					for (int r = 0; r < rows; r++) {
						idx1 = r * columns + c;
						idx2 = 2 * r;
						idx3 = 2 * rows + 2 * r;
						idx4 = idx3 + 2 * rows;
						idx5 = idx4 + 2 * rows;
						t[idx2] = a[idx1];
						t[idx2 + 1] = a[idx1 + 1];
						t[idx3] = a[idx1 + 2];
						t[idx3 + 1] = a[idx1 + 3];
						t[idx4] = a[idx1 + 4];
						t[idx4 + 1] = a[idx1 + 5];
						t[idx5] = a[idx1 + 6];
						t[idx5 + 1] = a[idx1 + 7];
					}
					fftRows.complexForward(t, 0);
					fftRows.complexForward(t, 2 * rows);
					fftRows.complexForward(t, 4 * rows);
					fftRows.complexForward(t, 6 * rows);
					for (int r = 0; r < rows; r++) {
						idx1 = r * columns + c;
						idx2 = 2 * r;
						idx3 = 2 * rows + 2 * r;
						idx4 = idx3 + 2 * rows;
						idx5 = idx4 + 2 * rows;
						a[idx1] = t[idx2];
						a[idx1 + 1] = t[idx2 + 1];
						a[idx1 + 2] = t[idx3];
						a[idx1 + 3] = t[idx3 + 1];
						a[idx1 + 4] = t[idx4];
						a[idx1 + 5] = t[idx4 + 1];
						a[idx1 + 6] = t[idx5];
						a[idx1 + 7] = t[idx5 + 1];
					}
				}
			} else if (columns == 4) {
				for (int r = 0; r < rows; r++) {
					idx1 = r * columns;
					idx2 = 2 * r;
					idx3 = 2 * rows + 2 * r;
					t[idx2] = a[idx1];
					t[idx2 + 1] = a[idx1 + 1];
					t[idx3] = a[idx1 + 2];
					t[idx3 + 1] = a[idx1 + 3];
				}
				fftRows.complexForward(t, 0);
				fftRows.complexForward(t, 2 * rows);
				for (int r = 0; r < rows; r++) {
					idx1 = r * columns;
					idx2 = 2 * r;
					idx3 = 2 * rows + 2 * r;
					a[idx1] = t[idx2];
					a[idx1 + 1] = t[idx2 + 1];
					a[idx1 + 2] = t[idx3];
					a[idx1 + 3] = t[idx3 + 1];
				}
			} else if (columns == 2) {
				for (int r = 0; r < rows; r++) {
					idx1 = r * columns;
					idx2 = 2 * r;
					t[idx2] = a[idx1];
					t[idx2 + 1] = a[idx1 + 1];
				}
				fftRows.complexForward(t, 0);
				for (int r = 0; r < rows; r++) {
					idx1 = r * columns;
					idx2 = 2 * r;
					a[idx1] = t[idx2];
					a[idx1 + 1] = t[idx2 + 1];
				}
			}
		} else {
			if (columns > 4) {
				for (int c = 0; c < columns; c += 8) {
					for (int r = 0; r < rows; r++) {
						idx1 = r * columns + c;
						idx2 = 2 * r;
						idx3 = 2 * rows + 2 * r;
						idx4 = idx3 + 2 * rows;
						idx5 = idx4 + 2 * rows;
						t[idx2] = a[idx1];
						t[idx2 + 1] = a[idx1 + 1];
						t[idx3] = a[idx1 + 2];
						t[idx3 + 1] = a[idx1 + 3];
						t[idx4] = a[idx1 + 4];
						t[idx4 + 1] = a[idx1 + 5];
						t[idx5] = a[idx1 + 6];
						t[idx5 + 1] = a[idx1 + 7];
					}
					fftRows.complexInverse(t, 0, scale);
					fftRows.complexInverse(t, 2 * rows, scale);
					fftRows.complexInverse(t, 4 * rows, scale);
					fftRows.complexInverse(t, 6 * rows, scale);
					for (int r = 0; r < rows; r++) {
						idx1 = r * columns + c;
						idx2 = 2 * r;
						idx3 = 2 * rows + 2 * r;
						idx4 = idx3 + 2 * rows;
						idx5 = idx4 + 2 * rows;
						a[idx1] = t[idx2];
						a[idx1 + 1] = t[idx2 + 1];
						a[idx1 + 2] = t[idx3];
						a[idx1 + 3] = t[idx3 + 1];
						a[idx1 + 4] = t[idx4];
						a[idx1 + 5] = t[idx4 + 1];
						a[idx1 + 6] = t[idx5];
						a[idx1 + 7] = t[idx5 + 1];
					}
				}
			} else if (columns == 4) {
				for (int r = 0; r < rows; r++) {
					idx1 = r * columns;
					idx2 = 2 * r;
					idx3 = 2 * rows + 2 * r;
					t[idx2] = a[idx1];
					t[idx2 + 1] = a[idx1 + 1];
					t[idx3] = a[idx1 + 2];
					t[idx3 + 1] = a[idx1 + 3];
				}
				fftRows.complexInverse(t, 0, scale);
				fftRows.complexInverse(t, 2 * rows, scale);
				for (int r = 0; r < rows; r++) {
					idx1 = r * columns;
					idx2 = 2 * r;
					idx3 = 2 * rows + 2 * r;
					a[idx1] = t[idx2];
					a[idx1 + 1] = t[idx2 + 1];
					a[idx1 + 2] = t[idx3];
					a[idx1 + 3] = t[idx3 + 1];
				}
			} else if (columns == 2) {
				for (int r = 0; r < rows; r++) {
					idx1 = r * columns;
					idx2 = 2 * r;
					t[idx2] = a[idx1];
					t[idx2 + 1] = a[idx1 + 1];
				}
				fftRows.complexInverse(t, 0, scale);
				for (int r = 0; r < rows; r++) {
					idx1 = r * columns;
					idx2 = 2 * r;
					a[idx1] = t[idx2];
					a[idx1 + 1] = t[idx2 + 1];
				}
			}
		}
	}

	private void cdft2d_sub(int isgn, double[][] a, boolean scale) {
		int idx2, idx3, idx4, idx5;
		if (isgn == -1) {
			if (columns > 4) {
				for (int c = 0; c < columns; c += 8) {
					for (int r = 0; r < rows; r++) {
						idx2 = 2 * r;
						idx3 = 2 * rows + 2 * r;
						idx4 = idx3 + 2 * rows;
						idx5 = idx4 + 2 * rows;
						t[idx2] = a[r][c];
						t[idx2 + 1] = a[r][c + 1];
						t[idx3] = a[r][c + 2];
						t[idx3 + 1] = a[r][c + 3];
						t[idx4] = a[r][c + 4];
						t[idx4 + 1] = a[r][c + 5];
						t[idx5] = a[r][c + 6];
						t[idx5 + 1] = a[r][c + 7];
					}
					fftRows.complexForward(t, 0);
					fftRows.complexForward(t, 2 * rows);
					fftRows.complexForward(t, 4 * rows);
					fftRows.complexForward(t, 6 * rows);
					for (int r = 0; r < rows; r++) {
						idx2 = 2 * r;
						idx3 = 2 * rows + 2 * r;
						idx4 = idx3 + 2 * rows;
						idx5 = idx4 + 2 * rows;
						a[r][c] = t[idx2];
						a[r][c + 1] = t[idx2 + 1];
						a[r][c + 2] = t[idx3];
						a[r][c + 3] = t[idx3 + 1];
						a[r][c + 4] = t[idx4];
						a[r][c + 5] = t[idx4 + 1];
						a[r][c + 6] = t[idx5];
						a[r][c + 7] = t[idx5 + 1];
					}
				}
			} else if (columns == 4) {
				for (int r = 0; r < rows; r++) {
					idx2 = 2 * r;
					idx3 = 2 * rows + 2 * r;
					t[idx2] = a[r][0];
					t[idx2 + 1] = a[r][1];
					t[idx3] = a[r][2];
					t[idx3 + 1] = a[r][3];
				}
				fftRows.complexForward(t, 0);
				fftRows.complexForward(t, 2 * rows);
				for (int r = 0; r < rows; r++) {
					idx2 = 2 * r;
					idx3 = 2 * rows + 2 * r;
					a[r][0] = t[idx2];
					a[r][1] = t[idx2 + 1];
					a[r][2] = t[idx3];
					a[r][3] = t[idx3 + 1];
				}
			} else if (columns == 2) {
				for (int r = 0; r < rows; r++) {
					idx2 = 2 * r;
					t[idx2] = a[r][0];
					t[idx2 + 1] = a[r][1];
				}
				fftRows.complexForward(t, 0);
				for (int r = 0; r < rows; r++) {
					idx2 = 2 * r;
					a[r][0] = t[idx2];
					a[r][1] = t[idx2 + 1];
				}
			}
		} else {
			if (columns > 4) {
				for (int c = 0; c < columns; c += 8) {
					for (int r = 0; r < rows; r++) {
						idx2 = 2 * r;
						idx3 = 2 * rows + 2 * r;
						idx4 = idx3 + 2 * rows;
						idx5 = idx4 + 2 * rows;
						t[idx2] = a[r][c];
						t[idx2 + 1] = a[r][c + 1];
						t[idx3] = a[r][c + 2];
						t[idx3 + 1] = a[r][c + 3];
						t[idx4] = a[r][c + 4];
						t[idx4 + 1] = a[r][c + 5];
						t[idx5] = a[r][c + 6];
						t[idx5 + 1] = a[r][c + 7];
					}
					fftRows.complexInverse(t, 0, scale);
					fftRows.complexInverse(t, 2 * rows, scale);
					fftRows.complexInverse(t, 4 * rows, scale);
					fftRows.complexInverse(t, 6 * rows, scale);
					for (int r = 0; r < rows; r++) {
						idx2 = 2 * r;
						idx3 = 2 * rows + 2 * r;
						idx4 = idx3 + 2 * rows;
						idx5 = idx4 + 2 * rows;
						a[r][c] = t[idx2];
						a[r][c + 1] = t[idx2 + 1];
						a[r][c + 2] = t[idx3];
						a[r][c + 3] = t[idx3 + 1];
						a[r][c + 4] = t[idx4];
						a[r][c + 5] = t[idx4 + 1];
						a[r][c + 6] = t[idx5];
						a[r][c + 7] = t[idx5 + 1];
					}
				}
			} else if (columns == 4) {
				for (int r = 0; r < rows; r++) {
					idx2 = 2 * r;
					idx3 = 2 * rows + 2 * r;
					t[idx2] = a[r][0];
					t[idx2 + 1] = a[r][1];
					t[idx3] = a[r][2];
					t[idx3 + 1] = a[r][3];
				}
				fftRows.complexInverse(t, 0, scale);
				fftRows.complexInverse(t, 2 * rows, scale);
				for (int r = 0; r < rows; r++) {
					idx2 = 2 * r;
					idx3 = 2 * rows + 2 * r;
					a[r][0] = t[idx2];
					a[r][1] = t[idx2 + 1];
					a[r][2] = t[idx3];
					a[r][3] = t[idx3 + 1];
				}
			} else if (columns == 2) {
				for (int r = 0; r < rows; r++) {
					idx2 = 2 * r;
					t[idx2] = a[r][0];
					t[idx2 + 1] = a[r][1];
				}
				fftRows.complexInverse(t, 0, scale);
				for (int r = 0; r < rows; r++) {
					idx2 = 2 * r;
					a[r][0] = t[idx2];
					a[r][1] = t[idx2 + 1];
				}
			}
		}
	}

	private void fillSymmetric(final double[] a) {
		final int twon2 = 2 * columns;
		int idx1, idx2, idx3, idx4;
		int n1d2 = rows / 2;

		for (int r = (rows - 1); r >= 1; r--) {
			idx1 = r * columns;
			idx2 = 2 * idx1;
			for (int c = 0; c < columns; c += 2) {
				a[idx2 + c] = a[idx1 + c];
				a[idx1 + c] = 0;
				a[idx2 + c + 1] = a[idx1 + c + 1];
				a[idx1 + c + 1] = 0;
			}
		}
		for (int r = 1; r < n1d2; r++) {
			idx2 = r * twon2;
			idx3 = (rows - r) * twon2;
			a[idx2 + columns] = a[idx3 + 1];
			a[idx2 + columns + 1] = -a[idx3];
		}

		for (int r = 1; r < n1d2; r++) {
			idx2 = r * twon2;
			idx3 = (rows - r + 1) * twon2;
			for (int c = columns + 2; c < twon2; c += 2) {
				a[idx2 + c] = a[idx3 - c];
				a[idx2 + c + 1] = -a[idx3 - c + 1];

			}
		}
		for (int r = 0; r <= rows / 2; r++) {
			idx1 = r * twon2;
			idx4 = ((rows - r) % rows) * twon2;
			for (int c = 0; c < twon2; c += 2) {
				idx2 = idx1 + c;
				idx3 = idx4 + (twon2 - c) % twon2;
				a[idx3] = a[idx2];
				a[idx3 + 1] = -a[idx2 + 1];
			}
		}
		a[columns] = -a[1];
		a[1] = 0;
		idx1 = n1d2 * twon2;
		a[idx1 + columns] = -a[idx1 + 1];
		a[idx1 + 1] = 0;
		a[idx1 + columns + 1] = 0;
	}
}
