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

package boofcv.struct.sfm;

import georegression.struct.point.Point2D_F64;

/**
 * Storage for a point on a 2D plane in the key-frame and the observed normalized image coordinate in the current frame
 *
 * @author Peter Abeles
 */
public class PlanePtPixel {

	/**
	 * Location of key-frame on the 2D plane
	 */
	public Point2D_F64 planeKey = new Point2D_F64();
	/**
	 * Observed pixel location in normalized image coordinates of track in current-frame
	 */
	public Point2D_F64 normalizedCurr = new Point2D_F64();

	public PlanePtPixel(Point2D_F64 planeKey, Point2D_F64 normalizedCurr) {
		this.planeKey = planeKey;
		this.normalizedCurr = normalizedCurr;
	}

	public PlanePtPixel() {
	}

	public Point2D_F64 getPlaneKey() {
		return planeKey;
	}

	public Point2D_F64 getNormalizedCurr() {
		return normalizedCurr;
	}
}
