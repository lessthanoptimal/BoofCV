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
 * <p>
 * Selects the disparity the smallest error and optionally applies several different types of validation to remove false
 * positives.  The two validations it can apply are maxError and texture based.
 * See {@link boofcv.alg.feature.disparity.SelectRectStandard} for more details on validation checks.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class SelectSparseStandardWta<ArrayType>
		implements DisparitySparseSelect<ArrayType> {

	// found disparity
	protected double disparity;

	// maximum allowed error
	protected int maxError;

	/**
	 *
	 * @param maxError Maximum allowed error.  See comments above.
	 * @param texture Texture threshold.  See comments above.
	 */
	public SelectSparseStandardWta(int maxError, double texture) {
		this.maxError = maxError <= 0 ? Integer.MAX_VALUE : maxError;
		setTexture(texture);
	}

	/**
	 * Sets the texture threshold.
	 *
	 * @param texture Texture threshold.
	 */
	protected abstract void setTexture( double texture );

	@Override
	public double getDisparity() {
		return disparity;
	}

}
