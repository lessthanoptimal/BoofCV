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

package boofcv.abst.distort;

import boofcv.struct.distort.Point2Transform2_F32;
import georegression.struct.point.Point2D_F32;

import java.util.List;

/**
 * <p>Defines a {@link Point2Transform2_F32 mapping} which deforms the image based on the location of key points inside the image.
 * Image pixels will gravitate towards closer key points and their distorted location based on some distance metric.</p>
 *
 * <p>Concurrency: Do not use this class to render at the same time you're invoking any of its functions!  The
 * transformation will be in an unsafe state.</p>
 *
 * @author Peter Abeles
 */
public interface PointDeformKeyPoints extends Point2Transform2_F32 {

	/**
	 * Must be called first.  Specifies the shape of the image this transform will be applied to
	 * @param width image width
	 * @param height image height
	 */
	void setImageShape( int width , int height );

	/**
	 * Specifies the location of all the key points.  The initial distorted location of each keypoint will be set to
	 * the same location.
	 * @param locations Location of key points in undistorted image.  Local copy of points is saved.
	 */
	void setSource(List<Point2D_F32> locations );

	/**
	 * Specifies the distorted location of all the key points.
	 *
	 * @param locations location of key points in distorted image.   Local copy of points is saved.
	 */
	void setDestination(List<Point2D_F32> locations );

	/**
	 * Changes the source location of a single key point
	 * 
	 * @param which Index of key point to change
	 * @param x distorted x-coordinate
	 * @param y distorted y-coordinate
	 */
	void setSource(int which , float x , float y );

	/**
	 * Changes the destination location of a single key point
	 * @param which Index of key point to change
	 * @param x distorted x-coordinate
	 * @param y distorted y-coordinate
	 */
	void setDestination(int which , float x , float y );
}
