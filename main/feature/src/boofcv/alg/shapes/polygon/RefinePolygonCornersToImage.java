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

package boofcv.alg.shapes.polygon;

import boofcv.alg.shapes.corner.RefineCornerLinesToImage;
import boofcv.struct.image.ImageSingleBand;
import georegression.geometry.UtilPolygons2D_I32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Polygon2D_I32;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * Given a contour and the index of corner points it will fit the corners individually to sub-pixel precision
 * by locally fitting line segments near by each corner.  The length of each line segment is specified
 * by the number of pixels away it will traverse the contour.  It makes sure that the end point does not go
 * past the next corner.
 *
 * For the optimization to work it needs to know if the right side of the line is positive or negative.  The
 * input contour is assumed to be in the clock
 *
 * @author Peter Abeles
 */
public class RefinePolygonCornersToImage<T extends ImageSingleBand> {

	// number of pixels along the contour away should the end point be from the corner
	int pixelsAway;

	// When traversing the corners in the splits list in the clockwise direction is the
	// darker side to the right of the line or the left?  true for right
	boolean positiveRight;

	// used to refine individual corners
	RefineCornerLinesToImage<T> refineCorner;

	Point2D_F64 cornerPt = new Point2D_F64();
	Point2D_F64 leftPt = new Point2D_F64();
	Point2D_F64 rightPt = new Point2D_F64();

	protected List<Point2D_I32> contour;
	protected GrowQueue_I32 split;

	// work space used to determin if its CW or CCW
	Polygon2D_I32 poly = new Polygon2D_I32();

	/**
	 *
	 * @param pixelsAway How many indexes away in the contour list should end points be.
	 * @param positiveRight True the right side of the lines is darker than the left side.  See class description.
	 */
	public RefinePolygonCornersToImage(int pixelsAway, boolean positiveRight ) {
		this.pixelsAway = pixelsAway;
		this.positiveRight = positiveRight;
	}

	public void setImage( T gray ) {
		refineCorner.setImage(gray);
	}

	public boolean refine(List<Point2D_I32> contour , GrowQueue_I32 splits, Polygon2D_F64 refined) {

		int dir = determineDirection(contour, splits);

		int numGood = 0;
		for (int cornerS = 0; cornerS < splits.size(); cornerS++) {
			int indexLeft  = pickEndIndex(cornerS,  dir);
			int indexRight = pickEndIndex(cornerS, -dir);

			Point2D_I32 contourCorner = contour.get(splits.get(cornerS));
			Point2D_I32 contourLeft = contour.get(indexLeft);
			Point2D_I32 contourRight = contour.get(indexRight);

			cornerPt.set(contourCorner.x,contourCorner.y);
			leftPt.set(contourLeft.x,contourLeft.y);
			rightPt.set(contourRight.x,contourRight.y);

			if( refineCorner.refine(cornerPt,leftPt,rightPt)) {
				refined.get(cornerS).set( refineCorner.getRefinedCorner());
				numGood++;
			} else {
				// use the original
				refined.get(cornerS).set(cornerPt);
			}
		}

		// allow partial success
		return numGood >= splits.size/2;
	}

	/**
	 * Determines the direction it should process the lines. 1 = left positive.  -1 = left
	 * negative
	 */
	private int determineDirection(List<Point2D_I32> contour, GrowQueue_I32 splits) {
		poly.vertexes.resize(splits.size);
		for (int i = 0; i < splits.size(); i++) {
			poly.vertexes.get(i).set( contour.get( splits.get(i)));
		}

		boolean ccw = UtilPolygons2D_I32.isCCW(poly.vertexes.toList());

		int dir;
		if( ccw ) {
			if( positiveRight ) {
				dir = -1;
			} else {
				dir = 1;
			}
		} else {
			if( positiveRight ) {
				dir = 1;
			} else {
				dir = -1;
			}
		}
		return dir;
	}

	/**
	 * Selects the index to use as the end point.  Makes sure that the index does not go past
	 * the next corner.
	 *
	 * @param dir Specifies which corner is next.  can be 0 or 1.
	 */
	protected int pickEndIndex(int cornerS, int dir)
	{
		int cornerIndex = split.get(cornerS);
		int endIndex = split.get(UtilShapePolygon.addOffset(cornerIndex, dir, split.size));

		int distance = UtilShapePolygon.subtract(cornerIndex, endIndex, contour.size());

		if( distance > 0 ) {
			distance = Math.min(distance,pixelsAway);
		} else {
			distance = Math.max(dir,-pixelsAway);
		}

		return UtilShapePolygon.addOffset(cornerIndex,distance,contour.size());
	}

	public int getPixelsAway() {
		return pixelsAway;
	}

	public void setPixelsAway(int pixelsAway) {
		this.pixelsAway = pixelsAway;
	}

	public boolean isPositiveRight() {
		return positiveRight;
	}

	public void setPositiveRight(boolean positiveRight) {
		this.positiveRight = positiveRight;
	}
}
