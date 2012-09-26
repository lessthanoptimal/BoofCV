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

package boofcv.alg.geo;

import boofcv.struct.geo.TrifocalTensor;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.factory.SingularValueDecomposition;
import org.ejml.ops.CommonOps;
import org.ejml.ops.SingularOps;

/**
 * <p>
 * Contains commonly used operations used in 2-view and 3-view perspective geometry.
 * </p>
 *
 * <p>
 * LINES:  lines on the image place are represented in homogeneous or generic form as a 3D vector. If a point in
 * homogeneous coordinates is on a line and the dot product is computed the result will be zero.
 * </p>
 *
 * @author Peter Abeles
 */
public class MultiViewOps {

	/**
	 * Create a 3x4 camera matrix. For calibrated camera P = [R|T].  For uncalibrated camera it is P = K*[R|T].
	 *
	 * @param R Rotation matrix. 3x3
	 * @param T Translation vector.
	 * @param K Optional camera calibration matrix 3x3.
	 * @param ret Storage for camera calibration matrix. If null a new instance will be created.
	 * @return Camera calibration matrix.
	 */
	public static DenseMatrix64F createCameraMatrix( DenseMatrix64F R , Vector3D_F64 T , DenseMatrix64F K ,
													 DenseMatrix64F ret ) {
		if( ret == null )
			ret = new DenseMatrix64F(3,4);

		CommonOps.insert(R,ret,0,0);

		ret.data[3] = T.x;
		ret.data[7] = T.y;
		ret.data[11] = T.z;

		if( K == null )
			return ret;

		DenseMatrix64F temp = new DenseMatrix64F(3,4);
		CommonOps.mult(K,ret,temp);

		ret.set(temp);

		return ret;
	}

	/**
	 * Creates a trifocal tensor from two camera matrices. IMPORTANT: It is assumed that the first camera
	 * has the following camera matrix P1 = [I|0], where I is an identify matrix.
	 *
	 * @param P2 Camera matrix from view 1 to view 2
	 * @param P3 Camera matrix from view 1 to view 3
	 * @param ret Storage for trifocal tensor.  If null a new instance will be created.
	 * @return The trifocal tensor
	 */
	public static TrifocalTensor createTrifocal( DenseMatrix64F P2 , DenseMatrix64F P3 , TrifocalTensor ret ) {
		if( ret == null )
			ret = new TrifocalTensor();

		for( int col = 0; col < 3; col++ ) {
			DenseMatrix64F T = ret.getT(col);

			int index = 0;
			for( int i = 0; i < 3; i++ ) {
				for( int j = 0; j < 3; j++ ) {
					T.data[index++] = P2.get(i,col)*P3.get(j,3) - P2.get(i,3)*P3.get(j,col);
				}
			}
		}

		return ret;
	}

	/**
	 * <p>
	 * Trifocal tensor with line-line-line correspondence:<br>
	 * (l2<sup>T</sup>*[T1,T2,T3]*L2)*[l1]<sub>x</sub> = 0
	 * </p>
	 *
	 * @param tensor Trifocal tensor
	 * @param l1 A line in the first view.
	 * @param l2 A line in the second view.
	 * @param l3 A line in the third view.
	 * @param ret Storage for output.  If null a new instance will be declared.
	 * @return Result of applying the constraint.  With perfect inputs will be zero.
	 */
	public static Vector3D_F64 constraintTrifocal( TrifocalTensor tensor ,
												   Vector3D_F64 l1 , Vector3D_F64 l2 , Vector3D_F64 l3 ,
												   Vector3D_F64 ret )
	{
		if( ret == null )
			ret = new Vector3D_F64();

		double x = GeometryMath_F64.innerProd(l2, tensor.T1, l3);
		double y = GeometryMath_F64.innerProd(l2, tensor.T2, l3);
		double z = GeometryMath_F64.innerProd(l2, tensor.T3, l3);

		GeometryMath_F64.cross(new Vector3D_F64(x, y, z), l1, ret);

		return ret;
	}

	/**
	 * <p>
	 * Trifocal tensor with point-line-line correspondence:<br>
	 * (l2<sup>T</sup>*(sum p1<sup>i</sup>*T<sub>i</sub>)*l3 = 0
	 * </p>
	 *
	 * @param tensor Trifocal tensor
	 * @param p1 A point in the first view.
	 * @param l2 A line in the second view.
	 * @param l3 A line in the third view.
	 * @return Result of applying the constraint.  With perfect inputs will be zero.
	 */
	public static double constraintTrifocal( TrifocalTensor tensor ,
											 Point2D_F64 p1 , Vector3D_F64 l2 , Vector3D_F64 l3 )
	{
		DenseMatrix64F sum = new DenseMatrix64F(3,3);

		CommonOps.add(p1.x,tensor.T1,sum,sum);
		CommonOps.add(p1.y,tensor.T2,sum,sum);
		CommonOps.add(tensor.T3, sum, sum);

		return GeometryMath_F64.innerProd(l2,sum,l3);
	}

	/**
	 * <p>
	 * Trifocal tensor with point-line-point correspondence:<br>
	 * (l2<sup>T</sup>(sum p1<sup>i</sup>*T<sub>i</sub>)[p3]<sub>x</sub> = 0
	 * </p>
	 *
	 * @param tensor Trifocal tensor
	 * @param p1 A point in the first view.
	 * @param l2 A line in the second view.
	 * @param p3 A point in the third view.
	 * @return Result of applying the constraint.  With perfect inputs will be zero.
	 */
	public static Vector3D_F64 constraintTrifocal( TrifocalTensor tensor ,
												   Point2D_F64 p1 , Vector3D_F64 l2 , Point2D_F64 p3 ,
												   Vector3D_F64 ret )
	{
		if( ret == null )
			ret = new Vector3D_F64();

		DenseMatrix64F sum = new DenseMatrix64F(3,3);

		CommonOps.add(p1.x,tensor.T1,sum,sum);
		CommonOps.add(p1.y,tensor.T2,sum,sum);
		CommonOps.add(tensor.T3,sum,sum);

		Vector3D_F64 tempV = new Vector3D_F64();
		GeometryMath_F64.multTran(sum, l2, tempV);

		GeometryMath_F64.cross(tempV, new Vector3D_F64(p3.x, p3.y, 1), ret);

		return ret;
	}

	/**
	 * <p>
	 * Trifocal tensor with point-point-line correspondence:<br>
	 * [p2]<sub>x</sub>(sum p1<sup>i</sup>*T<sub>i</sub>)*l3 = 0
	 * </p>
	 *
	 * @param tensor Trifocal tensor
	 * @param p1 A point in the first view.
	 * @param p2 A point in the second view.
	 * @param l3 A line in the third view.
	 * @return Result of applying the constraint.  With perfect inputs will be zero.
	 */
	public static Vector3D_F64 constraintTrifocal( TrifocalTensor tensor ,
												   Point2D_F64 p1 , Point2D_F64 p2 , Vector3D_F64 l3 ,
												   Vector3D_F64 ret )
	{
		if( ret == null )
			ret = new Vector3D_F64();

		DenseMatrix64F sum = new DenseMatrix64F(3,3);

		CommonOps.add(p1.x,tensor.T1,sum,sum);
		CommonOps.add(p1.y,tensor.T2,sum,sum);
		CommonOps.add(tensor.T3,sum,sum);

		DenseMatrix64F cross2 = GeometryMath_F64.crossMatrix(p2.x,p2.y,1,null);

		DenseMatrix64F temp = new DenseMatrix64F(3,3);

		CommonOps.mult(cross2,sum,temp);
		GeometryMath_F64.mult(temp, l3, ret);

		return ret;
	}

	/**
	 * <p>
	 * Trifocal tensor with point-point-point correspondence:<br>
	 * [p2]<sub>x</sub>(sum p1<sup>i</sup>*T<sub>i</sub>)[p3]<sub>x</sub> = 0
	 * </p>
	 *
	 * @param tensor Trifocal tensor
	 * @param p1 A point in the first view.
	 * @param p2 A point in the second view.
	 * @param p3 A point in the third view.
	 * @param ret Optional storage for output. 3x3 matrix.  Modified.
	 * @return Result of applying the constraint.  With perfect inputs will be zero.
	 */
	public static DenseMatrix64F constraintTrifocal( TrifocalTensor tensor ,
													 Point2D_F64 p1 , Point2D_F64 p2 , Point2D_F64 p3 ,
													 DenseMatrix64F ret )
	{
		if( ret == null )
			ret = new DenseMatrix64F(3,3);

		DenseMatrix64F sum = new DenseMatrix64F(3,3);

		CommonOps.add(p1.x,tensor.T1,p1.y,tensor.T2,sum);
		CommonOps.add(sum,tensor.T3,sum);

		DenseMatrix64F cross2 = GeometryMath_F64.crossMatrix(p2.x,p2.y,1,null);
		DenseMatrix64F cross3 = GeometryMath_F64.crossMatrix(p3.x,p3.y,1,null);

		DenseMatrix64F temp = new DenseMatrix64F(3,3);

		CommonOps.mult(cross2,sum,temp);
		CommonOps.mult(temp, cross3, ret);

		return ret;
	}

	/**
	 * <p>
	 * Computes the epipoles of the first camera in the second and third images.  Epipoles are found
	 * in homogeneous coordinates.
	 * </p>
	 *
	 * <p>
	 * Properties:
	 * <ul>
	 *     <li> e2<sup>T</sup>*F12 = 0
	 *     <li> e3<sup>T</sup>*F13 = 0
	 * </ul>
	 * where F1i is a fundamental matrix from image 1 to i.
	 * </p>
	 *
	 * @param tensor Trifocal tensor.  Not Modified
	 * @param e2  Output: Epipole in image 2. Homogeneous coordinates. Modified
	 * @param e3  Output: Epipole in image 3. Homogeneous coordinates. Modified
	 */
	public static void extractEpipoles( TrifocalTensor tensor , Point3D_F64 e2 , Point3D_F64 e3 ) {
		SingularValueDecomposition<DenseMatrix64F> svd = DecompositionFactory.svd(3,3,true,true,false);
		if( svd.inputModified() ) {
			tensor = tensor.copy();
		}

		DenseMatrix64F u1 = new DenseMatrix64F(3,1);
		DenseMatrix64F u2 = new DenseMatrix64F(3,1);
		DenseMatrix64F u3 = new DenseMatrix64F(3,1);
		DenseMatrix64F v1 = new DenseMatrix64F(3,1);
		DenseMatrix64F v2 = new DenseMatrix64F(3,1);
		DenseMatrix64F v3 = new DenseMatrix64F(3,1);

		svd.decompose(tensor.T1);
		SingularOps.nullVector(svd, true, v1);
		SingularOps.nullVector(svd, false,u1);

		svd.decompose(tensor.T2);
		SingularOps.nullVector(svd,true,v2);
		SingularOps.nullVector(svd,false,u2);

		svd.decompose(tensor.T3);
		SingularOps.nullVector(svd,true,v3);
		SingularOps.nullVector(svd,false,u3);

		DenseMatrix64F U = new DenseMatrix64F(3,3);
		DenseMatrix64F V = new DenseMatrix64F(3,3);

		for( int i = 0; i < 3; i++ ) {
			U.set(i,0,u1.get(i));
			U.set(i,1,u2.get(i));
			U.set(i,2,u3.get(i));

			V.set(i, 0, v1.get(i));
			V.set(i, 1, v2.get(i));
			V.set(i, 2, v3.get(i));
		}

		DenseMatrix64F tempE = new DenseMatrix64F(3,1);

		svd.decompose(U);
		SingularOps.nullVector(svd, false, tempE);
		e2.set(tempE.get(0),tempE.get(1),tempE.get(2));

		svd.decompose(V);
		SingularOps.nullVector(svd, false, tempE);
		e3.set(tempE.get(0),tempE.get(1),tempE.get(2));
	}

	/**
	 * Extract the fundamental matrices between views 1 + 2 and views 1 + 3.  The returned Fundamental
	 * matrices will have the following properties: x<sub>i</sub><sup>T</sup>*Fi*x<sub>1</sub> = 0, where i is view 2 or 3.
	 *
	 * @param tensor Trifocal tensor.  Not modified.
	 * @param F2 Output: Fundamental matrix for views 1 and 2. Modified.
	 * @param F3 Output: Fundamental matrix for views 1 and 3. Modified.
	 */
	public static void extractFundamental( TrifocalTensor tensor , DenseMatrix64F F2 , DenseMatrix64F F3 ) {
		// extract the epipoles
		Point3D_F64 e2 = new Point3D_F64();
		Point3D_F64 e3 = new Point3D_F64();

		extractEpipoles(tensor, e2, e3);

		// storage for intermediate results
		Point3D_F64 temp0 = new Point3D_F64();
		Point3D_F64 column = new Point3D_F64();

		// compute the Fundamental matrices one column at a time
		for( int i = 0; i < 3; i++ ) {
			DenseMatrix64F T = tensor.getT(i);

			GeometryMath_F64.mult(T,e3,temp0);
			GeometryMath_F64.cross(e2,temp0,column);

			F2.set(0,i,column.x);
			F2.set(1,i,column.y);
			F2.set(2,i,column.z);

			GeometryMath_F64.multTran(T,e2,temp0);
			GeometryMath_F64.cross(e3,temp0,column);

			F3.set(0,i,column.x);
			F3.set(1,i,column.y);
			F3.set(2,i,column.z);
		}
	}

	/**
	 * Extract the camera matrices up to a common projective transform.  The camera matrix for the
	 * first view is assumed to be P1 = [I|0].
	 *
	 * @param tensor Trifocal tensor.  Not modified.
	 * @param P2 Output: 3x4 camera matrix for views 1 to 2. Modified.
	 * @param P3 Output: 3x4 camera matrix for views 1 to 3. Modified.
	 */
	public static void extractCameraMatrices( TrifocalTensor tensor , DenseMatrix64F P2 , DenseMatrix64F P3 ) {
		// extract the epipoles
		Point3D_F64 e2 = new Point3D_F64();
		Point3D_F64 e3 = new Point3D_F64();

		extractEpipoles(tensor, e2, e3);

		// storage for intermediate results
		Point3D_F64 temp0 = new Point3D_F64();
		Point3D_F64 column = new Point3D_F64();
		// temp1 = [e3*e3^T -I]
		DenseMatrix64F temp1 = new DenseMatrix64F(3,3);
		for( int i = 0; i < 3; i++ ) {
			for( int j = 0; j < 3; j++ ) {
				temp1.set(i,j,e3.getIndex(i)*e3.getIndex(j));
			}
			temp1.set(i,i , temp1.get(i,i) - 1);
		}

		// compute the Fundamental matrices one column at a time
		for( int i = 0; i < 3; i++ ) {
			DenseMatrix64F T = tensor.getT(i);

			GeometryMath_F64.mult(T, e3, column);
			P2.set(0,i,column.x);
			P2.set(1,i,column.y);
			P2.set(2,i,column.z);
			P2.set(i,3,e2.getIndex(i));

			GeometryMath_F64.multTran(T,e2,temp0);
			GeometryMath_F64.mult(temp1, temp0, column);

			P3.set(0,i,column.x);
			P3.set(1,i,column.y);
			P3.set(2,i,column.z);
			P3.set(i,3,e3.getIndex(i));
		}
	}
}
