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

package boofcv.alg.shapes.polygon;

import georegression.fitting.line.FitLine_I32;
import georegression.geometry.UtilLine2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.line.LinePolar2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * Fits lines to the contour then finds the intersection of the lines. This should provide a more accurate line estimate
 * than using the contour corner pixels alone.
 *
 * @author Peter Abeles
 */
public class RefinePolygonToContour {

	private List<Point2D_I32> work = new ArrayList<>();
	private LinePolar2D_F64 polar = new LinePolar2D_F64();

	private FastQueue<LineGeneral2D_F64> lines = new FastQueue<>(LineGeneral2D_F64.class,true);

	private Polygon2D_F64 delta = new Polygon2D_F64();

	/**
	 * Refines the estimate using all the points in the contour
	 *
	 * @param contour (Input) The shape's contour
	 * @param vertexes (Input) List of indexes that are vertexes in the contour
	 * @param clockwise (Input) Orientation of the polygon
	 * @param output (Output) Storage for where the found polygon is saved to
	 */
	public void process(List<Point2D_I32> contour , GrowQueue_I32 vertexes , boolean clockwise, Polygon2D_F64 output ) {

		fitLinesToContour(contour, vertexes, output);
		adjustForThresholdBias(output,clockwise);
	}

	protected void fitLinesToContour(List<Point2D_I32> contour, GrowQueue_I32 vertexes, Polygon2D_F64 output) {
		int numDecreasing = 0;
		for (int i = vertexes.size-1,j=0; j < vertexes.size; i=j,j++) {
			if( vertexes.get(i) > vertexes.get(j ) )
				numDecreasing++;
		}

		boolean decreasing = numDecreasing > 1;

		output.vertexes.resize(vertexes.size);
		lines.resize(vertexes.size);

		// fit lines to each size
		for (int i = vertexes.size-1,j=0; j < vertexes.size; i=j,j++) {
			int idx0 = vertexes.get(i);
			int idx1 = vertexes.get(j);

			if( decreasing ) {
				int tmp = idx0;idx0 = idx1;idx1=tmp;
			}

			if( idx0 > idx1 ) {
				// handle special case where it wraps around
				work.clear();
				for (int k = idx0; k < contour.size(); k++) {
					work.add( contour.get(k));
				}
				for (int k = 0; k < idx1; k++) {
					work.add( contour.get(k));
				}
				FitLine_I32.polar(work,0,work.size(),polar);
			} else {
				FitLine_I32.polar(contour,idx0,idx1-idx0,polar);
			}

			UtilLine2D_F64.convert(polar,lines.get(i));
		}

		// find the corners by intersecting the side
		for (int i = vertexes.size-1,j=0; j < vertexes.size; i=j,j++) {
			LineGeneral2D_F64 lineA = lines.get(i);
			LineGeneral2D_F64 lineB = lines.get(j);

			Intersection2D_F64.intersection(lineA,lineB,output.get(j));
		}
	}

	public void adjustForThresholdBias(Polygon2D_F64 polygon, boolean clockwise) {
		int N = polygon.size();
		delta.vertexes.resize(N);
		for (int i = 0; i < N; i++) {
			delta.get(i).set(0, 0);
		}

		for (int i = N - 1, j = 0; j < N; i = j, j++) {

			int ii,jj;
			if( clockwise ) {
				ii = i; jj = j;
			} else {
				ii = j; jj = i;
			}

			Point2D_F64 a = polygon.get(ii), b = polygon.get(jj);

			double dx0 = b.x - a.x;
			double dy0 = b.y - a.y;
			double l0 = Math.sqrt(dx0 * dx0 + dy0 * dy0);

			if( dx0 < 0 )
				dx0 = 0;
			if( dy0 > 0 )
				dy0 = 0;

			Point2D_F64 _a = delta.get(ii), _b = delta.get(jj);

			_a.x += -dy0/l0;
			_a.y +=  dx0/l0;
			_b.x += -dy0/l0;
			_b.y +=  dx0/l0;
		}

		for (int i = 0; i < N; i++) {
			Point2D_F64 a = polygon.get(i);
			Point2D_F64 b = delta.get(i);
			a.x += b.x;
			a.y += b.y;
		}
	}
}
