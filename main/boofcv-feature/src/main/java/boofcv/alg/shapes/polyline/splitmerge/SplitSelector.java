/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.shapes.polyline.splitmerge;

import georegression.struct.point.Point2D_I32;

import java.util.List;

/**
 * Interface for splitting a line along a contour.
 *
 * @author Peter Abeles
 */
public interface SplitSelector {
	/**
	 * Selects the best point to split a long along a contour. Start and end locations are always traversed in the
	 * positive direction along the contour.
	 *
	 * @param contour List of points along a contour in order
	 * @param indexA Start of line
	 * @param indexB End of line
	 * @param results Where to split
	 */
	void selectSplitPoint(List<Point2D_I32> contour , int indexA , int indexB , PolylineSplitMerge.SplitResults results );

	/**
	 * Compares two scores against each other
	 *
	 * @param scoreA Score
	 * @param scoreB Score
	 * @return 1 = scoreA is best, 0 both equal, -1 scoreB is best
	 */
	int compareScore( double scoreA , double scoreB );
}
