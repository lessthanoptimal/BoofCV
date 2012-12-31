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

package boofcv.numerics;

/**
 * Do linear interpolation between points in an array.  Sample points must be positive and less than one minus
 * the array's index.
 *
 * @author Peter Abeles
 */
public class InterpolateArray {
	public double data[];
	public double value;
	private int end;

	public InterpolateArray(double[] data) {
		this.data = data;
		end = data.length-1;
	}

	public boolean interpolate( double where ) {
		if( where < 0 )
			return false;
		int index = (int)where;
		if( index >= end )
			return false;

		double w = where-index;

		value = data[index]*(1.0-w) + data[index+1]*w;
		return true;
	}
}
