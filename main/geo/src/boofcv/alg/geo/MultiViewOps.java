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
import georegression.struct.point.Vector3D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
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
		CommonOps.add(tensor.T3,sum,sum);

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

		GeometryMath_F64.cross(tempV, new Vector3D_F64(p3.x,p3.y,1), ret);

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
		GeometryMath_F64.mult(temp,l3,ret);

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

	public void extractEpipoles( TrifocalTensor tensor , Point2D_F64 e2 , Point2D_F64 e3 ) {

	}

	/**
	 * Extract the fundamental matrices between views 1 + 2 and views 1 + 3.  The returned Fundamental
	 * matrices will have the following properties: x<sub>i</sub><sup>T</sup>*Fi*x<sub>1</sub> = 0, where i is for view 2 or 3.
	 *
	 * @param tensor Trifocal tensor
	 * @param F2 Output for Fundamental matrix for views 1 and 2.
	 * @param F3 Output for Fundamental matrix for views 1 and 3.
	 */
	public void extractFundamental( TrifocalTensor tensor , DenseMatrix64F F2 , DenseMatrix64F F3 ) {

	}

	public void extractCameraMatrices( TrifocalTensor tensor , DenseMatrix64F P2 , DenseMatrix64F P3 ) {

	}
}
