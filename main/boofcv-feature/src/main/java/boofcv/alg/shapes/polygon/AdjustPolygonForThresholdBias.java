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

package boofcv.alg.shapes.polygon;

import georegression.geometry.UtilLine2D_F64;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;

/**
 * When a binary image is created some of the sides are shifted up to a pixel. This is due to how the image is
 * discretized. What this algorithm does is adjust the polygon to move the bias by basically undoing the
 * floor() operation/
 *
 * @author Peter Abeles
 */
public class AdjustPolygonForThresholdBias {

	private FastQueue<LineSegment2D_F64> segments = new FastQueue<>(LineSegment2D_F64.class,true);
	private LineGeneral2D_F64 ga = new LineGeneral2D_F64();
	private LineGeneral2D_F64 gb = new LineGeneral2D_F64();
	private Point2D_F64 intersection = new Point2D_F64();

	/**
	 * Processes and adjusts the polygon. If after adjustment a corner needs to be removed because two sides are
	 * parallel then the size of the polygon can be changed.
	 *
	 * @param polygon The polygon that is to be adjusted. Modified.
	 * @param clockwise Is the polygon in a lockwise orientation?
	 */
	public void process( Polygon2D_F64 polygon, boolean clockwise) {
		int N = polygon.size();
		segments.resize(N);

		// Apply the adjustment independently to each side
		for (int i = N - 1, j = 0; j < N; i = j, j++) {

			int ii,jj;
			if( clockwise ) {
				ii = i; jj = j;
			} else {
				ii = j; jj = i;
			}

			Point2D_F64 a = polygon.get(ii), b = polygon.get(jj);

			double dx = b.x - a.x;
			double dy = b.y - a.y;
			double l = Math.sqrt(dx * dx + dy * dy);
			if( l == 0) {
				throw new RuntimeException("Two identical corners!");
			}

			// only needs to be shifted in two directions
			if( dx < 0 )
				dx = 0;
			if( dy > 0 )
				dy = 0;

			LineSegment2D_F64 s = segments.get(ii);
			s.a.x = a.x - dy/l;
			s.a.y = a.y + dx/l;
			s.b.x = b.x - dy/l;
			s.b.y = b.y + dx/l;
		}

		// Find the intersection between the adjusted lines to convert it back into polygon format
		for (int i = N - 1, j = 0; j < N; i = j, j++) {
			int ii,jj;
			if( clockwise ) {
				ii = i; jj = j;
			} else {
				ii = j; jj = i;
			}

			UtilLine2D_F64.convert(segments.get(ii),ga);
			UtilLine2D_F64.convert(segments.get(jj),gb);

			if( null != Intersection2D_F64.intersection(ga,gb,intersection)) {
				// very acute angles can cause a large delta. This is conservative and prevents that
				if( intersection.distance2(polygon.get(jj)) < 20 ) {
					polygon.get(jj).set(intersection);
				}
			}
		}

		// if two corners have a distance of 1 there are some conditions which exist where the corners can be shifted
		// such that two points will now be equal. Avoiding the shift isn't a good idea shift the shift should happen
		// there might be a more elegant solution to this problem but this is probably the simplest
		UtilPolygons2D_F64.removeAdjacentDuplicates(polygon,1e-8);
	}
}
