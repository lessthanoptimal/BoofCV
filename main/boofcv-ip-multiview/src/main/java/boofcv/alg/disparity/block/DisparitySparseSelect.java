/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.disparity.block;

import boofcv.alg.disparity.block.score.DisparitySparseRectifiedScoreBM;

/**
 * Computes the disparity given disparity score calculations provided by
 * {@link DisparitySparseRectifiedScoreBM}. Array specifies the
 * type of primitive array that stores the scores that it processes,
 *
 * @author Peter Abeles
 */
public interface DisparitySparseSelect<ArrayType> {

	/**
	 * Examines disparity scores and looks for the best correspondence. If no correspondence
	 * can be found then false is returned.
	 *
	 * @param scorer Computes the disparity score
	 * @param x Pixel coordinate x-axis
	 * @param y Pixel coordinate y-axis
	 * @return true if a valid correspondence was found
	 */
	boolean select(DisparitySparseRectifiedScoreBM<ArrayType,?> scorer , int x, int y);

	/**
	 * Returns the found disparity
	 *
	 * @return disparity
	 */
	double getDisparity();
}
