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

package boofcv.factory.transform.census;

/**
 * Different sampling patterns for {@link boofcv.alg.transform.census.CensusTransform}.
 *
 * @author Peter Abeles
 */
public enum CensusVariants {
	BLOCK_3_3(3*3-1),
	BLOCK_5_5(5*5-1),
	BLOCK_7_7(7*7-1),
	BLOCK_9_7(9*7-1),
	BLOCK_13_5(13*5-1),
	CIRCLE_9(9*9 - 4*6 - 1);

	CensusVariants( int bits ) {
		this.bits = bits;
	}

	final int bits;

	public int getBits() {
		return bits;
	}
}
