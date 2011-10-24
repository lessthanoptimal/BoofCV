/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.benchmark.feature.homography;

import boofcv.struct.feature.TupleDesc_F64;
import georegression.struct.point.Point2D_F64;


/**
 * @author Peter Abeles
 */
public class FeatureInfo {
	Point2D_F64 location;
	double orientation;
	TupleDesc_F64 description;

	public FeatureInfo( int descLength ) {
		location = new Point2D_F64();
		description = new TupleDesc_F64(descLength);
	}

	public Point2D_F64 getLocation() {
		return location;
	}

	public double getOrientation() {
		return orientation;
	}

	public void setOrientation(double orientation) {
		this.orientation = orientation;
	}

	public void setLocation(Point2D_F64 location) {
		this.location = location;
	}

	public TupleDesc_F64 getDescription() {
		return description;
	}

	public void setDescription(TupleDesc_F64 description) {
		this.description = description;
	}
}
