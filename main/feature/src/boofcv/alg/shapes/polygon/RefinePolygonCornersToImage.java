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

package boofcv.alg.shapes.polygon;

import boofcv.alg.shapes.corner.RefineCornerLinesToImage;
import boofcv.alg.shapes.edge.SnapToLineEdge;
import boofcv.misc.CircularIndex;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * Given a contour and the index of corner points it will fit the corners individually to sub-pixel precision
 * by locally fitting line segments near by each corner.  The length of each line segment is specified
 * by the number of pixels away it will traverse the contour.  It makes sure that the end point does not go
 * past the next corner.
 *
 * @author Peter Abeles
 */
public class RefinePolygonCornersToImage<T extends ImageGray> implements RefineBinaryPolygon<T> {

	// number of pixels along the contour away should the end point be from the corner
	private int pixelsAway;

	// used to refine individual corners
	protected RefineCornerLinesToImage<T> refineCorner;

	// storage for corner points
	protected Point2D_F64 cornerPt = new Point2D_F64();
	protected Point2D_F64 leftPt = new Point2D_F64();
	protected Point2D_F64 rightPt = new Point2D_F64();

	// reference ti input contour and corner indexes
	protected List<Point2D_I32> contour;
	protected GrowQueue_I32 splits;

	/**
	 * Configures all parameters of the detector
	 *
	 * @param endPointDistance How many indexes away in the contour list should end points be.
	 * @param cornerOffset pixels this close to the corner will be ignored. Try 2
	 * @param maxLineSamples Number of points along the line which will be sampled.  try 10
	 * @param sampleRadius How far away from the line will it sample pixels.  &ge; 1
	 * @param maxIterations Maximum number of iterations it will perform.  Try 10
	 * @param convergeTolPixels When the corner changes less than this amount it will stop iterating. Try 1e-5
	 * @param maxCornerChangePixels maximum change in corner location allowed from previous iteration in pixels.  Try 2.0
	 * @param imageType Type of input image.
	 */
	public RefinePolygonCornersToImage(int endPointDistance,
									   double cornerOffset, int maxLineSamples, int sampleRadius,
									   int maxIterations, double convergeTolPixels,double maxCornerChangePixels,
									   Class<T> imageType) {
		this.pixelsAway = endPointDistance;
		this.refineCorner = new RefineCornerLinesToImage<>(cornerOffset, maxLineSamples, sampleRadius, maxIterations,
				convergeTolPixels, maxCornerChangePixels, imageType);
	}

	/**
	 * Provides reasonable default values for all parameters that it can
	 *
	 * @param imageType Type of input image.
	 */
	public RefinePolygonCornersToImage(Class<T> imageType) {
		this(12,2,10,1,10,1e-6,2,imageType);
	}

	/**
	 * Sets the input image
	 *
	 * @param input Input image-
	 */
	@Override
	public void setImage( T input ) {
		refineCorner.setImage(input);
	}

	@Override
	public void setLensDistortion(int width, int height, PixelTransform2_F32 distToUndist, PixelTransform2_F32 undistToDist) {
		this.refineCorner.getSnapToEdge().setTransform(undistToDist);
	}

	@Override
	public void clearLensDistortion() {
		this.refineCorner.getSnapToEdge().setTransform(null);
	}

	/**
	 * Refines the corner estimates in the contour.
	 *
	 * @param contour (Input) Shape's contour
	 * @param splits (Input) index of corners in the contour
	 * @param refined (Output) Refined polygon with sub-pixel accurate corners
	 * @return Number of corners it successfully optimized.
	 */
	@Override
	public boolean refine(Polygon2D_F64 input, List<Point2D_I32> contour , GrowQueue_I32 splits, Polygon2D_F64 refined) {

		if( refined.size() != splits.size() )
			throw new IllegalArgumentException("Miss match between number of splits and polygon order");

		this.contour = contour;
		this.splits = splits;

		int numGood = 0;
		for (int cornerS = 0; cornerS < splits.size(); cornerS++) {
			int indexLeft  = pickEndIndex(cornerS,  1);
			int indexRight = pickEndIndex(cornerS, -1);

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
		return numGood >= refined.size()-1;
	}

	/**
	 * Selects the index to use as the end point.  Makes sure that the index does not go past
	 * the next corner.
	 *
	 * @param cornerS index of corner in split list
	 * @param dir Specifies which corner is next.  can be -1 or 1.
	 */
	protected int pickEndIndex(int cornerS, int dir)
	{
		int cornerIndex = splits.get(cornerS);
		int endIndex0 = splits.get(CircularIndex.addOffset(cornerS, dir, splits.size));
		int endIndex1 = splits.get(CircularIndex.addOffset(cornerS, -dir, splits.size));

		// splits being in increasing or decreasing order isn't specified.  This make sure the correct
		// point is selected
		int dist0 = CircularIndex.distanceP(cornerIndex, endIndex0, contour.size());
		int dist1 = CircularIndex.distanceP(cornerIndex, endIndex1, contour.size());

		if( dir == 1 != dist1 > dist0 )
			dir = -dir;

		if( dir < 0 ) {
			int distance = CircularIndex.distanceP(endIndex0, cornerIndex, contour.size());
			distance = Math.min(distance,pixelsAway);
			return CircularIndex.addOffset(cornerIndex, -distance, contour.size());
		} else {
			int distance = CircularIndex.distanceP(cornerIndex, endIndex0, contour.size());
			distance = Math.min(distance,pixelsAway);
			return CircularIndex.addOffset(cornerIndex, distance, contour.size());
		}
	}

	public int getPixelsAway() {
		return pixelsAway;
	}

	public void setPixelsAway(int pixelsAway) {
		this.pixelsAway = pixelsAway;
	}

	public SnapToLineEdge<T> getSnapToEdge() {
		return refineCorner.getSnapToEdge();
	}
}
