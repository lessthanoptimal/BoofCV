/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.color;

import boofcv.struct.feature.TupleDesc_F64;

/**
 * A multi dimensional histogram.  This is an extension of {@link TupleDesc_F64} to faciliate comparision of different
 * histograms.  Each dimension in the histogram coverages a range from the minimum to maximum value.  This range is
 * divided by the number of bins in a dimension.  Data is stored in a row major format from lower dimension to upper
 * dimension.  For a 3D histogram a coordinate (a,b,c) would have index = c + b*length[0] + a*length[0]*length[1].
 *
 * @author Peter Abeles
 */
public class Histogram_F64 extends TupleDesc_F64 {

	// number of elements in each dimension
	int length[];
	// precomputed strides.  strides[n] = strides[n-1]*length[n]
	int strides[];

	// the range of each dimension
	double valueMin[];
	double valueMax[];

	/**
	 * Creates a multi dimensional histogram where each dimension has the specified lengths.
	 *
	 * @param lengths Number of elements in each dimension
	 */
	public Histogram_F64( int ...lengths ) {
		this.length = lengths.clone();

		this.strides = new int[lengths.length];
		int N = lengths[0];

		for (int i = 1; i < lengths.length; i++) {
			strides[i-1] = N;
			N *= lengths[i];
		}

		value = new double[N];

		valueMin = new double[ lengths.length ];
		valueMax = new double[ lengths.length ];
	}

	/**
	 * Returns true if the min and max value for each dimension has been set
	 * @return true if range has been set
	 */
	public boolean isRangeSet() {
		for (int i = 0; i < getDimensions(); i++) {
			if( valueMin[i] == 0 && valueMax[i] == 0 ) {
				return false;
			}
		}

		return true;
	}

	/**
	 * The number of dimensions in the histogram.
	 * @return dimensions
	 */
	public int getDimensions() {
		return length.length;
	}

	/**
	 * Number of elements/bins along the specified dimension
	 * @param dimension Which dimension
	 * @return Number of bins
	 */
	public int getLength( int dimension ) {
		return length[dimension];
	}

	/**
	 * Specifies the minimum and maximum values for a specific dimension
	 *
	 * @param dimension Which dimension
	 * @param min The minimum value
	 * @param max The maximum value
	 */
	public void setRange( int dimension , double min , double max ) {
		valueMin[dimension] = min;
		valueMax[dimension] = max;
	}

	public void setMinimum( int dimension , double value ) {
		valueMin[dimension] = value;
	}

	public void setMaximum( int dimension , double value ) {
		valueMax[dimension] = value;
	}

	public double getMinimum( int dimension ) {
		return valueMin[dimension];
	}

	public double getMaximum( int dimension ) {
		return valueMax[dimension];
	}

	public int getDimensionIndex( int dimension , double value ) {
		double min = valueMin[dimension];
		double max = valueMin[dimension];

		double fraction = ((value-min)/(max-min));
		if( fraction >= 1.0 )
			return length[dimension]-1;
		else {
			return (int)(fraction*length[dimension]);
		}
	}

	public final int getIndex( int i , int j ) {
		return i*strides[0]+j;
	}

	public final int getIndex( int i , int j , int k ) {
		return i*strides[1]+j*strides[0] + k;
	}

	public final int getIndex( int coordinate[] ) {
		int index = coordinate[0];
		for (int i = 1; i < coordinate.length; i++) {
			index += strides[i]*coordinate[i];
		}

		return index;
	}

	public double get( int i , int j ) {
		return  value[getIndex(i,j)];
	}

	public double get( int i , int j , int k ) {
		return  value[getIndex(i,j, k)];
	}
}
