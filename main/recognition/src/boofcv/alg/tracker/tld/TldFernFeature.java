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

package boofcv.alg.tracker.tld;

/**
 * Contains information on a fern feature value pair.
 *
 * @author Peter Abeles
 */
public class TldFernFeature {

	/**
	 * Numerical value of this descriptor
	 */
	public int value;
	/**
	 * Number of times P-constraint has been applied
	 */
	public int numP;
	/**
	 * Number of times N-constraint has been applied
	 */
	public int numN;

	/**
	 * Posterior probability.  P/(P+N)
	 */
	public double posterior;

	public void init( int value ) {
		this.value = value;
		numP = numN = 0;
	}

	public void incrementP() {
		numP++;
		computePosterior();
	}

	public void incrementN() {
		numN++;
		computePosterior();
	}

	private void computePosterior() {
		posterior = numP/(double)(numN + numP);
	}

	public double getPosterior() {
		return posterior;
	}

	public int getValue() {
		return value;
	}
}
