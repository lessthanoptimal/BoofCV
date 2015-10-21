/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.chess;

import boofcv.alg.shapes.polygon.BinaryPolygonDetector;
import boofcv.alg.shapes.polygon.PolygonHelper;
import boofcv.alg.shapes.polygon.RefineBinaryPolygon;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * Helper which expands polygons prior to optimization.  This is done to counter act the erosion step which shrunk
 * the polygon
 *
 * @author Peter Abeles
 */
public class ChessboardPolygonHelper<T extends ImageSingleBand> implements PolygonHelper {
	BinaryPolygonDetector<T> detectorSquare;
	RefineBinaryPolygon<T> refineLine;
	RefineBinaryPolygon<T> refineCorner;

	Point2D_F64 center = new Point2D_F64();

	double threshold = 400;

	public ChessboardPolygonHelper(BinaryPolygonDetector<T> detectorSquare,
								   RefineBinaryPolygon<T> refineLine ,
								   RefineBinaryPolygon<T> refineCorner ) {
		this.detectorSquare = detectorSquare;
		this.refineLine = refineLine;
		this.refineCorner = refineCorner;
	}

	@Override
	public void adjustBeforeOptimize(Polygon2D_F64 polygon) {

		center.x = 0;
		center.y = 0;
		for (int j = 0; j < 4; j++) {
			Point2D_F64 p = polygon.get(j);
			center.x += p.x;
			center.y += p.y;
		}
		center.x /= 4.0;
		center.y /= 4.0;

		for (int j = 0; j < 4; j++) {
			Point2D_F64 p = polygon.get(j);
			double dx = p.x-center.x;
			double dy = p.y-center.y;

			double r = Math.sqrt(dx*dx + dy*dy);

			// not really sure how this happens, but it is possible for the center to be exactly equal to one of the
			// corner points
			if( r > 0 ) {
				p.x += 1.4 * dx / r;
				p.y += 1.4 * dy / r;
			}
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
	public boolean filterPolygon(List<Point2D_I32> external, GrowQueue_I32 splits) {
		return true;
	}
}
