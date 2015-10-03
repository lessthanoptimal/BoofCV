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

package boofcv.evaluation;

import org.ddogleg.sorting.QuickSort_F64;


/**
 * Computes different statistics
 *
 * @author Peter Abeles
 */
public class ErrorStatistics {
	double errors[];
	int size = 0;
	boolean fixated = false;

	QuickSort_F64 sort = new QuickSort_F64();

	public ErrorStatistics( int initialSize ) {
		this.errors = new double[ initialSize ];
	}

	public void reset() {
		size = 0;
		fixated = false;
	}

	public void add( double e ) {
		if( size >= errors.length ) {
			double t[] = new double[ errors.length*2 ];
			System.arraycopy(errors,0,t,0,errors.length );
			errors = t;
		}
		errors[size++] = e;
	}

	public void fixate() {
		sort.sort(errors,size);
		fixated = true;
	}

	public double getFraction( double frac ) {
		if( !fixated )
			fixate();
		return errors[ (int)(size*frac)];
	}
}
