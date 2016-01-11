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

import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * Interface which allows low level customization of {@link BinaryPolygonDetector}
 *
 * @author Peter Abeles
 */
public interface PolygonHelper {

	/**
	 * Specifies width and height of the image being processed
	 *
	 * @param width Image width in pixels
	 * @param height Image height in pixels
	 */
	void setImageShape( int width , int height );

	/**
	 * This function is invoked just before a polygon's edge is optimized.  It's pixel precise at this point.
	 */
	void adjustBeforeOptimize( Polygon2D_F64 polygon );

	/**
	 * User defined filter to accept/reject or modify the contour of a shape.  Called at most twice. First
	 * with distorted pixels and after distortion has been removed.
	 *
	 * @param contour External contour around a shape.  Can be modified
	 * @param touchesBorder true if the contour touches the image border or false if it doesnt
	 * @param distorted True if pixels are distorted or false for undistorted pixel coordinates
	 * @return true to keep the contour for further processing or false to reject it
	 */
	boolean filterContour( List<Point2D_I32> contour , boolean touchesBorder , boolean distorted );

	/**
	 * Function which allows a custom filter function to be used on the pixel level precise polygon
	 *
	 * @param externalUndist External contour around a shape in undistorted pixel coordinates.  Can be modified
	 * @param externalDist External contour around a shape in distorted pixel coordinates.  Modifications ignored.
	 * @param splits Indexes in the contour where the polygon has a vertex
	 * @param touchesBorder true if the contour touches the image border or false if it doesnt
	 * @return true to keep the contour for further processing or false to reject it
	 */
	boolean filterPixelPolygon(List<Point2D_I32> externalUndist , List<Point2D_I32> externalDist,  GrowQueue_I32 splits, boolean touchesBorder);
}
