/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.edge;

import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_I32;

import java.util.List;


/**
 * <p>
 * Detects sets of pixels which belong to the contour of objects.  What defines a
 * contour is not specified by this interface.
 * </p>
 *
 * <p>
 * NOTE: The ordering of pixels is not specified by this interface.  Some
 * implementations might be ordered while others are not.
 * </p>
 *
 * @author Peter Abeles
 */
public interface DetectEdgeContour<T extends ImageSingleBand> {

	/**
	 * Processes the input image and extract object contours.
	 *
	 * @param input Input image.
	 */
	public void process( T input );

	/**
	 * Returns found object contours.  Ordering of pixels is not specified.
	 * 
	 * @return List of pixel lists.  One set of pixels for each object.
	 */
	public List<List<Point2D_I32>> getContours();
}
