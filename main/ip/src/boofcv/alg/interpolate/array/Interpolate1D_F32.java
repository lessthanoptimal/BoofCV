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

package boofcv.alg.interpolate.array;


/**
 * Provides much of the basic house keeping needed for interpolating 1D data.  Interpolation
 * is done using sample points y[i] = f(x[i]) where x is a monotonically increasing or decreasing
 * function.
 *
 * @author Peter Abeles
 */
public abstract class Interpolate1D_F32 {

	// the sample data
	// both axises must be monotonic increasing or decreasing
	protected int size;
	protected float x[];
	protected float y[];

	// how many points should the interpolation use
	// this is equal to the degree + 1
	protected int M;

	// this is an index that the sample point is between
	private int center;
	// this is the first index that the interpolation algorithm will use
	// 0 <= index0 <= size - M
	protected int index0;

	// should it hunt next time instead of searching?
	private boolean doHunt;

	// used to help it decide to hunt or search
	private int dj;
	// true if the data is increasing
	protected boolean ascend;

	/**
	 * @param degree The number of points used in the interpolation minus one
	 */
	public Interpolate1D_F32(int degree) {
		changeDegree(degree);
	}

	/**
	 * @param degree The number of points used in the interpolation minus one
	 * @param x	  Where the points are sample at. Not modifed. Reference saved.
	 * @param y	  The value at the sample points. Not modifed. Reference saved.
	 * @param size   The number of points used.
	 */
	public Interpolate1D_F32(int degree, float x[], float y[], int size) {
		this(degree);
		setInput(x, y, size);
	}

	/**
	 * Sets the data that is being interpolated.
	 *
	 * @param x	Where the points are sample at. Not modifed. Reference saved.
	 * @param y	The value at the sample points. Not modifed. Reference saved.
	 * @param size The number of points used.
	 */
	public void setInput(float x[], float y[], int size) {
		if (x.length < size || y.length < size) {
			throw new IllegalArgumentException("Arrays too small for size.");
		}
		if (size < M) {
			throw new IllegalArgumentException("Not enough data points for M");
		}

		this.x = x;
		this.y = y;
		this.size = size;
		this.dj = Math.min(1, (int) Math.pow(size, 0.25));
		ascend = x[size - 1] >= x[0];
	}

	/**
	 * Performs interpolation at the sample point.
	 *
	 * @param testX Where the interpolated value is done at.
	 * @return The interpolated value at sampleX.
	 */
	public float process(float testX) {
		if (doHunt) {
			hunt(testX);
		} else {
			bisectionSearch(testX, 0, size - 1);
		}

		return compute(testX);
	}

	/**
	 * Performs an interpolation using sample data starting at index0.  Little checking
	 * is done and it is assumed the user knows what he is doing.  Interpolation is done
	 * using points from index0 to index0 + M - 1
	 *
	 * @param index0 first sample point used in the interpolation.
	 * @param testX  Where the interpolated value is done at.
	 * @return The interpolated value at sampleX.
	 */
	public float process(int index0, float testX) {
		this.index0 = index0;
		return compute(testX);
	}

	/**
	 * This is where the specific implementation of the interpolation is done.  It should
	 * use points index0 to index0 + M - 1 in its interpolation
	 *
	 * @param testX Where the interpolated value is done at.
	 * @return The interpolated value at sampleX.
	 */
	protected abstract float compute(float testX);

	/**
	 * Changes the number of points used in the interpolation.
	 *
	 * @param degree Number of points used minus one.
	 */
	public void changeDegree(int degree) {
		this.M = degree + 1;
		doHunt = false;
	}

	/**
	 * To speed up finding the appropriate indexes to use in the interpolation it can use its
	 * previous results to search a smaller region than it would otherwise.
	 *
	 * @param val The value that is to be interpolated.
	 */
	protected void hunt(float val) {
		int lowerLimit = center;
		int upperLimit;
		int inc = 1;

		if (val >= x[lowerLimit] && ascend) {
			// hunt up
			for (; ; ) {
				upperLimit = lowerLimit + inc;
				// see if it is outside the table
				if (upperLimit >= size - 1) {
					upperLimit = size - 1;
					break;
				} else if (val < x[upperLimit] && ascend) {
					break;
				} else {
					lowerLimit = upperLimit;
					inc += inc;
				}
			}
		} else {
			// hunt down
			upperLimit = lowerLimit;
			for (; ; ) {
				lowerLimit = lowerLimit - inc;
				if (lowerLimit <= 0) {
					lowerLimit = 0;
					break;
				} else if (val >= x[lowerLimit] && ascend) {
					break;
				} else {
					upperLimit = lowerLimit;
					inc += inc;
				}
			}
		}

		bisectionSearch(val, lowerLimit, upperLimit);
	}

	/**
	 * Searches the x array by bisecting it.  This takes advantage of the data being
	 * monotonic.  This finds a center index which has the following property:
	 * x[center] &le; val < x[center+1]
	 * From that it selects index0 which is center - M/2.
	 *
	 * @param val		The value that is to be interpolated.
	 * @param lowerLimit Lower limit for x index.
	 * @param upperLimit The largest possible index of x
	 */
	protected void bisectionSearch(float val, int lowerLimit, int upperLimit) {
		while (upperLimit - lowerLimit > 1) {
			int middle = (upperLimit + lowerLimit) / 2;
			if (val >= x[middle] && ascend) {
				lowerLimit = middle;
			} else {
				upperLimit = middle;
			}
		}

		// decide if it should hunt or locate next time
		doHunt = Math.abs(lowerLimit - center) > dj;

		// make sure the points sampled for the polynomial are all within bounds
		center = lowerLimit;
		index0 = center - M / 2;
		if (index0 + M > size) {
			index0 = size - M;
		} else if (index0 < 0) {
			index0 = 0;
		}
	}
}