/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;
import org.ddogleg.fitting.modelset.ModelCodec;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.interfaces.decomposition.SingularValueDecomposition;
import org.ejml.ops.CommonOps;

/**
 * Encoding an decoding a rotation and translation where the rotation is encoded as a 3-vector
 * Rodrigues coordinate.
 *
 * @author Peter Abeles
 */
public class PnPRodriguesCodec implements ModelCodec<Se3_F64> {

	// used to make sure the rotation matrix is in SO(3)
	SingularValueDecomposition<DenseMatrix64F> svd = DecompositionFactory.svd(3, 3, true, true, false);

	// storage for rotation matrix
	DenseMatrix64F R = new DenseMatrix64F(3,3);

	Rodrigues_F64 rotation = new Rodrigues_F64();

	@Override
	public void decode(double[] input, Se3_F64 outputModel) {
		rotation.setParamVector(input[0],input[1],input[2]);

		ConvertRotation3D_F64.rodriguesToMatrix(rotation, outputModel.getR());

		Vector3D_F64 T = outputModel.getT();
		T.x = input[3];
		T.y = input[4];
		T.z = input[5];
	}

	@Override
	public void encode(Se3_F64 input, double[] output) {

		// force the "rotation matrix" to be an exact rotation matrix
		// otherwise Rodrigues will have issues with the noise
		if( !svd.decompose(input.getR()) )
			throw new RuntimeException("SVD failed");

		DenseMatrix64F U = svd.getU(null,false);
		DenseMatrix64F V = svd.getV(null,false);

		CommonOps.multTransB(U, V, R);

		// extract Rodrigues coordinates
		ConvertRotation3D_F64.matrixToRodrigues(R,rotation);

		output[0] = rotation.unitAxisRotation.x*rotation.theta;
		output[1] = rotation.unitAxisRotation.y*rotation.theta;
		output[2] = rotation.unitAxisRotation.z*rotation.theta;

		Vector3D_F64 T = input.getT();

		output[3] = T.x;
		output[4] = T.y;
		output[5] = T.z;
	}

	@Override
	public int getParamLength() {
		return 6;
	}
}
