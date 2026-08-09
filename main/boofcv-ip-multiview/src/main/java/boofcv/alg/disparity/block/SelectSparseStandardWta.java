/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

/**
 * <p>
 * Selects the disparity the smallest error and optionally applies several different types of validation to remove false
 * positives. The two validations it can apply are maxError and texture based.
 * See {@link SelectDisparityWithChecksWta} for more details on validation checks.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class SelectSparseStandardWta<ArrayType>
		implements DisparitySparseSelect<ArrayType> {

	// found disparity
	protected double disparity;

	// Maximum allowed error, in the score's native format so that no conversion is needed when comparing
	protected int maxErrorI;
	protected float maxErrorF;

	// Tolerance for right to left validation. Disable with -1
	protected int tolRightToLeft;

	/**
	 * @param maxError Maximum allowed error. See comments above.
	 * @param texture Texture threshold. See comments above.
	 */
	protected SelectSparseStandardWta( double maxError, double texture, int tolRightToLeft ) {
		// Scores are rejected when strictly greater than the limit, so truncating towards zero leaves integer
		// scores with the same accept/reject decision. A limit too large for an int saturates, which disables it.
		this.maxErrorI = maxError <= 0 ? Integer.MAX_VALUE : (int)maxError;
		this.maxErrorF = maxError <= 0 ? Float.MAX_VALUE : (float)maxError;
		this.tolRightToLeft = tolRightToLeft;
		setTexture(texture);
	}

	/**
	 * Sets the texture threshold.
	 *
	 * @param texture Texture threshold.
	 */
	protected abstract void setTexture( double texture );

	/**
	 * Specifies tolerance for right to left validation.
	 *
	 * @param maxError Maximum number of pixels different for right to left validation. If error is < 0 then disabled.
	 */
	protected void setValidateRtoL( int maxError ) {
		this.tolRightToLeft = maxError;
	}

	@Override
	public double getDisparity() {
		return disparity;
	}
}
