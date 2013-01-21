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
public abstract class MatchScoreType {
	/**
	 * Correlation scores can be both positive and negative values.  Scores with a larger positive value are considered
	 * to be better.
	 */
	public static MatchScoreType CORRELATION = new MatchScoreType() {

		@Override
		public boolean isZeroBest() {
			return false;
		}

		@Override
		public int compareTo(double scoreA, double scoreB) {
			if( scoreA > scoreB )
				return 1;
			else if( scoreA < scoreB )
				return -1;
			return 0;
		}
	};
	/**
	 * These error metrics have values greater than or equal to zero.  Closer the error is to zero the better
	 * the match is considered.
	 */
	public static MatchScoreType NORM_ERROR = new MatchScoreType() {

		@Override
		public boolean isZeroBest() {
			return true;
		}

		@Override
		public int compareTo(double scoreA, double scoreB) {
			if( scoreA < scoreB )
				return 1;
			else if( scoreA > scoreB )
				return -1;
			return 0;
		}
	};


	/**
	 * True if the best possible score has a value of zero
	 *
	 * @return True if the best possible score has a value of zero
	 */
	public abstract boolean isZeroBest();

	/**
	 * <p>
	 * Used to test to see which score is better than another score.
	 * </p>
	 *
	 * <p>
	 * 1 if scoreA better than scoreB<br>
	 * 0 if scoreA == scoreB<br>
	 * -1 if scoreA worse than scoreB<br>
	 * </p>
	 *
	 * @param scoreA match score
	 * @param scoreB match score
	 * @return  Returns a value of (-1,0,1) which indicates which score is better.
	 */
	public abstract int compareTo( double scoreA , double scoreB );
}
