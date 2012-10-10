/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.associate;

/**
 * Scores the quality of fit between two feature descriptions.
 * Designed to handle fit metrics with different statics.  In general it is assumed that
 * scores with lower values are better.  They can be limited to zero or go negative.
 *
 * @param <T> Feature description type.
 *
 * @author Peter Abeles
 */
public interface ScoreAssociation<T> {

	/**
	 * Compute the fit score between the two features.  A better fit score will have a lower value.
	 *
	 * @param a first feature
	 * @param b second feature
	 * @return Quality of fit score.  Lower is better.
	 */
	public double score( T a , T b );

	/**
	 * Is the best/minimum score zero?
	 *
	 * @return true if the best zero is zero.
	 */
	public boolean isZeroMinimum();
}
