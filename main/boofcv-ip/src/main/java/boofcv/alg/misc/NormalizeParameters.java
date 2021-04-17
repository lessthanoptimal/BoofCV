/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.misc;

/**
 * Parameters used to normalize an image for numerics.
 *
 * output = scale*(input - offset)
 *
 * @author Peter Abeles
 */
public class NormalizeParameters {
	public double offset;
	public double divisor;

	public NormalizeParameters() {
	}

	public NormalizeParameters( double offset, double divisor ) {
		this.offset = offset;
		this.divisor = divisor;
	}

	public double getOffset() {
		return offset;
	}

	public void setOffset( double offset ) {
		this.offset = offset;
	}

	public double getDivisor() {
		return divisor;
	}

	public void setDivisor( double divisor ) {
		this.divisor = divisor;
	}
}
