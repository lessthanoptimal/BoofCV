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

package boofcv.alg.feature.detect.extract;


/**
 * <p/>
 * Only examine around candidate regions for corners using a relaxed rule
 * <p/>
 *
 * @author Peter Abeles
 */
public class NonMaxCandidateRelaxed extends NonMaxCandidateStrict {

	public NonMaxCandidateRelaxed(int searchRadius, float thresh, int border )
	{
	    super(searchRadius,thresh,border);
	}

	public NonMaxCandidateRelaxed() {
	}

	@Override
	protected boolean checkBorder(int center, float val, int c_x , int c_y )
	{
		int x0 = Math.max(0,c_x-radius);
		int y0 = Math.max(0,c_y-radius);
		int x1 = Math.min(input.width, c_x + radius + 1);
		int y1 = Math.min(input.height,c_y+radius+1);

		for( int i = y0; i < y1; i++ ) {
			int index = input.startIndex + i * input.stride + x0;
			for( int j = x0; j < x1; j++ , index++ ) {
				// don't compare the center point against itself
				if ( center == index )
					continue;

				if (val < input.data[index]) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	protected boolean checkInner( int center, float val ) {
		for (int i = -radius; i <= radius; i++) {
			int index = center + i * input.stride - radius;
			for (int j = -radius; j <= radius; j++, index++) {
				// don't compare the center point against itself
				if ( index == center )
					continue;

				if (val < input.data[index]) {
					return false;
				}
			}
		}
		return true;
	}
}
