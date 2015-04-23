/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.robust;

import boofcv.alg.geo.robust.GenerateAffine2D;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.sfm.ScaleTranslateRotate2D;
import georegression.struct.affine.Affine2D_F64;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.interfaces.decomposition.SingularValueDecomposition;
import org.ejml.ops.CommonOps;

import java.util.List;

/**
 * Estimates a {@link boofcv.struct.sfm.ScaleTranslateRotate2D} from three 2D point correspondences.
 * The transform will take a point from p1 to p2. First the affine transform is found using the standard
 * linear equation.  Scale and translation are found by finding a bit fit solution using SVD.
 *
 * NOTE: The found solution is not going to be optimal due to the initial approximation using an affine transform.
 *
 * @author Peter Abeles
 */
public class GenerateScaleTranslateRotate2D
		implements ModelGenerator<ScaleTranslateRotate2D,AssociatedPair>
{
	private Affine2D_F64 affine = new Affine2D_F64();
	private GenerateAffine2D generateAffine = new GenerateAffine2D();

	private DenseMatrix64F R = new DenseMatrix64F(2,2);
	private DenseMatrix64F U = new DenseMatrix64F(2,2);
	private DenseMatrix64F V = new DenseMatrix64F(2,2);

	private SingularValueDecomposition<DenseMatrix64F> svd = DecompositionFactory.svd(2,2,true,true,true);

	@Override
	public boolean generate(List<AssociatedPair> dataSet, ScaleTranslateRotate2D output) {

		if( !generateAffine.generate(dataSet,affine) )
			return false;

		R.data[0] = affine.a11;
		R.data[1] = affine.a12;
		R.data[2] = affine.a21;
		R.data[3] = affine.a22;


		if( !svd.decompose(R) )
			return false;

		// determinant of a rotation matrix is 1.  Assume that scale makes it not one
		double sv[] = svd.getSingularValues();
		output.scale = (sv[0]+sv[1])/2.0;

		if( output.scale < 0 )
			throw new RuntimeException("Handle this case");

		svd.getU(U, false);
		svd.getV(V, false);

		CommonOps.multTransB(U, V, R);

		if( CommonOps.det(R) < 0 ) {
			// There are situations where R might not have a determinant of one and is instead
			// a reflection is returned
			for( int i = 0; i < 2; i++ )
				V.set( i, 1, -V.get( i, 1 ) );
			CommonOps.mult(U, V, R);
		}

		// theta = atan2( sin(theta) , cos(theta) )
		output.theta = Math.atan2( -R.data[1], R.data[0] );
		output.transX = affine.tx;
		output.transY = affine.ty;

		return true;
	}


	@Override
	public int getMinimumPoints() {
		return 3;
	}
}
