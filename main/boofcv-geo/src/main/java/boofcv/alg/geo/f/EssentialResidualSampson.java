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

package boofcv.alg.geo.f;

import boofcv.alg.geo.ModelObservationResidual;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

/**
 * <p>
 * Computes the Sampson distance residual for a set of observations given an esesntial matrix. For use
 * in least-squares non-linear optimization algorithms. Error is computed in pixels.
 * </p>
 *
 * <p>0 = x2<sup>T</sup>*F*x1<br></p>
 * <p>E=K2'*F*K1</p>
 * <p>F=inv(K2')*E*inv(K1)</p>
 * <p>0=(K2*n2)'inv(K2)'*E*inv(K1)*(K1*n2), where n1 and n2 are normalized image coordinates</p>
 * <p>
 * Page 287 in: R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class EssentialResidualSampson implements ModelObservationResidual<DMatrixRMaj, AssociatedPair> {
	DMatrixRMaj E;
	DMatrixRMaj K2E = new DMatrixRMaj(3, 3);
	DMatrixRMaj EK1 = new DMatrixRMaj(3, 3);
	Point3D_F64 temp = new Point3D_F64();

	DMatrixRMaj K1_inv = new DMatrixRMaj(3, 3);
	DMatrixRMaj K2_inv = new DMatrixRMaj(3, 3);

	public void setCalibration1( CameraPinhole pinhole ) {
		DMatrixRMaj K = new DMatrixRMaj(3, 3);
		PerspectiveOps.pinholeToMatrix(pinhole, K);
		CommonOps_DDRM.invert(K, K1_inv);
	}

	public void setCalibration2( CameraPinhole pinhole ) {
		DMatrixRMaj K = new DMatrixRMaj(3, 3);
		PerspectiveOps.pinholeToMatrix(pinhole, K);
		CommonOps_DDRM.invert(K, K2_inv);
	}

	@Override
	public void setModel( DMatrixRMaj E ) {
		this.E = E;
		CommonOps_DDRM.multTransA(K2_inv, E, K2E);
		CommonOps_DDRM.mult(E, K1_inv, EK1);
	}

	@Override
	public double computeResidual( AssociatedPair observation ) {
		double bottom = 0;

		GeometryMath_F64.mult(K2E, observation.p1, temp);
		bottom += temp.x*temp.x + temp.y*temp.y;

		GeometryMath_F64.multTran(EK1, observation.p2, temp);
		bottom += temp.x*temp.x + temp.y*temp.y;


		if (bottom == 0) {
			return Double.MAX_VALUE;
		} else {
			GeometryMath_F64.multTran(E, observation.p2, temp);

			return (temp.x*observation.p1.x + temp.y*observation.p1.y + temp.z)/bottom;
		}
	}
}
