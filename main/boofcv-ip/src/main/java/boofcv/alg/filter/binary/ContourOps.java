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

package boofcv.alg.filter.binary;

import georegression.struct.point.Point2D_I32;

import java.util.List;

/**
 * Operations related to contours
 *
 * @author Peter Abeles
 */
public class ContourOps {
	/**
	 * Checks to see if the two contours are equivalent up to a shift in order.
	 *
	 * @param contourA First contour
	 * @param contourB Second contour
	 * @return true if equivalent
	 */
	public static boolean isEquivalent( final List<Point2D_I32> contourA,
										final List<Point2D_I32> contourB ) {
		if (contourA.size() != contourB.size())
			return false;
		if (contourA.size() == 0)
			return true;
		final int N = contourA.size();
		Point2D_I32 first = contourA.get(0);

		for (int i = 0; i < N; i++) {
			if (!contourB.get(i).equals(first)) {
				continue;
			}

			boolean success = true;
			for (int j = 1; j < N; j++) {
				int indexB = (j + i)%N;
				if (!contourA.get(j).equals(contourB.get(indexB))) {
					success = false;
					break;
				}
			}
			if (success)
				return true;
		}
		return false;
	}

	/**
	 * Returns true if the contour touches the image border
	 *
	 * @param contour the contour
	 * @param width Image width
	 * @param height Image height
	 * @return true if it touches or false if it doesn't
	 */
	public static boolean isTouchBorder( final List<Point2D_I32> contour, final int width, final int height ) {
		final int w = width - 1;
		final int h = height - 1;

		for (int j = 0; j < contour.size(); j++) {
			Point2D_I32 p = contour.get(j);
			if (p.x == 0 || p.y == 0 || p.x == w || p.y == h) {
				return true;
			}
		}

		return false;
	}
}
