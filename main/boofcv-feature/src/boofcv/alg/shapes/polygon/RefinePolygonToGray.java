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

import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.ImageGray;
import georegression.struct.shapes.Polygon2D_F64;

/**
 * Refines a polygon using the gray scale image. This results in a more accurate fit than just the contour alone
 * will provide and removes the shift that fitting to contour injects.
 *
 * @author Peter Abeles
 */
public interface RefinePolygonToGray<T extends ImageGray<T>> {

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
	 * @param output Storage for the refined polygon
	 * @return true if successful or false if it failed
	 */
	boolean refine(Polygon2D_F64 input, Polygon2D_F64 output);
}
