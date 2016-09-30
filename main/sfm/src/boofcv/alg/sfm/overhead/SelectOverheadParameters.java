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

package boofcv.alg.sfm.overhead;

import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;

/**
 * Give a camera's intrinsic and extrinsic parameters, selects a reasonable overhead view to render the image onto.  It
 * attempts to maximize viewing area.  The user can crop the height of the overhead view to reduce the amount of
 * unusable image space in the map.  This is particularly useful for cameras at an acute angle relative to the
 * ground plane.  Overhead cameras pointed downward should set it to 1.0
 *
 * @author Peter Abeles
 */
public class SelectOverheadParameters {

	// used to project pixels onto the plane
	CameraPlaneProjection proj = new CameraPlaneProjection();

	// the selected overhead map values
	int overheadWidth;
	int overheadHeight;
	double centerX;
	double centerY;

	// --- User specified parameters
	// size of cells on the plane
	double cellSize;
	// determines the minimum resolution
	double maxCellsPerPixel;
	// used to crop the views's height.  Specifies the fraction of the "optimal" height which is actually used.
	double viewHeightFraction;

	// local variables
	Point2D_F64 plane0 = new Point2D_F64();
	Point2D_F64 plane1 = new Point2D_F64();

	/**
	 * Configure algorithm.
	 *
	 * @param cellSize Size of cells in plane in world units
	 * @param maxCellsPerPixel Specifies minimum resolution of a region in overhead image. A pixel in the camera
	 *                         can't overlap more than this number of cells.   Higher values allow lower
	 *                         resolution regions.  Try 4.
	 * @param viewHeightFraction Reduce the view height by this fraction to avoid excessive unusable image space.  Set to
	 *                          1.0 to maximize the viewing area and any value less than one to crop it.
	 */
	public SelectOverheadParameters(double cellSize, double maxCellsPerPixel, double viewHeightFraction) {
		this.cellSize = cellSize;
		this.maxCellsPerPixel = maxCellsPerPixel;
		this.viewHeightFraction = viewHeightFraction;
	}

	/**
	 * Computes the view's characteristics
	 *
	 * @param intrinsic Intrinsic camera parameters
	 * @param planeToCamera Extrinsic camera parameters which specify the plane
	 * @return true if successful or false if it failed
	 */
	public boolean process(CameraPinholeRadial intrinsic , Se3_F64 planeToCamera )
	{
		proj.setPlaneToCamera(planeToCamera,true);
		proj.setIntrinsic(intrinsic);

		// find a bounding rectangle on the ground which is visible to the camera and at a high enough resolution
		double x0 = Double.MAX_VALUE;
		double y0 = Double.MAX_VALUE;
		double x1 = -Double.MAX_VALUE;
		double y1 = -Double.MAX_VALUE;

		for( int y = 0; y < intrinsic.height; y++ ) {
			for( int x = 0; x < intrinsic.width; x++ ) {
				if( !checkValidPixel(x,y) )
					continue;

				if( plane0.x < x0 )
					x0 = plane0.x;
				if( plane0.x > x1 )
					x1 = plane0.x;
				if( plane0.y < y0 )
					y0 = plane0.y;
				if( plane0.y > y1 )
					y1 = plane0.y;
			}
		}

		if( x0 == Double.MAX_VALUE )
			return false;

		// compute parameters with the intent of maximizing viewing area
		double mapWidth = x1-x0;
		double mapHeight = y1-y0;
		overheadWidth = (int)Math.floor(mapWidth/cellSize);
		overheadHeight = (int)Math.floor(mapHeight* viewHeightFraction /cellSize);
		centerX = -x0;
		centerY = -(y0+mapHeight*(1- viewHeightFraction)/2.0);

		return true;
	}

	/**
	 * Creates a new instance of the overhead view
	 */
	public <T extends ImageBase> OverheadView createOverhead( ImageType<T> imageType ) {
		OverheadView ret = new OverheadView();
		ret.image = imageType.createImage(overheadWidth,overheadHeight);
		ret.cellSize = cellSize;
		ret.centerX = centerX;
		ret.centerY = centerY;
		
		return ret;
	}

	private boolean checkValidPixel( int x , int y ) {
		if( !proj.pixelToPlane(x,y,plane0) )
			return false;
		if( !proj.pixelToPlane(x+1,y+1,plane1) )
			return false;

		double width = Math.abs(plane0.x - plane1.x);
		double height = Math.abs(plane0.y - plane1.y);

		if( width > maxCellsPerPixel *cellSize)
			return false;
		if( height > maxCellsPerPixel *cellSize)
			return false;

		return true;
	}

	public int getOverheadWidth() {
		return overheadWidth;
	}

	public int getOverheadHeight() {
		return overheadHeight;
	}

	public double getCenterX() {
		return centerX;
	}

	public double getCenterY() {
		return centerY;
	}
}
