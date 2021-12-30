/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.bundle;

import boofcv.struct.distort.Point2Transform2_F64;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import lombok.Setter;

/**
 * Wrapper around {@link BundleAdjustmentCamera} for {@link boofcv.struct.distort.Point2Transform2_F64}
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class Point2Transform2BundleCamera implements Point2Transform2_F64 {
	@Getter @Setter BundleAdjustmentCamera model;

	public Point2Transform2BundleCamera( BundleAdjustmentCamera model ) {
		this.model = model;
	}

	public Point2Transform2BundleCamera() {}

	@Override public void compute( double x, double y, Point2D_F64 out ) {
		// Assume the input is in normalized image coordinates, which corresponds to (x,y,1.0)
		model.project(x, y, 1.0, out);
	}

	@Override public Point2Transform2_F64 copyConcurrent() {
		throw new RuntimeException("Not sure if this is possible. Check model and update code");
	}
}
