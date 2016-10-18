/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.calib.chess;

import boofcv.alg.shapes.polygon.BinaryPolygonDetector;
import boofcv.alg.shapes.polygon.PolygonHelper;
import boofcv.alg.shapes.polygon.RefineBinaryPolygon;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * Helper which expands polygons prior to optimization.  This is done to counter act the erosion step which shrunk
 * the polygon
 *
 * @author Peter Abeles
 */
public class ChessboardPolygonHelper<T extends ImageGray> implements PolygonHelper {
	BinaryPolygonDetector<T> detectorSquare;
	RefineBinaryPolygon<T> refineLine;
	RefineBinaryPolygon<T> refineCorner;

	double threshold = 400;

	int width,height;

	FastQueue<Point2D_F64> tmp = new FastQueue<>(Point2D_F64.class, true);

	public ChessboardPolygonHelper(BinaryPolygonDetector<T> detectorSquare,
								   RefineBinaryPolygon<T> refineLine ,
								   RefineBinaryPolygon<T> refineCorner ) {
		this.detectorSquare = detectorSquare;
		this.refineLine = refineLine;
		this.refineCorner = refineCorner;
	}

	@Override
	public void setImageShape(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public void adjustBeforeOptimize(Polygon2D_F64 polygon) {

		int N = polygon.size();
		tmp.resize(N);
		for (int i = 0; i < N; i++) {
			tmp.get(i).set(0,0);
		}

		for (int i = N-1,j=0; j < N; i=j,j++) {
			Point2D_F64 a = polygon.get(i);
			Point2D_F64 b = polygon.get(j);

			double dx = b.x-a.x;
			double dy = b.y-a.y;

			double l = Math.sqrt(dx*dx + dy*dy);
			dx *= 1.3/l;
			dy *= 1.3/l;

			Point2D_F64 _a = tmp.get(i);
			Point2D_F64 _b = tmp.get(j);

			_a.x -= dx;
			_a.y -= dy;
			_b.x += dx;
			_b.y += dy;
		}

		for (int i = 0; i < N; i++) {
			Point2D_F64 a = polygon.get(i);
			Point2D_F64 t = tmp.get(i);

			a.x += t.x;
			a.y += t.y;

			if( a.x < 0) a.x = 0;
			else if( a.x > width-1 ) a.x = width-1;
			if( a.y < 0) a.y = 0;
			else if( a.y > height-1 ) a.y = height-1;
		}

		if( refineCorner != null ) {
			double area = polygon.areaSimple();
			if (area < threshold) {
				detectorSquare.setRefinePolygon(refineLine);
			} else {
				detectorSquare.setRefinePolygon(refineCorner);
			}
		} else {
			detectorSquare.setRefinePolygon(refineLine);
		}
	}

	@Override
	public boolean filterContour(List<Point2D_I32> contour, boolean touchesBorder, boolean distorted) {
		return true;
	}

	/**
	 * If not touching the border then the number of corners must be 4.  If touching the border there must be
	 * at least 3 corners not touching the border.  7 corners at most.  If there were 8 then all sides of a square
	 * would be touching the border.    No more than 3 corners since that's the most number of non-border corners
	 * a square can have.
	 */
	@Override
	public boolean filterPixelPolygon(List<Point2D_I32> externalUndist, List<Point2D_I32> externalDist,
									  GrowQueue_I32 splits, boolean touchesBorder) {

		if( touchesBorder ) {
			if( splits.size() > 7 || splits.size() < 3)
				return false;
			int totalRegular = 0;
			for (int i = 0; i < splits.size(); i++) {
				Point2D_I32 p = externalDist.get(splits.get(i));
				if( !(p.x == 0 || p.y == 0 || p.x == width-1 || p.y == height-1))
					totalRegular++;
			}
			return totalRegular > 0 && totalRegular <= 4; // should be 3, but noise/imprecision in corner can make it 4
		} else {
			return splits.size() == 4;
		}
	}
}
