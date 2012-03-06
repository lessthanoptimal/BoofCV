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

package boofcv.abst.geo;

import boofcv.alg.geo.AssociatedPair;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Interface for performing non-linear optimization on an essential,fundamental,and homography matrix.
 * Basically any geometric transform describing the relationship between two images that uses a 3x3
 * matrix.
 *
 * @author Peter Abeles
 */
public interface RefineEpipolarMatrix {

	/**
	 * Processes and refined the specified matrix.  Be sure the order
	 * of the observations on 'obs' is correct for the given matrix.
	 *
	 * @param F Matrix that describes the relationship. Must be 3x3. Not modified.
	 * @param obs List of observations.  Pixel for fundamental or normalized for essential.
	 * @return true if it was successful.
	 */
	public boolean process( DenseMatrix64F F , List<AssociatedPair> obs );

	/**
	 * The refined 3x3 matrix.
	 *
	 * @return Found solution.
	 */
	public DenseMatrix64F getRefinement();
}
