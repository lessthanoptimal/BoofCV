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

package boofcv.alg.geo.pose;

import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.interfaces.decomposition.SingularValueDecomposition;
import org.ejml.ops.CommonOps;
import org.ejml.ops.SingularOps;

import java.util.List;


/**
 * <p>
 * Estimates the camera motion using linear algebra given a set of N associated point observations and the
 * depth (z-coordinate) of each object, where N &ge; 6.  Note this is similar to, but not exactly the PnP problem.
 * </p>
 *
 * <p>
 * Output from this class is a rotation and translation that converts a point from the first to second
 * camera's reference frame:</p>
 * {@code X' = R*X+T}<br>
 * where R is a rotation matrix, T is a translation matrix, X is a coordinate in 1st reference frame, and X'
 * in the second.
 *
 * <p>
 * This approach is a modified version of the approach discussed in [1].  It is derived by using
 * bilinear and trilinear constraints, as is discussed in Section 8.3. It has been modified to remove
 * redundant rows and so that the computed rotation matrix is row major.  The solution is derived from
 * the equations below and by computing the null space from the resulting matrix:<br>
 * cross(x<sub>2</sub>)*(R*x<sub>1</sub>) + cross(x<sub>2</sub>)*T/&lambda;<sub>i</sub>=0<br>
 * where cross(x) is the cross product matrix of X,  x<sub>i</sub> is the calibrated image pixel coordinate in the
 * i<sup>th</sup> image, R is rotation and T translation.
 * </p>
 *
 * <p>
 * [1] "An Invitation to 3-D Vision, From Images to Geometric Models" 1st Ed. 2004. Springer.
 * </p>
 *
 * @author Peter Abeles
 */
public class PoseFromPairLinear6 {

	// The rank 11 linear system
	private DenseMatrix64F A = new DenseMatrix64F(1,12);

	// used to decompose and compute the null space of A
	private SingularValueDecomposition<DenseMatrix64F> svd = DecompositionFactory.svd(0, 0, true, true, false);

	// parameterized rotation and translation
	private DenseMatrix64F x = new DenseMatrix64F(12,1);

	// the found motion
	private Se3_F64 motion = new Se3_F64();
    
	/**
	 * Computes the transformation between two camera frames using a linear equation.  Both the
	 * observed feature locations in each camera image and the depth (z-coordinate) of each feature
	 * must be known.  Feature locations are in calibrated image coordinates.
	 *
	 * @param observations List of observations on the image plane in calibrated coordinates.
	 * @param locations List of object locations.  One for each observation pair.
	 */
	public void process( List<AssociatedPair> observations , List<Point3D_F64> locations ) {
		if( observations.size() != locations.size() )
			throw new IllegalArgumentException("Number of observations and locations must match.");

		if( observations.size() < 6 )
			throw new IllegalArgumentException("At least (if not more than) six points are required.");

		setupA(observations,locations);

		computeTransform(A);

		// make sure R is really a rotation matrix
		massageResults();
	}

	/**
	 * Estimated motion
	 *
	 * @return rigid body motion
	 */
	public Se3_F64 getMotion() {
		return motion;
	}

	/**
	 * Matrix used internally.
	 */
	protected DenseMatrix64F getA() {
		return A;
	}

	private void setupA(List<AssociatedPair> observations , List<Point3D_F64> locations) {
		A.reshape(2*observations.size(),12,false);

		for( int i = 0; i < observations.size(); i++ ) {
			AssociatedPair p = observations.get(i);
			Point3D_F64 loc = locations.get(i);

			Point2D_F64 pt1 = p.p1;
			Point2D_F64 pt2 = p.p2;

			// normalize the points
			int w=i*2;

			double alpha = 1.0/loc.z;

			A.set( w , 3  , -pt1.x);
			A.set( w , 4  , -pt1.y);
			A.set( w , 5  , -1);
			A.set( w , 6  , pt2.y*pt1.x);
			A.set( w , 7  , pt2.y*pt1.y);
			A.set( w , 8  , pt2.y);
			A.set( w , 9  , 0);
			A.set( w , 10 , -alpha);
			A.set( w , 11 , alpha*pt2.y);

			w++;

			A.set( w , 0  , pt1.x);
			A.set( w , 1  , pt1.y);
			A.set( w , 2  , 1);
			A.set( w , 6  , -pt2.x*pt1.x);
			A.set( w , 7  , -pt2.x*pt1.y);
			A.set( w , 8  , -pt2.x);
			A.set( w , 9  , alpha);
			A.set( w , 10 , 0);
			A.set( w , 11 , -alpha*pt2.x);
		}
	}

	/**
	 * Computes the null space of A and extracts the transform.
	 */
	private void computeTransform( DenseMatrix64F A ) {
		if( !svd.decompose(A) )
			throw new RuntimeException("SVD failed?");

		SingularOps.nullVector(svd,true,x);

		DenseMatrix64F R = motion.getR();
		Vector3D_F64 T = motion.getT();

		// extract the results
		System.arraycopy(x.data,0,R.data,0,9);

		T.x = x.data[9];
		T.y = x.data[10];
		T.z = x.data[11];
	}

	/**
	 * <p>
	 * Since the linear solution is probably not an exact rotation matrix, this code finds the best
	 * approximation.
	 * </p>
	 *
	 * See page 280 of [1]
	 */
	private void massageResults() {
		DenseMatrix64F R = motion.getR();
		Vector3D_F64 T = motion.getT();

		if( !svd.decompose(R))
			throw new RuntimeException("SVD Failed");

		CommonOps.multTransB(svd.getU(null,false),svd.getV(null,false),R);

		// determinant should be +1
		double det = CommonOps.det(R);

		if( det < 0 )
			CommonOps.scale(-1,R);

		// compute the determinant of the singular matrix
		double b = 1.0;
		double s[] = svd.getSingularValues();

		for( int i = 0; i < svd.numberOfSingularValues(); i++ ) {
			b *= s[i];
		}

		b = Math.signum(det)/Math.pow(b,1.0/3.0);

		GeometryMath_F64.scale(T,b);
	}

}
