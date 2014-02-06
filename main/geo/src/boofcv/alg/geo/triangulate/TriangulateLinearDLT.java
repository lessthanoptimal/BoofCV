/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.interfaces.decomposition.SingularValueDecomposition;
import org.ejml.ops.SingularOps;

import java.util.List;

/**
 * <p>
 * Triangulates the location of a 3D point given two or more views of the point using the
 * Discrete Linear Transform (DLT).
 * </p>
 *
 * <p>
 * [1] Page 312 in R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </p>
 *
 * @author Peter Abeles
 */
public class TriangulateLinearDLT {

	SingularValueDecomposition<DenseMatrix64F> svd = DecompositionFactory.svd(4, 4,true,true,false);
	DenseMatrix64F v = new DenseMatrix64F(4,1);
	DenseMatrix64F A = new DenseMatrix64F(4,4);

	/**
	 * <p>
	 * Given N observations of the same point from two views and a known motion between the
	 * two views, triangulate the point's position in camera 'b' reference frame.
	 * </p>
	 * <p>
	 * Modification of [1] to be less generic and use calibrated cameras.
	 * </p>
	 *
	 * @param observations Observation in each view in normalized coordinates. Not modified.
	 * @param worldToView Transformations from world to the view.  Not modified.
	 * @param found Output, the found 3D position of the point.  Modified.
	 */
	public void triangulate( List<Point2D_F64> observations ,
							 List<Se3_F64> worldToView ,
							 Point3D_F64 found ) {
		if( observations.size() != worldToView.size() )
			throw new IllegalArgumentException("Number of observations must match the number of motions");
		
		final int N = worldToView.size();
		
		A.reshape(2*N,4,false);
		
		int index = 0;
		
		for( int i = 0; i < N; i++ ) {
			index = addView(worldToView.get(i),observations.get(i),index);
		}

		if( !svd.decompose(A) )
			throw new RuntimeException("SVD failed!?!?");

		SingularOps.nullVector(svd,true,v);

		double w = v.get(3);
		found.x = v.get(0)/w;
		found.y = v.get(1)/w;
		found.z = v.get(2)/w;
	}
	
	/**
	 * <p>
	 * Given two observations of the same point from two views and a known motion between the
	 * two views, triangulate the point's position in camera 'b' reference frame.
	 * </p>
	 * <p>
	 * Modification of [1] to be less generic and use calibrated cameras.
	 * </p>
	 *
	 * @param a Observation 'a' in normalized coordinates. Not modified.
	 * @param b Observation 'b' in normalized coordinates. Not modified.
	 * @param fromAtoB Transformation from camera view 'a' to 'b'  Not modified.
	 * @param foundInA Output, the found 3D position of the point.  Modified.
	 */
	public void triangulate( Point2D_F64 a , Point2D_F64 b ,
							 Se3_F64 fromAtoB ,
							 Point3D_F64 foundInA ) {
		A.reshape(4, 4, false);

		int index = addView(fromAtoB,b,0);

		// third row
		A.data[index++] = -1;
		A.data[index++] = 0;
		A.data[index++] = a.x;
		A.data[index++] = 0;
		
		// fourth row
		A.data[index++] = 0;
		A.data[index++] = -1;
		A.data[index++] = a.y;
		A.data[index  ] = 0;
		
		if( !svd.decompose(A) )
			throw new RuntimeException("SVD failed!?!?");

		SingularOps.nullVector(svd,true,v);
		
		double w = v.get(3);
		foundInA.x = v.get(0)/w;
		foundInA.y = v.get(1)/w;
		foundInA.z = v.get(2)/w;
	}
	
	private int addView( Se3_F64 motion , Point2D_F64 a , int index ) {

		DenseMatrix64F R = motion.getR();
		Vector3D_F64 T = motion.getT();
		
		double r11 = R.data[0], r12 = R.data[1], r13 = R.data[2];
		double r21 = R.data[3], r22 = R.data[4], r23 = R.data[5];
		double r31 = R.data[6], r32 = R.data[7], r33 = R.data[8];

		// no normalization of observations are needed since they are in normalized coordinates
		
		// first row
		A.data[index++] = a.x*r31-r11;
		A.data[index++] = a.x*r32-r12;
		A.data[index++] = a.x*r33-r13;
		A.data[index++] = a.x*T.z-T.x;

		// second row
		A.data[index++] = a.y*r31-r21;
		A.data[index++] = a.y*r32-r22;
		A.data[index++] = a.y*r33-r23;
		A.data[index++] = a.y*T.z-T.y;
		
		return index;
	}
}
