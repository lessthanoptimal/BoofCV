/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.bundle;

import georegression.struct.point.Point2D_F64;

/**
 * Specifies which feature produced what observation in a particular view.  The individual feature is referenced
 * by its list index.  The observation itself is specified using a {@link Point2D_F64}, which can be in either
 * pixel or normalized image coordinates, depending on the application.
 *
 * @author Peter Abeles
 */
public class PointIndexObservation {

	/** Index of the feature that is observed */
	public int pointIndex;
	/** The observation of the feature */
	public Point2D_F64 obs;

	public void set( int pointIndex , Point2D_F64 obs ) {
		this.pointIndex = pointIndex;
		this.obs = obs;
	}

	public void reset() {
		pointIndex = -1;
		obs = null;
	}

	public Point2D_F64 getObservation() {
		return obs;
	}

	public int getPointIndex() {
		return pointIndex;
	}
}
