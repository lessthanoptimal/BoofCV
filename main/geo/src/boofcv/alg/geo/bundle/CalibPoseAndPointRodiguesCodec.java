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

package boofcv.alg.geo.bundle;

import boofcv.numerics.fitting.modelset.ModelCodec;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues;
import org.ejml.alg.dense.decomposition.DecompositionFactory;
import org.ejml.alg.dense.decomposition.SingularValueDecomposition;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * Parametrization for Bundle Adjustment with known calibration where the
 * rotation matrix is encoded using {@link Rodrigues} coordinates.
 * 
 * @author Peter Abeles
 */
public class CalibPoseAndPointRodiguesCodec
	implements ModelCodec<CalibratedPoseAndPoint>
{
	// number of camera views
	int numViews;
	// number of points in world coordinates
	int numPoints;
	// number of views with unknown extrinsic parameters
	int numViewsUnknown;

	// storage
	Rodrigues rotation = new Rodrigues();
	DenseMatrix64F R = new DenseMatrix64F(3,3);

	// used to make sure the rotation matrix is in SO(3)
	SingularValueDecomposition<DenseMatrix64F> svd =
			DecompositionFactory.svd(3,3,true,true,false);

	/**
	 * Specify the number of views and points it can expected
	 */
	public void configure( int numViews , int numPoints , int numViewsUnknown ) {
		this.numViews = numViews;
		this.numPoints = numPoints;
		this.numViewsUnknown = numViewsUnknown;
	}

	@Override
	public void decode(double[] param, CalibratedPoseAndPoint outputModel) {

		int paramIndex = 0;

		// first decode the transformation
		for( int i = 0; i < numViews; i++ ) {
			// don't decode if it is already known
			if( outputModel.isViewKnown(i) )
				continue;

			Se3_F64 se = outputModel.getWorldToCamera(i);

			rotation.setParamVector(param[paramIndex++],param[paramIndex++],param[paramIndex++]);

			RotationMatrixGenerator.rodriguesToMatrix(rotation,se.getR());

			Vector3D_F64 T = se.getT();
			T.x = param[paramIndex++];
			T.y = param[paramIndex++];
			T.z = param[paramIndex++];
		}
		
		// now decode the points
		for( int i = 0; i < numPoints; i++ ) {
			Point3D_F64 p = outputModel.getPoint(i);
			p.x = param[paramIndex++];
			p.y = param[paramIndex++];
			p.z = param[paramIndex++];
		}
	}

	@Override
	public void encode(CalibratedPoseAndPoint model, double[] param) {
		int paramIndex = 0;

		// first decode the transformation
		for( int i = 0; i < numViews; i++ ) {
			// don't encode if it is already known
			if( model.isViewKnown(i) )
				continue;

			Se3_F64 se = model.getWorldToCamera(i);

			// force the "rotation matrix" to be an exact rotation matrix
			// otherwise Rodrigues will have issues with the noise
			if( !svd.decompose(se.getR()) )
				throw new RuntimeException("SVD failed");

			DenseMatrix64F U = svd.getU(false);
			DenseMatrix64F V = svd.getV(false);

			CommonOps.multTransB(U,V,R);

			// extract Rodrigues coordinates
			RotationMatrixGenerator.matrixToRodrigues(R,rotation);

			param[paramIndex++] = rotation.unitAxisRotation.x*rotation.theta;
			param[paramIndex++] = rotation.unitAxisRotation.y*rotation.theta;
			param[paramIndex++] = rotation.unitAxisRotation.z*rotation.theta;

			Vector3D_F64 T = se.getT();

			param[paramIndex++] = T.x;
			param[paramIndex++] = T.y;
			param[paramIndex++] = T.z;
		}

		for( int i = 0; i < numPoints; i++ ) {
			Point3D_F64 p = model.getPoint(i);

			param[paramIndex++] = p.x;
			param[paramIndex++] = p.y;
			param[paramIndex++] = p.z;
		}
	}

	@Override
	public int getParamLength() {
		return numViewsUnknown*6 + numPoints*3;
	}
}
