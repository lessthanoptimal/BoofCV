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

package boofcv.alg.sfm.d3;

import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

/**
 * Computes the acute angle between two observations. The acute angle can be used to determine if two
 * observations have a favorable geometry for triangulation.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ComputeObservationAcuteAngle {

	Se3_F64 fromAtoB;

	Point3D_F64 A = new Point3D_F64();
	Point3D_F64 B = new Point3D_F64();

	public void setFromAtoB( Se3_F64 fromAtoB ) {
		this.fromAtoB = fromAtoB;
	}

	public double computeAcuteAngle( Point2D_F64 a, Point2D_F64 b ) {

		A.setTo(a.x, a.y, 1);
		B.setTo(b.x, b.y, 1);

		GeometryMath_F64.mult(fromAtoB.getR(), A, A);

		double dot = GeometryMath_F64.dot(A, B);
		return Math.acos(dot/(A.norm()*B.norm()));
	}
}
