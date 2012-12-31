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

package boofcv.abst.calib;

import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;

import java.util.List;

/**
 * Interface for extracting points from a planar calibration grid.
 *
 * @author Peter Abeles
 */
public interface PlanarCalibrationDetector {

	/**
	 * Image processing for calibration target detection
	 *
	 * @param input Gray scale image containing calibration target
	 * @return true if target was detected and false if not
	 */
	public boolean process( ImageFloat32 input );

	/**
	 * Returns the set of detected points.  Each time this function is invoked a new instance
	 * of the list and points is returned.  No data reuse here.
	 *
	 * @return List of detected points in row major grid order.
	 */
	public List<Point2D_F64> getPoints();
}
