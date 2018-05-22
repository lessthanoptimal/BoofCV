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

package boofcv.alg.fiducial.calib.chess;

import boofcv.abst.shapes.polyline.PointsToPolyline;
import boofcv.alg.shapes.polygon.PolygonHelper;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.GrowQueue_B;

import java.util.List;

/**
 * Helper which expands polygons prior to optimization.  This is done to counter act the erosion step which shrunk
 * the polygon
 *
 * @author Peter Abeles
 */
public class ChessboardPolygonHelper<T extends ImageGray<T>> implements PolygonHelper {

	int width,height;

	@Override
	public void setImageShape(int width, int height) {
		this.width = width;
		this.height = height;
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
	public boolean filterPixelPolygon(Polygon2D_F64 undistorted , Polygon2D_F64 distorted,
									  GrowQueue_B touches, boolean touchesBorder) {

		if( touchesBorder ) {
			if( distorted.size() < 3)
				return false;
			int totalRegular = distorted.size();
			for (int i = 0; i < distorted.size(); i++) {
				if( touches.get(i) )
					totalRegular--;
			}
			return totalRegular > 0;
			// Would be 3 if it was filled in, but local binary can cause external contour to be concave
		} else {
			return distorted.size() == 4;
		}
	}

	@Override
	public void configureBeforePolyline(PointsToPolyline contourToPolyline, boolean touchesBorder) {
		if( touchesBorder ) {
			contourToPolyline.setConvex(false);
			contourToPolyline.setMinimumSides(3);
			contourToPolyline.setMaximumSides(8);
		} else {
			contourToPolyline.setConvex(true);
			contourToPolyline.setMinimumSides(4);
			contourToPolyline.setMaximumSides(4);
		}
	}
}
