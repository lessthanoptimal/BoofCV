/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort.kanbra;

import boofcv.struct.calib.CameraKannalaBrandt;
import boofcv.struct.distort.Point2Transform3_F64;
import georegression.struct.point.Point3D_F64;

/**
 * Backwards project from a distorted 2D pixel to 3D unit sphere coordinate using the {@link CameraKannalaBrandt} model.
 *
 * @author Peter Abeles
 */
public class KannalaBrandtPtoS_F64 implements Point2Transform3_F64 {

	public KannalaBrandtPtoS_F64( CameraKannalaBrandt model ) {

	}

	@Override
	public void compute(double x, double y, Point3D_F64 out) {

	}

	@Override
	public Point2Transform3_F64 copyConcurrent() {
		return null;
	}
}
