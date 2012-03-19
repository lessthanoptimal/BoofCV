/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.pose;

import boofcv.numerics.fitting.modelset.ModelCodec;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues;
import org.ejml.alg.dense.decomposition.DecompositionFactory;
import org.ejml.alg.dense.decomposition.SingularValueDecomposition;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * Encoding an decoding a rotation and translation where the rotation is encoded as a 3-vector
 * Rodrigues coordinate.
 *
 * @author Peter Abeles
 */
public class PoseRodriguesCodec implements ModelCodec<Se3_F64> {

	// used to make sure the rotation matrix is in SO(3)
	SingularValueDecomposition<DenseMatrix64F> svd =
			DecompositionFactory.svd(3, 3, true, true, false);

	// storage for rotation matrix
	DenseMatrix64F R = new DenseMatrix64F(3,3);

	Rodrigues rotation = new Rodrigues();

	@Override
	public void decode(double[] param, Se3_F64 outputModel) {
		rotation.setParamVector(param[0],param[1],param[2]);

		RotationMatrixGenerator.rodriguesToMatrix(rotation, outputModel.getR());

		Vector3D_F64 T = outputModel.getT();
		T.x = param[3];
		T.y = param[4];
		T.z = param[5];
	}

	@Override
	public void encode(Se3_F64 se, double[] param) {

		// force the "rotation matrix" to be an exact rotation matrix
		// otherwise Rodrigues will have issues with the noise
		if( !svd.decompose(se.getR()) )
			throw new RuntimeException("SVD failed");

		DenseMatrix64F U = svd.getU(false);
		DenseMatrix64F V = svd.getV(false);

		CommonOps.multTransB(U, V, R);

		// extract Rodrigues coordinates
		RotationMatrixGenerator.matrixToRodrigues(R,rotation);

		param[0] = rotation.unitAxisRotation.x*rotation.theta;
		param[1] = rotation.unitAxisRotation.y*rotation.theta;
		param[2] = rotation.unitAxisRotation.z*rotation.theta;

		Vector3D_F64 T = se.getT();

		param[3] = T.x;
		param[4] = T.y;
		param[5] = T.z;
	}

	@Override
	public int getParamLength() {
		return 6;
	}
}
