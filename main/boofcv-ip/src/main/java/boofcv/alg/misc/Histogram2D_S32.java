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

package boofcv.alg.misc;

import java.util.Arrays;

/**
 * 2D histogram used to count.
 *
 * @author Peter Abeles
 */
public class Histogram2D_S32 {
	/** Value of each cell in the histogram */
	public int[] data = new int[0];

	/** Histogram's shape */
	public int rows, cols;

	public Histogram2D_S32() {
	}

	/**
	 * Changes the size of the histogram to match the specified shape. Size of data is changed only
	 * when it's too small for the new shape.
	 */
	public void reshape( int rows, int cols ) {
		this.rows = rows;
		this.cols = cols;
		int length = rows*cols;
		if (length > data.length)
			data = new int[length];
	}

	/**
	 * Fills all elements in data that are in use to zero.
	 */
	public void zero() {
		Arrays.fill(data, 0, size(), 0);
	}

	/**
	 * Increments the value at the specified coordinate by 1
	 */
	public void increment( int row, int col ) {
		data[row*cols + col]++;
	}

	/**
	 * Sums up all the elements in the specified row and returns the result
	 */
	public int sumRow( int row ) {
		int total = 0;
		for (int col = 0; col < cols; col++) {
			total += data[row*cols + col];
		}
		return total;
	}

	/**
	 * Sums up all the elements in the specified column and returns the result
	 */
	public int sumCol( int col ) {
		int total = 0;
		for (int row = 0; row < rows; row++) {
			total += data[row*cols + col];
		}
		return total;
	}

	/**
	 * Returns the column with the largest value in the specified row
	 */
	public int maximumColIdx( int row ) {
		int best = 0;
		int bestIdx = -1;
		for (int col = 0; col < cols; col++) {
			int v = data[row*cols + col];
			if (v > best) {
				best = v;
				bestIdx = col;
			}
		}
		return bestIdx;
	}

	/**
	 * Returns the row with the largest value in the specified column
	 */
	public int maximumRowIdx( int col ) {
		int best = 0;
		int bestIdx = -1;
		for (int row = 0; row < rows; row++) {
			int v = data[row*cols + col];
			if (v > best) {
				best = v;
				bestIdx = row;
			}
		}
		return bestIdx;
	}

	/**
	 * Sum of all the elements in the histogram
	 */
	public int sum() {
		int total = 0;
		int size = size();
		for (int i = 0; i < size; i++) {
			total += data[i];
		}
		return total;
	}

	/**
	 * Value at the specified coordinate in the histogram
	 */
	public int get( int row, int col ) {
		return data[row*cols + col];
	}

	/**
	 * Value at the specified coordinate in the histogram
	 */
	public void set( int row, int col, int value ) {
		data[row*cols + col] = value;
	}

	/**
	 * Index of the specified coordinate
	 */
	public int indexOf( int row, int col ) {
		return row*cols + col;
	}

	/**
	 * Number of elements in the histogram
	 */
	public int size() {
		return rows*cols;
	}

	/**
	 * Prints the histogram to stdout
	 *
	 * @param format Printf format for the value
	 */
	public void print( String format ) {
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				System.out.printf(format + " ", get(row, col));
			}
			System.out.println();
		}
	}
}
