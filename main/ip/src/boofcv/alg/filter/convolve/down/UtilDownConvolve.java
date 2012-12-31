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

package boofcv.alg.filter.convolve.down;

/**
 * @author Peter Abeles
 */
public class UtilDownConvolve {
	public static int computeMaxSide( int sideLength , int skip , int radius ) {
		// length of size of output image in input image coordinates
		int ret = sideLength - (sideLength % skip);

		if( ret + radius >= sideLength ) {
			ret = sideLength - radius - 1;
			ret = ret - (ret%skip);
		} else {
			ret -= skip;
		}

		return ret;
	}

	public static int computeOffset( int skip , int radius ) {
		return radius <= skip ? skip : radius + radius % skip;
	}
}
