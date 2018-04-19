/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

/**
 * Computes a moving average with a decay function
 *
 * decay variable sets how quickly the average is updated. 1.0 = static and no change. 0.0 = sets to most
 * recent sample.
 *
 * @author Peter Abeles
 */
public class MovingAverage {
	double decay;
	double average;
	boolean first; // first time it is run

	public MovingAverage() {
		this(0.95);
	}

	public MovingAverage( double decay ) {
		setDecay(decay);
		this.decay = decay;
		reset();
	}

	public void reset() {
		first = true;
		average = 0;
	}

	public double update( double sample ) {
		if( first ) {
			first = false;
			average = sample;
		} else {
			average = average * decay + (1.0 - decay) * sample;
		}
		return average;
	}

	public double getAverage() {
		return average;
	}

	public double getDecay() {
		return decay;
	}

	public void setDecay(double decay) {
		if( decay < 0 || decay > 1.0 )
			throw new IllegalArgumentException("Decay must be from 0 to 1, inclusive");
		this.decay = decay;
	}

	public boolean isFirst() {
		return first;
	}
}
