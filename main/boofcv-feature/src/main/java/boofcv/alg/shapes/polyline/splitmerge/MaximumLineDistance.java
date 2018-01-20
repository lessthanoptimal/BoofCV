/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import georegression.metric.Distance2D_F64;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.point.Point2D_I32;

import java.util.List;

/**
 * Selects the point which is the farthest away from the line.
 */
public class MaximumLineDistance implements SplitSelector {

	LineParametric2D_F64 line = new LineParametric2D_F64();

	@Override
	public void selectSplitPoint(List<Point2D_I32> contour, int indexA, int indexB, PolylineSplitMerge.SplitResults results) {
		PolylineSplitMerge.assignLine(contour, indexA, indexB, line);

		if( indexB >= indexA ) {
			results.index = indexA;
			results.score = -1;
			for (int i = indexA+1; i < indexB; i++) {
				Point2D_I32 p = contour.get(i);
				double distanceSq = Distance2D_F64.distanceSq(line,p.x,p.y);

				if( distanceSq > results.score ) {
					results.score = distanceSq;
					results.index = i;
				}
			}
		} else {
			results.index = indexA;
			results.score = -1;
			int distance = contour.size()-indexA + indexB;
			for (int i = 1; i < distance; i++) {
				int index = (indexA+i)%contour.size();
				Point2D_I32 p = contour.get(index);
				double distanceSq = Distance2D_F64.distanceSq(line,p.x,p.y);

				if( distanceSq > results.score ) {
					results.score = distanceSq;
					results.index = index;
				}
			}
		}

//		if( results.index >= contour.size() )
//			throw new RuntimeException("Egads");
	}

	@Override
	public int compareScore(double scoreA, double scoreB) {
		if( scoreA > scoreB )
			return 1;
		else if( scoreA < scoreB )
			return -1;
		else
			return 0;
	}
}
