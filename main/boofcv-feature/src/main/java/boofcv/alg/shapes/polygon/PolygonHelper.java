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

import boofcv.abst.shapes.polyline.PointsToPolyline;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.GrowQueue_B;

import java.util.List;

/**
 * Interface which allows low level customization of {@link DetectPolygonFromContour}
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
	 * @param undistorted Polygon in undistorted image pixels.
	 * @param distorted Polygon in distorted/original image pixels.
	 * @param touches Array of booleans indicating which corners touch the image border. Only valid if part of it touches
	 * @param touchesBorder true if the contour touches the image border or false if it doesnt
	 * @return true to keep the contour for further processing or false to reject it
	 */
	boolean filterPixelPolygon(Polygon2D_F64 undistorted , Polygon2D_F64 distorted, GrowQueue_B touches, boolean touchesBorder);

	/**
	 * Provide an oportunity to configure the polyline fit based on what's currently known
	 */
	void configureBeforePolyline(PointsToPolyline contourToPolyline, boolean touchesBorder);
}
