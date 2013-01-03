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

package boofcv.struct.feature;

/**
 * Specifies the meaning of a match score.
 *
 * @author Peter Abeles
 */
public enum MatchScoreType {
	/**
	 * Correlation scores can be both positive and negative values.  Scores with a larger positive value are considered
	 * to be better.
	 */
	CORRELATION(false),
	/**
	 * These error metrics have values greater than or equal to zero.  Closer the error is to zero the better
	 * the match is considered.
	 */
	NORM_ERROR(true);

	boolean zeroBest;

	private MatchScoreType(boolean zeroBest) {
		this.zeroBest = zeroBest;
	}

	/**
	 * True if the best possible score has a value of zero
	 *
	 * @return True if the best possible score has a value of zero
	 */
	public boolean isZeroBest() {
		return zeroBest;
	}
}
