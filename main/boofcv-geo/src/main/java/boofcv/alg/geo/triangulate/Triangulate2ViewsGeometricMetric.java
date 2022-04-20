/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.triangulate;

import georegression.geometry.GeometryMath_F64;
import georegression.metric.ClosestPoint3D_F64;
import georegression.struct.line.LineParametric3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;

/**
 * Triangulates two views by finding the point which minimizes the distance between two rays.
 * Optimal in the geometric sense, but does not take in account stereo constraints.
 *
 * @author Peter Abeles
 */
public class Triangulate2ViewsGeometricMetric {

	// ray going from principle point to observation point
	final LineParametric3D_F64 rayA = new LineParametric3D_F64();
	final LineParametric3D_F64 rayB = new LineParametric3D_F64();

	/**
	 * <p>
	 * Given two observations of the same point from two views and a known motion between the
	 * two views, triangulate the point's 3D position in camera 'a' reference frame.
	 * </p>
	 *
	 * @param a Observation from camera view 'a' in normalized coordinates. Not modified.
	 * @param b Observation from camera view 'b' in normalized coordinates. Not modified.
	 * @param a_to_b Transformation from camera view 'a' to 'b'  Not modified.
	 * @param foundInA (Output) Found 3D position of the point in reference frame 'a'. Modified.
	 * @return true if they intersect at a point that's not at infinity
	 */
	public boolean triangulate( Point2D_F64 a, Point2D_F64 b, Se3_F64 a_to_b, Point3D_F64 foundInA ) {
		// b_to_a = R'*(X_b-T)=X_a
		// rayB should start at origin of B so X_b = 0
		// Thus, rayB.p = -R'*T

		// set camera B's principle point
		Vector3D_F64 t = a_to_b.getT();
		rayB.p.setTo(-t.x, -t.y, -t.z);

		// rotate observation in B into camera A's view
		GeometryMath_F64.multTran(a_to_b.getR(), rayB.p, rayB.p);
		GeometryMath_F64.multTran(a_to_b.getR(), b, rayB.slope);

		rayA.slope.setTo(a.x, a.y, 1);

		return null != ClosestPoint3D_F64.closestPoint(rayA, rayB, foundInA);
	}

	/**
	 * <p>
	 * Same as {@link #triangulate(Point2D_F64, Point2D_F64, Se3_F64, Point3D_F64)} but in homogenous coordinates
	 * and can handle points at infinity.
	 * </p>
	 *
	 * @param a Observation from camera view 'a' in normalized coordinates. Not modified.
	 * @param b Observation from camera view 'b' in normalized coordinates. Not modified.
	 * @param a_to_b Transformation from camera view 'a' to 'b'  Not modified.
	 * @param foundInA (Output) Found 3D position of the point in reference frame 'a'. Homogenous coordinates. Modified.
	 */
	public void triangulate( Point2D_F64 a, Point2D_F64 b, Se3_F64 a_to_b, Point4D_F64 foundInA ) {
		// b_to_a = R'*(X_b-T)=X_a
		// rayB should start at origin of B so X_b = 0
		// Thus, rayB.p = -R'*T

		// set camera B's principle point
		Vector3D_F64 t = a_to_b.getT();
		rayB.p.setTo(-t.x, -t.y, -t.z);

		// rotate observation in B into camera A's view
		GeometryMath_F64.multTran(a_to_b.getR(), rayB.p, rayB.p);
		GeometryMath_F64.multTran(a_to_b.getR(), b, rayB.slope);

		rayA.slope.setTo(a.x, a.y, 1);

		ClosestPoint3D_F64.closestPoint(rayA, rayB, foundInA);
	}

	/**
	 * <p>
	 * Triangulates two observations that are 3D pointing vectors and saves the results ad a 3D point in
	 * homogenous coordinates.
	 * </p>
	 *
	 * @param a Observation from camera view 'a' as a 3D pointing vector. Not modified.
	 * @param b Observation from camera view 'b' as a 3D pointing vector. Not modified.
	 * @param a_to_b Transformation from camera view 'a' to 'b'  Not modified.
	 * @param foundInA (Output) Found 3D position of the point in reference frame 'a'. Homogenous coordinates. Modified.
	 */
	public void triangulate( Point3D_F64 a, Point3D_F64 b, Se3_F64 a_to_b, Point4D_F64 foundInA ) {
		// b_to_a = R'*(X_b-T)=X_a
		// rayB should start at origin of B so X_b = 0
		// Thus, rayB.p = -R'*T

		// set camera B's principle point
		Vector3D_F64 t = a_to_b.getT();
		rayB.p.setTo(-t.x, -t.y, -t.z);

		// rotate observation in B into camera A's view
		GeometryMath_F64.multTran(a_to_b.getR(), rayB.p, rayB.p);
		GeometryMath_F64.multTran(a_to_b.getR(), b, rayB.slope);

		rayA.slope.setTo(a.x, a.y, a.z);

		ClosestPoint3D_F64.closestPoint(rayA, rayB, foundInA);
	}
}
