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
 * <p>
 * A multi dimensional histogram.  This is an extension of {@link TupleDesc_F64} to faciliate comparision of different
 * histograms.  Each dimension in the histogram coverages a range from the minimum to maximum value.  This range is
 * divided by the number of bins in a dimension.  Data is stored in a row major format from lower dimension to upper
 * dimension.  For a 3D histogram a coordinate (a,b,c) would have index = c + b*length[0] + a*length[0]*length[1].
 * </p>
 *
 * <p>Usage example for RGB image:</p>
 * <pre>
 * Histogram_F64 hist = new Histogram_F64(20,20,20);
 * hist.setRange(0,0,255);
 * hist.setRange(1,0,255);
 * hist.setRange(2,0,255);
 * GHistogramFeatureOps.histogram(image,hist);</pre>
 *
 *
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
		int N = lengths[lengths.length-1];

		for (int i = 1; i < lengths.length; i++) {
			strides[strides.length-i-1] = N;
			N *= lengths[lengths.length-i-1];
		}
		strides[strides.length-1] = 1;

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

	/**
	 * Sets the minimum allowed value in a particular dimension
	 * @param dimension Which dimension
	 * @param value minimum value
	 */
	public void setMinimum( int dimension , double value ) {
		valueMin[dimension] = value;
	}

	/**
	 * Sets the maximum allowed value in a particular dimension
	 * @param dimension Which dimension
	 * @param value maximum value
	 */
	public void setMaximum( int dimension , double value ) {
		valueMax[dimension] = value;
	}

	/**
	 * Returns the minimum allowed value in a dimension
	 * @param dimension Which dimension
	 * @return minimum value
	 */
	public double getMinimum( int dimension ) {
		return valueMin[dimension];
	}

	/**
	 * Returns the maximum allowed value in a dimension
	 * @param dimension Which dimension
	 * @return maximum value
	 */
	public double getMaximum( int dimension ) {
		return valueMax[dimension];
	}

	/**
	 * Given a value it returns the corresponding bin index in this histogram for the specified dimension. This
	 * is for floating point values.
	 *
	 * @param dimension  Which dimension the value belongs to
	 * @param value Floating point value between min and max, inclusive.
	 * @return The index/bin
	 */
	public int getDimensionIndex( int dimension , double value ) {
		double min = valueMin[dimension];
		double max = valueMax[dimension];

		double fraction = ((value-min)/(max-min));
		if( fraction >= 1.0 )
			return length[dimension]-1;
		else {
			return (int)(fraction*length[dimension]);
		}
	}

	/**
	 * Given a value it returns the corresponding bin index in this histogram for integer values.  The discretion
	 * is taken in account and 1 is added to the range.
	 *
	 * @param dimension  Which dimension the value belongs to
	 * @param value Floating point value between min and max, inclusive.
	 * @return The index/bin
	 */
	public int getDimensionIndex( int dimension , int value ) {
		double min = valueMin[dimension];
		double max = valueMax[dimension];

		double fraction = ((value-min)/(max-min+1.0));
		return (int)(fraction*length[dimension]);
	}

	/**
	 * For a 2D histogram it returns the array index for coordinate (i,j)
	 * @param i index along axis 0
	 * @param j index along axis 1
	 * @return array index
	 */
	public final int getIndex( int i , int j ) {
		return i*strides[0]+j;
	}
	/**
	 * For a 3D histogram it returns the array index for coordinate (i,j,k)
	 * @param i index along axis 0
	 * @param j index along axis 1
	 * @param k index along axis 2
	 * @return array index
	 */

	public final int getIndex( int i , int j , int k ) {
		return i*strides[0]+j*strides[1] + k;
	}

	/**
	 * For a N-Dimensional histogram it will return the array index for the N-D coordinate
	 *
	 * @param coordinate N-D coordinate
	 * @return index
	 */
	public final int getIndex( int coordinate[] ) {
		int index = coordinate[0]*strides[0];
		for (int i = 1; i < coordinate.length; i++) {
			index += strides[i]*coordinate[i];
		}

		return index;
	}

	/**
	 * Returns the value at the 2D coordinate
	 * @param i index along axis-0
	 * @param j index along axis-1
	 * @return histogram value
	 */
	public double get( int i , int j ) {
		return  value[getIndex(i,j)];
	}

	/**
	 * Returns the value at the 3D coordinate
	 * @param i index along axis-0
	 * @param j index along axis-1
	 * @param k index along axis-2
	 * @return histogram value
	 */
	public double get( int i , int j , int k ) {
		return  value[getIndex(i,j, k)];
	}

	/**
	 * Returns the value at the N-D coordinate
	 * @param coordinate N-D coordinate
	 * @return histogram value
	 */
	public double get( int coordinate[]  ) {
		return  value[getIndex(coordinate)];
	}

	/**
	 * Creates an exact copy of "this" histogram
	 */
	public Histogram_F64 copy() {
		Histogram_F64 out = newInstance();

		System.arraycopy(value,0,out.value,0,length.length);

		return out;
	}

	/**
	 * Creates a new instance of this histogram which has the same "shape" and min / max values.
	 */
	public Histogram_F64 newInstance() {
		Histogram_F64 out = new Histogram_F64(length);

		for (int i = 0; i < length.length; i++) {
			out.setRange(i,valueMin[i],valueMax[i]);
		}

		return out;
	}
}
