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

package boofcv.alg.feature.detect.extract;

/**
 * <p/>
 * Applies a strict peak rule to candidates
 * <p/>
 *
 * @author Peter Abeles
 */
public class NonMaxCandidateStrict extends NonMaxCandidate {

	@Override
	protected boolean searchMin(int center, float val) {
		for( int i = y0; i < y1; i++ ) {
			int index = input.startIndex + i * input.stride + x0;
			for( int j = x0; j < x1; j++ , index++ ) {
				// don't compare the center point against itself
				if ( center == index )
					continue;

				if (val >= input.data[index]) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	protected boolean searchMax(int center, float val) {
		for( int i = y0; i < y1; i++ ) {
			int index = input.startIndex + i * input.stride + x0;
			for( int j = x0; j < x1; j++ , index++ ) {
				// don't compare the center point against itself
				if ( center == index )
					continue;

				if (val <= input.data[index]) {
					return false;
				}
			}
		}
		return true;
	}
}
