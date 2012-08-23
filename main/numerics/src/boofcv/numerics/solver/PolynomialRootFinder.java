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

package boofcv.numerics.solver;

import org.ejml.data.Complex64F;

import java.util.List;

/**
 * Interface for finding the roots of a polynomial.
 *
 * @author Peter Abeles
 */
public interface PolynomialRootFinder {

	/**
	 * <p>
	 * Finds the roots of a polynomial with the specified coefficients.  The coefficient's index is the
	 * same as the variables order:<br>
	 * <br>
	 * polynomial = c[0] + x*c[1] + x<sup>2</sup>*c[2] + ... + x<sup>n</sup>*c[n]
	 * </p>
	 *
	 * @param coefficients Polynomial coefficients in increasing order.
	 * @return true if successful.
	 */
	public boolean process( double []coefficients );

	/**
	 * Returns all the found roots of the polynomial.
	 *
	 * @return roots
	 */
	public List<Complex64F> getRoots();
}
