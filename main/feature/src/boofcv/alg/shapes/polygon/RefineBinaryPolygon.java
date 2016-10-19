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

import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * Abstract interface for refining a polygon detected inside of {@link BinaryPolygonDetector}.
 *
 * @author Peter Abeles
 */
public interface RefineBinaryPolygon<T extends ImageGray> {

	/**
	 * Sets the input image
	 */
	void setImage(T image);

	/**
	 * Specifies lens distortion
	 */
	void setLensDistortion(int width , int height ,
						   PixelTransform2_F32 distToUndist , PixelTransform2_F32 undistToDist );


	/**
	 * Clears the previously set lens distortion
	 */
	void clearLensDistortion();

	/**
	 * Refines the initial polygon
	 *
	 * @param input 2D polygon version of contour polygon
	 * @param contour List of pixels in the contour
	 * @param splits Indexes in the contour where the initial polygon was found
	 * @param output Storage for the refined polygon
	 * @return true if successful or false if it failed
	 */
	boolean refine(Polygon2D_F64 input, List<Point2D_I32> contour , GrowQueue_I32 splits, Polygon2D_F64 output);
}
