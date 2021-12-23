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

package boofcv.alg.geo.trifocal;

import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.f.EpipolarMinimizeGeometricError;
import boofcv.struct.geo.TrifocalTensor;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ejml.data.DMatrixRMaj;

/**
 * Given a trifocal tensor and a feature observed in two of the views, predict where it will
 * appear in the third view.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class TrifocalTransfer {

	TrifocalTensor tensor;

	// extracts fundamental matrices
	TrifocalExtractGeometries extract = new TrifocalExtractGeometries();
	// extracted fundamental matrices
	DMatrixRMaj F21 = new DMatrixRMaj(3, 3);
	DMatrixRMaj F31 = new DMatrixRMaj(3, 3);
	// used to force observations to lie on the epipolar lines
	EpipolarMinimizeGeometricError adjuster = new EpipolarMinimizeGeometricError();

	// work space
	Point2D_F64 pa = new Point2D_F64();
	Point2D_F64 pb = new Point2D_F64();

	Point3D_F64 la = new Point3D_F64();
	Vector3D_F64 l = new Vector3D_F64();

	/**
	 * Specify the trifocaltensor
	 *
	 * @param tensor tensor
	 */
	public void setTrifocal( TrifocalTensor tensor ) {
		this.tensor = tensor;
		extract.setTensor(tensor);
		extract.extractFundmental(F21, F31);
	}

	/**
	 * Transfer a point to third view give its observed location in view one and two.
	 *
	 * @param x1 (Input) Observation in view 1. pixels.
	 * @param y1 (Input) Observation in view 1. pixels.
	 * @param x2 (Input) Observation in view 2. pixels.
	 * @param y2 (Input) Observation in view 2. pixels.
	 * @param p3 (Output) Estimated location in view 3.
	 */
	public void transfer_1_to_3( double x1, double y1,
								 double x2, double y2, Point3D_F64 p3 ) {
		// Adjust the observations so that they lie on the epipolar lines exactly
		adjuster.process(F21, x1, y1, x2, y2, pa, pb);

		GeometryMath_F64.mult(F21, pa, la);

		// line through pb and perpendicular to la
		l.x = la.y;
		l.y = -la.x;
		l.z = -pb.x*la.y + pb.y*la.x;

		MultiViewOps.transfer_1_to_3(tensor, pa, l, p3);
	}

	/**
	 * Transfer a point to third view give its observed location in view one and three.
	 *
	 * @param x1 (Input) Observation in view 1. pixels.
	 * @param y1 (Input) Observation in view 1. pixels.
	 * @param x3 (Input) Observation in view 3. pixels.
	 * @param y3 (Input) Observation in view 3. pixels.
	 * @param p2 (Output) Estimated location in view 2.
	 */
	public void transfer_1_to_2( double x1, double y1,
								 double x3, double y3, Point3D_F64 p2 ) {
		// Adjust the observations so that they lie on the epipolar lines exactly
		adjuster.process(F31, x1, y1, x3, y3, pa, pb);

		GeometryMath_F64.multTran(F31, pa, la);

		// line through pb and perpendicular to la
		l.x = la.y;
		l.y = -la.x;
		l.z = -pb.x*la.y + pb.y*la.x;

		MultiViewOps.transfer_1_to_2(tensor, pa, l, p2);
	}
}
