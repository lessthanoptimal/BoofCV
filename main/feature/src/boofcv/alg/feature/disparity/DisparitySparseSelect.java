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

package boofcv.alg.feature.disparity;

/**
 * Computes the disparity given disparity score calculations provided by
 * {@link boofcv.alg.feature.disparity.DisparitySparseScoreSadRect}. Array specifies the
 * type of primitive array that stores the scores that it processes,
 *
 * @author Peter Abeles
 */
public interface DisparitySparseSelect<ArrayType> {

	/**
	 * Examines disparity scores and looks for the best correspondence. If no correspondence
	 * can be found then false is returned.
	 *
	 * @param scores Set of disparity scores.
	 * @param maxDisparity Maximum allowed disparity.
	 * @return true if a valid correspondence was found
	 */
	public boolean select( ArrayType scores , int maxDisparity );

	/**
	 * Returns the found disparity
	 *
	 * @return disparity
	 */
	public double getDisparity();
}
